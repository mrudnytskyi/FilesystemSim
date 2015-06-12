package filesystem;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Class for representing file descriptor.
 * 
 * @author Mir4ik
 * @version 0.1 14.11.2013
 * @see java.io.File
 */
public class FileDescriptor implements Serializable {

	private static final long serialVersionUID = 7017738593299405382L;
	
	// maximum blocks count in file descriptor
	// data block (1 KB) * maximum blocks count (1024) = maximum file size (1 MB)
	public static final transient int MAX_BLOCKS_COUNT = 1024;
	
	// maximum links on these file descriptor
	public static final transient int MAX_LINKS_COUNT = 8;
	
	// maximum count of files in file system (1 block = 1 file as minimum) 
	public static final transient int MAX_DESCRIPTORS_COUNT = 
		FileSystem.MAX_BLOCKS_COUNT;
	
	public static final transient int WRONG_DESCRIPTOR = -3;
	
	private static final String[] MONTH = {"January", "February", "March", 
		"April", "May", "June", "July", "August", "September", "October", 
		"November", "December"};

	private long uid;
	
	private boolean directory;
	
	private List<Integer> blocks = new LinkedList<Integer>();
	
	private int hardLinksCount = 1;

	public FileDescriptor(boolean directory) {
		uid = createUniqueId();
		this.directory = directory;
	}
	
	public long getUid() {
		return uid;
	}
	
	public boolean addBlock(int number) {
		if (blocks.size() < MAX_BLOCKS_COUNT) {
			return blocks.add(number);
		}
		return false;
	}
	
	public void removeBlock(int number) {
		blocks.remove(new Integer(number));
	}
	
	public List<Integer> getBlocks() {
		return blocks;
	}
	
	public int blocksCount() {
		return blocks.size();
	}
	
	public boolean isEmpty() {
		return blocks.size() == 0;
	}

	public boolean incrementHardLinksCount() {
		if (hardLinksCount == MAX_LINKS_COUNT) {
			return false;
		}
		hardLinksCount++;
		return true;
	}
	
	public void decrementHardLinksCount() {
		hardLinksCount--;
		if (hardLinksCount < 0) {
			hardLinksCount = 0;
		}
	}
	
	public int getHardlinksCount() {
		return hardLinksCount;
	}
	
	public boolean isNoHardLinksCount() {
		return hardLinksCount == 0;
	}
	
	public boolean isDirectory() {
		return directory;
	}
	
	public boolean isFile() {
		return !directory;
	}

	@Override
	public int hashCode() {
		int result = 3;
		return (int) (37*result + blocks.hashCode() + uid + hardLinksCount + 
			(directory ? 1 : 0));
	}
	
	@Override
	public String toString() {
		return "File descriptor " + uid + " with type " + 
			(directory ? "directory" : "file") + " and blocks " + blocks +
			" and hard links count " + hardLinksCount + " and creating data " + 
			convertDate(uid);
	}
	
	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		} else {
			if (object instanceof FileDescriptor) {
				FileDescriptor casted = (FileDescriptor) object;
				if (uid == casted.uid) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static long createUniqueId() {
		return System.currentTimeMillis();
	}
	
	private static String convertDate(long time) {
		if (time < 0) {
			throw new IllegalArgumentException("Time can not be negative!");
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(time));
		StringBuffer sb = new StringBuffer();
		sb.append(calendar.get(Calendar.HOUR_OF_DAY));
		sb.append(':');
		sb.append(calendar.get(Calendar.MINUTE));
		sb.append(':');
		sb.append(calendar.get(Calendar.SECOND));
		sb.append(',');
		sb.append(' ');
		sb.append(calendar.get(Calendar.DAY_OF_MONTH));
		sb.append(' ');
		sb.append(MONTH[calendar.get(Calendar.MONTH)]);
		sb.append(' ');
		sb.append(calendar.get(Calendar.YEAR));
		return sb.toString();
	}
}