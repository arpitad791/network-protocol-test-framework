package com.networktest.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.networktest.parser.PcapJsonParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Validates the TCP three-way handshake (SYN, SYN-ACK, ACK) captured in
 * src/test/resources/captures/tcp_handshake.json
 */
public class TcpHandshakeTest {

    private List<JsonNode> tcpPackets;

    @BeforeClass
    public void loadCapture() throws Exception {
        PcapJsonParser parser = new PcapJsonParser("captures/tcp_handshake.json");
        tcpPackets = parser.getPacketsContainingLayer("tcp");
    }

    @Test
    public void captureContainsThreeHandshakePackets() {
        Assert.assertEquals(tcpPackets.size(), 3, "Expected exactly SYN, SYN-ACK, ACK packets in the capture");
    }

    @Test
    public void firstPacketIsSynOnly() {
        JsonNode flags = tcpPackets.get(0).path("tcp").path("tcp.flags_tree");
        Assert.assertEquals(flags.path("tcp.flags.syn").asText(), "1", "First packet must have SYN set");
        Assert.assertEquals(flags.path("tcp.flags.ack").asText(), "0", "First packet must not have ACK set");
    }

    @Test
    public void secondPacketIsSynAck() {
        JsonNode flags = tcpPackets.get(1).path("tcp").path("tcp.flags_tree");
        Assert.assertEquals(flags.path("tcp.flags.syn").asText(), "1", "Second packet must have SYN set");
        Assert.assertEquals(flags.path("tcp.flags.ack").asText(), "1", "Second packet must have ACK set");
    }

    @Test
    public void thirdPacketIsAckOnly() {
        JsonNode flags = tcpPackets.get(2).path("tcp").path("tcp.flags_tree");
        Assert.assertEquals(flags.path("tcp.flags.syn").asText(), "0", "Third packet must not have SYN set");
        Assert.assertEquals(flags.path("tcp.flags.ack").asText(), "1", "Third packet must have ACK set");
    }

    @Test
    public void sequenceAndAckNumbersAreConsistent() {
        long clientSyn = tcpPackets.get(0).path("tcp").path("tcp.seq").asLong();
        long serverAck = tcpPackets.get(1).path("tcp").path("tcp.ack").asLong();
        Assert.assertEquals(serverAck, clientSyn + 1,
                "Server's SYN-ACK should acknowledge client's SYN sequence number + 1");
    }
}
