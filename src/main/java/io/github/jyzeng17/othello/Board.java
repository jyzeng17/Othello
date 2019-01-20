package io.github.jyzeng17.othello;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;

public class Board implements Comparable<Board> {

	public static final Logger logger = LogManager.getLogger();

	//public static final float EXPLORATION_PARAMETER = 0.3162f; // sqrt(1/10)
	public static final float EXPLORATION_PARAMETER = 0.2236f; // sqrt(1/20)
	//public static final float EXPLORATION_PARAMETER = 0.7071f; // test sqrt(1/2)
	//public static final int EXPLORATION_PARAMETER = 1; // test

	private boolean playerIsBlack;
	private long blackBitboard;
	private long whiteBitboard;
	private long nextMovesBitboard;

	// for MCS
	private int winngTimes;
	private int totalSimulationTimes;
	private int siblingsSimulationTimes;

	static enum Direction {
		TOP,
		LEFT,
		RIGHT,
		BOTTOM,
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT
	}

	public Board(boolean playerIsBlack, long blackBitboard, long whiteBitboard) {
		this(playerIsBlack, blackBitboard, whiteBitboard, 0, 0, 0);
	}

	public Board(boolean playerIsBlack, long blackBitboard, long whiteBitboard, int winngTimes, int totalSimulationTimes, int siblingsSimulationTimes) {
		this.playerIsBlack = playerIsBlack;
		this.blackBitboard = blackBitboard;
		this.whiteBitboard = whiteBitboard;
		this.nextMovesBitboard = findNextMovesBitboard();

		this.winngTimes = winngTimes;
		this.totalSimulationTimes = totalSimulationTimes;
		this.siblingsSimulationTimes = siblingsSimulationTimes;
	}

	// Comparable interface's method (might have to override equals() as well if something's wrong)
	@Override
	public int compareTo(Board comparedBoard) {
		int result;
		float thisUCB = this.getUCB();
		float comparedUCB = comparedBoard.getUCB();

		// avoid the error caused by directly casting float to integer as result
		if (comparedUCB > thisUCB) {
			result = 1;
		} else if (comparedUCB < thisUCB) {
			result = -1;
		} else {
			result = 0;
		}

		//logger.debug("this board's UCB : compared board's UCB = " + thisUCB + " : " + comparedUCB + ", result = " + result);

		// a negative integer, zero, or a positive integer
		// as this object is less than, equal to, or greater than the specified object
		return result;
	}

	public Square getSquare(int index) {
		long mask = 1L << index;

		if ((blackBitboard & mask) != 0) {
			return Square.BLACK;
		}
		else if ((whiteBitboard & mask) != 0) {
			return Square.WHITE;
		}
		else {
			return Square.NONE;
		}
	}

	public float getWinningRate() {
		return (float)winngTimes / totalSimulationTimes;
	}

	public int getWinningTimes() {
		return winngTimes;
	}

	public int getTotalSimulationTimes() {
		return totalSimulationTimes;
	}

	public int getSiblingsSimulationTimes() {
		return siblingsSimulationTimes;
	}

	// from 0 ~ 60
	public int getGameTreeDepth() {
		long mergedBitboard = blackBitboard | whiteBitboard;
		long mask = 1L;

		int depth = 0;
		for (int i = 0; i < 64; ++i) {
			if ((mergedBitboard & (mask << i)) != 0) {
				++depth;
			}
		}

		return depth - 4;
	}

	public void updateWinningTimes(int extraWinngTimes) {
		winngTimes += extraWinngTimes;
	}

	public void updateSiblingsSimulationTimes(int extraSimulationTimes) {
		siblingsSimulationTimes += extraSimulationTimes;
	}

	public void updateTotalSimulationTimes(int extraSimulationTimes) {
		totalSimulationTimes += extraSimulationTimes;
	}

	public Player getPlayer() {
		return (playerIsBlack)? Player.BLACK : Player.WHITE;
	}

	public boolean getPlayerIsBlack() {
		return playerIsBlack;
	}

	public void addSiblingsSimulationTimes(int addedTimes) {
		siblingsSimulationTimes += addedTimes;
	}

