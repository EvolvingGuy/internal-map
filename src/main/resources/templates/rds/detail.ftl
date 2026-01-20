<#import "common/indicator.ftl" as ind>
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
        #info h3 { margin-bottom: 8px; color: #059669; }
        #info div { margin: 4px 0; }
        #pnuCount {
            font-size: 24px;
            font-weight: bold;
            color: #059669;
            margin: 8px 0;
        }
<@ind.indicatorStyle/>
    </style>
</head>
<body>
    <div id="map"></div>
<@ind.indicatorHtml/>
    <div id="info">
        <h3>${title}</h3>
        <div>줌레벨: <span id="zoomLevel">0</span></div>
        <div id="pnuCount">0 PNU</div>
        <div>items: <span id="itemCount">0</span>개</div>
        <div>응답시간: <span id="elapsed">0</span>ms</div>
    </div>

    <script>
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 18
        });

        let debounceTimer = null;
        let markers = [];

        function clearMarkers() {
            markers.forEach(m => m.setMap(null));
            markers = [];
        }

        function drawPoints(items) {
            clearMarkers();
            items.forEach(item => {
                const marker = new naver.maps.Marker({
                    map: map,
                    position: new naver.maps.LatLng(item.centerLat, item.centerLng),
                    icon: {
                        content: '<div style="width:8px;height:8px;background:#059669;border-radius:50%;border:1px solid #fff;"></div>',
                        anchor: new naver.maps.Point(4, 4)
                    }
                });
                markers.push(marker);
            });
        }

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
                neLat: ne.lat()
            });

            fetch(`/api/pnu/detail?${'$'}{params}`)
                .then(res => res.json())
                .then(data => {
                    if (data.exceeded) {
                        document.getElementById('pnuCount').textContent = data.pnuCount.toLocaleString() + ' PNU (초과)';
                        document.getElementById('pnuCount').style.color = '#dc2626';
                        clearMarkers();
                    } else {
                        document.getElementById('pnuCount').textContent = data.pnuCount.toLocaleString() + ' PNU';
                        document.getElementById('pnuCount').style.color = '#059669';
                        drawPoints(data.items);
                    }
                    document.getElementById('itemCount').textContent = data.items.length.toLocaleString();
                    document.getElementById('elapsed').textContent = data.elapsedMs;
                })
                .catch(err => console.error('fetch error:', err));
        }

<@ind.indicatorScript/>

        function onMapIdle() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                fetchData();
                fetchIndicator();
            }, 300);
        }

        naver.maps.Event.addListener(map, 'idle', onMapIdle);
        fetchData();
        fetchIndicator();
    </script>
</body>
</html>
