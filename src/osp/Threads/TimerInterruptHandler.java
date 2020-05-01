package osp.Threads;

import osp.IFLModules.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**    
       The timer interrupt handler.  This class is called upon to
       handle timer interrupts.

       @OSPProject Threads
*/
public class TimerInterruptHandler extends IflTimerInterruptHandler
{
    /**
       This basically only needs to reset the times and dispatch
       another process.

       @OSPProject Threads
    */
	
    //Author: Noura Al-Dakhil
  	//ID: 1614549
  	//Last Modification Date: 6/3/2020
    public void do_handleInterrupt() {
    	ThreadCB.dispatch(); //Schedules the next thread to run when the system timer expires
    }
}