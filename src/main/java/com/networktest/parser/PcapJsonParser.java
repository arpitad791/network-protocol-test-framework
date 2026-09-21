package com.networktest.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a Wireshark/tshark JSON export (produced with `tshark -r capture.pcapng -T json`)
 * and exposes each packet's "layers" object so tests can assert on protocol fields
 * without depending on a native pcap library.
 */
public class PcapJsonParser {

    private final List<JsonNode> packetLayers = new ArrayList<>();

    public PcapJsonParser(String classpathResource) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IOException("Capture file not found on classpath: " + classpathResource);
            }
            JsonNode root = mapper.readTree(in);
            for (JsonNode packet : root) {
                packetLayers.add(packet.path("_source").path("layers"));
            }
        }
    }

    public List<JsonNode> getPackets() {
        return packetLayers;
    }

    /** Returns only the packets whose layer stack includes the given protocol, e.g. "tcp", "dns", "http", "icmp". */
    public List<JsonNode> getPacketsContainingLayer(String layerName) {
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode layers : packetLayers) {
            if (!layers.path(layerName).isMissingNode()) {
                matches.add(layers);
            }
        }
        return matches;
    }
}
