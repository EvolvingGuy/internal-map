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
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            font-size: 13px;
            min-width: 200px;
        }
        #info h3 { margin-bottom: 12px; color: #1f2937; }
        #info .section { margin: 8px 0; padding: 8px; border-radius: 4px; }
        #info .section.type1 { background: #dbeafe; }
        #info .section.type2 { background: #ffedd5; }
        #info .section-title { font-weight: 600; margin-bottom: 4px; }
        #info .section.type1 .section-title { color: #1d4ed8; }
        #info .section.type2 .section-title { color: #ea580c; }
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

        /* Sidebar */
        #sidebar {
            width: 360px;
            height: 100vh;
            overflow-y: auto;
            background: #f8fafc;
            border-left: 1px solid #e2e8f0;
            padding: 16px;
        }
        #sidebar h2 {
            font-size: 16px;
            margin-bottom: 12px;
            color: #1e293b;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        #resetBtn {
            font-size: 12px;
            padding: 4px 8px;
            background: #ef4444;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        #resetBtn:hover { background: #dc2626; }

        .filter-section {
            background: white;
            border-radius: 8px;
            padding: 12px;
            margin-bottom: 12px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .filter-section h3 {
            font-size: 14px;
            color: #475569;
            margin-bottom: 10px;
            padding-bottom: 6px;
            border-bottom: 1px solid #e2e8f0;
        }
        .filter-section.registration h3 { color: #7c3aed; }
        .filter-group {
            margin-bottom: 10px;
        }
        .filter-group label {
            display: block;
            font-size: 12px;
            color: #64748b;
            margin-bottom: 4px;
        }
        .filter-group input[type="number"],
        .filter-group input[type="date"] {
            width: 100%;
            padding: 6px 8px;
            border: 1px solid #cbd5e1;
            border-radius: 4px;
            font-size: 12px;
        }
        .range-inputs {
            display: flex;
            gap: 8px;
        }
        .range-inputs input { flex: 1; }
        .checkbox-group {
            display: flex;
            flex-wrap: wrap;
            gap: 4px;
        }
        .checkbox-item {
            display: flex;
            align-items: center;
            padding: 4px 8px;
            background: #f1f5f9;
            border-radius: 4px;
            font-size: 11px;
            cursor: pointer;
            transition: all 0.2s;
        }
        .checkbox-item:hover { background: #e2e8f0; }
        .checkbox-item.checked { background: #059669; color: white; }
        .checkbox-item input { display: none; }
        .single-checkbox {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
            cursor: pointer;
        }
        .single-checkbox input { width: 14px; height: 14px; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>Markers (Land + Registration)</h3>
        <div>줌 레벨: <span id="zoomLevel">-</span></div>
        <div class="section type1">
            <div class="section-title">Type1 (Land 우선)</div>
            <div>마커: <span id="type1Count">0</span>개</div>
            <div>응답: <span id="type1Elapsed">0</span>ms</div>
            <div>용량: <span id="type1Size">0</span></div>
        </div>
        <div class="section type2">
            <div class="section-title">Type2 (Reg 우선)</div>
            <div>마커: <span id="type2Count">0</span>개</div>
            <div>응답: <span id="type2Elapsed">0</span>ms</div>
            <div>용량: <span id="type2Size">0</span></div>
        </div>
    </div>
    <div id="zoom-warning">줌 레벨 17 이상에서만 마커가 표시됩니다</div>

    <div id="sidebar">
        <h2>Filters <button id="resetBtn">Reset All</button></h2>

        <!-- Registration Section -->
        <div class="filter-section registration">
            <h3>Registration (등기)</h3>
            <div class="filter-group">
                <label>Created Date (From)</label>
                <input type="date" id="minCreatedDate">
            </div>
            <div class="filter-group">
                <label>Created Date (To)</label>
                <input type="date" id="maxCreatedDate">
            </div>
        </div>

        <!-- Building Section -->
        <div class="filter-section">
            <h3>Building</h3>
            <div class="filter-group">
                <label>Main Purpose</label>
                <div class="checkbox-group" id="buildingMainPurpsCdNm">
                    <label class="checkbox-item"><input type="checkbox" value="단독주택">단독주택</label>
                    <label class="checkbox-item"><input type="checkbox" value="제2종근린생활시설">제2종근린</label>
                    <label class="checkbox-item"><input type="checkbox" value="제1종근린생활시설">제1종근린</label>
                    <label class="checkbox-item"><input type="checkbox" value="공동주택">공동주택</label>
                    <label class="checkbox-item"><input type="checkbox" value="창고시설">창고시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="공장">공장</label>
                    <label class="checkbox-item"><input type="checkbox" value="업무시설">업무시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="숙박시설">숙박시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="판매시설">판매시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="노유자시설">노유자시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="교육연구시설">교육연구</label>
                    <label class="checkbox-item"><input type="checkbox" value="운동시설">운동시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="동물및식물관련시설">동식물시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="위험물저장및처리시설">위험물</label>
                    <label class="checkbox-item"><input type="checkbox" value="자동차관련시설">자동차</label>
                </div>
            </div>
            <div class="filter-group">
                <label>Register Type</label>
                <div class="checkbox-group" id="buildingRegstrGbCdNm">
                    <label class="checkbox-item"><input type="checkbox" value="일반">일반</label>
                    <label class="checkbox-item"><input type="checkbox" value="집합">집합</label>
                </div>
            </div>
            <div class="filter-group">
                <label class="single-checkbox"><input type="checkbox" id="buildingPmsDayRecent5y"> Permit within 5 years</label>
            </div>
            <div class="filter-group">
                <label class="single-checkbox"><input type="checkbox" id="buildingStcnsDayRecent5y"> Construction start within 5 years</label>
            </div>
            <div class="filter-group">
                <label>Approval Year</label>
                <div class="range-inputs">
                    <input type="number" id="buildingUseAprDayStart" placeholder="From">
                    <input type="number" id="buildingUseAprDayEnd" placeholder="To">
                </div>
            </div>
            <div class="filter-group">
                <label>Total Area (m²)</label>
                <div class="range-inputs">
                    <input type="number" id="buildingTotAreaMin" placeholder="Min">
                    <input type="number" id="buildingTotAreaMax" placeholder="Max">
                </div>
            </div>
            <div class="filter-group">
                <label>Site Area (m²)</label>
                <div class="range-inputs">
                    <input type="number" id="buildingPlatAreaMin" placeholder="Min">
                    <input type="number" id="buildingPlatAreaMax" placeholder="Max">
                </div>
            </div>
            <div class="filter-group">
                <label>Building Area (m²)</label>
                <div class="range-inputs">
                    <input type="number" id="buildingArchAreaMin" placeholder="Min">
                    <input type="number" id="buildingArchAreaMax" placeholder="Max">
                </div>
            </div>
        </div>

        <!-- Land Section -->
        <div class="filter-section">
            <h3>Land</h3>
            <div class="filter-group">
                <label>Zoning (jiyukCd1)</label>
                <div class="checkbox-group" id="landJiyukCd1">
                    <label class="checkbox-item"><input type="checkbox" value="64">계획관리</label>
                    <label class="checkbox-item"><input type="checkbox" value="71">농림</label>
                    <label class="checkbox-item"><input type="checkbox" value="63">생산관리</label>
                    <label class="checkbox-item"><input type="checkbox" value="41">제1종일반주거</label>
                    <label class="checkbox-item"><input type="checkbox" value="42">제2종일반주거</label>
                    <label class="checkbox-item"><input type="checkbox" value="43">제3종일반주거</label>
                    <label class="checkbox-item"><input type="checkbox" value="31">중심상업</label>
                    <label class="checkbox-item"><input type="checkbox" value="32">일반상업</label>
                    <label class="checkbox-item"><input type="checkbox" value="21">전용공업</label>
                    <label class="checkbox-item"><input type="checkbox" value="22">일반공업</label>
                    <label class="checkbox-item"><input type="checkbox" value="23">준공업</label>
                    <label class="checkbox-item"><input type="checkbox" value="72">자연녹지</label>
                </div>
            </div>
            <div class="filter-group">
                <label>Land Category (jimokCd)</label>
                <div class="checkbox-group" id="landJimokCd">
                    <label class="checkbox-item"><input type="checkbox" value="02">답</label>
                    <label class="checkbox-item"><input type="checkbox" value="08">대</label>
                    <label class="checkbox-item"><input type="checkbox" value="01">전</label>
                    <label class="checkbox-item"><input type="checkbox" value="05">임야</label>
                    <label class="checkbox-item"><input type="checkbox" value="07">도로</label>
                    <label class="checkbox-item"><input type="checkbox" value="18">잡종지</label>
                    <label class="checkbox-item"><input type="checkbox" value="04">과수원</label>
                    <label class="checkbox-item"><input type="checkbox" value="12">공장용지</label>
                    <label class="checkbox-item"><input type="checkbox" value="17">창고용지</label>
                </div>
            </div>
            <div class="filter-group">
                <label>Land Area (m²)</label>
                <div class="range-inputs">
                    <input type="number" id="landAreaMin" placeholder="Min">
                    <input type="number" id="landAreaMax" placeholder="Max">
                </div>
            </div>
            <div class="filter-group">
                <label>Land Price (won/m²)</label>
                <div class="range-inputs">
                    <input type="number" id="landPriceMin" placeholder="Min">
                    <input type="number" id="landPriceMax" placeholder="Max">
                </div>
            </div>
        </div>

        <!-- Trade Section -->
        <div class="filter-section">
            <h3>Real Estate Trade</h3>
            <div class="filter-group">
                <label>Property Type</label>
                <div class="checkbox-group" id="tradeProperty">
                    <label class="checkbox-item"><input type="checkbox" value="LAND">LAND</label>
                    <label class="checkbox-item"><input type="checkbox" value="SINGLE">SINGLE</label>
                    <label class="checkbox-item"><input type="checkbox" value="MULTI">MULTI</label>
                    <label class="checkbox-item"><input type="checkbox" value="APARTMENT">APARTMENT</label>
                    <label class="checkbox-item"><input type="checkbox" value="OFFICETEL">OFFICETEL</label>
                    <label class="checkbox-item"><input type="checkbox" value="COMMERCIAL_BUILDING">COMMERCIAL</label>
                    <label class="checkbox-item"><input type="checkbox" value="FACTORY_WAREHOUSE">FACTORY</label>
                    <label class="checkbox-item"><input type="checkbox" value="SHOPPING_AND_OFFICE">SHOPPING</label>
                </div>
            </div>
            <div class="filter-group">
                <label>Contract Date</label>
                <div class="range-inputs">
                    <input type="date" id="tradeContractDateStart">
                    <input type="date" id="tradeContractDateEnd">
                </div>
            </div>
            <div class="filter-group">
                <label>Trade Amount (won)</label>
                <div class="range-inputs">
                    <input type="number" id="tradeEffectiveAmountMin" placeholder="Min">
                    <input type="number" id="tradeEffectiveAmountMax" placeholder="Max">
                </div>
            </div>
            <div class="filter-group">
                <label>Building Price (won/m²)</label>
                <div class="range-inputs">
                    <input type="number" id="tradeBuildingAmountPerM2Min" placeholder="Min">
                    <input type="number" id="tradeBuildingAmountPerM2Max" placeholder="Max">
                </div>
            </div>
            <div class="filter-group">
                <label>Land Price (won/m²)</label>
                <div class="range-inputs">
                    <input type="number" id="tradeLandAmountPerM2Min" placeholder="Min">
                    <input type="number" id="tradeLandAmountPerM2Max" placeholder="Max">
                </div>
            </div>
        </div>
    </div>

    <script>
        const MIN_ZOOM = 17;

        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(37.5665, 126.9780),
            zoom: 18
        });

        let debounceTimer = null;
        let type1Markers = new Map();
        let type2Markers = new Map();
        let polygons = new Map();
        let infoWindows = new Map();

        // Checkbox styling
        document.querySelectorAll('.checkbox-group input[type="checkbox"]').forEach(cb => {
            cb.addEventListener('change', function() {
                this.closest('.checkbox-item').classList.toggle('checked', this.checked);
                onFilterChange();
            });
        });

        // Range/single inputs
        document.querySelectorAll('#sidebar input[type="number"], #sidebar input[type="date"]').forEach(input => {
            input.addEventListener('input', onFilterChange);
        });
        document.querySelectorAll('#sidebar > .filter-section input[type="checkbox"][id]').forEach(cb => {
            cb.addEventListener('change', onFilterChange);
        });

        function onFilterChange() {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fetchData, 300);
        }

        // Reset button
        document.getElementById('resetBtn').addEventListener('click', function() {
            document.querySelectorAll('#sidebar input[type="checkbox"]').forEach(cb => {
                cb.checked = false;
                const item = cb.closest('.checkbox-item');
                if (item) item.classList.remove('checked');
            });
            document.querySelectorAll('#sidebar input[type="number"], #sidebar input[type="date"]').forEach(input => {
                input.value = '';
            });
            fetchData();
        });

        function getCheckedValues(groupId) {
            const group = document.getElementById(groupId);
            if (!group) return [];
            return Array.from(group.querySelectorAll('input:checked')).map(cb => cb.value);
        }

        function formatSize(bytes) {
            const kb = (bytes / 1024).toFixed(1);
            const mb = (bytes / 1024 / 1024).toFixed(2);
            return kb + ' KB (' + mb + ' MB)';
        }

        function buildParams() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();

            const params = {
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat(),
                userId: 3
            };

            // Registration filters
            const minCreated = document.getElementById('minCreatedDate').value;
            const maxCreated = document.getElementById('maxCreatedDate').value;
            if (minCreated) params.minCreatedDate = minCreated;
            if (maxCreated) params.maxCreatedDate = maxCreated;

            // Building filters
            const mainPurps = getCheckedValues('buildingMainPurpsCdNm');
            if (mainPurps.length) params.buildingMainPurpsCdNm = mainPurps.join(',');

            const regstr = getCheckedValues('buildingRegstrGbCdNm');
            if (regstr.length) params.buildingRegstrGbCdNm = regstr.join(',');

            if (document.getElementById('buildingPmsDayRecent5y').checked) params.buildingPmsDayRecent5y = true;
            if (document.getElementById('buildingStcnsDayRecent5y').checked) params.buildingStcnsDayRecent5y = true;

            ['buildingUseAprDayStart', 'buildingUseAprDayEnd', 'buildingTotAreaMin', 'buildingTotAreaMax',
             'buildingPlatAreaMin', 'buildingPlatAreaMax', 'buildingArchAreaMin', 'buildingArchAreaMax'].forEach(id => {
                const val = document.getElementById(id).value;
                if (val) params[id] = val;
            });

            // Land filters
            const jiyuk = getCheckedValues('landJiyukCd1');
            if (jiyuk.length) params.landJiyukCd1 = jiyuk.join(',');

            const jimok = getCheckedValues('landJimokCd');
            if (jimok.length) params.landJimokCd = jimok.join(',');

            ['landAreaMin', 'landAreaMax', 'landPriceMin', 'landPriceMax'].forEach(id => {
                const val = document.getElementById(id).value;
                if (val) params[id] = val;
            });

            // Trade filters
            const tradeProp = getCheckedValues('tradeProperty');
            if (tradeProp.length) params.tradeProperty = tradeProp.join(',');

            const contractStart = document.getElementById('tradeContractDateStart').value;
            const contractEnd = document.getElementById('tradeContractDateEnd').value;
            if (contractStart) params.tradeContractDateStart = contractStart;
            if (contractEnd) params.tradeContractDateEnd = contractEnd;

            ['tradeEffectiveAmountMin', 'tradeEffectiveAmountMax', 'tradeBuildingAmountPerM2Min', 'tradeBuildingAmountPerM2Max', 'tradeLandAmountPerM2Min', 'tradeLandAmountPerM2Max'].forEach(id => {
                const val = document.getElementById(id).value;
                if (val) params[id] = val;
            });

            return Object.entries(params)
                .map(([k, v]) => encodeURIComponent(k) + '=' + encodeURIComponent(v))
                .join('&');
        }

        function fetchData() {
            const zoom = map.getZoom();
            document.getElementById('zoomLevel').textContent = zoom;

            if (zoom < MIN_ZOOM) {
                document.getElementById('zoom-warning').style.display = 'block';
                clearAllMarkers();
                return;
            }
            document.getElementById('zoom-warning').style.display = 'none';

            const queryString = buildParams();

            // Type 1 호출 (폴리곤도 같이 그림) - markers-geo API
            fetch('/api/markers-geo/type1?' + queryString)
                .then(res => res.text())
                .then(text => {
                    const bytes = new Blob([text]).size;
                    document.getElementById('type1Size').textContent = formatSize(bytes);
                    const data = JSON.parse(text);
                    document.getElementById('type1Count').textContent = data.totalCount.toLocaleString();
                    document.getElementById('type1Elapsed').textContent = data.elapsedMs;
                    drawMarkers(data.items, 'type1');
                    drawPolygons(data.items);
                })
                .catch(err => console.error('type1 error:', err));

            // Type 2 호출 - markers-geo API
            fetch('/api/markers-geo/type2?' + queryString)
                .then(res => res.text())
                .then(text => {
                    const bytes = new Blob([text]).size;
                    document.getElementById('type2Size').textContent = formatSize(bytes);
                    const data = JSON.parse(text);
                    document.getElementById('type2Count').textContent = data.totalCount.toLocaleString();
                    document.getElementById('type2Elapsed').textContent = data.elapsedMs;
                    drawMarkers(data.items, 'type2');
                })
                .catch(err => console.error('type2 error:', err));
        }

        function drawMarkers(items, type) {
            const markerMap = type === 'type1' ? type1Markers : type2Markers;
            const activeKeys = new Set();

            // 좌우 오프셋 (경도 기준, 약 10m 정도)
            const lngOffset = type === 'type1' ? -0.0001 : 0.0001;

            for (const item of items) {
                const key = item.pnu;

                // 등기 없으면 마커 안 그림 (폴리곤만)
                if (!item.registration) continue;

                activeKeys.add(key);

                const center = new naver.maps.LatLng(item.center.lat, item.center.lon + lngOffset);
                const existing = markerMap.get(key);

                if (existing) {
                    existing.setPosition(center);
                } else {
                    const marker = new naver.maps.Marker({
                        map: map,
                        position: center,
                        icon: {
                            content: buildMarkerHtml(item, type),
                            anchor: new naver.maps.Point(24, 24)
                        },
                        zIndex: type === 'type1' ? 100 : 50
                    });

                    naver.maps.Event.addListener(marker, 'click', () => {
                        showInfoWindow(item, marker);
                    });

                    markerMap.set(key, marker);
                }
            }

            // 사라진 마커 제거
            for (const [key, marker] of markerMap) {
                if (!activeKeys.has(key)) {
                    marker.setMap(null);
                    markerMap.delete(key);
                }
            }
        }

        function drawPolygons(items) {
            const activeKeys = new Set();

            for (const item of items) {
                const key = item.pnu;
                activeKeys.add(key);

                // geometry가 이미 object로 옴 (JSON.parse 불필요)
                const geo = item.land?.geometry;
                if (!geo) continue;

                const existing = polygons.get(key);
                if (existing) continue;

                try {
                    const paths = geojsonToPaths(geo);
                    if (!paths || paths.length === 0) continue;

                    const polygon = new naver.maps.Polygon({
                        map: map,
                        paths: paths,
                        fillColor: '#3b82f6',
                        fillOpacity: 0.15,
                        strokeColor: '#1d4ed8',
                        strokeWeight: 1,
                        strokeOpacity: 0.6,
                        zIndex: 10
                    });

                    polygons.set(key, polygon);
                } catch (e) {
                    console.warn('polygon error:', key, e);
                }
            }

            // 사라진 폴리곤 제거
            for (const [key, polygon] of polygons) {
                if (!activeKeys.has(key)) {
                    polygon.setMap(null);
                    polygons.delete(key);
                }
            }
        }

        function geojsonToPaths(geo) {
            if (!geo || !geo.type) return null;

            if (geo.type === 'Polygon') {
                return geo.coordinates.map(ring =>
                    ring.map(coord => new naver.maps.LatLng(coord[1], coord[0]))
                );
            } else if (geo.type === 'MultiPolygon') {
                // 첫 번째 폴리곤만 사용 (간단히)
                const first = geo.coordinates[0];
                if (!first) return null;
                return first.map(ring =>
                    ring.map(coord => new naver.maps.LatLng(coord[1], coord[0]))
                );
            }
            return null;
        }

        function buildMarkerHtml(item, type) {
            const reg = item.registration;
            const size = type === 'type1' ? 48 : 40;
            const color = type === 'type1' ? '#1d4ed8' : '#ea580c';

            // 등기 정보 없으면 '-' 표시, 있으면 count/myCount
            const label = reg ? (reg.count + '/' + reg.myCount) : '-';
            const bgColor = reg ? color : '#9ca3af';  // 등기 없으면 회색

            return '<div style="background:' + bgColor + ';color:#fff;width:' + size + 'px;height:' + size + 'px;' +
                'border-radius:50%;font-size:10px;font-weight:bold;display:flex;align-items:center;justify-content:center;' +
                'box-shadow:0 2px 6px rgba(0,0,0,0.3);border:2px solid white;">' +
                label + '</div>';
        }

        function showInfoWindow(item, marker) {
            // 기존 infoWindow 닫기
            for (const iw of infoWindows.values()) {
                iw.close();
            }

            const reg = item.registration;
            const land = item.land || {};
            const building = item.building || {};

            let content = '<div style="padding:12px;min-width:280px;font-size:13px;">';
            content += '<div style="font-weight:bold;margin-bottom:8px;border-bottom:1px solid #e5e7eb;padding-bottom:8px;">PNU: ' + item.pnu + '</div>';

            // Registration
            content += '<div style="background:#f3f4f6;padding:8px;border-radius:4px;margin-bottom:8px;">';
            content += '<div style="font-weight:600;margin-bottom:4px;">등기 정보</div>';
            if (reg) {
                content += '<div>전체 등기: ' + reg.count + '건</div>';
                if (reg.lastAt) content += '<div>최근 등기: ' + reg.lastAt.substring(0, 10) + '</div>';
                if (reg.myCount > 0) {
                    content += '<div style="color:#1d4ed8;">내 등기: ' + reg.myCount + '건</div>';
                    if (reg.myLastAt) content += '<div style="color:#1d4ed8;">내 최근: ' + reg.myLastAt.substring(0, 10) + '</div>';
                }
            } else {
                content += '<div style="color:#9ca3af;">등기 없음</div>';
            }
            content += '</div>';

            // Land
            if (land.area || land.price) {
                content += '<div style="margin-bottom:8px;">';
                content += '<div style="font-weight:600;margin-bottom:4px;">토지</div>';
                if (land.area) content += '<div>면적: ' + land.area.toLocaleString() + 'm²</div>';
                if (land.price) content += '<div>공시지가: ' + land.price.toLocaleString() + '원/m²</div>';
                content += '</div>';
            }

            // Building
            if (building.mainPurpsCdNm) {
                content += '<div style="margin-bottom:8px;">';
                content += '<div style="font-weight:600;margin-bottom:4px;">건물</div>';
                content += '<div>용도: ' + building.mainPurpsCdNm + '</div>';
                if (building.totArea) content += '<div>연면적: ' + Number(building.totArea).toLocaleString() + 'm²</div>';
                if (building.useAprDay) content += '<div>준공일: ' + building.useAprDay + '</div>';
                content += '</div>';
            }

            content += '</div>';

            const infoWindow = new naver.maps.InfoWindow({
                content: content,
                backgroundColor: 'white',
                borderColor: '#e5e7eb',
                borderWidth: 1,
                anchorSize: new naver.maps.Size(10, 10),
                pixelOffset: new naver.maps.Point(0, -10)
            });

            infoWindow.open(map, marker);
            infoWindows.set(item.pnu, infoWindow);
        }

        function clearAllMarkers() {
            for (const marker of type1Markers.values()) marker.setMap(null);
            for (const marker of type2Markers.values()) marker.setMap(null);
            for (const polygon of polygons.values()) polygon.setMap(null);
            type1Markers.clear();
            type2Markers.clear();
            polygons.clear();

            document.getElementById('type1Count').textContent = '0';
            document.getElementById('type2Count').textContent = '0';
            document.getElementById('type1Elapsed').textContent = '0';
            document.getElementById('type2Elapsed').textContent = '0';
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
