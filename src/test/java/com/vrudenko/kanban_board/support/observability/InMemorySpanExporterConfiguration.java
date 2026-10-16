package com.vrudenko.kanban_board.support.observability;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Collect every span the application finishes in memory, so a tracing test reads the spans
 * instead of calling a collector.
 *
 * Spring Boot adds each SpanExporter bean to its span processor, so declaring the exporter here is
 * all the wiring needed. Import it next to AutoConfigureObservability: without that annotation
 * Spring Boot disables tracing in a SpringBootTest and nothing is recorded.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InMemorySpanExporterConfiguration {

    @Bean
    public InMemorySpanExporter inMemorySpanExporter() {
        return InMemorySpanExporter.create();
    }
}
