package io.github.jyzeng17.othello;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.PriorityQueue;

public class BoardTest {

	@Test
	public void testOrderOfBoardPriorityQueue() {
		PriorityQueue<Board> boardPQ = new PriorityQueue<Board>();

		boolean playerIsBlack = true;
		long blackBitboard = (1L << 28) + (1L << 35);
		long whiteBitboard = (1L << 27) + (1L << 36);

		// adjust these properties to make board1's UCB greater than board2's UCB
		int winngTimes1 = 10;
		int winngTimes2 = 1;
		int totalSimulationTimes1 = 100;
		int totalSimulationTimes2 = 100;
		int siblingsSimulationTimes = 200;

		Board board1 = new Board(playerIsBlack, blackBitboard, whiteBitboard, winngTimes1, totalSimulationTimes1, siblingsSimulationTimes);
		Board board2 = new Board(playerIsBlack, blackBitboard, whiteBitboard, winngTimes2, totalSimulationTimes2, siblingsSimulationTimes);

		boardPQ.offer(board1);
		boardPQ.offer(board2);

		System.out.println("board1's : board2's UCB = " + board1.getUCB() + " : " + board2.getUCB());

		assertEquals(boardPQ.poll(), board1);
		//assertEquals(boardPQ.poll(), board2);
	}
}
