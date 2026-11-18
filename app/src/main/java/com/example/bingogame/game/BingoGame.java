package com.example.bingogame.game;

import java.io.Serializable;
import java.util.Random;

/**
 * BingoGame holds the rules and the current state of ONE Bingo match:
 * the human player against a computer opponent.
 *
 * <h3>Rules</h3>
 * <ol>
 *   <li>Each side gets its own shuffled 5 x 5 card with the numbers 1-25.</li>
 *   <li>Every <b>round</b> has two calls: first the player taps a number on
 *       their card, then the computer calls a number.</li>
 *   <li>A called number is marked on <b>both</b> cards.</li>
 *   <li>After every call both cards are checked. The first side that
 *       completes a row, column or diagonal wins ("Bingo"). If one call
 *       completes a line on both cards at the same time, the player wins.</li>
 *   <li>The round counter starts at 1 and goes up after the computer's call,
 *       so when the game ends it tells in which round the Bingo happened.
 *       This is the "round number" saved in the database.</li>
 * </ol>
 *
 * <p>The class has no Android code. The Activity only calls
 * {@link #playRound(int)} and then reads the getters to redraw the screen.</p>
 */
public class BingoGame implements Serializable {

    /** Version id required by Serializable. */
    private static final long serialVersionUID = 1L;

    /** Who called a particular number. */
    public enum Caller { PLAYER, COMPUTER }

    /** Result of the match so far. */
    public enum Winner { NONE, PLAYER, COMPUTER }

    /** Shared random generator (shuffling cards and breaking ties). */
    private final Random random;

    /** The card shown on screen. */
    private final BingoBoard playerBoard;

    /** The computer's card. It is hidden; only its progress is shown. */
    private final BingoBoard computerBoard;

    /**
     * callers[n] tells who called number n. Index 0 is unused so that the
     * array index equals the Bingo number. null means "not called yet".
     */
    private final Caller[] callers = new Caller[BingoBoard.CELL_COUNT + 1];

    /** Current round, starting at 1. */
    private int roundNumber = 1;

    /** Who has won; NONE while the game is still running. */
    private Winner winner = Winner.NONE;

    /** The five numbers of the winning line (null until someone wins). */
    private int[] bingoNumbers;

    /** The number the computer called last (0 = it has not called yet). */
    private int lastComputerNumber;

    /** Creates a game with a truly random shuffle (used by the app). */
    public BingoGame() {
        this(new Random());
    }

    /**
     * Creates a game with a given random generator.
     *
     * @param random use {@code new Random(seed)} in tests to get the same cards every run
     */
    public BingoGame(Random random) {
        this.random = random;
        this.playerBoard = new BingoBoard(random);
        this.computerBoard = new BingoBoard(random);
    }

    // ------------------------------------------------------------------
    // Playing
    // ------------------------------------------------------------------

    /**
     * Plays one full round: the player's call followed by the computer's call.
     * The round stops early as soon as somebody gets Bingo.
     *
     * @param playerNumber the number the player tapped (1..25)
     * @return true if the move was accepted; false if the game is already
     *         over or the number is invalid / already called
     */
    public boolean playRound(int playerNumber) {
        // Ignore taps after the game has ended, numbers outside 1..25 and
        // numbers that were already called (e.g. a fast double tap).
        if (isOver() || !isValidNumber(playerNumber) || callers[playerNumber] != null) {
            return false;
        }

        // ----- 1. The player's call -----
        callNumber(playerNumber, Caller.PLAYER);
        if (checkForWinner()) {
            return true; // Bingo! The round ends here.
        }

        // ----- 2. The computer's call -----
        int computerNumber = chooseComputerNumber();
        if (computerNumber > 0) { // always true while nobody has won, kept as a safety check
            lastComputerNumber = computerNumber;
            callNumber(computerNumber, Caller.COMPUTER);
            if (checkForWinner()) {
                return true;
            }
        }

        // ----- 3. Nobody has Bingo yet: go to the next round -----
        roundNumber++;
        return true;
    }

