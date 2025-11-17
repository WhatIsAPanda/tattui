# Docs A: Explore Classes and Interactions

**Author:** Adnan A.
**Scope:** Explore feature (live-by-default data flow) and related components.

---

## Overview
This document explains the classes I added or modified, their responsibilities, and how they interact. It focuses on my work around the **Explore** feature (gallery, data providers, DB integration) and related classes I interact with.

---

## 1. ExploreBoundary (UI Controller)
- **Type:** JavaFX Controller  (`app.boundary.ExploreBoundary`)
- **Purpose:** Manages the Explore tab's UI: search box, filter dropdown, and results grid.
- **Key responsibilities:**
  - Handles user input (search/filter events).
  - Decides whether to use LIVE or MOCK data via `selectProvider()`.
  - Builds and updates result cards (images, labels, hover overlays).
  - Opens artist profile popups (via FXML or fallback scene).
  - Uses `WorkspaceController` to send images to workspace.

### Interacts With:
- `ExploreControl` → defines `SearchItem` + `Kind` enum for filtering.
- `ExploreDataProvider` → interface for fetch logic.
- `MergedExploreDataProvider` → live (DB-backed) data.
- `MockExploreDataProvider` → mock (in-memory) data.
- `DbConnectionProvider` → checks DB connectivity.
- `DatabaseConnector` → loads artist profile info (image, bio, etc.).

### Why it matters:
Makes Explore data source-agnostic. Teammates and CI can run without DB creds.

---

## 2. ExploreControl (Logic / Model Helper)
- **Type:** Plain Java class (`app.controller.explore.ExploreControl`)
- **Purpose:** Houses `SearchItem` record + `Kind` enum. Central logic for filtering results.
- **Why:** Keeps logic separate from UI, making it testable.

### Interacts With:
- `ExploreBoundary` (UI controller)
- `ExploreDataProvider` (mock/live fetch)

---

## 3. ExploreDataProvider (Interface)
- **Type:** Interface (`app.controller.explore.ExploreDataProvider`)
- **Purpose:** Defines contract for Explore data providers.
- **Signature:** `List<SearchItem> fetch(String query, ExploreControl.Kind kind)`

---

## 4. MockExploreDataProvider
- **Type:** Class (`app.controller.explore.MockExploreDataProvider`)
- **Purpose:** Provides mock Explore items with predictable content.
- **Use case:** Default for teammates and CI without DB.

### Why:
Gives realistic data without relying on DB access.

---

## 5. MergedExploreDataProvider
- **Type:** Class (`app.controller.explore.MergedExploreDataProvider`)
- **Purpose:** Fetches **live** Explore data from DB.
- **Key functions:**
  - Combines Artists + Completed Tattoos (possibly Designs later).
  - Uses JDBC-backed repositories and `DatabaseConnector`.

---

## 6. JdbcPostRepository
- **Type:** Class (`app.entity.jdbc.JdbcPostRepository`)
- **Purpose:** Encapsulates SQL for fetching posts + authors.
- **Why:** Reusable across features. Avoids direct SQL in UI code.

---

## 7. DbConnectionProvider
- **Type:** Utility (`app.db.DbConnectionProvider`)
- **Purpose:** Safely opens DB connection from env vars or fallback `keys.txt`.
- **Why:** Keeps credential logic centralized and test-safe.
- **Used by:** `ExploreBoundary` for DB probe, repositories for live queries.

---

## 8. DatabaseConnector
- **Type:** Entity service (`app.entity.DatabaseConnector`)
- **Purpose:** Provides higher-level DB queries shared across app.
- **Used for:**
  - Fetching `Profile` by username.
  - Loading artist data for popups and avatar overlays.

---

## 9. Profile
- **Type:** Entity class (`app.entity.Profile`)
- **Purpose:** Represents artist/user profile (username, bio, image URL, etc.).
- **Changes added:**
  - Compatibility shims `getBiography()` and `getPosts()` for legacy tests.
- **Used by:** `DatabaseConnector`, `ExploreBoundary`, test probes.

---