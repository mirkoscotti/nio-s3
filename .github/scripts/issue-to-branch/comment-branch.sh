#!/bin/bash
set -e

COMMENT="🚀 **Branch successfully created!**

📋 **Branch:** \`${BRANCH_NAME}\`

🛠️ The branch is ready for development."
gh issue comment ${ISSUE_NUMBER} --body "$COMMENT"