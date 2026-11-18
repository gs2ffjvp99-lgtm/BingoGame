# Bingo Game with SQLite (Vibe Coding Practice 04)

An Android Bingo game where you race a computer opponent to complete a line on a 5 × 5 card. Every time you get Bingo, the app saves your username, the round number, the five winning numbers and the system time into an SQLite database, and you can browse those records inside the app.

Written in Java with only built-in Android classes (`Activity`, `SQLiteOpenHelper`, `GridLayout`), so no extra libraries are needed to run it.

## How the game works

1. Enter a username on the start screen and tap **Start game**.
2. You and the computer each get a card with the numbers 1–25 in random order.
3. Each round, you tap a number to call it, then the computer calls one. Every called number is marked on both cards (blue = your calls, magenta = computer calls).
4. The first side to complete a row, column or diagonal wins. Your winning line turns yellow.
5. When you win, the record is saved to SQLite automatically. Open **Records** to see all saved wins.

The computer is not random: it scores each free cell by how close the lines through it are to completion and calls the best one, so you need a plan to beat it.

## Assignment checklist

| Requirement | Where it is |
|---|---|
| 1. Clean project, copy to a new project | Steps below |
| 2. Game logic | `game/BingoBoard.java`, `game/BingoGame.java` |
| 3. SQLiteDatabase with username, round number, five Bingo numbers, system time | `data/BingoDbHelper.java`, saved from `GameActivity.saveBingoRecord()` |
| 4. Detailed comments | Every Java, XML and Gradle file |
| 5. Clean code on GitHub | `.gitignore` + steps below |

## Database

Database file: `bingo.db`, table `bingo_records`. One row is added each time the player gets Bingo.

| Column | Type | Meaning |
|---|---|---|
| `_id` | INTEGER PRIMARY KEY AUTOINCREMENT | Row id |
| `username` | TEXT | Name entered on the start screen |
| `round_number` | INTEGER | Round in which the Bingo happened (1 round = your call + the computer's call) |
| `number1` … `number5` | INTEGER | The five numbers of the winning line, in board order |
| `system_time` | TEXT | Device time when saved, `yyyy-MM-dd HH:mm:ss` |

To look at the table while the app runs: **View → Tool Windows → App Inspection → Database Inspector**, pick the running app, then open `bingo.db → bingo_records`.

## Project structure

```
app/src/main/java/com/example/bingogame/
├── MainActivity.java        Start screen (username input)
├── GameActivity.java        Game screen (draws the card, saves wins)
├── RecordActivity.java      Records screen (reads / deletes rows)
├── game/
│   ├── BingoBoard.java      One 5x5 card: shuffle, mark, detect lines
│   └── BingoGame.java       Rules, rounds, computer strategy, winner
├── data/
│   ├── BingoDbHelper.java   SQLiteOpenHelper: create table, insert, query, delete
│   └── BingoRecord.java     One database row as a Java object
└── ui/
    └── SquareGridLayout.java  Keeps the card square on any screen
app/src/main/res/            Layouts, strings, colours, theme
app/src/test/.../BingoGameTest.java   Unit tests for the game rules
```

## Step-by-step for the assignment

### 1. Clean the old project and copy it to a new project

1. Open your previous project in Android Studio and run **Build → Clean Project** (or type `./gradlew clean` in the Terminal tab).
2. **File → Close Project**, then copy the whole project folder in File Explorer / Finder and rename the copy (for example `BingoSQLite`).
3. In the copy, delete the generated folders `.idea`, `.gradle`, `build`, `app/build` and the file `local.properties`. Android Studio recreates them.
4. Open the copy in Android Studio. Change `rootProject.name` in `settings.gradle(.kts)` and `applicationId` in `app/build.gradle(.kts)` so the new app does not replace the previous app on your phone.

### 2–4. Add the code from this repository

Copy the `java`, `res` and `test` files into the matching folders of your copied project and add the three `<activity>` entries from `AndroidManifest.xml`. If your package name is not `com.example.bingogame`, change the `package` line at the top of each Java file (or use **Refactor → Rename** on the package) and update the `<com.example.bingogame.ui.SquareGridLayout>` tag in `activity_game.xml`.

You can also open this folder directly in Android Studio as a complete project. If Android Studio offers to update the Android Gradle Plugin, accept.

Run the unit tests by right-clicking `BingoGameTest` → **Run 'BingoGameTest'**.

### 5. Upload to GitHub

1. On github.com, click **New repository**, give it a name, set it to **Public** and leave "Add a README" and ".gitignore" unchecked (this project already has both).
2. In Android Studio's Terminal, from the project folder:

   ```bash
   git init
   git add .
   git commit -m "Bingo game with SQLite records"
   git branch -M main
   git remote add origin https://github.com/YOUR-USERNAME/YOUR-REPO.git
   git push -u origin main
   ```

   (Or use **Git → GitHub → Share Project on GitHub** in Android Studio.)
3. Refresh the GitHub page and check that `build/`, `.idea/` and `local.properties` were **not** uploaded.
4. Copy the repository URL (`https://github.com/YOUR-USERNAME/YOUR-REPO`) and submit it on Moodle.

## Notes

- `targetSdk` is 34 so the screens keep the classic layout below the status bar. Targeting 35+ makes Android 15 draw edge-to-edge, which needs extra window-inset code.
- Database work runs on the main thread because each query is tiny. A larger app should move it to a background thread.
