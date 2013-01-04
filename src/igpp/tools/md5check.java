package igpp.tools;

import igpp.util.Digest;

import java.io.File;
import java.io.FileFilter;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Tool to calculate, check and manage MD5 checksums on files.
 *
 **/
class md5check {
	boolean mRecurse = false;
	String mExclude = "";
	
	boolean mShowMatch = true;
	boolean mShowFailed = true;
	boolean mShowMissing = true;
	boolean mShowErrors = true;
	
	PrintStream	mOut = System.out;
	String mUpdatePath = null;
	
	/** 
	 * Create an instance
	 **/
 	public md5check() 
 	{
 	}
 
 	/** 
 	 * Command-line interface. 
 	 *
 	 * @param args  command-line arguments.
	 */
	public static void main(String[] args) 
	{
		md5check me = new md5check();
		
		me.setMessage("ofme");
		try {
			for(int i = 0; i < args.length; i++) {
				if(args[i].compareTo("-h") == 0) {	// help
					me.showHelp();
				} else if(args[i].compareTo("-r") == 0) {	// Recurse
					me.mRecurse = true;
				} else if(args[i].compareTo("-l") == 0) {	// Checksum list
					i++;
					if(i < args.length) me.checkList(args[i]);
				} else if(args[i].compareTo("-x") == 0) {	// Exclude for checks
					i++;
					if(i < args.length) me.mExclude = args[i];
				} else if(args[i].compareTo("-f") == 0) {	// Refresh a checksum list
					i++;
					if(i < args.length) me.refresh(args[i]);
				} else if(args[i].compareTo("-o") == 0) {	// Output filename for checksum list
					i++;
					if(i < args.length) me.setOutput(args[i], false);
				} else if(args[i].compareTo("-a") == 0) {	// Append checksums to file
					i++;
					if(i < args.length) me.setOutput(args[i], true);
				} else if(args[i].compareTo("-u") == 0) {	// Update checksum list
					i++;
					if(i < args.length) { me.mUpdatePath = args[i]; me.setOutput(args[i], true); }
				} else if(args[i].compareTo("-m") == 0) {	// Output flags
					i++;
					if(i < args.length) me.setMessage(args[i]);
				} else {	// Load resource at path
					if(args[i].startsWith("-")) { System.out.println("Unkown option: " + args[i]); continue; }
					if(args[i].startsWith("+")) { System.out.println("Unkown option: " + args[i]); continue; }
					me.checksum(args[i]);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
   }
   
 	/** 
 	 * Show the help for the tool. 
	 */
   public void showHelp()
   {
		System.out.println("Usage: " + getClass().getName() + " [options] [{file|path} {file|path}]");
		System.out.println("");
		System.out.println("Calculate, check and manage MD5 checksums on files.");
		System.out.println("");
		System.out.println("Options:");
		System.out.println("   -h        : dispay this text.");
		System.out.println("   -r        : recursively scan sub-directories.");
		System.out.println("   -l {file} : check the integrity of the checksums in {file}.");
		System.out.println("   -x {file} : exclude {file} from checksum generation.");
		System.out.println("   -f {file} : freshen checksum list in {file} by removing entries");
		System.out.println("               for missing files and updating checksums for changed files.");
		System.out.println("   -o {file} : write checksum list in {file}.");
		System.out.println("   -u        : update output (-o) checksum list by adding entries for new files.");
		System.out.println("   -m [o|f|m|e] : select the type of messages to output.");
		System.out.println("                    o: valid matches");
		System.out.println("                    f: failed matches");
		System.out.println("                    m: missing files");
		System.out.println("                    e: error");
   }
   
   /**
    * Set the output file for checksum list.
    * The output file is opened so that items are appended.
    *
    * @param pathname   the pathname to the output file.
    * @param append     indicator whether to append output to pathname (true)
    *                   or overwrite (false);
    **/
   public void setOutput(String pathname, boolean append)
   {
   	try {
	   	mOut = new PrintStream(new FileOutputStream(pathname, append));
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
		out.println(igpp.util.Digest.digestFile(pathname) + "  " + pathname);
   }
   
   /** 
    * Add or update (freshen) a checksum list file.
    *
    * Open the current checksum list file and search for a file. If present update the record 
    * with a newly calculated checksum. If not present add the file with checksum to the list.
    *
    * @param pathname  the pathname of the file to add or update.
    *
    * @return <code>true</code> is successful, otherwise <code>false</code> 
    **/
   public boolean addToList(String pathname) 
   	throws Exception
   {
   	String	buffer;
   	String[]	part;
   	int	cnt = 0;
   	String	tempPath;
   	
   	if(mUpdatePath == null) return true;	// No list to check
		File file = new File(mUpdatePath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

		// Search for word
		while((buffer = reader.readLine()) != null) {
			cnt++;
			buffer = buffer.trim();
			if(buffer.length() == 0) continue;	// Skip blank lines
			part = buffer.split(" ", 2);
			if(part.length < 2) {
				if(mShowErrors) System.out.println(mUpdatePath + ": Error on line " + cnt);
				continue;
			}
			if(part[1].length() < 2) {
				if(mShowErrors) System.out.println(mUpdatePath + ": Invalid file name on line " + cnt);
				continue;
			}
			tempPath = part[1].substring(1);
			if(pathname.compareTo(tempPath) == 0) { reader.close(); return false; }	// Already in list
		}
		
		reader.close();
		return true;
   }
   
	/** 
	 * Determine the checksum for a file or all files in a directory.
	 * 
	 * If recursion is enabled, all sub-directories are also checked.
	 * For each file the calculate a checksum and write a checksum record.
	 * If an update list is specified also update the entry in the specified list.
	 *
	 * @param path  the path to the file or directory to calculate the checksum.
	 *
	 **/
	public void checksum(String path)
		throws Exception
	{
		boolean add = true;
		
		if(path == null) return;
				
		// File name filter
	   File filePath = new File(path);
	   File[] list = new File[1];
	   
	   if(filePath.isDirectory()) {
		   list = filePath.listFiles(new FileFilter()	
		   	{ 
		   		public boolean accept(File pathname) { return (pathname.isFile() && pathname.getName().compareToIgnoreCase(mExclude) != 0); } 
		   	} 
		   	);
	   } else {
	   	list[0] = filePath;
	   }

		String resourcePath;
		if(list != null) {	// Found some files to process
			for(File item : list) {
				resourcePath = item.getPath(); // item.getCanonicalPath();
				try {
					add = addToList(resourcePath);
					if(add) writeChecksum(mOut, resourcePath);
				} catch(Exception e) {
					if(mShowErrors) {
						System.out.println(resourcePath + ": " + e.getMessage());
					}
				}
			}		
		}
		
		// Now recurse if asked to
		if(mRecurse) {
		   list = filePath.listFiles(new FileFilter()	
		   	{ 
		   		public boolean accept(File pathname) { return (pathname.isDirectory() && !pathname.getName().startsWith(".")); } 
		   	} 
		   	);
			if(list != null) {	// Found some files to process
				for(int y = 0; y < list.length; y++) {
					checksum(list[y].getPath());			
				}
			}
		}
	}

	/** 
	 * Determine if files in a checksum list have checksums 
	 * that match the value in the list.
	 *
	 * For each record in the file claculate the checksum and compare
	 * it to the stored checksum. Report the status of the comparison based
	 * on the current report settings. See {@link #setMessage(String flags)}
	 *
	 * @param path   the pathname of the file containing the checksum records.
	 **/
	public void checkList(String path)
		throws Exception
	{
		String	buffer;
		String[] part;
		String	pathName;
		String	digest;
		int		cnt = 0;
		
		File file = new File(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

		// Search for word
		while((buffer = reader.readLine()) != null) {
			cnt++;
			buffer = buffer.trim();
			if(buffer.length() == 0) continue;	// Skip blank lines
			part = buffer.split(" ", 2);
			if(part.length < 2) {
				if(mShowErrors) System.out.println(path + ": Error on line " + cnt);
				continue;
			}
			digest = part[0];
			if(part[1].length() < 2) {
				if(mShowErrors) System.out.println(path + ": Invalid file name on line " + cnt);
				continue;
			}
			pathName = part[1].substring(1);
			File test = new File(pathName);
			if(!test.exists()) {
				if(mShowMissing) System.out.println(pathName + " [MISSING]");
				continue;
			}
		
			if(digest.compareTo(igpp.util.Digest.digestFile(pathName)) == 0) { // Match
				if(mShowMatch) System.out.println(pathName + " [OK]");
			} else {	// Failed
				if(mShowFailed) System.out.println(pathName + " [FAILED]");
			}
		}	
		reader.close();	
	}

	/** 
	 * Refresh a checksum list. 
	 *
	 * If any checksum does not match then update the
	 * checksum value with the new calculated value. If an item is missing then 
	 * remove it from the list.
	 *
	 * @param path   the pathname of the file containing the checksum records.
	 **/
	public void refresh(String path)
		throws Exception
	{
		String	buffer;
		String[] part;
		String	pathName;
		String	digest;
		int		cnt = 0;

		// Create temp file.
      File temp = File.createTempFile("xxx", ".md5");
      System.out.println("Temp: " + temp.getCanonicalPath());
    
      // Delete temp file when program exits.
      // temp.deleteOnExit();
    
    	PrintStream out = new PrintStream(temp);
    	
		// Open list
		File file = new File(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

		// Check each entry
		while((buffer = reader.readLine()) != null) {
			cnt++;
			buffer = buffer.trim();
			if(buffer.length() == 0) continue;	// Skip blank lines
			part = buffer.split(" ", 2);
			if(part.length < 2) {
				if(mShowErrors) System.out.println(path + ": Error on line " + cnt);
				continue;
			}
			digest = part[0];
			if(part[1].length() < 2) {
				if(mShowErrors) System.out.println(path + ": Invalid file name on line " + cnt);
				continue;
			}
			pathName = part[1].substring(1);
			File test = new File(pathName);
			if(!test.exists()) {
				if(mShowMissing) System.out.println(pathName + " [REMOVE]");
				continue;
			}
			
			if(digest.compareTo(igpp.util.Digest.digestFile(pathName)) == 0) { // Match
				// 
			} else {	// Failed
				if(mShowFailed) System.out.println(pathName + " [UPDATE]");
			}

			writeChecksum(out, pathName);
		}
		
		// Close files
		reader.close();
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
}
