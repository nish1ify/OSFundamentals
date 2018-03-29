package osp.FileSystem;

import java.util.Enumeration;


import java.util.Vector;
import osp.FileSystem.DirectoryEntry;
import osp.FileSystem.INode;
import osp.FileSystem.MountTable;
import osp.FileSystem.OpenFile;
import osp.IFLModules.IflFileSys;
import osp.Memory.MMU;
import osp.Utilities.c;

/**
 * This class performs operations like file creation and deletion, linking,
 *  opening, closing, reading, and writing files.
 */
public class FileSys extends IflFileSys

{
	private static int Sizeofeachpage;
	static int sizeofeachDirectory = 1;

	/**
	 * This method is used to initialize static members and this method is 
	 * called once before the simulation starts.
	 */
	
	public static void init() 
	{
		DirectoryEntry.init();
		INode.init();
		OpenFile.init();
		
		//Calculates size of each page
		
	Sizeofeachpage = (int) Math.pow(2.0, MMU.getVirtualAddressBits() - MMU.getPageAddressBits());
	
	}



	/**
	 * Creates a new hard link.
	 * 
	 * This system call associates the new name <code>linkname</code> with the
	 * existing i-node associated with <code>name</code>. If the parent
	 * directory of <code>linkname</code> does not exist, return with FAILURE.
	 * (Note that this is different from do_create(). The hard link counter of
	 * that <code>INode</code> object is incremented.
	 * 
	 * The system call makes sure that the link is created on the same device as
	 * the original i-node. Also, links to directories are not allowed.
	 * 
	 * Returns FAILURE if there exists a directory entry with the name
	 * <code>linkname</code> or if <code>name</code> does not exist or is a
	 * directory. Returns SUCCESS if nothing went wrong.
	 * 
	 * @param name
	 *            name of the file or dir to link.
	 * @param linkname
	 *            name of the link to create.
	 * @return SUCCESS or FAILURE.
	 * 
	 * @OSPProject FileSys
	 */
	public static final int do_link(String filename, String linkname) 
	{
		INode iNode;
		
		//Check with the normalized filename
		filename = FileSys.normalize(filename);
		
		if (MountTable.isMountPoint((String) (linkname = FileSys.normalize(linkname)))) {
			return FAILURE;

		}
		//Checks if link name is valid
		if (!FileSys.checkIfValid(FileSys.intermediatePath(linkname))) 
		{
			return FAILURE;
		}
		if (FileSys.isInvalidFile(linkname)) 
		{
			return FAILURE;
		}
		
		int m = 111;
		if (FileSys.checkIfValid(filename)) 
		{
			return FAILURE;

		} 
		else {
			if (!FileSys.isFile(linkname) || !FileSys.isInvalidFile(filename)) {
				return FAILURE;
			}
		}
		
		if (MountTable.getDeviceID((String) filename) != MountTable.getDeviceID((String) linkname)) {
			return FAILURE;
		}
		
		//New Directory entry for the filename and linkname
		DirectoryEntry dEntry = new DirectoryEntry(linkname, m, iNode);
		dEntry.addEntry((DirectoryEntry) dEntry);
		iNode.incrementLinkCount();
		return SUCCESS;

	}
	
