package igpp.tools;

// import java.io.*;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

// import java.net.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Copies the file at a URL to the local system.
 *
 * usage: webcp url dest
 *
 * @author Todd King
 * @version 1.00 06/01/13
 */
public class Webcp
{
	String	version = "1.0.0";
	
    public static void main(String[] args) {
    	Webcp web = new Webcp();
    
    	if(args.length < 2) {
   		System.out.println("webcp version: "  + web.version);
    		System.out.println("Copies a file located at a URL to the local system.");
    		System.out.println("usage: webcp url dest");
    		System.out.println("");
    		System.out.println("If 'dest' is a directory then the source file name is used as the");
    		System.out.println("destination file name.");
    		return;
    	}
    	
    	// If we reach here we have the options.
    	
		try {
			URL url  = new URL(args[0]);
			URLConnection con = url.openConnection();
			InputStream inStream = con.getInputStream();
			
			String	pathName = args[1];
			File	outFile = new File(pathName);
			if(outFile.isDirectory()) {	// Add source file name to destination path
				pathName += outFile.separator + url.getFile();
			}
			
			FileOutputStream outStream = new FileOutputStream(pathName);
			
			byte[]		c = new byte[1024];
			int			n;
			
			while ((n = inStream.read(c, 0, 1024)) != -1) {
				outStream.write(c, 0, n);
			}
			
			inStream.close();
			outStream.close();
			
		} catch(Exception e) {
			System.out.print("Unable to perform copy...");
			System.out.print(e.getMessage());
		}
	}
}