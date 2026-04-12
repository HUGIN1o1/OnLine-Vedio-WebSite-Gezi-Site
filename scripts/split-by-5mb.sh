#!/usr/bin/env bash
# 按每块 5MiB 切分文件，分块文件名为纯 chunk1、chunk2、chunk3…（在子目录内）
# 使用 MD5：整文件 filehash + 每个分块的 filehash，写入同子目录清单
#
# 用法:
#   bash scripts/split-by-5mb.sh "文件路径"
#
# Git Bash 示例:
#   bash scripts/split-by-5mb.sh "/c/Users/Lenovo/Desktop/tes/76681-559745365_medium.mp4"
#
# 输出目录（与源文件同级的子目录）:
#   {原文件名无扩展}_split/chunk1
#   {原文件名无扩展}_split/chunk2
#   …
#   {原文件名无扩展}_split/chunks_manifest.txt

set -euo pipefail

CHUNK_BYTES=$((5 * 1024 * 1024))

md5_hex() {
  local f="$1"
  if command -v md5sum >/dev/null 2>&1; then
    md5sum "$f" | awk '{print $1}'
  elif command -v md5 >/dev/null 2>&1; then
    md5 -q "$f"
  else
    echo "未找到 md5sum 或 md5，请安装或改用 Git Bash" >&2
    exit 1
  fi
}

INPUT_FILE="${1:?用法: $0 <文件路径>}"

if [[ ! -f "$INPUT_FILE" ]]; then
  echo "文件不存在: $INPUT_FILE" >&2
  exit 1
fi

DIR=$(dirname "$INPUT_FILE")
BASE=$(basename "$INPUT_FILE")
STEM="${BASE%.*}"
[[ -z "$STEM" ]] && STEM="$BASE"

OUT_DIR="${DIR}/${STEM}_split"
mkdir -p "$OUT_DIR"

MANIFEST="${OUT_DIR}/chunks_manifest.txt"

echo "计算整文件 MD5…"
SOURCE_FULL_MD5=$(md5_hex "$INPUT_FILE")

{
  echo "source_file=${BASE}"
  echo "output_dir=$(basename "$OUT_DIR")"
  echo "source_full_filehash_md5=${SOURCE_FULL_MD5}"
  echo "chunk_size_bytes=${CHUNK_BYTES}"
  echo "--- chunks ---"
} > "$MANIFEST"

i=1
while true; do
  chunk_path="${OUT_DIR}/chunk${i}"
  rm -f "$chunk_path"
  set +e
  dd if="$INPUT_FILE" of="$chunk_path" bs="${CHUNK_BYTES}" skip=$((i - 1)) count=1 2>/dev/null
  set -e

  if [[ ! -f "$chunk_path" ]] || [[ ! -s "$chunk_path" ]]; then
    rm -f "$chunk_path"
    break
  fi

  chunk_hash=$(md5_hex "$chunk_path")
  echo "chunk${i} filehash_md5=${chunk_hash} path=chunk${i}" >> "$MANIFEST"

  i=$((i + 1))
done

echo "完成。"
echo "分块目录: ${OUT_DIR}/"
echo "  chunk1, chunk2, chunk3, …"
echo "清单: ${MANIFEST}"
