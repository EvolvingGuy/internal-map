#!/usr/bin/env python3
"""
OpenSearch 부하테스트 (locust)

사용법:
  # 웹 UI (http://localhost:8089)
  locust -f locustfile.py --host http://localhost:9200

  # headless (CLI only)
  locust -f locustfile.py --host http://localhost:9200 \
    --headless -u 10 -r 5 -t 60s \
    --csv results/4_4_local_f

  # 단일 쿼리 레이턴시 (users=1)
  locust -f locustfile.py --host http://localhost:9200 \
    --headless -u 1 -r 1 -t 60s

환경변수:
  ES_INDEX: 대상 인덱스 패턴 (default: lnbtpu_*)
  AGG_LEVEL: sd / sgg / emd / all (default: all, round-robin)
"""

import os
import sys
import random
import json

from requests.auth import HTTPBasicAuth
from locust import HttpUser, task, events, between

# VS_FORCEMERGE_BENCH.py에서 쿼리 생성 로직 재사용
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from VS_FORCEMERGE_BENCH import generate_filter_combo, combo_to_query

INDEX_PATTERN = os.environ.get("ES_INDEX", "lnbtpu_*")
AGG_LEVEL = os.environ.get("AGG_LEVEL", "all")  # sd / sgg / emd / all

AGG_LEVELS_MAP = {
    "sd":  {"field": "sd", "size": 100},
    "sgg": {"field": "sgg", "size": 10000},
    "emd": {"field": "emd", "size": 100000},
}


class ESAggUser(HttpUser):
    """OpenSearch 집계 쿼리 부하 사용자."""
    wait_time = between(2, 5)  # 지도 서비스: 유저가 지도 이동 후 결과 확인 → 2~5초 간격

    def on_start(self):
        self._counter = random.randint(0, 99999)
        self.client.auth = HTTPBasicAuth("admin", "admin")

    @task
    def agg_query(self):
        self._counter += 1
        combo = generate_filter_combo(self._counter)

        # agg level 오버라이드
        if AGG_LEVEL != "all" and AGG_LEVEL in AGG_LEVELS_MAP:
            combo["agg"] = AGG_LEVELS_MAP[AGG_LEVEL]

        query = combo_to_query(combo)
        agg_field = combo["agg"]["field"]

        self.client.post(
            f"/{INDEX_PATTERN}/_search?request_cache=false",
            json=query,
            name=f"/{INDEX_PATTERN}/_search [{agg_field}]",
        )
