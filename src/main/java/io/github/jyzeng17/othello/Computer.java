package io.github.jyzeng17.othello;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Random;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
//import java.util.HashMap;

import java.util.List;

public class Computer {

	public static final Logger logger = LogManager.getLogger();

	private static final Random random = new Random();

	private static boolean timeIsUp = false;

	//private static final int SEARCH_PARTITION_RANGE = 20;

	private static final int TIME_TO_THINK = 30; // seconds

	// Alpha-Beta arguments
	private static final int SEARCH_DEPTH = 8;
	private static final int INITIAL_ALPHA_VALUE = -10000;
	private static final int INITIAL_BETA_VALUE = 10000;
	private static final int ACTUAL_SCORE_WEIGHT = 1;
	private static final int NUMBER_OF_MOVES_WEIGHT = 10;
	private static final int CORNER_POSITION_WEIGHT = 100;
	private static final int AROUND_CORNER_POSITION_WEIGHT = -50;

	// Enhanced Alpha-Beta variables
	//private static final int SEARCH_WINDOW_THRESHOLD = 0;
	//private int lastAlpha;
	//private int lastBeta;
	//private HashMap<Byte, Byte> pvHashMap;

	private static final int UCT_SIMULATION_TIMES = 30;

	UCTDataBase uctDB;

	public Computer() {
		//lastAlpha = 0;
		//lastBeta = 0;
		//pvHashMap = new HashMap<Byte, Byte>();
		//uctDB = new UCTDataBase();
	}

	public void saveLastestDB() {
		// 
	}

	public byte generateMoves(Board board) {
		// fixed first move
		//if ((board.getBlackBitboard() == ((1L << 28) + (1L << 35))) && (board.getWhiteBitboard() == ((1L << 27) + (1L << 36)))) {
		//	return 19;
		//}

		// only one legal next move situation
		if (board.getNextMovesArrayList().size() == 1) {
			return board.getNextMovesArrayList().get(0);
		}

		// time threshold
		Thread timerThread = new Thread(() -> {
			try {

				System.out.print("[Othello] ");

				for (int i = 0; i < TIME_TO_THINK; ++i) {
					System.out.print(TIME_TO_THINK - i + "... ");
					Thread.sleep(1000);
				}

				System.out.println("0!");

				timeIsUp = true;
			} catch (InterruptedException e) {
				System.out.println("done!");
			}
		});

		timeIsUp = false;
		timerThread.start();

		byte nextMoveBitIndex = -1;

		//int gameTreeDepth = board.getGameTreeDepth();

		//if (gameTreeDepth <= 5) {
		//	byte uctDBResult = uctDB.get(board);

		//	if (uctDBResult == (byte)-1) { // database miss
		//		nextMoveBitIndex = generateMovesByUCT(board);

		//		// record the result to the DB
		//		uctDB.put(board, nextMoveBitIndex);
		//	} else { // database hit
		//		logger.info("UCT database hit!");

		//		nextMoveBitIndex = uctDBResult;
		//	}
		//} else if (gameTreeDepth <= 50) {
		//	nextMoveBitIndex = generateMovesByUCT(board);
		//} else {
		//	nextMoveBitIndex = generateMovesByAlphaBeta(board);
		//}

		//if (gameTreeDepth <= 50) {
		//	nextMoveBitIndex = generateMovesByUCT(board);
		//} else {
		//	nextMoveBitIndex = generateMovesByAlphaBeta(board);
		//}

			nextMoveBitIndex = generateMovesByAlphaBeta(board);
		//nextMoveBitIndex = generateMovesByAlphaBeta(board);

		//if (gameTreeDepth < SEARCH_PARTITION_RANGE) {
		//	nextMoveBitIndex = generateMovesByAlphaBeta(board);
		//} else if (gameTreeDepth < 2 * SEARCH_PARTITION_RANGE) {
		//	nextMoveBitIndex = generateMovesByUCB(board);
		//} else {
		//	nextMoveBitIndex = generateMovesByAlphaBeta(board);
		//}

		if (!timeIsUp) {
			timerThread.interrupt();
			try {
				timerThread.join();
			} catch (Exception e) {
				logger.error(e);
			}
		}

		return nextMoveBitIndex;
	}

