<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H3 Visualization</title>
    <script src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapClientId}"></script>
    <script src="https://unpkg.com/h3-js@4"></script>
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
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            font-size: 13px;
        }
        #controls h3 { margin-bottom: 8px; color: #2563eb; }
        #controls div { margin: 6px 0; }
        #controls label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
        #controls input[type="checkbox"] { width: 16px; height: 16px; }
        .color-box { width: 16px; height: 16px; border-radius: 3px; display: inline-block; }
        #info {
            position: absolute;
            bottom: 10px;
            left: 10px;
            z-index: 1000;
            background: white;
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            font-size: 12px;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="controls">
        <h3>H3 Resolution</h3>
        <div>
            <label><input type="checkbox" name="res" value="5" checked> <span class="color-box" style="background:#ef4444;"></span> Res 5</label>
        </div>
        <div>
            <label><input type="checkbox" name="res" value="6"> <span class="color-box" style="background:#22c55e;"></span> Res 6</label>
        </div>
    </div>
    <div id="info">
        <div>셀 수: <span id="cellCount">0</span></div>
        <div>렌더링: <span id="renderTime">0</span>ms</div>
        <div id="warning" style="color:#ef4444;display:none;"></div>
    </div>

    <script>
        const RES_COLORS = {
            5: { fill: 'rgba(239, 68, 68, 0.2)', stroke: '#ef4444' },   // red
            6: { fill: 'rgba(34, 197, 94, 0.2)', stroke: '#22c55e' }    // green
        };

        // 한국 영역 범위 (대략적)
        const KOREA_BOUNDS = {
            minLat: 32,
            maxLat: 40,
            minLng: 123,
            maxLng: 133
        };

        function isInKorea(lat, lng) {
            return lat >= KOREA_BOUNDS.minLat && lat <= KOREA_BOUNDS.maxLat &&
                   lng >= KOREA_BOUNDS.minLng && lng <= KOREA_BOUNDS.maxLng;
        }

        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(36.5, 127.5),
            zoom: 7
        });

        let polygons = [];
        let debounceTimer = null;

        function getSelectedResolutions() {
            const checked = document.querySelectorAll('input[name="res"]:checked');
            return Array.from(checked).map(el => parseInt(el.value));
        }

        function clearPolygons() {
            polygons.forEach(p => p.setMap(null));
            polygons = [];
        }

        function drawH3Cells() {
            const startTime = performance.now();
            clearPolygons();

            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            console.log('bounds:', sw.lat(), sw.lng(), ne.lat(), ne.lng());

            const resolutions = getSelectedResolutions();
            let drawnCells = 0;
            let warnings = [];

            for (const res of resolutions) {
                // 뷰포트를 한국 범위로 클램핑
                const clampedSw = {
                    lat: Math.max(sw.lat(), KOREA_BOUNDS.minLat),
                    lng: Math.max(sw.lng(), KOREA_BOUNDS.minLng)
                };
                const clampedNe = {
                    lat: Math.min(ne.lat(), KOREA_BOUNDS.maxLat),
                    lng: Math.min(ne.lng(), KOREA_BOUNDS.maxLng)
                };

                // 한국 범위 밖이면 스킵
                if (clampedSw.lat >= clampedNe.lat || clampedSw.lng >= clampedNe.lng) {
                    console.log('뷰포트가 한국 범위 밖');
                    continue;
                }

                // 클램핑된 뷰포트로 H3 셀 구하기 (GeoJSON: [lng, lat] 순서)
                const poly = [
                    [clampedSw.lng, clampedSw.lat],
                    [clampedNe.lng, clampedSw.lat],
                    [clampedNe.lng, clampedNe.lat],
                    [clampedSw.lng, clampedNe.lat],
                    [clampedSw.lng, clampedSw.lat]
                ];

                const h3Indexes = h3.polygonToCells([poly], res, true);
                console.log('Res', res, '- 뷰포트 H3 셀:', h3Indexes.length);

                // 너무 많으면 스킵 (res별 제한)
                const limit = res === 5 ? 500 : 300;
                if (h3Indexes.length > limit) {
                    warnings.push('Res ' + res + ': ' + h3Indexes.length + '개 (줌인 필요)');
                    continue;
                }

                const colors = RES_COLORS[res];

                for (const h3Index of h3Indexes) {
                    const boundary = h3.cellToBoundary(h3Index);
                    const paths = boundary.map(([lat, lng]) => new naver.maps.LatLng(lat, lng));

                    const newPolygon = new naver.maps.Polygon({
                        map: map,
                        paths: paths,
                        fillColor: colors.fill,
                        fillOpacity: 0.3,
                        strokeColor: colors.stroke,
                        strokeWeight: 1,
                        strokeOpacity: 0.8
                    });

                    polygons.push(newPolygon);
                    drawnCells++;
                }
            }

            const elapsed = Math.round(performance.now() - startTime);
            document.getElementById('cellCount').textContent = drawnCells.toLocaleString();
            document.getElementById('renderTime').textContent = elapsed;

            const warningEl = document.getElementById('warning');
            if (warnings.length > 0) {
                warningEl.textContent = warnings.join(', ');
                warningEl.style.display = 'block';
            } else {
                warningEl.style.display = 'none';
            }
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(drawH3Cells, 300);
        }

        document.querySelectorAll('input[name="res"]').forEach(el => {
            el.addEventListener('change', drawH3Cells);
        });

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        drawH3Cells();
    </script>
</body>
</html>
