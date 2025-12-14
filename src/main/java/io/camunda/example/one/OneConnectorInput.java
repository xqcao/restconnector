package io.camunda.example.one;

import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty.PropertyType;

public record OneConnectorInput(
        @TemplateProperty(group = "request", label = "URL", description = "Target URL") String url,

        @TemplateProperty(group = "request", label = "Method", defaultValue = "GET", description = "HTTP Method (GET, POST, etc.)") String method,

        @TemplateProperty(group = "request", label = "Headers", optional = true) Object headers,

        @TemplateProperty(group = "request", label = "Request Body", type = PropertyType.Text, optional = true, description = "JSON payload or FEEL expression") Object body) {
}
