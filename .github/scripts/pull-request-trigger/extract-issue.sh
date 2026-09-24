#!/bin/bash
set -e

REPOSITORY_NAME="${REPOSITORY#*/}"
ISSUE_NUMBER="${BRANCH_NAME#$REPOSITORY_NAME-}"
if [[ "${ISSUE_NUMBER}" =~ ^[0-9]+$ ]]; then
echo "issue_number=${ISSUE_NUMBER}" >> "${GITHUB_OUTPUT}"
else
echo "⚠️ No valid issue number found. Skipping remaining steps..."
fi