package osp.FileSystem;

import java.util.*;


import osp.IFLModules.*;
import osp.Devices.*;



/**
   The Mount table class provides the correct mapping of files to the physical devices
*/

public class MountTable extends IflMountTable
{
	
	 /**
    This method takes in pathname as the parameter and checks the mount table for 
    an entry for a device with an associated pathname and return its ID.
    While returning the Device ID, it takes into consideration the longest prefix which
    means that it is the best match in the mount table.
 */
 public static int do_getDeviceID(String pathname)
 {
    
        Vector<String> pathHierarchy=Path("root/" + pathname);
        int properMatch=0;
        int betterMatch=0;
        int immediateMatch;
        int i=0;
        
    	//To obtain the best possible match for the path hierarchy.
    	while(i!=Device.getTableSize())
        {
         Vector<String> mountHierarchy =Path("root/" + getMountPoint(i));
         immediateMatch=Match(pathHierarchy,mountHierarchy);
         if (immediateMatch > betterMatch) 
         {
             betterMatch = immediateMatch;
             properMatch = i;
         }
           i++;
    	}
    	return properMatch;
    }
 
 
    /**
       This method takes in the directory method as a parameter and checks the mount point for
       a valid entry in the mount table and if so returns true.
       
    */
    public static boolean do_isMountPoint(String dirname)
    {
    	int i=0;
    	
    	while(i!=Device.getTableSize())
    	
    	{
    		if(IflMountTable.getMountPoint(i).contains(dirname))
    			return true;
    		    i++;
    	}
    		
    	    return false;
    	
 }

    /**
     * This method takes in 2 parameters which are mount point and path and returns the 
     * exact match sequence.   
     */
    	
        //Match function to return the best match
        private static int Match(
             Vector<String> pathHierarchy, Vector<String> mountHierarchy)
         {
        	 int match=0;
             int i=0;
             
             while(i!=mountHierarchy.size())
             {
            	 if (i>=pathHierarchy.size()){
                     return 0;
                 } 
            	 else if (!pathHierarchy.get(i).equals(mountHierarchy.get(i)))
                 {
                     return 0;
                 }
                 match ++; 
                 i++;
           
             }
             return match;
         }
         
        /**
         *  This is a method which takes in pathname as parameter and return a string 
         *  which depicts the hierarchy of the pathname.
         */   
         
        //Obtain the heirarchy from pathname
         private static Vector<String> Path(String pathname)
         {
              
         
         Vector<String> hier = new Vector<String>();
         String[] Split = pathname.split("/");
         for (String directory :Split)
         {
             if (directory != null && directory.length() > 0)
             {
                 hier.add(directory);
             }
         }
         return hier;
         
         }
    }
    
        
   
    	
        
        
        
        
    
 



    


    
        
         

        

    
    
