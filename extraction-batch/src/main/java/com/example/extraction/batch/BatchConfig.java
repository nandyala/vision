package com.example.extraction.batch;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.FixedDelayOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.example.extraction.ExtractionService;
import com.example.extraction.decision.DecisionEngine;
import com.example.extraction.di.AzureDocumentIntelligenceGateway;
import com.example.extraction.di.DocumentIntelligenceGateway;
import com.example.extraction.di.TransientDocumentIntelligenceException;
import com.example.extraction.mapping.FieldMapper;
import com.example.extraction.registry.Registry;
import com.example.extraction.registry.RegistryLoader;
import com.example.extraction.validation.ValidatorRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HexFormat;

@Configuration
public class BatchConfig {
    private static final Logger log = LoggerFactory.getLogger(BatchConfig.class);

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    Registry registry(ObjectMapper mapper) {
        return new RegistryLoader(mapper).loadDefault();
    }

    @Bean
    ExtractionService extractionService(Registry registry, DocumentIntelligenceClient client, ObjectMapper mapper,
                                        @Value("${extraction.maxInFlightRequests:4}") int maxInFlightRequests,
                                        @Value("${extraction.assumeFormatId:}") String assumeFormatId) {
        DocumentIntelligenceGateway gateway = new AzureDocumentIntelligenceGateway(
                client,
                mapper,
                maxInFlightRequests,
                3,
                Duration.ofSeconds(1));
        if (!assumeFormatId.isBlank()) {
            gateway = new AssumedFormatGateway(gateway, assumeFormatId);
        }
        ValidatorRegistry validators = ValidatorRegistry.defaults();
        return new ExtractionService(registry, gateway, new FieldMapper(validators), new DecisionEngine());
    }

    @Bean
    DocumentIntelligenceClient documentIntelligenceClient(
            @Value("${azure.document-intelligence.endpoint}") String endpoint,
            @Value("${azure.document-intelligence.key:}") String key,
            @Value("${azure.document-intelligence.tenant-id:}") String tenantId,
            @Value("${azure.document-intelligence.client-id:}") String clientId,
            @Value("${azure.document-intelligence.certificate-path:}") String certificatePath,
            @Value("${azure.document-intelligence.proxy-host:}") String proxyHost,
            @Value("${azure.document-intelligence.proxy-port:0}") int proxyPort) {
        if (endpoint.isBlank()) {
            throw new IllegalArgumentException("AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT is required");
        }
        log.info(
                "Using Azure Document Intelligence endpoint {} with key {}",
                endpoint,
                key.isBlank() ? "not configured" : maskedKey(key));
        DocumentIntelligenceClientBuilder builder = new DocumentIntelligenceClientBuilder()
                .endpoint(endpoint)
                .retryPolicy(new RetryPolicy(new RetryOptions(new FixedDelayOptions(1, Duration.ZERO))));

        if (!proxyHost.isBlank()) {
            builder.httpClient(new NettyAsyncHttpClientBuilder()
                    .proxy(new ProxyOptions(ProxyOptions.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)))
                    .build());
        }

        if (!key.isBlank()) {
            return builder.credential(new AzureKeyCredential(key)).buildClient();
        }

        return builder.credential(new ClientCertificateCredentialBuilder()
                        .tenantId(tenantId)
                        .clientId(clientId)
                        .pemCertificate(certificatePath)
                        .build())
                .buildClient();
    }

    private String maskedKey(String key) {
        String trimmed = key.trim();
        String suffix = trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
        return "configured length=" + trimmed.length() + " sha256=" + sha256Prefix(trimmed) + " last4=****" + suffix;
    }

    private String sha256Prefix(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Bean
    ItemReader<DocumentItem> reader(@Value("${extraction.inputFolder}") Path inputFolder) {
        return new DocumentItemReader(inputFolder);
    }

    @Bean
    ItemWriter<com.example.extraction.model.ExtractionResult> writer(ObjectMapper mapper,
                                                                     @Value("${extraction.outputFolder}") Path outputFolder) {
        return new JsonItemWriter(mapper, outputFolder);
    }

    @Bean
    SkipListener<DocumentItem, com.example.extraction.model.ExtractionResult> skipListener() {
        return new ExtractionSkipListener();
    }

    @Bean
    Step extractionStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        ItemReader<DocumentItem> reader,
                        ItemWriter<com.example.extraction.model.ExtractionResult> writer,
                        SkipListener<DocumentItem, com.example.extraction.model.ExtractionResult> skipListener,
                        ExtractionService extractionService,
                        @Value("${extraction.category:RECURRING_AUTO_LOAN_PAYMENT}") String category,
                        @Value("${extraction.chunkSize:10}") int chunkSize,
                        @Value("${extraction.concurrency:4}") int concurrency) {
        var stepBuilder = new StepBuilder("extractDocuments", jobRepository)
                .<DocumentItem, com.example.extraction.model.ExtractionResult>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(new ExtractionItemProcessor(extractionService, category))
                .writer(writer)
                .faultTolerant()
                .retry(TransientDocumentIntelligenceException.class)
                .retryLimit(2)
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .listener(skipListener);

        if (concurrency > 1) {
            SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("extraction-");
            taskExecutor.setConcurrencyLimit(concurrency);
            stepBuilder.taskExecutor(taskExecutor);
        }

        return stepBuilder.build();
    }

    @Bean
    Job extractionJob(JobRepository jobRepository, Step extractionStep) {
        return new JobBuilder("documentExtraction", jobRepository)
                .start(extractionStep)
                .build();
    }
}
