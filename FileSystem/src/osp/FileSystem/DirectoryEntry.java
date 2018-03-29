package osp.FileSystem;

import java.util.Enumeration;
import osp.FileSystem.FileSys;
import osp.FileSystem.INode;
import osp.IFLModules.IflDirectoryEntry;
import java.util.Hashtable;
import java.util.TreeMap;


/**
 * This class implements directory entry includes a pathname, an inode, 
 * and a type (FileEntry or DirEntry). 
*/
public class DirectoryEntry extends IflDirectoryEntry 
{
	//Tree Map to store the list od directory entires
	private static TreeMap<String, DirectoryEntry> ListOfEntries;

	static void init() 
	{
		ListOfEntries = new TreeMap<String, DirectoryEntry>();
	}

	
	
	/**
	 * This constructor initializes a new directory entry for an inode using super
	 * using the parameters string, device number and the associated inode number
	*/
	public DirectoryEntry(String string, int n, INode iNode)
	 {
		
		super(string, n, iNode);
	}

	/**
	 * This method returns the inode associated with the pathname which is taken in as a parameter
	*/
	public static INode do_getINodeOf(String pathname) 
	{
		//Obtains directory entry from the given pathname
		DirectoryEntry dEntry = DirectoryEntry.getEntry(pathname);
		if (dEntry == null) {
			return null;
		}
		return dEntry.getINode();
	}

	static boolean checkIfContains(String string)
	{
		//Checks existence of directory from the main list
		return ListOfEntries.containsKey(string);
	}
	
	private static DirectoryEntry getEntry(String pathname) 
	{
		
		DirectoryEntry dEntry = (DirectoryEntry) ((Object) ListOfEntries.get(FileSys.pathToDir(pathname)));
		if (dEntry != null) {
			return dEntry;
		}
		return (DirectoryEntry) ((Object) ListOfEntries.get(FileSys.pathToFile(pathname)));
	}

	
//Removes entry from the main list
	static DirectoryEntry removeEntry(String pathname) {
		DirectoryEntry dEntry = (DirectoryEntry) ((Object) ListOfEntries.remove(string = FileSys.pathToDir(pathname)));
		if (dEntry != null) {
			return dEntry;
		}
		string = FileSys.pathToFile(pathname);
		return (DirectoryEntry) ((Object) ListOfEntries.remove(pathname));
	}

	static Enumeration<DirectoryEntry> getElements()
	{
		return ListOfEntries.elements();
	}
	
	//Adds directory entry to the main list
	
	static void addEntry(DirectoryEntry dEntry) {
		ListOfEntries.put(dEntry.getPathname(), dEntry);
	}

	}