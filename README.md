

## Current State
- Build tool: **Gradle (via wrapper)** -- no Maven
- Target JDK: **21** (enforced by Gradle toolchain, auto-downloaded if not installed)
- Pure JavaFX (no Gluon or external dependencies).
- Cleaned out generated/IDE files (`bin/`, `.gradle/`) from version control.
- `.gitignore` added to prevent clutter (IDE configs, build outputs, caches).
- Verified baseline runs with the included Gradle wrapper.

---

## Quickstart

### If you have not cloned yet
```bash
git clone <repo-url>
cd tattui
```

### If you already cloned
```bash
git pull origin main
```

### Run the project (from your IDE terminal)
Use the Gradle wrapper — no need to install Gradle or JavaFX manually.

**Mac/Linux:**
```bash
./gradlew run
```
**Windows:**
```bash
gradlew.bat run
```
<img width="1914" height="1032" alt="image" src="https://github.com/user-attachments/assets/409fda6d-27ef-472a-8bee-25cc74fb41dc" />
<img width="1917" height="1030" alt="Screenshot 2026-01-12 121448" src="https://github.com/user-attachments/assets/ea151ab1-3847-46c6-806e-a613a8d42fad" 
