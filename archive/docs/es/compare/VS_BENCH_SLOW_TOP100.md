## lnbtp16 (1/16 local f)

> source: `bench_lnbtp16.json` | n=2000 | avg=17.5ms | p99=207ms

| Rank | took_ms | hits | buckets | bbox | agg | filters |
|------|---------|------|---------|------|-----|---------|
| 1 | 699ms | 10,000 | 5000 | 전국 | emd |  |
| 2 | 599ms | 10,000 | 5000 | 전국 | emd |  |
| 3 | 561ms | 10,000 | 5000 | 전국 | emd |  |
| 4 | 509ms | 10,000 | 252 | 전국 | sgg |  |
| 5 | 499ms | 10,000 | 5000 | 전국 | emd | landAreaMin, landAreaMax |
| 6 | 424ms | 10,000 | 17 | 전국 | sd |  |
| 7 | 404ms | 10,000 | 252 | 전국 | sgg |  |
| 8 | 394ms | 10,000 | 252 | 전국 | sgg |  |
| 9 | 380ms | 10,000 | 252 | 전국 | sgg |  |
| 10 | 378ms | 10,000 | 17 | 전국 | sd |  |
| 11 | 370ms | 10,000 | 252 | 전국 | sgg |  |
| 12 | 337ms | 10,000 | 17 | 전국 | sd | tradeProperty |
| 13 | 323ms | 5,132 | 610 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingStcnsDayRecent5y |
| 14 | 272ms | 7 | 1 | 부산 | sd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 15 | 271ms | 10,000 | 5 | 경기 | sd | landJimokCd, landPriceMin, landPriceMax |
| 16 | 254ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax |
| 17 | 231ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 18 | 214ms | 10,000 | 1129 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 19 | 212ms | 10,000 | 17 | 전국 | sd | buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 20 | 207ms | 10,000 | 3080 | 전국 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 21 | 204ms | 2,645 | 70 | 제주 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm |
| 22 | 198ms | 4,057 | 61 | 경기 | sgg | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 23 | 198ms | 10,000 | 3221 | 전국 | emd | landJiyukCd1, buildingMainPurpsCdNm |
| 24 | 190ms | 10,000 | 4594 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 25 | 190ms | 5,046 | 363 | 경기 | emd | landJiyukCd1, landJimokCd, tradeContractDateStart, tradeContractDateEnd |
| 26 | 179ms | 10,000 | 3390 | 전국 | emd | landJiyukCd1 |
| 27 | 165ms | 10,000 | 1579 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 28 | 152ms | 0 | 0 | 부산 | emd | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 29 | 151ms | 10,000 | 5000 | 전국 | emd | buildingTotAreaMin, buildingTotAreaMax |
| 30 | 149ms | 10,000 | 1045 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 31 | 146ms | 10,000 | 4 | 서울 | sd |  |
| 32 | 138ms | 10,000 | 17 | 전국 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 33 | 136ms | 10,000 | 5000 | 전국 | emd | buildingRegstrGbCdNm |
| 34 | 132ms | 0 | 0 | 전국 | sgg | landJiyukCd1, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 35 | 126ms | 10,000 | 5000 | 전국 | emd | tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 36 | 123ms | 10,000 | 2974 | 전국 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm |
| 37 | 122ms | 10,000 | 3845 | 전국 | emd | landPriceMin, landPriceMax |
| 38 | 120ms | 10,000 | 685 | 서울 | emd |  |
| 39 | 120ms | 1,275 | 188 | 전국 | sgg | landJiyukCd1, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 40 | 114ms | 10,000 | 252 | 전국 | sgg | landJimokCd |
| 41 | 113ms | 10,000 | 5 | 경기 | sd |  |
| 42 | 112ms | 10,000 | 251 | 전국 | sgg | landJiyukCd1, landPriceMin, landPriceMax |
| 43 | 112ms | 10,000 | 4859 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 44 | 111ms | 0 | 0 | 전국 | sd | landJiyukCd1, landJimokCd, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 45 | 108ms | 10,000 | 1014 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 46 | 107ms | 10,000 | 252 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 47 | 102ms | 10,000 | 88 | 경기 | sgg |  |
| 48 | 102ms | 18 | 3 | 경기 | sd | landJiyukCd1, landAreaMin, landAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 49 | 101ms | 10,000 | 17 | 전국 | sd | landJiyukCd1, landAreaMin, landAreaMax |
| 50 | 101ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingMainPurpsCdNm |
| 51 | 101ms | 10,000 | 5000 | 전국 | emd | buildingUseAprDayStart, buildingUseAprDayEnd |
| 52 | 96ms | 0 | 0 | 전국 | emd | tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 53 | 95ms | 10,000 | 17 | 전국 | sd | landJiyukCd1, landJimokCd, tradeProperty |
| 54 | 94ms | 10,000 | 50 | 서울 | sgg | landAreaMin, landAreaMax, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd |
| 55 | 92ms | 10,000 | 3737 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 56 | 91ms | 10,000 | 252 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 57 | 90ms | 10,000 | 4958 | 전국 | emd | tradeContractDateStart, tradeContractDateEnd |
| 58 | 88ms | 417 | 2 | 부산 | sd | landJiyukCd1, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 59 | 86ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingTotAreaMin, buildingTotAreaMax |
| 60 | 85ms | 10,000 | 243 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 61 | 85ms | 161 | 28 | 광주 | emd | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 62 | 85ms | 10,000 | 252 | 전국 | sgg | tradeProperty |
| 63 | 84ms | 10 | 1 | 서울 | sd | buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 64 | 83ms | 10,000 | 17 | 전국 | sd | landJiyukCd1 |
| 65 | 81ms | 10,000 | 162 | 전국 | sgg | landJiyukCd1 |
| 66 | 81ms | 10,000 | 4879 | 전국 | emd | tradeProperty |
| 67 | 80ms | 10,000 | 893 | 경기 | emd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 68 | 80ms | 10,000 | 5 | 경기 | sd | tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 69 | 79ms | 10,000 | 1299 | 경기 | emd | landAreaMin, landAreaMax |
| 70 | 79ms | 10,000 | 4 | 서울 | sd | buildingUseAprDayStart, buildingUseAprDayEnd |
| 71 | 78ms | 101 | 41 | 전국 | sgg | landAreaMin, landAreaMax, buildingMainPurpsCdNm, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 72 | 78ms | 7,693 | 76 | 경기 | sgg | landJiyukCd1, landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 73 | 77ms | 10,000 | 510 | 서울 | emd | buildingRegstrGbCdNm, tradeProperty |
| 74 | 77ms | 8 | 7 | 전국 | emd | landJiyukCd1, landJimokCd, buildingMainPurpsCdNm, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 75 | 76ms | 10,000 | 677 | 전국 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 76 | 75ms | 0 | 0 | 세종 | emd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 77 | 75ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 78 | 74ms | 10,000 | 17 | 전국 | sd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 79 | 74ms | 10,000 | 17 | 전국 | sd | landPriceMin, landPriceMax |
| 80 | 74ms | 1,259 | 10 | 전국 | sd | landJiyukCd1, landPriceMin, landPriceMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 81 | 74ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 82 | 73ms | 10,000 | 1311 | 경기 | emd |  |
| 83 | 73ms | 10,000 | 251 | 전국 | sgg | tradeProperty |
| 84 | 73ms | 2,479 | 337 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 85 | 73ms | 293 | 204 | 전국 | emd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 86 | 72ms | 0 | 0 | 제주 | sgg | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 87 | 72ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 88 | 72ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingMainPurpsCdNm |
| 89 | 71ms | 10,000 | 3174 | 전국 | emd | landAreaMin, landAreaMax, buildingMainPurpsCdNm, tradeProperty |
| 90 | 71ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax |
| 91 | 70ms | 76 | 54 | 전국 | emd | landJimokCd, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 92 | 69ms | 10,000 | 929 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 93 | 69ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 94 | 68ms | 10,000 | 1311 | 경기 | emd |  |
| 95 | 67ms | 2 | 2 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 96 | 67ms | 10,000 | 1197 | 전국 | emd | landJiyukCd1, landJimokCd, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd |
| 97 | 66ms | 10,000 | 2493 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 98 | 66ms | 10,000 | 1663 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 99 | 65ms | 10,000 | 1311 | 경기 | emd |  |
| 100 | 64ms | 10,000 | 1059 | 경기 | emd | landJiyukCd1, landPriceMin, landPriceMax |

