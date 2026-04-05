#!/bin/bash
journalctl -u assetmind -n 80 --no-pager 2>&1 | grep -E "ERROR|Exception|Failed|Caused|started|Starting|Cannot|refused|denied" | tail -25
