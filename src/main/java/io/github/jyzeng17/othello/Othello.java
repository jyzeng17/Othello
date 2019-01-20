package io.github.jyzeng17.othello;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Scanner;

public class Othello {

	public static final Logger logger = LogManager.getLogger();
	
	private static final Scanner scanner = new Scanner(System.in);

	private boolean userIsBlack;
	private Board board;
	private Computer computer;

	public Othello() {
		this.userIsBlack = readUserIsBlack();

		boolean playerIsBlack = true;
		long initialBlackBitBoard = (1L << 28) + (1L << 35);
		long initialWhiteBitBoard = (1L << 27) + (1L << 36);

		this.board = new Board(playerIsBlack, initialBlackBitBoard, initialWhiteBitBoard);

		this.computer = new Computer();
	}

	public static void main(String[] args) {
		Othello othello = new Othello();
		othello.run();
	}

	public static void printMessage(String message) {
		System.out.print("[Othello] " + message);
	}

	public static void printNewLineMessage(String message) {
		System.out.print("\n[Othello] " + message);
	}

	public void run() {
		Player currentPlayer = Player.BLACK;

		boolean hasSkippedOnce = false; // for debugging

		while (true) {
			// test
			if (board.isGameEnd()) {
				break;
			}

			printGameState(currentPlayer);

			// check if board has next moves
			if (!board.hasNextMoves()) {
				if (hasSkippedOnce) {
					logger.fatal("Both sides have no legal moves");
					System.exit(1);
				}

				printMessage("No legal moves, skip this turn\n\n");

				board.switchPlayer();

				hasSkippedOnce = true;
			} else {
				playerPerformsAction(currentPlayer);

				hasSkippedOnce = false;
			}

			// check if game ends
			if (board.isGameEnd()) {
				break;
			}

			// switch player
			currentPlayer = (currentPlayer == Player.BLACK)? Player.WHITE : Player.BLACK;
		}

		// print final result
		board.printFinalResult(userIsBlack);
	}

	private boolean readUserIsBlack() {
		do {
			printNewLineMessage("User starts first? [y/n] ");

			String answer = scanner.nextLine();

			if (answer.compareTo("y") == 0) {
				System.out.println("");

				return true;
			}
			
			if (answer.compareTo("n") == 0) {
				System.out.println("");

				return false;
			}

			printMessage("Invalid answer.\n");
		} while (true);
	}

	private void printGameState(Player currentPlayer) {
		long blackBitboard = board.getBlackBitboard();
		long whiteBitboard = board.getWhiteBitboard();
		int blackScore = 0;
		int whiteScore = 0;
		long mask = 1;

		// calculate the scores
		for (int i = 0; i < 64; ++i) {
			if ((blackBitboard & (mask << i)) != 0) {
				++blackScore;
				continue;
			}

			if ((whiteBitboard & (mask << i)) != 0) {
				++whiteScore;
			}
		}

		if (userIsBlack) {
			printMessage("Black (User) : White (Computer) = " + blackScore + " : " + whiteScore + "\n");

			printMessage(((currentPlayer == Player.BLACK)? "User" : "Computer") + "'s turn:\n");
		} else {
			printMessage("Black (Computer) : White (User) = " + blackScore + " : " + whiteScore + "\n");

			printMessage(((currentPlayer == Player.WHITE)? "User" : "Computer") + "'s turn:\n");
		}

		board.printBoard();
	}

	private void playerPerformsAction(Player currentPlayer) {
		byte nextMoveBitIndex = -1;

		if ((userIsBlack && (currentPlayer == Player.BLACK)) || (!userIsBlack && (currentPlayer == Player.WHITE))) {
			// user's turn
			while (true) {
				printMessage("User's move (e.g., a1):\n");
				printMessage("> ");

				String input = scanner.nextLine();

				if (input.length() == 2) {
					byte col = (byte)(input.charAt(0) - 97);
					byte row = (byte)(input.charAt(1) - 49);

					if ((col >= 0) && (col < 8) && (row >= 0) && (row < 8)) {
						nextMoveBitIndex = (byte)(8 * row + col);

						// check if the square is a legal next move position
						if (board.isSquareLegalNextMove(nextMoveBitIndex)) {
							break;
						}
					}
				}

				printMessage("Invalid input.\n");
			}
		} else {
			// computer's turn
			nextMoveBitIndex = computer.generateMoves(board);

			char col = (char)((nextMoveBitIndex % 8) + 97);
			char row = (char)(Math.floor((nextMoveBitIndex / 8)) + 49);
			printMessage("Computer's move: " + col + row + "\n");
		}

		// Update board state
		System.out.println("");
		board = board.applyNextMove(nextMoveBitIndex);
	}
}
