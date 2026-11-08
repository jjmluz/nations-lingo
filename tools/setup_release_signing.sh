#!/usr/bin/env bash
# Creates the release signing key and stores it in the GitHub repo's Actions secrets,
# so CI signs every release with the same key and installed copies can update in place.
#
# Run from the repo root, with the GitHub CLI logged in:
#   tools/setup_release_signing.sh
#
# The key and its password are kept in ~/.android/nations-lingo-release/. Back that folder up:
# if the key is lost, published APKs can never be updated; everyone would have to uninstall first.
#
# If the key already exists, it is reused and only the secrets are set again. With SKIP_SECRETS=1,
# the script only creates the key.
set -euo pipefail

REPO="${REPO:-jjmluz/nations-lingo}"
DIR="$HOME/.android/nations-lingo-release"
KEYSTORE="$DIR/nations-lingo-release.keystore"
ALIAS="nations-lingo"
KEYTOOL="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}/bin/keytool"

if [[ -e "$KEYSTORE" ]]; then
  echo "Reusing the existing key in $KEYSTORE."
  PASSWORD="$(sed -n 's/^Store and key password: //p' "$DIR/README.txt")"
else
  mkdir -p "$DIR"
  chmod 700 "$DIR"
  PASSWORD="$(openssl rand -base64 48 | tr -d '/+=\n' | cut -c1-32)"

  "$KEYTOOL" -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -alias "$ALIAS" \
    -keyalg RSA -keysize 4096 -validity 18250 \
    -storepass "$PASSWORD" -keypass "$PASSWORD" -dname "CN=Nations Lingo"

  cat > "$DIR/README.txt" <<EOF
Release signing key for the Nations Lingo Android TV game (github.com/$REPO).
Back this folder up, e.g. in a password manager. If it's lost, published APKs can
never be updated in place: everyone would have to uninstall and reinstall.

Keystore: nations-lingo-release.keystore (PKCS12)
Alias:    $ALIAS
Store and key password: $PASSWORD
EOF
  chmod 600 "$KEYSTORE" "$DIR/README.txt"
  echo "Created the key in $DIR; back that folder up."
fi

if [[ "${SKIP_SECRETS:-}" == 1 ]]; then
  echo "Skipping the GitHub secrets (SKIP_SECRETS=1)."
  exit 0
fi

base64 < "$KEYSTORE" | tr -d '\n' | gh secret set SIGNING_KEYSTORE_BASE64 --repo "$REPO"
printf %s "$PASSWORD" | gh secret set SIGNING_STORE_PASSWORD --repo "$REPO"
printf %s "$PASSWORD" | gh secret set SIGNING_KEY_PASSWORD --repo "$REPO"
printf %s "$ALIAS" | gh secret set SIGNING_KEY_ALIAS --repo "$REPO"

echo "Secrets set on $REPO."
gh secret list --repo "$REPO"
