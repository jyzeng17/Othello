package io.github.jyzeng17.othello;

import java.io.Serializable;
import java.util.HashMap;

public class UCTDataBase implements Serializable {

	private static final long serialVersionUID = 2L;

	private HashMap<UCTDataBaseKey, Byte> hashMap;

	public UCTDataBase() {
		hashMap = new HashMap<UCTDataBaseKey, Byte>();

		// load the old records
	}

	public byte get(Board board) {
		return (hashMap.getOrDefault(new UCTDataBaseKey(board), new Byte((byte)-1))).byteValue();
	}

	public void put(Board board, byte nextMoveBitIndex) {
		hashMap.put(new UCTDataBaseKey(board), new Byte(nextMoveBitIndex));
	}
}
