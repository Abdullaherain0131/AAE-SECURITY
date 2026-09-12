#!/usr/bin/env bash
# Tek komutla: 1) git ilk commit  2) doctor veri toplama
cd "$(dirname "$0")" || exit 1

echo "=== 1) GIT ILK COMMIT ==="
git add -A
git commit -m "İlk commit: AAE Security projesinin tam anlık görüntüsü" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
echo ""

echo "=== 2) DOCTOR VERI TOPLAMA ==="
bash .doctor-collect.sh
