<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H3 - Viewport Grid</title>
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
        .grid-info { color: #6b7280; font-size: 11px; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>Viewport Grid</h3>
        <div>그리드: <span id="gridSize">0x0</span></div>
        <div>클러스터: <span id="cellCount">0</span>개</div>
        <div>총 PNU: <span id="pnuCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
        <div class="grid-info">뷰포트: <span id="viewport">0x0</span>px</div>
        <div class="grid-info">셀 크기: <span id="targetCell">450</span>px</div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 18
        });

        let debounceTimer = null;
        let overlayMap = new Map();
        let pendingDraw = null;
        const TARGET_CELL_SIZE = 450;

        function fetchData() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();
            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;

            document.getElementById('viewport').textContent = viewportWidth + 'x' + viewportHeight;
            document.getElementById('targetCell').textContent = TARGET_CELL_SIZE;

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat(),
                viewportWidth: viewportWidth,
                viewportHeight: viewportHeight,
                targetCellSize: TARGET_CELL_SIZE
            });

            fetch(`/api/h3/jvm/viewport-grid?${r"${params}"}`)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('gridSize').textContent = data.cols + 'x' + data.rows;
                    document.getElementById('cellCount').textContent = data.cells.length.toLocaleString();
                    document.getElementById('pnuCount').textContent = data.totalCount.toLocaleString();
                    document.getElementById('elapsed').textContent = data.elapsedMs;
                    drawCells(data.cells);
                })
                .catch(err => console.error('fetch error:', err));
        }

        function drawCells(cells) {
            if (pendingDraw) cancelAnimationFrame(pendingDraw);

            pendingDraw = requestAnimationFrame(() => {
                const activeKeys = new Set();

                for (const cell of cells) {
                    if (cell.cnt === 0) continue;
                    const key = cell.row + '_' + cell.col;
                    activeKeys.add(key);

                    const center = new naver.maps.LatLng(cell.lat, cell.lng);
                    const existing = overlayMap.get(key);

                    if (existing) {
                        existing.marker.setPosition(center);
                        existing.marker.setIcon({
                            content: buildLabelHtml(cell.cnt),
                            anchor: new naver.maps.Point(0, 0)
                        });
                    } else {
                        const marker = new naver.maps.Marker({
                            map: map,
                            position: center,
                            icon: {
                                content: buildLabelHtml(cell.cnt),
                                anchor: new naver.maps.Point(0, 0)
                            }
                        });
                        overlayMap.set(key, { marker });
                    }
                }

                for (const [key, { marker }] of overlayMap) {
                    if (!activeKeys.has(key)) {
                        marker.setMap(null);
                        overlayMap.delete(key);
                    }
                }

                pendingDraw = null;
            });
        }

        function buildLabelHtml(count) {
            return '<div style="position:relative;"><div style="background:#7c3aed;color:#fff;padding:6px 10px;border-radius:20px;font-size:12px;font-weight:bold;white-space:nowrap;transform:translate(-50%,-50%);text-align:center;min-width:40px;">' +
                count.toLocaleString() + '</div></div>';
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fetchData, 300);
        }

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        window.addEventListener('resize', () => {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fetchData, 300);
        });
        fetchData();
    </script>
</body>
</html>
