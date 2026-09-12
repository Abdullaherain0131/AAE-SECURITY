#!/usr/bin/env bash
# /doctor veri toplama scripti — SADECE OKUR, hiçbir ayar değiştirmez.
# Tüm çıktı makineye özgüdür; gizli anahtar/env değeri YazDIRMAZ (sadece isim/anahtar listeler).

C="\033[1;34m"; R="\033[0m"
sec() { echo -e "\n${C}=== $1 ===${R}"; }

sec "KURULUM (check 0/7)"
echo "PATH-claude: $(which -a claude 2>/dev/null | tr '\n' ' ')"
echo "resolved: $(readlink -f "$(which claude 2>/dev/null)" 2>/dev/null)"
echo "version: $(claude --version 2>/dev/null)"
echo "installMethod: $(jq -r '.installMethod // "unset"' "$HOME/.claude.json")"
echo "numStartups: $(jq -r '.numStartups // 0' "$HOME/.claude.json")"
echo "autoUpdates: $(jq -r '.autoUpdates // "unset"' "$HOME/.claude.json")"
[ -d "$HOME/.claude/local" ] && echo "npm-local-leftover: VAR" || echo "npm-local-leftover: yok"
NP=$(npm -g config get prefix 2>/dev/null); echo "npm-prefix: $NP"
[ -d "$NP/lib/node_modules/@anthropic-ai/claude-code" ] && echo "npm-global-install: VAR" || echo "npm-global-install: yok"
echo "local-bin-in-PATH: $(echo "$PATH" | grep -q "$HOME/.local/bin" && echo evet || echo hayir)"
echo "DISABLE_AUTOUPDATER: ${DISABLE_AUTOUPDATER:-unset}"
echo "DISABLE_UPDATES: ${DISABLE_UPDATES:-unset}"
echo "NONESSENTIAL_TRAFFIC_OFF: ${CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC:-unset}"

sec "AYAR DOSYALARI PARSE (check 0)"
for f in "$HOME/.claude/settings.json" ".claude/settings.json" ".claude/settings.local.json" ".mcp.json" "$HOME/.claude.json"; do
  if [ -f "$f" ]; then
    jq empty "$f" >/dev/null 2>&1 && echo "$f: OK" || echo "$f: BOZUK — $(jq empty "$f" 2>&1 | head -1)"
  else
    echo "$f: yok"
  fi
done

