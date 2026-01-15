<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
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
        #info h3 { margin-bottom: 8px; color: #7c3aed; }
        #info div { margin: 4px 0; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>${title}</h3>
        <div>줌레벨: <span id="zoomLevel">0</span></div>
        <div>그리드: <span id="gridSize">0x0</span></div>
        <div>셀: <span id="cellCount">0</span>개</div>
        <div>총 PNU: <span id="pnuCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 18
        });

        let debounceTimer = null;
        let markerMap = new Map();
        let pendingDraw = null;

        const MIN_CIRCLE_SIZE = 40;
        const MAX_CIRCLE_SIZE = 100;

        function fetchData() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();
            const zoom = map.getZoom();
            const size = map.getSize();

            document.getElementById('zoomLevel').textContent = zoom;

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat(),
                zoomLevel: zoom,
                viewportWidth: size.width,
                viewportHeight: size.height
            });

            fetch(`/api/pnu/agg/grid?${'$'}{params}`)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('gridSize').textContent = data.cols + 'x' + data.rows;
                    document.getElementById('cellCount').textContent = data.cells.length.toLocaleString();
                    document.getElementById('pnuCount').textContent = data.totalCount.toLocaleString();
                    document.getElementById('elapsed').textContent = data.elapsedMs;
                    drawCells(data.cells, data.maxCount);
                })
                .catch(err => console.error('fetch error:', err));
        }

        function drawCells(cells, maxCount) {
            if (pendingDraw) cancelAnimationFrame(pendingDraw);

            pendingDraw = requestAnimationFrame(() => {
                const activeKeys = new Set();

                for (const cell of cells) {
                    if (cell.cnt === 0) continue;
                    const key = cell.row + '_' + cell.col;
                    activeKeys.add(key);

                    const center = new naver.maps.LatLng(cell.lat, cell.lng);
                    const existing = markerMap.get(key);
                    const circleSize = getCircleSize(cell.cnt, maxCount);

                    if (existing) {
                        existing.setPosition(center);
                        existing.setIcon({
                            content: buildLabelHtml(cell.cnt, circleSize),
                            anchor: new naver.maps.Point(0, 0)
                        });
                    } else {
                        const marker = new naver.maps.Marker({
                            map: map,
                            position: center,
                            icon: {
                                content: buildLabelHtml(cell.cnt, circleSize),
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

        function getCircleSize(cnt, maxCount) {
            if (maxCount === 0) return MIN_CIRCLE_SIZE;
            const ratio = Math.sqrt(cnt / maxCount);
            return Math.round(MIN_CIRCLE_SIZE + (MAX_CIRCLE_SIZE - MIN_CIRCLE_SIZE) * ratio);
        }

        function buildLabelHtml(count, size) {
            const fontSize = Math.max(10, Math.round(size * 0.14));
            return '<div style="position:relative;"><div style="background:#7c3aed;color:#fff;width:' + size + 'px;height:' + size + 'px;border-radius:50%;font-size:' + fontSize + 'px;font-weight:bold;display:flex;align-items:center;justify-content:center;transform:translate(-50%,-50%);box-shadow:0 2px 6px rgba(0,0,0,0.3);">' +
                count.toLocaleString() + '</div></div>';
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
