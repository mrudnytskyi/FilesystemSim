package tests;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

import filesystem.DataBlock;
import filesystem.FileDescriptor;
import filesystem.FileSystem;

/**
 * Class for console application, used to work with file system.
 * 
 * @author Mir4ik
 * @version 0.3 13.12.2013
 */
public class Runner {
	
	private static FileSystem fs = null;
	
	public static final int NUMBER_PARSE_ERROR = -1;

	public static void main(String[] args) {
		System.out.println("Welcome in file system manager.");
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("> ");
			String line = scanner.nextLine();
			StringTokenizer tokenizer = new StringTokenizer(line, " ");
			String[] commands = new String[tokenizer.countTokens()];
			for (int i = 0; tokenizer.hasMoreTokens(); i++) {
				commands[i] = tokenizer.nextToken();
			}
			switch (commands[0]) {
			case "mount":
				if (alreadyMounted()) {
					System.err.println("There is already mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				try {
					fs = FileSystem.readXML(commands[1]);
				} catch (Exception e) {
					System.err.println(e);
					break;
				}
				System.out.println("File system successfully mounted.");
				break;
			case "umount":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				try {
					FileSystem.writeXML(fs, commands[1]);
					fs = null;
				} catch (Exception e) {
					System.err.println(e);
					break;
				}
				System.out.println("File system successfully unmounted.");
				break;
			case "filestat":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				long uid = parseLong(commands[1]);
				if (uid == NUMBER_PARSE_ERROR) {
					break;
				}
				FileDescriptor fd = fs.getFileDescriptor(uid);
				if (fd == null) {
					System.err.println(
						"There is no file descriptor with uid " + uid + "!");
					break;
				}
				System.out.println(fd);
				break;
			case "ls":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				FileDescriptor current = fs.getCurrentDirectory();
				List<Integer> blocks = current.getBlocks();
				Iterator<Integer> iterator = blocks.iterator();
				while (iterator.hasNext()) {
					DataBlock block = fs.getBlock(iterator.next());
					// folder blocks contain names of sub folders and files
					System.out.print(block + " --- ");
					fd = fs.getFileDescriptor(block.toString());
					System.out.println(fd);
				}
				break;
			case "create":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				String name = commands[1];
				int index = fs.getFirstFreeBlock();
				if (index == FileSystem.WRONG_BLOCK) {
					System.err.println("There is no free blocks!");
					break;
				}
				DataBlock block = fs.getBlock(index);
				block.addData("");
				int indexDir = fs.getFirstFreeBlock();
				if (indexDir == FileSystem.WRONG_BLOCK) {
					System.err.println("There is no free blocks!");
					block.removeData();
					break;
				}				
				DataBlock blockDir = fs.getBlock(indexDir);
				blockDir.addData(fs.truncate(name));
				// check if path is absolute or not
				if (name.contains(FileSystem.DIVIDER)) {
					int lastIndex = name.lastIndexOf(FileSystem.DIVIDER);
					String parent = name.substring(0, lastIndex);
					current = fs.getFileDescriptor(parent);
					if (current == null) {
						System.err.println(
							"There is no directory with name " + parent + "!");
						block.removeData();
						blockDir.removeData();
						break;
					}
				} else {
					current = fs.getCurrentDirectory();
				}
				// add link to parent
				current.addBlock(indexDir);
				fd = new FileDescriptor(false);
				fd.addBlock(index);
				int message = fs.addLink(name, fd);
				if (message == FileSystem.TRUNCATED_FILENAME) {
					System.out.println("File name was truncated!");
				}
				if (message == FileDescriptor.WRONG_DESCRIPTOR) {
					System.err.println("Maximum count of descriptors reached!");
					block.removeData();
					blockDir.removeData();
					current.removeBlock(indexDir);
					break;
				}
				System.out.println("File with name " + fs.truncate(name) + 
					" was successfully created with " + fd + ".");
				break;
			case "open":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				name = commands[1];
				fd = fs.getFileDescriptor(name);
				if (fd == null) {
					System.err.println(
						"There is no file with name " + name + "!");
					break;
				}
				// check for symlink
				String newName = fs.getBlock(fd.getBlocks().get(0)).toString();
				current = fs.getFileDescriptor(newName);
				if (current != null && current.isFile()) {
					fd = current;
					name = newName;
				}
				fs.addOpen(fd.getUid());
				System.out.println("File with name " + name + 
					" was successfully opened with " + fd + ".");
				break;
			case "close":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				if ((uid = parseLong(commands[1])) == NUMBER_PARSE_ERROR) {
					break;
				}
				if (!fs.isOpened(uid)) {
					System.err.println("File with uid " + uid + 
						" was not opened!");
					break;
				}
				fs.removeOpen(uid);
				System.out.println("File with uid " + uid + 
					" was successfully closed.");
				break;
			case "read":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 3) {
					System.err.println("Wrong parameters count!");
					break;
				}
				if ((uid = parseLong(commands[1])) == NUMBER_PARSE_ERROR) {
					break;
				}
				if (!fs.isOpened(uid)) {
					System.err.println("File with uid " + uid + 
						" was not opened!");
					break;
				}
				int displacement = parseInt(commands[2]);
				int size = parseInt(commands[3]);
				if (displacement == NUMBER_PARSE_ERROR || 
					size == NUMBER_PARSE_ERROR) {
						break;
				}
				fd = fs.getFileDescriptor(uid);
				if (fd == null) {
					System.err.println(
						"There is no file descriptor with uid " + uid + "!");
					break;
				}
				blocks = fd.getBlocks();
				int blocksCount = DataBlock.roundToBlockSize(size)/
					DataBlock.BLOCK_SIZE;
				int blockNumber = displacement/DataBlock.BLOCK_SIZE;
				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < blocksCount; i++) {
					String data = fs.getBlock(blocks.get(blockNumber + i)).
						toString();
					builder.append(data);
				}
				String data = builder.toString();
				System.out.println("Data " + data + 
					" was successfully read form file with uid " + uid +
					" on displacement " + displacement + " and with "
					+ "real size " + data.length() + ".");
				break;
			case "write":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 3) {
					System.err.println("Wrong parameters count!");
					break;
				}
				if ((uid = parseLong(commands[1])) == NUMBER_PARSE_ERROR) {
					break;
				}
				if (!fs.isOpened(uid)) {
					System.err.println("File with uid " + uid + 
						" was not opened!");
					break;
				}
				displacement = parseInt(commands[2]);
				size = parseInt(commands[3]);
				if (displacement == NUMBER_PARSE_ERROR || 
					size == NUMBER_PARSE_ERROR) {
						break;
				}
				fd = fs.getFileDescriptor(uid);
				if (fd == null) {
					System.err.println(
						"There is no file descriptor with uid " + uid + "!");
					break;
				}
				blocks = fd.getBlocks();
				builder = new StringBuilder();
				size = DataBlock.roundToBlockSize(size);
				for (int i = 0; i < size; i++) {
					builder.append(randomChar());
				}
				data = builder.toString();
				String[] dataArray = DataBlock.packData(data);
				if (dataArray.length > blocks.size()) {
					System.err.println(
						"Can not write data because file size is to small!" +
						" Please, use truncate command instead.");
					break;
				}
				blockNumber = displacement/DataBlock.BLOCK_SIZE;
				for (int i = 0, j = 0; j < dataArray.length; i++, j ++) {
					block = fs.getBlock(blocks.get(blockNumber + i));
					block.addData(dataArray[j]);
				}
				System.out.println("Data " + data + 
					" was successfully writed in file with uid " + uid +
					" on displacement " + displacement + " and with "
					+ "real size " + size + ".");
				break;
			case "link":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 2) {
					System.err.println("Wrong parameters count!");
					break;
				}
				name = commands[1];
				String linkName = commands[2];
				if (name.equals(linkName)) {
					System.err.println("Can not make cyclic hard link!");
					break;
				}
				fd = fs.getFileDescriptor(name);
				if (fd == null) {
					System.err.println(
						"There is no file with name " + name + "!");
					break;
				}
				if (fd.isDirectory()) {
					System.err.println("Can not make hard link on directory!");
					break;
				}
				// check if path is absolute or not
				if (linkName.contains(FileSystem.DIVIDER)) {
					int lastIndex = linkName.lastIndexOf(FileSystem.DIVIDER);
					String parent = linkName.substring(0, lastIndex);
					current = fs.getFileDescriptor(parent);
					if (current == null) {
						System.err.println(
							"There is no directory with name " + parent + "!");
						break;
					}
				} else {
					current = fs.getCurrentDirectory();
				}
				indexDir = fs.getFirstFreeBlock();
				if (indexDir == FileSystem.WRONG_BLOCK) {
					System.err.println("There is no free blocks!");
					break;
				}
				// copy descriptor
				if (fd.incrementHardLinksCount()) {
					fs.addLink(linkName, fd);
				} else {
					System.err.println("Maximum count of hard links on this " 
						+ fd + " reached!");
					break;
				}
				// add link to parent directory
				blockDir = fs.getBlock(indexDir);
				blockDir.addData(linkName);
				current.addBlock(indexDir);
				System.out.println("Link " + linkName + " on file " + name + 
					" was successfully created.");
				break;
			case "unlink":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				linkName = commands[1];
				fd = fs.getFileDescriptor(linkName);
				if (fd == null) {
					System.err.println(
						"There is no link with name " + linkName + "!");
					break;
				}
				fs.removeLink(linkName);
				fd.decrementHardLinksCount();
				// check if path is absolute or not
				if (linkName.contains(FileSystem.DIVIDER)) {
					int lastIndex = linkName.lastIndexOf(FileSystem.DIVIDER);
					String parent = linkName.substring(0, lastIndex);
					current = fs.getFileDescriptor(parent);
				} else {
					current = fs.getCurrentDirectory();
				}
				// remove link from parent directory
				blocks = current.getBlocks();
				for (Integer integer: blocks) {
					if (fs.getBlock(integer).toString().equals(linkName)) {
						fs.getBlock(integer).removeData();
						current.removeBlock(integer);
					}
				}
				if (fd.isNoHardLinksCount()) {
					// delete file
					blocks = fd.getBlocks();
					for (Integer integer: blocks) {
						fs.getBlock(integer).removeData();
					}
					// close file
					uid = fd.getUid();
					if (fs.isOpened(uid)) {
						fs.removeOpen(uid);
					}
					System.out.println("File " + linkName + 
						" was successfully removed.");
				} else {
					System.out.println("Link " + linkName + 
						" was successfully removed.");
				}
				break;
			case "truncate":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 2) {
					System.err.println("Wrong parameters count!");
					break;
				}
				name = commands[1];
				if ((size = parseInt(commands[2])) == NUMBER_PARSE_ERROR) {
					break;
				}
				fd =  fs.getFileDescriptor(name);
				if (fd == null || fd.isDirectory()) {
					System.err.println(
						"There is no file with name " + name + "!");
					break;
				}
				uid = fd.getUid();
				if (!fs.isOpened(uid)) {
					System.err.println("File with uid " + uid + 
						" was not opened!");
					break;
				}
				size = DataBlock.roundToBlockSize(size);
				int count = size/DataBlock.BLOCK_SIZE;
				if (count > fd.blocksCount()) {
					int counter = fd.blocksCount();
					while (counter < count) {
						index = fs.getFirstFreeBlock();
						if (index == FileSystem.WRONG_BLOCK) {
							System.err.println();
							break;
						}
						// add blocks
						fs.getBlock(index).addData(DataBlock.ZERO);
						fd.addBlock(index);
						counter++;
					}
				} else {
					blocks = fd.getBlocks();
					List<Integer> blocksPart = blocks.subList(count,
						blocks.size());
					for (Integer integer: blocksPart) {
						// remove last blocks
						fs.getBlock(integer).removeData();
						fd.removeBlock(integer);
					}
				}
				System.out.println("File with name " + name + 
					" was successfully truncated to new size " + size + ".");
				break;
			case "mkdir":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				name = commands[1];
				// check if path is absolute or not
				if (name.contains(FileSystem.DIVIDER)) {
					int lastIndex = name.lastIndexOf(FileSystem.DIVIDER);
					String parent = name.substring(0, lastIndex);
					current = fs.getFileDescriptor(parent);
				} else {
					current = fs.getCurrentDirectory();
				}
				indexDir = fs.getFirstFreeBlock();
				if (indexDir == FileSystem.WRONG_BLOCK) {
					System.err.println("There is no free blocks!");
					break;
				}				
				if (current == null || current.isFile()) {
					System.err.println("There is no directory with name " +
						name + "!");
					break;
				}
				// add link to parent directory
				blockDir = fs.getBlock(indexDir);
				blockDir.addData(fs.truncate(name));
				current.addBlock(indexDir);
				fd = new FileDescriptor(true);
				message = fs.addLink(name, fd);
				if (message == FileDescriptor.WRONG_DESCRIPTOR) {
					System.err.println("Maximum count of descriptors reached!");
					blockDir.removeData();
					current.removeBlock(indexDir);
					break;
				}
				if (message == FileSystem.TRUNCATED_FILENAME) {
					System.out.println("Directory name was truncated!");
				}
				System.out.println("Directory with name " + fs.truncate(name) + 
					" was succesfully created.");
				break;
			case "rmdir":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				name = commands[1];
				fd = fs.getFileDescriptor(name);
				if (fd == null) {
					System.err.println(
						"There is no directory with name " + name + "!");
					break;
				}
				if (!fd.isEmpty()) {
					System.err.println("Can not remove not empty directory " +
						name + "!");
					break;
				}
				if (fs.getCurrentDirectory().equals(fd)) {
					System.err.println("Can not remove current directory " + 
						name + "!");
					break;
				}
				// check if path is absolute or not
				if (name.contains(FileSystem.DIVIDER)) {
					int lastIndex = name.lastIndexOf(FileSystem.DIVIDER);
					String parent = name.substring(0, lastIndex);
					current = fs.getFileDescriptor(parent);
				} else {
					current = fs.getCurrentDirectory();
				}
				if (current == null || current.isFile()) {
					System.err.println("There is no directory with name " +
						name + "!");
					break;
				}
				// remove link from parent directory
				blocks = current.getBlocks();
				for (Integer integer: blocks) {
					if (fs.getBlock(integer).toString().equals(name)) {
						fs.getBlock(integer).removeData();
						current.removeBlock(integer);
					}
				}
				fs.removeLink(name);
				System.out.println("Directory with name " + name + 
					" was succesfully removed.");
				break;
			case "cd":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 1) {
					System.err.println("Wrong parameters count!");
					break;
				}
				name = commands[1];
				current = fs.getFileDescriptor(name);
				if (current == null) {
					System.err.println("There is no directory with name " +
						name + "!");
					break;
				}
				// check for symlink
				if (current.isFile()) {
					name = fs.getBlock(current.getBlocks().get(0)).toString();
					fd = fs.getFileDescriptor(name);
					if (fd != null && fd.isDirectory()) {
						current = fd;
					} else {
						System.err.println("There is no directory with name " +
							name + "!");
						break;
					}
				}
				fs.setCurrentDirectory(current);
				System.out.println("Current directory changed to " + name + ".");
				break;
			case "pwd":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				name = fs.getName(fs.getCurrentDirectory());
				System.out.println("Current directory is " + name + ".");
				break;
			case "symlink":
				if (!alreadyMounted()) {
					System.err.println("There is no mounted file system!");
					break;
				}
				if (commands.length - 1 != 2) {
					System.err.println("Wrong parameters count!");
					break;
				}
				String path = commands[1];
				name = commands[2];
				if (path.equals(name)) {
					System.err.println("Can not make cyclic symbolic link!");
					break;
				}
				// creating file, containing specified path
				index = fs.getFirstFreeBlock();
				if (index == FileSystem.WRONG_BLOCK) {
					System.err.println("There is no free blocks!");
					break;
				}
				block = fs.getBlock(index);
				block.addData(path);
				indexDir = fs.getFirstFreeBlock();
				if (indexDir == FileSystem.WRONG_BLOCK) {
					System.err.println("There is no free blocks!");
					block.removeData();
					break;
				}				
				blockDir = fs.getBlock(indexDir);
				blockDir.addData(fs.truncate(name));
				// check if path is absolute or not
				if (name.contains(FileSystem.DIVIDER)) {
					int lastIndex = name.lastIndexOf(FileSystem.DIVIDER);
					String parent = name.substring(0, lastIndex);
					current = fs.getFileDescriptor(parent);
					if (current == null) {
						System.err.println(
							"There is no directory with name " + parent + "!");
						break;
					}
				} else {
					current = fs.getCurrentDirectory();
				}
				// add link to parent
				current.addBlock(indexDir);
				fd = new FileDescriptor(false);
				fd.addBlock(index);
				message = fs.addLink(name, fd);
				if (message == FileSystem.TRUNCATED_FILENAME) {
					System.out.println("File name was truncated!");
				}
				if (message == FileDescriptor.WRONG_DESCRIPTOR) {
					System.err.println("Maximum count of descriptors reached!");
					block.removeData();
					blockDir.removeData();
					current.removeBlock(indexDir);
					break;
				}
				System.out.println("Symbolic link with name " + name + " on " 
					+ path + " was successfully created.");
				break;
			case "help":
				System.out.println(
					"In this application you can use next commands:");
				System.out.println(
					"mount		Load file system from specified file.");
				System.out.println(
					"umount		Load current file system to specified file"
					+ " and delete it from system.");
				System.out.println(
					"filestat	Show information about specified by id file "
					+ "descriptor.");
				System.out.println(
					"ls		Show file list with their descriptions.");
				System.out.println(
					"create		Create file with specified name.");
				System.out.println(
					"open		Open file with specified name and create "
					+ "unique id.");
				System.out.println(
					"close		Close file, specified by unique id.");
				System.out.println(
					"read		Read data from specified by id file with "
					+ "specified offset and size.");
				System.out.println(
					"write		Write data to specified by id file with "
					+ "specified offset and size.");
				System.out.println(
					"link		Create link on specified file with specified "
					+ "name.");
				System.out.println(
					"unlink		Destroy link with specified name.");
				System.out.println(
					"truncate	Change file size, specified by id.");
				System.out.println(
					"mkdir		Make directory with specified name.");
				System.out.println(
					"rmdir		Delete specified empty directory.");
				System.out.println(
					"cd		Change current working directory to specified.");
				System.out.println(
					"pwd		Show current working directory.");
				System.out.println(
					"symlink		Create link with specified name on "
					+ "specified path.");
				System.out.println("help		Show these information.");
				System.out.println(
					"exit		Close application immediately.");
				break;
			case "exit":
				System.out.println("Goodbye.");
				scanner.close();
				System.exit(0);
			default:
				System.err.println("Unknown command!");
			}
		}
	}
	
	private static boolean alreadyMounted() {
		return fs != null;
	}
	
	private static long parseLong(String data) {
		long parsed = NUMBER_PARSE_ERROR;
		try {
			parsed = Long.parseLong(data);
		} catch (NumberFormatException e) {
			System.err.println(e);
		}
		return parsed;
	}
	
	private static int parseInt(String data) {
		int parsed = NUMBER_PARSE_ERROR;
		try {
			parsed = Integer.parseInt(data);
		} catch (NumberFormatException e) {
			System.err.println(e);
		}
		return parsed;
	}
	
	private static char randomChar() {
		Random random = new Random();
		char generated = (char) random.nextInt();
		while (generated < 'A' || generated > 'z') {
			generated = (char) random.nextInt();
		}
		return generated;
	}
}