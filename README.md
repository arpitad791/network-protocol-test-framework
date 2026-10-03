# Network Protocol Test Automation Framework

A small TestNG automation framework that validates core network protocol behaviour
(TCP handshake, DNS resolution, HTTP request/response, ICMP ping) from packet
captures taken with **Wireshark / tshark**.

## Why this project exists

Network problems often show up as protocol-level symptoms: a handshake that
never completes, a DNS response with no answer, a reply that arrives too late.
Wireshark shows these in a GUI, but checking them by eye doesn't scale or
repeat.

This project turns those checks into automated tests. It takes packet
captures exported from Wireshark/tshark and uses TestNG to assert that each
protocol behaved correctly: field values, packet ordering, and timing. The
tests can run in CI on every commit, so the same checks run the same way
each time.

It covers fundamentals (TCP, DNS, HTTP, ICMP) and applies the same approach
to protocol verification and regression testing used in larger networking
and telecom test environments:

- Reading packet captures and reasoning about protocol correctness
- Writing TestNG functional and regression test cases
- Structuring a Maven test project with fixtures and CI
- Producing execution reports that a reviewer can read

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

## Captures included

`src/test/resources/captures/*.json` contains one small capture export per
protocol, so `mvn test` runs immediately without Wireshark installed:

| File | Source |
|------|--------|
| `icmp_ping.json` | **Real capture** — `ping -n 1 8.8.8.8` recorded with Wireshark, exported with tshark |
| `dns_query.json` | **Real capture** — `nslookup example.com 8.8.8.8` recorded with Wireshark, exported with tshark |
| `tcp_handshake.json` | Hand-built sample in tshark's `-T json` format |
| `http_get.json` | Hand-built sample in tshark's `-T json` format |

In the real captures, MAC addresses, the local IP address and the capture
interface ID were replaced with placeholder values before committing. Packet
timing, protocol fields and public addresses are unmodified.

## Running the tests

```bash
mvn test
```

TestNG's HTML/XML report is written to `target/surefire-reports/`. CI
(`.github/workflows/ci.yml`) runs the same command on every push/PR and
uploads that report as a build artifact — this is the "participate in CI"
and "execution reports" part of the workflow.

## Capturing your own traffic

To reproduce or extend the captures above (or replace the two hand-built TCP
and HTTP samples with real ones), follow these steps:

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
