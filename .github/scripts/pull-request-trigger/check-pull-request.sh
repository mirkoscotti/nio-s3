#!/bin/bash
set -e

if gh pr list --head "${BRANCH_NAME}" --json number | grep -q "number"; then
  echo "ℹ️ Pull Request already exists. Ignoring creation..."
  echo "pull_request_exists=true" >> "${GITHUB_OUTPUT}"
else
  echo "pull_request_exists=false" >> "${GITHUB_OUTPUT}"
fi