package igpp.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

/**
 * Tool to calculate, check and manage MD5 checksums on files.
 *
 **/
class md5check {
	private String mVersion = "2.0.0";
	private String mOverview = "Calculate, check and manage MD5 checksums on files.";
	private String mAcknowledge = "Development funded by NASA's VMO and PDS projects at UCLA.";

	private static boolean mVerbose = false;

	// create the Options
	Options mAppOptions = new org.apache.commons.cli.Options();

	private HashSet<String> mSet = new HashSet<String>();

    private static boolean mRecurse = false;
	private static ArrayList<String> mExclude = new ArrayList<String>();
	
	private static boolean mShowMatch = true;
	private static boolean mShowFailed = true;
	private static boolean mShowMissing = true;
	private static boolean mShowErrors = true;
	
	PrintStream	mOut = System.out;
	
	private static long mCount = 0L;
	private static long mUpdateCount = 0L;
	private static int mThreadPool = 16;

    /**
     * Create an instance - initialize threads
     **/
    public md5check(int numberOfThreads) {
		mAppOptions.addOption( "h", "help", false, "Display this text" );
		mAppOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step." );
		mAppOptions.addOption( "r", "recursive", false, "Recursive. Recursively scan sub-directories." );
		
		mAppOptions.addOption( "t", "threads", true, "Threads. Number of threads to use. Default: " + mThreadPool );
		
		mAppOptions.addOption( "c", "check", true, "Check. Check the integrity of the checksums in a file." );
		mAppOptions.addOption( "x", "exclude", true, "Exclude. Exclude file from checksum generation.");
		mAppOptions.addOption( "f", "freshen", true, "Freshen. Freshen checksum list by removing entries for missing files and updating checksums for changed files.");
		mAppOptions.addOption( "o", "output", true, "Output. write checksum list to a file.");
		mAppOptions.addOption( "a", "add", true, "Add. Update checksum list by adding entries for new files.");
		mAppOptions.addOption( "s", "sort", true, "Sort. Sort a checksum file so that entries are alphabetical by file name.");
		mAppOptions.addOption( "m", "message", true, "Message. Select the type of messages to output. f: failed matches, o: valid matches, m: missing files, e: error. Default: fome");
		
		mAppOptions.addOption( "d", "debug", true, "Debug. Debug mode");		
    }
    
 
 	/** 
 	 * Command-line interface. 
 	 *
 	 * @param args  command-line arguments.
	 */
	public static void main(String[] args) 
	{
		md5check me = new md5check(10);

		String checkFile = null;
		String freshenFile = null;
		String updateFile = null;
		String sortFile = null;
		
		me.setMessage("fome");
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine line = parser.parse(me.mAppOptions, args);
			if (line.hasOption("h")) { me.showHelp(); return; }
			if (line.hasOption("v")) mVerbose = true;
			if (line.hasOption("r")) mRecurse = true;
			
			if (line.hasOption("t")) mThreadPool = Integer.parseInt(line.getOptionValue("t"));
			
			if (line.hasOption("c")) checkFile = line.getOptionValue("c");
			if (line.hasOption("x")) mExclude.add(line.getOptionValue("x"));
			if (line.hasOption("o")) { mExclude.add(line.getOptionValue("o")); me.setOutput(line.getOptionValue("o")); }
			if (line.hasOption("a")) updateFile = line.getOptionValue("a");
			if (line.hasOption("f")) freshenFile = line.getOptionValue("f");
			if (line.hasOption("s")) sortFile = line.getOptionValue("s");
			if (line.hasOption("m")) me.setMessage(line.getOptionValue("m"));

	        final long start = System.currentTimeMillis();
	       
			if(checkFile != null) {
				me.checklist(checkFile);
			} else if(freshenFile != null) {
				me.refresh(freshenFile);
			} else if(sortFile != null) {
				me.sort(sortFile);
			} else {	// Process args
				String rootPath = ".";
				
	  			if(line.getArgs().length > 0) {   // Start in passed folder		
	   				rootPath = line.getArgs()[0];
	   			}
	  			if(updateFile != null) me.update(updateFile, rootPath);
	  			else me.checksum(rootPath);
			}
			
            long elapsed = System.currentTimeMillis() - start;
            if(mVerbose) {
          	   System.out.println("Files processed: " + igpp.util.Text.toThousands(mCount));
         	   System.out.println("Updates made: " + igpp.util.Text.toThousands(mUpdateCount));
         	   System.out.println("Total time     : " + elapsed / 1000.0 + " (s)");
            }

		} catch(Exception e) {
			e.printStackTrace();
		}
   }
   
	/**
	 * Show the help for the tool.
	 */
	public void showHelp() {
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
		System.out.println(mOverview);
		System.out.println("");
		System.out.println("Usage: java " + getClass().getName() + " [options]");
		System.out.println("");
		System.out.println("Options:");

		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(getClass().getName(), mAppOptions);

		System.out.println("");
		System.out.println("Acknowledgements:");
		System.out.println(mAcknowledge);
		System.out.println("");
	}
   
   /** 
    * Output a checksum record.
    *
    * Write a checksum record in the standard format. This
    * format is compatible with the MD5deep utility.
    *
    * @param out  the {@link PrintStream} to write the output record.
    * @param pathname   The pathname of the file to determine the checksum and write in the record.
    *
    **/
   public void writeChecksum(PrintStream out, String pathname)
   	throws Exception
   {
	   // if(mVerbose) System.out.println(pathname);
	   out.println(igpp.util.Digest.digestFile(pathname) + "  " + pathname);
   }
   
   /**
    * Calculate the checksum for all files at a given path.
    * 
    * @param fileName the pathname of where to start calculating checksums
    */
   public void checksum(String fileName) 
   {
	   ExecutorService executor = Executors.newFixedThreadPool(mThreadPool);

	   checksumQ(executor, fileName);
	   
	   executor.shutdown();

	   while (!executor.isTerminated()) {
	        try {
	            Thread.sleep(2);	// Slight pause
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	   }
   }
   
   /**
    * Create a ChecksumTask and place it in the queue for the passed executor. 
    * 
    * @param executor	a configured ExecutorService.
    * @param pathname	the pathname of the file to calculate a checksum and write to an open file.
    */
   private void checksumQ(ExecutorService executor, String pathname)
   {
	   	File filePath = new File(pathname);
	   	File[] list = new File[1];
	   	if(filePath.isDirectory()) {
	   		list = filePath.listFiles(new FileFilter()	
	   			{ 
	   				public boolean accept(File pathname) { return (  ( ! pathname.equals(".")) && ( ! pathname.equals("..")) && ! igpp.util.Text.isInPrefixList(pathname.getName(), mExclude) ); } 
				} 
	   		);
	   	} else {
	   		list[0] = filePath;
	   	}
	
		if(list != null) {	// Found some files to process
			for(File item : list) {
				if(item.isDirectory()) {
					if(mRecurse) {
						checksumQ(executor, item.getPath());
					}
				} else {	// Calculate checksum
			    	  Runnable worker = new ChecksumTask(item);
			    	  executor.execute(worker);
				}
			}
		}
   }
   
   /**
    * Task to calculate a checksum on a file.
    * 
    * @author tking
    *
    */
   class ChecksumTask implements Runnable
   {
       public File item;

       /**
        * Instantiate a checksum task.
        * 
        * @param item the File that points to the item to check.
        */
       public ChecksumTask(File item) {
           this.item = item;
       }

       @Override
       public void run() {
    	   mCount++;
    	   String resourcePath = item.getPath(); // item.getCanonicalPath();
			try {
				// boolean add = addToList(resourcePath);
				// if(add) writeChecksum(mOut, resourcePath);
				writeChecksum(mOut, resourcePath);
				mUpdateCount++;
			} catch(Exception e) {
				if(mShowErrors) {
					System.out.println(resourcePath + ": " + e.getMessage());
				}
			}
       }
 
   }
   
   /** 
	* Determine if files in a checksum list have checksums 
	* that match the value in the list.
	*
	* For each record in the file calculate the checksum and compare
	* it to the stored checksum. Report the status of the comparison based
	* on the current report settings. See {@link #setMessage(String flags)}
	*
	* @param pathname   the pathname of the file containing the checksum records.
	* 
    * @throws IOException
    */
   public void checklist(String pathname) throws IOException 
   {
      ExecutorService executor = Executors.newFixedThreadPool(mThreadPool);
      
      String buffer;
      long cnt = 0L;
      
      File file = new File(pathname);
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

      // Scan file for entries 
      while((buffer = reader.readLine()) != null) {
    	  cnt++;
    	  buffer = buffer.trim();
    	  if(buffer.length() == 0) continue;	// Skip blank lines
    	  Runnable worker = new ChecklistTask(buffer, cnt);
    	  executor.execute(worker);
      }
      reader.close();

      executor.shutdown();

      while (!executor.isTerminated()) {
          try {
              Thread.sleep(2);	// Slight pause
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
      }
   }
   
   /**
    * Performs the task of validating a checksum record.
    * 
    * Note: Parameters are passed when instantiated. 
    * 
    * @author tking
    *
    */
   class ChecklistTask implements Runnable
   {
	   private String record;
	   private long lineNumber;
	   
	   /**
	    * Instantiate a task
	    * 
	    * @param record	the String containing a record from the checksum list.
	    * @param lineNumber	the line number in the checksum list associated with the record.
	    */
	   public ChecklistTask(String record, long lineNumber) 
	   {
		   this.record = record;
		   this.lineNumber = lineNumber;
	   }
	   
	   @Override
	   public void run() {
			String[] part = record.split(" ", 2);
			if(part.length < 2) {
				if(mShowErrors) System.out.println("[ERROR]  Invalid record on line " + lineNumber + ": " + record);
				return;
			}
			String digest = part[0];
			if(part[1].length() < 2) {
				if(mShowErrors) System.out.println("[ERROR]  Invalid file name on line " + lineNumber + ": " + record);
				return;
			}
			
			mCount++;	// Items processed
			String pathName = part[1].substring(1);
			File test = new File(pathName);
			if(!test.exists()) {
				if(mShowMissing) System.out.println("[MISSING] " + pathName);
				return;
			}
		
			
			try {
				if(digest.compareTo(igpp.util.Digest.digestFile(pathName)) == 0) { // Match
					if(mShowMatch) System.out.println("[OK]      " + pathName);
				} else {	// Failed
					if(mShowFailed) System.out.println("[FAILED]  " + pathName + " (line: " + lineNumber + ")");
				}
			} catch (Exception e) {
				if(mShowFailed) System.out.println("[FAILED]  " + pathName + " (line: " + lineNumber + ")");
				e.printStackTrace();
			}		   
	   }
   }

	/** 
	 * Refresh a checksum list. 
	 *
	 * If any checksum does not match then update the
	 * checksum value with the new calculated value. If an item is missing then 
	 * remove it from the list.
	 *
	 * @param path   the pathname of the file containing the checksum records.
	 * @throws FileNotFoundException, IOException 
	 **/
   public void refresh(String pathname) throws FileNotFoundException, IOException 
   {
	   ExecutorService executor = Executors.newFixedThreadPool(mThreadPool);

	   String	buffer;
	   long		cnt = 0L;
	   File file = new File(pathname);
	   
	   // Create temp file.
	   File temp = File.createTempFile("xxx", ".md5");
	    
	   // Delete temp file when program exits.
	   // temp.deleteOnExit();
	    
	   PrintStream out = new PrintStream(temp);
	   
	   BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

	   // Scan file for entries 
	   while((buffer = reader.readLine()) != null) {
		   cnt++;
		   buffer = buffer.trim();
		   if(buffer.length() == 0) continue;	// Skip blank lines
		   Runnable worker = new RefreshTask(buffer, cnt, out);
		   executor.execute(worker);
	   }
	   reader.close();
   
	   executor.shutdown();

	   while (!executor.isTerminated()) {
	        try {
	            Thread.sleep(2);	// Slight pause
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	   }
	   
		// Close files
		out.close();
		
		// Now copy temporary file to checksum file.
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
		out = new PrintStream(new FileOutputStream(file));
		
		// Check each entry
		while((buffer = reader.readLine()) != null) {
			out.println(buffer);
		}
		reader.close();
		out.close();

   }

   class RefreshTask implements Runnable
   {
	   private String record;
	   private long lineNumber;
	   private PrintStream out;
	   
	   /**
	    * Instantiate a task
	    * 
	    * @param record	the String containing a record from the checksum list.
	    * @param lineNumber	the line number in the checksum list associated with the record.
	    */
	   public RefreshTask(String record, long lineNumber, PrintStream out) 
	   {
		   this.record = record;
		   this.lineNumber = lineNumber;
		   this.out = out;
	   }
	   
	   @Override
	   public void run() {
			String[] part;
			String	pathName;
			String	digest;

			part = record.split(" ", 2);
			if(part.length < 2) {
				if(mShowErrors) System.out.println("[ERROR]  Invalid record on line " + lineNumber + ": " + record);
				return;
			}
			digest = part[0];
			if(part[1].length() < 2) {
				if(mShowErrors) System.out.println("[ERROR]  Invalid file name on line " + lineNumber + ": " + record);
				return;
			}

			mCount++;	// Items processed
			pathName = part[1].substring(1);
			File test = new File(pathName);
			if(!test.exists()) {
				if(mShowMissing) System.out.println("[REMOVE] " + pathName);
				return;
			}
			
			try {
				if(digest.compareTo(igpp.util.Digest.digestFile(pathName)) == 0) { // Match
					// 
				} else {	// Failed
					if(mShowFailed) System.out.println("[UPDATE] " + pathName);
				}

				writeChecksum(out, pathName);
				mUpdateCount++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}

   public void loadChecksum(String filename) throws IOException 
   {	 
	   String[] part;
	   String record;
	   long lineNumber = 0L;
	   
	   BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));

	   mSet.clear();
	   
	   // Scan file for entries 
	   while((record = reader.readLine()) != null) {
		   lineNumber++;
		   record = record.trim();
		   if(record.length() == 0) continue;	// Skip blank lines
		   part = record.split(" ", 2);
			if(part.length < 2) {
				if(mShowErrors) System.out.println("[ERROR]  Invalid record on line " + lineNumber + ": " + record);
				continue;
			}
			mSet.add(part[1].trim());	// Add file name to set
	   }
	   reader.close();
	   
	   if(mVerbose) System.out.println("Records Processed: " + igpp.util.Text.toThousands(lineNumber));
   }


   public void sort(String filename) throws IOException 
   {	 
	   String[] part;
	   String record;
	   long lineNumber = 0L;
	   
	   BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));

	   TreeMap<String, String> map = new TreeMap<String, String>();
	   
	   // Scan file for entries 
	   while((record = reader.readLine()) != null) {
		   lineNumber++;
		   record = record.trim();
		   if(record.length() == 0) continue;	// Skip blank lines
		   part = record.split(" ", 2);
			if(part.length < 2) {
				if(mShowErrors) System.out.println("[ERROR]  Invalid record on line " + lineNumber + ": " + record);
				continue;
			}
			map.put(part[1].trim(), record);	// Add file name and hash to map
	   }
	   reader.close();
	   
	   // Now open and write values is sort order
	   PrintStream out = new PrintStream(new FileOutputStream(filename));
	   
	   for(String value : map.values()) {
		   out.println(value);
	   }
	   
	   out.close();
	   
	   
	   if(mVerbose) System.out.println("Records Processed: " + igpp.util.Text.toThousands(lineNumber));
   }
   

   /**
    * Calculate the checksum for all files at a given path.
    * 
    * @param fileName the pathname of where to start calculating checksums
 * @throws IOException 
    */
   public void update(String fileName, String rootPath) throws IOException 
   {
	   ExecutorService executor = Executors.newFixedThreadPool(mThreadPool);

	   loadChecksum(fileName);
	   
	   // Open checksum file for append
		try {
		   	mOut = new PrintStream(new FileOutputStream(fileName, true));
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}

	   updateQ(executor, rootPath);
	   
	   executor.shutdown();

	   while (!executor.isTerminated()) {
	        try {
	            Thread.sleep(2);	// Slight pause
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	   }
   }
   
   /**
    * Create a ChecksumTask and place it in the queue for the passed executor. 
    * 
    * @param executor	a configured ExecutorService.
    * @param pathname	the pathname of the file to calculate a checksum and write to an open file.
    */
   private void updateQ(ExecutorService executor, String pathname)
   {
	   	File filePath = new File(pathname);
	   	File[] list = new File[1];
	   	if(filePath.isDirectory()) {
	   		list = filePath.listFiles(new FileFilter()	
	   			{ 
	   				public boolean accept(File pathname) { return (  ( ! pathname.equals(".")) && ( ! pathname.equals("..")) && ! igpp.util.Text.isInPrefixList(pathname.getName(), mExclude) ); } 
				} 
	   		);
	   	} else {
	   		list[0] = filePath;
	   	}
	
		if(list != null) {	// Found some files to process
			for(File item : list) {
				if(item.isDirectory()) {
					if(mRecurse) {
						updateQ(executor, item.getPath());
					}
				} else {	// Calculate checksum
			    	  Runnable worker = new UpdateTask(item);
			    	  executor.execute(worker);
				}
			}
		}
   }
   
   /**
    * Task to calculate a checksum on a file.
    * 
    * @author tking
    *
    */
   class UpdateTask implements Runnable
   {
       public File item;

       /**
        * Instantiate a checksum task.
        * 
        * @param item the File that points to the item to check.
        */
       public UpdateTask(File item) {
           this.item = item;
       }

       @Override
       public void run() {
    	   mCount++;
    	   String resourcePath = item.getPath(); // item.getCanonicalPath();
    	   if(mSet.contains(resourcePath)) return;	// Already in list
    	   
			try {
				// boolean add = addToList(resourcePath);
				// if(add) writeChecksum(mOut, resourcePath);
				writeChecksum(mOut, resourcePath);
				mUpdateCount++;
			} catch(Exception e) {
				if(mShowErrors) {
					System.out.println(resourcePath + ": " + e.getMessage());
				}
			}
       }
 
   }
   /**
    * Set the output file for checksum list.
    * The output file is opened so that items are appended.
    *
    * @param pathname   the pathname to the output file.
    **/
   public void setOutput(String pathname)
   {
		try {
		   	mOut = new PrintStream(new FileOutputStream(pathname, false));
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
   }
   
   /**
    * Set what messages to output based on character codes.
    * 
    * Supported flags character codes are:
    *   <dt>o: Matching items, status of "OK". Checksums match.</dt>
    *   <dt>f: Failed. Checksums do not match.</dt>
    *   <dt>m: Missing. File is not present.</dt>
    *   <dt>e: Errors. Read or calculation errors.</dt>
    *
    * @param flags    sequence of flag character codes.
    **/
   public void setMessage(String flags)
   {
	   	mShowMatch = false;
	   	mShowFailed = false;
	   	mShowMissing = false;
	   	mShowErrors = false;
	   	
	   	for(int i = 0; i < flags.length(); i++) {
	   		if(flags.charAt(i) == 'o') mShowMatch = true;
	   		if(flags.charAt(i) == 'f') mShowFailed = true;
	   		if(flags.charAt(i) == 'm') mShowMissing = true;
	   		if(flags.charAt(i) == 'e') mShowErrors = true;
	   	}
   }
}
