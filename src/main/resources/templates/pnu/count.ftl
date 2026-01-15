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
        #info h3 { margin-bottom: 8px; color: #0891b2; }
        #info div { margin: 4px 0; }
        #countDisplay {
            font-size: 28px;
            font-weight: bold;
            color: #0891b2;
            margin: 8px 0;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>${title}</h3>
        <div>줌레벨: <span id="zoomLevel">0</span></div>
        <div id="countDisplay">0</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 18
        });

        let debounceTimer = null;

        function fetchData() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();
            const zoom = map.getZoom();

            document.getElementById('zoomLevel').textContent = zoom;

            const params = new URLSearchParams({
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat(),
                zoomLevel: zoom
            });

            fetch(`/api/pnu/agg/count?${'$'}{params}`)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('countDisplay').textContent = data.count.toLocaleString() + ' 필지';
                    document.getElementById('elapsed').textContent = data.elapsedMs;
                })
                .catch(err => console.error('fetch error:', err));
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
