#!/bin/bash
set -e

COMMENT="🚀 **Pull Request successfully created!**

📋 **Branch:** \`${BRANCH_NAME}\`
📋 **Base Branch:** \`${BASE_BRANCH}\`
🔄 **Pull Request:** Go to [Pull Requests](${PULL_REQUEST_URL}) to view the Pull Request created.

The branch is ready for development. Happy coding! 🎉"
gh issue comment "${ISSUE_NUMBER}" --body "${COMMENT}"