#!/bin/bash

# Flagsmith Complete Setup Script
# This script automatically configures Flagsmith for the RTMP application
# - Creates admin user
# - Creates environment if needed
# - Creates all feature flags
# - Updates .env file
# - Provides restart instructions

set -e

# Configuration
FLAGSMITH_URL="http://localhost:8098"
FLAGSMITH_ADMIN_EMAIL="${FLAGSMITH_ADMIN_EMAIL:-admin@example.com}"
FLAGSMITH_ADMIN_PASSWORD="${FLAGSMITH_ADMIN_PASSWORD:-admin123}"
ENVIRONMENT_NAME="Development"

echo "================================================"
echo "Flagsmith Complete Setup"
echo "================================================"
echo "Flagsmith URL: $FLAGSMITH_URL"
echo ""

# Function to wait for Flagsmith to be ready
wait_for_flagsmith() {
    echo "⏳ Waiting for Flagsmith to be ready..."
    MAX_RETRIES=30
    RETRY_COUNT=0
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if curl -s "$FLAGSMITH_URL/health" > /dev/null 2>&1; then
            echo "✅ Flagsmith is ready!"
            return 0
        fi
        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo "   Attempt $RETRY_COUNT/$MAX_RETRIES..."
        sleep 2
    done

    echo "❌ Flagsmith did not become ready in time"
    exit 1
}

# Function to check if admin user exists
check_admin_user_exists() {
    echo "👤 Checking if admin user exists..."
    USER_EXISTS=$(docker exec -i rtmp-flagsmith python manage.py shell << 'EOF' 2>&1
from django.contrib.auth import get_user_model
User = get_user_model()
try:
    user = User.objects.get(email='admin@example.com')
    print("EXISTS")
except User.DoesNotExist:
    print("NOT_EXISTS")
except Exception as e:
    print(f"ERROR:{e}")
EOF
)

    if echo "$USER_EXISTS" | grep -q "EXISTS"; then
        echo "✅ Admin user already exists - Flagsmith is already configured"
        echo ""
        return 0
    else
        return 1
    fi
}

# Function to create admin user
create_admin_user() {
    echo "👤 Creating admin user..."
    docker exec -i rtmp-flagsmith python manage.py shell << 'EOF' 2>&1 | grep -v "^$"
from django.contrib.auth import get_user_model
User = get_user_model()
try:
    user, created = User.objects.get_or_create(
        email='admin@example.com',
        defaults={'is_staff': True, 'is_superuser': True}
    )
    user.set_password('admin123')
    user.is_staff = True
    user.is_superuser = True
    user.save()
    print("✅ Admin user created successfully")
except Exception as e:
    print(f"⚠️  Error: {e}")
EOF
    echo ""
}

# Function to get authentication token
get_auth_token() {
    echo "🔐 Authenticating with Flagsmith..." >&2
    TOKEN=$(curl -s -X POST "$FLAGSMITH_URL/api/v1/auth/login/" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$FLAGSMITH_ADMIN_EMAIL\",\"password\":\"$FLAGSMITH_ADMIN_PASSWORD\"}" \
        | grep -o '"key":"[^"]*"' | cut -d'"' -f4)

    if [ -z "$TOKEN" ]; then
        echo "❌ Failed to authenticate. Please check credentials and ensure Flagsmith is running." >&2
        exit 1
    fi
    echo "✅ Authentication successful" >&2
    echo "$TOKEN"
}

