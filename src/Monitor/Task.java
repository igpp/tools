package igpp.tools;

import java.io.File;
import java.lang.Thread;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import pds.util.PPIUtil;
import pds.util.PPIOption;
import pds.util.PPIProcess;
import pds.util.PPIVariableList;
import pds.label.*;


/**
 * Run a task on one or more files at a specified location.
 *
 * @author Todd King
 * @version 1.00 2006
 */
public class Task extends Thread
{
	String	mVersion = "0.0.1";
	
	String	mPathName;
	String	mFilter;
	ArrayList	mProcess = new ArrayList();
	String	mProject = null;
	String	mSubject = "Unknown";
	String	mSuccessMessage = "";
	String	mFailureMessage = "";
	boolean	mRecurse = false;
	ArrayList	mOutput = new ArrayList();
	ArrayList	mNotify = new ArrayList();
	String		mLister = "";
	boolean		mLog = false;
	ArrayList	mDirStack = new ArrayList();
	int			mItemsProcessed = 0;
	
	/**
	 * Command-line interface.
	 *
	 * @param args command-line arguments.
	 **/
    public static void main(String[] args) {
    	Task task = new Task();
   
   		System.out.println("Task version: "  + task.mVersion);
    }
    
    /**
     * Create instance
     **/
    void Task()
    {
    	mPathName = "";
    	mFilter = "";
    	mRecurse = false;
    }
    
    /**
     * Set the pathname to the executable for the task.
     *
     * @param pathName  the pathname to the executable.
     **/
    void setPathName(String pathName) 
    { 
    	mPathName = pathName; 
    }
    
    /**
     * Set the message associated with successful execution.
     *
     * @param message  the message text.
     **/
    void setSuccess(String message) 
    { 
    	mSuccessMessage = message; 
    }
    
    /**
     * Set the message associated with failure of the execution.
     *
     * @param message  the message text.
     **/
    void setFailure(String message) 
    { 
    	mFailureMessage = message; 
    }
    
    /**
     * Set list of recipients of messages.
     *
     * Recipients are given as a semi-colon separated list.
     *
     * @param notify  the list of recipients.
     **/
    void setNotify(String notify) 
    { 
    	String	buffer;
    	if(notify == null) return;
    	
    	String[]	part = notify.split(";");
    	for(int i = 0; i < part.length; i++) {
    		buffer = part[i].trim();
    		if(buffer.indexOf('@') != -1) mNotify.add(buffer);
    	}
    }
    
    /** 
     * Get the list of recipient to recieve notifications.
     *
     * @return {@link ArrayList} of recipients.
     **/
    public ArrayList getNotify()
    {
    	return mNotify;
    } 
    
    /** 
     * Add a process to the current task.
     *
     * @param process   the system path to the execuable process.
     **/
    void addProcess(String process) 
    { 
    	mProcess.add(process); 
    }
    
    /**
     * Set the name of the project.
     *
     * @param project  the project name.
     **/
    void setProject(String project) 
    { 
    	mProject = project; 
    }
    
    /** 
     * Get the name of the project. If not sent an empty string is returned.
     *
     * @return the project text.
     **/
    String getProject() 
    { 
    	if(mProject == null) return "";
    	return mProject; 
    }
    
    /** 
     * Set the name lister.
     *
     * @param the lister text.
     **/
    void setLister(String lister) 
    { 
    	mLister = lister; 
    }
    
    /** 
     * Get the name lister.
     *
     * @return the lister name.
     **/
    String getLister() 
    { 
    	return mLister; 
    }
    
    /** 
     * Set the subject line to use in e-mails.
     *
     * @param the subject text.
     **/
    void setSubject(String subject) 
    { 
    	mSubject = subject; 
    }
    
    /** 
     * Set the log state based on a string.
     *
     * @param log   the state text.
     **/
    void setLog(String log) 
    { 
    	mLog = igpp.util.Text.isTrue(log); 
    }
    
    /** 
     * Set the log state.
     *
     * @param log   the state text.
     **/
    void setLog(boolean log) 
    { 
    	mLog = log;
    }
    
    /** 
     * Get the subject line to use in e-mails.
     *
     * The subject line has the syntax project:message. 
     * The project is set with {@link #setProject} and the message set with {@link #setSubject}.
     *
     * @return  the subject text.
     **/
    String getEmailSubject()
    {
    	return mProject + ": " + mSubject;
    }
    
    /**
     * Set the file filter. Converts simple pattern syntax to regex format.
     * 
     * Recognized patterns are "?" for a single character and "*" for any sequence of characters.
     *
     * @param filer   the file filter pattern.
     **/
    void setFilter(String filter) 
    { 
    	// Fix-up tokens
    	filter = filter.replaceAll("\\.", "\\\\.");
    	filter = filter.replaceAll("\\?", ".");
    	filter = filter.replaceAll("\\*", ".*");
    	mFilter = filter; 
    }
    
    /** 
     * Set the recurse flag. 
     *
     * @param recurse   the state text.
     **/
    void setRecurse(String recurse) 
    { 
    	mRecurse = igpp.util.Text.isTrue(recurse);
    }
    
