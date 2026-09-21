package com.networktest.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.networktest.parser.PcapJsonParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Validates a DNS query/response pair captured in
 * src/test/resources/captures/dns_query.json
 */
public class DnsResolutionTest {

    private List<JsonNode> dnsPackets;

    @BeforeClass
    public void loadCapture() throws Exception {
        PcapJsonParser parser = new PcapJsonParser("captures/dns_query.json");
        dnsPackets = parser.getPacketsContainingLayer("dns");
    }

    @Test
    public void captureContainsQueryAndResponse() {
        Assert.assertEquals(dnsPackets.size(), 2, "Expected one DNS query and one DNS response");
    }

    @Test
    public void queryAsksForExpectedDomain() {
        JsonNode queries = dnsPackets.get(0).path("dns").path("Queries");
        Iterator<Map.Entry<String, JsonNode>> fields = queries.fields();
        Assert.assertTrue(fields.hasNext(), "Query packet should contain a Queries section");
        String queryName = fields.next().getValue().path("dns.qry.name").asText();
        Assert.assertEquals(queryName, "example.com");
    }

    @Test
    public void responseContainsAtLeastOneAnswer() {
        int answerCount = dnsPackets.get(1).path("dns").path("dns.count.answers").asInt();
        Assert.assertTrue(answerCount > 0, "DNS response should contain at least one answer record");
    }

    @Test
    public void responseTransactionIdMatchesQuery() {
        String queryId = dnsPackets.get(0).path("dns").path("dns.id").asText();
        String responseId = dnsPackets.get(1).path("dns").path("dns.id").asText();
        Assert.assertEquals(responseId, queryId, "Response transaction ID must match the query it answers");
    }
}
