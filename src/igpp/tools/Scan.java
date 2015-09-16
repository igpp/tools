package igpp.tools;

//import org.apache.commons.cli.*;
import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

// Supported in Java 7+
//import java.nio.file.Files;
//import java.nio.file.attribute.BasicFileAttributes;
// import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;

/**
 * Tool to determine the maximum and minimum attributes for files in a branch of a file system.
 * 
 * @author Todd King
 *
 */
public class Scan {
	private String mVersion = "1.0.0";
	private String mOverview = "Tool to determine the maximum and minimum attributes for files in a branch of a file system. Attributes include access time, creation time, modified time and size."
			 ;
	private String mAcknowledge = "Development funded by NASA's VMO and PDS project at UCLA.";

	private boolean mVerbose= false;
	private boolean mRecursive = false;
	
	private long mModifiedDateMaximum = 0;
	private long mModifiedDateMinimum = Long.MAX_VALUE;
	private File mModifiedFileMinimum = null; 
	private File mModifiedFileMaximum = null; 
	
	private long mCreationDateMaximum = 0;
	private long mCreationDateMinimum = Long.MAX_VALUE;
	
	private long mAccessedDateMaximum = 0;
	private long mAccessedDateMinimum = Long.MAX_VALUE;

	private long mSizeMaximum = 0;
	private long mSizeMinimum = Long.MAX_VALUE;


	// create the Options
	Options mAppOptions = new org.apache.commons.cli.Options();

	ArrayList<String> mExclude = new ArrayList<String>();

	public Scan() {
		mAppOptions.addOption( "h", "help", false, "Display this text" );
		mAppOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step." );
		mAppOptions.addOption( "r", "recursive", false, "Recursive. Run on folder and all sub-folders.");
		mAppOptions.addOption( "n", "minimum", false, "Minimum. Show the smallest value found.");
		mAppOptions.addOption( "m", "maximum", false, "Maximum. Show the largest value found.");
		mAppOptions.addOption( "l", "label", false, "Label. Show labels for all values.");
		mAppOptions.addOption( "p", "path", false, "Path. Show path name of file for each value.");
		mAppOptions.addOption( "s", "show", true, "Show. Show one or more attributes. Provide a comma separated list. Choices are: modified, creation, accessed, size.");
	}

