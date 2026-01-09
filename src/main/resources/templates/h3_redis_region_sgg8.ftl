<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H3 Redis - 시군구 집계 (res 8)</title>
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
        #info h3 { margin-bottom: 8px; color: #ea580c; }
        #info div { margin: 4px 0; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>H3 Redis - 시군구 집계 (res 8)</h3>
        <div>시군구: <span id="regionCount">0</span>개</div>
        <div>총 PNU: <span id="pnuCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 12
        });

        let debounceTimer = null;
        let overlayMap = new Map();
        let pendingDraw = null;

        function fetchData() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat()
            });
            fetch(`/api/h3/redis/region/sgg8?${r"${params}"}`)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('regionCount').textContent = data.regions.length.toLocaleString();
                    document.getElementById('pnuCount').textContent = data.totalCount.toLocaleString();
                    document.getElementById('elapsed').textContent = data.elapsedMs;
                    drawRegions(data.regions);
                })
                .catch(err => console.error('fetch error:', err));
        }

        function drawRegions(regions) {
            if (pendingDraw) cancelAnimationFrame(pendingDraw);

            pendingDraw = requestAnimationFrame(() => {
                const activeKeys = new Set();

                for (const region of regions) {
                    if (region.cnt === 0) continue;
                    activeKeys.add(region.bjdongCd);

                    const center = new naver.maps.LatLng(region.lat, region.lng);
                    const existing = overlayMap.get(region.bjdongCd);

                    if (existing) {
                        existing.marker.setPosition(center);
                        existing.marker.setIcon({
                            content: buildLabelHtml(region.cnt, region.regionName || region.bjdongCd),
                            anchor: new naver.maps.Point(0, 0)
                        });
                    } else {
                        const marker = new naver.maps.Marker({
                            map: map,
                            position: center,
                            icon: {
                                content: buildLabelHtml(region.cnt, region.regionName || region.bjdongCd),
                                anchor: new naver.maps.Point(0, 0)
                            }
                        });
                        overlayMap.set(region.bjdongCd, { marker });
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

        function buildLabelHtml(count, regionName) {
            return '<div style="position:relative;"><div style="background:#ea580c;color:#fff;padding:4px 8px;border-radius:8px;font-size:11px;font-weight:bold;white-space:nowrap;transform:translate(-50%,-50%);text-align:center;">' +
                regionName + '<br>' + count.toLocaleString() + '</div></div>';
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
