package igpp.tools;

import pds.util.PPIUtil;

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
		return PPIUtil.filename(mPathName);
	}
	
	public String getParent() 
	{
		return PPIUtil.parent(mPathName);
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