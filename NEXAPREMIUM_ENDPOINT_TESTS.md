# NEXAPREMIUM — BACKEND ENDPOINT TESTING & VERIFICATION REPORT

**Target Base URL:** `https://movie-box-sepia-six.vercel.app`  
**Audit Execution Date:** November 2026  
**Testing Tooling:** Automated Python Probing Engine (`urllib.request`)  

---

## 1. COMPREHENSIVE ENDPOINT TEST MATRIX

| Key | Target Path / Query | HTTP Method | Expected Status | Actual Status | Time (ms) | Verification Result |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `health` | `/health` | `GET` | `200` | `200` | `592.01` | **VERIFIED PASS** |
| `v1_home` | `/v1/home` | `GET` | `200` | `200` | `1064.89` | **VERIFIED PASS** |
| `v1_browse` | `/v1/browse` | `GET` | `200` | `200` | `813.44` | **VERIFIED PASS** |
| `v1_search_batman` | `/v1/search?q=batman` | `GET` | `200` | `200` | `1470.70` | **VERIFIED PASS** |
| `v1_search_suggestions_batman` | `/v1/search/suggestions?q=batman` | `GET` | `200` | `200` | `769.24` | **VERIFIED PASS** |
| `v1_search_popular` | `/v1/search/popular` | `GET` | `200` | `200` | `975.26` | **VERIFIED PASS** |
| `v1_rankings` | `/v1/rankings` | `GET` | `200` | `200` | `665.08` | **VERIFIED PASS** |
| `v1_ranking_detail` | `/v1/rankings/62133389738001440` | `GET` | `200` | `200` | `1038.95` | **VERIFIED PASS** |
| `v1_catalog_series_filters` | `/v1/catalog/series/filters` | `GET` | `200` | `200` | `693.06` | **VERIFIED PASS** |
| `v1_catalog_movies_filters` | `/v1/catalog/movies/filters` | `GET` | `200` | `404` | `735.93` | **FAIL** |
| `v1_catalog_series` | `/v1/catalog/series?page=1` | `GET` | `200` | `200` | `1279.23` | **VERIFIED PASS** |
| `v1_catalog_movies` | `/v1/catalog/movies?page=1` | `GET` | `200` | `200` | `1206.88` | **VERIFIED PASS** |
| `v1_collections_dynamic` | `/v1/collections/popular-series` | `GET` | `200` | `404` | `688.59` | **FAIL** |
| `v1_movie_detail` | `/v1/titles/batman-MenTqxvHqCa` | `GET` | `200` | `200` | `773.70` | **VERIFIED PASS** |
| `v1_movie_recommendations` | `/v1/titles/batman-MenTqxvHqCa/recommendations?page=1` | `GET` | `200` | `200` | `1120.59` | **VERIFIED PASS** |
| `v1_movie_playback` | `/v1/titles/batman-MenTqxvHqCa/playback?season=0&episode=0` | `GET` | `200` | `200` | `761.55` | **VERIFIED PASS** |
| `v1_series_detail` | `/v1/titles/batman-4gNci8rCyt3` | `GET` | `200` | `200` | `6853.05` | **VERIFIED PASS** |
| `v1_series_episodes` | `/v1/titles/batman-4gNci8rCyt3/episodes?season=1` | `GET` | `200` | `200` | `669.84` | **VERIFIED PASS** |
| `v1_series_playback` | `/v1/titles/batman-4gNci8rCyt3/playback?season=1&episode=1` | `GET` | `200` | `200` | `1017.12` | **VERIFIED PASS** |
| `v1_movie_captions` | `/v1/titles/batman-MenTqxvHqCa/captions?stream_id=4279869683869909208&season=0&episode=0` | `GET` | `200` | `200` | `852.83` | **VERIFIED PASS** |
| `auth_test_none` | `/v1/titles/batman-MenTqxvHqCa/playback?season=0&episode=0` | `GET` | `200` | `200` | `791.69` | **VERIFIED PASS** |
| `auth_test_bearer` | `/v1/titles/batman-MenTqxvHqCa/playback?season=0&episode=0` | `GET` | `200` | `200` | `802.77` | **VERIFIED PASS** |
| `auth_test_custom` | `/v1/titles/batman-MenTqxvHqCa/playback?season=0&episode=0` | `GET` | `200` | `200` | `773.46` | **VERIFIED PASS** |
| `v1_search_missing_q_422` | `/v1/search` | `GET` | `422` | `422` | `949.41` | **VERIFIED PASS** |
| `v1_title_not_found_404` | `/v1/titles/invalid-nonexistent-title-xyz999` | `GET` | `404` | `404` | `1266.16` | **VERIFIED PASS** |
| `v1_episodes_missing_season_422` | `/v1/titles/batman-4gNci8rCyt3/episodes` | `GET` | `422` | `422` | `662.88` | **VERIFIED PASS** |
| `v1_captions_missing_stream_id_422` | `/v1/titles/batman-MenTqxvHqCa/captions` | `GET` | `422` | `422` | `730.41` | **VERIFIED PASS** |

