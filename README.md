# EveryDay — Personal Productivity & Growth Dashboard

EveryDay is a production-quality, local-first, offline-capable desktop application built for Windows 11+.

> **Core Philosophy**: Plan → Do → Check → Analyze → Improve

---

## 🚀 Quick Start

### 1. Launch Standalone Windows EXE
A standalone Windows distribution has been built using `jpackage` (no external JRE required):
```
dist\EveryDay\EveryDay.exe
```
Or double-click:
```
run.bat
```

### 2. Launch via Maven
```bash
mvn javafx:run
```

### 3. Launch via Bundled JAR
```bash
java -jar target\everyday-dashboard-1.0.0-all.jar
```

---

## 🏛 Clean Architecture

The application strictly enforces a separation of concerns:

```
UI (JavaFX / Modern Dark CSS)
  → Controllers / Views (Dashboard, Daily Tasks, Projects, Calendar, Analytics, Streaks, Settings, Profile)
  → Services (DailyEngineService, TaskService, ProjectService, StreakService, CalendarService, AnalyticsService, BackupService, SettingsService, ProfileService)
  → Repositories / DAO (Prepared SQL Statements)
  → SQLite Database (Foreign keys enabled, WAL journal mode)
```

- **JavaFX controllers contain zero raw database logic.**
- **All business logic is isolated in dedicated services.**
- **Offline & Local-first**: No mandatory internet, no telemetry, no mandatory login.

---

## ⚙️ Critical Business Rules Enforced

1. **Equal Weight**: Every active daily task has equal weight. No points or arbitrary weights.
2. **Formula**: Daily completion = `(completed active baseline tasks / total active baseline tasks) * 100`.
3. **Late-Added Tasks**:
   - A task added after the logical day has started may be worked on today.
   - **MUST NOT** affect today's completion percentage or denominator.
   - Begins normal tracking starting the next logical day.
   - **MUST NOT** become a missed task merely because it was created late today.
4. **Missed Tasks**:
   - Unfinished tasks **NEVER** automatically carry over into the next day.
   - Preserved in historical records and displayed in a dedicated Missed Tasks section.
   - Do not reduce today's completion percentage or denominator.
5. **Project Independence**: Project progress (0–100%) is completely independent from daily completion. They are never averaged or combined.
6. **Task Progress Independence**: Multi-day task progress (e.g. Day 2/7, 45%) is completely independent of daily completion.
7. **Streak System & Locked Levels**:
   - **Beginner**: 0–99 days, minimum completion = 50%
   - **Elite**: 100–199 days, minimum completion = 60%
   - **Epic**: 200–299 days, minimum completion = 70%
   - **Master**: 300–399 days, minimum completion = 80%
   - **Legend**: 400+ days, minimum completion = 90%
   - Exact threshold is accepted (`>= threshold` continues/increases streak; `< threshold` breaks streak).
8. **Day Status vs Streak**:
   - Incomplete day (`>= threshold`, `< 100%`) continues streak.
   - Completed day (`100%`) increases streak.
9. **Skip Day**:
   - Requires a **mandatory** reason.
   - Permanently stored with the reason.
   - Neither breaks nor increases streak.
10. **Configurable Day Start**:
    - Default: 12:00 AM (00:00).
    - User can set another boundary (e.g., 04:00 AM).
    - Changing setting **does not rewrite historical data**.

---

## 🧪 Comprehensive Test Suite

21 unit tests covering all critical rules:
- `TaskManagementTest`: Creation, editing, deletion, completion, undo, ordering, schedules, ON/OFF.
- `DailyCalculationTest`: 0/8, 4/8, 5/8, 7/8, 8/8, late-added tasks, missed tasks archival.
- `StreakEngineTest`: Exact threshold boundaries (49%, 50%, 59%, 60%, 69%, 70%, 79%, 80%, 89%, 90%), skip logic.
- `ProjectAndHistoryTest`: Project independence, snapshot history preservation, day start setting changes.
- `BackupRestoreTest`: JSON export, import, validation, corrupt backup rejection.
- `ApplicationIntegrationTest`: Full end-to-end service and seed initialization.

Run tests:
```bash
mvn test
```
Result: **21/21 passed**.
