<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>주소 검색</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #f5f5f5;
            min-height: 100vh;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            padding: 40px 20px;
        }
        h1 {
            text-align: center;
            margin-bottom: 30px;
            color: #333;
        }
        .search-box {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
        }
        #searchInput {
            flex: 1;
            padding: 14px 18px;
            font-size: 16px;
            border: 2px solid #ddd;
            border-radius: 8px;
            outline: none;
            transition: border-color 0.2s;
        }
        #searchInput:focus {
            border-color: #3b82f6;
        }
        #searchBtn {
            padding: 14px 28px;
            font-size: 16px;
            background: #3b82f6;
            color: white;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            transition: background 0.2s;
        }
        #searchBtn:hover {
            background: #2563eb;
        }
        #resultCount {
            color: #666;
            margin-bottom: 15px;
            font-size: 14px;
        }
        #results {
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .result-item {
            padding: 16px 20px;
            border-bottom: 1px solid #eee;
            cursor: pointer;
            transition: background 0.15s;
        }
        .result-item:last-child {
            border-bottom: none;
        }
        .result-item:hover {
            background: #f8fafc;
        }
        .result-item.selected {
            background: #eff6ff;
            border-left: 4px solid #3b82f6;
        }
        .address-main {
            font-size: 15px;
            font-weight: 500;
            color: #333;
            margin-bottom: 4px;
        }
        .address-sub {
            font-size: 13px;
            color: #666;
        }
        .building-name {
            font-size: 13px;
            color: #3b82f6;
            margin-top: 4px;
        }
        .detail-panel {
            margin-top: 20px;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            padding: 20px;
            display: none;
        }
        .detail-panel.show {
            display: block;
        }
        .detail-panel h3 {
            margin-bottom: 15px;
            color: #333;
        }
        .detail-row {
            display: flex;
            padding: 8px 0;
            border-bottom: 1px solid #f0f0f0;
        }
        .detail-row:last-child {
            border-bottom: none;
        }
        .detail-label {
            width: 100px;
            font-size: 13px;
            color: #666;
        }
        .detail-value {
            flex: 1;
            font-size: 13px;
            color: #333;
        }
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #999;
        }
        .loading {
            text-align: center;
            padding: 40px;
            color: #666;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>주소 검색</h1>

        <div class="search-box">
            <input type="text" id="searchInput" placeholder="주소, 건물명 등을 입력하세요">
            <button id="searchBtn">검색</button>
        </div>

        <div id="resultCount"></div>

        <div id="results">
            <div class="empty-state">검색어를 입력하고 검색 버튼을 누르세요</div>
        </div>

        <div id="detailPanel" class="detail-panel">
            <h3>상세 정보</h3>
            <div class="detail-row">
                <span class="detail-label">PNU</span>
                <span class="detail-value" id="detailPnu">-</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">법정동코드</span>
                <span class="detail-value" id="detailBjdongCd">-</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">지번주소</span>
                <span class="detail-value" id="detailJibun">-</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">도로명주소</span>
                <span class="detail-value" id="detailRoad">-</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">건물명</span>
                <span class="detail-value" id="detailBuilding">-</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">좌표</span>
                <span class="detail-value" id="detailCoord">-</span>
            </div>
        </div>
    </div>

    <script>
        const searchInput = document.getElementById('searchInput');
        const searchBtn = document.getElementById('searchBtn');
        const resultsDiv = document.getElementById('results');
        const resultCountDiv = document.getElementById('resultCount');
        const detailPanel = document.getElementById('detailPanel');

        let selectedItem = null;
        let lastKeyword = '';
        let debounceTimer = null;

        function search() {
            const keyword = searchInput.value.trim();
            if (keyword === lastKeyword) return;
            lastKeyword = keyword;
            if (!keyword) {
                resultsDiv.innerHTML = '<div class="empty-state">검색어를 입력해주세요</div>';
                resultCountDiv.textContent = '';
                detailPanel.classList.remove('show');
                return;
            }

            resultsDiv.innerHTML = '<div class="loading">검색 중...</div>';
            resultCountDiv.textContent = '';
            detailPanel.classList.remove('show');

            fetch('/api/search?keyword=' + encodeURIComponent(keyword))
                .then(res => res.json())
                .then(function(data) {
                    var results = data.results;
                    if (results.length === 0) {
                        resultsDiv.innerHTML = '<div class="empty-state">검색 결과가 없습니다</div>';
                        resultCountDiv.textContent = data.elapsedMs + 'ms';
                        return;
                    }

                    resultCountDiv.textContent = '검색 결과: ' + data.count + '건 (' + data.elapsedMs + 'ms)';

                    resultsDiv.innerHTML = results.map(function(item, idx) {
                        var html = '<div class="result-item" data-idx="' + idx + '">';
                        html += '<div class="address-main">' + (item.jibunAddress || item.roadAddress || '-') + '</div>';
                        if (item.roadAddress && item.jibunAddress) {
                            html += '<div class="address-sub">' + item.roadAddress + '</div>';
                        }
                        if (item.buildingName) {
                            html += '<div class="building-name">' + item.buildingName + '</div>';
                        }
                        html += '</div>';
                        return html;
                    }).join('');

                    // 클릭 이벤트
                    document.querySelectorAll('.result-item').forEach(function(el) {
                        el.addEventListener('click', function() {
                            var idx = parseInt(this.dataset.idx);
                            showDetail(results[idx], this);
                        });
                    });
                })
                .catch(err => {
                    console.error('Search error:', err);
                    resultsDiv.innerHTML = '<div class="empty-state">검색 중 오류가 발생했습니다</div>';
                });
        }

        function showDetail(item, element) {
            // 선택 표시
            if (selectedItem) selectedItem.classList.remove('selected');
            element.classList.add('selected');
            selectedItem = element;

            // 상세 정보 표시
            document.getElementById('detailPnu').textContent = item.pnu || '-';
            document.getElementById('detailBjdongCd').textContent = item.bjdongCd || '-';
            document.getElementById('detailJibun').textContent = item.jibunAddress || '-';
            document.getElementById('detailRoad').textContent = item.roadAddress || '-';
            document.getElementById('detailBuilding').textContent = item.buildingName || '-';
            document.getElementById('detailCoord').textContent =
                (item.lat && item.lng) ? item.lat + ', ' + item.lng : '-';

            detailPanel.classList.add('show');
        }

        // 이벤트
        searchBtn.addEventListener('click', search);
        searchInput.addEventListener('keyup', function(e) {
            if (debounceTimer) clearTimeout(debounceTimer);
            debounceTimer = setTimeout(search, 300);
        });

        // 포커스
        searchInput.focus();
    </script>
</body>
</html>
