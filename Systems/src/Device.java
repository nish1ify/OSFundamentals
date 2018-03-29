

/**
 This class stores all pertinent information about a device in
 the device table.  This class should be sub-classed by all
 device classes, such as the Disk class.

 @OSPProject Devices
 */

import java.util.Enumeration;

import osp.FileSys.OpenFile;
import osp.Hardware.Disk;
import osp.IFLModules.IflDevice;
import osp.Memory.MMU;
import osp.Memory.PageTableEntry;
import osp.Threads.ThreadCB;
import osp.Utilities.GenericList;

public class Device extends IflDevice {
	/**
	 * This constructor initializes a device with the provided parameters. As a
	 * first statement it must have the following:
	 * 
	 * super(id,numberOfBlocks);
	 * 
	 * @param numberOfBlocks
	 *            -- number of blocks on device
	 * @OSPProject Devices7
	 */

	public Device(int id, int numberOfBlocks) {
		super(id, numberOfBlocks);

		this.iorbQueue = new GenericList();
	}

	/**
	 * This method is called once at the beginning of the simulation. Can be
	 * used to initialize static variables.
	 * 
	 * @OSPProject Devices
	 */
	public static void init() {
		// your code goes here

	}

	/**
	 * Enqueues the IORB to the IORB queue for this device according to some
	 * kind of scheduling algorithm.
	 * 
	 * This method must lock the page (which may trigger a page fault), check
	 * the device's state and call startIO() if the device is idle, otherwise
	 * append the IORB to the IORB queue.
	 * 
	 * @return SUCCESS or FAILURE. FAILURE is returned if the IORB wasn't
	 *         enqueued (for instance, locking the page fails or thread is
	 *         killed). SUCCESS is returned if the IORB is fine and either the
	 *         page was valid and device started on the IORB immediately or the
	 *         IORB was successfully enqueued (possibly after causing pagefault
	 *         pagefault)
	 * @OSPProject Devices
	 */
	public int do_enqueueIORB(IORB iorb) {

		PageTableEntry pgTable = iorb.getPage();

		//int lockingStatus = pgTable.lock(iorb);
		//if (lockingStatus != SUCCESS)
			//return FAILURE;

		if (iorb.getThread().getStatus() != ThreadKill) {
			OpenFile openFile = iorb.getOpenFile();
			openFile.incrementIORBCount();
		}

		// A block size
		// equals the size of a virtual memory page, which can be obtained using
		// the two methods
		// provided by the class MMU: getVirtualAddressBits() and
		// getPageAddressBits()

		int vAddressBit = MMU.getVirtualAddressBits();
		int pAddressBit = MMU.getPageAddressBits();

		int blockNumber = iorb.getBlockNumber();

		double blockSize = Math.pow(2, vAddressBit - pAddressBit);

		Disk disk = (Disk) this;
		int bytesPerSector = disk.getBytesPerSector();
		int sectorPerTrack = disk.getSectorsPerTrack();

		// Refer to image at http://kias.dyndns.org/comath/42.html
		// to visualize sectors, tracks, cylinders
		int blockPerTrack = sectorPerTrack * bytesPerSector
				/ (int) blockSize;
		int trackPerCylinder = disk.getPlatters();
		int cylinder = blockNumber
				/ (blockPerTrack * trackPerCylinder);

		iorb.setCylinder(cylinder);

		if (iorb.getThread().getStatus() == ThreadKill) {
			return FAILURE;
		}

		// if thread is alive and device is idle i.e. not busy

		if ((iorb.getThread().getStatus() != ThreadKill) && !isBusy()) {
			startIO(iorb);
			return SUCCESS;
		} else {
			((GenericList) iorbQueue).append(iorb);
			return SUCCESS;
		}

		// return FAILURE;

	}

	/**
	 * Selects an IORB (according to some scheduling strategy) and dequeues it
	 * from the IORB queue.
	 * 
	 * @OSPProject Devices
	 */
	public IORB do_dequeueIORB() {
		if (iorbQueue.isEmpty())
			return null;
		else {

			return (IORB) ((GenericList) iorbQueue).removeHead();
		}
	}

	/**
	 * Remove all IORBs that belong to the given ThreadCB from this device's
	 * IORB queue
	 * 
	 * The method is called when the thread dies and the I/O operations it
	 * requested are no longer necessary. The memory page used by the IORB must
	 * be unlocked and the IORB count for the IORB's file must be decremented.
	 * 
	 * @param thread
	 *            thread whose I/O is being canceled
	 * @OSPProject Devices
	 */
	public void do_cancelPendingIO(ThreadCB thread) {

		if (iorbQueue.isEmpty())
			return;

		@SuppressWarnings("unchecked")
		Enumeration<IORB> enumerator = ((GenericList) iorbQueue)
				.forwardIterator();

		while (enumerator.hasMoreElements()) {
			IORB iorb = enumerator.nextElement();
			if (iorb.getThread().equals(thread)) {
				// unlock the buffer page
				iorb.getPage().unlock();
				iorb.getOpenFile().decrementIORBCount();

				if (iorb.getOpenFile().closePending) {
					if (iorb.getOpenFile().getIORBCount() == 0) {
						iorb.getOpenFile().close();
					}
				}
				((GenericList) iorbQueue).remove(iorb);
			}
		}

	}

	/**
	 * Called by OSP after printing an error message. The student can insert
	 * code here to print various tables and data structures in their state just
	 * after the error happened. The body can be left empty, if this feature is
	 * not used.
	 * 
	 * @OSPProject Devices
	 */
	public static void atError() {

	}

	/**
	 * Called by OSP after printing a warning message. The student can insert
	 * code here to print various tables and data structures in their state just
	 * after the warning happened. The body can be left empty, if this feature
	 * is not used.
	 * 
	 * @OSPProject Devices
	 */
	public static void atWarning() {
		// your code goes here

	}

	/*
	 * Feel free to add methods/fields to improve the readability of your code
	 */

}

/*
 * Feel free to add local classes to improve the readability of your code
 */
