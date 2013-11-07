package mapreduce;

import java.io.*;

public class Logger 
{
    private String _fileLocation;
    
    public Logger(String fileLocation)
    {
        _fileLocation = fileLocation;
    }
    
    public void Log(String content)
    {
        try
        {
	        File file = new File(_fileLocation);
	
	        // If the file doesn't exists, then create it
	        if (!file.exists()) {
	            file.createNewFile();
	        }
	
	        FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
	        BufferedWriter bw = new BufferedWriter(fw);
	        
	        bw.write(content);
	        bw.close();
	
	        System.out.println("Done");
	
	    } 
	    catch (IOException e) 
	    {
	        e.printStackTrace();
	    }
    }
    
    public static void main(String args[])
    {
        Logger log = new Logger("/home/hadoop/Desktop/Output.txt");
        log.Log("This is the content to write into file");
    }
}