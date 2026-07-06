#!/usr/bin/env bash
#
# sanitize-lint.sh — guard the public documentation against JPL-internal data.
#
# Scans the docs/ content tree for a denylist of JPL-specific patterns
# (internal hostnames, the CAM system, MOZART/PGE examples, internal ticket
# IDs, AWS account specifics, etc.). Exits non-zero if any match is found so
# CI fails before such content is ever published to GitHub Pages.
#
# Usage:
#   tools/docs/sanitize-lint.sh [DOCS_DIR]
#
# DOCS_DIR defaults to "docs". Only the content tree is scanned — this script
# and tools/docs/ (which necessarily name the denylisted terms) are excluded.
#
# To intentionally allow a specific line (rare, e.g. a sanitized illustrative
# example), append the marker:  <!-- sanitize-ok -->

set -euo pipefail

DOCS_DIR="${1:-docs}"

if [[ ! -d "$DOCS_DIR" ]]; then
  echo "sanitize-lint: docs directory '$DOCS_DIR' not found" >&2
  exit 2
fi

# Each entry: "<label>::<extended-regex>". Patterns are matched with grep -E.
# Keep patterns tight to avoid false positives:
#   - "jpl.nasa.gov" matches internal hosts but NOT the public "ammos.nasa.gov"
#     nor the reversed Java package coordinate "gov.nasa.jpl".
#   - "\bCAM\b" matches the CAM system but NOT "Camunda".
PATTERNS=(
  "internal JPL host/domain::[A-Za-z0-9._-]*\.jpl\.nasa\.gov"
  "internal 'cae-' host::\bcae-[A-Za-z0-9]"
  "CAM system reference::\bCAM\b"
  "MOZART example::[Mm][Oo][Zz][Aa][Rr][Tt]"
  "PGE reference::\bPGE\b"
  "internal ticket ID::\bIDS-[0-9]+"
  "AWS AMI ID::\bami-[0-9a-f]{6,}"
  "AWS IAM account ARN::arn:aws:iam::[0-9]+"
  "internal Artifactory::[Aa]rtifactory"
)

found=0
for entry in "${PATTERNS[@]}"; do
  label="${entry%%::*}"
  regex="${entry#*::}"
  # -I skips binaries; --exclude keeps allow-listed lines out.
  if matches="$(grep -rInE "$regex" "$DOCS_DIR" 2>/dev/null | grep -v 'sanitize-ok')"; then
    if [[ -n "$matches" ]]; then
      echo "✗ Denylisted content — ${label}:"
      echo "$matches" | sed 's/^/    /'
      echo
      found=1
    fi
  fi
done

if [[ "$found" -ne 0 ]]; then
  echo "sanitize-lint: FAILED — JPL-internal content found in $DOCS_DIR/." >&2
  echo "Remove or genericize the matches above before publishing." >&2
  exit 1
fi

echo "sanitize-lint: OK — no JPL-internal content found in $DOCS_DIR/."
