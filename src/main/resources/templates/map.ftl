<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Boundary Map</title>
    <script src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapClientId}"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        #map { width: 100%; height: 100vh; }
        #controls {
            position: absolute;
            top: 10px;
            left: 10px;
            z-index: 1000;
            background: white;
            padding: 10px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        }
        #controls select {
            padding: 8px 12px;
            font-size: 14px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        #info {
            position: absolute;
            bottom: 10px;
            left: 10px;
            z-index: 1000;
            background: white;
            padding: 10px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            font-size: 12px;
            max-width: 300px;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="controls">
        <select id="levelSelect">
            <option value="SIDO">시도</option>
            <option value="SIGUNGU">시군구</option>
            <option value="DONG" selected>읍면동</option>
        </select>
    </div>
    <div id="info">
        <div>검색된 지역: <span id="regionCount">0</span>개</div>
        <div>PNU 카운트: <span id="pnuCount">0</span>개</div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 18
        });

        let currentLevel = 'DONG';
        let debounceTimer = null;
        let overlayMap = new Map();  // regionCode -> { circle, marker }
        let pendingDraw = null;

        document.getElementById('levelSelect').addEventListener('change', function(e) {
            currentLevel = e.target.value;
            searchBoundary();
        });

        function searchBoundary() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();
            const swLng = sw.lng(), swLat = sw.lat(), neLng = ne.lng(), neLat = ne.lat();

            const params = new URLSearchParams({ swLng, swLat, neLng, neLat, level: currentLevel });

            fetch('/api/boundary/search?' + params)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('regionCount').textContent = data.matched.length;
                    console.log('Boundary search result:', data);

                    // PNU 카운트 조회 (멀티키 + bbox 트리밍)
                    if (data.matched.length > 0) {
                        const regionCodes = data.matched.map(r => r.regionCode);
                        fetchPnuCount(regionCodes, swLng, swLat, neLng, neLat);
                    } else {
                        document.getElementById('pnuCount').textContent = '0';
                    }
                })
                .catch(err => console.error('Search error:', err));
        }

        function fetchPnuCount(regionCodes, swLng, swLat, neLng, neLat) {
            fetch('/api/pnu/count', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ regionCodes, swLng, swLat, neLng, neLat })
            })
                .then(res => res.json())
                .then(data => {
                    const total = Object.values(data).reduce((sum, r) => sum + r.count, 0);
                    document.getElementById('pnuCount').textContent = total.toLocaleString();
                    console.log('PNU count result:', data);
                    drawCircles(data);
                })
                .catch(err => console.error('PNU count error:', err));
        }

        function drawCircles(data) {
            // 이전 프레임 취소
            if (pendingDraw) cancelAnimationFrame(pendingDraw);

            pendingDraw = requestAnimationFrame(() => {
                const activeKeys = new Set();

                for (const [regionCode, result] of Object.entries(data)) {
                    if (result.count === 0 || result.centerLat === 0) continue;
                    activeKeys.add(regionCode);

                    const center = new naver.maps.LatLng(result.centerLat, result.centerLng);
                    const existing = overlayMap.get(regionCode);

                    if (existing) {
                        // 기존 overlay 재사용 - 위치/내용만 업데이트
                        existing.circle.setCenter(center);
                        existing.marker.setPosition(center);
                        existing.marker.setIcon({
                            content: buildLabelHtml(result),
                            anchor: new naver.maps.Point(0, 0)
                        });
                    } else {
                        // 새로 생성
                        const circle = new naver.maps.Circle({
                            map: map,
                            center: center,
                            radius: 200,
                            fillColor: '#3b82f6',
                            fillOpacity: 0.35,
                            strokeColor: '#2563eb',
                            strokeWeight: 2
                        });
                        const marker = new naver.maps.Marker({
                            map: map,
                            position: center,
                            icon: {
                                content: buildLabelHtml(result),
                                anchor: new naver.maps.Point(0, 0)
                            }
                        });
                        overlayMap.set(regionCode, { circle, marker });
                    }
                }

                // 화면에서 벗어난 overlay 제거
                for (const [key, { circle, marker }] of overlayMap) {
                    if (!activeKeys.has(key)) {
                        circle.setMap(null);
                        marker.setMap(null);
                        overlayMap.delete(key);
                    }
                }

                pendingDraw = null;
            });
        }

        function buildLabelHtml(result) {
            return '<div style="position:relative;"><div style="background:#2563eb;color:#fff;padding:4px 10px;border-radius:12px;font-size:11px;font-weight:bold;white-space:nowrap;text-align:center;transform:translate(-50%,-50%);">' +
                result.name + '<br>' + result.count.toLocaleString() +
                '</div></div>';
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(searchBoundary, 300);
        }

        naver.maps.Event.addListener(map, 'idle', onMapIdle);

        searchBoundary();
    </script>
</body>
</html>
