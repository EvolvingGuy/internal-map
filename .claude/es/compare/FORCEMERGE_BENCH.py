#!/usr/bin/env python3
"""
forcemerge 전/후 벤치마크 스크립트
- 다양한 필터 조합을 랜덤 생성하여 매번 다른 쿼리로 cold 성능 측정
- request_cache=false로 강제 캐시 미스
- 결과를 JSON으로 저장하여 재활용

사용법:
  python3 FORCEMERGE_BENCH.py --url http://localhost:9200 --index "lnbtpu_*" --tag lnbtpu --rounds 2000
  python3 FORCEMERGE_BENCH.py --url http://localhost:9200 --index "lnbtpunf_*" --tag lnbtpunf --rounds 2000
"""

import json, random, time, sys, argparse, os
from datetime import date, timedelta
from urllib.request import Request, urlopen

# ============================================================
# 필터 값 풀 (실제 인덱스에서 추출한 값)
# ============================================================
BBOXES = [
    {"name": "전국",   "tl_lat": 38.8, "tl_lon": 124.5, "br_lat": 33.0, "br_lon": 132.0},
    {"name": "서울",   "tl_lat": 37.7, "tl_lon": 126.75, "br_lat": 37.4, "br_lon": 127.2},
    {"name": "경기",   "tl_lat": 37.9, "tl_lon": 126.5, "br_lat": 37.0, "br_lon": 127.5},
    {"name": "부산",   "tl_lat": 35.3, "tl_lon": 128.9, "br_lat": 35.0, "br_lon": 129.2},
    {"name": "대전",   "tl_lat": 36.5, "tl_lon": 127.3, "br_lat": 36.2, "br_lon": 127.5},
    {"name": "광주",   "tl_lat": 35.25, "tl_lon": 126.75, "br_lat": 35.05, "br_lon": 127.0},
    {"name": "대구",   "tl_lat": 35.95, "tl_lon": 128.5, "br_lat": 35.75, "br_lon": 128.75},
    {"name": "인천",   "tl_lat": 37.55, "tl_lon": 126.55, "br_lat": 37.35, "br_lon": 126.8},
    {"name": "세종",   "tl_lat": 36.65, "tl_lon": 127.0, "br_lat": 36.45, "br_lon": 127.15},
    {"name": "제주",   "tl_lat": 33.55, "tl_lon": 126.15, "br_lat": 33.2, "br_lon": 126.95},
]

JIYUK_CD1 = ["64", "71", "63", "62", "43", "14", "13", "44", "22", "81", "42", "15", "16", "41", "32", "33"]
JIMOK_CD = ["02", "08", "01", "05", "14", "18", "28", "17", "09", "03", "19", "16", "27", "04", "13"]
BUILDING_PURPOSE = ["단독주택", "제2종근린생활시설", "제1종근린생활시설", "공동주택", "창고시설", "동물및식물관련시설", "공장", "노유자시설", "교육연구시설", "숙박시설", "업무시설"]
BUILDING_REGSTR = ["일반", "집합"]
TRADE_PROPERTY = ["APARTMENT", "LAND", "MULTI", "SINGLE", "OFFICETEL", "SHOPPING_AND_OFFICE", "COMMERCIAL_BUILDING"]

AGG_LEVELS = [
    {"field": "sd", "size": 20},
    {"field": "sgg", "size": 300},
    {"field": "emd", "size": 5000},
]


def rand_subset(lst, min_n=1, max_n=None):
    max_n = max_n or len(lst)
    n = random.randint(min_n, min(max_n, len(lst)))
    return random.sample(lst, n)


def rand_date(start_year=2018, end_year=2025):
    start = date(start_year, 1, 1)
    end = date(end_year, 12, 31)
    delta = (end - start).days
    return start + timedelta(days=random.randint(0, delta))