	private static byte generateRandomMoves(Board board) {
		ArrayList<Byte> nextMovesArrayList = board.getNextMovesArrayList();

		return nextMovesArrayList.get(random.nextInt(nextMovesArrayList.size())).byteValue();
	}

	private static byte generateMovesByUCB(Board board) {
		final int SIMULATION_TIMES = 10000;

		ArrayList<Byte> nextMovesIndices = board.getNextMovesArrayList();
		ArrayList<Board> childrenArrayList = new ArrayList<Board>();

		// create a list of children
		for (Byte b : nextMovesIndices) {
			childrenArrayList.add(board.applyNextMove(b.byteValue()));
		}

		int simulationTimesPerChild = SIMULATION_TIMES / childrenArrayList.size();

		long startTime = System.nanoTime();

		// for each child p perform x simulations to obtain basic UCB
		for (Board b : childrenArrayList) {
			performSimulation(b, simulationTimesPerChild);

			b.updateSiblingsSimulationTimes(childrenArrayList.size() * simulationTimesPerChild);
		}

		long elapsedTime = (System.nanoTime() - startTime) / 1000000;

		logger.info(nextMovesIndices.size() + " * " + simulationTimesPerChild +
				" basic simulations finished, elapsed time = " + elapsedTime + " ms");

		PriorityQueue<Board> childrenPriorityQueue = new PriorityQueue<Board>(childrenArrayList);

		int extraSimulationTimes = 0;
		final int EXTRA_UCB_SIMULATION_TIMES = 100;
		// while there's still time
		while (!timeIsUp) {
			// pick a child with largest UCB score (where PQ works)
			Board largestUCBChild = childrenPriorityQueue.poll();

			// perform y simulation for it
			performSimulation(largestUCBChild, EXTRA_UCB_SIMULATION_TIMES);

			// update the UCB score of it
			for (Board bs : childrenPriorityQueue) {
				bs.updateSiblingsSimulationTimes(EXTRA_UCB_SIMULATION_TIMES);
			}

			childrenPriorityQueue.offer(largestUCBChild);

			++extraSimulationTimes;
		}

		elapsedTime = (System.nanoTime() - startTime) / 1000000;

		logger.info(extraSimulationTimes * EXTRA_UCB_SIMULATION_TIMES +
				" UCB simulations finished, elapsed time = " + elapsedTime + " ms");

		// pick the child with largest winning rate
		float maxWinngRate = 0;
		Board finalNextBoard = null;
		for (Board bs : childrenPriorityQueue) {
			float childsWinngRate = bs.getWinningRate();

			logger.info("child's winning rate = " + childsWinngRate +
					", winngTimes/totalSimulationTimes = "
					+ bs.getWinningTimes() + " / " + bs.getTotalSimulationTimes());

			if (childsWinngRate >= maxWinngRate) {
				maxWinngRate = childsWinngRate;
				finalNextBoard = bs;
			}
		}

		logger.info("best child's winning rate = " + maxWinngRate +
				", winngTimes/totalSimulationTimes = " +
				finalNextBoard.getWinningTimes() + " / " + finalNextBoard.getTotalSimulationTimes());

		// calculate the best child's last move (can save in Board class alternatively)
		long lastMoveBitboard = ((finalNextBoard.getBlackBitboard() | finalNextBoard.getWhiteBitboard()) ^ (board.getBlackBitboard() | board.getWhiteBitboard()));
		byte nextMoveBitIndex = 0;
		long mask = 1L;
		while (true) {
			if ((lastMoveBitboard & mask) != 0) {
				// check if nextMoveBitIndex is legal
				if (!nextMovesIndices.contains(Byte.valueOf(nextMoveBitIndex))) {
					logger.fatal("nextMoveBitIndex generated by Computer is not legal: " + nextMoveBitIndex);
					logger.fatal("print finalNextBoard");
					finalNextBoard.printBoard();
					logger.fatal("print lastMoveBitboard");
					Board.printBitboard(lastMoveBitboard);
					System.exit(1);
				}
				
				return nextMoveBitIndex;
			}
			++nextMoveBitIndex;
			mask <<= 1;
		}
	}

