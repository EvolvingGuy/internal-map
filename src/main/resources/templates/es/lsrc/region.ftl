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
        #info h3 { margin-bottom: 8px; color: #16a34a; }
        #info div { margin: 4px 0; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>${title}</h3>
        <div>행정구역: <span id="regionCount">0</span>개</div>
        <div>총 PNU: <span id="pnuCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 12
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
                neLat: ne.lat()
            });

            fetch(`${apiPath}?${'$'}{params}`)
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
                    const key = String(region.code);
                    activeKeys.add(key);

                    const center = new naver.maps.LatLng(region.centerLat, region.centerLng);
                    const existing = markerMap.get(key);

                    if (existing) {
                        existing.setPosition(center);
                        existing.setIcon({
                            content: buildLabelHtml(region.cnt, region.name),
                            anchor: new naver.maps.Point(0, 0)
                        });
                    } else {
                        const marker = new naver.maps.Marker({
                            map: map,
                            position: center,
                            icon: {
                                content: buildLabelHtml(region.cnt, region.name),
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

        const circleSize = ${circleSize};
        const fontSize = ${fontSize};
        const labelFontSize = Math.max(8, fontSize - 2);

        function buildLabelHtml(count, name) {
            return '<div style="position:relative;"><div style="background:#16a34a;color:#fff;width:' + circleSize + 'px;height:' + circleSize + 'px;border-radius:50%;font-size:' + fontSize + 'px;font-weight:bold;display:flex;flex-direction:column;align-items:center;justify-content:center;transform:translate(-50%,-50%);box-shadow:0 2px 6px rgba(0,0,0,0.3);line-height:1.2;">' +
                '<span style="font-size:' + labelFontSize + 'px;">' + name + '</span><span>' + count.toLocaleString() + '</span></div></div>';
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
