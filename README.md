# Tattui JavaFX Baseline

This repository is now the **clean baseline** for our JavaFX project.  
It has been standardized so everyone can clone, build, and run with minimal setup.

---

## Current State
- Build tool: **Gradle (via wrapper)** -- no Maven
- Target JDK: **21** (enforced by Gradle toolchain, no manual install required)
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

---
## Git Workflow Guidelines

To keep the repo clean and consistent:

- **Do not upload files directly** (no drag‑and‑drop into GitHub).
- Always use Git commands:
    - `git pull origin main` before starting work
    - `git add <files>` to stage changes
    - `git commit -m "clear, descriptive message"`
    - `git push origin main` to share your work
- **Only commit**:
    - Source code (`src/main/java`)
    - Resources (`src/main/resources`)
    - Build scripts (`build.gradle`, `settings.gradle`, etc.)
    - Documentation (`README.md`, etc.)
- **Never commit**:
    - Generated files (`bin/`, `build/`, `.gradle/`)
    - IDE configs (`.idea/`, `.vscode/`)
    - OS junk (`.DS_Store`, `Thumbs.db`)
- Write meaningful commit messages (describe *what* and *why*, not just “changes”).
- Example: `git commit -m "Add login controller and update FXML for login view"`