sec "AYAR ANAHTARLARI (check 8)"
for f in "$HOME/.claude/settings.json" ".claude/settings.json" ".claude/settings.local.json"; do
  if [ -f "$f" ]; then
    echo "--- $f"
    jq -r '"  defaultMode: " + (.permissions.defaultMode // "unset"),
           "  autoUpdatesChannel: " + (.autoUpdatesChannel // "unset"),
           "  hooks: " + ((.hooks // {}) | keys | join(", ")),
           "  allow-kural-sayisi: " + (((.permissions.allow // []) | length) | tostring),
           "  deny-kural-sayisi: " + (((.permissions.deny // []) | length) | tostring),
           "  skillOverrides: " + ((.skillOverrides // {}) | keys | join(", ")),
           "  enabledPlugins: " + ((.enabledPlugins // {}) | tostring)' "$f" 2>/dev/null
  else
    echo "--- $f: yok"
  fi
done

sec "MCP SUNUCULARI (check 1) — sadece isimler"
echo "user-scope: $(jq -c '.mcpServers // {} | keys' "$HOME/.claude.json")"
jq -r '.projects // {} | to_entries[]
       | select((.value.mcpServers // {}) | length > 0)
       | "local[\(.key)]: \(.value.mcpServers | keys | join(", "))"' "$HOME/.claude.json"
jq -r '.projects // {} | to_entries[]
       | select((.value.disabledMcpServers // []) | length > 0)
       | "disabled[\(.key)]: \(.value.disabledMcpServers | join(", "))"' "$HOME/.claude.json"
[ -f ".mcp.json" ] && echo "project-scope: $(jq -c '.mcpServers // {} | keys' .mcp.json)" || echo "project-scope: .mcp.json yok"

sec "KULLANIM SAYAÇLARI (check 1)"
jq -r '.skillUsage // {} | to_entries[] | "skill: \(.key) | toplam=\(.value.usageCount) | son=\(.value.lastUsedAt // "-")"' "$HOME/.claude.json"
jq -r '.pluginUsage // {} | to_entries[] | "plugin: \(.key) | toplam=\(.value.usageCount) | son=\(.value.lastUsedAt // "-")"' "$HOME/.claude.json"
[ -z "$(jq -r '.skillUsage // {} | keys | join("")' "$HOME/.claude.json")" ] && echo "(skillUsage boş)"
[ -z "$(jq -r '.pluginUsage // {} | keys | join("")' "$HOME/.claude.json")" ] && echo "(pluginUsage boş)"

sec "DİZİNLER (check 0/2/3)"
for d in "$HOME/.claude/agents" ".claude/agents" "$HOME/.claude/skills" ".claude/skills" "$HOME/.claude/plugins" ".claude/rules"; do
  if [ -d "$d" ]; then echo "$d: $(ls "$d" | tr '\n' ' ')"; else echo "$d: yok"; fi
done
echo "marketplaces: $(ls "$HOME/.claude/plugins/marketplaces" 2>/dev/null | tr '\n' ' ')"
echo "plugin-repos: $(ls "$HOME/.claude/plugins/repos" 2>/dev/null | tr '\n' ' ')"
echo "--- CLAUDE.md dosyaları (bayt):"
for f in "$HOME/.claude/CLAUDE.md" "CLAUDE.md" "CLAUDE.local.md" ".claude/CLAUDE.md"; do
  [ -f "$f" ] && wc -c "$f"
done
find . -maxdepth 4 -name "CLAUDE.md" -not -path "./app/*" 2>/dev/null

sec "AGENT/SKILL FRONTMATTER (check 0) — ilk 8 satır"
for f in "$HOME"/.claude/agents/*.md .claude/agents/*.md; do
  [ -f "$f" ] && echo "--- $f" && head -8 "$f"
done
for f in "$HOME"/.claude/skills/*/SKILL.md .claude/skills/*/SKILL.md; do
  [ -f "$f" ] && echo "--- $f" && head -8 "$f"
done

sec "TRANSKRIPT TARAMASI (check 1/5/9)"
TMP=$(mktemp -d)
FILES=$(ls -t "$HOME"/.claude/projects/*/*.jsonl 2>/dev/null | head -50)
N=$(echo "$FILES" | grep -c .)
echo "taranan dosya: $N"
echo "yeni: $(echo "$FILES" | head -1 | xargs -r stat -c '%y' 2>/dev/null)"
echo "eski: $(echo "$FILES" | tail -1 | xargs -r stat -c '%y' 2>/dev/null)"

# Tek geçiş: çağrılar + reddedilmeler
cat $FILES 2>/dev/null | jq -r '
  if .type=="assistant" then
    .message.content[]? | select(.type=="tool_use")
    | "CALL\t\(.id)\t\(.name)\t\((.input.command // .input.skill // .input.file_path // "" | tostring) | gsub("[\n\t\r]"; " ") | .[0:110])"
  elif .toolDenialKind != null then
    (.message.content[]? | select(.type=="tool_result") | .tool_use_id) as $id
    | "DENY\t\($id)\t\(.toolDenialKind)"
  else empty end' > "$TMP/events" 2>/dev/null

echo "--- araç adı sayıları (top 25):"
awk -F'\t' '$1=="CALL"{print $3}' "$TMP/events" | sort | uniq -c | sort -rn | head -25
echo "--- Skill çağrıları (input.skill):"
awk -F'\t' '$1=="CALL" && $3=="Skill"{print $4}' "$TMP/events" | sort | uniq -c | sort -rn | head -20
echo "--- slash komutları:"
grep -ho '<command-name>[^<]*</command-name>' $FILES 2>/dev/null | sort | uniq -c | sort -rn | head -15
echo "--- reddedilen çağrılar (tür + araç + komut başı):"
awk -F'\t' '$1=="CALL"{cmd[$2]=$3" :: "$4}
           $1=="DENY"{print $3"\t"cmd[$2]}' "$TMP/events" | sort | uniq -c | sort -rn | head -25
echo "--- hook süreleri (isim: adet ort max):"
cat $FILES 2>/dev/null | jq -r 'select(.type=="attachment") | .attachment
    | select((.type // "") | startswith("hook_"))
    | [(.hookName // "?"), (.hookEvent // "?"), (.durationMs // 0), .type, (.timedOut // false)] | @tsv' 2>/dev/null \
  | awk -F'\t' '{k=$1" ("$2")"; n[k]++; s[k]+=$3; if($3>m[k])m[k]=$3; t[k]=$4}
       END{for(x in n) printf "  %s: n=%d ort=%.0fms max=%dms son-tur=%s\n", x, n[x], s[x]/n[x], m[x], t[x]}' | sort
rm -rf "$TMP"
echo ""
echo "=== BITTI ==="
