#!/bin/bash

# Keycloak RBAC Setup Script - REALM ROLES Version
# This script sets up the Role-Based Access Control (RBAC) structure in Keycloak

set -e

SETUP_USERS=false
PROD_MODE=false
REALM="rtmp"
ADMIN_USER="admin"
ADMIN_PASS="admin"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --setup-users)
            SETUP_USERS=true
            shift
            ;;
        --prod)
            PROD_MODE=true
            shift
            ;;
        *)
            FRONTEND_URL="$1"
            shift
            ;;
    esac
done

if [ -z "$FRONTEND_URL" ]; then
    echo "❌ Error: FRONTEND_URL is required"
    echo "Usage: $0 [--setup-users] [--prod] <FRONTEND_URL>"
    echo "Example: $0 http://localhost:8090"
    echo "Example: $0 --setup-users http://localhost:8090"
    echo "Example: $0 --setup-users --prod https://domain.com"
    echo "Example: $0 --prod --setup-users https://domain.com"
    exit 1
fi

# Set Keycloak server URL based on mode
if [ "$PROD_MODE" = true ]; then
    KEYCLOAK_SERVER="http://localhost:8080/auth"
else
    KEYCLOAK_SERVER="http://localhost:8080"
fi

# Find running Keycloak container
KEYCLOAK_CONTAINER=$(docker ps --filter "name=rtmp-keycloak" --format "{{.Names}}" | head -1)

if [ -z "$KEYCLOAK_CONTAINER" ]; then
    echo "❌ Error: No running Keycloak container found"
    exit 1
fi

echo "🔐 Setting up Keycloak (RBAC + Theme)..."
echo "📍 Frontend URL: ${FRONTEND_URL}"
echo "🐳 Using container: ${KEYCLOAK_CONTAINER}"
echo "🌐 Server URL: ${KEYCLOAK_SERVER}"
if [ "$SETUP_USERS" = true ]; then
    echo "👥 User creation: ENABLED"
fi
if [ "$PROD_MODE" = true ]; then
    echo "🚀 Production mode: ENABLED"
fi
echo ""

# Wait for Keycloak to be ready
echo "⏳ Waiting for Keycloak to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec $KEYCLOAK_CONTAINER bash -c "timeout 2 bash -c ':> /dev/tcp/127.0.0.1/8080' 2>/dev/null"; then
        echo "✅ Keycloak is ready!"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "   Attempt $RETRY_COUNT/$MAX_RETRIES..."
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ Keycloak did not become ready in time"
    exit 1
fi
echo ""

# Login to Keycloak
echo "1️⃣ Logging into Keycloak..."
docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh config credentials \
  --server "$KEYCLOAK_SERVER" \
  --realm master \
  --user "$ADMIN_USER" \
  --password "$ADMIN_PASS"
echo "✅ Logged in successfully"
echo ""

# Check if realm exists
echo "2️⃣ Checking if realm '$REALM' exists..."
if docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get realms/"$REALM" &>/dev/null; then
    echo "✅ Realm '$REALM' already exists - skipping RBAC setup"
    echo ""
