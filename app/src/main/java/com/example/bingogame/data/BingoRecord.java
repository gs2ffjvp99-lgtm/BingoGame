package com.example.bingogame.data;

import com.example.bingogame.game.BingoGame;

/**
 * BingoRecord is one row of the {@code bingo_records} SQLite table.
 *
 * <p>It is an immutable "data holder": all fields are final and are set once
 * in the constructor. {@link BingoDbHelper} creates these objects when it
 * reads the database, and the records screen displays them.</p>
 */
public class BingoRecord {

    /** Auto-increment primary key (_id column). */
    private final long id;

    /** Name the player typed on the start screen. */
    private final String username;

    /** Round in which the player got Bingo. */
    private final int roundNumber;

    /** The five numbers of the winning line. */
    private final int[] bingoNumbers;

    /** Device date and time when the record was saved ("yyyy-MM-dd HH:mm:ss"). */
    private final String systemTime;

    /**
     * @param id           row id from the database
     * @param username     player name
     * @param roundNumber  round of the Bingo
     * @param bingoNumbers the five winning numbers (copied, so later changes to
     *                     the caller's array do not affect this record)
     * @param systemTime   formatted save time
     */
    public BingoRecord(long id, String username, int roundNumber, int[] bingoNumbers, String systemTime) {
        this.id = id;
        this.username = username;
        this.roundNumber = roundNumber;
        this.bingoNumbers = bingoNumbers.clone();
        this.systemTime = systemTime;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    /** @return a copy of the five numbers (keeps this object immutable). */
    public int[] getBingoNumbers() {
        return bingoNumbers.clone();
    }

    /** @return the five numbers as text, e.g. "7, 19, 2, 25, 11". */
    public String getBingoNumbersText() {
        return BingoGame.formatNumbers(bingoNumbers);
    }

    public String getSystemTime() {
        return systemTime;
    }
}
