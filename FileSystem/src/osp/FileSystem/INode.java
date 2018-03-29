package osp.FileSystem;
import java.util.ArrayList;
import java.util.Vector;
import osp.Devices.Device;
import osp.FileSystem.MountTable;
import osp.IFLModules.IflINode;
import osp.Utilities.GenericList;

/**
 * This class maintains information about where the file is located, the size of it
 * (in blocks), its open count and number of links to it(hard link). 
 */

public class INode extends IflINode 
{
   
	private static ArrayList<INode> inodeList;
	private static int[] freeBlocks;
	private Vector elements = new Vector();
	private static boolean[][] FileBlockMap;
	
    /**
     * This method is a constructor which creates an inode by calling super which takes 
     * device number as paramter.
     */

	
	public INode(int n) {
		super(n);
	}
//for simulation to begin
	static void init() 
	{
		int n=0;
		FileBlockMap = new boolean[Device.getTableSize()][];
		inodeList = new ArrayList<INode>();
		int tablesize= Device.getTableSize();
		
		
		//Initialize FileBlockMap using mountpoint
		while(n!=FileBlockMap.length)
		{
			if (MountTable.getMountPoint((int) n) == null)
				continue;
			INode.FileBlockMap[n] = new boolean[Device.get((int) n).getNumberOfBlocks()];
			n++;
		}	
			
		freeBlocks = new int[tablesize];
		n=0;
		while(n!=tablesize)
			{
				INode.freeBlocks[n] = Device.get((int) n).getNumberOfBlocks();
               n++;
			}
	}
	
	//Adding an inode to the iNode List
	static void addInode(INode iNode) {
		inodeList.add(iNode);
	}

	public static int iflGetNumberOfFreeBlocks(int n) {
		return freeBlocks[n];
	}
	
	/**
	 * This method checks whether blocks are free within a device and returns true;
	 * This method takes in device id and block number
	*/
	public static boolean do_isFreeBlock(int n, int m) {
		if (m < 0 || m >= Device.getTableSize()) 
		{
			return false;
		}
		return !FileBlockMap[m][n];
	}

	/**
	 * This method allocates free block in a device for a file and returns the block number
	 */
	public int do_allocateFreeBlock() 
	{
		int k=0;
		if ((this.getDeviceID())< 0 || (this.getDeviceID() >= Device.getTableSize()))
		{
			return -1;
		}
		
		while(k!= Device.get((int) this.getDeviceID()).getNumberOfBlocks())
		{
			if (FileBlockMap[this.getDeviceID()][k])
			{
				k++;
				continue;
			}
			INode.FileBlockMap[this.getDeviceID()][k] = true;
			Integer newnumber = new Integer(k);
			this.elements.addElement(newnumber);
			this.setBlockCount(this.getBlockCount() + 1);
			int[] arrayofblocks = freeBlocks;
			int newdevice = this.getDeviceID();
			arrayofblocks[newdevice] = arrayofblocks[newdevice] - 1;
			return k;
			
		}
		return -1;
		
	}
	
	//Obtains physical address of the device of which the file is associated with
	int getPhysicalAddress(int n) {
		return (Integer) this.elements.elementAt(n);
	}	
	
	/**
	 * This method releases all the blocks that are associated with the 
	 * particular inode.
	 * */
	public void do_releaseBlocks() 
	{
		Vector blockvector = this.elements;
		int n = this.getDeviceID();
		if (n < 0 || n >= Device.getTableSize()) {
			return;
		}
		if (blockvector == null) {
			return;
		}
		while (blockvector.size() > 0) 
		{
		int newblock = (Integer) blockvector.elementAt(0);
		blockvector.removeElementAt(0);
		INode.FileBlockMap[n][newblock] = false;
		int[] arrayoffreeblocks = freeBlocks;
		int newinteger = this.getDeviceID();
		arrayoffreeblocks[newinteger] = arrayoffreeblocks[newinteger] + 1;
			} 
			
			
		
		this.setBlockCount(0);
	}
		
	static void removeInode(INode iNode) 
	{
		inodeList.remove((Object) iNode);	
	}	
		
}		
	
	

	

	
	
	
	
		
		
		
		
	

	

	

