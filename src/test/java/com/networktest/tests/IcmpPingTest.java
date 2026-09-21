package com.networktest.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.networktest.parser.PcapJsonParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Validates an ICMP echo request/reply pair captured in
 * src/test/resources/captures/icmp_ping.json
 */
public class IcmpPingTest {

    private static final double MAX_ACCEPTABLE_LATENCY_MS = 500.0;

    private List<JsonNode> icmpPackets;

    @BeforeClass
    public void loadCapture() throws Exception {
        PcapJsonParser parser = new PcapJsonParser("captures/icmp_ping.json");
        icmpPackets = parser.getPacketsContainingLayer("icmp");
    }

    @Test
    public void captureContainsRequestAndReply() {
        Assert.assertEquals(icmpPackets.size(), 2, "Expected one echo request and one echo reply");
    }

    @Test
    public void requestIsEchoAndReplyIsEchoReply() {
        String requestType = icmpPackets.get(0).path("icmp").path("icmp.type").asText();
        String replyType = icmpPackets.get(1).path("icmp").path("icmp.type").asText();
        Assert.assertEquals(requestType, "8", "First packet should be an ICMP echo request (type 8)");
        Assert.assertEquals(replyType, "0", "Second packet should be an ICMP echo reply (type 0)");
    }

    @Test
    public void requestAndReplyShareIdentifier() {
        String requestId = icmpPackets.get(0).path("icmp").path("icmp.ident").asText();
        String replyId = icmpPackets.get(1).path("icmp").path("icmp.ident").asText();
        Assert.assertEquals(replyId, requestId, "Echo reply must carry the same identifier as the request");
    }

    @Test
    public void roundTripLatencyIsUnderThreshold() {
        double requestTime = Double.parseDouble(icmpPackets.get(0).path("frame").path("frame.time_epoch").asText());
        double replyTime = Double.parseDouble(icmpPackets.get(1).path("frame").path("frame.time_epoch").asText());
        double latencyMs = (replyTime - requestTime) * 1000;
        Assert.assertTrue(latencyMs < MAX_ACCEPTABLE_LATENCY_MS,
                "Round-trip latency should be under " + MAX_ACCEPTABLE_LATENCY_MS + "ms, was " + latencyMs + "ms");
    }
}