## lnbtpu (4/4 local f)

> source: `bench_lnbtpu_forcemerged.json` | n=2000 | avg=13.7ms | p99=139ms

| Rank | took_ms | hits | buckets | bbox | agg | filters |
|------|---------|------|---------|------|-----|---------|
| 1 | 661ms | 10,000 | 5000 | 전국 | emd |  |
| 2 | 563ms | 10,000 | 5000 | 전국 | emd |  |
| 3 | 544ms | 10,000 | 5000 | 전국 | emd |  |
| 4 | 469ms | 10,000 | 17 | 전국 | sd |  |
| 5 | 422ms | 10,000 | 252 | 전국 | sgg |  |
| 6 | 395ms | 10,000 | 17 | 전국 | sd |  |
| 7 | 387ms | 10,000 | 252 | 전국 | sgg |  |
| 8 | 385ms | 10,000 | 5000 | 전국 | emd | landAreaMin, landAreaMax |
| 9 | 376ms | 10,000 | 252 | 전국 | sgg |  |
| 10 | 372ms | 10,000 | 252 | 전국 | sgg |  |
| 11 | 350ms | 10,000 | 252 | 전국 | sgg |  |
| 12 | 279ms | 4,057 | 61 | 경기 | sgg | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 13 | 261ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax |
| 14 | 219ms | 10,000 | 4859 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 15 | 194ms | 10,000 | 3390 | 전국 | emd | landJiyukCd1 |
| 16 | 167ms | 10,000 | 4594 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 17 | 167ms | 10,000 | 252 | 전국 | sgg | landJimokCd |
| 18 | 146ms | 10,000 | 5 | 경기 | sd | landPriceMin, landPriceMax |
| 19 | 146ms | 10,000 | 1197 | 전국 | emd | landJiyukCd1, landJimokCd, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd |
| 20 | 139ms | 10,000 | 1045 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 21 | 134ms | 10,000 | 2493 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 22 | 133ms | 10,000 | 3845 | 전국 | emd | landPriceMin, landPriceMax |
| 23 | 132ms | 10,000 | 5000 | 전국 | emd | tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 24 | 130ms | 10,000 | 5000 | 전국 | emd | buildingRegstrGbCdNm |
| 25 | 128ms | 10,000 | 1234 | 경기 | emd | landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingRegstrGbCdNm |
| 26 | 113ms | 12 | 9 | 서울 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 27 | 111ms | 10,000 | 3737 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 28 | 106ms | 10,000 | 5000 | 전국 | emd | buildingTotAreaMin, buildingTotAreaMax |
| 29 | 104ms | 10,000 | 2974 | 전국 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm |
| 30 | 103ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 31 | 103ms | 10,000 | 5000 | 전국 | emd | buildingUseAprDayStart, buildingUseAprDayEnd |
| 32 | 100ms | 10,000 | 17 | 전국 | sd | landJiyukCd1, landAreaMin, landAreaMax |
| 33 | 93ms | 80 | 2 | 서울 | sd | buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 34 | 93ms | 10,000 | 4958 | 전국 | emd | tradeContractDateStart, tradeContractDateEnd |
| 35 | 91ms | 10,000 | 1579 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 36 | 88ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 37 | 88ms | 2,479 | 337 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 38 | 86ms | 10,000 | 893 | 경기 | emd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 39 | 86ms | 10,000 | 17 | 전국 | sd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 40 | 85ms | 10,000 | 3080 | 전국 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 41 | 84ms | 2 | 2 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 42 | 81ms | 10,000 | 763 | 경기 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 43 | 81ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 44 | 79ms | 10,000 | 88 | 경기 | sgg |  |
| 45 | 79ms | 4,815 | 745 | 경기 | emd | landAreaMin, landAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 46 | 78ms | 10,000 | 4879 | 전국 | emd | tradeProperty |
| 47 | 76ms | 10,000 | 243 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 48 | 76ms | 10,000 | 1311 | 경기 | emd |  |
| 49 | 76ms | 10,000 | 17 | 전국 | sd | landPriceMin, landPriceMax |
| 50 | 76ms | 7,693 | 76 | 경기 | sgg | landJiyukCd1, landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 51 | 75ms | 10,000 | 17 | 전국 | sd | tradeProperty |
| 52 | 75ms | 7,671 | 2 | 제주 | sgg | buildingUseAprDayStart, buildingUseAprDayEnd |
| 53 | 74ms | 10,000 | 17 | 전국 | sd | landJiyukCd1 |
| 54 | 73ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingMainPurpsCdNm |
| 55 | 72ms | 10,000 | 1311 | 경기 | emd |  |
| 56 | 72ms | 10,000 | 17 | 전국 | sd | buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 57 | 71ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingTotAreaMin, buildingTotAreaMax |
| 58 | 69ms | 10,000 | 1299 | 경기 | emd | landAreaMin, landAreaMax |
| 59 | 69ms | 10,000 | 929 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 60 | 69ms | 10,000 | 251 | 전국 | sgg | landJiyukCd1 |
| 61 | 68ms | 10,000 | 252 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 62 | 67ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax |
| 63 | 67ms | 10,000 | 1311 | 경기 | emd |  |
| 64 | 67ms | 10,000 | 252 | 전국 | sgg | tradeProperty |
| 65 | 66ms | 10,000 | 1311 | 경기 | emd |  |
| 66 | 66ms | 197 | 63 | 경기 | emd | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax |
| 67 | 65ms | 10,000 | 1208 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 68 | 64ms | 10,000 | 251 | 전국 | sgg | landJiyukCd1, landPriceMin, landPriceMax |
| 69 | 63ms | 0 | 0 | 전국 | sd | landJiyukCd1, landJimokCd, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 70 | 62ms | 10,000 | 474 | 서울 | emd | buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, tradeProperty |
| 71 | 62ms | 10,000 | 687 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 72 | 60ms | 20 | 5 | 대구 | sgg | landJiyukCd1, landAreaMin, landAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 73 | 59ms | 10,000 | 1059 | 경기 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 74 | 59ms | 18 | 3 | 경기 | sd | landJiyukCd1, landAreaMin, landAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 75 | 58ms | 10,000 | 1014 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 76 | 57ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 77 | 56ms | 0 | 0 | 서울 | emd | landJimokCd, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 78 | 56ms | 10,000 | 756 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 79 | 55ms | 10,000 | 5 | 경기 | sd | tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 80 | 55ms | 10,000 | 87 | 경기 | sgg | landAreaMin, landAreaMax |
| 81 | 54ms | 3,228 | 70 | 경기 | sgg | landJiyukCd1, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 82 | 54ms | 10,000 | 4683 | 전국 | emd | tradeProperty |
| 83 | 53ms | 10,000 | 162 | 전국 | sgg | landJiyukCd1 |
| 84 | 53ms | 10,000 | 17 | 전국 | sd | buildingRegstrGbCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 85 | 53ms | 10,000 | 2398 | 전국 | emd | landPriceMin, landPriceMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 86 | 53ms | 10,000 | 1127 | 경기 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 87 | 53ms | 10,000 | 88 | 경기 | sgg |  |
| 88 | 52ms | 10,000 | 88 | 경기 | sgg |  |
| 89 | 51ms | 10,000 | 252 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 90 | 51ms | 10,000 | 5 | 경기 | sd |  |
| 91 | 50ms | 10,000 | 631 | 서울 | emd | tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 92 | 50ms | 0 | 0 | 전국 | emd | landJimokCd, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 93 | 49ms | 10,000 | 17 | 전국 | sd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 94 | 49ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 95 | 49ms | 10,000 | 5 | 경기 | sd |  |
| 96 | 49ms | 10,000 | 1067 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 97 | 49ms | 10,000 | 47 | 경기 | sgg | landPriceMin, landPriceMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 98 | 49ms | 10,000 | 69 | 경기 | sgg | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax |
| 99 | 48ms | 10,000 | 16 | 전국 | sd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 100 | 48ms | 10,000 | 88 | 경기 | sgg |  |