	/**
	 * Creates a new file or directory with the given pathname if it does not
	 * already exist.
	 * 
	 * This system call determines whether <code>pathname</code> is a name of
	 * file or a directory. It must check, whether a file or directory with such
	 * name already exists, determine, which device it must be created on, find
	 * out, whether there is enough space on the device for creation of the file
	 * with the initial size <code>size</code> bytes. If it's a directory, the
	 * second parameter must be ignored (directory size is always 1 block). If
	 * the parent directory of the file or dir to be created does not exist, it
	 * must be created with by calling <code>create()</code>
	 * 
	 * This system call must create and properly initialize the new
	 * <code>INode</code> and <code>DirectoryEntry</code> objects, allocate the
	 * necessary amount of device blocks to hold the file's data if the
	 * parameter <code>size</code> is not zero, update the internal data
	 * structures that hold information about the existing i-nodes and directory
	 * entries and return.
	 * 
	 * The system call returns SUCCESS if the file or dir creation was
	 * successfull, FAILURE otherwise.
	 * 
	 * @param pathname
	 *            pathname to create.
	 * @param size
	 *            initial file size, ignored for directories (directory size is
	 *            always 1 block).
	 * @return SUCCESS or FAILURE. FAILURE is returned if the device has no
	 *         space, if the name is a mount point, if the file with this name
	 *         already exists, or if we cannot create intermediate directories
	 *         when creating a file.
	 * 
	 *         This method creates a file with a given pathname and size (in
	 *         bytes). In one sentence, this means making the necessary checks
	 *         and then creating the corresponding inode and
	 * @OSPProject FileSys
	 * 
	 */
	
	
	public static final int do_create(String pathname, int size)
	{
		int new2;
		INode iNode;
		pathname = FileSys.normalize(pathname);
		
		if (MountTable.isMountPoint((String) pathname)) 
		{
			return FAILURE;

		}
		int new3 = 111;
		
		//Check its it is a directory.
		if (FileSys.isDirectory(pathname))
		{
			size = sizeofeachDirectory;
			new3 = 112;
		}
		else if (FileSys.isFile(pathname)) {
			new3 = 111;
		}
		else 
		{
			return FAILURE;
		}
		
		//Invalid file check
		if (!FileSys.isInvalidFile(FileSys.intermediatePath(pathname)))
		{
			FileSys.create((String) FileSys.intermediatePath(pathname), (int) sizeofeachDirectory);
		}
		if (!FileSys.checkIfValid(FileSys.intermediatePath(pathname))) {
			return FAILURE;
		}
		if (FileSys.isInvalidFile(pathname)) {
			return FAILURE;

		}
		
		if (!FileSys.isInvalidFile(FileSys.intermediatePath(pathname))) 
		{
			return FAILURE;
		}
		
		int new4 = MountTable.getDeviceID((String) pathname);
		int new5 = (int) Math.ceil((float) size / (float) Sizeofeachpage);

		if (INode.iflGetNumberOfFreeBlocks((int) new4) < new5) 
		{
			return FAILURE;

		}
		
		//Allocates free blocks for the new inode
		iNode = new INode(new4);
		INode.addInode(iNode);
		iNode.incrementLinkCount();
		for (new2 = 0; new2 < new5; ++new2) 
		{
			int n6 = iNode.allocateFreeBlock();
			if (n6 != -1)
				continue;
			return FAILURE;
		}
		new2=0;
		while(new2!=new5)
		{
			int n6 = iNode.allocateFreeBlock();
			if (n6 != -1)
			{   n6++;
				continue;
			}
				
			return FAILURE;
			
		}
		
		
		if (new c("fileSysAllocatingExtraBlock", "FileSys", "do_create").i()) {
			new2 = INode.iflGetNumberOfFreeBlocks((int) new4);
			iNode.allocateFreeBlock();
		}

		DirectoryEntry dEntry = new DirectoryEntry(pathname, new3, iNode);

		DirectoryEntry.addEntry((DirectoryEntry) dEntry);

		return SUCCESS;
	}


	/**
	 * This system call deletes a file or directory.
	 * 
	 * If the file is open, its <code>INode</code> cannot be deleted, the file's
	 * blocks on the device must remain allocated until the i-node can be
	 * cleaned up in the <code>close()</code> system call, at which point the
	 * device blocks must also be set free. However the directory entry for the
	 * file must disappear so that creation of a new file with the same name
	 * will again be possible regardless of whether the i-node is still there or
	 * not.
	 * 
	 * A directory can be deleted only if it is empty.
	 * 
	 * @param name
	 *            pathname of file or dir to delete
	 * @return SUCCESS or FAILURE. FAILURE if file is a mount point, does not
	 *         exist, or if deleting a non-empty directory.
	 * 
	 * @OSPProject FileSys
	 */
	public static final int do_delete(String filename) 
	{
		if (!FileSys.isInvalidFile(filename = FileSys.normalize(filename))) {
			return FAILURE;
		}
		if (MountTable.isMountPoint((String) filename)) {
			return FAILURE;
		}
		if (FileSys.checkIfValid(filename) && !FileSys.nullCheck(filename)) {
			return FAILURE;
		}
		
		//Directory entry to decrement the link count of the inode
		INode iNode = DirectoryEntry.getINodeOf((String) filename);
		
		if (iNode.getLinkCount() > 0) {
			iNode.decrementLinkCount();
		}
		DirectoryEntry.removeEntry((String) string);
		if (iNode.getLinkCount() + iNode.getOpenCount() == 0)
		{
			INode.removeInode(iNode);
			iNode.releaseBlocks();
		}
		return SUCCESS;
	}

