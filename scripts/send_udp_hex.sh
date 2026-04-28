#!/usr/bin/env bash
set -euo pipefail

# lt = less than, and gt = greater than, $# = number of arguments passed
if [ "$#" -lt 3 ] || [ "$#" -gt 4 ]; then
  # print equivalent of bash shell
  echo "Usage: $0 <host> <port> <hex_payload_no_spaces> [timeout_seconds]"
  echo "Example: $0 127.0.0.1 12345 01022047"
  exit 1
fi

HOST="$1"
PORT="$2"
HEX_PAYLOAD="$3"
TIMEOUT="${4:-1}"

# =~ is the bash regex-match operator, [0-9] = a digit, + = one or more of the previous token, $ = end of the string, ^ = beginning of the string
if ! [[ "$PORT" =~ ^[0-9]+$ ]]; then
  echo "Port must be numeric"
  exit 1
fi

# [[ ... ]] is Bash’s advanced test syntax.
if ! [[ "$HEX_PAYLOAD" =~ ^[0-9A-Fa-f]+$ ]]; then
  echo "Hex payload must contain only 0-9a-fA-F"
  exit 1
fi

# (( ... )) is Bash's arithmetic context, you can perform operations such as: %, !=, +, -
if (( ${#HEX_PAYLOAD} % 2 != 0 )); then
  echo "Hex payload length must be even"
  exit 1
fi

# xxd converts between hex and binary, -p expects plain hex input, -r reverse mode(hex text -> raw bytes)
# 01022047 becomes 01 02 20 47
set +e
printf '%s' "$HEX_PAYLOAD" | xxd -r -p | nc -u -w "$TIMEOUT" "$HOST" "$PORT" | tee /tmp/crn_response.bin >/dev/null
NC_STATUS=$?
set -e

if [ ! -s /tmp/crn_response.bin ]; then
  echo "[no response]"
  # nc often returns non-zero on timeout/no-response for UDP; treat this as non-fatal.
  exit 0
fi

if [ "$NC_STATUS" -ne 0 ]; then
  echo "[warning] nc returned status $NC_STATUS but response bytes were captured"
fi

echo "--- response hex ---"

# -c number of bytes per output line
xxd -p -c 9999 /tmp/crn_response.bin

echo "--- response bytes ---"
# Canonical hex+ASCII view.
hexdump -C /tmp/crn_response.bin

echo "--- response text (lossy) ---"
LC_ALL=C tr -d '\000' < /tmp/crn_response.bin || true