## lnbtpu23 (2/3 local f)

> source: `bench_lnbtpu23.json` | n=2000 | avg=26.1ms | p99=309ms

| Rank | took_ms | hits | buckets | bbox | agg | filters |
|------|---------|------|---------|------|-----|---------|
| 1 | 747ms | 10,000 | 5000 | 전국 | emd |  |
| 2 | 719ms | 10,000 | 5000 | 전국 | emd |  |
| 3 | 640ms | 10,000 | 252 | 전국 | sgg |  |
| 4 | 630ms | 10,000 | 5000 | 전국 | emd |  |
| 5 | 561ms | 10,000 | 252 | 전국 | sgg |  |
| 6 | 529ms | 10,000 | 17 | 전국 | sd |  |
| 7 | 510ms | 10,000 | 17 | 전국 | sd |  |
| 8 | 487ms | 10,000 | 252 | 전국 | sgg |  |
| 9 | 474ms | 10,000 | 252 | 전국 | sgg |  |
| 10 | 459ms | 10,000 | 5000 | 전국 | emd | landAreaMin, landAreaMax |
| 11 | 456ms | 10,000 | 252 | 전국 | sgg |  |
| 12 | 452ms | 10,000 | 1045 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 13 | 428ms | 4,057 | 61 | 경기 | sgg | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 14 | 379ms | 27 | 8 | 광주 | sgg | landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty |
| 15 | 354ms | 19 | 16 | 광주 | emd | landJiyukCd1, buildingMainPurpsCdNm, buildingRegstrGbCdNm, tradeProperty |
| 16 | 351ms | 1 | 1 | 제주 | emd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd |
| 17 | 340ms | 0 | 0 | 전국 | emd | tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 18 | 334ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax |
| 19 | 325ms | 10,000 | 3080 | 전국 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 20 | 309ms | 10,000 | 252 | 전국 | sgg | landJimokCd |
| 21 | 308ms | 10,000 | 2 | 인천 | sd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 22 | 308ms | 10,000 | 1129 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 23 | 305ms | 10,000 | 1579 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 24 | 300ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 25 | 297ms | 10,000 | 17 | 전국 | sd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 26 | 294ms | 4,507 | 14 | 전국 | sd | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingPmsDayRecent5y |
| 27 | 254ms | 10,000 | 2974 | 전국 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm |
| 28 | 254ms | 10,000 | 4 | 서울 | sd | tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 29 | 251ms | 10,000 | 17 | 전국 | sd | tradeProperty |
| 30 | 251ms | 10,000 | 4594 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 31 | 251ms | 10,000 | 17 | 전국 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 32 | 244ms | 10,000 | 17 | 전국 | sd | landJiyukCd1 |
| 33 | 242ms | 25 | 16 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 34 | 230ms | 10,000 | 252 | 전국 | sgg | landJimokCd |
| 35 | 227ms | 2,084 | 110 | 대전 | emd | tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 36 | 226ms | 5,132 | 610 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingStcnsDayRecent5y |
| 37 | 225ms | 0 | 0 | 경기 | emd | landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingUseAprDayStart, buildingUseAprDayEnd, buildingPmsDayRecent5y |
| 38 | 221ms | 10,000 | 893 | 경기 | emd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 39 | 218ms | 0 | 0 | 제주 | sd | landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, buildingPmsDayRecent5y, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 40 | 206ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 41 | 204ms | 10,000 | 3390 | 전국 | emd | landJiyukCd1 |
| 42 | 198ms | 18 | 3 | 경기 | sd | landJiyukCd1, landAreaMin, landAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 43 | 197ms | 2,829 | 169 | 대구 | emd | tradeContractDateStart, tradeContractDateEnd |
| 44 | 195ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingTotAreaMin, buildingTotAreaMax |
| 45 | 194ms | 10,000 | 5 | 경기 | sd | landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd |
| 46 | 194ms | 10,000 | 1067 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 47 | 193ms | 10,000 | 5 | 경기 | sd | landPriceMin, landPriceMax |
| 48 | 175ms | 10,000 | 5 | 경기 | sd | tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 49 | 171ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 50 | 165ms | 417 | 2 | 부산 | sd | landJiyukCd1, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 51 | 161ms | 2,038 | 126 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax, buildingStcnsDayRecent5y |
| 52 | 161ms | 0 | 0 | 경기 | sgg | landJimokCd, landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingRegstrGbCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 53 | 161ms | 10,000 | 5 | 경기 | sd | landAreaMin, landAreaMax |
| 54 | 160ms | 10,000 | 251 | 전국 | sgg | landJiyukCd1, landPriceMin, landPriceMax |
| 55 | 160ms | 2,322 | 28 | 전국 | sgg | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 56 | 160ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 57 | 156ms | 62 | 45 | 전국 | sgg | landJimokCd, landAreaMin, landAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 58 | 153ms | 10,000 | 17 | 전국 | sd | buildingMainPurpsCdNm, buildingRegstrGbCdNm |
| 59 | 151ms | 10,000 | 1663 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 60 | 151ms | 2 | 1 | 경기 | sd | buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty |
| 61 | 150ms | 10,000 | 893 | 전국 | emd | landJiyukCd1, buildingMainPurpsCdNm |
| 62 | 148ms | 10,000 | 4859 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 63 | 146ms | 10,000 | 80 | 경기 | sgg | landJiyukCd1, buildingRegstrGbCdNm, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 64 | 143ms | 10,000 | 17 | 전국 | sd | landJiyukCd1, landAreaMin, landAreaMax |
| 65 | 143ms | 10,000 | 5 | 경기 | sd | tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 66 | 143ms | 10,000 | 5000 | 전국 | emd | buildingUseAprDayStart, buildingUseAprDayEnd |
| 67 | 139ms | 10,000 | 4958 | 전국 | emd | tradeContractDateStart, tradeContractDateEnd |
| 68 | 139ms | 5,046 | 363 | 경기 | emd | landJiyukCd1, landJimokCd, tradeContractDateStart, tradeContractDateEnd |
| 69 | 136ms | 10,000 | 3737 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 70 | 134ms | 54 | 2 | 부산 | sd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingUseAprDayStart, buildingUseAprDayEnd |
| 71 | 134ms | 10,000 | 83 | 경기 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 72 | 133ms | 0 | 0 | 전국 | sgg | landJiyukCd1, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 73 | 133ms | 10,000 | 47 | 경기 | sgg | landPriceMin, landPriceMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 74 | 130ms | 10,000 | 5000 | 전국 | emd | buildingTotAreaMin, buildingTotAreaMax |
| 75 | 130ms | 10,000 | 82 | 경기 | sgg | tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 76 | 130ms | 10,000 | 5000 | 전국 | emd | buildingRegstrGbCdNm |
| 77 | 129ms | 8 | 7 | 전국 | emd | landJiyukCd1, landJimokCd, buildingMainPurpsCdNm, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 78 | 129ms | 654 | 416 | 전국 | emd | landJiyukCd1, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 79 | 128ms | 10,000 | 1197 | 전국 | emd | landJiyukCd1, landJimokCd, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd |
| 80 | 126ms | 10,000 | 1299 | 경기 | emd | landAreaMin, landAreaMax |
| 81 | 126ms | 10,000 | 162 | 전국 | sgg | landJiyukCd1 |
| 82 | 124ms | 849 | 4 | 경기 | sd | landJimokCd, landAreaMin, landAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 83 | 123ms | 10,000 | 243 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 84 | 123ms | 2,479 | 337 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 85 | 120ms | 10,000 | 69 | 경기 | sgg | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax |
| 86 | 120ms | 10,000 | 252 | 전국 | sgg | tradeContractDateStart, tradeContractDateEnd |
| 87 | 118ms | 10,000 | 50 | 서울 | sgg | tradeContractDateStart, tradeContractDateEnd |
| 88 | 118ms | 10,000 | 5000 | 전국 | emd | tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 89 | 118ms | 10,000 | 452 | 경기 | emd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 90 | 117ms | 10,000 | 2493 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 91 | 116ms | 101 | 41 | 전국 | sgg | landAreaMin, landAreaMax, buildingMainPurpsCdNm, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 92 | 115ms | 10,000 | 3208 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 93 | 115ms | 10,000 | 84 | 경기 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd |
| 94 | 115ms | 10,000 | 252 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 95 | 114ms | 7,693 | 76 | 경기 | sgg | landJiyukCd1, landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 96 | 113ms | 10,000 | 3845 | 전국 | emd | landPriceMin, landPriceMax |
| 97 | 113ms | 2,594 | 222 | 경기 | emd | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingStcnsDayRecent5y |
| 98 | 112ms | 7,936 | 16 | 전국 | sd | buildingRegstrGbCdNm, buildingStcnsDayRecent5y, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 99 | 112ms | 10,000 | 1127 | 경기 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 100 | 111ms | 10,000 | 17 | 전국 | sd | buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax |

