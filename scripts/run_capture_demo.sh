#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -gt 5 ]; then
  echo "Usage: $0 [pcap_file] [hex_payload] [port] [interface] [send_timeout_seconds]"
  echo "Example (single command): $0"
  echo "Example (custom): $0 capture-12345.pcap 01022047 12345 lo0 1"
  exit 1
fi

DEFAULT_PCAP_FILE="capture-$(date +%Y%m%d-%H%M%S).pcap"
PCAP_FILE="${1:-$DEFAULT_PCAP_FILE}"
HEX_PAYLOAD="${2:-01022047}"
PORT="${3:-12345}"
IFACE="${4:-lo0}"
SEND_TIMEOUT="${5:-1}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_PID=""
TCPDUMP_PID=""

cleanup() {
  if [ -n "$TCPDUMP_PID" ] && kill -0 "$TCPDUMP_PID" 2>/dev/null; then
    kill "$TCPDUMP_PID" 2>/dev/null || true
    wait "$TCPDUMP_PID" 2>/dev/null || true
  fi

  if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

cd "$ROOT_DIR"

echo "[1/5] Compiling Java sources..."
javac -d out src/*.java

echo "[2/5] Starting receiver (PersonalTest)..."
java -cp out PersonalTest >/tmp/crn_personaltest.log 2>&1 &
APP_PID=$!
sleep 1

if ! kill -0 "$APP_PID" 2>/dev/null; then
  echo "Receiver failed to start. See /tmp/crn_personaltest.log"
  exit 1
fi

echo "[3/5] Starting tcpdump on interface '$IFACE', UDP port $PORT..."
sudo tcpdump -i "$IFACE" -nn "udp port $PORT" -w "$PCAP_FILE" >/tmp/crn_tcpdump.log 2>&1 &
TCPDUMP_PID=$!
sleep 1

if ! kill -0 "$TCPDUMP_PID" 2>/dev/null; then
  echo "tcpdump failed to start. See /tmp/crn_tcpdump.log"
  exit 1
fi

echo "[4/5] Sending packet ($HEX_PAYLOAD) to 127.0.0.1:$PORT..."
"$ROOT_DIR/scripts/send_udp_hex.sh" 127.0.0.1 "$PORT" "$HEX_PAYLOAD" "$SEND_TIMEOUT"
sleep 1

echo "[5/5] Stopping capture..."
kill "$TCPDUMP_PID" 2>/dev/null || true
wait "$TCPDUMP_PID" 2>/dev/null || true
TCPDUMP_PID=""

echo
echo "Done."
echo "pcap: $ROOT_DIR/$PCAP_FILE"
echo "receiver log: /tmp/crn_personaltest.log"
echo "tcpdump log: /tmp/crn_tcpdump.log"
echo "Inspect capture: tcpdump -nn -X -r \"$ROOT_DIR/$PCAP_FILE\""

echo "IF YOU HAVE WIRESHARK INSTALLED, PASTE THIS COMMAND FOR MORE DETAILED CAPTURE THREAD:"
echo "open -a YOUR_PATH/CRN/my-test.pcap"