	public Board applyNextMove(byte nextMoveBitIndex) {
		// check if the move is valid
		if ((nextMoveBitIndex < 0) || (nextMoveBitIndex >= 64) ||
				((nextMovesBitboard & (1L << nextMoveBitIndex)) == 0)) {
			return null;
				}

		// update value in 8 directions
		long newBlackBitboard = blackBitboard;
		long newWhiteBitboard = whiteBitboard;
		for (Direction d : Direction.values()) {
			// check if first adjacent square is an opponent piece
			byte firstBitIndex = findBitIndexInDirection(d, nextMoveBitIndex);
			if (firstBitIndex != -1) {
				long opponentBitboard = (playerIsBlack)? whiteBitboard : blackBitboard;
				if (((1L << firstBitIndex) & opponentBitboard) != 0) {
					// save changes in a buffer bitboard
					long changeBufferBitboard = 1L << firstBitIndex;

					// then check if there's a friend piece at the end
					byte otherBitIndex = findBitIndexInDirection(d, firstBitIndex);
					while (otherBitIndex != -1) {
						Square squareState = getSquare(otherBitIndex);

						// meet friend piece
						if (squareState == ((playerIsBlack)? Square.BLACK : Square.WHITE)) {
							newBlackBitboard = (playerIsBlack)? newBlackBitboard | (changeBufferBitboard + (1L << nextMoveBitIndex)) :
								newBlackBitboard ^ changeBufferBitboard;
							newWhiteBitboard = (!playerIsBlack)? newWhiteBitboard | (changeBufferBitboard + (1L << nextMoveBitIndex)) :
								newWhiteBitboard ^ changeBufferBitboard;
							break;
						}

						// meet an empty square
						if (squareState == Square.NONE) {
							// discard buffer changes
							break;
						}

						// meet opponent piece
						if (squareState == ((playerIsBlack)? Square.WHITE : Square.BLACK)) {
							// save changes in a buffer bitboard
							changeBufferBitboard += 1L << otherBitIndex;
						}

						otherBitIndex = findBitIndexInDirection(d, otherBitIndex);
					}
				}
			}
		}

		// check if new board state is different
		if (((newBlackBitboard ^ blackBitboard) != 0) && ((newWhiteBitboard ^ whiteBitboard) != 0)) {
			// check if new black board and new white board have same position
			if ((newBlackBitboard & newWhiteBitboard) != 0) {
				logger.fatal("In applying next move, new black board and new white board have same position");
				logger.fatal("print new black board");
				printBitboard(newBlackBitboard);
				logger.fatal("print new white board");
				printBitboard(newWhiteBitboard);
				System.exit(1);
			}
			return new Board(!playerIsBlack, newBlackBitboard, newWhiteBitboard);
		}

		return null;
	}

	public void printFinalResult(boolean userIsBlack) {
		byte blackFinalScore = 0;
		byte whiteFinalScore = 0;
		long mask = 1L;

		for (int i = 0; i < 64; ++i) {
			if ((blackBitboard & (mask << i)) != 0) {
				++blackFinalScore;
				continue;
			}

			if ((whiteBitboard & (mask << i)) != 0) {
				++whiteFinalScore;
			}
		}

		Othello.printMessage("Game's over!\n");
		Othello.printMessage("Black : White = " + blackFinalScore + " : " + whiteFinalScore + "\n");

		printBoard();

		if (blackFinalScore > whiteFinalScore) {
			Othello.printNewLineMessage(((userIsBlack)? "User" : "Computer") + " wins!\n");
		} else if (whiteFinalScore > blackFinalScore) {
			Othello.printNewLineMessage(((!userIsBlack)? "User" : "Computer") + " wins!\n");
		} else {
			Othello.printNewLineMessage("Draw!\n");
		}
	}

	public void printBoard() {
		System.out.println("  a b c d e f g h");

		for (int i = 0; i < 8; ++i) {
			System.out.printf(i + 1 + " ");

			for (int j = 0; j < 8; ++j) {
				// check if it's legal next move position index
				// if true, print its symbol and continue
				if (isSquareLegalNextMove(8 * i + j)) {
					System.out.printf("\u25a9 ");	// square with diagonal crosshatch fill unicode symbol
					continue;
				}

				Square squareState = getSquare(8 * i + j);

				switch (squareState) {
					case NONE:
						System.out.printf("\u25a1 ");	// white square unicode symbol
						break;
					case BLACK:
						System.out.printf("\u25cf ");	// black circle unicode symbol
						break;
					case WHITE:
						System.out.printf("\u25cb ");	// white circle unicode symbol
						break;
				}
			}

			System.out.printf(" " + (i + 1) + "\n");
		}

		System.out.println("  a b c d e f g h");
	}