# Function to get or create organisation
get_organisation() {
    local TOKEN=$1
    echo "🏢 Getting organisation..." >&2

    ORG_ID=$(curl -s -X GET "$FLAGSMITH_URL/api/v1/organisations/" \
        -H "Authorization: Token $TOKEN" \
        | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

    if [ -n "$ORG_ID" ]; then
        echo "✅ Organisation ID: $ORG_ID" >&2
    else
        echo "⚠️  No organisation found, creating one..." >&2
        ORG_RESPONSE=$(curl -s -X POST "$FLAGSMITH_URL/api/v1/organisations/" \
            -H "Authorization: Token $TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"name":"RTMP Organization"}')
        ORG_ID=$(echo "$ORG_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$ORG_ID" ]; then
            echo "✅ Created Organisation ID: $ORG_ID" >&2
        else
            echo "❌ Failed to create organisation" >&2
            exit 1
        fi
    fi

    echo "$ORG_ID"
}

# Function to get project
get_project() {
    local TOKEN=$1
    local ORG_ID=$2
    echo "📋 Getting project..." >&2

    PROJECT_ID=$(curl -s -X GET "$FLAGSMITH_URL/api/v1/projects/" \
        -H "Authorization: Token $TOKEN" \
        | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

    if [ -n "$PROJECT_ID" ]; then
        echo "✅ Project ID: $PROJECT_ID" >&2
    else
        echo "⚠️  No project found, creating one..." >&2
        PROJECT_RESPONSE=$(curl -s -X POST "$FLAGSMITH_URL/api/v1/projects/" \
            -H "Authorization: Token $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"name\":\"RTMP Project\",\"organisation\":$ORG_ID}")
        PROJECT_ID=$(echo "$PROJECT_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$PROJECT_ID" ]; then
            echo "✅ Created Project ID: $PROJECT_ID" >&2
        else
            echo "❌ Failed to create project" >&2
            exit 1
        fi
    fi

    echo "$PROJECT_ID"
}

# Function to get or create environment via Django
get_or_create_environment() {
    local PROJECT_ID=$1
    echo "🌍 Getting or creating environment '$ENVIRONMENT_NAME'..." >&2

    # Create environment via Django shell if it doesn't exist
    ENV_INFO=$(docker exec -i rtmp-flagsmith python manage.py shell << EOF
from environments.models import Environment
from projects.models import Project

try:
    project = Project.objects.get(id=$PROJECT_ID)
    env, created = Environment.objects.get_or_create(
        name="$ENVIRONMENT_NAME",
        project=project,
        defaults={'description': 'Development environment for RTMP'}
    )
    print(f"{env.id}|{env.api_key}")
except Exception as e:
    print(f"ERROR:{e}")
EOF
)

    if echo "$ENV_INFO" | grep -q "ERROR:"; then
        echo "❌ Failed to create environment" >&2
        echo "$ENV_INFO" >&2
        exit 1
    fi

    ENV_ID=$(echo "$ENV_INFO" | cut -d'|' -f1)
    ENV_KEY=$(echo "$ENV_INFO" | cut -d'|' -f2)

    if [ -n "$ENV_ID" ] && [ -n "$ENV_KEY" ]; then
        echo "✅ Environment ID: $ENV_ID" >&2
        echo "🔑 Environment Key: $ENV_KEY" >&2
    else
        echo "❌ Failed to get environment info" >&2
        exit 1
    fi

    echo "$ENV_ID|$ENV_KEY"
}

# Function to create or update a feature flag
create_or_update_feature() {
    local TOKEN=$1
    local PROJECT_ID=$2
    local FEATURE_NAME=$3
    local DESCRIPTION=$4
    local DEFAULT_ENABLED=$5

    echo "📝 Creating feature: $FEATURE_NAME"

    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$FLAGSMITH_URL/api/v1/projects/$PROJECT_ID/features/" \
        -H "Authorization: Token $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"$FEATURE_NAME\",\"description\":\"$DESCRIPTION\",\"initial_value\":\"$DEFAULT_ENABLED\",\"default_enabled\":$DEFAULT_ENABLED,\"type\":\"STANDARD\"}")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n-1)

    if [ "$HTTP_CODE" = "201" ]; then
        echo "   ✅ Created successfully - $([ "$DEFAULT_ENABLED" = "true" ] && echo "ENABLED ✅" || echo "DISABLED ❌")"
    elif echo "$BODY" | grep -q "already exists"; then
        echo "   ℹ️  Already exists - $([ "$DEFAULT_ENABLED" = "true" ] && echo "ENABLED ✅" || echo "DISABLED ❌")"
    else
        echo "   ⚠️  Unexpected response (HTTP $HTTP_CODE)"
    fi
}

# Function to update .env file with environment key
update_env_file() {
    local ENV_KEY=$1
    local ENV_FILE=".env"

    echo ""
    echo "📝 Updating .env file with environment key..."

    if [ -f "$ENV_FILE" ]; then
        if grep -q "^FLAGSMITH_ENVIRONMENT_KEY=" "$ENV_FILE"; then
            sed -i "s|^FLAGSMITH_ENVIRONMENT_KEY=.*|FLAGSMITH_ENVIRONMENT_KEY=$ENV_KEY|" "$ENV_FILE"
            echo "✅ Updated FLAGSMITH_ENVIRONMENT_KEY in .env"
        else
            echo "FLAGSMITH_ENVIRONMENT_KEY=$ENV_KEY" >> "$ENV_FILE"
            echo "✅ Added FLAGSMITH_ENVIRONMENT_KEY to .env"
        fi
    else
        echo "⚠️  .env file not found at $ENV_FILE"
        echo "   Please manually add: FLAGSMITH_ENVIRONMENT_KEY=$ENV_KEY"
    fi
}

# Main execution
main() {
    echo "Starting complete Flagsmith setup..."
    echo ""

    # Wait for Flagsmith to be ready
    wait_for_flagsmith

    # Check if admin user exists
    if check_admin_user_exists; then
        echo "================================================"
        echo "Admin user already exists - resetting password"
        echo "================================================"
        echo ""
        # Reset password for existing user
        create_admin_user
    else
        # Create admin user
        create_admin_user
    fi

    # Get authentication token
    TOKEN=$(get_auth_token)

    # Get organisation
    ORG_ID=$(get_organisation "$TOKEN")

    # Get project
    PROJECT_ID=$(get_project "$TOKEN" "$ORG_ID")

    # Get or create environment (via Django shell)
    ENV_INFO=$(get_or_create_environment "$PROJECT_ID")
    ENV_ID=$(echo "$ENV_INFO" | cut -d'|' -f1)
    ENV_KEY=$(echo "$ENV_INFO" | cut -d'|' -f2)

    echo ""
    echo "================================================"
    echo "Creating Feature Flags"
    echo "================================================"
    echo ""

    # Create feature flags
    # Note: Feature flags from previous releases have been removed
    # Only maintenance_mode remains as an active feature flag
    create_or_update_feature "$TOKEN" "$PROJECT_ID" \
        "maintenance_mode" \
        "Enable/disable maintenance mode" \
        "false"

    create_or_update_feature "$TOKEN" "$PROJECT_ID" \
        "enable_export_features" \
        "Toggle PDF/CSV export features for threat models" \
        "true"

    create_or_update_feature "$TOKEN" "$PROJECT_ID" \
        "enable_threat_model_search" \
        "Toggle search functionality in threat models page" \
        "true"

    create_or_update_feature "$TOKEN" "$PROJECT_ID" \
        "enable_threat_model_filtering" \
        "Toggle filtering options in threat models page" \
        "true"

    create_or_update_feature "$TOKEN" "$PROJECT_ID" \
        "enable_component_search" \
        "Toggle search functionality in components tab" \
        "true"

    create_or_update_feature "$TOKEN" "$PROJECT_ID" \
        "enable_vulnerability_filtering" \
        "Toggle filtering options in vulnerabilities tab" \
        "true"

    create_or_update_feature "$TOKEN" "$PROJECT_ID" \
        "enable_chatbot" \
        "Enable AI-powered chatbot assistant for threat modeling guidance" \
        "true"

    create_or_update_feature "$TOKEN" "$PROJECT_ID" \
        "comments" \
        "Enable threaded comments and notifications for vulnerabilities and components" \
        "true"

    # Update .env file with environment key
    update_env_file "$ENV_KEY"

    echo ""
    echo "================================================"
    echo "Flagsmith setup completed!"
    echo "================================================"
    echo ""
}

# Run main function
main
