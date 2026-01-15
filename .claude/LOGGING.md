# 로깅 방식
테이블 그대로 (PnuAgg)
[PnuAgg] res={resolution}, h3={h3개수}, hit={캐시히트}, miss={캐시미스}, result={결과수} | h3={H3계산}ms, cacheGet={캐시조회}ms, db={DB조회}ms, cacheSet={캐시저장}ms, total={총시간}ms
예시:
[PnuAgg] res=10, h3=45, hit=40, miss=5, result=123 | h3=2ms, cacheGet=5ms, db=12ms, cacheSet=3ms, total=25ms

  ---
가변형 (PnuAggDynamic)
[PnuAggDynamic] res={resolution}, h3={h3개수}, hit={캐시히트}, miss={캐시미스}, regions={행정구역수} | h3={H3계산}ms, cacheGet={캐시조 회}ms, db={DB조회}ms, cacheSet={캐시저장}ms, total={총시간}ms
예시:
[PnuAggDynamic] res=5, h3=12, hit=12, miss=0, regions=8 | h3=1ms, cacheGet=3ms, db=0ms, cacheSet=0ms, total=6ms

  ---
고정형 (PnuAggStatic)
[PnuAggStatic] level={레벨}, res={resolution}, h3={h3개수}, hit={캐시히트}, miss={캐시미스}, regions={행정구역수} | h3={H3계산}ms, cacheGet={캐시조회}ms, db={DB조회}ms, cacheSet={캐시저장}ms, total={총시간}ms
예시:
[PnuAggStatic] level=sd, res=5, h3=20, hit=18, miss=2, regions=5 | h3=1ms, cacheGet=4ms, db=8ms, cacheSet=2ms, total=18ms
