# FM vs NoFM 비교 환경 셋업

볼륨 복제를 이용해 **인덱싱 1회**로 fm / nofm 두 환경을 만드는 절차.

---

## 전제

- Docker Compose 파일: `docker-compose.lbt1x16fm.yml`
- 볼륨: `geo_poc_os_lbt_1x16_fm`
- 포트: fm = `9200`, nofm = `9201`

---

## Step 1. 인덱싱 (forcemerge 없이)

> IndexingService의 자동 forcemerge가 켜져 있으면 **끄고** 인덱싱한다.
> 또는 자동 forcemerge 완료 전에 인덱싱만 끝난 시점에서 진행.

```bash
# 컨테이너 기동
docker compose -f docker-compose.lbt1x16fm.yml up -d

# Spring Boot에서 reindex 호출
curl -X PUT http://localhost:8080/api/es/lbt-1x16-fm/reindex
```

인덱싱 완료 확인:

```bash
curl -s http://localhost:9200/lbt_1x16_fm/_count | jq .count
```

---

## Step 2. 컨테이너 정지

데이터 정합성을 위해 **반드시 stop 후 복제**.

```bash
docker compose -f docker-compose.lbt1x16fm.yml down
```

---

## Step 3. 볼륨 복제

```bash
# nofm용 볼륨 생성
docker volume create geo_poc_os_lbt_1x16_nofm

# 복제 (alpine 임시 컨테이너)
docker run --rm \
  -v geo_poc_os_lbt_1x16_fm:/source:ro \
  -v geo_poc_os_lbt_1x16_nofm:/target \
  alpine sh -c "cp -a /source/. /target/"
```

---

## Step 4. nofm 컨테이너 기동 + 인덱스명 변경

nofm은 포트 `9201`로 띄운다.

```bash
docker run -d \
  --name opensearch-lbt1x16nofm \
  -e discovery.type=single-node \
  -e DISABLE_SECURITY_PLUGIN=true \
  -e "OPENSEARCH_JAVA_OPTS=-Xms6g -Xmx6g" \
  -p 9201:9200 \
  -v geo_poc_os_lbt_1x16_nofm:/usr/share/opensearch/data \
  --cpus 6 --memory 16g \
  opensearchproject/opensearch:2.11.1
```

헬스체크 대기:

```bash
until curl -s http://localhost:9201/_cluster/health | grep -q '"status":"green"\|"status":"yellow"'; do
  sleep 2
done
```

인덱스명 변경 (`lbt_1x16_fm` → `lbt_1x16_nofm`):

```bash
# reindex (노드 내부 복사 — 빠름)
curl -X POST "http://localhost:9201/_reindex" \
  -H 'Content-Type: application/json' -d '{
  "source": { "index": "lbt_1x16_fm" },
  "dest": { "index": "lbt_1x16_nofm" }
}'

# 완료 확인
curl -s http://localhost:9201/lbt_1x16_nofm/_count | jq .count

# 원본 삭제
curl -X DELETE "http://localhost:9201/lbt_1x16_fm"
```

nofm 준비 완료. **forcemerge 하지 않는다.**

---

## Step 5. fm 컨테이너 기동 + forcemerge

```bash
docker compose -f docker-compose.lbt1x16fm.yml up -d
```

forcemerge 실행:

```bash
curl -X PUT http://localhost:8080/api/es/lbt-1x16-fm/forcemerge
```

또는 직접:

```bash
curl -X POST "http://localhost:9200/lbt_1x16_fm/_forcemerge?max_num_segments=1"
```

완료 확인:

```bash
curl -s "http://localhost:9200/lbt_1x16_fm/_segments" | jq '.indices[].shards[][] | .num_search_segments'
```

모든 샤드가 `1`이면 완료.

---

## Step 6. 벤치마크

두 컨테이너가 동시에 떠 있는 상태:

| 환경 | 포트 | 인덱스명 |
|------|------|----------|
| fm | 9200 | `lbt_1x16_fm` |
| nofm | 9201 | `lbt_1x16_nofm` |

```bash
# fm 벤치마크
python3 VS_FORCEMERGE_BENCH.py \
  --url http://localhost:9200 \
  --index "lbt_1x16_fm" \
  --tag lbt_1x16_fm \
  --rounds 2000

# nofm 벤치마크
python3 VS_FORCEMERGE_BENCH.py \
  --url http://localhost:9201 \
  --index "lbt_1x16_nofm" \
  --tag lbt_1x16_nofm \
  --rounds 2000
```

---

## 정리

```bash
# nofm 컨테이너 제거
docker stop opensearch-lbt1x16nofm && docker rm opensearch-lbt1x16nofm

# nofm 볼륨 제거
docker volume rm geo_poc_os_lbt_1x16_nofm

# fm 컨테이너 제거
docker compose -f docker-compose.lbt1x16fm.yml down -v
```

---

## 다른 샤드 전략에도 동일 적용

`2x3`, `17x4_region` 등도 같은 절차:

1. 해당 인덱스 인덱싱 (forcemerge 없이)
2. stop → 볼륨 복제
3. nofm 컨테이너 (포트 분리) → `_reindex`로 이름 변경
4. fm 컨테이너 → forcemerge
5. 벤치마크
