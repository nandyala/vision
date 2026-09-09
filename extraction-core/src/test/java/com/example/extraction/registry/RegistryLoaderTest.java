package com.example.extraction.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryLoaderTest {
    @Test
    void loadsTwoFormatsWithoutCodeBranches() {
        Registry registry = new RegistryLoader(new ObjectMapper().findAndRegisterModules()).loadDefault();

        CategoryConfig category = registry.category("RECURRING_AUTO_LOAN_PAYMENT");

        assertThat(category.formats()).containsKeys("RALP_FORMAT_01", "RALP_FORMAT_03");
        assertThat(category.format("RALP_FORMAT_03").fields())
                .extracting(FieldConfig::canonical)
                .contains("bankRoutingNumber", "bankAccountNumber", "paymentAmount", "firstPaymentDate", "paymentFrequency");
    }
}
