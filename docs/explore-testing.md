# Docs B: Explore Tests and Rubric Alignment

**Author:** Adnan A.
**Scope:** Testing for Explore data flow and DB integration.

---

## Testing Summary
Explore testing covers three categories: **unit**, **loop**, and **acceptance**.

### 1. Unit Tests
| File | Purpose | Methods |
|------|----------|----------|
| `ExploreProviderSmokeTest.java` | Verifies data provider behavior. | - `mockProvider_filtersDesignsOnly_whenKindIsDESIGNS()`  <br> - `mockProvider_supportsCaseInsensitiveSearch()` <br> - `liveProvider_returnsSomething_whenLiveEnabled()` |
| `DedupLoopTest.java` | Validates dedup & tag parsing logic. | - `linkedHashSet_dedupsAndPreservesOrder()` <br> - `parseArtistTag_findsFirstArtistTag_caseInsensitive()` |

**Requirement satisfied:** Two test classes, four methods total.

---

### 2. Loop Tests
Loop tests are inside `DedupLoopTest.java`:
- **Loop 1:** Deduplication of styles using a `LinkedHashSet`.
- **Loop 2:** Stream filter for tags starting with `artist:`.

**Requirement satisfied:** 2 loops tested.

---

### 3. Acceptance Test
| File | Purpose |
|------|----------|
| `ExploreLiveAcceptanceProbe.java` | Runs end-to-end live provider fetch (only when EXPLORE_LIVE + DB reachable). |

**Requirement satisfied:** One acceptance test present.

---

## Supporting Tests
| File | Purpose |
|------|----------|
| `DbSmokeTest.java` | Prints DB info + tables; skips when no creds. |

---

## How to Run
### Default / Mock (safe on any machine)
```bash
./gradlew clean test
```

### Live-only probes (requires DB credentials)
```bash
# Windows PowerShell
./gradlew --stop
$env:EXPLORE_LIVE="1"
./gradlew -DHEADLESS_TESTS=true test --tests "*Explore*Live*"
Remove-Item Env:EXPLORE_LIVE -ErrorAction SilentlyContinue

# macOS/Linux
./gradlew --stop
EXPLORE_LIVE=1 ./gradlew -DHEADLESS_TESTS=true test --tests "*Explore*Live*"
```

**Live tests auto-skip** if the DB is not reachable/configured.

---

## Rubric Mapping
| Rubric Requirement | Where Satisfied | Notes |
|---------------------|-----------------|--------|
| **4.1 Unit Testing** | `ExploreProviderSmokeTest`, `DedupLoopTest` | 2 test classes, 4+ methods. |
| **4.2 Loop Testing** | `DedupLoopTest` | 2 loop tests. |
| **4.3 Acceptance Testing** | `ExploreLiveAcceptanceProbe` | Validated through live DB fetch (env-gated). |

---

## Takeaway
- Tests run green in both mock and live modes.
- Live-only tests retry DB connection, skipping if unreachable.
- Teammates and CI don’t need DB setup to pass tests.

---

