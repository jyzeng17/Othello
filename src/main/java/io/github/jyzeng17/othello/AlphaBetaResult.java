package io.github.jyzeng17.othello;

public class AlphaBetaResult {

	// original value
	private int heuristicValue;

	// metadata
	private Board bestChild;
	private byte lastMove;

	private int alpha;
	private int beta;

	public AlphaBetaResult() {
		this(0, null, (byte)0, 0, 0);
	}

	public AlphaBetaResult(int heuristicValue, Board bestChild, byte lastMove, int alpha, int beta) {
		this.heuristicValue = heuristicValue;

		this.bestChild = bestChild;
		this.lastMove = lastMove;

		this.alpha = alpha;
		this.beta = beta;
	}

	public int getNegativeHeuristicValue() {
		return -heuristicValue;
	}

	public Board getBestChild() {
		return bestChild;
	}

	public byte getLastMove() {
		return lastMove;
	}

	public int getAlpha() {
		return alpha;
	}

	public int getBeta() {
		return beta;
	}

	public void setHeuristicValue(int heuristicValue) {
		this.heuristicValue = heuristicValue;
	}

	public void updateMetadata(Board bestChild, byte lastMove, int alpha, int beta) {
		this.bestChild = bestChild;
		this.lastMove = lastMove;
		this.alpha = alpha;
		this.beta = beta;
	}
}
