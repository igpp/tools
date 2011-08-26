package igpp.tools;

//import pds.label.*;
import pds.label.PDSLabel;
import pds.label.PDSItem;

import igpp.web.SMTPMail;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;

import java.lang.Thread;

import javax.mail.MessagingException;

/**
 * Monitor one or more directories for new files and perform a task on each.
 *
 * @author Todd King
 * @version 1.00 05/09/14
 */
public class Monitor extends Thread
{
	String	mVersion = "0.0.3";
	PDSLabel	mConfig = new PDSLabel();
	String		mConfigPathName = "monitor.cfg";
	long		mInterval = 60;	// Seconds
	String		mSMTPHost = "";
	String		mProject = "";
	String		mNotify = "";
	String		mFrom = "";
	boolean		mLog = false;
	LinkedList	mTaskList = new LinkedList();
	
	/**
	 * Command-line interface.
	 *
	 * @param args    comand-line arguments.
	 **/
    public static void main(String[] args) {
    	Monitor mon = new Monitor();
    	
    
   		System.out.println("Monitor version: "  + mon.mVersion);
    
    	mon.processArgs(args);	
    	if(!mon.loadConfig()) return;
    	
    	// Run task list at the specified interval
    	try {
	    	while(true) {
	    		mon.run();
				if(mon.mLog) System.out.println("Waiting: " + mon.mInterval + " seconds");		     	
	    		mon.sleep(mon.mInterval * 1000);	// Milli seconds
	    	}
    	} catch(Exception e) {
    		e.printStackTrace(System.out);
    	}
    }
    
    /**
     * Process arguments.
     *
     * Arguments are specified in a "name=value" syntax. For each argument parse
     * the string set the appropriate variable with the given value.
     *
     * @param args  the arguments to parse.
     **/
    void processArgs(String[] args)
    {
    	String	part[];
    	String	keyword;
    	String	value;
    	
    	for(int i = 0; i < args.length; i++) {
    		part = args[i].split("=", 2);
    		keyword = part[0];
    		value = "";
    		if(part.length > 1) value = part[1];
    		if(keyword.compareToIgnoreCase("CONFIG") == 0) {
    			mConfigPathName = value;	
    			continue;
    		}
    	}
    }
    
    /**
     * Load the currently defined configuration file.
     *
     * Recognized configuration information is:
     * <dd>interval: time in milliseconds between excutions of the monitor sequence.</dd>
     * <dd>smtp_host: the host name for sending e-mail message.</dd>
     * <dd>project: the name of the project.</dd>
     * <dd>notify: the e-mail addresses of who to notify with the output of the monitor sequence.</dd>
     * <dd>from: the from addresses for sent e-mail messages.</dd>
     * <dd>log: whether to log status (YES|NO)</dd>
     * <dd>task: one or more tasks to perform</dd>
     *
     **/
     boolean loadConfig()
     {
     	PDSItem		item;
     	PDSItem		block;
     	String		buffer;
     	
     	try {
	     	mConfig.parseXML(mConfigPathName);
     	} catch(Exception e) {
     		System.out.println("Unable to open configuration file: " + mConfigPathName);
     		e.printStackTrace(System.out);
     		return false;
    	}

	    block = mConfig.findObject("monitor");
	    
     	buffer = mConfig.getElementValueInObject("interval", block, true);
     	if(buffer.length() > 0) mInterval = Long.parseLong(buffer);
     	 
     	mSMTPHost = mConfig.getElementValueInObject("smtp_host", block, true);
     	mProject = mConfig.getElementValueInObject("project", block, true);
     	mNotify = mConfig.getElementValueInObject("notify", block, true);
     	mFrom = mConfig.getElementValueInObject("from", block, true);
     	buffer = mConfig.getElementValueInObject("log", block, true);
     	mLog = (buffer.compareToIgnoreCase("YES") == 0);
     	
	    boolean more = true;
	    item = null;
	    while(more) {
		    item = mConfig.findNextObject("task", item, block);
		    if(item == null) break;
		    if(!item.isValid()) break;
		    Task task = new Task();
		    task.setNotify(mNotify);
		    task.setFromLabel(mConfig, item);
		    task.setLog(mLog);
		    
		    // Transfer defaults if not set
		    if(task.getProject().length() == 0) task.setProject(mProject);
		    
		    mTaskList.add(task);
		}
		
		return true;
     }

    /*
     * Run all tasks.
     *
     * For each defined task, execute the task and collect the output. When all tasks
     * are complete, send an e-mail with the output to the designated recipient.
     */
     public void run()
     {
     	ListIterator it;
		Task	task;
		ArrayList	output;
		
     	it = mTaskList.listIterator();
     	while(it.hasNext()) {
     		task = (Task) it.next();
     		task.run();
     		if(task.mItemsProcessed == 0) continue;	// Nothing processed - no message
     		
     		try {
     			if(task.mLog) {
     				output = task.getOutput();
     				Iterator i = output.iterator();
     				while(i.hasNext()) {
     					System.out.println((String) i.next());
     				}	
     			}
		     	if(mSMTPHost.length() > 0) {
		     		output = task.getOutput();
		     		SMTPMail.send(mSMTPHost, task.getNotify(), task.getEmailSubject(), output, mFrom);
		     	}
     		} catch(Exception e) {
     			e.printStackTrace(System.out);
     		}
     	}
     }
     
}