	/**
	 * List the contents of a directory.
	 * 
	 * This system call returns a <code>java.util.Vector</code> object
	 * containing <code>String</code> objects, each of them a fully qualified
	 * name of a file or subdirectory in the directory <code>dirname</code>.
	 * Returns FAILURE if <code>dirname</code> is not a directory or does not
	 * exist.
	 * 
	 * @param dirname
	 *            directory name to list contents of
	 * @return Vector containing the String objects with names of all directory
	 *         entries in the directory. NULL if the directory does not exist.
	 * 
	 * @OSPProject FileSys
	 */
	public static final Vector do_dir(String dirname)
	{
		dirname = FileSys.pathToDir(dirname);
		if (!FileSys.checkIfValid(str)) {

			return null;

		}
		Vector<String> vector = new Vector<String>();
		Enumeration enumeration = DirectoryEntry.getElements();
		while (enumeration.hasMoreElements()) {
			DirectoryEntry dEntry = (DirectoryEntry) enumeration.nextElement();
			String str = FileSys.pathToFile(directoryEntry.getPathname());
			if (!string2.startsWith(dirname) || dEntry.getPathname().equals(dirname)
					|| str.indexOf("/", dirname.length()) != -1)
				continue;
			vector.addElement(dEntry.getPathname());
		}
		return vector;
	}

	//Checks if the input paramter is null
	private static boolean nullCheck(String string) 
	{
		String str= FileSys.pathToDir(string);
		Enumeration enumeration = DirectoryEntry.getElements();
		while (enumeration.hasMoreElements()) {
			DirectoryEntry dEntry = (DirectoryEntry) enumeration.nextElement();
			if (!dEntry.getPathname().startsWith(str) || dEntry.getPathname().equals(str))
				continue;
			return false;
		}
		return true;
	}

	
	private static boolean isDirectory(String string) {
		if (!string.startsWith("/") || !string.endsWith("/")) {
			return false;
		}
		return true;
	}
	

	private static boolean isFile(String string)
	{
		return !string.endsWith("/");
	}
	
	//Checks for intermediatePath
	private static String intermediatePath(String string) {
		int n = (string = FileSys.normalize(string)).endsWith("/") ? string.lastIndexOf("/", string.length() - 2)
				: string.lastIndexOf("/");
		String string2 = new String(string.substring(0, n + 1));
		return string2;
	}
	
	
	public static boolean isInvalidFile(String string) {
		if ((string = FileSys.pathToDir(string)).equals("/")) {
			return true;
		}
		
		iNode = DirectoryEntry.getINodeOf((String) (string = FileSys.pathToFile(string)));
		return iNode != null;
	}


	//Check is the filename is valid

	private static boolean checkIfValid(String string) {
		if ((string = FileSys.pathToDir(string)).equals("/")) {
			return true;
		}
		if (MountTable.isMountPoint((String) string)) {
			return true;
		}
		return DirectoryEntry.checkIfContains((String) string);
	}
	
	//Function to map the pathname into the Directory
	static String pathToDir(String string) {
		int n;
		str = string + "/";
		String strnew = new String("/" + str);
		String strnew2 = new String("//");
		while ((n = strnew.indexOf(strnew2)) != -1) {
			strnew2 = new String(
					strnew.substring(0, n) + strnew.substring(n + strnew2.length() - 1, strnew.length()));
		}
		return strnew;

	}

//Function to normalize the pathname 
	static String normalize(String string) {
		int n;
		String string2 = new String("/" + string);
		String string3 = new String("//");
		while ((n = string2.indexOf(string3)) != -1) {
			string2 = new String(
					string2.substring(0, n) + string2.substring(n + string3.length() - 1, string2.length()));
		}
		return string2;
	}

	

	static String pathToFile(String string)
	{
		String str = FileSys.pathToDir(string);
		return str.substring(0, str.length() - 1);
	}
	
	public static void atError() {
	}

	public static void atWarning() {
	}
	
	

}