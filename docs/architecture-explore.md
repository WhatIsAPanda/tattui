# Explore: Live-by-default with Mock Fallback

**Author:** Adnan A.  
**Scope:** Explore gallery data path and profile popup

## What runs by default?
- App tries **LIVE** first (DB reachable) and falls back to **MOCK** if not.
- Overrides:
  - `EXPLORE_MOCK=1` → force Mock
  - `EXPLORE_LIVE=1` → force Live
- Debug logs: `-DTATTUI_DEBUG=true`

## Selection logic (in `ExploreBoundary`)
1. If `EXPLORE_MOCK` set → `MockExploreDataProvider`
2. Else if `EXPLORE_LIVE` set → `MergedExploreDataProvider`
3. Else probe DB via `DbConnectionProvider.open()`:
   - success → Live
   - failure → Mock

## Data flow (Live)
`MergedExploreDataProvider` pulls Artists + Completed Tattoos from JDBC-backed queries (`JdbcPostRepository`, `DatabaseConnector`). Designs are derived from posts for now.

## Artist profile popup
- Tries FXML (`/app/view/ArtistProfile.fxml`) first; if missing, uses a programmatic fallback.
- Avatar uses profile picture URL or falls back to `/icons/artist_raven.jpg`.

## Tests
- Added shims to `Profile` (`getBiography()`, `getPosts()`) for legacy probe tests.
- `DbSmokeTest` auto-skips if DB env vars are missing to avoid false failures.

## Notes
- No writes in Explore path.
- Teammates still run `./gradlew run` normally; they get Live if DB is reachable, otherwise Mock.
