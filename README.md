igpp.tools
======

Collection of tools for data movement and maintenance. Includes the following tools:

**igpp.tools.md5check** Calculate and maintain MD5 checksums for files. 

**igpp.tools.Monitor** Monitors locations in a file system and then executes scripts which files appear.

**igpp.tools.Scan** Scan portions of a file system and determine the minimum and maximum values for creation date, modification date and size.

**igpp.tools.Webcp** Copy files from the web given a URL to the local system. A very simple tool similar to curl or wget.

Documentation for each tool can be viewed by passing the "-h" options to the tool. 

Installation
======
To install the package download the zip file for the latest build from the "dist" folder.
Unzip the file where you would like the install the package. The package is self-contained
(it has the necessary support .jar files) so can be installed any where.

The distribution contains the following:

**README.txt**   
    A README file directing the user to the available documentation for the project.
	
**LICENSE.txt**   
    The copyright notice from the Regents of the University of California detailing the restrictions regarding the use and distribution of this software. Although the license is strictly worded, the software has been classified as Technology and Software Publicly Available (TSPA) and is available for anyone to download and use.
    
**bin/**   
    This directory contains batch and shell scripts for executing the tool.

**doc/**   
    This directory contains a local web site with documentation, api javadoc, and other related information. Just point the desired web browser to the index.html file in this directory.
	
**examples/**   
    This directory contains examples of using the tool.
	
**lib/**   
    This directory contains the dependent jar files for the tool along with the executable jar file (igpp-docgen-1.3.0.jar) containing the igpp.docgen tool software.
