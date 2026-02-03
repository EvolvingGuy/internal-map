#!/bin/bash
# 부하테스트 자동화 (6구조 × 6유저레벨 = 36회)
# 지도 기반 웹 서비스 설정: wait_time 2~5s, spawn_rate 10/s, duration 60s

set -e
cd /Users/sanghoonseo/Privates/geo_poc

LOCUSTFILE="archive/docs/es/compare/locustfile.py"
RESULTS_DIR="archive/docs/es/compare/results"
USER_LEVELS="5 10 20 30 40 50"
SPAWN_RATE=10
DURATION="60s"

# 컨테이너별 설정: compose_file index_pattern tag
declare -a TARGETS=(
  "docker-compose.lbt1x16fm.yml|lbt_1x16_fm|lbt_1x16_fm"
  "docker-compose.lbt1x16nofm.yml|lbt_1x16_nofm|lbt_1x16_nofm"
  "docker-compose.lbt2x3fm.yml|lbt_2x3_fm_*|lbt_2x3_fm"
  "docker-compose.lbt2x3nofm.yml|lbt_2x3_nofm_*|lbt_2x3_nofm"
  "docker-compose.lbt17x4regionfm.yml|lbt_17x4_region_fm_*|lbt_17x4_region_fm"
  "docker-compose.lbt17x4regionnofm.yml|lbt_17x4_region_nofm_*|lbt_17x4_region_nofm"
)

echo "=========================================="
echo " Load Test: 6 structures × 6 user levels"
echo " spawn_rate=${SPAWN_RATE}, duration=${DURATION}"
echo " wait_time=2~5s (map service)"
echo "=========================================="

TOTAL=${#TARGETS[@]}
IDX=0

for TARGET in "${TARGETS[@]}"; do
  IFS='|' read -r COMPOSE INDEX TAG <<< "$TARGET"
  IDX=$((IDX + 1))

  echo ""
  echo "[$IDX/$TOTAL] ===== $TAG ====="
  echo "  compose: $COMPOSE"
  echo "  index:   $INDEX"

  # 컨테이너 시작
  echo "  Starting container..."
  docker compose -f "$COMPOSE" up -d 2>&1 | tail -1

  # health 대기
  echo "  Waiting for health..."
  for i in $(seq 1 60); do
    STATUS=$(curl -s "http://localhost:9200/_cluster/health" -u admin:admin 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null || echo "")
    if [ "$STATUS" = "green" ] || [ "$STATUS" = "yellow" ]; then
      echo "  Cluster ready: $STATUS"
      break
    fi
    sleep 2
  done

  # 웜업 (5초, 1유저)
  echo "  Warmup (5s, 1 user)..."
  ES_INDEX="$INDEX" python3 -m locust -f "$LOCUSTFILE" \
    --host http://localhost:9200 \
    --headless -u 1 -r 1 -t 5s \
    --skip-log 2>/dev/null || true

  # 유저 레벨별 테스트
  for USERS in $USER_LEVELS; do
    CSV_PREFIX="${RESULTS_DIR}/${TAG}_u${USERS}"
    echo "  [u=$USERS] Running ${DURATION}..."

    ES_INDEX="$INDEX" python3 -m locust -f "$LOCUSTFILE" \
      --host http://localhost:9200 \
      --headless -u "$USERS" -r "$SPAWN_RATE" -t "$DURATION" \
      --csv "$CSV_PREFIX" \
      --skip-log 2>&1 | tail -3

    echo "  [u=$USERS] Done → ${CSV_PREFIX}_stats.csv"
  done

  # 컨테이너 종료
  echo "  Stopping container..."
  docker compose -f "$COMPOSE" down 2>&1 | tail -1
  echo "  ===== $TAG complete ====="
done

echo ""
echo "=========================================="
echo " All tests complete!"
echo " Results: $RESULTS_DIR/"
echo "=========================================="