def generate_filter_combo(combo_id):
    """랜덤 필터 조합 1개 생성. 최소 1개 ~ 최대 전부 섞음."""
    combo = {"id": combo_id, "filters": {}}
    bbox = random.choice(BBOXES)
    combo["bbox"] = bbox
    combo["agg"] = random.choice(AGG_LEVELS)

    # 어떤 필터 카테고리를 켤지 결정 (최소 1개)
    categories = []
    if random.random() < 0.6:
        categories.append("land")
    if random.random() < 0.5:
        categories.append("building")
    if random.random() < 0.5:
        categories.append("trade")
    if not categories:
        categories.append(random.choice(["land", "building", "trade"]))

    f = combo["filters"]

    if "land" in categories:
        if random.random() < 0.5:
            f["landJiyukCd1"] = rand_subset(JIYUK_CD1, 1, 4)
        if random.random() < 0.4:
            f["landJimokCd"] = rand_subset(JIMOK_CD, 1, 3)
        if random.random() < 0.4:
            mn = random.randint(50, 500)
            mx = mn + random.randint(100, 5000)
            f["landAreaMin"] = mn
            f["landAreaMax"] = mx
        if random.random() < 0.3:
            mn = random.randint(10000, 500000)
            mx = mn + random.randint(100000, 2000000)
            f["landPriceMin"] = mn
            f["landPriceMax"] = mx

    if "building" in categories:
        if random.random() < 0.6:
            f["buildingMainPurpsCdNm"] = rand_subset(BUILDING_PURPOSE, 1, 3)
        if random.random() < 0.3:
            f["buildingRegstrGbCdNm"] = rand_subset(BUILDING_REGSTR, 1, 2)
        if random.random() < 0.3:
            mn = random.randint(10, 200)
            mx = mn + random.randint(50, 3000)
            f["buildingTotAreaMin"] = mn
            f["buildingTotAreaMax"] = mx
        if random.random() < 0.25:
            f["buildingUseAprDayStart"] = random.randint(1990, 2015)
            f["buildingUseAprDayEnd"] = random.randint(f["buildingUseAprDayStart"], 2025)
        if random.random() < 0.15:
            f["buildingPmsDayRecent5y"] = True
        if random.random() < 0.15:
            f["buildingStcnsDayRecent5y"] = True

    if "trade" in categories:
        if random.random() < 0.6:
            f["tradeProperty"] = rand_subset(TRADE_PROPERTY, 1, 3)
        if random.random() < 0.5:
            ds = rand_date(2020, 2024)
            de = ds + timedelta(days=random.randint(30, 730))
            f["tradeContractDateStart"] = ds.isoformat()
            f["tradeContractDateEnd"] = de.isoformat()
        if random.random() < 0.3:
            mn = random.randint(10000, 500000) * 1000
            mx = mn + random.randint(100000, 3000000) * 1000
            f["tradeEffectiveAmountMin"] = mn
            f["tradeEffectiveAmountMax"] = mx
        if random.random() < 0.2:
            mn = random.randint(1, 5) * 1000000
            mx = mn + random.randint(1, 10) * 1000000
            f["tradeBuildingAmountPerM2Min"] = mn
            f["tradeBuildingAmountPerM2Max"] = mx

    return combo


