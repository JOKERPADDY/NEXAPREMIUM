# NEXAPREMIUM — KNOWN ISSUES & TECHNICAL LIMITATIONS LOG

**Project:** NEXAPREMIUM (`com.nexapremium`)  

---

## 1. BACKEND BEHAVIORAL QUIRKS & LIMITATIONS

1. **DASH Stream 403 Forbidden:**
   - **Issue:** The backend returns DASH manifest streams (`format: "DASH"`, host `sbcdnw.hakunaymatata.com`), but the CDN rejects requests with `403 Forbidden`.
   - **Mitigation:** Agent 4 (Media Player Developer) will explicitly filter streams to select only MP4 direct URLs (`format: "MP4"`).

2. **Default ExoPlayer User-Agent Rejection:**
   - **Issue:** Video CDN `bcdnxw.hakunaymatata.com` rejects requests using default ExoPlayer User-Agent strings with `HTTP 428 Precondition Required`.
   - **Mitigation:** Agent 4 will configure `OkHttpDataSource.Factory` to inject `User-Agent: okhttp/4.11.0` alongside required `Referer` and `Origin` headers.

3. **Movies Catalog Filters 404:**
   - **Issue:** Calling `GET /v1/catalog/movies/filters` returns `HTTP 404 Not Found`, whereas `GET /v1/catalog/series/filters` returns `HTTP 200 OK`.
   - **Mitigation:** Agent 3 (API Developer) will gracefully fallback to default genre/sort options when displaying movie catalog filters.

4. **Lack of Backend User Accounts & Auth:**
   - **Issue:** The backend has no authentication, bookmarking, or watch history endpoints.
   - **Mitigation:** Agent 5 (Database Developer) will persist Continue Watching, My List, and playback progress locally using Room DB.

5. **Vercel Cold Starts:**
   - **Issue:** Initial response times from Vercel Serverless Functions can take up to 1.5–2.0 seconds.
   - **Mitigation:** Agent 2 (UI Developer) will implement animated skeleton shimmer placeholders for all carousels and grids.