	private static byte generateMovesByUCT(Board board) {
		// initialize UCT
		UCTNode rootUCTNode = new UCTNode(board, null);

		// while there's still time
		while (!timeIsUp) {
			// selection
			// find the PV path from root to a leaf node in the UCT
			UCTNode currentUCTNode = rootUCTNode;
			while (currentUCTNode.hasChildren()) {
				currentUCTNode = currentUCTNode.getBestChild();
			}

			// expansion
			// when actual game leaf met, re-simulate its parent
			if (currentUCTNode.getBoard().isGameEnd()) {
				currentUCTNode = currentUCTNode.getParent();
			}

			// find the children of the leaf node and add it to the UCT
			ArrayList<UCTNode> childrenArrayList = (currentUCTNode.getChildren().size() == 0)? currentUCTNode.getAndAddChildren() : currentUCTNode.getChildren();

			// record children's original winning times
			int childrenOriginalWinningTimes = 0;
			for (UCTNode u : childrenArrayList) {
				childrenOriginalWinningTimes += u.getBoard().getWinningTimes();
			}
			
			// simulation
			// perform a fixed number of simulations for the expanded children of the leaf node
			for (UCTNode u : childrenArrayList) {
				performSimulation(u.getBoard(), UCT_SIMULATION_TIMES);

				u.getBoard().updateSiblingsSimulationTimes(childrenArrayList.size() * UCT_SIMULATION_TIMES);
			}

			// propagation
			// update the UCB values of all nodes in the UCT
			// recursively update parent and parent's sblings' UCB
			int increasedTotalSimulationTimes = childrenArrayList.size() * UCT_SIMULATION_TIMES;
			int childrenIncreasedWinningTimes = 0;
			for (UCTNode u : childrenArrayList) {
				childrenIncreasedWinningTimes += u.getBoard().getWinningTimes();
			}
			childrenIncreasedWinningTimes -= childrenOriginalWinningTimes;

			// children's lose is root's win
			int increasedWinningTimes = increasedTotalSimulationTimes - childrenIncreasedWinningTimes;
			while (true) {
				currentUCTNode.getBoard().updateWinningTimes(increasedWinningTimes);
				currentUCTNode.getBoard().updateTotalSimulationTimes(increasedTotalSimulationTimes);

				// reach root node
				if (!currentUCTNode.hasParent()) {
					currentUCTNode.getBoard().updateSiblingsSimulationTimes(increasedTotalSimulationTimes);
					break;
				}

				// update currentUCTNode and its siblings' sibingsSimulationTimes
				currentUCTNode = currentUCTNode.getParent();

				childrenArrayList = currentUCTNode.getChildren();
				for (UCTNode u : childrenArrayList) {
					u.getBoard().updateSiblingsSimulationTimes(increasedTotalSimulationTimes);
				}

				// minimax
				increasedWinningTimes = increasedTotalSimulationTimes - increasedWinningTimes;
			}
		}

		// debug
		int level = 0;
		UCTNode currentUCTNode = rootUCTNode;
		while (currentUCTNode.hasChildren()) {
			logger.debug("Tree level: " + level);
			logger.debug("parent");
			logger.debug("winning times = " + currentUCTNode.getBoard().getWinningTimes() +
					", total simulation times = " + currentUCTNode.getBoard().getTotalSimulationTimes() +
					", siblings simulation times = " + currentUCTNode.getBoard().getSiblingsSimulationTimes());

			int node = 0;
			ArrayList<UCTNode> children = currentUCTNode.getChildren();
			for (UCTNode u : children) {
				logger.debug("child " + node);
				logger.debug("winning times = " + u.getBoard().getWinningTimes() +
						", total simulation times = " + u.getBoard().getTotalSimulationTimes() +
						", siblings simulation times = " + u.getBoard().getSiblingsSimulationTimes());
				++node;
			}
			currentUCTNode = currentUCTNode.getBestChild();
			++level;
		}

		// pick the child with lowest winning rate
		ArrayList<UCTNode> childrenArrayList = rootUCTNode.getChildren();
		float minWinngRate = 1;
		Board finalNextBoard = null;
		for (UCTNode u : childrenArrayList) {
			float childsWinngRate = u.getBoard().getWinningRate();

			logger.info("child's winning rate = " + childsWinngRate +
					", winngTimes/totalSimulationTimes = " +
					u.getBoard().getWinningTimes() + " / " + u.getBoard().getTotalSimulationTimes());

			if (childsWinngRate <= minWinngRate) {
				minWinngRate = childsWinngRate;
				finalNextBoard = u.getBoard();
			}
		}

		logger.info("best child's winning rate = " + minWinngRate +
				", winngTimes/totalSimulationTimes = " +
				finalNextBoard.getWinningTimes() + " / " + finalNextBoard.getTotalSimulationTimes());

		// calculate the best child's last move (can save in Board class alternatively)
		long lastMoveBitboard = ((finalNextBoard.getBlackBitboard() | finalNextBoard.getWhiteBitboard()) ^ (board.getBlackBitboard() | board.getWhiteBitboard()));
		byte nextMoveBitIndex = 0;
		long mask = 1L;
		while (true) {
			if ((lastMoveBitboard & mask) != 0) {
				// check if nextMoveBitIndex is legal
				if (!board.getNextMovesArrayList().contains(Byte.valueOf(nextMoveBitIndex))) {
					logger.fatal("nextMoveBitIndex generated by Computer is not legal: " + nextMoveBitIndex);
					logger.fatal("print finalNextBoard");
					finalNextBoard.printBoard();
					logger.fatal("print lastMoveBitboard");
					Board.printBitboard(lastMoveBitboard);
					System.exit(1);
				}
				
				return nextMoveBitIndex;
			}
			++nextMoveBitIndex;
			mask <<= 1;
		}
	}

