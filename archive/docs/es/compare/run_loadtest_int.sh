#!/bin/zsh
# INT 환경 부하 테스트 자동화
# Locust → Spring app (localhost:3000) → Remote OpenSearch
#
# 사용법:
#   chmod +x run_loadtest_int.sh
#   ./run_loadtest_int.sh [nofm|fm]

set -e

PHASE="${1:-nofm}"
LOCUSTFILE="archive/docs/es/compare/locustfile_agg.py"
SPRING_HOST="http://localhost:3000"
RESULT_DIR="archive/docs/es/compare/results/int"
USERS_LIST=(5 10 20 30 40 50)
SPAWN_RATE=10
DURATION="60s"
WARMUP_DURATION="5s"

# 테스트 대상 구조 (tag:api_path)
STRUCTURES=(
    "17x4_region:/api/es/lbt-17x4-region-fm/agg"
    "4x4:/api/es/lbt-4x4-fm/agg"
    "2x3:/api/es/lbt-2x3-fm/agg"
)

mkdir -p "$RESULT_DIR"

echo "=========================================="
echo " INT Load Test (phase: $PHASE)"
echo " Host: $SPRING_HOST"
echo " Users: ${USERS_LIST[*]}"
echo " Duration: $DURATION per test"
echo "=========================================="

for ENTRY in "${STRUCTURES[@]}"; do
    TAG="${ENTRY%%:*}"
    AGG_API="${ENTRY#*:}"
    echo ""
    echo "====== $TAG ($AGG_API) ======"

    # Warmup (1 user, 5초)
    echo "[warmup] $TAG ..."
    AGG_API="$AGG_API" python3 -m locust \
        -f "$LOCUSTFILE" \
        --host "$SPRING_HOST" \
        --headless -u 1 -r 1 -t "$WARMUP_DURATION" \
        > /dev/null 2>&1 || true
    echo "[warmup] done"

    for USERS in "${USERS_LIST[@]}"; do
        CSV_PREFIX="${RESULT_DIR}/int_${TAG}_${PHASE}_u${USERS}"
        echo "[test] $TAG $PHASE u=$USERS → $CSV_PREFIX"

        AGG_API="$AGG_API" python3 -m locust \
            -f "$LOCUSTFILE" \
            --host "$SPRING_HOST" \
            --headless -u "$USERS" -r "$SPAWN_RATE" -t "$DURATION" \
            --csv "$CSV_PREFIX" \
            2>&1 | tail -1

        echo "[done] $TAG $PHASE u=$USERS"
        sleep 2
    done
done

echo ""
echo "=========================================="
echo " All tests complete. Results: $RESULT_DIR"
echo "=========================================="