## lnbtps_nf (17/2 local nf)

> source: `bench_lnbtps_nf.json` | n=2000 | avg=42.8ms | p99=380ms

| Rank | took_ms | hits | buckets | bbox | agg | filters |
|------|---------|------|---------|------|-----|---------|
| 1 | 674ms | 10,000 | 17 | 전국 | sd | landJiyukCd1 |
| 2 | 621ms | 2,498 | 117 | 인천 | emd | buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 3 | 555ms | 4,507 | 14 | 전국 | sd | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingPmsDayRecent5y |
| 4 | 553ms | 10,000 | 5000 | 전국 | emd |  |
| 5 | 537ms | 10,000 | 17 | 전국 | sd |  |
| 6 | 521ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 7 | 520ms | 10,000 | 5000 | 전국 | emd |  |
| 8 | 505ms | 10,000 | 162 | 전국 | sgg | landJiyukCd1 |
| 9 | 499ms | 10,000 | 252 | 전국 | sgg |  |
| 10 | 494ms | 10,000 | 5000 | 전국 | emd | landAreaMin, landAreaMax |
| 11 | 474ms | 10,000 | 252 | 전국 | sgg |  |
| 12 | 456ms | 213 | 66 | 광주 | emd | buildingMainPurpsCdNm, buildingRegstrGbCdNm |
| 13 | 453ms | 10,000 | 252 | 전국 | sgg | landJimokCd |
| 14 | 447ms | 10,000 | 252 | 전국 | sgg |  |
| 15 | 444ms | 10,000 | 5000 | 전국 | emd |  |
| 16 | 441ms | 10,000 | 252 | 전국 | sgg |  |
| 17 | 411ms | 10,000 | 17 | 전국 | sd |  |
| 18 | 390ms | 10,000 | 50 | 서울 | sgg | tradeContractDateStart, tradeContractDateEnd |
| 19 | 389ms | 10,000 | 3080 | 전국 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 20 | 380ms | 0 | 0 | 대구 | emd | landJimokCd, landAreaMin, landAreaMax, buildingRegstrGbCdNm, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 21 | 378ms | 10,000 | 252 | 전국 | sgg | tradeContractDateStart, tradeContractDateEnd |
| 22 | 370ms | 10,000 | 252 | 전국 | sgg |  |
| 23 | 346ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax |
| 24 | 334ms | 2,322 | 28 | 전국 | sgg | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 25 | 333ms | 136 | 2 | 인천 | sd | landJimokCd, buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingStcnsDayRecent5y, tradeProperty |
| 26 | 332ms | 10,000 | 4594 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 27 | 327ms | 10,000 | 1213 | 경기 | emd | buildingUseAprDayStart, buildingUseAprDayEnd |
| 28 | 311ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingMainPurpsCdNm |
| 29 | 308ms | 10,000 | 3208 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 30 | 303ms | 3,726 | 460 | 서울 | emd | buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 31 | 301ms | 10,000 | 1140 | 경기 | emd | buildingTotAreaMin, buildingTotAreaMax, tradeProperty |
| 32 | 294ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 33 | 287ms | 9,301 | 65 | 제주 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 34 | 285ms | 10,000 | 4859 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 35 | 278ms | 10,000 | 1311 | 경기 | emd |  |
| 36 | 272ms | 10,000 | 1579 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 37 | 258ms | 10,000 | 88 | 경기 | sgg |  |
| 38 | 257ms | 673 | 2 | 경기 | sd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingTotAreaMin, buildingTotAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 39 | 254ms | 10,000 | 3390 | 전국 | emd | landJiyukCd1 |
| 40 | 248ms | 6,668 | 129 | 대전 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 41 | 240ms | 78 | 28 | 서울 | emd | landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingPmsDayRecent5y, tradeContractDateStart, tradeContractDateEnd |
| 42 | 240ms | 4,057 | 61 | 경기 | sgg | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 43 | 235ms | 10,000 | 17 | 전국 | sd | tradeProperty |
| 44 | 234ms | 10,000 | 126 | 대구 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 45 | 232ms | 5,371 | 648 | 전국 | emd | landJiyukCd1, landJimokCd |
| 46 | 231ms | 5,735 | 250 | 전국 | sgg | buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty |
| 47 | 228ms | 0 | 0 | 대전 | sgg | buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 48 | 226ms | 10,000 | 2974 | 전국 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm |
| 49 | 225ms | 10,000 | 2 | 인천 | sd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 50 | 211ms | 10,000 | 14 | 인천 | sgg | tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 51 | 207ms | 10,000 | 234 | 전국 | sgg | landJiyukCd1, landPriceMin, landPriceMax |
| 52 | 207ms | 106 | 2 | 부산 | sd | landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingStcnsDayRecent5y |
| 53 | 206ms | 10,000 | 5000 | 전국 | emd | buildingTotAreaMin, buildingTotAreaMax |
| 54 | 201ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingMainPurpsCdNm |
| 55 | 198ms | 10,000 | 17 | 전국 | sd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 56 | 197ms | 2,783 | 17 | 전국 | sd | landJimokCd, buildingRegstrGbCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 57 | 196ms | 5 | 3 | 경기 | sgg | buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingStcnsDayRecent5y |
| 58 | 193ms | 10,000 | 3 | 서울 | sd | tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 59 | 187ms | 10,000 | 247 | 전국 | sgg | landJiyukCd1, landJimokCd |
| 60 | 185ms | 7,243 | 213 | 전국 | sgg | landPriceMin, landPriceMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 61 | 180ms | 10,000 | 251 | 전국 | sgg | landJiyukCd1 |
| 62 | 179ms | 0 | 0 | 전국 | emd | landJimokCd, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 63 | 178ms | 10,000 | 1045 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 64 | 177ms | 10,000 | 4879 | 전국 | emd | tradeProperty |
| 65 | 177ms | 10,000 | 3737 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 66 | 176ms | 21 | 9 | 대전 | emd | landJiyukCd1, buildingMainPurpsCdNm |
| 67 | 175ms | 10,000 | 5000 | 전국 | emd | buildingUseAprDayStart, buildingUseAprDayEnd |
| 68 | 172ms | 10,000 | 243 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 69 | 170ms | 447 | 15 | 부산 | sgg | landJimokCd, landAreaMin, landAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty |
| 70 | 169ms | 10,000 | 251 | 전국 | sgg | landJiyukCd1, landPriceMin, landPriceMax |
| 71 | 167ms | 0 | 0 | 광주 | emd | buildingMainPurpsCdNm, buildingPmsDayRecent5y, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 72 | 161ms | 1 | 1 | 경기 | sgg | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingStcnsDayRecent5y |
| 73 | 160ms | 10,000 | 1129 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 74 | 160ms | 10,000 | 1311 | 경기 | emd |  |
| 75 | 159ms | 10,000 | 157 | 전국 | sgg | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax |
| 76 | 158ms | 10,000 | 120 | 부산 | emd | landJimokCd |
| 77 | 157ms | 10,000 | 136 | 인천 | emd | tradeContractDateStart, tradeContractDateEnd |
| 78 | 157ms | 1,103 | 66 | 인천 | emd | buildingMainPurpsCdNm |
| 79 | 155ms | 10,000 | 252 | 전국 | sgg | tradeProperty |
| 80 | 154ms | 8 | 2 | 서울 | sd | buildingMainPurpsCdNm, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 81 | 153ms | 417 | 2 | 부산 | sd | landJiyukCd1, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 82 | 150ms | 8 | 6 | 부산 | emd | buildingMainPurpsCdNm, buildingPmsDayRecent5y |
| 83 | 149ms | 10,000 | 3845 | 전국 | emd | landPriceMin, landPriceMax |
| 84 | 148ms | 2,084 | 110 | 대전 | emd | tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 85 | 147ms | 10,000 | 18 | 부산 | sgg | buildingMainPurpsCdNm |
| 86 | 144ms | 10,000 | 4958 | 전국 | emd | tradeContractDateStart, tradeContractDateEnd |
| 87 | 140ms | 10,000 | 893 | 전국 | emd | landJiyukCd1, buildingMainPurpsCdNm |
| 88 | 138ms | 33 | 27 | 전국 | sgg | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 89 | 136ms | 0 | 0 | 전국 | emd | tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 90 | 136ms | 1 | 1 | 부산 | sd | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 91 | 136ms | 0 | 0 | 광주 | emd | landJimokCd, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingPmsDayRecent5y, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 92 | 136ms | 10,000 | 5000 | 전국 | emd | tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 93 | 136ms | 10,000 | 1 | 제주 | sd | landJiyukCd1, landJimokCd |
| 94 | 133ms | 10,000 | 17 | 전국 | sd | landJiyukCd1, landJimokCd, tradeProperty |
| 95 | 133ms | 10,000 | 133 | 부산 | emd | buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, tradeProperty, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 96 | 133ms | 10,000 | 2493 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 97 | 132ms | 10,000 | 252 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 98 | 131ms | 1,564 | 48 | 서울 | sgg | buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 99 | 131ms | 10,000 | 87 | 경기 | sgg | landAreaMin, landAreaMax |
| 100 | 131ms | 10,000 | 1311 | 경기 | emd |  |

