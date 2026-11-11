# NEXAPREMIUM — TESTING & VERIFICATION REPORT

**Target Application:** NEXAPREMIUM (`com.nexapremium`)  
**Backend Host:** `https://movie-box-sepia-six.vercel.app`  
**QA Lead:** Agent 6 (QA / Performance Engineer)  

---

## 1. BACKEND API VERIFICATION SUMMARY

| Endpoint | Method | Path/Params | Expected Status | Result Status | Latency | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `/health` | GET | None | 200 | 200 | 592 ms | PASSED |
| `/v1/home` | GET | None | 200 | 200 | 1064 ms | PASSED |
| `/v1/browse` | GET | None | 200 | 200 | 813 ms | PASSED |
| `/v1/search` | GET | `q=batman` | 200 | 200 | 1470 ms | PASSED |
| `/v1/search/suggestions` | GET | `q=batman` | 200 | 200 | 769 ms | PASSED |
| `/v1/search/popular` | GET | None | 200 | 200 | 975 ms | PASSED |
| `/v1/rankings` | GET | None | 200 | 200 | 665 ms | PASSED |
| `/v1/rankings/{id}` | GET | `ranking_id=62133389738001440` | 200 | 200 | 1038 ms | PASSED |
| `/v1/catalog/series/filters` | GET | `name=series` | 200 | 200 | 693 ms | PASSED |
| `/v1/catalog/movies/filters` | GET | `name=movies` | 404 | 404 | 735 ms | HANDLED (404) |
| `/v1/titles/{path}` | GET | `batman-MenTqxvHqCa` | 200 | 200 | 773 ms | PASSED |
| `/v1/titles/{path}/playback` | GET | `season=0&episode=0` | 200 | 200 | 761 ms | PASSED |
| `/v1/titles/{path}/captions` | GET | `stream_id=...` | 200 | 200 | 852 ms | PASSED |

---

## 2. STREAM MEDIA & HEADERS AUDIT

- **MP4 Stream Access:** `HTTP 200 OK` verified with headers (`Referer`, `Origin`, `User-Agent: okhttp/4.11.0`).
- **DASH Stream Access:** `HTTP 403 Forbidden` on `sbcdnw.hakunaymatata.com`. (Rule: Filter out DASH streams in media player).
- **Subtitles (SRT):** `HTTP 200 OK` verified with CloudFront signed parameters.

---

## 3. UI & COMPONENT VERIFICATION MATRIX (PENDING PHASE 3)

| Component / Screen | Phone Layout | TV D-Pad Focus | Data Integration | Status |
| :--- | :--- | :--- | :--- | :--- |
| Splash Screen | Pending | Pending | N/A | PENDING |
| Onboarding | Pending | Pending | N/A | PENDING |
| Home Screen | Pending | Pending | Pending | PENDING |
| Movies Screen | Pending | Pending | Pending | PENDING |
| Series Screen | Pending | Pending | Pending | PENDING |
| Search Screen | Pending | Pending | Pending | PENDING |
| Detail Screen | Pending | Pending | Pending | PENDING |
| Video Player | Pending | Pending | Pending | PENDING |
| My List | Pending | Pending | Pending | PENDING |
| Continue Watching | Pending | Pending | Pending | PENDING |
| Settings | Pending | Pending | Pending | PENDING |
