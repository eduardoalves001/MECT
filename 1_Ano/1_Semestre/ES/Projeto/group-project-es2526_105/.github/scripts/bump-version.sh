#!/usr/bin/env bash
set -euo pipefail

# Usage: bump-version.sh [major|minor|patch]
BUMP="${1:-patch}"

# Make sure tags are fetched
git fetch --tags --prune

# Find latest semver tag like v1.2.3
latest_tag=$(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -n1 || true)

if [ -z "$latest_tag" ]; then
  MAJOR=0
  MINOR=0
  PATCH=0
else
  ver=${latest_tag#v}
  IFS='.' read -r MAJOR MINOR PATCH <<< "$ver"
fi

case "$BUMP" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  patch)
    PATCH=$((PATCH + 1))
    ;;
  *)
    echo "Invalid bump '${BUMP}'. Use 'major', 'minor' or 'patch'."
    exit 1
    ;;
esac

new_tag="v${MAJOR}.${MINOR}.${PATCH}"
echo "$new_tag"