else
    echo "✅ Realm does not exist - proceeding with RBAC setup"
    echo ""

    # Create RTMP Realm
    echo "3️⃣ Creating '$REALM' realm..."
    docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh create realms \
      -s realm="$REALM" \
      -s enabled=true \
      -s registrationAllowed=false \
      -s registrationEmailAsUsername=false \
      -s rememberMe=true \
      -s verifyEmail=false \
      -s loginWithEmailAllowed=true \
      -s duplicateEmailsAllowed=false
    echo "✅ Realm '$REALM' created"
    echo ""

    # Create Client
    echo "4️⃣ Creating client 'rtmp-client'..."
    docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh create clients \
      -r "$REALM" \
      -s clientId=rtmp-client \
      -s enabled=true \
      -s publicClient=true \
      -s directAccessGrantsEnabled=true \
      -s "redirectUris=[\"${FRONTEND_URL}/*\"]" \
      -s "webOrigins=[\"${FRONTEND_URL}\"]" \
      -s standardFlowEnabled=true \
      -s implicitFlowEnabled=false \
      -s serviceAccountsEnabled=false
    echo "✅ Client 'rtmp-client' configured with redirect URI: ${FRONTEND_URL}/*"
    echo ""

    # Create Realm Roles
    echo "5️⃣ Creating Realm Roles..."
    ROLES=(
      "threatmodel:create" "threatmodel:read" "threatmodel:update" "threatmodel:delete"
      "component:create" "component:read" "component:update" "component:delete"
      "vulnerability:create" "vulnerability:read" "vulnerability:update" "vulnerability:delete"
      "threat:create" "threat:read"
      "comment:create" "comment:read" "comment:delete"
      "chatbot:use"
    )

    for role in "${ROLES[@]}"; do
      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh create roles \
        -r "$REALM" \
        -s name="$role" \
        -s 'description='"$role"' permission'
    done
    echo "✅ 18 Realm Roles created"
    echo ""

    # Create Groups
    echo "6️⃣ Creating Groups..."
    KEYCLOAK_GROUPS=(
      "Security Architect"
      "Software Developer"
      "Project Manager"
      "Auditor"
    )

    for group in "${KEYCLOAK_GROUPS[@]}"; do
      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh create groups \
        -r "$REALM" \
        -s name="$group"
    done
    echo "✅ 4 Groups created"
    echo ""

    # Assign Roles to Groups
    echo "7️⃣ Assigning Roles to Groups..."

    # Security Architect - Full access
    GROUP_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get groups \
      -r "$REALM" \
      -q search="Security Architect" \
      --fields id \
      | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

    for role in "${ROLES[@]}"; do
      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh add-roles \
        -r "$REALM" \
        --gid "$GROUP_ID" \
        --rolename "$role"
    done
    echo "  ✅ Security Architect: 18 roles"

    # Software Developer
    GROUP_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get groups \
      -r "$REALM" \
      -q search="Software Developer" \
      --fields id \
      | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

    DEV_ROLES=(
      "threatmodel:create" "threatmodel:read" "threatmodel:update" "threatmodel:delete"
      "component:read" "component:update"
      "vulnerability:create" "vulnerability:read" "vulnerability:update" "vulnerability:delete"
      "threat:read"
      "comment:create" "comment:read" "comment:delete"
      "chatbot:use"
    )

    for role in "${DEV_ROLES[@]}"; do
      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh add-roles \
        -r "$REALM" \
        --gid "$GROUP_ID" \
        --rolename "$role"
    done
    echo "  ✅ Software Developer: 15 roles"

    # Project Manager & Auditor - Read-only
    for GROUP_NAME in "Project Manager" "Auditor"; do
      GROUP_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get groups \
        -r "$REALM" \
        -q search="$GROUP_NAME" \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      READ_ROLES=("threatmodel:read" "component:read" "vulnerability:read" "threat:read" "comment:read" "chatbot:use")
      for role in "${READ_ROLES[@]}"; do
        docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh add-roles \
          -r "$REALM" \
          --gid "$GROUP_ID" \
          --rolename "$role"
      done
      echo "  ✅ $GROUP_NAME: 6 roles"
    done
    echo ""

    # Create Test Users (if flag is set)
    if [ "$SETUP_USERS" = true ]; then
      echo "8️⃣ Creating Test Users..."

      # Security Architect User
      echo "  Creating user: architect@rtmp.com"
      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh create users \
        -r "$REALM" \
        -s username=architect@rtmp.com \
        -s email=architect@rtmp.com \
        -s firstName=Security \
        -s lastName=Architect \
        -s enabled=true \
        -s emailVerified=true

      USER_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get users \
        -r "$REALM" \
        -q username=architect@rtmp.com \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh set-password \
        -r "$REALM" \
        --userid "$USER_ID" \
        --new-password "Architect@2024"

      GROUP_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get groups \
        -r "$REALM" \
        -q search="Security Architect" \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh update users/"$USER_ID"/groups/"$GROUP_ID" \
        -r "$REALM" \
        -s realm="$REALM" \
        -s userId="$USER_ID" \
        -s groupId="$GROUP_ID" \
        -n

      echo "    ✅ architect@rtmp.com (password: Architect@2024)"

      # Software Developer User
      echo "  Creating user: developer@rtmp.com"
      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh create users \
        -r "$REALM" \
        -s username=developer@rtmp.com \
        -s email=developer@rtmp.com \
        -s firstName=Software \
        -s lastName=Developer \
        -s enabled=true \
        -s emailVerified=true

      USER_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get users \
        -r "$REALM" \
        -q username=developer@rtmp.com \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh set-password \
        -r "$REALM" \
        --userid "$USER_ID" \
        --new-password "Developer@2024"

      GROUP_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get groups \
        -r "$REALM" \
        -q search="Software Developer" \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh update users/"$USER_ID"/groups/"$GROUP_ID" \
        -r "$REALM" \
        -s realm="$REALM" \
        -s userId="$USER_ID" \
        -s groupId="$GROUP_ID" \
        -n

      echo "    ✅ developer@rtmp.com (password: Developer@2024)"

      # Project Manager User
      echo "  Creating user: manager@rtmp.com"
      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh create users \
        -r "$REALM" \
        -s username=manager@rtmp.com \
        -s email=manager@rtmp.com \
        -s firstName=Project \
        -s lastName=Manager \
        -s enabled=true \
        -s emailVerified=true

      USER_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get users \
        -r "$REALM" \
        -q username=manager@rtmp.com \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh set-password \
        -r "$REALM" \
        --userid "$USER_ID" \
        --new-password "Manager@2024"

      GROUP_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get groups \
        -r "$REALM" \
        -q search="Project Manager" \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh update users/"$USER_ID"/groups/"$GROUP_ID" \
        -r "$REALM" \
        -s realm="$REALM" \
        -s userId="$USER_ID" \
        -s groupId="$GROUP_ID" \
        -n

      echo "    ✅ manager@rtmp.com (password: Manager@2024)"

      # Auditor User
      echo "  Creating user: auditor@rtmp.com"
      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh create users \
        -r "$REALM" \
        -s username=auditor@rtmp.com \
        -s email=auditor@rtmp.com \
        -s firstName=Security \
        -s lastName=Auditor \
        -s enabled=true \
        -s emailVerified=true

      USER_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get users \
        -r "$REALM" \
        -q username=auditor@rtmp.com \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh set-password \
        -r "$REALM" \
        --userid "$USER_ID" \
        --new-password "Auditor@2024"

      GROUP_ID=$(docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh get groups \
        -r "$REALM" \
        -q search="Auditor" \
        --fields id \
        | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)

      docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh update users/"$USER_ID"/groups/"$GROUP_ID" \
        -r "$REALM" \
        -s realm="$REALM" \
        -s userId="$USER_ID" \
        -s groupId="$GROUP_ID" \
        -n

      echo "    ✅ auditor@rtmp.com (password: Auditor@2024)"
      echo ""
      echo "✅ 4 Test Users created and assigned to groups"
      echo ""
    fi
fi

# Configure Theme
echo "Configuring RTMP theme..."
docker exec $KEYCLOAK_CONTAINER /opt/keycloak/bin/kcadm.sh update realms/"$REALM" \
  -s loginTheme=rtmp
echo "✅ Theme 'rtmp' applied to realm"
echo ""

echo "✅ Keycloak Setup Complete!"
echo ""
