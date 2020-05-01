package osp.Threads;
import java.util.Vector;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/ 

public class ThreadCB extends IflThreadCB 
{
	
	
    /**
       The thread constructor. Must call
       	   super();
       as its first statement.

       @OSPProject Threads
    */
	
    public ThreadCB() {
    	super();
    }

    public static GenericList readyQueue; //Creates the global variable "readyQueue" of type GenericList
    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    
    //Authors:                ID:
    //Orjwan Zaafarani        1506807
  	//Noura Al-Dakhil         1614549
  	//Last Modification Date: 5/3/2020 
    public static void init() {
    	readyQueue = new GenericList(); //Initializing the global variable "readyQueue" of type GenericList
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
    
    //Author:                 ID:
    //Noura Al-Dakhil         1614549
  	//Last Modification Date: 6/3/2020
    static public   do_create(TaskCB task) {
    	//Checks whether the given task is null or it already has the max number of threads.
        if (task == null || task.getThreadCount() == MaxThreadsPerTask) {
        	//If yes, dispatch is called and null is returned
        	dispatch();  
        	return null;
        }
        
        else {
        	//If no, a thread object is created using the default constructor ThreadCB()
        	ThreadCB thread = new ThreadCB(); //
        	
        	//Checks whether adding the created thread to the given argument task fails. 
        	if (task.addThread(thread) == FAILURE) {
        		//If yes, dispatch is called and null is returned
        		dispatch();
            	return null;
        	}
        	else {
        		//If no, the following procedures are done: 
        		thread.setPriority(task.getPriority()); //The thread inherits the given task's priority
            	thread.setStatus(ThreadReady);// After the thread's has been created, it's status is set to "ThreadReady" (ready to be dispatched)
            	thread.setTask(task); //Associates the given task as an argument with the newly created thread
        		readyQueue.append(thread); //The created thread is appended to the end of the list
        		dispatch(); //A new thread is dispatched to optimize CPU usage
        		return thread; //The created thread is returned
        	}  
        }
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
    
    //Author:                 ID:
  	//Noura Al-Dakhil         1614549
  	//Last Modification Date: 6/3/2020
    public void do_kill() {
    	//Checks whether the thread requested to be killed has status of "ThreadReady"
    	if (this.getStatus() == ThreadReady)
    		//If yes, the designated thread is removed from the readyQueue
    		readyQueue.remove(this); 
    	
    	//If no, it is checked whether the thread requested to be killed has status of "ThreadRunning" and it's what the OSP think is the current thread
    	else if(this.getStatus() == ThreadRunning && MMU.getPTBR().getTask().getCurrentThread() == this) {
    		MMU.setPTBR(null); //PTBR is set to null if condition applies
    		this.getTask().setCurrentThread(null); //Current thread of previously running task is set to null
    	}
    	
    	this.getTask().removeThread(this); //Disassociates the thread from its task to prevent attempts to kill a dead thread
    	this.setStatus(ThreadKill); //The given thread's status is set to "ThreadKill"
    	
    	//The device table was iterated to purge any IORB associated with the thread killed
    	for (int i = 0 ; i < Device.getTableSize() ; i++) {
    		Device.get(i).cancelPendingIO(this);
    	}
    	//All resourced held by the given thread were released to the common pool so other threads could use them
    	ResourceCB.giveupResources(this);
 
    	dispatch(); //A new thread is dispatched to optimize CPU usage    	
    	
    	//Checks whether the corresponding task has any remaining threads. If not, this task is killed
    	if(this.getTask().getThreadCount() == 0)
    		this.getTask().kill();
    	
    	
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
    
    //Author:                 ID:
    //Orjwan Zaafarani        1506807
    //Noura Al-Dakhil         1614549
  	//Last Modification Date: 6/3/2020
    public void do_suspend(Event event)
    {
    	//Checks whether the thread requested to be suspended has status of "ThreadRunning" and it's what the OSP think is the current thread
        if(this.getStatus() == ThreadRunning && MMU.getPTBR().getTask().getCurrentThread() == this) {
        	//If yes, the thread loses control of the CPU by: 
        	this.getTask().setCurrentThread(null); //Setting PTBR to null
        	MMU.setPTBR(null); //The corresponding task's current thread is set to null
        	this.setStatus(ThreadWaiting); //The thread is suspended to "ThreadWaiting"
        	event.addThread(this); //The thread is placed in the waiting queue of the given event
        }
        
        //If thread is already waiting, its status is incremented by 1
        else if (this.getStatus() >= ThreadWaiting) {
        	this.setStatus(this.getStatus()+1);
        	//If the thread is not in the "readyQueue", it's placed in the waiting queue of the given event
        	if(!readyQueue.contains(this)) {
            	event.addThread(this);
        	}		
        }
        dispatch(); //A new thread is dispatched to optimize CPU usage
    }
    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    
    //Author:                 ID:
    //Orjwan Zaafarani        1506807
  	//Last Modification Date: 5/3/2020
    public void do_resume()
    {
    	if(this.getStatus() < ThreadWaiting)
    		{ MyOut.print(this,"Attempt to resume "+ this + ", which wasn’t waiting");
    		return;
    	}
    	//Set thread’s status.
    	if (getStatus() == ThreadWaiting) {
    		setStatus(ThreadReady);
    	}
    	else if (getStatus() > ThreadWaiting)
    		setStatus(getStatus()-1);
    	
    	//If appropriate, append thread to the ready queue
    	if (getStatus() == ThreadReady)
    		readyQueue.append(this);
    	
    	dispatch();
    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one thread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    
    //Authors:          ID:
    //Orjwan Zaafarani  1506807
  	//Noura Al-Dakhil   1614549
  	//Last Modification Date: 8/3/2020
    public static int do_dispatch(){
    	ThreadCB prevThread = null;
    	
    	//Try and catch block employed to catch any null pointer exceptions
    	try {
    		prevThread = MMU.getPTBR().getTask().getCurrentThread();
    	} catch (NullPointerException e) {	
    	}
    	//Since FCFS is followed, any currently running thread is allowed to continue running until it finishes execution
    	if(prevThread != null) {
    		int prevStat = prevThread.getStatus();
    		if(prevStat == ThreadRunning)
        		return SUCCESS;
    	}
    	
    	//Checks whether the "readyQueue"is empty, meaning no threads are ready to be dispatched.
    	if(readyQueue.isEmpty()) {
    		//If empty, PTBR is set to null and this method returns FAILURE meaning the dispatch was unsuccessful 
    		MMU.setPTBR(null);
    		return FAILURE;
    	}
    	
    	else {
    		//If not empty, the following is done: 
    		ThreadCB thread = (ThreadCB) readyQueue.removeHead(); //Since FCFS is followed, the head of the "readyQueue" is removed to be dispatched. 
    		MMU.setPTBR(thread.getTask().getPageTable()); //The PTBR is set to point the the thread's page table
    		thread.getTask().setCurrentThread(thread); //The thread is set as the current thread of its task
    		thread.setStatus(ThreadRunning); //The dispatched thread's status is set to "ThreadRunning"
    		HTimer.set(0); //The interrupt timer is set to zero
    		return SUCCESS; //SUCCESS is returned meaning a thread has been successfully dispatched
    	}
    }
    
    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    

    public static void atError() {
    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning(){
    }
}