	private static byte generateMovesByAlphaBeta(Board board) {
		ArrayList<Byte> nextMovesIndices = board.getNextMovesArrayList();
		ArrayList<Board> childrenArrayList = new ArrayList<Board>();

		// create a list of children
		for (Byte b : nextMovesIndices) {
			childrenArrayList.add(board.applyNextMove(b.byteValue()));
		}

		// Parallel version
		ExecutorService exec = Executors.newCachedThreadPool();
		List<Callable<Result>> tasks = new ArrayList<Callable<Result>>();

		for (final Board b : childrenArrayList) {
			Callable<Result> c = new Callable<Result>() {
				@Override
				public Result call() throws Exception {
					int childsHeuristicValue = negaMaxAlphaBeta(b, INITIAL_ALPHA_VALUE, INITIAL_BETA_VALUE, SEARCH_DEPTH);
					//logger.info("search depth = " + searchDepth);

					return new Result(b, childsHeuristicValue);
				}
			};
			tasks.add(c);
		}

		int lowestChildHeuristicValue = 10000;
		Board bestChild = null;
		try {
			List<Future<Result>> results = exec.invokeAll(tasks);

			// pick the child with highest alpha beta value as next move
			for (Future<Result> f : results) {
				int childsHeuristicValue = f.get().getHeuristicValue();

				logger.info("child's heuristic value = " + childsHeuristicValue);

				if (childsHeuristicValue < lowestChildHeuristicValue) {
					lowestChildHeuristicValue = childsHeuristicValue;
					bestChild = f.get().getBoard();
				}

			}
		} catch (Exception e) {
			logger.error("while doing parallel alpha beta, " + e);
			System.exit(1);
		}

		logger.info("lowest child's heuristic value = " + lowestChildHeuristicValue);

		// calculate the best child's last move (can save in Board class alternatively)
		long lastMoveBitboard = ((bestChild.getBlackBitboard() | bestChild.getWhiteBitboard()) ^ (board.getBlackBitboard() | board.getWhiteBitboard()));
		byte nextMoveBitIndex = 0;
		long mask = 1L;
		while (true) {
			if ((lastMoveBitboard & mask) != 0) {
				// check if nextMoveBitIndex is legal
				if (!board.getNextMovesArrayList().contains(Byte.valueOf(nextMoveBitIndex))) {
					logger.fatal("nextMoveBitIndex generated by Computer is not legal: " + nextMoveBitIndex);
					logger.fatal("print finalNextBoard");
					bestChild.printBoard();
					logger.fatal("print lastMoveBitboard");
					Board.printBitboard(lastMoveBitboard);
					System.exit(1);
				}
				
				return nextMoveBitIndex;
			}
			++nextMoveBitIndex;
			mask <<= 1;
		}
	}

