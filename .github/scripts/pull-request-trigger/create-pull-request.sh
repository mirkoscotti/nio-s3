#!/bin/bash
set -e

PULL_REQUEST_BODY="## 🔗 Link to the issue
Resolves #${ISSUE_NUMBER}

## 📋 Description
${ISSUE_DETAILS}

Thanks to @${ISSUE_USER} for reporting this issue!

## ✅ Checklist
- [ ] Implementation completed
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] Code review required

---
*This PR was automatically created from issue [#${ISSUE_NUMBER}](${ISSUE_URL})*"

PULL_REQUEST_URL=$(gh pr create \
  --base "${BASE_BRANCH}" \
  --head "${BRANCH_NAME}" \
  --title "WIP: #${ISSUE_NUMBER} - ${ISSUE_TITLE}" \
  --body "${PULL_REQUEST_BODY}" \
  --draft)
echo "url=${PULL_REQUEST_URL}" >> "${GITHUB_OUTPUT}"