	public Player getWinningPlayer() {
		byte blackFinalScore = 0;
		byte whiteFinalScore = 0;
		long mask = 1L;

		for (int i = 0; i < 64; ++i) {
			if ((blackBitboard & (mask << i)) != 0) {
				++blackFinalScore;
				continue;
			}

			if ((whiteBitboard & (mask << i)) != 0) {
				++whiteFinalScore;
			}
		}

		if (blackFinalScore > whiteFinalScore) {
			return Player.BLACK;
		} else if (whiteFinalScore > blackFinalScore) {
			return Player.WHITE;
		} else {
			return null;
		}
	}

	public long getBlackBitboard() {
		return blackBitboard;
	}

	public long getWhiteBitboard() {
		return whiteBitboard;
	}

	public long getNextMovesBitboard() {
		return nextMovesBitboard;
	}

	public float getUCB() {
		if (totalSimulationTimes == 0) {
			return 0;
		}

		float exploitation = (float)winngTimes / totalSimulationTimes;
		float exploration = (float)(EXPLORATION_PARAMETER * Math.sqrt(Math.log10(siblingsSimulationTimes) / totalSimulationTimes));

		//logger.debug("exploitation = " + exploitation + ", exploration = " + exploration);

		return exploitation + exploration;
	}

	public float getLosingUCB() {
		if (totalSimulationTimes == 0) {
			return 0;
		}

		int losingTimes = totalSimulationTimes - winngTimes;

		float exploitation = (float)losingTimes / totalSimulationTimes;
		float exploration = (float)(EXPLORATION_PARAMETER * Math.sqrt(Math.log10(siblingsSimulationTimes) / totalSimulationTimes));

		//logger.debug("exploitation = " + exploitation + ", exploration = " + exploration);

		return exploitation + exploration;
	}

	public ArrayList<Byte> getNextMovesArrayList() {
		ArrayList<Byte> array = new ArrayList<Byte>();

		for (byte i = 0; i < 64; ++i) {
			if (((1L << i) & nextMovesBitboard) != 0) {
				array.add(new Byte(i));
			}
		}

		return array;
	}

	public boolean hasNextMoves() {
		return (nextMovesBitboard != 0L);
	}

	public boolean isGameEnd() {
		Board opponentsBoard = new Board(!(this.playerIsBlack), this.blackBitboard, this.whiteBitboard);

		return ((nextMovesBitboard == 0L) && (opponentsBoard.getNextMovesBitboard() == 0L));
	}

	public boolean isSquareLegalNextMove(int index) {
		long mask = 1L << index;

		return ((nextMovesBitboard & mask) != 0)? true : false;
	}

	public void switchPlayer() {
		playerIsBlack = !playerIsBlack;
		nextMovesBitboard = findNextMovesBitboard();
	}

	private long findNextMovesBitboard() {
		// find empty squares: all - black - white
		long emptyBitboard = (~ (blackBitboard | whiteBitboard));

		// filter those who are not neighbors of an opponent piece
		long opponentBitboard = (playerIsBlack)? whiteBitboard : blackBitboard;
		long possibleNextMovesBitboard = emptyBitboard & getBitboardNeighbors(opponentBitboard);

		// iteratively check if squares is legal next move (lose bitwise-parallelism)
		long finalNextMovesBitboard = 0L;
		byte bitIndex = 0;
		while (bitIndex < 64) {
			if ((possibleNextMovesBitboard & 1) == 1) {
				// check 8 directions according to its index
				for (Direction d : Direction.values()) {
					if (isDirectionLegal(d, bitIndex)) {
						if ((finalNextMovesBitboard & (1L << bitIndex)) == 0) {
							finalNextMovesBitboard += 1L << bitIndex;
						}
					}
				}
			}
			possibleNextMovesBitboard >>>= 1;
			++bitIndex;
		}

		return finalNextMovesBitboard;
	}

