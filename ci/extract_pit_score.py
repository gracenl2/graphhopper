#!/usr/bin/env python3
import re
import sys
from pathlib import Path

if len(sys.argv) != 2:
    print("Usage: extract_pit_score.py <path-to-index.html>", file=sys.stderr)
    sys.exit(1)

report_path = Path(sys.argv[1])
if not report_path.is_file():
    print(f"ERROR: report file not found: {report_path}", file=sys.stderr)
    sys.exit(1)

text = report_path.read_text(encoding="utf-8", errors="ignore")

# Find the "Project Summary" section
parts = text.split("<h3>Project Summary</h3>", 1)
if len(parts) < 2:
    print("ERROR: Could not find 'Project Summary' section in report", file=sys.stderr)
    sys.exit(1)
section = parts[1]

# First row of the summary table
m = re.search(r"<tbody>\s*<tr>(.*?)</tr>", section, re.S)
if not m:
    print("ERROR: Could not find summary row in report", file=sys.stderr)
    sys.exit(1)
row = m.group(1)

# Extract percentages: [line, mutation, test strength]
nums = re.findall(r"([0-9]+(?:\.[0-9]+)?)%", row)
if len(nums) < 2:
    print("ERROR: Could not find mutation coverage percentage", file=sys.stderr)
    sys.exit(1)

# Second percentage is mutation coverage
print(nums[1])
