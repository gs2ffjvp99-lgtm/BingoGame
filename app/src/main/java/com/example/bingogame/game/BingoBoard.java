package com.example.bingogame.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * BingoBoard models ONE 5 x 5 Bingo card.
 *
 * <p>The card holds the numbers 1 to 25 in a random order. When a number is
 * "called" (by the player or by the computer) its cell becomes <b>marked</b>.
 * A Bingo happens when all five cells of a row, a column or one of the two
 * diagonals are marked.</p>
 *
 * <p>Cells are addressed with a single index from 0 to 24:</p>
 * <pre>
 *   index = row * SIZE + column
 *
 *    0  1  2  3  4
 *    5  6  7  8  9
 *   10 11 12 13 14
 *   15 16 17 18 19
 *   20 21 22 23 24
 * </pre>
 *
 * <p>This class is plain Java (no Android imports), so the rules can be
 * unit-tested on the computer without an emulator. It implements
 * {@link Serializable} so the whole game can be stored in a Bundle when the
 * screen rotates.</p>
 */
public class BingoBoard implements Serializable {

    /** Version id required by Serializable. Change it only if the fields change. */
    private static final long serialVersionUID = 1L;

    /** Number of rows (and columns) on the card. */
    public static final int SIZE = 5;

    /** Number of cells on the card; also the largest number on it (1..25). */
    public static final int CELL_COUNT = SIZE * SIZE;

    /**
     * All 12 winning lines: 5 rows + 5 columns + 2 diagonals.
     * Each line is an array of 5 cell indexes. The table is the same for every
     * board, so it is static and built only once when the class is loaded.
     */
    private static final int[][] LINES = buildLines();

    /** numbers[i] = the number printed in cell i. */
    private final int[] numbers = new int[CELL_COUNT];

    /** marked[i] = true once the number in cell i has been called. */
    private final boolean[] marked = new boolean[CELL_COUNT];

    /**
     * Creates a new card with the numbers 1..25 shuffled into random cells.
     *
     * @param random random generator used for shuffling. Passing it in (instead
     *               of creating one here) lets unit tests use a fixed seed so
     *               the board is the same every time the test runs.
     */
    public BingoBoard(Random random) {
        // Step 1: put the numbers 1..25 into a list.
        List<Integer> pool = new ArrayList<>(CELL_COUNT);
        for (int number = 1; number <= CELL_COUNT; number++) {
            pool.add(number);
        }

        // Step 2: shuffle the list so every game gets a different card.
        Collections.shuffle(pool, random);

        // Step 3: copy the shuffled numbers into the cells.
        for (int i = 0; i < CELL_COUNT; i++) {
            numbers[i] = pool.get(i);
        }
    }

    // ------------------------------------------------------------------
    // Reading the board
    // ------------------------------------------------------------------

    /**
     * @param index cell index 0..24
     * @return the number shown in that cell
     */
    public int getNumberAt(int index) {
        return numbers[index];
    }

    /**
     * @param index cell index 0..24
     * @return true if that cell has already been marked
     */
    public boolean isMarkedAt(int index) {
        return marked[index];
    }

    /**
     * Finds where a number is printed on this card.
     *
     * @param number the number to look for (1..25)
     * @return its cell index 0..24, or -1 if the number is not on the card
     */
    public int indexOf(int number) {
        for (int i = 0; i < CELL_COUNT; i++) {
            if (numbers[i] == number) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Changing the board
    // ------------------------------------------------------------------

    /**
     * Marks a called number on this card.
     *
     * @param number the called number
     * @return true if a cell was newly marked; false if the number is not on
     *         the card or was already marked before
     */
    public boolean mark(int number) {
        int index = indexOf(number);
        if (index < 0 || marked[index]) {
            return false;
        }
        marked[index] = true;
        return true;
    }

    // ------------------------------------------------------------------
    // Bingo detection
    // ------------------------------------------------------------------

    /**
     * Looks for the first line whose five cells are all marked.
     *
     * <p>The returned numbers are the "five numbers of Bingo" that the app
     * saves into the SQLite database. They are listed in board order
     * (left to right for rows, top to bottom for columns and diagonals).</p>
     *
     * @return the five numbers of the first complete line, or null if there
     *         is no Bingo yet
     */
    public int[] getFirstBingoNumbers() {
        for (int[] line : LINES) {
            if (countMarkedIn(line) == SIZE) {
                int[] result = new int[SIZE];
                for (int k = 0; k < SIZE; k++) {
                    result[k] = numbers[line[k]];
                }
                return result;
            }
        }
        return null;
    }

    /** @return true if at least one row, column or diagonal is complete. */
    public boolean hasBingo() {
        return getFirstBingoNumbers() != null;
    }

    /**
     * Shows how close this card is to a Bingo.
     *
     * @return the largest number of marked cells found in any single line
     *         (0..5). The game screen uses it to show the computer's progress.
     */
    public int getBestLineProgress() {
        int best = 0;
        for (int[] line : LINES) {
            best = Math.max(best, countMarkedIn(line));
        }
        return best;
    }

    /**
     * Rates how useful it would be to mark the cell at {@code index}.
     * The computer player calls the number with the highest score.
     *
     * <p>For every line that passes through the cell we add
     * (markedCellsInThatLine + 1)². Squaring makes nearly finished lines far
     * more attractive, so the computer goes for a quick Bingo. The "+ 1" still
     * gives a score to empty lines, so on a fresh card the centre cell (which
     * sits on 4 lines) is preferred.</p>
     *
     * @param index cell index 0..24
     * @return the score, or -1 if the cell is already marked
     */
    public int scoreCell(int index) {
        if (marked[index]) {
            return -1;
        }
        int score = 0;
        for (int[] line : LINES) {
            if (lineContains(line, index)) {
                int progress = countMarkedIn(line) + 1;
                score += progress * progress;
            }
        }
        return score;
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /** Counts how many cells of one line are marked (0..5). */
    private int countMarkedIn(int[] line) {
        int count = 0;
        for (int index : line) {
            if (marked[index]) {
                count++;
            }
        }
        return count;
    }

    /** @return true if the line passes through the given cell. */
    private static boolean lineContains(int[] line, int index) {
        for (int cell : line) {
            if (cell == index) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the table of the 12 winning lines.
     * Called once, when the class is loaded.
     */
    private static int[][] buildLines() {
        int[][] lines = new int[SIZE * 2 + 2][SIZE];
        int lineNo = 0;

        // 5 rows, e.g. row 0 = cells 0, 1, 2, 3, 4
        for (int row = 0; row < SIZE; row++, lineNo++) {
            for (int col = 0; col < SIZE; col++) {
                lines[lineNo][col] = row * SIZE + col;
            }
        }

        // 5 columns, e.g. column 0 = cells 0, 5, 10, 15, 20
        for (int col = 0; col < SIZE; col++, lineNo++) {
            for (int row = 0; row < SIZE; row++) {
                lines[lineNo][row] = row * SIZE + col;
            }
        }

        // Diagonal from top-left to bottom-right = cells 0, 6, 12, 18, 24
        for (int i = 0; i < SIZE; i++) {
            lines[lineNo][i] = i * SIZE + i;
        }
        lineNo++;

        // Diagonal from top-right to bottom-left = cells 4, 8, 12, 16, 20
        for (int i = 0; i < SIZE; i++) {
            lines[lineNo][i] = i * SIZE + (SIZE - 1 - i);
        }

        return lines;
    }
}
