package igpp.tools;

import java.io.File;

/**
 * Simplified interface for file information.
 *
 * @author Todd King
 * @version 1.00 2006
 */
public class FileItem extends Object
{
	String	mVersion = "0.0.1";
	
	String	mPathName		= "";
	boolean	mIsDirectory	= false;
	String	mName			= "";
	
    /*
     * Create instance
     */
    public FileItem(String pathName)
    {
    	setPathName(pathName);
    	mIsDirectory = false;
    }
	
	public void setPathName(String pathName) 
	{
		mPathName = pathName;
	}
	
	public String getPathName() 
	{
		return mPathName;
	}
	
	public String getName() 
	{
		File pathname = new File (mPathName);		
		return pathname.getName();
	}
	
	public String getParent() 
	{
		File pathname = new File (mPathName);		
		return pathname.getParent();
	}
	
	public String getBaseName() 
	{
		java.io.File file = new java.io.File (mPathName);		
		String base = file.getName();
		int n = base.lastIndexOf(".");
		if(n != -1) base = base.substring(0, n);
		
		return base;
	}
	
	public String getExtension() 
	{
		java.io.File file = new java.io.File (mPathName);		
		String ext = file.getName();
		int n = ext.lastIndexOf(".");
		if(n != -1) ext = ext.substring(n);
		else ext = "";
		
		return ext;
	}

	public void setIsDirectory(boolean state)
	{
		mIsDirectory = state;
	}
	
	public boolean isDirectory() 
	{
		return mIsDirectory;
	}
	
	public boolean isFile()
	{
		return (! mIsDirectory);
	}
}