## lnbtpunf (4/4 local nf)

> source: `bench_lnbtpunf_noforcemerge.json` | n=2000 | avg=35.5ms | p99=282ms

| Rank | took_ms | hits | buckets | bbox | agg | filters |
|------|---------|------|---------|------|-----|---------|
| 1 | 705ms | 10,000 | 5000 | 전국 | emd |  |
| 2 | 664ms | 10,000 | 5000 | 전국 | emd |  |
| 3 | 588ms | 10,000 | 5000 | 전국 | emd |  |
| 4 | 587ms | 10,000 | 17 | 전국 | sd |  |
| 5 | 581ms | 1,375 | 8 | 광주 | sgg | buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 6 | 579ms | 10,000 | 252 | 전국 | sgg |  |
| 7 | 551ms | 10,000 | 252 | 전국 | sgg |  |
| 8 | 511ms | 10,000 | 5000 | 전국 | emd | landAreaMin, landAreaMax |
| 9 | 508ms | 10,000 | 252 | 전국 | sgg |  |
| 10 | 477ms | 10,000 | 17 | 전국 | sd |  |
| 11 | 436ms | 10,000 | 252 | 전국 | sgg |  |
| 12 | 433ms | 10,000 | 17 | 전국 | sd | tradeProperty |
| 13 | 409ms | 10,000 | 252 | 전국 | sgg |  |
| 14 | 391ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax |
| 15 | 375ms | 10,000 | 5 | 경기 | sd | tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 16 | 334ms | 10,000 | 82 | 경기 | sgg | buildingTotAreaMin, buildingTotAreaMax, tradeProperty |
| 17 | 316ms | 10,000 | 1256 | 경기 | emd | buildingMainPurpsCdNm |
| 18 | 305ms | 10,000 | 5000 | 전국 | emd | tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 19 | 295ms | 10,000 | 252 | 전국 | sgg | landPriceMin, landPriceMax |
| 20 | 282ms | 10,000 | 2974 | 전국 | emd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm |
| 21 | 261ms | 0 | 0 | 서울 | sd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingUseAprDayStart, buildingUseAprDayEnd, buildingPmsDayRecent5y |
| 22 | 255ms | 10,000 | 5 | 경기 | sd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 23 | 250ms | 0 | 0 | 세종 | sgg | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 24 | 233ms | 1,385 | 86 | 대전 | emd | buildingMainPurpsCdNm, buildingRegstrGbCdNm, tradeProperty, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 25 | 222ms | 10,000 | 3390 | 전국 | emd | landJiyukCd1 |
| 26 | 221ms | 150 | 13 | 전국 | sd | buildingRegstrGbCdNm, buildingPmsDayRecent5y, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 27 | 207ms | 673 | 2 | 경기 | sd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingTotAreaMin, buildingTotAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 28 | 202ms | 9,506 | 1 | 세종 | sd | tradeProperty |
| 29 | 202ms | 0 | 0 | 전국 | sgg | landPriceMin, landPriceMax, buildingRegstrGbCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, buildingStcnsDayRecent5y, tradeContractDateStart, tradeContractDateEnd |
| 30 | 201ms | 10,000 | 3080 | 전국 | emd | landJiyukCd1, landPriceMin, landPriceMax |
| 31 | 198ms | 10,000 | 4594 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 32 | 198ms | 1,105 | 53 | 제주 | emd | buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 33 | 195ms | 30 | 18 | 경기 | emd | tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 34 | 194ms | 10,000 | 1129 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 35 | 191ms | 9 | 2 | 전국 | sgg | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd |
| 36 | 191ms | 10,000 | 3221 | 전국 | emd | landJiyukCd1, buildingMainPurpsCdNm |
| 37 | 190ms | 10,000 | 17 | 전국 | sd | buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty |
| 38 | 188ms | 0 | 0 | 제주 | sgg | landJiyukCd1, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd |
| 39 | 185ms | 10,000 | 5 | 경기 | sd | tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 40 | 183ms | 10,000 | 1208 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 41 | 181ms | 10,000 | 5000 | 전국 | emd | buildingRegstrGbCdNm |
| 42 | 180ms | 10,000 | 252 | 전국 | sgg | tradeProperty |
| 43 | 180ms | 10,000 | 3737 | 전국 | emd | landJiyukCd1, landAreaMin, landAreaMax |
| 44 | 174ms | 10,000 | 3845 | 전국 | emd | landPriceMin, landPriceMax |
| 45 | 173ms | 10,000 | 252 | 전국 | sgg | landJimokCd |
| 46 | 171ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 47 | 166ms | 10,000 | 251 | 전국 | sgg | landJiyukCd1, landPriceMin, landPriceMax |
| 48 | 164ms | 10,000 | 86 | 경기 | sgg | landPriceMin, landPriceMax |
| 49 | 164ms | 10,000 | 252 | 전국 | sgg | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 50 | 162ms | 4,507 | 14 | 전국 | sd | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingPmsDayRecent5y |
| 51 | 159ms | 32 | 5 | 대전 | sgg | landJiyukCd1, buildingMainPurpsCdNm, buildingRegstrGbCdNm, tradeProperty |
| 52 | 158ms | 1 | 1 | 인천 | sd | landJiyukCd1, buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 53 | 158ms | 10,000 | 1014 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 54 | 157ms | 10,000 | 169 | 광주 | emd | tradeProperty |
| 55 | 156ms | 417 | 2 | 부산 | sd | landJiyukCd1, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 56 | 156ms | 820 | 2 | 광주 | sd | buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd |
| 57 | 155ms | 0 | 0 | 광주 | sd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 58 | 154ms | 476 | 2 | 광주 | sd | buildingTotAreaMin, buildingTotAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 59 | 150ms | 43 | 11 | 광주 | emd | landJiyukCd1, landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax |
| 60 | 149ms | 10,000 | 17 | 전국 | sd | landJiyukCd1, landAreaMin, landAreaMax |
| 61 | 147ms | 10,000 | 185 | 부산 | emd | landAreaMin, landAreaMax |
| 62 | 147ms | 1,300 | 160 | 전국 | sgg | landJimokCd, landAreaMin, landAreaMax, tradeProperty |
| 63 | 144ms | 0 | 0 | 전국 | emd | tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 64 | 143ms | 2,322 | 28 | 전국 | sgg | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 65 | 142ms | 2,962 | 3 | 서울 | sd | buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingPmsDayRecent5y |
| 66 | 142ms | 10,000 | 162 | 전국 | sgg | landJiyukCd1 |
| 67 | 142ms | 10,000 | 17 | 전국 | sd | landPriceMin, landPriceMax |
| 68 | 141ms | 10,000 | 1311 | 경기 | emd |  |
| 69 | 140ms | 656 | 60 | 경기 | sgg | landJimokCd, landAreaMin, landAreaMax, tradeContractDateStart, tradeContractDateEnd |
| 70 | 139ms | 228 | 70 | 서울 | emd | landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd, tradeProperty, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 71 | 139ms | 10,000 | 252 | 전국 | sgg | landAreaMin, landAreaMax, buildingRegstrGbCdNm |
| 72 | 137ms | 293 | 204 | 전국 | emd | landJimokCd, landAreaMin, landAreaMax, landPriceMin, landPriceMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 73 | 135ms | 1 | 1 | 제주 | sd | landJimokCd, landPriceMin, landPriceMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 74 | 135ms | 0 | 0 | 부산 | sd | buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd, buildingStcnsDayRecent5y, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 75 | 133ms | 16 | 16 | 대구 | emd | buildingMainPurpsCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 76 | 132ms | 4,057 | 61 | 경기 | sgg | landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingRegstrGbCdNm, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 77 | 132ms | 0 | 0 | 대구 | sgg | buildingMainPurpsCdNm, buildingRegstrGbCdNm, buildingStcnsDayRecent5y, tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 78 | 132ms | 10,000 | 252 | 전국 | sgg | landJimokCd |
| 79 | 131ms | 10,000 | 5000 | 전국 | emd | buildingUseAprDayStart, buildingUseAprDayEnd |
| 80 | 130ms | 0 | 0 | 경기 | sgg | landJiyukCd1, landJimokCd, landAreaMin, landAreaMax, buildingMainPurpsCdNm, buildingRegstrGbCdNm, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 81 | 128ms | 10,000 | 5000 | 전국 | emd | buildingTotAreaMin, buildingTotAreaMax |
| 82 | 128ms | 1,275 | 188 | 전국 | sgg | landJiyukCd1, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 83 | 128ms | 10,000 | 5 | 경기 | sd | tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 84 | 128ms | 10,000 | 4859 | 전국 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd |
| 85 | 125ms | 1,259 | 10 | 전국 | sd | landJiyukCd1, landPriceMin, landPriceMax, tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 86 | 123ms | 10,000 | 2 | 인천 | sd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax |
| 87 | 121ms | 10,000 | 3174 | 전국 | emd | landAreaMin, landAreaMax, buildingMainPurpsCdNm, tradeProperty |
| 88 | 121ms | 10,000 | 196 | 대구 | emd | tradeProperty |
| 89 | 121ms | 10,000 | 17 | 전국 | sd | landJiyukCd1 |
| 90 | 120ms | 57 | 1 | 서울 | sd | landAreaMin, landAreaMax, landPriceMin, landPriceMax, tradeContractDateStart, tradeContractDateEnd, tradeBuildingAmountPerM2Min, tradeBuildingAmountPerM2Max |
| 91 | 119ms | 10,000 | 81 | 경기 | sgg | buildingTotAreaMin, buildingTotAreaMax, buildingUseAprDayStart, buildingUseAprDayEnd |
| 92 | 119ms | 0 | 0 | 세종 | sd | landJiyukCd1, landJimokCd, buildingMainPurpsCdNm, buildingUseAprDayStart, buildingUseAprDayEnd |
| 93 | 117ms | 10,000 | 1045 | 경기 | emd | tradeProperty, tradeContractDateStart, tradeContractDateEnd, tradeEffectiveAmountMin, tradeEffectiveAmountMax |
| 94 | 116ms | 10,000 | 4958 | 전국 | emd | tradeContractDateStart, tradeContractDateEnd |
| 95 | 114ms | 10,000 | 3 | 서울 | sd | buildingUseAprDayStart, buildingUseAprDayEnd |
| 96 | 113ms | 10,000 | 247 | 전국 | sgg | landJiyukCd1, landJimokCd |
| 97 | 112ms | 1 | 1 | 경기 | emd | landJiyukCd1, landAreaMin, landAreaMax, landPriceMin, landPriceMax, buildingMainPurpsCdNm, buildingStcnsDayRecent5y |
| 98 | 111ms | 10,000 | 17 | 전국 | sd | landJiyukCd1, landJimokCd, tradeProperty |
| 99 | 111ms | 10,000 | 82 | 경기 | sgg | buildingTotAreaMin, buildingTotAreaMax, buildingPmsDayRecent5y |
| 100 | 111ms | 10,000 | 4 | 서울 | sd |  |