 	/** 
 	 * Command-line interface. 
 	 *
 	 * @param args  command-line arguments.
	 */
	public static void main(String[] args) 
	{	
		boolean showMaximum = true;
		boolean showMinimum = false;
		boolean showModified = true;
		boolean showCreation = false;
		boolean showAccessed = false;
		boolean showSize = false;
		boolean showLabel = false;
		boolean showPath = false;
		
		String	showValue = null;
		
		
		long counts[] = new long[2];
		counts[0] = 0L;	// files
		counts[1] = 0L;	// bytes
		
		Scan me = new Scan();

		if(args.length == 0) { me.showHelp(); return; }
		
		CommandLineParser parser = new PosixParser();
        try {
            CommandLine line = parser.parse(me.mAppOptions, args);

   			if(line.hasOption("h")) { me.showHelp(); return; }
   			if(line.hasOption("v")) me.mVerbose = true;
   			if(line.hasOption("r")) me.mRecursive = true;
   			if(line.hasOption("n")) showMinimum = true;   			
   			if(line.hasOption("m")) showMaximum = true;
   			if(line.hasOption("l")) showLabel = true;
   			if(line.hasOption("p")) showPath = true;
   			if(line.hasOption("s")) showValue = line.getOptionValue("s");
   			
   			// Parse showValue option
   			if(showValue != null) {
   				showModified = false;	// Turn off default
   				String[] part = showValue.split(",");
   				for(String opt : part) {
   					if(opt.equalsIgnoreCase("modified")) showModified = true;
   					else if(opt.equalsIgnoreCase("creation")) showCreation = true;
   					else if(opt.equalsIgnoreCase("accessed")) showAccessed = true;
   					else if(opt.equalsIgnoreCase("size")) showSize = true;
   					else System.out.println("Unknown show options value: " + opt);
   				}
   			}

   			if(line.getArgs().length == 0) {	// Default - do current folder
   				counts = me.scan(".");
   				// counts = me.scan(".", fileList);
   			} else {   			
	   			// Process source argument 
	   			for(String sourcePath : line.getArgs()) {
	   				long tempCounts[] = me.scan(sourcePath);
	   				counts[0] += tempCounts[0];	// File count
	   				counts[1] += tempCounts[1];	// Byte count
	   				if(me.mVerbose) {
	   					System.out.println("Scanned files: " + igpp.util.Text.toThousands(counts[0]));
	   					System.out.println("Scanned bytes: " + igpp.util.Text.toUnitizedBytes(counts[1]));
	   				}
	   				
	   				if(showMinimum) {
	   					if(showLabel) System.out.println("Minimum values:");
	   					if(showCreation) {
		   					if(showLabel) System.out.print("   Creation: ");
		   					if(me.mCreationDateMinimum == Long.MAX_VALUE) System.out.println(""); 
		   					else System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(me.mCreationDateMinimum));
	   					}
	   					if(showModified) {
		   					if(showLabel) System.out.print("   Modified: ");
		   					if(me.mModifiedDateMinimum == Long.MAX_VALUE) System.out.println(""); 
		   					else System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(me.mModifiedDateMinimum));
		   					if(showPath) System.out.println(me.mModifiedFileMinimum.getPath());
	   					}
	   					if(showAccessed) {
		   					if(showLabel) System.out.print("   Accessed: ");
		   					if(me.mAccessedDateMinimum == Long.MAX_VALUE) System.out.println(""); 
		   					else System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(me.mAccessedDateMinimum));
	   					}
	   					if(showSize) {
		   					if(showLabel) System.out.print("   Size    : ");
		   					System.out.println(igpp.util.Text.toUnitizedBytes(me.mSizeMinimum));
	   					}
	   					if(showLabel) System.out.println("");
	   				}
	   				if(showMaximum) {
	   					if(showLabel) System.out.println("Maximum values:");
	   					if(showCreation) {
		   					if(showLabel) System.out.print("   Creation: ");
		   					if(me.mCreationDateMaximum == 0) System.out.println(""); 
		   					else System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(me.mCreationDateMaximum));
	   					}
	   					if(showModified) {
		   					if(showLabel) System.out.print("   Modified: ");
		   					if(me.mModifiedDateMaximum == 0) System.out.println(""); 
		   					else System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(me.mModifiedDateMaximum));
		   					if(showPath) System.out.println(me.mModifiedFileMaximum.getPath());
	   					}
	   					if(showAccessed) {
		   					if(showLabel) System.out.print("   Accessed: ");
		   					if(me.mAccessedDateMaximum == 0) System.out.println(""); 
		   					else System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(me.mAccessedDateMaximum));
	   					}
	   					if(showSize) {
		   					if(showLabel) System.out.print("   Size    : ");
		   					System.out.println(igpp.util.Text.toUnitizedBytes(me.mSizeMaximum));
	   					}
	   				}
	   			}
   			}
   	    } catch(Exception e) {
   	        System.out.println(":: Problem performing task ::");
   	        System.out.println(e.getMessage());
   	        if(me.mVerbose) e.printStackTrace(System.out);
   	        return;
   	    }
   }

	/** 
 	 * Show the help for the tool. 
	 */
   public void showHelp()
   {
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
		System.out.println(mOverview);
		System.out.println("");
		System.out.println("Usage: java " + getClass().getName() + " [options] [file ...]");
		System.out.println("");
		System.out.println("If no file or folder is specified the current folder is used.");
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
	 * Scan a path and count the number of files and total bytes occupied. 
	 * 
	 * @param path the file system path to scan for files.
	 * 
	 * @return array containing file count and byte count.
	 * 
	 * @throws Exception	if any error occurs.
	 */
	public long[] scan(String path)
			throws Exception
	{
	   	// File name filter
	   	File filePath = new File(path);
	   	File[] list = new File[1];
	   	long byteCount = 0L;
	   	long fileCount = 0L;
	   	
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
					if(mRecursive) {
						long tempCounts[] = scan(item.getPath());
						fileCount += tempCounts[0];
						byteCount += tempCounts[1];
					}
				} else {
					fileCount++;
					
					byteCount += item.length();					
					if(byteCount < mSizeMinimum) mSizeMinimum = byteCount;
					if(byteCount > mSizeMaximum) mSizeMaximum = byteCount;				
					
					// Supported in java 7+
				   	// BasicFileAttributes attrib = Files.readAttributes(filePath.toPath(), BasicFileAttributes.class);;
					// long stamp = attrib.lastModifiedTime().to(TimeUnit.MILLISECONDS);
					
					long stamp = item.lastModified();
					if(stamp < mModifiedDateMinimum) { mModifiedDateMinimum = stamp; mModifiedFileMinimum = item; }
					if(stamp > mModifiedDateMaximum) { mModifiedDateMaximum = stamp; mModifiedFileMaximum = item; }
					
					/*	 Supported in Java 7+

					stamp = attrib.creationTime().to(TimeUnit.MILLISECONDS);
					if(stamp < mCreationDateMinimum) mCreationDateMinimum = stamp;
					if(stamp > mCreationDateMaximum) mCreationDateMaximum = stamp;
					
					stamp = attrib.lastAccessTime().to(TimeUnit.MILLISECONDS);
					if(stamp < mAccessedDateMinimum) mAccessedDateMinimum = stamp;
					if(stamp > mAccessedDateMaximum) mAccessedDateMaximum = stamp;
					*/
				}
			}
		}
		
	   	long counts[] = new long[2];
		counts[0] = fileCount;
		counts[1] = byteCount;
		return counts;
	}

	public boolean getVerbose() { return mVerbose;}
	public boolean setVerbose(boolean state) { mVerbose = state; return mVerbose;}
	
	public boolean getRecursive() { return mRecursive;}
	public boolean setRecursive(boolean state) { mRecursive = state; return mRecursive;}
	
	public ArrayList<String> getExclude() { return mExclude;}
	public ArrayList<String> setExclude(ArrayList<String> list) { mExclude.clear(); return addExclude(list); }
	public ArrayList<String> addExclude(ArrayList<String> list) { mExclude.addAll(list); return mExclude;}
}