    // ------------------------------------------------------------------
    // Getters used by the user interface
    // ------------------------------------------------------------------

    /** @return the number in cell {@code index} (0..24) of the player's card. */
    public int getPlayerNumberAt(int index) {
        return playerBoard.getNumberAt(index);
    }

    /** @return who called {@code number}, or null if it has not been called. */
    public Caller getCaller(int number) {
        return isValidNumber(number) ? callers[number] : null;
    }

    /** @return the current round (or, after the game, the round of the Bingo). */
    public int getRoundNumber() {
        return roundNumber;
    }

    /** @return who won, or {@link Winner#NONE} while the game is running. */
    public Winner getWinner() {
        return winner;
    }

    /** @return true once somebody has Bingo. */
    public boolean isOver() {
        return winner != Winner.NONE;
    }

    /** @return the number the computer called last, or 0 if none yet. */
    public int getLastComputerNumber() {
        return lastComputerNumber;
    }

    /** @return the highest number of marked cells in any line of the computer's card (0..5). */
    public int getComputerBestLineProgress() {
        return computerBoard.getBestLineProgress();
    }

    /**
     * @return a copy of the five numbers of the winning line, or null if the
     *         game is not over. A copy is returned so callers cannot change
     *         the game's internal state.
     */
    public int[] getBingoNumbers() {
        return bingoNumbers == null ? null : bingoNumbers.clone();
    }

    /** @return true if {@code number} belongs to the winning line. */
    public boolean isBingoNumber(int number) {
        if (bingoNumbers == null) {
            return false;
        }
        for (int n : bingoNumbers) {
            if (n == number) {
                return true;
            }
        }
        return false;
    }

    /**
     * Turns numbers into readable text, e.g. {3, 17, 22} becomes "3, 17, 22".
     * Shared by the game screen and the records screen.
     */
    public static String formatNumbers(int[] numbers) {
        if (numbers == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < numbers.length; i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(numbers[i]);
        }
        return text.toString();
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /** @return true if {@code number} is between 1 and 25. */
    private static boolean isValidNumber(int number) {
        return number >= 1 && number <= BingoBoard.CELL_COUNT;
    }

    /** Remembers who called the number and marks it on both cards. */
    private void callNumber(int number, Caller caller) {
        callers[number] = caller;
        playerBoard.mark(number);
        computerBoard.mark(number);
    }

    /**
     * Checks both cards for a complete line. The player's card is checked
     * first, so the player wins a tie.
     *
     * @return true if the game is now over
     */
    private boolean checkForWinner() {
        int[] line = playerBoard.getFirstBingoNumbers();
        if (line != null) {
            winner = Winner.PLAYER;
            bingoNumbers = line;
            return true;
        }

        line = computerBoard.getFirstBingoNumbers();
        if (line != null) {
            winner = Winner.COMPUTER;
            bingoNumbers = line;
            return true;
        }
        return false;
    }

    /**
     * The computer's strategy: score every free cell on its own card with
     * {@link BingoBoard#scoreCell(int)} and call the number with the best
     * score. When several cells share the best score one of them is picked
     * at random, so the computer does not play the same way every game.
     *
     * @return the number to call, or -1 if every number was already called
     */
    private int chooseComputerNumber() {
        int bestScore = -1;
        int bestNumber = -1;
        int ties = 0;

        for (int i = 0; i < BingoBoard.CELL_COUNT; i++) {
            int score = computerBoard.scoreCell(i); // -1 for cells already marked
            if (score < 0) {
                continue;
            }
            if (score > bestScore) {
                // New best cell: forget the previous candidates.
                bestScore = score;
                bestNumber = computerBoard.getNumberAt(i);
                ties = 1;
            } else if (score == bestScore) {
                // Same score: "reservoir sampling" gives every tied cell
                // an equal chance (1/ties) of being the final choice.
                ties++;
                if (random.nextInt(ties) == 0) {
                    bestNumber = computerBoard.getNumberAt(i);
                }
            }
        }
        return bestNumber;
    }
}