	private static int negaMaxAlphaBeta(Board board, int alpha, int beta, int searchDepth) {
		// determine successor positions p1, ..., pb (not necessary)
		ArrayList<Byte> nextMovesIndices = board.getNextMovesArrayList();
		int m = 0;

		// or other knowledge
		if (board.isGameEnd()) { // if it's a terminal node
			// get black and white's scores
			long blackBitboard = board.getBlackBitboard();
			long whiteBitboard = board.getWhiteBitboard();
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

			// convert it to heuristic value
			if (board.getPlayerIsBlack()) {
				return (blackFinalScore - whiteFinalScore) * ACTUAL_SCORE_WEIGHT;
			} else {
				return (whiteFinalScore - blackFinalScore) * ACTUAL_SCORE_WEIGHT;
			}
		} else if ((searchDepth == 0) || (timeIsUp)) { // or iterative deepening limit met / time's up
			// consider the number of moves can apply
			int numberOfMovesHeuristicValue = nextMovesIndices.size() * NUMBER_OF_MOVES_WEIGHT;

			// consider the wieght of important position (the corners)
			int cornerPositionHeuristicValue = 0;
			int positionAroundCornerHeuristicValue = 0;
			// get the number of the player's pieces in the corners and its surroundings
			long playersBoard = (board.getPlayerIsBlack())? board.getBlackBitboard() : board.getWhiteBitboard();
			long opponentsBoard = (!board.getPlayerIsBlack())? board.getBlackBitboard() : board.getWhiteBitboard();

			long topLeftCorner = 1L << 0;
			if ((playersBoard & topLeftCorner) != 0) {
				++cornerPositionHeuristicValue;
			} else if ((opponentsBoard & topLeftCorner) == 0) { // top left corner is empty
				if ((playersBoard & (1L << 1)) != 0) {
					++positionAroundCornerHeuristicValue;
				}

				if ((playersBoard & (1L << 8)) != 0) {
					++positionAroundCornerHeuristicValue;
				}

				if ((playersBoard & (1L << 9)) != 0) {
					++positionAroundCornerHeuristicValue;
				}
			}

			long topRightCorner = 1L << 7;
			if ((playersBoard & topRightCorner) != 0) {
				++cornerPositionHeuristicValue;
			} else if ((opponentsBoard & topRightCorner) == 0) { // top right corner is empty
				if ((playersBoard & (1L << 6)) != 0) {
					++positionAroundCornerHeuristicValue;
				}

				if ((playersBoard & (1L << 15)) != 0) {
					++positionAroundCornerHeuristicValue;
				}

				if ((playersBoard & (1L << 14)) != 0) {
					++positionAroundCornerHeuristicValue;
				}
			}

			long bottomLeftCorner = 1L << 56;
			if ((playersBoard & bottomLeftCorner) != 0) {
				++cornerPositionHeuristicValue;
			} else if ((opponentsBoard & bottomLeftCorner) == 0) { // bottom left corner is empty
				if ((playersBoard & (1L << 57)) != 0) {
					++positionAroundCornerHeuristicValue;
				}

				if ((playersBoard & (1L << 48)) != 0) {
					++positionAroundCornerHeuristicValue;
				}

				if ((playersBoard & (1L << 49)) != 0) {
					++positionAroundCornerHeuristicValue;
				}
			}

			long bottomRightCorner = 1L << 63;
			if ((playersBoard & bottomRightCorner) != 0) {
				++cornerPositionHeuristicValue;
			} else if ((opponentsBoard & bottomRightCorner) == 0) { // bottom right corner is empty
				if ((playersBoard & (1L << 62)) != 0) {
					++positionAroundCornerHeuristicValue;
				}

				if ((playersBoard & (1L << 55)) != 0) {
					++positionAroundCornerHeuristicValue;
				}

				if ((playersBoard & (1L << 54)) != 0) {
					++positionAroundCornerHeuristicValue;
				}
			}

			cornerPositionHeuristicValue *= CORNER_POSITION_WEIGHT;
			positionAroundCornerHeuristicValue *= AROUND_CORNER_POSITION_WEIGHT;

			return numberOfMovesHeuristicValue + cornerPositionHeuristicValue + positionAroundCornerHeuristicValue;
		} else {
			ArrayList<Board> childrenArrayList = new ArrayList<Board>();

			// create a list of children
			if (nextMovesIndices.size() > 0) {
				for (Byte b : nextMovesIndices) {
					childrenArrayList.add(board.applyNextMove(b.byteValue()));
				}
			} else { // when no next moves
				Board onlyChild = new Board(!board.getPlayerIsBlack(), board.getBlackBitboard(), board.getWhiteBitboard());
				childrenArrayList.add(onlyChild);
			}

			// m = alpha (hard initial value)
			m = alpha;
			for (Board b : childrenArrayList) {
				// t = - F2(pi, -beta, -m)
				int t = -negaMaxAlphaBeta(b, -beta, -m, searchDepth - 1);
				// if (t > m)
				if (t > m) {
					// m = t (the returned value is "used")
					m = t;
				}
				// if m >= beta
				if (m >= beta) {
					// return m (cut-off)
					return m;
				}
			}
		}
		return m;
	}

