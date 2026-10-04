package com.networktest.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.networktest.parser.PcapJsonParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Validates an HTTP request/response pair captured in
 * src/test/resources/captures/http_get.json
 */
public class HttpResponseTest {

    private List<JsonNode> httpPackets;

    @BeforeClass
    public void loadCapture() throws Exception {
        PcapJsonParser parser = new PcapJsonParser("captures/http_get.json");
        httpPackets = parser.getPacketsContainingLayer("http");
    }

    @Test
    public void captureContainsRequestAndResponse() {
        Assert.assertEquals(httpPackets.size(), 2, "Expected one HTTP request and one HTTP response");
    }

    // tshark nests some HTTP fields under a key like "GET / HTTP/1.1\r\n", so search all levels.
    private String httpField(JsonNode packet, String fieldName) {
        JsonNode value = packet.path("http").findValue(fieldName);
        return value == null ? null : value.asText();
    }

    @Test
    public void requestUsesGetMethod() {
        Assert.assertEquals(httpField(httpPackets.get(0), "http.request.method"), "GET");
    }

    @Test
    public void requestTargetsExpectedHost() {
        Assert.assertEquals(httpField(httpPackets.get(0), "http.host"), "example.com");
    }

    @Test
    public void responseReturnsHttp200() {
        Assert.assertEquals(httpField(httpPackets.get(1), "http.response.code"), "200",
                "Server should respond with 200 OK");
    }
}
