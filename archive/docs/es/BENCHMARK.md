# ES Index Benchmark
필터 있는 SD 집계 기준 인덱스별 성능 비교

# force merge 전후
- 별 차이 없음 -> 그렇다면 하지 말자
- LNBTPU vs LNBTPUNF

# 인덱스
- LNBTPU 4인덱스(균등) 16샤드
  - Local 41분 27초
- LNBTP16 1인덱스 16샤드
  - Local 47분 30초
- LBNTPS 17인덱스 4샤드 (시도별 인덱스)
  - Local 47분 30초

- LNBTPU23 2인덱스(균등) 3샤드
  - Cluster 
    - index 30분 1초
  - forcemerge  
    - 50.12s
    - 101.73s 




- LNBTPU23
  - [LNBTPU23] ========== 인덱싱 완료 ==========
  - [LNBTPU23] 총 문서: 39,668,369건, 총 건물: 6,210,982건, 총 실거래: 41,844,124건, 벌크 42,338회, 총 소요시간: 30.01m (1800.6s)
  - [LNBTPU23] 파티션별 문서 수: P1=19,835,179, P2=19,833,190

- LNBTP16 1인덱스 2샤드
  - [LNBTP16] 총 문서: 39,668,369건, 총 건물: 6,210,982건, 총 실거래: 41,844,124건, 벌크 42,338회, 총 소요시간: 47.30m (2837.8s)
  - Forcemerge 요청 완료 (ES 백그라운드 처리 중): 60,000 milliseconds timeout on connection http-outgoing-13 [ACTIVE], 경과: 60,081ms (60.08s)
- LNBTPS 시도 17인덱스 4샤드
  - [LNBTPS] 총 문서: 39,668,369건, 총 건물: 6,210,982건, 총 실거래: 41,844,124건, 벌크 42,338회, 총 소요시간: 35.04m (2102.2s)
  - [LNBTPS] 시도별 문서 수: 11=903,166, 26=714,165, 27=791,389, 28=669,158, 29=390,256, 30=292,049, 31=507,628, 36=206,484, 41=5,164,227, 43=2,395,086, 44=3,740,187, 46=5,899,035, 47=5,694,523, 48=4,819,948, 50=880,280
  - [LNBTPS] ========== Forcemerge 시작 (17개 인덱스) ==========
  - [LNBTPS] Forcemerge 완료 [lnbtps_11]: 9,762ms (9.76s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_26]: 13,956ms (13.96s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_27]: 18,681ms (18.68s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_28]: 22,665ms (22.67s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_29]: 24,708ms (24.71s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_30]: 26,447ms (26.45s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_31]: 28,493ms (28.49s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_36]: 29,299ms (29.30s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_41]: 52,967ms (52.97s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_42]: 53,015ms (53.02s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_43]: 60,025ms (60.03s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_44]: 71,458ms (71.46s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_45]: 71,476ms (71.48s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_46]: 89,620ms (89.62s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_47]: 107,035ms (107.04s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_48]: 122,702ms (122.70s)
  - [LNBTPS] Forcemerge 완료 [lnbtps_50]: 124,984ms (124.98s)
- LNBTPU
  - 41분 27초
    [LNBTPU] 총 문서: 39,668,369건, 총 건물: 6,210,982건, 총 실거래: 41,844,124건, 벌크 42,338회, 총 소요시간: 41.27m (2475.9s)
    [LNBTPU] 파티션별 문서 수: P1=9,919,172, P2=9,916,515, P3=9,916,007, P4=9,916,675
    [LNBTPU] ========== Forcemerge 시작 (4개 인덱스) ==========
    [LNBTPU] Forcemerge 완료 [lnbtpu_1]: 56,732ms (56.73s)
    [LNBTPU] Forcemerge 요청 완료 [lnbtpu_2] (ES 백그라운드 처리 중): 60,000 milliseconds timeout on connection http-outgoing-70 [ACTIVE], 경과: 116,869ms (116.87s)
    [LNBTPU] Forcemerge 완료 [lnbtpu_3]: 166,652ms (166.65s)
    [LNBTPU] Forcemerge 완료 [lnbtpu_4]: 209,966ms (209.97s)
- LNBTPU23 2인덱스(균등) 3샤드
  - 
- LNBTPP


# 검색
- 카운트
- 클러스터