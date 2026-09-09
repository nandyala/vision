# Document Extraction PoC

Minimal Java 21 pipeline for recurring auto-loan payment authorization forms.

## Modules

- `extraction-core`: Spring-free classification, extraction mapping, validation, decisioning, Azure Document Intelligence wrapper, and canonical result records.
- `extraction-batch`: Spring Batch folder reader, core-backed processor, JSON writer, concurrency, and caller-owned Azure credential setup.
- `eval`: held-out-set scoring harness for classifier accuracy, per-field exact match, outcome distribution, straight-through-processing rate, and high-confidence misses.

## Azure SDK Shape Confirmed

Microsoft Learn currently lists:

- Maven artifact: `com.azure:azure-ai-documentintelligence:1.0.7`
- Client: `com.azure.ai.documentintelligence.DocumentIntelligenceClient`
- Methods used here:
  - `beginClassifyDocument(String classifierId, BinaryData classifyRequest, RequestOptions requestOptions)`
  - `beginAnalyzeDocument(String modelId, BinaryData analyzeRequest, RequestOptions requestOptions)`

The core sends document bytes with a `base64Source` request body and does not persist source documents.

## Run Tests

```bash
mvn test
```

## Run Batch

Provide either a resource key or certificate-backed service principal settings from the batch module. The core does not read environment variables.

```bash
export EXTRACTION_INPUT_FOLDER=/path/to/input
export EXTRACTION_OUTPUT_FOLDER=/path/to/output
export AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT=https://YOUR-RESOURCE.cognitiveservices.azure.com/
export AZURE_DOCUMENT_INTELLIGENCE_KEY=YOUR_KEY
export EXTRACTION_ASSUME_FORMAT_ID=RALP_FORMAT_01
export EXTRACTION_CONCURRENCY=1
export EXTRACTION_MAX_IN_FLIGHT_REQUESTS=1

mvn -pl extraction-batch spring-boot:run
```

`EXTRACTION_ASSUME_FORMAT_ID` skips custom classification and routes directly to the extraction model configured for that format in `registry.json`. Use this when you only have a custom extraction model.

For Entra ID certificate auth, omit the key and provide:

```bash
export AZURE_DOCUMENT_INTELLIGENCE_TENANT_ID=...
export AZURE_DOCUMENT_INTELLIGENCE_CLIENT_ID=...
export AZURE_DOCUMENT_INTELLIGENCE_CERTIFICATE_PATH=/path/to/cert.pem
```

Optional proxy settings:

```bash
export AZURE_DOCUMENT_INTELLIGENCE_PROXY_HOST=proxy.example.com
export AZURE_DOCUMENT_INTELLIGENCE_PROXY_PORT=8080
```

## Registry

The registry lives at `extraction-core/src/main/resources/registry.json`. Add a new form layout by adding a new format entry with its trained extraction model ID and field mappings. The extraction path does not branch on concrete format IDs.
