<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H3 Map - 읍면동 (res 10)</title>
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
        <h3>읍면동 (H3 res 10)</h3>
        <div>H3 셀: <span id="cellCount">0</span>개</div>
        <div>총 PNU: <span id="pnuCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 16,
            minZoom: 13
        });

        let debounceTimer = null;
        let overlayMap = new Map();
        let pendingDraw = null;

        function fetchH3Data() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat()
            });
            fetch(`/api/h3/cells/emd?${r"${params}"}`)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('cellCount').textContent = data.cells.length.toLocaleString();
                    document.getElementById('pnuCount').textContent = data.totalCount.toLocaleString();
                    document.getElementById('elapsed').textContent = data.elapsedMs;
                    drawCells(data.cells);
                })
                .catch(err => console.error('H3 fetch error:', err));
        }

        function drawCells(cells) {
            if (pendingDraw) cancelAnimationFrame(pendingDraw);

            pendingDraw = requestAnimationFrame(() => {
                const activeKeys = new Set();

                for (const cell of cells) {
                    if (cell.cnt === 0) continue;
                    activeKeys.add(cell.h3Index);

                    const center = new naver.maps.LatLng(cell.lat, cell.lng);
                    const existing = overlayMap.get(cell.h3Index);

                    if (existing) {
                        existing.circle.setCenter(center);
                        existing.marker.setPosition(center);
                        existing.marker.setIcon({
                            content: buildLabelHtml(cell.cnt),
                            anchor: new naver.maps.Point(0, 0)
                        });
                    } else {
                        const circle = new naver.maps.Circle({
                            map: map,
                            center: center,
                            radius: 66,
                            fillColor: '#8b5cf6',
                            fillOpacity: 0.4,
                            strokeColor: '#7c3aed',
                            strokeWeight: 1
                        });
                        const marker = new naver.maps.Marker({
                            map: map,
                            position: center,
                            icon: {
                                content: buildLabelHtml(cell.cnt),
                                anchor: new naver.maps.Point(0, 0)
                            }
                        });
                        overlayMap.set(cell.h3Index, { circle, marker });
                    }
                }

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

        function buildLabelHtml(count) {
            return '<div style="position:relative;"><div style="background:#7c3aed;color:#fff;padding:2px 6px;border-radius:8px;font-size:10px;font-weight:bold;white-space:nowrap;transform:translate(-50%,-50%);">' +
                count.toLocaleString() + '</div></div>';
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fetchH3Data, 300);
        }

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        fetchH3Data();
    </script>
</body>
</html>
