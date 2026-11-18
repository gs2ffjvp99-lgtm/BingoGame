package com.example.bingogame.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BingoDbHelper creates and manages the app's SQLite database.
 *
 * <p>{@link SQLiteOpenHelper} does the hard work for us: the first time the
 * database is opened it calls {@link #onCreate(SQLiteDatabase)} to build the
 * table, and when {@link #DATABASE_VERSION} is increased it calls
 * {@link #onUpgrade(SQLiteDatabase, int, int)}.</p>
 *
 * <h3>Table: bingo_records</h3>
 * <pre>
 * | column       | type    | meaning                               |
 * |--------------|---------|---------------------------------------|
 * | _id          | INTEGER | primary key, auto increment           |
 * | username     | TEXT    | player name (i)                       |
 * | round_number | INTEGER | round in which Bingo happened (ii)    |
 * | number1..5   | INTEGER | the five numbers of the Bingo (iii)   |
 * | system_time  | TEXT    | device time when saved (iv)           |
 * </pre>
 *
 * <p>The database file is stored on the device at
 * /data/data/com.example.bingogame/databases/bingo.db and can be viewed in
 * Android Studio with <i>View &gt; Tool Windows &gt; App Inspection &gt;
 * Database Inspector</i> while the app is running.</p>
 *
 * <p>Note: the queries are tiny, so they run on the main thread to keep the
 * code easy to follow. A larger app should move database work to a
 * background thread.</p>
 */
public class BingoDbHelper extends SQLiteOpenHelper {

    // ------------------------------------------------------------------
    // Database and table names
    // ------------------------------------------------------------------

    /** File name of the database. */
    private static final String DATABASE_NAME = "bingo.db";

    /** Schema version. Increase it whenever the table structure changes. */
    private static final int DATABASE_VERSION = 1;

    /** Name of the table that stores one row per Bingo win. */
    public static final String TABLE_RECORDS = "bingo_records";

    // ------------------------------------------------------------------
    // Column names (constants avoid typing mistakes in SQL strings)
    // ------------------------------------------------------------------

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_ROUND_NUMBER = "round_number";
    public static final String COLUMN_NUMBER_1 = "number1";
    public static final String COLUMN_NUMBER_2 = "number2";
    public static final String COLUMN_NUMBER_3 = "number3";
    public static final String COLUMN_NUMBER_4 = "number4";
    public static final String COLUMN_NUMBER_5 = "number5";
    public static final String COLUMN_SYSTEM_TIME = "system_time";

    /** The five number columns in order, so we can loop over them. */
    private static final String[] NUMBER_COLUMNS = {
            COLUMN_NUMBER_1, COLUMN_NUMBER_2, COLUMN_NUMBER_3, COLUMN_NUMBER_4, COLUMN_NUMBER_5
    };

    /** Format used for the system_time column, e.g. "2026-11-17 14:05:09". */
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** SQL statement that creates the table. */
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_RECORDS + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_USERNAME + " TEXT NOT NULL, "
                    + COLUMN_ROUND_NUMBER + " INTEGER NOT NULL, "
                    + COLUMN_NUMBER_1 + " INTEGER NOT NULL, "
                    + COLUMN_NUMBER_2 + " INTEGER NOT NULL, "
                    + COLUMN_NUMBER_3 + " INTEGER NOT NULL, "
                    + COLUMN_NUMBER_4 + " INTEGER NOT NULL, "
                    + COLUMN_NUMBER_5 + " INTEGER NOT NULL, "
                    + COLUMN_SYSTEM_TIME + " TEXT NOT NULL)";

    /** SQL statement that removes the table (used when upgrading). */
    private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_RECORDS;

    /**
     * @param context any Context; the application context is used internally
     *                so an Activity is never leaked by this helper
     */
    public BingoDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ------------------------------------------------------------------
    // SQLiteOpenHelper callbacks
    // ------------------------------------------------------------------

    /** Called automatically the very first time the database is opened. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    /**
     * Called when DATABASE_VERSION is increased. For this classroom project we
     * simply drop the old table and create a new one (old records are lost).
     * A real app would migrate the data with ALTER TABLE instead.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }

    // ------------------------------------------------------------------
    // Create (INSERT)
    // ------------------------------------------------------------------

    /**
     * Saves one Bingo win. The system time is taken from the device clock at
     * the moment of saving.
     *
     * @param username     player name
     * @param roundNumber  round in which the Bingo happened
     * @param bingoNumbers exactly five numbers of the winning line
     * @return the new row id, or -1 if the insert failed
     * @throws IllegalArgumentException if bingoNumbers does not contain 5 numbers
     */
    public long insertRecord(String username, int roundNumber, int[] bingoNumbers) {
        if (bingoNumbers == null || bingoNumbers.length != NUMBER_COLUMNS.length) {
            throw new IllegalArgumentException("A Bingo record needs exactly 5 numbers");
        }

        // ContentValues maps column name -> value; Android builds the
        // "INSERT INTO ... VALUES (...)" SQL for us and escapes the values,
        // which protects against SQL injection from the username.
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_ROUND_NUMBER, roundNumber);
        for (int i = 0; i < NUMBER_COLUMNS.length; i++) {
            values.put(NUMBER_COLUMNS[i], bingoNumbers[i]);
        }
        values.put(COLUMN_SYSTEM_TIME, formatSystemTime(System.currentTimeMillis()));

        SQLiteDatabase db = getWritableDatabase();
        return db.insert(TABLE_RECORDS, null, values);
    }

    // ------------------------------------------------------------------
    // Read (SELECT)
    // ------------------------------------------------------------------

    /**
     * Reads every saved record, newest first.
     * Equivalent SQL: SELECT * FROM bingo_records ORDER BY _id DESC
     *
     * @return list of records (empty if nothing has been saved yet)
     */
    public List<BingoRecord> getAllRecords() {
        List<BingoRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // try-with-resources closes the Cursor automatically, even on errors.
        try (Cursor cursor = db.query(
                TABLE_RECORDS,
                null,                   // null = all columns
                null, null,             // no WHERE clause
                null, null,             // no GROUP BY / HAVING
                COLUMN_ID + " DESC")) { // ORDER BY: newest first

            // Look up each column position once, before the loop.
            int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
            int usernameIndex = cursor.getColumnIndexOrThrow(COLUMN_USERNAME);
            int roundIndex = cursor.getColumnIndexOrThrow(COLUMN_ROUND_NUMBER);
            int timeIndex = cursor.getColumnIndexOrThrow(COLUMN_SYSTEM_TIME);
            int[] numberIndexes = new int[NUMBER_COLUMNS.length];
            for (int i = 0; i < NUMBER_COLUMNS.length; i++) {
                numberIndexes[i] = cursor.getColumnIndexOrThrow(NUMBER_COLUMNS[i]);
            }

            // moveToNext() returns false after the last row.
            while (cursor.moveToNext()) {
                int[] numbers = new int[NUMBER_COLUMNS.length];
                for (int i = 0; i < numbers.length; i++) {
                    numbers[i] = cursor.getInt(numberIndexes[i]);
                }
                records.add(new BingoRecord(
                        cursor.getLong(idIndex),
                        cursor.getString(usernameIndex),
                        cursor.getInt(roundIndex),
                        numbers,
                        cursor.getString(timeIndex)));
            }
        }
        return records;
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    /**
     * Deletes every record. Passing "1" as the WHERE clause makes SQLite
     * delete all rows AND return how many were deleted.
     *
     * @return number of deleted rows
     */
    public int deleteAllRecords() {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_RECORDS, "1", null);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Converts a time in milliseconds (from System.currentTimeMillis()) into
     * readable text in the device's time zone.
     */
    static String formatSystemTime(long millis) {
        // Locale.US keeps the digits in Western format on every device.
        SimpleDateFormat format = new SimpleDateFormat(TIME_PATTERN, Locale.US);
        return format.format(new Date(millis));
    }
}
