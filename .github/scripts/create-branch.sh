#!/bin/bash
set -e

# Extract repository name
REPOSITORY_NAME=$(echo "$REPOSITORY" | cut -d'/' -f2)
echo "Repository name: $REPOSITORY_NAME"

# Create branch name
BRANCH_NAME="${REPOSITORY_NAME}-${ISSUE_NUMBER}"
echo "Branch name: $BRANCH_NAME"

# Check if branch already exists
git fetch origin
if git ls-remote --heads origin "$BRANCH_NAME" | grep -q "$BRANCH_NAME"; then
  echo "Branch $BRANCH_NAME already exists on remote. Skipping."
  echo "branch_created=false" >> $GITHUB_OUTPUT
  exit 0
fi

# Check if a parent issue exists
PARENT_RESPONSE=$(curl -s -w "\n%{http_code}" \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${REPOSITORY}/issues/${ISSUE_NUMBER}/parent")

# Save the output  
HTTP_CODE=$(echo "$PARENT_RESPONSE" | tail -n1)
PARENT_DATA=$(echo "$PARENT_RESPONSE" | sed '$d')

# Check the output
if [ "$HTTP_CODE" = "200" ]; then
  PARENT_NUMBER=$(echo "$PARENT_DATA" | jq -r '.number // empty')
  if [ -n "$PARENT_NUMBER" ]; then
    PARENT_TITLE=$(echo "$PARENT_DATA" | jq -r '.title')
    BASE_BRANCH="${REPOSITORY_NAME}-${PARENT_NUMBER}"
    echo "✓ Found parent issue: #${PARENT_NUMBER} - ${PARENT_TITLE}"
  else
    BASE_BRANCH="main"
    echo "⚠️ API returned 200 but no parent data found"
  fi
  echo "✓ Will create branch from: ${BASE_BRANCH}"
elif [ "$HTTP_CODE" = "404" ]; then
  BASE_BRANCH="main"
  echo "ℹ️ No parent issue found (this is a top-level issue)"
  echo "ℹ️ Will create branch from: ${BASE_BRANCH}"
else
  echo "❌ API returned unexpected status code ${HTTP_CODE}"
  echo "Response body:"
  echo "$PARENT_DATA"
  echo "branch_created=false" >> $GITHUB_OUTPUT
  exit 1
fi

# Check if parent branch already exists
git fetch origin
if ! git ls-remote --heads origin "$BASE_BRANCH" | grep -q "$BASE_BRANCH"; then
  echo "❌ Base branch ${BASE_BRANCH} does not exist!"
  echo "branch_created=false" >> $GITHUB_OUTPUT
  exit 1
fi

# Create branch
echo "Creating new branch: $BRANCH_NAME" from origin/${BASE_BRANCH}
git checkout -b "${BRANCH_NAME}" "origin/${BASE_BRANCH}"
git push origin $BRANCH_NAME
echo "Branch $BRANCH_NAME created and pushed successfully."

# Create output
echo "branch_created=true" >> $GITHUB_OUTPUT
echo "branch_name=${BRANCH_NAME}" >> $GITHUB_OUTPUT
echo "base_branch=${BASE_BRANCH}" >> $GITHUB_OUTPUT