#!/bin/bash
set -e

ISSUE_JSON=$(gh issue view ${ISSUE_NUMBER} --json title,body,author,url)
echo "DEBUG JSON: ${ISSUE_JSON}"
echo "issue_url=$(echo "${ISSUE_JSON}" | jq -r .url)" >> "${GITHUB_OUTPUT}"
echo "issue_user=$(echo "${ISSUE_JSON}" | jq -r .author.login)" >> "${GITHUB_OUTPUT}"
echo "issue_title=$(echo "${ISSUE_JSON}" | jq -r .title)" >> "${GITHUB_OUTPUT}"
{
  echo "issue_body<<EOF"
  echo "${ISSUE_JSON}" | jq -r .body
  echo "EOF"
} >> "${GITHUB_OUTPUT}"