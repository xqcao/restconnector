package io.camunda.example.one;

import java.util.Map;

public record OneConnectorResult(
        int status,
        Object body,
        Map<String, String> headers) {
}
