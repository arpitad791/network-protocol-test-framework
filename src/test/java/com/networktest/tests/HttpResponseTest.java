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

    @Test
    public void requestUsesGetMethod() {
        String method = httpPackets.get(0).path("http").path("http.request.method").asText();
        Assert.assertEquals(method, "GET");
    }

    @Test
    public void requestTargetsExpectedHost() {
        String host = httpPackets.get(0).path("http").path("http.host").asText();
        Assert.assertEquals(host, "example.com");
    }

    @Test
    public void responseReturnsHttp200() {
        String statusCode = httpPackets.get(1).path("http").path("http.response.code").asText();
        Assert.assertEquals(statusCode, "200", "Server should respond with 200 OK");
    }
}
