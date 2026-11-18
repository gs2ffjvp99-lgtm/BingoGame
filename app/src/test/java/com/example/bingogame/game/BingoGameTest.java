package com.example.bingogame.game;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

/**
 * Unit tests for {@link BingoBoard} and {@link BingoGame}.
 *
 * <p>They run on the computer's JVM (no emulator needed):
 * right-click this file in Android Studio and choose "Run 'BingoGameTest'".
 * A fixed Random seed makes every run use the same cards.</p>
 */
public class BingoGameTest {

    /** Every card must contain each number 1..25 exactly once. */
    @Test
    public void boardContainsEachNumberOnce() {
        BingoBoard board = new BingoBoard(new Random(42));
        boolean[] seen = new boolean[BingoBoard.CELL_COUNT + 1];

        for (int i = 0; i < BingoBoard.CELL_COUNT; i++) {
            int number = board.getNumberAt(i);
            assertTrue("number out of range: " + number, number >= 1 && number <= 25);
            assertFalse("duplicate number: " + number, seen[number]);
            seen[number] = true;
        }
    }

    /** Marking the five cells of the top row must produce a Bingo with those numbers. */
    @Test
    public void fullRowIsBingo() {
        BingoBoard board = new BingoBoard(new Random(7));
        int[] expected = new int[BingoBoard.SIZE];

        for (int col = 0; col < BingoBoard.SIZE; col++) {
            assertNull("no Bingo before the row is complete", board.getFirstBingoNumbers());
            expected[col] = board.getNumberAt(col); // row 0 = cells 0..4
            board.mark(expected[col]);
        }

        assertArrayEquals(expected, board.getFirstBingoNumbers());
    }

    /** The diagonal 0, 6, 12, 18, 24 must also count as Bingo. */
    @Test
    public void diagonalIsBingo() {
        BingoBoard board = new BingoBoard(new Random(3));
        for (int i = 0; i < BingoBoard.SIZE; i++) {
            board.mark(board.getNumberAt(i * BingoBoard.SIZE + i));
        }
        assertTrue(board.hasBingo());
    }

    /** Calling the same number twice must be rejected. */
    @Test
    public void sameNumberCannotBeCalledTwice() {
        BingoGame game = new BingoGame(new Random(1));
        int number = game.getPlayerNumberAt(0);

        assertTrue(game.playRound(number));
        assertFalse(game.playRound(number));
    }

    /**
     * Plays many complete games. Each must end within 13 rounds
     * (25 numbers / 2 calls per round) and report five called winning numbers.
     */
    @Test
    public void everyGameEndsWithFiveBingoNumbers() {
        for (long seed = 0; seed < 500; seed++) {
            BingoGame game = new BingoGame(new Random(seed));

            while (!game.isOver()) {
                assertTrue(game.playRound(firstUncalledNumber(game)));
            }

            assertTrue(game.getRoundNumber() >= 1 && game.getRoundNumber() <= 13);
            int[] bingo = game.getBingoNumbers();
            assertNotNull(bingo);
            assertEquals(5, bingo.length);
            for (int number : bingo) {
                assertNotNull("winning number was never called", game.getCaller(number));
            }
        }
    }

    /** Helper: the first number on the player's card that nobody has called. */
    private static int firstUncalledNumber(BingoGame game) {
        for (int i = 0; i < BingoBoard.CELL_COUNT; i++) {
            int number = game.getPlayerNumberAt(i);
            if (game.getCaller(number) == null) {
                return number;
            }
        }
        throw new IllegalStateException("all numbers called but no winner");
    }
}