    /**
     * Set variables from a config file in PDSLabel format.
     *
     * @param label   the parsed PDSLabel.
     * @param context  the item that defines the context within the
     *                 label configuration information is located.
     **/
     public void setFromLabel(PDSLabel label, PDSItem context)
     {
		setLister(label.getElementValue("lister", context, true));
		setPathName(label.getElementValue("path", context, true));
		setFilter(label.getElementValue("filter", context, true));
		setRecurse(label.getElementValue("recurse", context, true));
		setNotify(label.getElementValue("notify", context, true));
		setProject(label.getElementValue("project", context, true));
		setSubject(label.getElementValue("subject", context, true));
		setSuccess(label.getElementValue("success", context, true));
		setFailure(label.getElementValue("failure", context, true));
		setLog(label.getElementValue("log", context, true));
		
		PDSItem item = null;
		PDSElement element;
		
		do {
			item = label.findNextItemInObject("run", item, context);
			element = label.getElement(item);
			if(element != null) addProcess(element.valueString(true));
		} while(element != null);
     }
     
     /** 
      * Retrieve the output array from the last run task.
      *
      * @return an {@link ArrayList} containing the task output.
      *
      **/
     public ArrayList getOutput()
     {
     	  return mOutput;
     } 
    
    /**
     * Run the task.
     *
     * Prepare the task environment and execute the defined task.
     **/
     public void run()
     {
     	  mOutput.clear();
     	  mItemsProcessed = 0;
     	  doTask(mPathName);
     }
     
     /**
      * Execute the application at a given path.
      *
      * If a lister is defined then perform the task on each file
      * returned by the lister. For each item processed pass the filename
      * basename, pathname, path and extension as seperate command line
      * arguments. Collect the output from the command. Check the exist 
      * status of the command to determine success or failure. An exist
      * status of 0 is considered success.
      *
      * @param pathName  the pathname to the executable.
      */
      public void doTask(String pathName)
      {
     	String	buffer;
     	String	absPath;
     	File	tempFile;
     	ArrayList	list = new ArrayList();
     	FileItem	item;
     	Iterator	listIt;
     	Iterator	procIt;
     	int			i;
     	boolean		success;
     	PPIProcess	process = new PPIProcess();
     	String[]	part;
     	
     	PPIVariableList variable = new PPIVariableList();
     	PPIVariableList command = new PPIVariableList();
     	
     	// Build up a list of files
     	if(mLog) System.out.println("Checking: " + pathName);
		list.clear();
		if(mLister.length() > 0) {
			command.findAndSet("path", pathName);
     		buffer = command.replaceVariable(mLister);
     		if(mLog) System.out.println("Lister: " + buffer);
     		process.run(buffer);
			ArrayList output = process.getOutput();
			if(output.size() == 0) return;	// Nothing to do
			Iterator it = output.iterator();
			while(it.hasNext()) {
				buffer = (String) it.next();
				part = buffer.split(" +", 9);
				if(part.length != 9) continue;	// Invalid "ls -l" format
				item = new FileItem(pathName + "/" + part[8]);
				item.setIsDirectory(buffer.charAt(0) == 'd');
				list.add(item);
			}
     	} else {
	     	File folder = new File(pathName);
	     	File[] fList = folder.listFiles();
	     	if(fList == null) {
	     		System.out.println("Unable to open folder: " + pathName);
	     		return;
	     	}
	     	if(fList.length == 0) return;	// Nothing to do
	     	for(i = 0; i < fList.length; i++) {
	     		item = new FileItem(fList[i].getAbsolutePath());
	     		item.setIsDirectory(fList[i].isDirectory());
	     		list.add(item);
	     	}
     	}
     	// Arrays.sort(list);
     	mItemsProcessed = list.size();
     	
     	// Process each file in the folder
     	listIt = list.iterator();
     	while(listIt.hasNext()) {
     		item = (FileItem) listIt.next();
     		if(item.isDirectory()) {
		     	// If recursion is requested - do it for each folder.
     			if(mRecurse) {
     				doTask(item.getPathName());
     			}
     			continue;	// We only do files
     		}
     		
     		variable.findAndSet("filename", item.getName());
     		variable.findAndSet("basename", PPIUtil.basename(item.getName()));
     		variable.findAndSet("extension", PPIUtil.extension(item.getName()));
     		variable.findAndSet("pathname", item.getPathName());
     		variable.findAndSet("path", item.getParent());
     		
     		if(mFilter.length() > 0) {	// Check if matches pattern
     			if(!item.getName().matches(mFilter)) continue;
     		}
     		
	     	// Run the process
	     	success = true;	
     		procIt = mProcess.iterator();
     		while(procIt.hasNext()) {
	     		buffer = variable.replaceVariable((String) procIt.next());
	     		if(mLog) System.out.println("Run: " + buffer);
	     		if(process.run(buffer) == 0) {	// Success
	     			if(mSuccessMessage.length() == 0) {
				     	mOutput.add("***********************************");
				     	mOutput.add("Success running: " + buffer);
				     	mOutput.add("***********************************");
				     	mOutput.addAll(process.getOutput());
	     			}
	     		} else {	// Failure
	     			success = false;
	     			if(mFailureMessage.length() == 0) {
				     	mOutput.add("***********************************");
				     	mOutput.add("Failed to run: " + buffer);
				     	mOutput.add("***********************************");
				     	mOutput.addAll(process.getOutput());
				     	break;	// Stop processing
	     			}
	     		}
     		}
     		if(success) {
     			if(mSuccessMessage.length() > 0) mOutput.add(mSuccessMessage);
     		} else {
     			if(mFailureMessage.length() > 0) mOutput.add(mFailureMessage);
     		}
     	}
     }
}
