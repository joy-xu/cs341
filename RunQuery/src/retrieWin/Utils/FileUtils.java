package retrieWin.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import fig.basic.LogInfo;

public class FileUtils {
	 public static void writeFile(Object data, String fileName) {
		  // Write to disk with FileOutputStream
		    try{
		    FileOutputStream f_out = new FileOutputStream(fileName);

		    // Write object with ObjectOutputStream
		    ObjectOutputStream obj_out = new ObjectOutputStream (f_out);

		    // Write object out to disk
		    obj_out.writeObject ( data);
		    }catch (Exception ex) {
		    	
		    }
	  }
	  
	  @SuppressWarnings("unchecked")
	public static Object readFile(String fileName) {
		// Read from disk using FileInputStream
	  try
	  {
	  FileInputStream f_in = new FileInputStream(fileName);

	  // Read object using ObjectInputStream
	  ObjectInputStream obj_in = new ObjectInputStream (f_in);

	  // Read an object
	  Object obj = obj_in.readObject();
	  return obj;
	  }catch(Exception ex) {
		  LogInfo.logs(ex.toString());
	  }
	  return null;
	}
}
