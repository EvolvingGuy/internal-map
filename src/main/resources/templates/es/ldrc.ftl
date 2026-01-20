<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ES LDRC - ${levelName}</title>
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
        #nav a.active { background: #2563eb; color: white; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>ES LDRC - ${levelName}</h3>
        <div>H3 셀: <span id="h3Count">0</span>개</div>
        <div>클러스터: <span id="clusterCount">0</span>개</div>
        <div>총 필지: <span id="totalCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>
    <div id="nav">
        <strong>Level</strong>
        <a href="/page/es/ldrc/sd" class="${(level == 'SD')?then('active', '')}">시도 (SD)</a>
        <a href="/page/es/ldrc/sgg" class="${(level == 'SGG')?then('active', '')}">시군구 (SGG)</a>
        <a href="/page/es/ldrc/emd" class="${(level == 'EMD')?then('active', '')}">읍면동 (EMD)</a>
    </div>

    <script>
        const LEVEL = '${level}';

        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: ${defaultZoom}
        });

        let debounceTimer = null;
        let markerMap = new Map();
        let pendingDraw = null;

        function fetchData() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat(),
                level: LEVEL,
                zoom: map.getZoom()
            });

            fetch(`/api/es/ldrc/clusters?${'$'}{params}`)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('h3Count').textContent = data.h3Count.toLocaleString();
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

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        fetchData();
    </script>
</body>
</html>
