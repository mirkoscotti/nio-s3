#!/bin/bash
set -e

RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${GITHUB_TOKEN}" \
            -H "Accept: application/vnd.github+json" \
            -H "X-GitHub-Api-Version: 2022-11-28" \
            "https://api.github.com/repos/${REPOSITORY}/issues/${ISSUE_NUMBER}/parent")
HTTP_CODE=$(echo "${RESPONSE}" | tail -n 1)
BODY=$(echo "${RESPONSE}" | sed '$d')
if [[ "${HTTP_CODE}" -eq 200 ]]; then
  PARENT_ISSUE_NUMBER=$(echo "${BODY}" | jq -r .number)
  REPOSITORY_NAME="${REPOSITORY#*/}"
  BASE_BRANCH_NAME="${REPOSITORY_NAME}-${PARENT_ISSUE_NUMBER}"
echo "name=${BASE_BRANCH_NAME}" >> "${GITHUB_OUTPUT}"
else
  echo "ℹ️ No parent issue found (API status: ${HTTP_CODE}). Defaulting base branch to 'main'..."
  echo "name=main" >> "${GITHUB_OUTPUT}"
fi