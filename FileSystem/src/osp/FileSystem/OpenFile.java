package osp.FileSystem;
import osp.FileSystem.MountTable;
import osp.IFLModules.Event;
import osp.IFLModules.IflOpenFile;
import osp.IFLModules.SystemEvent;
import osp.Memory.PageTableEntry;
import osp.Devices.Device;
import osp.Devices.IORB;
import osp.FileSystem.DirectoryEntry;
import osp.FileSystem.FileSys;
import osp.FileSystem.INode;
import osp.Tasks.TaskCB;
import osp.Threads.ThreadCB;
import osp.Utilities.GenericList;
import java.util.ArrayList;

/**
 * This class provides methods to create open file handles, access their components, 
 * and use them for performing I/O operations.
 */

public class OpenFile extends IflOpenFile {
	static ArrayList ListOfFiles;

	static void init() {
		ListOfFiles = new ArrayList();
	}

	/**
     * This constructor creates new instances of an OpenFile with parameters inode and
     * task to which the file associated with.	
	 */
	public OpenFile(INode iNode, TaskCB taskCB) {
		super(iNode, taskCB);
	}

	/**
	 * This method opens an existing file for reading and writing and takes the
	 * parameters filename and the associated task.
	 */
	public static OpenFile do_open(String filename, TaskCB task) 
	{
		String properstring = FileSys.normalize(filename);
		if (MountTable.isMountPoint((String) properString)) 
		{
			return null;
		}
		if (!FileSys.isInvalidFile(properString)) 
		{
			return null;
		}
		
		//Creates a new inode for the directory entry
		INode iNode = DirectoryEntry.getINodeOf((String) properString);
		OpenFile newFile = new OpenFile(iNode, taskCB);
		iNode.incrementOpenCount();
		ListOfFiles.insert((Object) newFile);
		task.addFile(newFile);

		return newFile;
	}
	

	/**
	 * This method closes an open file and closes any pending IORBs.
	 */
	public int do_close()
      {
		if (this.getIORBCount() > 0) 
		{
			this.closePending = true;
			return FAILURE;
		}
		if (!ListOfFiles.contains((Object) this)) 
		{
			return FAILURE;
		}
		
        //Decrements open count of this inode
		ListOfFiles.remove((Object) this);
		this.getINode().decrementOpenCount();
		INode iNodenew = this.getINode();
		
		int n=iNode.getOpenCount();
		int m=iNode.getLinkCount();
		if (n+m == 0) 
		{   //releases free blocks
			iNode.releaseBlocks();
			INode.removeInode(iNode);
		}

		this.getTask().removeFile(this);
		this.closePending = false;

		return SUCCESS;
	}
	
	

	/**
	 * This methods reads from an opened file and takes in the parameters the file block 
	 * number, the memory page to write and the associated thread to perform the
	 * read operation.
	 */
	public int do_read(int FileBlockNumber, PageTableEntry pageTableEntry, ThreadCB thread)
	{
		if (FileBlockNumber < 0 || n > this.getINode().getBlockCount() - 1) 
		{
			return FAILURE;
		}
		if (!ListOfFiles.contains((Object) this))
		{
			return FAILURE;
		}
		
		//New system event for reading by suspending previous thread
		SystemEvent Event = new SystemEvent("FileR");
		thread.suspend((Event) Event);
		INode iNode = this.getINode();
		
		if (pageTableEntry == null) {
			Event.notifyThreads();
			return FAILURE;
		}
		
		//Physical address to map to the particular device
		int phyAddress = iNode.getPhysicalAddress(FileBlockNumber);
		IORB iORBnew = new IORB(thread, pageTableEntry, phyAddress, iNode.getDeviceID(), FileRead, this);

		if (iORBnew != null) {
			int isdevicenumber = Device.get((int) devID).enqueueIORB(iORB);
			if ( isdevicenumber== SUCCESS) 
			{
				thread.suspend((Event) iORB);
			}
			if (thread.getStatus() == 22) 
			{
				return FAILURE;
			}
		}
		Event.notifyThreads();
		return SUCCESS;
	}

	/**
	 * This methods writes from an opened file and takes in the parameters the file block 
	 * number, the memory page to write and the associated thread to perform the
	 * read operation.
	 */
	public int do_write(int FileBlockNumber, PageTableEntry pageTableEntry, ThreadCB thread) 
	{
		int phyAddress;
		int n;
		
		if (!ListOfFiles.contains((Object) this)) {
			return FAILURE;
		}
		if (FileBlockNumber< 0) {
			return FAILURE;
		}
		
		//New system event for reading by suspending previous thread
		SystemEvent Event = new SystemEvent("FileW");
		thread.suspend((Event) Event);
		INode iNode = this.getINode();
		int devId = iNode.getDeviceID();
		int blockcount=iNode.getBlockCount();
		if (FileBlockNumber + 1 - blockcount > INode.iflGetNumberOfFreeBlocks(devId)) 
		{
			Event.notifyThreads();
			return FAILURE;
		}
		if (pageTableEntry == null) {
			Event.notifyThreads();
			return FAILURE;
		}
		int allocation = Math.max(FileBlockNumber + 1, this.getINode().getBlockCount());
		for (phyAddr = iNode.getBlockCount() + 1; phyAddress <= allocation; ++phyAddr)
		{
			n = iNode.allocateFreeBlock();
			if (n != -1)
				continue;
		}
		
		//Physical address to map to the particular device

		phyAddress = iNode.getPhysicalAddress(n);
		IORB iORBnew = new IORB(thread, pageTableEntry, phyAddress, devId, FileWrite, this);
		if (iORBnew != null) 
		{
			int m = Device.get((int) devId).enqueueIORB(iORB);
			if (m == SUCCESS) {
				thread.suspend((Event) iORB);
			}
			if (thread.getStatus() == 22)
			{
				return FAILURE;
			}
		}
		Event.notifyThreads();
		return SUCCESS;
	}
}