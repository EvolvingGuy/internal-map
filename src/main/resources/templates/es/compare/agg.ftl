<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <script src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapClientId}"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; }
        #map { flex: 1; height: 100vh; }

        #info {
            position: absolute;
            top: 10px;
            left: 10px;
            z-index: 1000;
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
            font-size: 14px;
            min-width: 460px;
        }
        #info h3 {
            margin-bottom: 12px;
            color: #1e293b;
            font-size: 18px;
            border-bottom: 3px solid #e2e8f0;
            padding-bottom: 10px;
        }

        .compare-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
        }
        .compare-table th, .compare-table td {
            padding: 10px 14px;
            text-align: right;
            border-bottom: 1px solid #e2e8f0;
        }
        .compare-table th {
            background: #f8fafc;
            font-weight: 600;
            color: #475569;
        }
        .compare-table th:first-child,
        .compare-table td:first-child {
            text-align: left;
        }
        .compare-table tr:hover { background: #f1f5f9; }

        .idx-lsrc { color: #16a34a; font-weight: bold; }
        .idx-ldrc { color: #ea580c; font-weight: bold; }
        .idx-lbt { color: #dc2626; font-weight: bold; }

        .best { background: #dcfce7 !important; }
        .worst { background: #fee2e2 !important; }
        .loading { color: #94a3b8; }

        #nav {
            position: absolute;
            top: 10px;
            right: 10px;
            z-index: 1000;
            background: white;
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            font-size: 12px;
        }
        #nav a {
            display: block;
            padding: 6px 12px;
            margin: 4px 0;
            text-decoration: none;
            color: #333;
            border-radius: 4px;
        }
        #nav a:hover { background: #f0f0f0; }
        #nav a.active { background: #1e293b; color: white; }

        .marker-toggle {
            margin-top: 12px;
            display: flex;
            gap: 12px;
        }
        .marker-toggle label {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 13px;
            cursor: pointer;
        }
        .marker-toggle input { width: 16px; height: 16px; }
        .cb-lsrc { accent-color: #16a34a; }
        .cb-ldrc { accent-color: #ea580c; }
        .cb-lbt { accent-color: #dc2626; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>${title}</h3>
        <table class="compare-table">
            <thead>
                <tr>
                    <th>Metric</th>
                    <th class="idx-lsrc">LSRC</th>
                    <th class="idx-ldrc">LDRC</th>
                    <th class="idx-lbt">LBT 17x4</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>Server Time (ms)</td>
                    <td id="lsrc-serverTime" class="loading">-</td>
                    <td id="ldrc-serverTime" class="loading">-</td>
                    <td id="lbt-serverTime" class="loading">-</td>
                </tr>
                <tr>
                    <td>Round Trip (ms)</td>
                    <td id="lsrc-roundTrip" class="loading">-</td>
                    <td id="ldrc-roundTrip" class="loading">-</td>
                    <td id="lbt-roundTrip" class="loading">-</td>
                </tr>
                <tr>
                    <td>Payload (KB)</td>
                    <td id="lsrc-payload" class="loading">-</td>
                    <td id="ldrc-payload" class="loading">-</td>
                    <td id="lbt-payload" class="loading">-</td>
                </tr>
                <tr>
                    <td>Total Count</td>
                    <td id="lsrc-totalCount" class="loading">-</td>
                    <td id="ldrc-totalCount" class="loading">-</td>
                    <td id="lbt-totalCount" class="loading">-</td>
                </tr>
                <tr>
                    <td>Region Count</td>
                    <td id="lsrc-regionCount" class="loading">-</td>
                    <td id="ldrc-regionCount" class="loading">-</td>
                    <td id="lbt-regionCount" class="loading">-</td>
                </tr>
            </tbody>
        </table>
        <div class="marker-toggle">
            <label><input type="checkbox" id="showLsrc" class="cb-lsrc" checked onchange="toggleMarkers()"> <span class="idx-lsrc">LSRC</span></label>
            <label><input type="checkbox" id="showLdrc" class="cb-ldrc" checked onchange="toggleMarkers()"> <span class="idx-ldrc">LDRC</span></label>
            <label><input type="checkbox" id="showLbt" class="cb-lbt" checked onchange="toggleMarkers()"> <span class="idx-lbt">LBT 17x4</span></label>
        </div>
    </div>

    <div id="nav">
        <strong>Level</strong>
        <a href="/page/es/compare/agg/sd">시도 (SD)</a>
        <a href="/page/es/compare/agg/sgg">시군구 (SGG)</a>
        <a href="/page/es/compare/agg/emd">읍면동 (EMD)</a>
    </div>

    <script>
        const lsrcApiPath = '${lsrcApiPath}';
        const ldrcApiPath = '${ldrcApiPath}';
        const lbtApiPath = '${lbtApiPath}';
        const allIndexes = ['lsrc', 'ldrc', 'lbt'];

        const COLORS = {
            lsrc: '#16a34a',
            ldrc: '#ea580c',
            lbt:  '#dc2626'
        };

        // 마커 오프셋 (겹침 방지)
        const OFFSETS = {
            lsrc: { lat: 0, lng: -0.012 },
            ldrc: { lat: 0, lng:  0 },
            lbt:  { lat: 0, lng:  0.012 }
        };

        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(36.5, 127.5),
            zoom: ${defaultZoom}
        });

        document.querySelectorAll('#nav a').forEach(a => {
            if (window.location.pathname === a.getAttribute('href')) {
                a.classList.add('active');
            }
        });

        let debounceTimer = null;

        // 인덱스별 마커 저장
        const markerMaps = { lsrc: new Map(), ldrc: new Map(), lbt: new Map() };
        // 최신 응답 데이터 저장
        const latestData = { lsrc: null, ldrc: null, lbt: null };

        function clearAllCells() {
            allIndexes.forEach(idx => {
                ['serverTime', 'roundTrip', 'payload', 'totalCount', 'regionCount'].forEach(metric => {
                    const el = document.getElementById(idx + '-' + metric);
                    el.textContent = '-';
                    el.classList.remove('best', 'worst');
                    el.classList.add('loading');
                });
            });
        }

        function updateCell(idx, metric, formatted) {
            const el = document.getElementById(idx + '-' + metric);
            el.textContent = formatted;
            el.classList.remove('loading');
        }

        function highlightBestWorst(metric, values) {
            const validEntries = Object.entries(values).filter(([k, v]) => v !== undefined && v !== null);
            if (validEntries.length < 2) return;
            const sorted = validEntries.sort((a, b) => a[1] - b[1]);
            const bestIdx = sorted[0][0];
            const worstIdx = sorted[sorted.length - 1][0];
            allIndexes.forEach(idx => {
                const el = document.getElementById(idx + '-' + metric);
                el.classList.remove('best', 'worst');
                if (idx === bestIdx) el.classList.add('best');
                if (idx === worstIdx && bestIdx !== worstIdx) el.classList.add('worst');
            });
        }

        function buildMarkerHtml(idx, name, count) {
            const color = COLORS[idx];
            const label = idx.toUpperCase();
            return '<div style="position:relative;"><div style="background:' + color + ';color:#fff;min-width:70px;padding:6px 10px;border-radius:16px;font-size:11px;font-weight:bold;text-align:center;transform:translate(-50%,-50%);box-shadow:0 2px 6px rgba(0,0,0,0.3);line-height:1.3;border:2px solid rgba(255,255,255,0.8);">' +
                '<span style="font-size:9px;opacity:0.85;">' + label + '</span><br>' +
                name + '<br>' + count.toLocaleString() + '</div></div>';
        }

        function drawMarkers(idx, regions) {
            const mmap = markerMaps[idx];
            const offset = OFFSETS[idx];
            const visible = document.getElementById('show' + idx.charAt(0).toUpperCase() + idx.slice(1)).checked;
            const activeKeys = new Set();

            for (const region of regions) {
                const count = region.cnt || region.count || 0;
                if (count === 0) continue;

                const key = String(region.code);
                activeKeys.add(key);

                const lat = (region.centerLat || 0) + offset.lat;
                const lng = (region.centerLng || 0) + offset.lng;
                const center = new naver.maps.LatLng(lat, lng);
                const name = region.name || String(region.code);
                const existing = mmap.get(key);

                if (existing) {
                    existing.setPosition(center);
                    existing.setIcon({
                        content: buildMarkerHtml(idx, name, count),
                        anchor: new naver.maps.Point(0, 0)
                    });
                    existing.setVisible(visible);
                } else {
                    const marker = new naver.maps.Marker({
                        map: map,
                        position: center,
                        icon: {
                            content: buildMarkerHtml(idx, name, count),
                            anchor: new naver.maps.Point(0, 0)
                        },
                        visible: visible
                    });
                    mmap.set(key, marker);
                }
            }

            for (const [key, marker] of mmap) {
                if (!activeKeys.has(key)) {
                    marker.setMap(null);
                    mmap.delete(key);
                }
            }
        }

        function toggleMarkers() {
            allIndexes.forEach(idx => {
                const cb = document.getElementById('show' + idx.charAt(0).toUpperCase() + idx.slice(1));
                const visible = cb.checked;
                for (const [, marker] of markerMaps[idx]) {
                    marker.setVisible(visible);
                }
            });
        }

        function normalizeRegions(idx, data) {
            if (idx === 'lsrc') {
                return data.regions || [];
            } else if (idx === 'ldrc') {
                return (data.clusters || []).map(c => ({
                    code: c.code,
                    name: c.name,
                    count: c.count,
                    centerLat: c.centerLat,
                    centerLng: c.centerLng
                }));
            } else {
                return (data.regions || []).map(r => ({
                    code: r.code,
                    name: r.name,
                    count: r.count,
                    centerLat: r.centerLat,
                    centerLng: r.centerLng
                }));
            }
        }

        async function fetchAll() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const bboxParams = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat()
            });

            clearAllCells();

            const apiConfigs = [
                { idx: 'lsrc', url: lsrcApiPath + '?' + bboxParams },
                { idx: 'ldrc', url: ldrcApiPath + '&' + bboxParams },
                { idx: 'lbt',  url: lbtApiPath  + '?' + bboxParams }
            ];

            const fetchPromises = apiConfigs.map(async (config) => {
                const startTime = performance.now();
                try {
                    const response = await fetch(config.url);
                    const text = await response.text();
                    const roundTrip = Math.round(performance.now() - startTime);
                    const payload = new Blob([text]).size;
                    const data = JSON.parse(text);
                    return { idx: config.idx, success: true, data, roundTrip, payload };
                } catch (err) {
                    console.error(config.idx + ' fetch error:', err);
                    return { idx: config.idx, success: false };
                }
            });

            const results = await Promise.all(fetchPromises);

            const metrics = {
                serverTime: {},
                roundTrip: {},
                payload: {},
                totalCount: {},
                regionCount: {}
            };

            results.forEach(result => {
                const idx = result.idx;
                if (!result.success) {
                    ['serverTime', 'roundTrip', 'payload', 'totalCount', 'regionCount'].forEach(m => {
                        document.getElementById(idx + '-' + m).textContent = 'ERR';
                        document.getElementById(idx + '-' + m).classList.remove('loading');
                    });
                    return;
                }

                const data = result.data;
                let serverTime, totalCount, regionCount;

                if (idx === 'lsrc') {
                    serverTime = data.elapsedMs;
                    totalCount = data.totalCount;
                    regionCount = data.regions ? data.regions.length : 0;
                } else if (idx === 'ldrc') {
                    serverTime = data.elapsedMs;
                    totalCount = data.totalCount;
                    regionCount = data.clusters ? data.clusters.length : 0;
                } else {
                    serverTime = data.elapsedMs;
                    totalCount = data.totalCount;
                    regionCount = data.regionCount || (data.regions ? data.regions.length : 0);
                }

                metrics.serverTime[idx] = serverTime;
                metrics.roundTrip[idx] = result.roundTrip;
                metrics.payload[idx] = result.payload;
                metrics.totalCount[idx] = totalCount;
                metrics.regionCount[idx] = regionCount;

                updateCell(idx, 'serverTime', serverTime != null ? serverTime.toLocaleString() : '-');
                updateCell(idx, 'roundTrip', result.roundTrip.toLocaleString());
                updateCell(idx, 'payload', (result.payload / 1024).toFixed(1));
                updateCell(idx, 'totalCount', totalCount != null ? totalCount.toLocaleString() : '-');
                updateCell(idx, 'regionCount', regionCount != null ? regionCount.toLocaleString() : '-');

                // 마커 그리기
                const regions = normalizeRegions(idx, data);
                latestData[idx] = regions;
                drawMarkers(idx, regions);
            });

            highlightBestWorst('serverTime', metrics.serverTime);
            highlightBestWorst('roundTrip', metrics.roundTrip);
            highlightBestWorst('payload', metrics.payload);
        }

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fetchAll, 300);
        }

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        fetchAll();
    </script>
</body>
</html>
