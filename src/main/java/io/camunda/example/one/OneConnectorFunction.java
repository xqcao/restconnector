package io.camunda.example.one;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OutboundConnector(name = "OneConnector", inputVariables = { "url", "method", "headers",
        "body" }, type = "io.camunda:one-connector:1")
@ElementTemplate(id = "io.camunda.connector.One.v1", name = "One Connector (REST)", version = 1, description = "Generic REST Connector with JSON support", icon = "icon.svg", documentationRef = "https://docs.camunda.io", propertyGroups = {
        @ElementTemplate.PropertyGroup(id = "request", label = "Request Config")
}, inputDataClass = OneConnectorInput.class)
public class OneConnectorFunction implements OutboundConnectorFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OneConnectorFunction.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OneConnectorFunction() {
        this(new ObjectMapper(), HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    public OneConnectorFunction(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Object execute(OutboundConnectorContext context) throws Exception {
        final var connectorInput = context.bindVariables(OneConnectorInput.class);
        return executeConnector(connectorInput);
    }

    private OneConnectorResult executeConnector(final OneConnectorInput input)
            throws IOException, InterruptedException {
        LOGGER.info("Executing OneConnector with input: {}", input);

        if (input.url() == null || input.url().isBlank()) {
            throw new ConnectorException("INVALID_URL", "URL must not be empty");
        }

        String method = input.method() != null ? input.method().toUpperCase() : "GET";

        // Prepare Request Body
        String requestBodyJson = "";
        if (input.body() != null) {
            if (input.body() instanceof String) {
                requestBodyJson = (String) input.body();
            } else {
                requestBodyJson = objectMapper.writeValueAsString(input.body());
            }
        }

        // Build Request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(input.url()))
                .method(method, BodyPublishers.ofString(requestBodyJson));

        // Headers
        Map<String, String> headerMap = new HashMap<>();
        if (input.headers() != null) {
            if (input.headers() instanceof Map) {
                ((Map<?, ?>) input.headers()).forEach((k, v) -> headerMap.put(String.valueOf(k), String.valueOf(v)));
            } else if (input.headers() instanceof String) {
                try {
                    Map<?, ?> map = objectMapper.readValue((String) input.headers(), Map.class);
                    map.forEach((k, v) -> headerMap.put(String.valueOf(k), String.valueOf(v)));
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse headers string: {}", input.headers(), e);
                }
            }
        }
        headerMap.forEach(requestBuilder::header);
        // Default Content-Type if not present and body exists
        if (!headerMap.containsKey("Content-Type") && !requestBodyJson.isEmpty()) {
            requestBuilder.header("Content-Type", "application/json");
        }

        HttpRequest request = requestBuilder.build();

        // Send Request
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse Response Body
        Object responseBody;
        try {
            if (response.body() != null && !response.body().isBlank()) {
                responseBody = objectMapper.readValue(response.body(), Object.class);
            } else {
                responseBody = null;
            }
        } catch (Exception e) {
            // Fallback to raw string if not JSON
            responseBody = response.body();
            LOGGER.warn("Failed to parse response as JSON (Status: {}). Returning raw string. Response: {}",
                    response.statusCode(), response.body(), e);
        }

        // Capture Response Headers
        Map<String, String> responseHeaders = new HashMap<>();
        response.headers().map().forEach((k, v) -> responseHeaders.put(k, String.join(",", v)));

        return new OneConnectorResult(response.statusCode(), responseBody, responseHeaders);
    }
}
