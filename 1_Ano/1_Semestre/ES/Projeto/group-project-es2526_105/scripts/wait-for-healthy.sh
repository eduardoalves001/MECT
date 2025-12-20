#!/bin/bash

# Script to wait for Docker container(s) to become healthy
# Usage: ./wait-for-healthy.sh <container_name1> [container_name2] ...

TIMEOUT=300  # Fixed timeout: 5 minutes (300 seconds)
INTERVAL=5
CONTAINERS=("$@")

# Check if at least one container is provided
if [ $# -eq 0 ]; then
    echo "❌ Error: At least one container name is required"
    echo "Usage: $0 <container_name1> [container_name2] ..."
    exit 1
fi

# Function to check a single container
check_container() {
    local CONTAINER_NAME=$1
    
    # Check if container exists
    if ! docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "❌ Container '$CONTAINER_NAME' does not exist"
        return 1
    fi
    
    # Check container state
    STATE=$(docker inspect "$CONTAINER_NAME" --format='{{.State.Status}}' 2>/dev/null || echo "unknown")
    
    if [ "$STATE" = "exited" ] || [ "$STATE" = "dead" ]; then
        echo "❌ Container '$CONTAINER_NAME' is in state: $STATE"
        echo ""
        echo "📋 Last 50 lines of logs:"
        docker logs --tail 50 "$CONTAINER_NAME" 2>&1 || echo "Failed to retrieve logs"
        echo ""
        return 1
    fi
    
    # Check health status (if health check is defined)
    HEALTH=$(docker inspect "$CONTAINER_NAME" --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' 2>/dev/null || echo "unknown")
    
    if [ "$HEALTH" = "healthy" ]; then
        return 0
    elif [ "$HEALTH" = "unhealthy" ]; then
        echo "❌ Container '$CONTAINER_NAME' is unhealthy"
        echo ""
        echo "📋 Last 50 lines of logs:"
        docker logs --tail 50 "$CONTAINER_NAME" 2>&1 || echo "Failed to retrieve logs"
        echo ""
        return 1
    elif [ "$HEALTH" = "no-healthcheck" ]; then
        # If no health check is defined, just check if running
        if [ "$STATE" = "running" ]; then
            return 0
        fi
    fi
    
    return 2  # Not ready yet
}

echo "⏳ Waiting for ${#CONTAINERS[@]} container(s) to become healthy..."
echo "   Containers: ${CONTAINERS[*]}"
echo "   Timeout: ${TIMEOUT}s | Check interval: ${INTERVAL}s"
echo ""

FAILED_CONTAINERS=()
HEALTHY_CONTAINERS=()

elapsed=0
while [ $elapsed -lt $TIMEOUT ]; do
    all_healthy=true
    
    for CONTAINER_NAME in "${CONTAINERS[@]}"; do
        # Skip if already marked as healthy
        if [[ " ${HEALTHY_CONTAINERS[*]} " =~ " ${CONTAINER_NAME} " ]]; then
            continue
        fi
        
        # Skip if already marked as failed
        if [[ " ${FAILED_CONTAINERS[*]} " =~ " ${CONTAINER_NAME} " ]]; then
            all_healthy=false
            continue
        fi
        
        check_container "$CONTAINER_NAME"
        result=$?
        
        if [ $result -eq 0 ]; then
            echo "✅ Container '$CONTAINER_NAME' is healthy!"
            HEALTHY_CONTAINERS+=("$CONTAINER_NAME")
        elif [ $result -eq 1 ]; then
            FAILED_CONTAINERS+=("$CONTAINER_NAME")
            all_healthy=false
        else
            # Not ready yet
            all_healthy=false
            STATE=$(docker inspect "$CONTAINER_NAME" --format='{{.State.Status}}' 2>/dev/null || echo "unknown")
            HEALTH=$(docker inspect "$CONTAINER_NAME" --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' 2>/dev/null || echo "unknown")
            echo "   $CONTAINER_NAME: Status=$STATE | Health=$HEALTH"
        fi
    done
    
    if [ "$all_healthy" = true ]; then
        echo ""
        echo "✅ All containers are healthy!"
        exit 0
    fi
    
    if [ ${#FAILED_CONTAINERS[@]} -gt 0 ]; then
        echo ""
        echo "❌ Some containers failed:"
        for failed in "${FAILED_CONTAINERS[@]}"; do
            echo "   - $failed"
        done
        exit 1
    fi
    
    sleep $INTERVAL
    elapsed=$((elapsed + INTERVAL))
    
    if [ $elapsed -lt $TIMEOUT ]; then
        echo "   Elapsed: ${elapsed}s / ${TIMEOUT}s"
    fi
done

echo ""
echo "❌ Timeout waiting for containers to become healthy (${TIMEOUT}s elapsed)"
echo "   Healthy containers: ${HEALTHY_CONTAINERS[*]:-none}"

# Show pending containers
PENDING_CONTAINERS=()
for container in "${CONTAINERS[@]}"; do
    if [[ ! " ${HEALTHY_CONTAINERS[*]} " =~ " ${container} " ]] && [[ ! " ${FAILED_CONTAINERS[*]} " =~ " ${container} " ]]; then
        PENDING_CONTAINERS+=("$container")
    fi
done

if [ ${#PENDING_CONTAINERS[@]} -gt 0 ]; then
    echo "   Pending: ${PENDING_CONTAINERS[*]}"
    echo ""
    echo "📋 Status and logs for pending containers:"
    for container in "${PENDING_CONTAINERS[@]}"; do
        STATE=$(docker inspect "$container" --format='{{.State.Status}}' 2>/dev/null || echo "unknown")
        HEALTH=$(docker inspect "$container" --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' 2>/dev/null || echo "unknown")
        echo ""
        echo "Container: $container"
        echo "  State: $STATE | Health: $HEALTH"
        echo "  Last 30 lines of logs:"
        docker logs --tail 30 "$container" 2>&1 || echo "  Failed to retrieve logs"
    done
fi

exit 1
