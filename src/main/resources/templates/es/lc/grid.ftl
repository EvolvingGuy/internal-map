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
        #info h3 { margin-bottom: 8px; color: #059669; }
        #info div { margin: 4px 0; }
        #info .label { color: #666; }
        #info .value { font-weight: bold; color: #111; }

        /* Sidebar */
        #sidebar {
            width: 300px;
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

        /* Grid Size Control */
        .grid-control {
            background: white;
            border-radius: 8px;
            padding: 12px;
            margin-bottom: 12px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .grid-control h3 {
            font-size: 14px;
            color: #475569;
            margin-bottom: 10px;
            padding-bottom: 6px;
            border-bottom: 1px solid #e2e8f0;
        }
        .grid-control input[type="range"] {
            width: 100%;
        }
        .grid-control .grid-value {
            text-align: center;
            font-size: 14px;
            font-weight: bold;
            color: #059669;
            margin-top: 4px;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>Grid Aggregation</h3>
        <div><span class="label">Grid:</span> <span class="value" id="gridInfo">-</span></div>
        <div><span class="label">Cells:</span> <span class="value" id="cellCount">-</span></div>
        <div><span class="label">Total:</span> <span class="value" id="totalCount">-</span></div>
        <div><span class="label">Time:</span> <span class="value" id="elapsed">-</span></div>
    </div>
    <div id="sidebar">
        <h2>Filters <button id="resetBtn">Reset All</button></h2>

        <!-- Grid Control -->
        <div class="grid-control">
            <h3>Grid Size</h3>
            <input type="range" id="gridSize" min="200" max="600" step="50" value="400">
            <div class="grid-value"><span id="gridSizeValue">400</span>px</div>
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
        const map = new naver.maps.Map('map', {
            center: new naver.maps.LatLng(36.5, 127.5),
            zoom: 10,
            minZoom: 6,
            maxZoom: 21
        });

        let debounceTimer = null;
        let markerMap = new Map();
        const MIN_SIZE = 50;
        const MAX_SIZE = 100;

        // Grid size slider
        const gridSizeSlider = document.getElementById('gridSize');
        const gridSizeValue = document.getElementById('gridSizeValue');
        gridSizeSlider.addEventListener('input', function() {
            gridSizeValue.textContent = this.value;
            onFilterChange();
        });

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
            gridSizeSlider.value = 400;
            gridSizeValue.textContent = '400';
            fetchData();
        });

        function getCheckedValues(groupId) {
            const group = document.getElementById(groupId);
            if (!group) return [];
            return Array.from(group.querySelectorAll('input:checked')).map(cb => cb.value);
        }

        function fetchData() {
            const bounds = map.getBounds();
            const mapDiv = document.getElementById('map');

            const params = {
                swLng: bounds.getSW().lng(),
                swLat: bounds.getSW().lat(),
                neLng: bounds.getNE().lng(),
                neLat: bounds.getNE().lat(),
                viewportWidth: mapDiv.clientWidth,
                viewportHeight: mapDiv.clientHeight,
                gridSize: parseInt(gridSizeSlider.value)
            };

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

            const queryString = Object.entries(params)
                .map(([k, v]) => encodeURIComponent(k) + '=' + encodeURIComponent(v))
                .join('&');

            fetch('/api/es/lc/agg/grid?' + queryString)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('gridInfo').textContent = data.cols + ' x ' + data.rows;
                    document.getElementById('cellCount').textContent = data.cellCount.toLocaleString();
                    document.getElementById('totalCount').textContent = data.totalCount.toLocaleString();
                    document.getElementById('elapsed').textContent = data.elapsedMs + 'ms';

                    // Calculate min/max for sizing
                    const counts = data.cells.map(c => c.count);
                    const minCount = Math.min(...counts);
                    const maxCount = Math.max(...counts);

                    const usedKeys = new Set();

                    data.cells.forEach(cell => {
                        const key = cell.gridX + '_' + cell.gridY;
                        usedKeys.add(key);

                        const size = calculateSize(cell.count, minCount, maxCount);
                        const existing = markerMap.get(key);

                        if (existing) {
                            existing.setPosition(new naver.maps.LatLng(cell.centerLat, cell.centerLng));
                            existing.setIcon({
                                content: buildMarkerHtml(cell.count, cell.gridX + ',' + cell.gridY, size),
                                anchor: new naver.maps.Point(size/2, size/2)
                            });
                        } else {
                            const marker = new naver.maps.Marker({
                                position: new naver.maps.LatLng(cell.centerLat, cell.centerLng),
                                map: map,
                                icon: {
                                    content: buildMarkerHtml(cell.count, cell.gridX + ',' + cell.gridY, size),
                                    anchor: new naver.maps.Point(size/2, size/2)
                                }
                            });
                            markerMap.set(key, marker);
                        }
                    });

                    for (const [key, marker] of markerMap) {
                        if (!usedKeys.has(key)) {
                            marker.setMap(null);
                            markerMap.delete(key);
                        }
                    }
                })
                .catch(err => console.error('Fetch error:', err));
        }

        function calculateSize(count, minCount, maxCount) {
            if (maxCount === minCount) return (MIN_SIZE + MAX_SIZE) / 2;
            const ratio = (count - minCount) / (maxCount - minCount);
            return MIN_SIZE + ratio * (MAX_SIZE - MIN_SIZE);
        }

        function buildMarkerHtml(count, label, size) {
            const fontSize = Math.max(10, Math.floor(size / 5));
            const labelFontSize = Math.max(8, fontSize - 2);
            return '<div style="width:' + size + 'px;height:' + size + 'px;background:#059669;color:#fff;border-radius:50%;display:flex;flex-direction:column;align-items:center;justify-content:center;font-weight:bold;box-shadow:0 2px 6px rgba(0,0,0,0.3);line-height:1.2;">' +
                '<span style="font-size:' + labelFontSize + 'px;opacity:0.8;">' + label + '</span>' +
                '<span style="font-size:' + fontSize + 'px;">' + count.toLocaleString() + '</span></div>';
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
