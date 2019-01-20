package io.github.jyzeng17.othello;

// for mapping board and its heuristic value
public class Result {

	private Board board;
	private int heuristicValue;

	public Result(Board board, int heuristicValue) {
		this.board = board;
		this.heuristicValue = heuristicValue;
	}

	public Board getBoard() {
		return board;
	}

	public int getHeuristicValue() {
		return heuristicValue;
	}
}