def combo_to_query(combo):
    """필터 조합을 OpenSearch JSON 쿼리로 변환."""
    bbox = combo["bbox"]
    agg = combo["agg"]
    f = combo["filters"]

    filters = []

    # Building nested
    bldg_filters = []
    if "buildingMainPurpsCdNm" in f:
        bldg_filters.append({"terms": {"buildings.mainPurpsCdNm": f["buildingMainPurpsCdNm"]}})
    if "buildingRegstrGbCdNm" in f:
        bldg_filters.append({"terms": {"buildings.regstrGbCdNm": f["buildingRegstrGbCdNm"]}})
    if f.get("buildingPmsDayRecent5y"):
        five_years_ago = (date.today() - timedelta(days=5*365)).isoformat()
        bldg_filters.append({"range": {"buildings.pmsDay": {"gte": five_years_ago}}})
    if f.get("buildingStcnsDayRecent5y"):
        five_years_ago = (date.today() - timedelta(days=5*365)).isoformat()
        bldg_filters.append({"range": {"buildings.stcnsDay": {"gte": five_years_ago}}})
    if "buildingUseAprDayStart" in f:
        bldg_filters.append({"range": {"buildings.useAprDay": {"gte": f"{f['buildingUseAprDayStart']}-01-01"}}})
    if "buildingUseAprDayEnd" in f:
        bldg_filters.append({"range": {"buildings.useAprDay": {"lte": f"{f['buildingUseAprDayEnd']}-12-31"}}})
    if "buildingTotAreaMin" in f:
        bldg_filters.append({"range": {"buildings.totArea": {"gte": f["buildingTotAreaMin"]}}})
    if "buildingTotAreaMax" in f:
        bldg_filters.append({"range": {"buildings.totArea": {"lte": f["buildingTotAreaMax"]}}})

    if bldg_filters:
        filters.append({"nested": {"path": "buildings", "query": {"bool": {"filter": bldg_filters}}}})

    # Land
    if "landJiyukCd1" in f:
        filters.append({"terms": {"land.jiyukCd1": f["landJiyukCd1"]}})
    if "landJimokCd" in f:
        filters.append({"terms": {"land.jimokCd": f["landJimokCd"]}})
    if "landAreaMin" in f:
        filters.append({"range": {"land.area": {"gte": f["landAreaMin"]}}})
    if "landAreaMax" in f:
        filters.append({"range": {"land.area": {"lte": f["landAreaMax"]}}})
    if "landPriceMin" in f:
        filters.append({"range": {"land.price": {"gte": f["landPriceMin"]}}})
    if "landPriceMax" in f:
        filters.append({"range": {"land.price": {"lte": f["landPriceMax"]}}})

    # Trade nested
    trade_filters = []
    if "tradeProperty" in f:
        trade_filters.append({"terms": {"trades.property": f["tradeProperty"]}})
    if "tradeContractDateStart" in f:
        trade_filters.append({"range": {"trades.contractDate": {"gte": f["tradeContractDateStart"]}}})
    if "tradeContractDateEnd" in f:
        trade_filters.append({"range": {"trades.contractDate": {"lte": f["tradeContractDateEnd"]}}})
    if "tradeEffectiveAmountMin" in f:
        trade_filters.append({"range": {"trades.effectiveAmount": {"gte": f["tradeEffectiveAmountMin"]}}})
    if "tradeEffectiveAmountMax" in f:
        trade_filters.append({"range": {"trades.effectiveAmount": {"lte": f["tradeEffectiveAmountMax"]}}})
    if "tradeBuildingAmountPerM2Min" in f:
        trade_filters.append({"range": {"trades.buildingAmountPerM2": {"gte": f["tradeBuildingAmountPerM2Min"]}}})
    if "tradeBuildingAmountPerM2Max" in f:
        trade_filters.append({"range": {"trades.buildingAmountPerM2": {"lte": f["tradeBuildingAmountPerM2Max"]}}})

    if trade_filters:
        filters.append({"nested": {"path": "trades", "query": {"bool": {"filter": trade_filters}}}})

    query = {
        "size": 0,
        "query": {
            "bool": {
                "must": [{
                    "geo_bounding_box": {
                        "land.center": {
                            "top_left": {"lat": bbox["tl_lat"], "lon": bbox["tl_lon"]},
                            "bottom_right": {"lat": bbox["br_lat"], "lon": bbox["br_lon"]}
                        }
                    }
                }],
                "filter": filters
            }
        },
        "aggs": {
            "by_region": {
                "terms": {"field": agg["field"], "size": agg["size"]},
                "aggs": {
                    "center": {"geo_centroid": {"field": "land.center"}}
                }
            }
        }
    }
    return query


def run_query(url, index, query_json):
    endpoint = f"{url}/{index}/_search?request_cache=false"
    data = json.dumps(query_json).encode("utf-8")
    req = Request(endpoint, data=data, headers={"Content-Type": "application/json"}, method="POST")
    resp = urlopen(req, timeout=30)
    return json.loads(resp.read())


