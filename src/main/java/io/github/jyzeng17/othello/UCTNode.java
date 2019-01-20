package io.github.jyzeng17.othello;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class UCTNode implements Comparable<UCTNode> {

	private Board board;
	private UCTNode parent;
	private ArrayList<UCTNode> childrenArrayList;

	public UCTNode(Board board, UCTNode parent) {
		this.board = board;
		this.parent = parent;
		this.childrenArrayList = new ArrayList<UCTNode>();
	}

	// Comparable interface's method (might have to override equals() as well if something's wrong)
	@Override
	public int compareTo(UCTNode comparedUCTNode) {
		int result;
		float thisUCB = this.getBoard().getLosingUCB();
		float comparedUCB = comparedUCTNode.getBoard().getLosingUCB();

		// avoid the error caused by directly casting float to integer as result
		if (comparedUCB > thisUCB) {
			result = 1;
		} else if (comparedUCB < thisUCB) {
			result = -1;
		} else {
			result = 0;
		}

		// a negative integer, zero, or a positive integer
		// as this object is less than, equal to, or greater than the specified object
		return result;
	}
	
	public boolean hasParent() {
		return (parent != null);
	}

	public boolean hasChildren() {
		return (childrenArrayList.size() > 0);
	}

	public UCTNode getBestChild() {
		PriorityQueue<UCTNode> childrenPriorityQueue = new PriorityQueue<UCTNode>(childrenArrayList);

		return childrenPriorityQueue.poll();
	}

	public ArrayList<UCTNode> getAndAddChildren() {
		ArrayList<Byte> nextMovesIndices = board.getNextMovesArrayList();

		if (nextMovesIndices.size() != 0) {
			// create a list of children
			for (Byte b : nextMovesIndices) {
				childrenArrayList.add(new UCTNode(board.applyNextMove(b.byteValue()), this));
			}
		} else {
			Board onlyChild = new Board(!board.getPlayerIsBlack(), board.getBlackBitboard(), board.getWhiteBitboard());
			childrenArrayList.add(new UCTNode(onlyChild, this));
		}

		return childrenArrayList;
	}

	public Board getBoard() {
		return board;
	}

	public UCTNode getParent() {
		return parent;
	}

	public ArrayList<UCTNode> getChildren() {
		return childrenArrayList;
	}
}