	// goal: dynamic search window, PV path move ordering, iterative deepening
	//private byte generateMovesByEnhancedAlphaBeta(Board board) {
	//	int currentDepth = 1;

	//	AlphaBetaResult searchResult = null;
	//	while (!timeIsUp) {
	//		// decide dynamic search window range
	//		int alpha = lastAlpha - SEARCH_WINDOW_THRESHOLD;
	//		int beta = lastBeta + SEARCH_WINDOW_THRESHOLD;

	//		// do the search
	//		searchResult = enhancedNegaMaxAlphaBeta(board, alpha, beta, currentDepth);

	//		// check if the search has reached all leaves
	//		// if (bestChildNode.isAllLeavesSearched) {
	//		// break;
	//		// }

	//		++currentDepth;
	//	}

	//	// calculate the byte index of the next move
	//	return searchResult.getLastMove();
	//}

	//private AlphaBetaResult enhancedNegaMaxAlphaBeta(Board board, int alpha, int beta, int searchDepth) {
	//	AlphaBetaResult finalSearchResult = new AlphaBetaResult();

	//	ArrayList<Byte> nextMovesIndices = board.getNextMovesArrayList();

	//	int m = 0;

	//	if (board.isGameEnd()) { // if it's a terminal node
	//		// get black and white's scores
	//		long blackBitboard = board.getBlackBitboard();
	//		long whiteBitboard = board.getWhiteBitboard();
	//		byte blackFinalScore = 0;
	//		byte whiteFinalScore = 0;
	//		long mask = 1L;