	private boolean isDirectionLegal(Direction direction, byte bitIndex) {
		long opponentBitboard = (playerIsBlack)? whiteBitboard : blackBitboard;

		// ensure the first top square is an opponent piece
		byte firstBitIndex = findBitIndexInDirection(direction, bitIndex);
		if ((firstBitIndex == -1) || ((opponentBitboard & (1L << firstBitIndex)) == 0)) {
			return false;
		}

		// check if sequencially adjacent squares are all opponenet pieces, and a friend piece at the end
		byte otherBitIndex = findBitIndexInDirection(direction, firstBitIndex);
		while (otherBitIndex != -1) {
			Square squareState = getSquare(otherBitIndex);

			// meet a friend piece
			if (squareState == ((playerIsBlack)? Square.BLACK : Square.WHITE)) {
				return true;
			}

			// meet an empty square
			if (squareState == Square.NONE) {
				return false;
			}

			otherBitIndex = findBitIndexInDirection(direction, otherBitIndex);
		}

		return false;
	}

	/*
	 * Return -1 if index doesn't exist
	 */
	private byte findBitIndexInDirection(Direction direction, byte bitIndex) {
		switch (direction) {
			case TOP:
				bitIndex -= 8;

				if (bitIndex < 0) {
					bitIndex = -1;
				}
				break;
			case LEFT:
				bitIndex -= 1;

				if (((bitIndex + 1) % 8) == 0) {
					bitIndex = -1;
				}
				break;
			case RIGHT:
				bitIndex += 1;

				if ((bitIndex % 8) == 0) {
					bitIndex = -1;
				}
				break;
			case BOTTOM:
				bitIndex += 8;

				if (bitIndex >= 64) {
					bitIndex = -1;
				}
				break;
			case TOP_LEFT:
				bitIndex -= 9;

				if ((bitIndex < 0) || (((bitIndex + 1) % 8) == 0)) {
					bitIndex = -1;
				}
				break;
			case TOP_RIGHT:
				bitIndex -= 7;

				if ((bitIndex < 0) || ((bitIndex % 8) == 0)) {
					bitIndex = -1;
				}
				break;
			case BOTTOM_LEFT:
				bitIndex += 7;

				if ((bitIndex >= 64) || (((bitIndex + 1) % 8) == 0)) {
					bitIndex = -1;
				}
				break;
			case BOTTOM_RIGHT:
				bitIndex += 9;

				if ((bitIndex >= 64) || ((bitIndex % 8) == 0)) {
					bitIndex = -1;
				}
				break;
		}

		return bitIndex;
	}

	public static void printBitboard(long bitboard) {
		for (int i = 0; i < 8; ++i) {
			for (int j = 0; j < 8; ++j) {
				System.out.print((((bitboard & 1) == 1)? "1" : "0") + ((j != 7)? " " : "\n"));
				bitboard >>>= 1;
			}
		}
	}

	private long getBitboardNeighbors(long bitboard) {
		long leftNeighbors = getBitboardLeftNeighbors(bitboard);
		long rightNeighbors = getBitboardRightNeighbors(bitboard);

		return (~bitboard) & (getBitboardTopNeighbors(bitboard) |
				leftNeighbors | rightNeighbors |
				getBitboardBottomNeighbors(bitboard) |
				getBitboardTopNeighbors(leftNeighbors) |
				getBitboardTopNeighbors(rightNeighbors) |
				getBitboardBottomNeighbors(leftNeighbors) |
				getBitboardBottomNeighbors(rightNeighbors));
	}

	private long getBitboardTopNeighbors(long bitboard) {
		return bitboard >>> 8;
	}

	private long getBitboardLeftNeighbors(long bitboard) {
		return (bitboard >>> 1) & 0b0111111101111111011111110111111101111111011111110111111101111111L;
	}

	private long getBitboardRightNeighbors(long bitboard) {
		return (bitboard << 1) & 0b1111111011111110111111101111111011111110111111101111111011111110L;
	}

	private long getBitboardBottomNeighbors(long bitboard) {
		return bitboard << 8;
	}
}
