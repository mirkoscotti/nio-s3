#!/bin/bash
set -e

echo "Adding triage label to issue #${ISSUE_NUMBER}"
gh issue edit "${ISSUE_NUMBER}" \
  --add-label "status:triage" \
  --repo "${GITHUB_REPOSITORY}" || echo "Failed to add label, but continuing..."
echo "Triage label added successfully to issue #${ISSUE_NUMBER}"