---

## 2. DETAILED TEST CASES & VERIFIED PAYLOAD ANALYSIS

### Test Case: `health`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/health`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `592.01 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "status": "ok"
}
```

---

### Test Case: `v1_home`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/home`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `1064.89 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "banners": [
    {
      "id": "4148784774271983128",
      "detail_path": "campus-drivers-english-ShHvTvEttW4",
      "kind": "series",
      "title": "Campus Drivers [English]",
      "description": "",
      "release_date": "2026-11-06",
      "duration_seconds": null,
      "genres": [
        "Drama",
        "Romance"
      ],
      "poster_url": "https://pbcdn.aoneroom.com/image/2026/11/07/3923b890c24401f73704d707a44f9d29.jpg",
      "backdrop_url": null,
      "country": "France",
      "rating": "7.4",
      "has_resource": true
    },
    {
      "id": "5132870165285303040",
      "detail_path": "your-mother-your-mother-your-mother-q79QbGbBa76",
      "kind": "movie",
      "title": "Your Mother Your Mother Your Mother",
      "description": "",
      "release_date": "2026-10-09",
      "duration_seconds": 6720,
      "genres": [
        "Action",
... [7210 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_browse`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/browse`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `813.44 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "items": [
    {
      "id": "9028867555875774472",
      "detail_path": "wednesday-urChTxajYKa",
      "kind": "series",
      "title": "Wednesday",
      "description": "",
      "release_date": "2022-11-23",
      "duration_seconds": null,
      "genres": [
        "Comedy",
        "Crime",
        "Fantasy"
      ],
      "poster_url": "https://pbcdnw.aoneroom.com/image/2026/11/08/b6710f4320bd40ce33f2b5673985d894.jpg",
      "backdrop_url": "https://pbcdnw.aoneroom.com/image/2023/03/23/f30581fa14782f5ba27fd1bb073ec530.jpg",
      "country": "United States",
      "rating": "8.0",
      "has_resource": true
    },
    {
      "id": "5139196938264400928",
      "detail_path": "the-boys-c8bx84KzD76",
      "kind": "series",
      "title": "The Boys",
      "description": "",
      "release_date": "2019-07-26",
      "duration_seconds": null,
      "genres": [
... [316 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_search_batman`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/search?q=batman`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `1470.7 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "items": [
    {
      "id": "2918032533596032808",
      "detail_path": "batman-4gNci8rCyt3",
      "kind": "series",
      "title": "Batman S1-S4",
      "description": "",
      "release_date": "1992-09-05",
      "duration_seconds": null,
      "genres": [
        "Animation",
        "Action",
        "Adventure"
      ],
      "poster_url": "https://pbcdnw.aoneroom.com/image/2026/01/27/10d784da20bf19c4ccbdcd7987df81aa.jpg",
      "backdrop_url": "https://pbcdnw.aoneroom.com/image/2022/09/12/d92b9cec928d513f507344ba4c93fb34.jpg",
      "country": "United States",
      "rating": "9.0",
      "has_resource": true
    },
    {
      "id": "8913233011051760120",
      "detail_path": "batman-MenTqxvHqCa",
      "kind": "movie",
      "title": "Batman",
      "description": "",
      "release_date": "1989-06-23",
      "duration_seconds": 7560,
      "genres": [
... [280 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_search_suggestions_batman`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/search/suggestions?q=batman`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `769.24 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
[
  "batman: knightfall - part 2: knightfall",
  "Batman: Knightfall - Part 1: Knightfall",
  "Batman v Superman: Dawn of Justice",
  "Batman",
  "Batman Beyond",
  "Batman Begins",
  "Batman: The Brave and the Bold",
  "batman vs superman",
  "Batman: Caped Crusader",
  "Batman & Robin"
]
```

---

### Test Case: `v1_search_popular`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/search/popular`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `975.26 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
[
  "Sistas",
  "Youngins",
  "The Oval",
  "Kwa Baba",
  "The Wife",
  "Merlin",
  "The Originals"
]
```

---

### Test Case: `v1_rankings`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/rankings`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `665.08 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
[
  {
    "id": "62133389738001440",
    "name": "Trending"
  },
  {
    "id": "1513079728666723416",
    "name": "TOP100"
  },
  {
    "id": "1006647325055502320",
    "name": "Super Power"
  },
  {
    "id": "9188526763708762512",
    "name": "Reincarnation"
  },
  {
    "id": "4894661178359793648",
    "name": "Isekai"
  },
  {
    "id": "4142067459379678568",
    "name": "Gore"
  },
  {
    "id": "5258713876362938768",
    "name": "School"
  },
  {
... [12 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_ranking_detail`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/rankings/62133389738001440`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `1038.95 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "items": [
    {
      "id": "6969976563899551152",
      "detail_path": "bleach-thousand-year-blood-war-english-IKoyJ28zSi8",
      "kind": "series",
      "title": "Bleach: Thousand-Year Blood War [English]",
      "description": "The peace is suddenly broken when warning sirens blare through the Soul Society. Residents are disappearing without a trace and nobody knows who's behind it. Meanwhile, a darkness is approaching Ichigo and his friends in Karakura Town.",
      "release_date": "2022-10-10",
      "duration_seconds": null,
      "genres": [
        "Anime",
        "Action",
        "Adventure"
      ],
      "poster_url": "https://pbcdnw.aoneroom.com/image/2026/08/18/dfb652a2ab5dbad1ab05172bb471f0e2.jpg",
      "backdrop_url": null,
      "country": "Japan",
      "rating": "9.0",
      "has_resource": true
    },
    {
      "id": "1424275672742135064",
      "detail_path": "steel-ball-run-jojos-bizarre-adventure-english-wIUYrVfcdH1",
      "kind": "series",
      "title": "Steel Ball Run: JoJo's Bizarre Adventure [English]",
      "description": "A paraplegic ex-jockey joins forces with an outlaw to compete in the first cross-continental horse race across North America in the 1800s.",
      "release_date": "2026-03-19",
      "duration_seconds": null,
      "genres": [
... [91 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_catalog_series_filters`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/catalog/series/filters`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `693.06 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "groups": [
    {
      "key": "genre",
      "label": "Genre",
      "values": [
        {
          "id": "All",
          "label": "All"
        },
        {
          "id": "Action",
          "label": "Action"
        },
        {
          "id": "Adventure",
          "label": "Adventure"
        },
        {
          "id": "Animation",
          "label": "Animation"
        },
        {
          "id": "Biography",
          "label": "Biography"
        },
        {
          "id": "Comedy",
          "label": "Comedy"
        },
... [396 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_catalog_movies_filters`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/catalog/movies/filters`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `404`
- **Response Latency:** `735.93 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "detail": "Filter choices are unavailable for this catalog"
}
```

---

### Test Case: `v1_catalog_series`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/catalog/series?page=1`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `1279.23 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "items": [
    {
      "id": "801622183708610352",
      "detail_path": "the-vampire-diaries-0KncV4ardX",
      "kind": "series",
      "title": "The Vampire Diaries",
      "description": "",
      "release_date": "2009-09-10",
      "duration_seconds": null,
      "genres": [
        "Drama",
        "Fantasy",
        "Horror"
      ],
      "poster_url": "https://pbcdnw.aoneroom.com/image/2026/01/27/cc2e447b636be4a800cedbaccc21b7dc.jpg",
      "backdrop_url": "https://pbcdnw.aoneroom.com/image/2023/03/23/2b3374fd77a4f9d28b860beef86116ba.jpg",
      "country": "United States",
      "rating": "7.7",
      "has_resource": true
    },
    {
      "id": "2730113747162387224",
      "detail_path": "the-originals-ShDV3wTWFf3",
      "kind": "series",
      "title": "The Originals",
      "description": "",
      "release_date": "2013-10-03",
      "duration_seconds": null,
      "genres": [
... [502 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_catalog_movies`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/catalog/movies?page=1`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `1206.88 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "items": [
    {
      "id": "8535219140137830216",
      "detail_path": "hope-qzPZdmrovaa",
      "kind": "movie",
      "title": "Hope",
      "description": "",
      "release_date": "2026-09-09",
      "duration_seconds": 9420,
      "genres": [
        "Action",
        "Horror",
        "Sci-Fi"
      ],
      "poster_url": "https://pbcdnw.aoneroom.com/image/2026/10/28/bed3b73afeece623905068e38b3f137f.jpg",
      "backdrop_url": null,
      "country": "Korea",
      "rating": "6.6",
      "has_resource": true
    },
    {
      "id": "4606371260002628112",
      "detail_path": "the-florist-efahwcdehu5",
      "kind": "movie",
      "title": "The Florist",
      "description": "",
      "release_date": "2026-10-09",
      "duration_seconds": 5820,
      "genres": [
... [467 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_collections_dynamic`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/collections/popular-series`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `404`
- **Response Latency:** `688.59 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "detail": "Collection not found"
}
```

---

### Test Case: `v1_movie_detail`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-MenTqxvHqCa`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `773.7 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "title": {
    "id": "8913233011051760120",
    "detail_path": "batman-MenTqxvHqCa",
    "kind": "movie",
    "title": "Batman",
    "description": "",
    "release_date": "1989-06-23",
    "duration_seconds": 7560,
    "genres": [
      "Action",
      "Adventure"
    ],
    "poster_url": "https://pbcdnw.aoneroom.com/image/2026/01/27/a34bdfc07ffe758a206aa0043b26a354.jpg",
    "backdrop_url": "https://pbcdnw.aoneroom.com/image/2023/03/23/baf682ceab4df1e34aea7072cecc551f.jpg",
    "country": "United States",
    "rating": "7.5",
    "has_resource": true
  },
  "cast": [],
  "seasons": [],
  "dubs": [],
  "trailer_url": null
}
```

---

### Test Case: `v1_movie_recommendations`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-MenTqxvHqCa/recommendations?page=1`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `1120.59 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "items": [
    {
      "id": "633754108498956072",
      "detail_path": "bloodshot-OxEfDteBOK",
      "kind": "movie",
      "title": "Bloodshot",
      "description": "",
      "release_date": "2020-03-13",
      "duration_seconds": 6540,
      "genres": [
        "Action",
        "Adventure",
        "Sci-Fi"
      ],
      "poster_url": "https://pbcdnw.aoneroom.com/image/2026/01/27/bdf7e5e44085cabd41f164ec14761172.jpg",
      "backdrop_url": "https://pbcdnw.aoneroom.com/image/2023/03/23/2237b64c7a50e9356aef950bd55d42ba.jpg",
      "country": "United States",
      "rating": "5.7",
      "has_resource": true
    },
    {
      "id": "1010018285435371752",
      "detail_path": "the-city-of-gold-2pO4xjsTBc1",
      "kind": "movie",
      "title": "The City of Gold",
      "description": "",
      "release_date": "2018-12-04",
      "duration_seconds": 5940,
      "genres": [
... [201 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_movie_playback`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-MenTqxvHqCa/playback?season=0&episode=0`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `761.55 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "title_id": "8913233011051760120",
  "season": 0,
  "episode": 0,
  "streams": [
    {
      "id": "4279869683869909208",
      "format": "MP4",
      "resolutions": "360",
      "codec": "h264",
      "size_bytes": 411789300,
      "duration_seconds": 7579,
      "url": "https://bcdnxw.hakunaymatata.com/bt/398a81b69c75c6d400a3fc7d641304b5.mp4?sign=66cf49f69782ba74f44bc03f73120dc4&t=1794244259",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa",
        "Origin": "https://themoviebox.xyz"
      },
      "vip_locked": false
    },
    {
      "id": "1414051612078784640",
      "format": "MP4",
      "resolutions": "480",
      "codec": "h264",
      "size_bytes": 599930067,
      "duration_seconds": 7579,
      "url": "https://bcdnxw.hakunaymatata.com/resource/fe500221a165ab1833fe9e39d425f0fd.mp4?sign=a1bf60a91b52d049acd43fe4b8dcd4de&t=1794244259",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa",
        "Origin": "https://themoviebox.xyz"
... [36 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_series_detail`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-4gNci8rCyt3`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `6853.05 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "title": {
    "id": "2918032533596032808",
    "detail_path": "batman-4gNci8rCyt3",
    "kind": "series",
    "title": "Batman S1-S4",
    "description": "",
    "release_date": "1992-09-05",
    "duration_seconds": null,
    "genres": [
      "Animation",
      "Action",
      "Adventure"
    ],
    "poster_url": "https://pbcdnw.aoneroom.com/image/2026/01/27/10d784da20bf19c4ccbdcd7987df81aa.jpg",
    "backdrop_url": "https://pbcdnw.aoneroom.com/image/2022/09/12/d92b9cec928d513f507344ba4c93fb34.jpg",
    "country": "United States",
    "rating": "9.0",
    "has_resource": true
  },
  "cast": [],
  "seasons": [
    {
      "number": 1,
      "episode_count": 40,
      "resolutions": [
        360,
        480,
        1080
      ]
... [32 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_series_episodes`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-4gNci8rCyt3/episodes?season=1`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `669.84 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
[
  {
    "season": 1,
    "number": 1
  },
  {
    "season": 1,
    "number": 2
  },
  {
    "season": 1,
    "number": 3
  },
  {
    "season": 1,
    "number": 4
  },
  {
    "season": 1,
    "number": 5
  },
  {
    "season": 1,
    "number": 6
  },
  {
    "season": 1,
    "number": 7
  },
  {
... [132 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_series_playback`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-4gNci8rCyt3/playback?season=1&episode=1`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `1017.12 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "title_id": "2918032533596032808",
  "season": 1,
  "episode": 1,
  "streams": [
    {
      "id": "6512988221621028456",
      "format": "MP4",
      "resolutions": "360",
      "codec": "h264",
      "size_bytes": 77990926,
      "duration_seconds": 1347,
      "url": "https://bcdnxw.hakunaymatata.com/bt/eb01fbfd90d7e7cbf6eefa7be0c759b0.mp4?sign=e9bbb8d599fffe2ea995a58ffbcc44f2&t=1794244260",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-4gNci8rCyt3",
        "Origin": "https://themoviebox.xyz"
      },
      "vip_locked": false
    },
    {
      "id": "4009801240501782152",
      "format": "MP4",
      "resolutions": "480",
      "codec": "h264",
      "size_bytes": 110821474,
      "duration_seconds": 1347,
      "url": "https://bcdnxw.hakunaymatata.com/bt/bcf2bd78f0a56c5acf77494f7dac71b7.mp4?sign=83e555f6b9068c1473675b835845b553&t=1794244260",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-4gNci8rCyt3",
        "Origin": "https://themoviebox.xyz"
... [36 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_movie_captions`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-MenTqxvHqCa/captions?stream_id=4279869683869909208&season=0&episode=0`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `852.83 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
[
  {
    "language": "ar",
    "label": "\u0627\u064e\u0644\u0652\u0639\u064e\u0631\u064e\u0628\u0650\u064a\u064e\u0651\u0629\u064f",
    "format": "srt",
    "url": "https://cacdn.hakunaymatata.com/subtitle/22c0a6793583f1554d4e2e04a051a804.srt?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jYWNkbi5oYWt1bmF5bWF0YXRhLmNvbS9zdWJ0aXRsZS8qIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzk0ODQ1OTI5fX19XX0_&Signature=54M0EjZz2oXlT-8tZRMQIfiyqTJO2v9aT7RZPPTB0rKTYUOrFvWuhWWU9Xmsl~CLSttmD5TxPoh0FwX7FfTOhpTbis3pHNG3brmSjBVwGAK2rwlC8ee9odPmkTJ9u25sB0Uj~W7egiyaLfMw3GYSDPDxEm5G2Jtz-w51Oogbloi4onlxGDvIIaRqcjPw0MFH0eln9UbzSY~6i8M3gboxzY0AifVoYYqUaqQzUdRQKmRBrpoDcSPhk3l80gd-WHdGnYqPDP49jkXXqjAry5UvtT0~cvU5fg6pNxM4rs0YDpSOqfXqnPGP1FVDLdmdTjyl5GGvRmeOELlFTnNypZ8-xQ__&Key-Pair-Id=KMHN1LQ1HEUPL",
    "size_bytes": 64755,
    "delay_ms": 0,
    "request_headers": {
      "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa"
    }
  },
  {
    "language": "bn",
    "label": "\u09ac\u09be\u0982\u09b2\u09be",
    "format": "srt",
    "url": "https://cacdn.hakunaymatata.com/msubt/c40a3f9e6731090ea4c51d2acace2b9f.srt?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jYWNkbi5oYWt1bmF5bWF0YXRhLmNvbS9tc3VidC8qIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzk0ODQ1OTI5fX19XX0_&Signature=LgWkFNPGV0R01f4iyia4RlYdaFLHfuCxmpahIyDEqEZBGAQGOlIgSs2Uv114CjZmtVZ-jI2gK6MI2cZbkNpb0RoGbbtg6lfZdw8piR5esCBfAFUT~rweGujq6Wk-TCbM4qG8KpRoJA6mpuKH-DK-yX8q6~NEGUpayQk~Xx5pVE2gkBz4vxdptutHnMz1BN4G7JxToAx7m4PmG8SaD0d6vEiRkVE-91esx9kKRxia1atvrS1kKBav5MGGX7mlAQdGtWwlk5qvEBOP0nsc0jgwLmQkYNCiYU1B3zPeghJu5skgEKWoos4jZfADZo4MDiZ2-25ObU5uSRulS5m89XXzmw__&Key-Pair-Id=KMHN1LQ1HEUPL",
    "size_bytes": 125385,
    "delay_ms": 0,
    "request_headers": {
      "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa"
    }
  },
  {
    "language": "en",
    "label": "English",
    "format": "srt",
    "url": "https://cacdn.hakunaymatata.com/subtitle/9e6b1c560498709c6d0451bb1cbc703d.srt?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jYWNkbi5oYWt1bmF5bWF0YXRhLmNvbS9zdWJ0aXRsZS8qIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzk0ODQ1OTI5fX19XX0_&Signature=54M0EjZz2oXlT-8tZRMQIfiyqTJO2v9aT7RZPPTB0rKTYUOrFvWuhWWU9Xmsl~CLSttmD5TxPoh0FwX7FfTOhpTbis3pHNG3brmSjBVwGAK2rwlC8ee9odPmkTJ9u25sB0Uj~W7egiyaLfMw3GYSDPDxEm5G2Jtz-w51Oogbloi4onlxGDvIIaRqcjPw0MFH0eln9UbzSY~6i8M3gboxzY0AifVoYYqUaqQzUdRQKmRBrpoDcSPhk3l80gd-WHdGnYqPDP49jkXXqjAry5UvtT0~cvU5fg6pNxM4rs0YDpSOqfXqnPGP1FVDLdmdTjyl5GGvRmeOELlFTnNypZ8-xQ__&Key-Pair-Id=KMHN1LQ1HEUPL",
    "size_bytes": 74729,
    "delay_ms": 0,
... [115 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `auth_test_none`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-MenTqxvHqCa/playback?season=0&episode=0`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `791.69 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "title_id": "8913233011051760120",
  "season": 0,
  "episode": 0,
  "streams": [
    {
      "id": "4279869683869909208",
      "format": "MP4",
      "resolutions": "360",
      "codec": "h264",
      "size_bytes": 411789300,
      "duration_seconds": 7579,
      "url": "https://bcdnxw.hakunaymatata.com/bt/398a81b69c75c6d400a3fc7d641304b5.mp4?sign=8756fe31a4f3979c530560db21892c02&t=1794244270",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa",
        "Origin": "https://themoviebox.xyz"
      },
      "vip_locked": false
    },
    {
      "id": "1414051612078784640",
      "format": "MP4",
      "resolutions": "480",
      "codec": "h264",
      "size_bytes": 599930067,
      "duration_seconds": 7579,
      "url": "https://bcdnxw.hakunaymatata.com/resource/fe500221a165ab1833fe9e39d425f0fd.mp4?sign=40d851d8617d03e202fd902b0771924b&t=1794244270",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa",
        "Origin": "https://themoviebox.xyz"
... [36 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `auth_test_bearer`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-MenTqxvHqCa/playback?season=0&episode=0`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `802.77 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "title_id": "8913233011051760120",
  "season": 0,
  "episode": 0,
  "streams": [
    {
      "id": "4279869683869909208",
      "format": "MP4",
      "resolutions": "360",
      "codec": "h264",
      "size_bytes": 411789300,
      "duration_seconds": 7579,
      "url": "https://bcdnxw.hakunaymatata.com/bt/398a81b69c75c6d400a3fc7d641304b5.mp4?sign=8756fe31a4f3979c530560db21892c02&t=1794244270",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa",
        "Origin": "https://themoviebox.xyz"
      },
      "vip_locked": false
    },
    {
      "id": "1414051612078784640",
      "format": "MP4",
      "resolutions": "480",
      "codec": "h264",
      "size_bytes": 599930067,
      "duration_seconds": 7579,
      "url": "https://bcdnxw.hakunaymatata.com/resource/fe500221a165ab1833fe9e39d425f0fd.mp4?sign=40d851d8617d03e202fd902b0771924b&t=1794244270",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa",
        "Origin": "https://themoviebox.xyz"
... [36 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `auth_test_custom`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-MenTqxvHqCa/playback?season=0&episode=0`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `200`
- **Response Latency:** `773.46 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "title_id": "8913233011051760120",
  "season": 0,
  "episode": 0,
  "streams": [
    {
      "id": "4279869683869909208",
      "format": "MP4",
      "resolutions": "360",
      "codec": "h264",
      "size_bytes": 411789300,
      "duration_seconds": 7579,
      "url": "https://bcdnxw.hakunaymatata.com/bt/398a81b69c75c6d400a3fc7d641304b5.mp4?sign=8756fe31a4f3979c530560db21892c02&t=1794244270",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa",
        "Origin": "https://themoviebox.xyz"
      },
      "vip_locked": false
    },
    {
      "id": "1414051612078784640",
      "format": "MP4",
      "resolutions": "480",
      "codec": "h264",
      "size_bytes": 599930067,
      "duration_seconds": 7579,
      "url": "https://bcdnxw.hakunaymatata.com/resource/fe500221a165ab1833fe9e39d425f0fd.mp4?sign=40d851d8617d03e202fd902b0771924b&t=1794244270",
      "request_headers": {
        "Referer": "https://themoviebox.xyz/movies/batman-MenTqxvHqCa",
        "Origin": "https://themoviebox.xyz"
... [36 lines truncated for document brevity; full response in NEXAPREMIUM_RESPONSE_SAMPLES.json]
```

---

### Test Case: `v1_search_missing_q_422`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/search`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `422`
- **Response Latency:** `949.41 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "detail": [
    {
      "type": "missing",
      "loc": [
        "query",
        "q"
      ],
      "msg": "Field required",
      "input": null
    }
  ]
}
```

---

### Test Case: `v1_title_not_found_404`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/invalid-nonexistent-title-xyz999`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `404`
- **Response Latency:** `1266.16 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "detail": "Title not found"
}
```

---

### Test Case: `v1_episodes_missing_season_422`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-4gNci8rCyt3/episodes`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `422`
- **Response Latency:** `662.88 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "detail": [
    {
      "type": "missing",
      "loc": [
        "query",
        "season"
      ],
      "msg": "Field required",
      "input": null
    }
  ]
}
```

---

### Test Case: `v1_captions_missing_stream_id_422`
- **Full Target URL:** `https://movie-box-sepia-six.vercel.app/v1/titles/batman-MenTqxvHqCa/captions`
- **HTTP Method:** `GET`
- **HTTP Status Code:** `422`
- **Response Latency:** `730.41 ms`
- **Server Headers:** `Server: N/A`, `Content-Type: N/A`

#### Payload Snippet / Summary:
```json
{
  "detail": [
    {
      "type": "missing",
      "loc": [
        "query",
        "stream_id"
      ],
      "msg": "Field required",
      "input": null
    }
  ]
}
```

---

## 3. VERIFIED FACTS VS UNTESTED ASSUMPTIONS

### 3.1 Verified Empirical Facts
1. **Backend Technology:** Confirmed Python FastAPI running on Starlette / Vercel Serverless Functions via `/openapi.json` and standard Pydantic error signatures.
2. **Playable Video Streams:** Progressive MP4 streams (`h264` codec, resolutions 360p, 480p, 720p, 1080p) are 100% accessible (`HTTP 200 OK`, Content-Type `video/mp4`) when supplying required `Referer`, `Origin`, and a non-ExoPlayer `User-Agent` (e.g. `okhttp/4.11.0`).
3. **DASH Stream Failure:** DASH manifests (`.mpd`) returned under `streams` fail with `HTTP 403 Forbidden` on `sbcdnw.hakunaymatata.com`. MP4 streams MUST be used for Android playback.
4. **Subtitles:** SRT subtitles (`.srt`) are accessible with `HTTP 200 OK` (`application/x-subrip`) using AWS CloudFront signed query parameters and `Referer` header.
5. **Catalog Filters:** Valid catalog name `series` returns 5 filter groups (Genre, Country, Year, Language, Sort), whereas `movies` filters endpoint returns `HTTP 404 Not Found`.
6. **Authorization Header:** Passing arbitrary `Authorization` tokens does not change playback limits or max resolutions; authorization is optional in practice.
7. **Collections Route:** `/v1/collections/{name}` returns `HTTP 404 Not Found` for tested keys. It is a reserved or non-functional route.

### 3.2 Untested / Out-of-Scope Assumptions
1. **User Accounts & History:** The backend currently has no user registration, bookmarking, or watch history endpoints. Client-side Room DB storage is required.
2. **CDN Stream Expiration:** Video signed URLs contain epoch timestamps (`t=...`). Exact expiration TTL is unknown, but stream URL re-fetches are recommended upon playback errors.