	//		for (int i = 0; i < 64; ++i) {
	//			if ((blackBitboard & (mask << i)) != 0) {
	//				++blackFinalScore;
	//				continue;
	//			}

	//			if ((whiteBitboard & (mask << i)) != 0) {
	//				++whiteFinalScore;
	//			}
	//		}

	//		// convert it to heuristic value
	//		finalSearchResult.setHeuristicValue(((board.getPlayerIsBlack())? (blackFinalScore - whiteFinalScore) : (whiteFinalScore - blackFinalScore)) * ACTUAL_SCORE_WEIGHT);

	//		return finalSearchResult;
	//	} else if ((searchDepth == 0) || (timeIsUp)) { // or iterative deepening limit met / time's up
	//		// consider the number of moves can apply
	//		int numberOfMovesHeuristicValue = nextMovesIndices.size() * NUMBER_OF_MOVES_WEIGHT;

	//		// consider the wieght of important position (the corners)
	//		int cornerPositionHeuristicValue = 0;
	//		int positionAroundCornerHeuristicValue = 0;
	//		// get the number of the player's pieces in the corners and its surroundings
	//		long playersBoard = (board.getPlayerIsBlack())? board.getBlackBitboard() : board.getWhiteBitboard();
	//		long opponentsBoard = (!board.getPlayerIsBlack())? board.getBlackBitboard() : board.getWhiteBitboard();

	//		long topLeftCorner = 1L << 0;
	//		if ((playersBoard & topLeftCorner) != 0) {
	//			++cornerPositionHeuristicValue;
	//		} else if ((opponentsBoard & topLeftCorner) == 0) { // top left corner is empty
	//			if ((playersBoard & (1L << 1)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}

	//			if ((playersBoard & (1L << 8)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}

	//			if ((playersBoard & (1L << 9)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}
	//		}

	//		long topRightCorner = 1L << 7;
	//		if ((playersBoard & topRightCorner) != 0) {
	//			++cornerPositionHeuristicValue;
	//		} else if ((opponentsBoard & topRightCorner) == 0) { // top right corner is empty
	//			if ((playersBoard & (1L << 6)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}

	//			if ((playersBoard & (1L << 15)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}

	//			if ((playersBoard & (1L << 14)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}
	//		}

	//		long bottomLeftCorner = 1L << 56;
	//		if ((playersBoard & bottomLeftCorner) != 0) {
	//			++cornerPositionHeuristicValue;
	//		} else if ((opponentsBoard & bottomLeftCorner) == 0) { // bottom left corner is empty
	//			if ((playersBoard & (1L << 57)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}

	//			if ((playersBoard & (1L << 48)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}

	//			if ((playersBoard & (1L << 49)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}
	//		}

	//		long bottomRightCorner = 1L << 63;
	//		if ((playersBoard & bottomRightCorner) != 0) {
	//			++cornerPositionHeuristicValue;
	//		} else if ((opponentsBoard & bottomRightCorner) == 0) { // bottom right corner is empty
	//			if ((playersBoard & (1L << 62)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}

	//			if ((playersBoard & (1L << 55)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}

	//			if ((playersBoard & (1L << 54)) != 0) {
	//				++positionAroundCornerHeuristicValue;
	//			}
	//		}

	//		cornerPositionHeuristicValue *= CORNER_POSITION_WEIGHT;
	//		positionAroundCornerHeuristicValue *= AROUND_CORNER_POSITION_WEIGHT;

	//		finalSearchResult.setHeuristicValue(numberOfMovesHeuristicValue + cornerPositionHeuristicValue + positionAroundCornerHeuristicValue);

