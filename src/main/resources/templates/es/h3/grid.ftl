<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H3 Grid (res ${resolution})</title>
    <script src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapClientId}"></script>
    <script src="https://unpkg.com/h3-js@3.7.2/dist/h3-js.umd.js"></script>
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
        #controls {
            position: absolute;
            top: 10px;
            right: 10px;
            z-index: 1000;
            background: white;
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            font-size: 13px;
        }
        #controls label { font-weight: bold; }
        #resSlider { width: 120px; vertical-align: middle; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>H3 Grid</h3>
        <div>Resolution: <span id="resValue">${resolution}</span></div>
        <div>셀 수: <span id="cellCount">0</span></div>
        <div>렌더링: <span id="elapsed">0</span>ms</div>
    </div>
    <div id="controls">
        <label>Resolution</label><br>
        <input type="range" id="resSlider" min="0" max="9" value="${resolution}">
        <span id="resLabel">${resolution}</span>
    </div>

    <script>
        let currentRes = ${resolution};
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: ${defaultZoom}
        });

        let polygonMap = new Map();
        let debounceTimer = null;

        function drawH3Grid() {
            const start = performance.now();
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const cells = h3.polyfill(
                [[sw.lat(), sw.lng()], [ne.lat(), sw.lng()], [ne.lat(), ne.lng()], [sw.lat(), ne.lng()]],
                currentRes,
                false
            );

            if (cells.length > 5000) {
                clearAll();
                document.getElementById('cellCount').textContent = cells.length.toLocaleString() + ' (too many, zoom in)';
                document.getElementById('elapsed').textContent = Math.round(performance.now() - start);
                return;
            }

            const activeKeys = new Set(cells);

            for (const cellId of cells) {
                if (polygonMap.has(cellId)) continue;

                const boundary = h3.h3ToGeoBoundary(cellId);
                const path = boundary.map(([lat, lng]) => new naver.maps.LatLng(lat, lng));
                path.push(path[0]);

                const polygon = new naver.maps.Polygon({
                    map: map,
                    paths: [path],
                    strokeColor: '#2563eb',
                    strokeWeight: 1,
                    strokeOpacity: 0.6,
                    fillColor: '#3b82f6',
                    fillOpacity: 0.08
                });

                polygonMap.set(cellId, polygon);
            }

            for (const [key, polygon] of polygonMap) {
                if (!activeKeys.has(key)) {
                    polygon.setMap(null);
                    polygonMap.delete(key);
                }
            }

            const elapsed = Math.round(performance.now() - start);
            document.getElementById('cellCount').textContent = cells.length.toLocaleString();
            document.getElementById('elapsed').textContent = elapsed;
        }

        function clearAll() {
            for (const [, polygon] of polygonMap) {
                polygon.setMap(null);
            }
            polygonMap.clear();
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(drawH3Grid, 200);
        }

        const resSlider = document.getElementById('resSlider');
        resSlider.addEventListener('input', function() {
            currentRes = parseInt(this.value);
            document.getElementById('resLabel').textContent = currentRes;
            document.getElementById('resValue').textContent = currentRes;
            clearAll();
            drawH3Grid();
        });

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        drawH3Grid();
    </script>
</body>
</html>
