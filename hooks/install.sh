#!/bin/sh
# Install git hooks for specflux-backend

HOOKS_DIR="$(dirname "$0")"
GIT_HOOKS_DIR="$(git rev-parse --git-dir)/hooks"

echo "Installing git hooks..."

cp "$HOOKS_DIR/pre-commit" "$GIT_HOOKS_DIR/pre-commit"
cp "$HOOKS_DIR/pre-push" "$GIT_HOOKS_DIR/pre-push"

chmod +x "$GIT_HOOKS_DIR/pre-commit"
chmod +x "$GIT_HOOKS_DIR/pre-push"

echo "✅ Git hooks installed successfully!"
echo ""
echo "Hooks installed:"
echo "  - pre-commit: Prevents direct commits to main"
echo "  - pre-push: Prevents direct pushes to main"
