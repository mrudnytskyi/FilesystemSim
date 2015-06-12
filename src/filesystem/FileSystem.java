package filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.XStream;

/**
 * Class for representing file system.
 * 
 * @author Mir4ik
 * @version 0.1 14.11.2013
 */
public class FileSystem implements Serializable {

	private static final long serialVersionUID = -5528362001681378344L;
	
	public static final transient int WRONG_BLOCK = -1;
	
	public static final transient int TRUNCATED_FILENAME = -2;
	
	public static final transient String DIVIDER = "/";
	
	public static final transient String ROOT = "root";
	
	// maximum file name length
	public static final transient int MAX_FILE_NAME_SIZE = 255;
	
	// for debugging
	private static final transient int SCALE = 1;
	
	// maximum count of blocks in file system
	public static final transient int MAX_BLOCKS_COUNT = 65536/SCALE;

	// array of all blocks in file system
	private DataBlock[] blocks = new DataBlock[MAX_BLOCKS_COUNT];
	
	private FileDescriptor root = new FileDescriptor(true);
	
	private FileDescriptor currentDirectory = root;
	
	// set of uid's of opened files
	private Set<Long> opened = new HashSet<Long>();
	
	// map with file names as keys and descriptors as values
	private Map<String, FileDescriptor> links = 
		new HashMap<String, FileDescriptor>();
	
	public FileSystem() {
		for (int i = 0; i < blocks.length; i++) {
			blocks[i] = new DataBlock();
		}
		links.put(ROOT, root);
	}
	
	public int getFirstFreeBlock() {
		for (int i = 0; i < blocks.length; i++) {
			if (!blocks[i].isUsed()) {
				return i;
			}
		}
		return WRONG_BLOCK;
	}
	
	public DataBlock getBlock(int index) {
		if (index < 0 || index >= MAX_BLOCKS_COUNT) {
			throw new IllegalArgumentException("Wrong index!");
		}
		return blocks[index];
	}
	
	public boolean addOpen(long uid) {
		return opened.add(uid);
	}
	
	public boolean removeOpen(long uid) {
		return opened.remove(uid);
	}
	
	public int openedCount() {
		return opened.size();
	}
	
	public boolean isOpened(long uid) {
		return opened.contains(uid);
	}
	
	public int addLink(String name, FileDescriptor fd) {
		if (name.isEmpty() || name == null || name.equals(ROOT)) {
			throw new IllegalArgumentException("Wrong name!");
		}
		if (links.size() == FileDescriptor.MAX_DESCRIPTORS_COUNT) {
			return FileDescriptor.WRONG_DESCRIPTOR;
		}
		String newName = fd.isDirectory() ? name : truncate(name);
		links.put(newName, fd);
		if (name.length() != newName.length()) {
			return TRUNCATED_FILENAME;
		}
		return 0;
	}
	
	public void removeLink(String name) {
		if (name.equals(ROOT)) {
			return;
		}
		links.remove(name);
	}
	
	public int linksCount() {
		return links.size();
	}
	
	public FileDescriptor getFileDescriptor(long uid) {
		Collection<FileDescriptor> descriptors = links.values();
		for (FileDescriptor fd: descriptors) {
			if (fd.getUid() == uid) {
				return fd;
			}
		}
		return null;
	}

	public FileDescriptor getFileDescriptor(String filename) {
		return links.get(filename);
	}
	
	public String getName(FileDescriptor fd) {
		Set<String> keys = links.keySet();
		for (String key: keys) {
			if (links.get(key).getUid() == fd.getUid()) {
				return key;
			}
		}
		return null;
	}
	
	public FileDescriptor getCurrentDirectory() {
		return currentDirectory;
	}
	
	public void setCurrentDirectory(FileDescriptor currentDirectory) {
		if (currentDirectory == null) {
			currentDirectory = root;
		}
		this.currentDirectory = currentDirectory;
	}
	
	public String truncate(String name) {
		if (name.length() > MAX_FILE_NAME_SIZE) {
			name = name.substring(0, MAX_FILE_NAME_SIZE);
		}
		return name;
	}
	
	public static FileSystem read(String pathName) throws Exception {
		if (pathName == null || pathName.isEmpty()) {
			throw new IllegalArgumentException("Wrong pathname!");
		}
		ObjectInputStream is = 
			new ObjectInputStream(new FileInputStream(pathName));
		FileSystem fs = (FileSystem) is.readObject();
		is.close();
		return fs;
	}
	
	public static void write(FileSystem fs, String pathName) throws Exception {
		if (pathName == null || pathName.isEmpty()) {
			throw new IllegalArgumentException("Wrong pathname!");
		}
		ObjectOutputStream os = 
			new ObjectOutputStream(new FileOutputStream(pathName));
		os.writeObject(fs);
		os.flush();
		os.close();
	}
	
	public static FileSystem readXML(String pathName) throws Exception {
		if (pathName == null || pathName.isEmpty()) {
			throw new IllegalArgumentException("Wrong pathname!");
		}
		XStream xml = new XStream();
		FileSystem fs = (FileSystem) xml.fromXML(new File(pathName));
		return fs;
	}
	
	public static void writeXML(FileSystem fs, String pathName) throws Exception {
		if (pathName == null || pathName.isEmpty()) {
			throw new IllegalArgumentException("Wrong pathname!");
		}
		XStream xml = new XStream();
		String data = xml.toXML(fs);
		FileOutputStream os = new FileOutputStream(pathName);
		os.write(data.getBytes());
		os.flush();
		os.close();
	}
}