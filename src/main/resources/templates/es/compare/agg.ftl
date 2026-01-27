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
            padding: 24px;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
            font-size: 14px;
            min-width: 520px;
        }
        #info h3 {
            margin-bottom: 16px;
            color: #1e293b;
            font-size: 20px;
            border-bottom: 3px solid #e2e8f0;
            padding-bottom: 12px;
        }

        .compare-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 14px;
        }
        .compare-table th, .compare-table td {
            padding: 12px 16px;
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

        .idx-ldrc { color: #9333ea; font-weight: bold; }
        .idx-lc { color: #2563eb; font-weight: bold; }
        .idx-lnb { color: #16a34a; font-weight: bold; }
        .idx-lnbt { color: #dc2626; font-weight: bold; }

        .best { background: #dcfce7 !important; }
        .worst { background: #fee2e2 !important; }
        .disabled { color: #cbd5e1; }

        .mode-indicator {
            font-size: 13px;
            padding: 6px 12px;
            border-radius: 6px;
            margin-bottom: 12px;
            font-weight: 500;
        }
        .mode-ldrc { background: #f3e8ff; color: #7c3aed; }
        .mode-compare { background: #dbeafe; color: #2563eb; }

        .loading { color: #94a3b8; }

        /* Sidebar */
        #sidebar {
            width: 380px;
            height: 100vh;
            overflow-y: auto;
            background: #f8fafc;
            border-left: 1px solid #e2e8f0;
            padding: 24px;
        }
        #sidebar h2 {
            font-size: 20px;
            margin-bottom: 16px;
            color: #1e293b;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        #resetBtn {
            font-size: 14px;
            padding: 8px 16px;
            background: #ef4444;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
        }
        #resetBtn:hover { background: #dc2626; }

        .filter-section {
            background: white;
            border-radius: 10px;
            padding: 16px;
            margin-bottom: 16px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.1);
        }
        .filter-section h3 {
            font-size: 16px;
            color: #475569;
            margin-bottom: 14px;
            padding-bottom: 8px;
            border-bottom: 2px solid #e2e8f0;
        }
        .filter-group {
            margin-bottom: 14px;
        }
        .filter-group label {
            display: block;
            font-size: 14px;
            color: #64748b;
            margin-bottom: 6px;
        }
        .filter-group input[type="number"],
        .filter-group input[type="date"] {
            width: 100%;
            padding: 10px 12px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font-size: 14px;
        }
        .range-inputs {
            display: flex;
            gap: 10px;
        }
        .range-inputs input { flex: 1; }
        .checkbox-group {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }
        .checkbox-item {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;
            padding: 8px 10px;
            background: #f1f5f9;
            border-radius: 6px;
            cursor: pointer;
        }
        .checkbox-item:hover { background: #e2e8f0; }
        .checkbox-item input { margin: 0; width: 16px; height: 16px; }
        .checkbox-item.checked { background: #dbeafe; color: #1d4ed8; }
        .single-checkbox {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            cursor: pointer;
        }
        .single-checkbox input { width: 18px; height: 18px; }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="info">
        <h3>${title}</h3>
        <div id="modeIndicator" class="mode-indicator mode-ldrc">LDRC Mode (No Filter)</div>
        <table class="compare-table">
            <thead>
                <tr>
                    <th>Metric</th>
                    <th class="idx-ldrc">LDRC</th>
                    <th class="idx-lc">LC</th>
                    <th class="idx-lnb">LNB</th>
                    <th class="idx-lnbt">LNBT</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>Server Time (ms)</td>
                    <td id="ldrc-serverTime" class="loading">-</td>
                    <td id="lc-serverTime" class="loading">-</td>
                    <td id="lnb-serverTime" class="loading">-</td>
                    <td id="lnbt-serverTime" class="loading">-</td>
                </tr>
                <tr>
                    <td>Round Trip (ms)</td>
                    <td id="ldrc-roundTrip" class="loading">-</td>
                    <td id="lc-roundTrip" class="loading">-</td>
                    <td id="lnb-roundTrip" class="loading">-</td>
                    <td id="lnbt-roundTrip" class="loading">-</td>
                </tr>
                <tr>
                    <td>Payload (KB)</td>
                    <td id="ldrc-payload" class="loading">-</td>
                    <td id="lc-payload" class="loading">-</td>
                    <td id="lnb-payload" class="loading">-</td>
                    <td id="lnbt-payload" class="loading">-</td>
                </tr>
                <tr>
                    <td>Total Count</td>
                    <td id="ldrc-totalCount" class="loading">-</td>
                    <td id="lc-totalCount" class="loading">-</td>
                    <td id="lnb-totalCount" class="loading">-</td>
                    <td id="lnbt-totalCount" class="loading">-</td>
                </tr>
                <tr>
                    <td>Region Count</td>
                    <td id="ldrc-regionCount" class="loading">-</td>
                    <td id="lc-regionCount" class="loading">-</td>
                    <td id="lnb-regionCount" class="loading">-</td>
                    <td id="lnbt-regionCount" class="loading">-</td>
                </tr>
            </tbody>
        </table>
    </div>

    <div id="sidebar">
        <h2>Filters <button id="resetBtn" onclick="resetFilters()">Reset All</button></h2>

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
                    <label class="checkbox-item"><input type="checkbox" value="동물및식물관련시설">동식물시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="공장">공장</label>
                    <label class="checkbox-item"><input type="checkbox" value="노유자시설">노유자시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="숙박시설">숙박시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="업무시설">업무시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="교육연구시설">교육연구</label>
                    <label class="checkbox-item"><input type="checkbox" value="종교시설">종교시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="자동차관련시설">자동차시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="위험물저장및처리시설">위험물시설</label>
                    <label class="checkbox-item"><input type="checkbox" value="문화및집회시설">문화집회</label>
                </div>
            </div>
            <div class="filter-group">
                <label>Registry Type</label>
                <div class="checkbox-group" id="buildingRegstrGbCdNm">
                    <label class="checkbox-item"><input type="checkbox" value="일반">일반</label>
                    <label class="checkbox-item"><input type="checkbox" value="집합">집합</label>
                </div>
            </div>
            <div class="filter-group">
                <label class="single-checkbox"><input type="checkbox" id="buildingPmsDayRecent5y">Permit Date (Recent 5y)</label>
            </div>
            <div class="filter-group">
                <label class="single-checkbox"><input type="checkbox" id="buildingStcnsDayRecent5y">Construction Start (Recent 5y)</label>
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
                <label>Plot Area (m²)</label>
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
                <label>Zone (Jiyuk)</label>
                <div class="checkbox-group" id="landJiyukCd1">
                    <label class="checkbox-item"><input type="checkbox" value="64">계획관리</label>
                    <label class="checkbox-item"><input type="checkbox" value="71">농림</label>
                    <label class="checkbox-item"><input type="checkbox" value="63">생산관리</label>
                    <label class="checkbox-item"><input type="checkbox" value="62">보전관리</label>
                    <label class="checkbox-item"><input type="checkbox" value="43">자연녹지</label>
                    <label class="checkbox-item"><input type="checkbox" value="14">2종일반주거</label>
                    <label class="checkbox-item"><input type="checkbox" value="13">1종일반주거</label>
                    <label class="checkbox-item"><input type="checkbox" value="44">개발제한</label>
                    <label class="checkbox-item"><input type="checkbox" value="22">일반상업</label>
                    <label class="checkbox-item"><input type="checkbox" value="81">자연환경보전</label>
                </div>
            </div>
            <div class="filter-group">
                <label>Category (Jimok)</label>
                <div class="checkbox-group" id="landJimokCd">
                    <label class="checkbox-item"><input type="checkbox" value="02">답</label>
                    <label class="checkbox-item"><input type="checkbox" value="08">대</label>
                    <label class="checkbox-item"><input type="checkbox" value="01">전</label>
                    <label class="checkbox-item"><input type="checkbox" value="05">임야</label>
                    <label class="checkbox-item"><input type="checkbox" value="14">도로</label>
                    <label class="checkbox-item"><input type="checkbox" value="18">구거</label>
                    <label class="checkbox-item"><input type="checkbox" value="28">잡종지</label>
                    <label class="checkbox-item"><input type="checkbox" value="17">하천</label>
                    <label class="checkbox-item"><input type="checkbox" value="09">공장용지</label>
                    <label class="checkbox-item"><input type="checkbox" value="03">과수원</label>
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
            <h3>Trade</h3>
            <div class="filter-group">
                <label>Property Type</label>
                <div class="checkbox-group" id="tradeProperty">
                    <label class="checkbox-item"><input type="checkbox" value="APARTMENT">Apartment</label>
                    <label class="checkbox-item"><input type="checkbox" value="COMMERCIAL_BUILDING">Commercial</label>
                    <label class="checkbox-item"><input type="checkbox" value="FACTORY_WAREHOUSE">Factory/WH</label>
                    <label class="checkbox-item"><input type="checkbox" value="FACTORY_WAREHOUSE_MULTI">Factory/WH M</label>
                    <label class="checkbox-item"><input type="checkbox" value="LAND">Land</label>
                    <label class="checkbox-item"><input type="checkbox" value="MULTI">Multi</label>
                    <label class="checkbox-item"><input type="checkbox" value="OFFICETEL">Officetel</label>
                    <label class="checkbox-item"><input type="checkbox" value="SHOPPING_AND_OFFICE">Shop/Office</label>
                    <label class="checkbox-item"><input type="checkbox" value="SINGLE">Single</label>
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
            zoom: ${defaultZoom}
        });

        const ldrcApiPath = '${ldrcApiPath}';
        const compareApiPaths = {
            lc: '${lcApiPath}',
            lnb: '${lnbApiPath}',
            lnbt: '${lnbtApiPath}'
        };
        const allIndexes = ['ldrc', 'lc', 'lnb', 'lnbt'];

        let debounceTimer = null;

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
            debounceTimer = setTimeout(fetchAll, 300);
        }

        function getCheckedValues(groupId) {
            const checkboxes = document.querySelectorAll('#' + groupId + ' input[type="checkbox"]:checked');
            return Array.from(checkboxes).map(cb => cb.value);
        }

        function buildFilterParams() {
            const params = {};

            // Building
            const mainPurps = getCheckedValues('buildingMainPurpsCdNm');
            if (mainPurps.length) params.buildingMainPurpsCdNm = mainPurps.join(',');

            const regstr = getCheckedValues('buildingRegstrGbCdNm');
            if (regstr.length) params.buildingRegstrGbCdNm = regstr.join(',');

            if (document.getElementById('buildingPmsDayRecent5y').checked) params.buildingPmsDayRecent5y = true;
            if (document.getElementById('buildingStcnsDayRecent5y').checked) params.buildingStcnsDayRecent5y = true;

            const useAprStart = document.getElementById('buildingUseAprDayStart').value;
            const useAprEnd = document.getElementById('buildingUseAprDayEnd').value;
            if (useAprStart) params.buildingUseAprDayStart = useAprStart;
            if (useAprEnd) params.buildingUseAprDayEnd = useAprEnd;

            ['buildingTotAreaMin', 'buildingTotAreaMax', 'buildingPlatAreaMin', 'buildingPlatAreaMax', 'buildingArchAreaMin', 'buildingArchAreaMax'].forEach(id => {
                const val = document.getElementById(id).value;
                if (val) params[id] = val;
            });

            // Land
            const jiyuk = getCheckedValues('landJiyukCd1');
            if (jiyuk.length) params.landJiyukCd1 = jiyuk.join(',');

            const jimok = getCheckedValues('landJimokCd');
            if (jimok.length) params.landJimokCd = jimok.join(',');

            ['landAreaMin', 'landAreaMax', 'landPriceMin', 'landPriceMax'].forEach(id => {
                const val = document.getElementById(id).value;
                if (val) params[id] = val;
            });

            // Trade
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

            return params;
        }

        function resetFilters() {
            document.querySelectorAll('#sidebar input[type="checkbox"]').forEach(cb => {
                cb.checked = false;
                const item = cb.closest('.checkbox-item');
                if (item) item.classList.remove('checked');
            });
            document.querySelectorAll('#sidebar input[type="number"], #sidebar input[type="date"]').forEach(input => {
                input.value = '';
            });
            fetchAll();
        }

        function hasAnyFilter() {
            const filterParams = buildFilterParams();
            return Object.keys(filterParams).length > 0;
        }

        function clearAllCells() {
            allIndexes.forEach(idx => {
                ['serverTime', 'roundTrip', 'payload', 'totalCount', 'regionCount'].forEach(metric => {
                    const el = document.getElementById(idx + '-' + metric);
                    el.textContent = '-';
                    el.classList.remove('best', 'worst');
                    el.classList.add('disabled');
                });
            });
        }

        function updateCell(idx, metric, value, formatted) {
            const el = document.getElementById(idx + '-' + metric);
            el.textContent = formatted;
            el.classList.remove('disabled', 'loading');
        }

        async function fetchAll() {
            const bounds = map.getBounds();
            const sw = bounds.getSW();
            const ne = bounds.getNE();
            const zoom = map.getZoom();

            const baseParams = {
                swLng: sw.lng(),
                swLat: sw.lat(),
                neLng: ne.lng(),
                neLat: ne.lat()
            };

            const filterParams = buildFilterParams();
            const hasFilter = Object.keys(filterParams).length > 0;

            // 모드 표시 업데이트
            const modeIndicator = document.getElementById('modeIndicator');
            if (hasFilter) {
                modeIndicator.textContent = 'Compare Mode (With Filter)';
                modeIndicator.className = 'mode-indicator mode-compare';
            } else {
                modeIndicator.textContent = 'LDRC Mode (No Filter)';
                modeIndicator.className = 'mode-indicator mode-ldrc';
            }

            // 모든 셀 초기화
            clearAllCells();

            if (hasFilter) {
                // 필터 있음: LC, LNB, LNBT 비교
                const params = new URLSearchParams({ ...baseParams, ...filterParams });

                const fetchPromises = Object.entries(compareApiPaths).map(async ([idx, path]) => {
                    const startTime = performance.now();
                    try {
                        const response = await fetch(path + '?' + params);
                        const text = await response.text();
                        const roundTrip = Math.round(performance.now() - startTime);
                        const payload = new Blob([text]).size;
                        const data = JSON.parse(text);
                        return { idx, success: true, data, roundTrip, payload };
                    } catch (err) {
                        console.error(idx + ' fetch error:', err);
                        return { idx, success: false };
                    }
                });

                const fetchResults = await Promise.all(fetchPromises);

                const metrics = { serverTime: {}, roundTrip: {}, payload: {}, totalCount: {}, regionCount: {} };

                fetchResults.forEach(result => {
                    const idx = result.idx;
                    if (result.success) {
                        metrics.serverTime[idx] = result.data.elapsedMs;
                        metrics.roundTrip[idx] = result.roundTrip;
                        metrics.payload[idx] = result.payload;
                        metrics.totalCount[idx] = result.data.totalCount;
                        metrics.regionCount[idx] = result.data.regionCount;

                        updateCell(idx, 'serverTime', result.data.elapsedMs, result.data.elapsedMs.toLocaleString());
                        updateCell(idx, 'roundTrip', result.roundTrip, result.roundTrip.toLocaleString());
                        updateCell(idx, 'payload', result.payload, (result.payload / 1024).toFixed(1));
                        updateCell(idx, 'totalCount', result.data.totalCount, result.data.totalCount.toLocaleString());
                        updateCell(idx, 'regionCount', result.data.regionCount, result.data.regionCount.toLocaleString());
                    } else {
                        ['serverTime', 'roundTrip', 'payload', 'totalCount', 'regionCount'].forEach(m => {
                            document.getElementById(idx + '-' + m).textContent = 'ERR';
                        });
                    }
                });

                // Best/Worst 하이라이트
                highlightBestWorst('serverTime', metrics.serverTime, true);
                highlightBestWorst('roundTrip', metrics.roundTrip, true);
                highlightBestWorst('payload', metrics.payload, true);

            } else {
                // 무필터: LDRC만 호출
                const params = new URLSearchParams({ ...baseParams, zoom: zoom });
                const url = ldrcApiPath + '&' + params;

                const startTime = performance.now();
                try {
                    const response = await fetch(url);
                    const text = await response.text();
                    const roundTrip = Math.round(performance.now() - startTime);
                    const payload = new Blob([text]).size;
                    const data = JSON.parse(text);

                    updateCell('ldrc', 'serverTime', data.elapsedMs, data.elapsedMs.toLocaleString());
                    updateCell('ldrc', 'roundTrip', roundTrip, roundTrip.toLocaleString());
                    updateCell('ldrc', 'payload', payload, (payload / 1024).toFixed(1));
                    updateCell('ldrc', 'totalCount', data.totalCount, data.totalCount.toLocaleString());
                    updateCell('ldrc', 'regionCount', data.clusters?.length || 0, (data.clusters?.length || 0).toLocaleString());
                } catch (err) {
                    console.error('ldrc fetch error:', err);
                    ['serverTime', 'roundTrip', 'payload', 'totalCount', 'regionCount'].forEach(m => {
                        document.getElementById('ldrc-' + m).textContent = 'ERR';
                    });
                }
            }
        }

        function highlightBestWorst(metric, values, lowerIsBetter) {
            const validEntries = Object.entries(values).filter(([k, v]) => v !== undefined);
            if (validEntries.length < 2) return;

            const sorted = validEntries.sort((a, b) => a[1] - b[1]);
            const bestIdx = lowerIsBetter ? sorted[0][0] : sorted[sorted.length - 1][0];
            const worstIdx = lowerIsBetter ? sorted[sorted.length - 1][0] : sorted[0][0];

            allIndexes.forEach(idx => {
                const el = document.getElementById(idx + '-' + metric);
                el.classList.remove('best', 'worst');
                if (idx === bestIdx) el.classList.add('best');
                if (idx === worstIdx && bestIdx !== worstIdx) el.classList.add('worst');
            });
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
