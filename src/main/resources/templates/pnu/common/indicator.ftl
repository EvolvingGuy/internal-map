<#-- 중심점 행정구역 인디케이터 공통 모듈 -->

<#-- CSS 스타일 (head 안에 포함) -->
<#macro indicatorStyle>
        #indicator {
            position: absolute;
            top: 50px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 1000;
            background: rgba(0, 0, 0, 0.75);
            color: white;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 500;
            white-space: nowrap;
            box-shadow: 0 2px 8px rgba(0,0,0,0.3);
        }
</#macro>

<#-- HTML 요소 (body 안에 포함) -->
<#macro indicatorHtml>
    <div id="indicator">위치 확인 중...</div>
</#macro>

<#-- JavaScript 함수 (script 안에 포함) -->
<#macro indicatorScript>
        function fetchIndicator() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat()
            });

            fetch(`/api/pnu/agg/indicator?${'$'}{params}`)
                .then(res => res.json())
                .then(data => {
                    const indicatorEl = document.getElementById('indicator');
                    if (data.indicator) {
                        indicatorEl.textContent = data.indicator;
                    } else {
                        indicatorEl.textContent = '위치 정보 없음';
                    }
                })
                .catch(err => {
                    console.error('indicator fetch error:', err);
                    document.getElementById('indicator').textContent = '위치 정보 오류';
                });
        }
</#macro>