def main():
    parser = argparse.ArgumentParser(description="Forcemerge benchmark")
    parser.add_argument("--url", default="http://localhost:9200")
    parser.add_argument("--index", default="lnbtpu_*")
    parser.add_argument("--tag", default="lnbtpu", help="결과 파일 태그")
    parser.add_argument("--rounds", type=int, default=2000)
    parser.add_argument("--seed", type=int, default=42, help="랜덤 시드 (동일 조합 재현)")
    args = parser.parse_args()

    random.seed(args.seed)

    # 조합 생성
    combos = [generate_filter_combo(i) for i in range(args.rounds)]
    print(f"[{args.tag}] {args.rounds}개 필터 조합 생성 완료 (seed={args.seed})")

    # 웜업 3회
    for i in range(3):
        try:
            q = combo_to_query(combos[i])
            run_query(args.url, args.index, q)
        except Exception:
            pass

    # 벤치마크
    results = []
    errors = 0
    tooks = []

    print(f"[{args.tag}] 벤치마크 시작: {args.rounds}회")
    start_all = time.time()

    for i, combo in enumerate(combos):
        q = combo_to_query(combo)
        try:
            resp = run_query(args.url, args.index, q)
            took = resp["took"]
            hits = resp["hits"]["total"]["value"]
            buckets = len(resp.get("aggregations", {}).get("by_region", {}).get("buckets", []))
            tooks.append(took)
            results.append({
                "id": i,
                "took_ms": took,
                "hits": hits,
                "buckets": buckets,
                "bbox": combo["bbox"]["name"],
                "agg": combo["agg"]["field"],
                "filter_keys": list(combo["filters"].keys()),
            })
        except Exception as e:
            errors += 1
            results.append({"id": i, "error": str(e)})

        if (i + 1) % 100 == 0:
            elapsed = time.time() - start_all
            avg_so_far = sum(tooks[-100:]) / min(100, len(tooks[-100:])) if tooks else 0
            print(f"  [{i+1}/{args.rounds}] last100_avg={avg_so_far:.0f}ms elapsed={elapsed:.1f}s errors={errors}")

    total_time = time.time() - start_all

    # 통계
    tooks.sort()
    n = len(tooks)
    if n > 0:
        avg = sum(tooks) / n
        p50 = tooks[n // 2]
        p90 = tooks[int(n * 0.9)]
        p95 = tooks[int(n * 0.95)]
        p99 = tooks[int(n * 0.99)]
        mn = tooks[0]
        mx = tooks[-1]
        stddev = (sum((t - avg) ** 2 for t in tooks) / n) ** 0.5
    else:
        avg = p50 = p90 = p95 = p99 = mn = mx = stddev = 0

    summary = {
        "tag": args.tag,
        "index": args.index,
        "rounds": args.rounds,
        "seed": args.seed,
        "errors": errors,
        "total_seconds": round(total_time, 1),
        "stats": {
            "n": n,
            "avg": round(avg, 1),
            "p50": p50,
            "p90": p90,
            "p95": p95,
            "p99": p99,
            "min": mn,
            "max": mx,
            "stddev": round(stddev, 1),
        },
    }

    print(f"\n{'='*60}")
    print(f"[{args.tag}] 결과 요약")
    print(f"{'='*60}")
    print(f"  n={n}  errors={errors}  total={total_time:.1f}s")
    print(f"  avg={avg:.0f}ms  p50={p50}ms  p90={p90}ms  p95={p95}ms  p99={p99}ms")
    print(f"  min={mn}ms  max={mx}ms  stddev={stddev:.1f}ms")
    print(f"{'='*60}")

    # 결과 저장
    output_dir = os.path.dirname(os.path.abspath(__file__))
    summary_file = os.path.join(output_dir, f"bench_{args.tag}.json")
    with open(summary_file, "w") as fp:
        json.dump({"summary": summary, "results": results}, fp, ensure_ascii=False)
    print(f"  결과 저장: {summary_file}")


if __name__ == "__main__":
    main()