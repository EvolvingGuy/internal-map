<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H3 Fixed Grid (300m)</title>
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
        #info h3 { margin-bottom: 8px; color: #dc2626; }
        #info div { margin: 4px 0; }
        #error {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            z-index: 1001;
            background: #fef2f2;
            border: 1px solid #fecaca;
            color: #dc2626;
            padding: 16px 24px;
            border-radius: 8px;
            font-size: 14px;
            display: none;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>H3 Fixed Grid (300m)</h3>
        <div>그리드: <span id="gridCount">0</span>개</div>
        <div>총 PNU: <span id="pnuCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>
    <div id="error"></div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 16
        });

        let debounceTimer = null;
        let markers = [];
        let rectangles = [];

        function fetchData() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat(),
                gridSize: 300
            });

            fetch(`/api/h3/redis/fixed-grid?${r"${params}"}`)
                .then(res => res.json())
                .then(data => {
                    if (data.error) {
                        showError(data.error);
                        clearOverlays();
                        document.getElementById('gridCount').textContent = '0';
                        document.getElementById('pnuCount').textContent = '0';
                        document.getElementById('elapsed').textContent = '0';
                        return;
                    }
                    hideError();
                    document.getElementById('gridCount').textContent = data.cells.length.toLocaleString();
                    document.getElementById('pnuCount').textContent = data.totalCount.toLocaleString();
                    document.getElementById('elapsed').textContent = data.elapsedMs;
                    drawGrids(data.cells);
                })
                .catch(err => {
                    console.error('fetch error:', err);
                    showError('데이터를 불러오는 중 오류가 발생했습니다.');
                });
        }

        function showError(msg) {
            const el = document.getElementById('error');
            el.textContent = msg;
            el.style.display = 'block';
        }

        function hideError() {
            document.getElementById('error').style.display = 'none';
        }

        function clearOverlays() {
            markers.forEach(m => m.setMap(null));
            rectangles.forEach(r => r.setMap(null));
            markers = [];
            rectangles = [];
        }

        function drawGrids(cells) {
            clearOverlays();

            for (const cell of cells) {
                if (cell.cnt === 0) continue;

                // 그리드 경계 사각형
                const rect = new naver.maps.Rectangle({
                    map: map,
                    bounds: new naver.maps.LatLngBounds(
                        new naver.maps.LatLng(cell.gridSwLat, cell.gridSwLng),
                        new naver.maps.LatLng(cell.gridNeLat, cell.gridNeLng)
                    ),
                    strokeColor: '#dc2626',
                    strokeOpacity: 0.6,
                    strokeWeight: 1,
                    fillColor: '#dc2626',
                    fillOpacity: 0.1
                });
                rectangles.push(rect);

                // 카운트 마커 (가중 평균 위치)
                const marker = new naver.maps.Marker({
                    map: map,
                    position: new naver.maps.LatLng(cell.lat, cell.lng),
                    icon: {
                        content: '<div style="background:#dc2626;color:#fff;padding:4px 8px;border-radius:4px;font-size:12px;font-weight:bold;white-space:nowrap;transform:translate(-50%,-50%);">' +
                            cell.cnt.toLocaleString() + '</div>',
                        anchor: new naver.maps.Point(0, 0)
                    }
                });
                markers.push(marker);
            }
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fetchData, 300);
        }

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        fetchData();
    </script>
</body>
</html>
