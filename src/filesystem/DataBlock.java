package filesystem;

import java.io.Serializable;

/**
 * Class represents each block in file system.
 * 
 * @author Mir4ik
 * @version 0.1 14.11.2013
 */
public class DataBlock implements Serializable {

	private static final long serialVersionUID = -4071794228306000939L;
	
	public static final transient int TRUNCATED_DATA = -4;
	
	public static final transient String ZERO = "0";
	
	// char (2 B) * block size (512) = data block (1 KB)
	public static final transient int BLOCK_SIZE = 512;

	private boolean used = false;
	
	private String data;
	
	public boolean isUsed() {
		return used;
	}
	
	public int addData(String data) {
		used = true;
		if (data.length() > BLOCK_SIZE) {
			data = data.substring(0, BLOCK_SIZE);
			return TRUNCATED_DATA;
		}
		this.data = data;
		return 0;
	}
	
	public void removeData() {
		used = false;
		data = null;
	}
	
	@Override
	public int hashCode() {
		int result = 1;
		return 37*result + data.hashCode() + (used ? 1 : 0);
	}
	
	@Override
	public String toString() {
		return data;
	}
	
	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		} else {
			if (object instanceof DataBlock) {
				DataBlock casted = (DataBlock) object;
				if (used == casted.used && data.equals(casted.data)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static int roundToBlockSize(int number) {
		if (number <= 0) {
			return 0;
		}
		int counter = 1;
		while (true) {
			if (counter*BLOCK_SIZE < number) {
				counter++;
			} else {
				return counter*BLOCK_SIZE;
			}
		}
	}
	
	public static String[] packData(String data) {
		if (data == null || data.isEmpty()) {
			return new String[0];
		}
		String[] dataArray = new String[data.length()/BLOCK_SIZE];
		for (int i = 0, j = 0; i < dataArray.length; i++, j += BLOCK_SIZE) {
			dataArray[i] = data.substring(j, j + BLOCK_SIZE);
		}
		return dataArray;
	}
}