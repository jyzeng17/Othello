package io.github.jyzeng17.othello;

import java.io.Serializable;

public class UCTDataBaseKey implements Serializable {

	// for Serializable interface
	private static final long serialVersionUID = 1L;

	private boolean playerIsBlack;
	private long blackBitboard;
	private long whiteBitboard;

	public UCTDataBaseKey(Board board) {
		this.playerIsBlack = board.getPlayerIsBlack();
		this.blackBitboard = board.getBlackBitboard();
		this.whiteBitboard = board.getWhiteBitboard();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (this == o) {
			return true;
		}

		if (this.getClass() != o.getClass()) {
			return false;
		}

		UCTDataBaseKey other = (UCTDataBaseKey)o;

		if (other.getPlayerIsBlack() != playerIsBlack) {
			return false;
		}

		if (other.getBlackBitboard() != blackBitboard) {
			return false;
		}

		if (other.getWhiteBitboard() != whiteBitboard) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = 0;

		result += new Boolean(playerIsBlack).hashCode();
		result += new Long(blackBitboard).hashCode();
		result += new Long(whiteBitboard).hashCode();

		return result;
	}

	public boolean getPlayerIsBlack() {
		return playerIsBlack;
	}

	public long getBlackBitboard() {
		return blackBitboard;
	}

	public long getWhiteBitboard() {
		return whiteBitboard;
	}
}