	//		return finalSearchResult;
	//	} else {
	//		// hard initial value
	//		m = alpha;

	//		// first search from the current PV path's leaf
	//		byte gameTreeDepth = (byte)(board.getGameTreeDepth());

	//		Byte pvMoveIndex = pvHashMap.getOrDefault(new Byte(gameTreeDepth), null);

	//		if (pvMoveIndex != null) {
	//			logger.info("Hit: pvHashMap at depth " + gameTreeDepth);

	//			Board pvChild = board.applyNextMove(pvMoveIndex.byteValue());

	//			// search
	//			AlphaBetaResult childSearchResult = enhancedNegaMaxAlphaBeta(pvChild, -beta, -m, searchDepth - 1);

	//			int t = childSearchResult.getNegativeHeuristicValue();

	//			if (t > m) {
	//				// the returned value is "used"
	//				m = t;

	//				//finalSearchResult.updateMetadata(pvChild, );
	//			}

	//			if (m >= beta) {
	//				// cut-off
	//				return m;
	//			}
	//		}

	//		// create a list of children
	//		ArrayList<Board> childrenArrayList = new ArrayList<Board>();
	//		if (nextMovesIndices.size() > 0) {
	//			// remove pvMoveIndex
	//			if (pvMoveIndex != null) {
	//				nextMovesIndices.remove(pvMoveIndex);
	//			}

	//			for (Byte b : nextMovesIndices) {
	//				childrenArrayList.add(board.applyNextMove(b.byteValue()));
	//			}
	//		} else { // when no next moves
	//			Board onlyChild = new Board(!board.getPlayerIsBlack(), board.getBlackBitboard(), board.getWhiteBitboard());
	//			childrenArrayList.add(onlyChild);
	//		}


	//		for (Board b : childrenArrayList) {
	//			AlphaBetaResult childSearchResult = enhancedNegaMaxAlphaBeta(b, -beta, -m, searchDepth - 1);

	//			int t = childSearchResult.getNegativeHeuristicValue();

	//			if (t > m) {
	//				// the returned value is "used"
	//				m = t;
	//			}

	//			if (m >= beta) {
	//				// cut-off
	//				return m;
	//			}
	//		}
	//	}

	//	return m;
	//}

	private static void performSimulation(Board rootBoard, int simulationTimes) {
		Player rootPlayer = rootBoard.getPlayer();
		int extraWinngTimes = 0;
		Board currentBoard = null;
		boolean hasSkippedOnce = false; // for debugging

		// for loop do simulationTimes times
		for (int i = 0; i < simulationTimes; ++i) {
			currentBoard = rootBoard;
			// while loop until game ends
			while (!currentBoard.isGameEnd()) {
				// get all children
				ArrayList<Byte> nextMovesIndices = currentBoard.getNextMovesArrayList();
				// random pick one
				// check if has next move
				if (nextMovesIndices.size() > 0) {
					currentBoard = currentBoard.applyNextMove(nextMovesIndices.get(random.nextInt(nextMovesIndices.size())).byteValue());
					hasSkippedOnce = false;
				} else {
					if (hasSkippedOnce) {
						logger.fatal("skip twice");
						currentBoard.printBoard();
						System.exit(1);
					} else {
						currentBoard = new Board(!(currentBoard.getPlayerIsBlack()), currentBoard.getBlackBitboard(), currentBoard.getWhiteBitboard());
						hasSkippedOnce = true;
					}
				}
			}

			// record the simulation result
			// rootPlayer is actually the opponent player
			// consider draw case as victory for laziness
			if (currentBoard.getWinningPlayer() == rootPlayer) {
				++extraWinngTimes;
			}
		}

		// update its simulation times
		rootBoard.updateWinningTimes(extraWinngTimes);
		rootBoard.updateTotalSimulationTimes(simulationTimes);
	}
}
