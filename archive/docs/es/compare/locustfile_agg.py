#!/usr/bin/env python3
"""
Spring Agg API 부하테스트 (locust)

Spring app의 /api/es/{tag}/agg/{level} 엔드포인트를 통한 E2E 부하 테스트.
Locust → Spring app (localhost:3000) → Remote OpenSearch

사용법:
  # headless (CLI only)
  AGG_API=/api/es/lbt-17x4-region-fm/agg python3 -m locust \
    -f archive/docs/es/compare/locustfile_agg.py \
    --host http://localhost:3000 \
    --headless -u 10 -r 10 -t 60s \
    --csv results/remote_17x4_fm_u10

환경변수:
  AGG_API: agg API base path (default: /api/es/lbt-17x4-region-fm/agg)
  AGG_LEVEL: sd / sgg / emd / all (default: all, round-robin)
"""

import os
import random
from datetime import date, timedelta

from locust import HttpUser, task, between

# ============================================================
# 설정
# ============================================================
AGG_API = os.environ.get("AGG_API", "/api/es/lbt-17x4-region-fm/agg")
AGG_LEVEL = os.environ.get("AGG_LEVEL", "all")
AGG_LEVELS = ["sd", "sgg", "emd"]

# 전국 bbox (sw=남서, ne=북동)
BBOX_BASE = {"swLat": 33.0, "swLng": 124.5, "neLat": 38.8, "neLng": 132.0}
BBOX_JITTER = 0.05

# ============================================================
# 필터 값 풀 (VS_FORCEMERGE_BENCH.py와 동일)
# ============================================================
JIYUK_CD1 = ["64", "71", "63", "62", "43", "14", "13", "44", "22", "81", "42", "15", "16", "41", "32", "33"]
JIMOK_CD = ["02", "08", "01", "05", "14", "18", "28", "17", "09", "03", "19", "16", "27", "04", "13"]
BUILDING_PURPOSE = ["단독주택", "제2종근린생활시설", "제1종근린생활시설", "공동주택", "창고시설",
                     "동물및식물관련시설", "공장", "노유자시설", "교육연구시설", "숙박시설", "업무시설"]
BUILDING_REGSTR = ["일반", "집합"]
TRADE_PROPERTY = ["APARTMENT", "LAND", "MULTI", "SINGLE", "OFFICETEL",
                   "SHOPPING_AND_OFFICE", "COMMERCIAL_BUILDING"]


def rand_subset(lst, min_n=1, max_n=None):
    max_n = max_n or len(lst)
    n = random.randint(min_n, min(max_n, len(lst)))
    return random.sample(lst, n)


def rand_date(start_year=2018, end_year=2025):
    start = date(start_year, 1, 1)
    end = date(end_year, 12, 31)
    delta = (end - start).days
    return start + timedelta(days=random.randint(0, delta))


def generate_params(combo_id):
    """랜덤 필터 조합을 Spring agg API query parameter dict로 생성."""
    j = BBOX_JITTER
    params = {
        "swLat": BBOX_BASE["swLat"] + random.uniform(-j, j),
        "swLng": BBOX_BASE["swLng"] + random.uniform(-j, j),
        "neLat": BBOX_BASE["neLat"] + random.uniform(-j, j),
        "neLng": BBOX_BASE["neLng"] + random.uniform(-j, j),
    }

    # agg level (round-robin or fixed)
    if AGG_LEVEL != "all" and AGG_LEVEL in AGG_LEVELS:
        level = AGG_LEVEL
    else:
        level = AGG_LEVELS[combo_id % len(AGG_LEVELS)]

    # 필터 카테고리 랜덤 선택 (최소 1개)
    categories = []
    if random.random() < 0.6:
        categories.append("land")
    if random.random() < 0.5:
        categories.append("building")
    if random.random() < 0.5:
        categories.append("trade")
    if not categories:
        categories.append(random.choice(["land", "building", "trade"]))

    # Land 필터
    if "land" in categories:
        if random.random() < 0.5:
            params["landJiyukCd1"] = ",".join(rand_subset(JIYUK_CD1, 1, 4))
        if random.random() < 0.4:
            params["landJimokCd"] = ",".join(rand_subset(JIMOK_CD, 1, 3))
        if random.random() < 0.4:
            mn = random.randint(50, 500)
            mx = mn + random.randint(100, 5000)
            params["landAreaMin"] = mn
            params["landAreaMax"] = mx
        if random.random() < 0.3:
            mn = random.randint(10000, 500000)
            mx = mn + random.randint(100000, 2000000)
            params["landPriceMin"] = mn
            params["landPriceMax"] = mx

    # Building 필터
    if "building" in categories:
        if random.random() < 0.6:
            params["buildingMainPurpsCdNm"] = ",".join(rand_subset(BUILDING_PURPOSE, 1, 3))
        if random.random() < 0.3:
            params["buildingRegstrGbCdNm"] = ",".join(rand_subset(BUILDING_REGSTR, 1, 2))
        if random.random() < 0.3:
            mn = random.randint(10, 200)
            mx = mn + random.randint(50, 3000)
            params["buildingTotAreaMin"] = mn
            params["buildingTotAreaMax"] = mx
        if random.random() < 0.25:
            start_year = random.randint(1990, 2015)
            params["buildingUseAprDayStart"] = start_year
            params["buildingUseAprDayEnd"] = random.randint(start_year, 2025)
        if random.random() < 0.15:
            params["buildingPmsDayRecent5y"] = "true"
        if random.random() < 0.15:
            params["buildingStcnsDayRecent5y"] = "true"

    # Trade 필터
    if "trade" in categories:
        if random.random() < 0.6:
            params["tradeProperty"] = ",".join(rand_subset(TRADE_PROPERTY, 1, 3))
        if random.random() < 0.5:
            ds = rand_date(2020, 2024)
            de = ds + timedelta(days=random.randint(30, 730))
            params["tradeContractDateStart"] = ds.isoformat()
            params["tradeContractDateEnd"] = de.isoformat()
        if random.random() < 0.3:
            mn = random.randint(10000, 500000) * 1000
            mx = mn + random.randint(100000, 3000000) * 1000
            params["tradeEffectiveAmountMin"] = mn
            params["tradeEffectiveAmountMax"] = mx
        if random.random() < 0.2:
            mn = random.randint(1, 5) * 1000000
            mx = mn + random.randint(1, 10) * 1000000
            params["tradeBuildingAmountPerM2Min"] = mn
            params["tradeBuildingAmountPerM2Max"] = mx

    return level, params


class AggApiUser(HttpUser):
    """Spring Agg API 부하 사용자."""
    wait_time = between(2, 5)

    def on_start(self):
        self._counter = random.randint(0, 99999)

    @task
    def agg_query(self):
        self._counter += 1
        level, params = generate_params(self._counter)

        self.client.get(
            f"{AGG_API}/{level}",
            params=params,
            name=f"{AGG_API}/{level}",
        )
