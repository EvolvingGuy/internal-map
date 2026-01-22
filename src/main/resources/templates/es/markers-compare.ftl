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
            padding: 12px 14px;
            border-radius: 10px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            font-size: 12px;
            min-width: 220px;
        }
        #info h3 {
            margin-bottom: 16px;
            color: #1f2937;
            font-size: 15px;
            border-bottom: 2px solid #e5e7eb;
            padding-bottom: 8px;
        }
        #info .row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 6px;
        }
        #info .label { color: #6b7280; }
        #info .value { font-weight: 600; color: #111827; }

        .section {
            margin: 12px 0;
            padding: 12px;
            border-radius: 8px;
        }
        .section.center { background: #dbeafe; }
        .section.intersect { background: #dcfce7; }
        .section.diff { background: #fef3c7; }
        .section.time-compare { background: #f3e8ff; }

        .section-title {
            font-weight: 700;
            margin-bottom: 8px;
            font-size: 13px;
        }
        .section.center .section-title { color: #1d4ed8; }
        .section.intersect .section-title { color: #16a34a; }
        .section.diff .section-title { color: #d97706; }
        .section.time-compare .section-title { color: #7c3aed; }

        .sample {
            font-size: 11px;
            color: #6b7280;
            word-break: break-all;
            margin-top: 4px;
        }

        .legend {
            margin-top: 12px;
            padding-top: 12px;
            border-top: 1px solid #e5e7eb;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 4px;
            font-size: 12px;
        }
        .legend-color {
            width: 16px;
            height: 16px;
            border-radius: 3px;
        }
        .legend-color.common { background: #a855f7; }
        .legend-color.only-center { background: #3b82f6; }
        .legend-color.only-intersect { background: #22c55e; }

        #zoom-warning {
            display: none;
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            z-index: 1000;
            background: rgba(0,0,0,0.8);
            color: white;
            padding: 20px 30px;
            border-radius: 8px;
            font-size: 16px;
        }

        .faster { color: #16a34a; font-weight: bold; }
        .slower { color: #dc2626; }
    </style>
</head>
<body>
    <div id="map"></div>

    <div id="info">
        <h3>Center vs Intersect Compare</h3>
        <div class="row">
            <span class="label">Zoom Level</span>
            <span class="value" id="zoomLevel">-</span>
        </div>

        <div class="section center">
            <div class="section-title">Center Contains (geo_bounding_box)</div>
            <div class="row">
                <span class="label">Count</span>
                <span class="value" id="centerCount">-</span>
            </div>
            <div class="row">
                <span class="label">Elapsed</span>
                <span class="value" id="centerElapsed">-</span>
            </div>
            <div class="row">
                <span class="label">Size</span>
                <span class="value" id="centerSize">-</span>
            </div>
        </div>

        <div class="section intersect">
            <div class="section-title">Intersect (geo_shape)</div>
            <div class="row">
                <span class="label">Count</span>
                <span class="value" id="intersectCount">-</span>
            </div>
            <div class="row">
                <span class="label">Elapsed</span>
                <span class="value" id="intersectElapsed">-</span>
            </div>
            <div class="row">
                <span class="label">Size</span>
                <span class="value" id="intersectSize">-</span>
            </div>
        </div>

        <div class="section time-compare">
            <div class="section-title">Compare</div>
            <div class="row">
                <span class="label">Faster</span>
                <span class="value" id="fasterMethod">-</span>
            </div>
            <div class="row">
                <span class="label">Time Diff</span>
                <span class="value" id="timeDiff">-</span>
            </div>
            <div class="row">
                <span class="label">Size Diff</span>
                <span class="value" id="sizeDiff">-</span>
            </div>
        </div>

        <div class="section diff">
            <div class="section-title">Difference</div>
            <div class="row">
                <span class="label">Common</span>
                <span class="value" id="diffCommon">-</span>
            </div>
            <div class="row">
                <span class="label">Only Center</span>
                <span class="value" id="diffOnlyCenter">-</span>
            </div>
            <div class="row">
                <span class="label">Only Intersect</span>
                <span class="value" id="diffOnlyIntersect">-</span>
            </div>
        </div>

        <div class="legend">
            <div class="legend-item">
                <div class="legend-color common"></div>
                <span>Common (both)</span>
            </div>
            <div class="legend-item">
                <div class="legend-color only-center"></div>
                <span>Only Center (NW offset)</span>
            </div>
            <div class="legend-item">
                <div class="legend-color only-intersect"></div>
                <span>Only Intersect</span>
            </div>
        </div>
    </div>

    <div id="zoom-warning">줌 레벨 17 이상에서만 조회됩니다</div>

    <script>
        const MIN_ZOOM = 17;

        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 18
        });

        let debounceTimer = null;
        let polygons = {
            common: [],
            onlyCenter: [],
            onlyIntersect: []
        };

        // 오프셋 (북서 방향으로 이동)
        const NW_OFFSET_LAT = 0.00015;
        const NW_OFFSET_LNG = -0.00015;

        function formatSize(bytes) {
            const kb = (bytes / 1024).toFixed(1);
            const mb = (bytes / 1024 / 1024).toFixed(2);
            return kb + ' KB (' + mb + ' MB)';
        }

        function clearPolygons() {
            Object.values(polygons).forEach(arr => {
                arr.forEach(p => p.setMap(null));
                arr.length = 0;
            });
        }

        function geojsonToPaths(geo, offsetLat = 0, offsetLng = 0) {
            if (!geo || !geo.type) return null;

            if (geo.type === 'Polygon') {
                return geo.coordinates.map(ring =>
                    ring.map(coord => new naver.maps.LatLng(coord[1] + offsetLat, coord[0] + offsetLng))
                );
            } else if (geo.type === 'MultiPolygon') {
                const first = geo.coordinates[0];
                if (!first) return null;
                return first.map(ring =>
                    ring.map(coord => new naver.maps.LatLng(coord[1] + offsetLat, coord[0] + offsetLng))
                );
            }
            return null;
        }

        function drawPolygons(items, type, color, offsetLat = 0, offsetLng = 0) {
            for (const item of items) {
                const geo = item.land?.geometry;
                if (!geo) continue;

                try {
                    const paths = geojsonToPaths(geo, offsetLat, offsetLng);
                    if (!paths || paths.length === 0) continue;

                    const polygon = new naver.maps.Polygon({
                        map: map,
                        paths: paths,
                        fillColor: color,
                        fillOpacity: 0.35,
                        strokeColor: color,
                        strokeWeight: 2,
                        strokeOpacity: 0.8,
                        zIndex: type === 'common' ? 30 : (type === 'onlyCenter' ? 20 : 10)
                    });

                    polygons[type].push(polygon);
                } catch (e) {
                    console.warn('polygon error:', e);
                }
            }
        }

        function fetchCompare() {
            const zoom = map.getZoom();
            document.getElementById('zoomLevel').textContent = zoom;

            if (zoom < MIN_ZOOM) {
                document.getElementById('zoom-warning').style.display = 'block';
                clearInfo();
                clearPolygons();
                return;
            }
            document.getElementById('zoom-warning').style.display = 'none';

            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat()
            });

            // 로딩 표시
            document.getElementById('centerCount').textContent = '...';
            document.getElementById('intersectCount').textContent = '...';

            fetch('/api/markers-compare?' + params.toString())
                .then(res => res.json())
                .then(data => {
                    // 각 items 크기 계산
                    const centerBytes = new Blob([JSON.stringify(data.center.items)]).size;
                    const intersectBytes = new Blob([JSON.stringify(data.intersect.items)]).size;

                    // Center
                    document.getElementById('centerCount').textContent = data.center.count.toLocaleString();
                    document.getElementById('centerElapsed').textContent = data.center.elapsedMs + 'ms';
                    document.getElementById('centerSize').textContent = formatSize(centerBytes);

                    // Intersect
                    document.getElementById('intersectCount').textContent = data.intersect.count.toLocaleString();
                    document.getElementById('intersectElapsed').textContent = data.intersect.elapsedMs + 'ms';
                    document.getElementById('intersectSize').textContent = formatSize(intersectBytes);

                    // Compare
                    const fasterEl = document.getElementById('fasterMethod');
                    if (data.faster === 'center') {
                        fasterEl.innerHTML = '<span class="faster">Center</span>';
                    } else if (data.faster === 'intersect') {
                        fasterEl.innerHTML = '<span class="faster">Intersect</span>';
                    } else {
                        fasterEl.textContent = 'Same';
                    }
                    document.getElementById('timeDiff').textContent = data.timeDiff + 'ms';

                    // Size diff
                    const sizeDiffBytes = Math.abs(centerBytes - intersectBytes);
                    const smaller = centerBytes < intersectBytes ? 'Center' : (intersectBytes < centerBytes ? 'Intersect' : 'Same');
                    document.getElementById('sizeDiff').textContent = formatSize(sizeDiffBytes) + ' (' + smaller + ' smaller)';

                    // Diff
                    document.getElementById('diffCommon').textContent = data.diff.common.toLocaleString();
                    document.getElementById('diffOnlyCenter').textContent = data.diff.onlyCenter.toLocaleString();
                    document.getElementById('diffOnlyIntersect').textContent = data.diff.onlyIntersect.toLocaleString();

                    // Draw polygons
                    clearPolygons();

                    // Common: 보라색
                    drawPolygons(data.diff.commonItems, 'common', '#a855f7');

                    // Only Center: 파란색, 북서쪽으로 오프셋
                    drawPolygons(data.diff.onlyCenterItems, 'onlyCenter', '#3b82f6', NW_OFFSET_LAT, NW_OFFSET_LNG);

                    // Only Intersect: 초록색
                    drawPolygons(data.diff.onlyIntersectItems, 'onlyIntersect', '#22c55e');
                })
                .catch(err => {
                    console.error('compare error:', err);
                    document.getElementById('centerCount').textContent = 'Error';
                    document.getElementById('intersectCount').textContent = 'Error';
                });
        }

        function clearInfo() {
            ['centerCount', 'centerElapsed', 'centerSize',
             'intersectCount', 'intersectElapsed', 'intersectSize',
             'diffCommon', 'diffOnlyCenter', 'diffOnlyIntersect',
             'fasterMethod', 'timeDiff', 'sizeDiff'].forEach(id => {
                document.getElementById(id).textContent = '-';
            });
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fetchCompare, 300);
        }

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        fetchCompare();
    </script>
</body>
</html>
