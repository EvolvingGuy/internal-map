<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ES Land Cluster</title>
    <script src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapClientId}"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        #map { width: 100%; height: 100vh; }
        #info {
            position: absolute;
            top: 10px;
            left: 10px;
            z-index: 1000;
            background: white;
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            font-size: 13px;
        }
        #info h3 { margin-bottom: 8px; color: #2563eb; }
        #info div { margin: 4px 0; }
        #filters {
            position: absolute;
            top: 10px;
            right: 10px;
            z-index: 1000;
            background: white;
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            font-size: 12px;
            max-height: 90vh;
            overflow-y: auto;
        }
        #filters label { display: block; margin: 4px 0; cursor: pointer; }
        #filters label:hover { background: #f0f0f0; }
        .filter-section { margin-top: 12px; border-top: 1px solid #eee; padding-top: 8px; }
        .filter-section strong { display: block; margin-bottom: 6px; }
        .filter-list { display: block; max-height: 200px; overflow-y: auto; padding-left: 4px; }
        .filter-count { font-size: 10px; color: #666; margin-left: 4px; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>ES Land Cluster</h3>
        <div>클러스터: <span id="clusterCount">0</span>개</div>
        <div>총 필지: <span id="totalCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>
    <div id="filters">
        <strong>ClusterType</strong>
        <label><input type="radio" name="clusterType" value="SD" checked> 시도</label>
        <label><input type="radio" name="clusterType" value="SGG"> 시군구</label>
        <label><input type="radio" name="clusterType" value="EMD"> 읍면동</label>
        <label><input type="radio" name="clusterType" value="GRID"> 그리드</label>

        <div class="filter-section">
            <strong>지목 <span id="jimok-count" class="filter-count">(0)</span></strong>
            <div id="jimok-list" class="filter-list">
                <label><input type="checkbox" name="jimokCd" value="01"> 전</label>
                <label><input type="checkbox" name="jimokCd" value="02"> 답</label>
                <label><input type="checkbox" name="jimokCd" value="03"> 과수원</label>
                <label><input type="checkbox" name="jimokCd" value="04"> 목장용지</label>
                <label><input type="checkbox" name="jimokCd" value="05"> 임야</label>
                <label><input type="checkbox" name="jimokCd" value="06"> 광천지</label>
                <label><input type="checkbox" name="jimokCd" value="07"> 염전</label>
                <label><input type="checkbox" name="jimokCd" value="08"> 대</label>
                <label><input type="checkbox" name="jimokCd" value="09"> 공장용지</label>
                <label><input type="checkbox" name="jimokCd" value="10"> 학교용지</label>
                <label><input type="checkbox" name="jimokCd" value="11"> 주차장</label>
                <label><input type="checkbox" name="jimokCd" value="12"> 주유소용지</label>
                <label><input type="checkbox" name="jimokCd" value="13"> 창고용지</label>
                <label><input type="checkbox" name="jimokCd" value="14"> 도로</label>
                <label><input type="checkbox" name="jimokCd" value="15"> 철도용지</label>
                <label><input type="checkbox" name="jimokCd" value="16"> 제방</label>
                <label><input type="checkbox" name="jimokCd" value="17"> 하천</label>
                <label><input type="checkbox" name="jimokCd" value="18"> 구거</label>
                <label><input type="checkbox" name="jimokCd" value="19"> 유지</label>
                <label><input type="checkbox" name="jimokCd" value="20"> 양어장</label>
                <label><input type="checkbox" name="jimokCd" value="21"> 수도용지</label>
                <label><input type="checkbox" name="jimokCd" value="22"> 공원</label>
                <label><input type="checkbox" name="jimokCd" value="23"> 체육용지</label>
                <label><input type="checkbox" name="jimokCd" value="24"> 유원지</label>
                <label><input type="checkbox" name="jimokCd" value="25"> 종교용지</label>
                <label><input type="checkbox" name="jimokCd" value="26"> 사적지</label>
                <label><input type="checkbox" name="jimokCd" value="27"> 묘지</label>
                <label><input type="checkbox" name="jimokCd" value="28"> 잡종지</label>
            </div>
        </div>

        <div class="filter-section">
            <strong>지역 <span id="jiyuk-count" class="filter-count">(0)</span></strong>
            <div id="jiyuk-list" class="filter-list">
                <label><input type="checkbox" name="jiyukCd1" value="11"> 제1종전용주거지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="12"> 제2종전용주거지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="13"> 제1종일반주거지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="14"> 제2종일반주거지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="15"> 제3종일반주거지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="16"> 준주거지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="21"> 중심상업지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="22"> 일반상업지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="23"> 근린상업지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="24"> 유통상업지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="31"> 전용공업지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="32"> 일반공업지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="33"> 준공업지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="41"> 보전녹지지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="42"> 생산녹지지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="43"> 자연녹지지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="44"> 개발제한구역</label>
                <label><input type="checkbox" name="jiyukCd1" value="51"> 용도미지정지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="61"> 관리지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="62"> 보전관리지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="63"> 생산관리지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="64"> 계획관리지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="71"> 농림지역</label>
                <label><input type="checkbox" name="jiyukCd1" value="81"> 자연환경보전지역</label>
            </div>
        </div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(36.5, 127.5),
            zoom: 7
        });

        let debounceTimer = null;
        let markerMap = new Map();
        let pendingDraw = null;

        function getClusterType() {
            return document.querySelector('input[name="clusterType"]:checked').value;
        }

        function getCheckedValues(name) {
            const checked = document.querySelectorAll('input[name="' + name + '"]:checked');
            return Array.from(checked).map(el => el.value);
        }

        function updateFilterCount(name) {
            const count = getCheckedValues(name).length;
            document.getElementById(name.replace('Cd', '').replace('1', '') + '-count').textContent = '(' + count + ')';
        }

        function fetchData() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const params = new URLSearchParams({
                southwestLatitude: sw.lat(),
                southwestLongitude: sw.lng(),
                northeastLatitude: ne.lat(),
                northeastLongitude: ne.lng(),
                clusterType: getClusterType()
            });

            const jimokCd = getCheckedValues('jimokCd');
            const jiyukCd1 = getCheckedValues('jiyukCd1');

            if (jimokCd.length > 0) {
                jimokCd.forEach(v => params.append('jimokCd', v));
            }
            if (jiyukCd1.length > 0) {
                jiyukCd1.forEach(v => params.append('jiyukCd1', v));
            }

            fetch(`/api/es/land-cluster/clusters?${'$'}{params}`)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('clusterCount').textContent = data.clusters.length.toLocaleString();
                    document.getElementById('totalCount').textContent = data.totalCount.toLocaleString();
                    document.getElementById('elapsed').textContent = data.elapsedMs;
                    drawClusters(data.clusters);
                })
                .catch(err => console.error('fetch error:', err));
        }

        function drawClusters(clusters) {
            if (pendingDraw) cancelAnimationFrame(pendingDraw);

            pendingDraw = requestAnimationFrame(() => {
                const activeKeys = new Set();

                for (const cluster of clusters) {
                    if (cluster.count === 0) continue;
                    const key = String(cluster.code);
                    activeKeys.add(key);

                    const center = new naver.maps.LatLng(cluster.centerLat, cluster.centerLng);
                    const existing = markerMap.get(key);

                    if (existing) {
                        existing.setPosition(center);
                        existing.setIcon({
                            content: buildLabelHtml(cluster.name, cluster.count),
                            anchor: new naver.maps.Point(0, 0)
                        });
                    } else {
                        const marker = new naver.maps.Marker({
                            map: map,
                            position: center,
                            icon: {
                                content: buildLabelHtml(cluster.name, cluster.count),
                                anchor: new naver.maps.Point(0, 0)
                            }
                        });
                        markerMap.set(key, marker);
                    }
                }

                for (const [key, marker] of markerMap) {
                    if (!activeKeys.has(key)) {
                        marker.setMap(null);
                        markerMap.delete(key);
                    }
                }

                pendingDraw = null;
            });
        }

        function buildLabelHtml(name, count) {
            return '<div style="position:relative;"><div style="background:#2563eb;color:#fff;min-width:60px;padding:8px 12px;border-radius:20px;font-size:11px;font-weight:bold;text-align:center;transform:translate(-50%,-50%);box-shadow:0 2px 6px rgba(0,0,0,0.3);">' +
                name + '<br>' + count.toLocaleString() + '</div></div>';
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                fetchData();
            }, 300);
        }

        document.querySelectorAll('input[name="clusterType"]').forEach(el => {
            el.addEventListener('change', fetchData);
        });

        document.querySelectorAll('input[name="jimokCd"]').forEach(el => {
            el.addEventListener('change', () => {
                updateFilterCount('jimokCd');
                fetchData();
            });
        });

        document.querySelectorAll('input[name="jiyukCd1"]').forEach(el => {
            el.addEventListener('change', () => {
                updateFilterCount('jiyukCd1');
                fetchData();
            });
        });

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        fetchData();
    </script>
</body>
</html>
