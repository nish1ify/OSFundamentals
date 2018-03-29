package osp.Devices;

import osp.FileSys.OpenFile;
import osp.IFLModules.IflDiskInterruptHandler;
import osp.Interrupts.InterruptVector;
import osp.Memory.FrameTableEntry;
import osp.Memory.PageTableEntry;
import osp.Tasks.TaskCB;
import osp.Threads.ThreadCB;

/**
 * The disk interrupt handler. When a disk I/O interrupt occurs, this class is
 * called upon the handle the interrupt.
 * 
 * @OSPProject Devices
 */
public class DiskInterruptHandler extends IflDiskInterruptHandler {
	/**
	 * Handles disk interrupts.
	 * 
	 * This method obtains the interrupt parameters from the interrupt vector.
	 * The parameters are IORB that caused the interrupt:
	 * (IORB)InterruptVector.getEvent(), and thread that initiated the I/O
	 * operation: InterruptVector.getThread(). The IORB object contains
	 * references to the memory page and open file object that participated in
	 * the I/O.
	 * 
	 * The method must unlock the page, set its IORB field to null, and
	 * decrement the file's IORB count.
	 * 
	 * The method must set the frame as dirty if it was memory write (but not,
	 * if it was a swap-in, check whether the device was SwapDevice)
	 * 
	 * As the last thing, all threads that were waiting for this event to
	 * finish, must be resumed.
	 * 
	 * @OSPProject Devices
	 */
	public void do_handleInterrupt() {

		IORB iorb = (IORB) InterruptVector.getEvent();
		ThreadCB thread = InterruptVector.getThread();
		PageTableEntry page = InterruptVector.getPage();
		FrameTableEntry frame = iorb.getPage().getFrame();
		TaskCB task = iorb.getThread().getTask();

		// decrease count for the IORB open file
		OpenFile openFile = iorb.getOpenFile();
		openFile.decrementIORBCount();
		if (openFile.closePending && (openFile.getIORBCount() == 0)) {
			openFile.close();
		}
		page.unlock();

		if (frame == null)
			return;

		// To find out whether an I/O is a swap-in or swap-out from/to the swap
		// device, one
		// should compare the device Id of the IORB (getDeviceID()) with
		// SwapDeviceID | a
		// constant defined in OSP 2 .

		if (thread.getStatus() != ThreadKill
				&& iorb.getDeviceID() != SwapDeviceID)
			frame.setReferenced(true);

		if (iorb.getDeviceID() != SwapDeviceID && iorb.getIOType() == FileRead
				&& (thread.getTask().getStatus() == TaskLive))
			frame.setDirty(true);

		if (iorb.getDeviceID() == SwapDeviceID
				&& (thread.getTask().getStatus() == TaskLive))
			frame.setDirty(false);

		if ((task.getStatus() == TaskTerm) && frame.isReserved()) {

			frame.setUnreserved(task);
		}

		iorb.notifyThreads();

		Device device = Device.get(iorb.getDeviceID());
		device.setBusy(false);
		IORB dequeuedIORB = device.dequeueIORB();
		if (dequeuedIORB != null) {
			device.startIO(dequeuedIORB);
		}

		ThreadCB.dispatch();
	}

	/*
	 * Feel free to add methods/fields to improve the readability of your code
	 */

}

/*
 * Feel free to add local classes to improve the readability of your code
 */
