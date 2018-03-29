package osp.Threads;


import osp.Devices.Device;
import osp.Hardware.HClock;
import osp.Hardware.HTimer;
import osp.IFLModules.Event;
import osp.IFLModules.IflThreadCB;
import java.util.TreeMap;
import java.util.Map;
import java.util.Vector;
import osp.Memory.MMU;
import osp.Resources.ResourceCB;
import osp.Tasks.TaskCB;
import osp.Utilities.MyOut;



/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.
   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
	private static Map<Integer,Long>initialSet = new TreeMap<Integer,Long>();
	private static Map<Integer,Long> endSet = new TreeMap<Integer,Long>();
    static private Vector<ThreadCB> threadList;
    static long threadcount;
	
	
    /**
       The thread constructor. Must call 
       	   super();
       as its first statement.
       @OSPProject Threads
    */
    
    //Default contructor
    public ThreadCB()
    {
    	super();
    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    
    
    public static void init()
    {
    	//The List which stores all the threads in ready state
        threadList = new Vector<ThreadCB>();
        threadcount = 0;
    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.
	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.
	@return thread or null
        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
    	//Check for MaxThreadsPerTask
    	if(task == null || task.getThreadCount() == MaxThreadsPerTask)
    	
    	{
    		ThreadCB.dispatch();
    		return null;
    	}
    	
        // New thread created and threadcount is incremented
    	ThreadCB th = new ThreadCB();
    	threadcount++;
    	
    	//Logging the clock time
    	System.out.println(HClock.get());
    	
    	//Assign thread to task and task to thread    	
    	th.setTask(task);
    	
    	
    	while(task.addThread(th) == FAILURE)
    	{
    		ThreadCB.dispatch();
    		return null;
    	}
    		
    		
    	
    	
    	//Set priority for priority scheduling algorithm
    	th.setPriority(task.getPriority());
    	
    	//Set status status to ThreadReady
    	th.setStatus(ThreadReady);
    	
    	//Add the new thread to the ready queue
        threadList.add(th);
    	
    	
    	
    	//Log both the ID and creation of the new thread to collect performance.
       initialSet.put(th.getID(), th.getCreationTime());
    	
       System.out.println(" the ID and creation time is "+th.getID() + th.getCreationTime());
    	
       //run dispatch on the new thread
       ThreadCB.dispatch();
    	
    	return th;
    }

    /** 
	Kills the specified thread. 
	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
    	//Create a Task
    	TaskCB task = this.getTask();
    	
    	if(this.getStatus() == ThreadReady)
    	{
    		//If the thread is in the ready queue just delete it.
    		threadList.remove(this);
    		
    	}
    	else if(this.getStatus() >= ThreadWaiting)
    	{
    		//Cancel all pending IO requests for the thread in waiting state
    		
    		int i=0;
    		while(i!=Device.getTableSize())
    		{
    			Device.get(i).cancelPendingIO(this);
    			i++;
    		}
    		
    	}
    	
    	else if(this.getStatus() == ThreadRunning)
    	{
    		//Set the current thread of the associated task to null
    		task.setCurrentThread(null);
    		
    		//Set the page table to null
    		MMU.setPTBR(null);
    		
    		//ThreadCB.dispatch();
    	}
    	
    	
    	
    	//Change status to killed
    	this.setStatus(ThreadKill);
    	
    	//release all shared resources to common pool which can be shared
    	//by other threads
    	ResourceCB.giveupResources(this);
    	
    	
    	//remove dead thread
    	task.removeThread(this);

    	
    	//Kill task if it has no other threads
    	if(task.getThreadCount() == 0)
    	{
    		task.kill();
    	}
    	
		ThreadCB.dispatch();
    }

	/** Suspends the thread that is currently on the processor on the 
        specified event. 
        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.
	@param event - event on which to suspend this thread.
        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
        // if the thread is running change status to waiting
    	if(this.getStatus() == ThreadRunning)
        {
    		this.setStatus(ThreadWaiting);
    		//set the memory management unit's page table to null
    		MMU.setPTBR(null);
    		this.getTask().setCurrentThread(null);
    	}
    	
    	// if ready then put it in waiting state
    	else if(this.getStatus() == ThreadReady)
    	{
    		this.setStatus(ThreadWaiting);
    	}
    	// if already waiting then increment the  waiting state by 1
    	else if(this.getStatus() >= ThreadWaiting){
    		this.setStatus(this.getStatus() + 1);
    	}
    	
    	if(!event.contains(this))
        	event.addThread(this);
    	
    	ThreadCB.dispatch();
    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
    	if(this.getStatus() == ThreadReady)
    	{   //If thread is ready just dispatch the thread
    		ThreadCB.dispatch();
    		return;
    	}
    	else if(this.getStatus() == ThreadWaiting)
    	{
    		//if waiting then move to ready queue
    		this.setStatus(ThreadReady);
    		threadList.add(this);
    	}
    	else{
    		//if already waiting decrement the waiting state level by 1
    		this.setStatus(this.getStatus() - 1);
    	}
    	
    	ThreadCB.dispatch();
    }

    /** 
        Selects a thread from the run queue and dispatches it. 
        If there is just one the read ready to run, reschedule the thread 
        currently on the processor.
        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE
        @OSPProject Threads
    */
    public static int do_dispatch()
    {
    	// Round Robin scheduling
    	//return doRoundRobin();
    	//return doFCFS();
    	
   	if(MMU.getPTBR() == null)
   	{
        	//First Come First Serve scheduling
        	return doFCFS();
    	}
   	
   	else 
   		return doRoundRobin();
   		
   	return SUCCESS;
    }
    
    private static int doRoundRobin() 
    {
    	ThreadCB previous = null;
    	ThreadCB newThread = null;
    	
    	//context switch the old thread with the new thread
    	try{
    		previous = MMU.getPTBR().getTask().getCurrentThread();
    		if(previous != null)
    		{
    			previous.setStatus(ThreadReady);
    			threadList.add(previous);
    			
    			//Set the task's current thread to null
    			previous.getTask().setCurrentThread(null);
    			MMU.setPTBR(null);
    		}
    	}catch(Exception e)
    	{
    		
    	}
		
    	//give CPU access to new thread
    	if(!threadList.isEmpty())
    	{
    		newThread = threadList.remove(0);
    		newThread.setStatus(ThreadRunning);
    		//Obtain the new thread's associated task
	    	MMU.setPTBR(newThread.getTask().getPageTable());
	    	newThread.getTask().setCurrentThread(newThread);
    	}
    	else{
    		//if no thread in ready queue do nothing
    		MMU.setPTBR(null);
    		return FAILURE;
    	}
    	
    	//Collect system performance
    	doCollectPerformance(newThread);
    	
    	//Generate interrupt after some quantum time
    	HTimer.set(1000);
    	
		return SUCCESS;
	}

    private static int doFCFS() {
    	ThreadCB thread = null;
		
    	if(!threadList.isEmpty()){
    		thread = threadList.remove(0);
    		thread.setStatus(ThreadRunning);
	    	MMU.setPTBR(thread.getTask().getPageTable());
	    	thread.getTask().setCurrentThread(thread);
    	}
    	else
    	{
    		MMU.setPTBR(null);
    		return FAILURE;
    	}
    	
    	//Collect system performance
    	doCollectPerformance(thread);
    	
		return SUCCESS;
	}
	
	
    
	 private static void doCollectPerformance(ThreadCB th) 
	 {
	    	
    	if(th == null)
    	{
    		return;
    	}
    	
    	Long startTime, endTime, responseTime;
    	double throughPut;
    	
    	if( !endSet.containsKey(th.getID()) )
    	{
    		
    		endSet.put(th.getID(), HClock.get());
    		
    		startTime = initialSet.get(th.getID());
    		endTime = endSet.get(th.getID());
    		responseTime = endSet.get(th.getID()) - initialSet.get(th.getID());
    		
    		if(HClock.get() == 0)
    		{
    			throughPut = 0.0;
    		}
    		else
    		{
    			throughPut = (double)threadcount*1000.0/(double)HClock.get();
    		}
    		
    		//Collect throughput and put it in log file
    		MyOut.print("osp.Threads.ThreadCB", "Clock Ticks Elapsed = "+ HClock.get() + " Number Of Threads = " +threadcount +" Throughput = " + throughPut);
    		
    		//Collect Response time and put it in log
    		MyOut.print("osp.Threads.ThreadCB", "Thread:"+th.getID()+" CreationTime = " + startTime + " First Scheduled Time = " + endTime + " Response Time = " + responseTime);
    	}
	}
	
	

	/**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.
       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning()
    {
        // your code goes here

    }

    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/