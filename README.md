# Network Protocol Test Automation Framework

A small TestNG automation framework that validates core network protocol behaviour
(TCP handshake, DNS resolution, HTTP request/response, ICMP ping) from packet
captures taken with **Wireshark / tshark**.

## Why this project exists

Built while preparing for a Testing Intern role focused on network/telecom
verification. It's deliberately scoped to fundamentals rather than anything
telecom-specific (DWDM/optical gear isn't something a fresher can access), but
it exercises the same muscle the role needs day to day:

- Reading a packet capture and reasoning about protocol correctness
- Writing **TestNG** based functional/regression test cases
- Structuring a test project with Maven, fixtures, and CI
- Producing test execution reports a reviewer can read

## How it works

1. Traffic is captured with **Wireshark** (or its CLI, `tshark`) and exported to JSON:
   ```bash
   tshark -r capture.pcapng -T json > capture.json
   ```
2. `PcapJsonParser` (in `src/main/java/com/networktest/parser`) loads that JSON
   and hands back each packet's `layers` object using Jackson's tree model —
   no native pcap library needed, just the JSON tshark already produces.
3. TestNG test classes under `src/test/java/com/networktest/tests` filter for
   packets containing a given protocol layer (`tcp`, `dns`, `http`, `icmp`) and
   assert on the fields that prove the protocol behaved correctly, e.g.:
   - **TcpHandshakeTest** — SYN → SYN-ACK → ACK flag sequence and that the
     server's ACK number equals the client's SYN sequence + 1
   - **DnsResolutionTest** — query/response pair share a transaction ID and the
     response carries at least one answer record
   - **HttpResponseTest** — request uses GET, response is HTTP 200
   - **IcmpPingTest** — echo request/reply share an identifier and round-trip
     latency is under a threshold

## Sample captures included

`src/test/resources/captures/*.json` contains four small, hand-built capture
exports (one per protocol) so `mvn test` passes immediately without needing
Wireshark installed. They mirror the exact shape tshark's `-T json` output
produces.

## Running the tests

```bash
mvn test
```

TestNG's HTML/XML report is written to `target/surefire-reports/`. CI
(`.github/workflows/ci.yml`) runs the same command on every push/PR and
uploads that report as a build artifact — this is the "participate in CI"
and "execution reports" part of the workflow.

## Using your own real captures (recommended before an interview)

The included fixtures are synthetic so the repo runs out of the box, but you
should replace at least one with a real capture so you can speak to it
first-hand:

1. Install [Wireshark](https://www.wireshark.org/) (includes `tshark`).
2. Start a capture on your active interface, then generate the traffic you
   want to test, e.g.:
   - `ping 8.8.8.8` for the ICMP case
   - `nslookup example.com` for the DNS case
   - visiting `http://example.com` in a browser for the HTTP case (use plain
     HTTP, not HTTPS, so the request/response are visible in plaintext)
3. Stop the capture, apply a display filter (`tcp`, `dns`, `http`, or `icmp`)
   to isolate the relevant packets, and export: File → Export Packet
   Dissections, or via the CLI:
   ```bash
   tshark -r your_capture.pcapng -Y "dns" -T json > src/test/resources/captures/dns_query.json
   ```
4. Re-run `mvn test`. If field values differ from your traffic (e.g. a
   different domain name), update the assertions in the matching test class
   to match what you actually captured — that's the point: you're now
   asserting on real behaviour you observed yourself.

## Project structure

```
network-protocol-test-framework/
├── pom.xml
├── src/
│   ├── main/java/com/networktest/parser/PcapJsonParser.java
│   └── test/
│       ├── java/com/networktest/tests/
│       │   ├── TcpHandshakeTest.java
│       │   ├── DnsResolutionTest.java
│       │   ├── HttpResponseTest.java
│       │   └── IcmpPingTest.java
│       └── resources/
│           ├── testng.xml
│           └── captures/*.json
└── .github/workflows/ci.yml
```

## Talking points for an interview

- **What it tests and why**: three protocols most networking coursework
  covers (TCP, DNS, ICMP) plus HTTP, validated against the actual field
  values Wireshark decodes, not just "did the ping succeed."
- **Why JSON export instead of a pcap parsing library**: keeps the framework
  dependency-light (just Jackson) and mirrors how a CI pipeline would consume
  `tshark` output without needing native packet-capture permissions on a
  build agent.
- **How this would extend to a real E2E/telecom environment**: same pattern —
  capture on a DUT (device under test) or lab interface, export, assert on
  protocol/field correctness — just pointed at real hardware captures instead
  of sample fixtures.
