
/**
* @author Konstantin Weixelbaum
*/
//native java libraries
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
//Google gson to parse json files
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GoEuroTest {

	public static void main(String[] args) throws Exception 
	{
		//If no argument is entered throw an exception and inform the user to enter one argument. An example is given 
		if(args.length==0)
		{
			throw new Exception("No Argument entered! Please enter one argument as in java -jar GoEuroTest.jar \"Berlin\"");
		}
		//else execute the function turnJsonInfoToCsv
		else
		{
				for (int i = 0; i < args.length; i++)
				{
					String QueryString=args[i];
					//call to the central function turnJsonInfoToCsv with the QueryStrings. The boolean true shows that the Querystring hasn't been changed since the Users input.
					turnJsonInfoToCsv(QueryString,true);	
				}
		}
	}
	
	//the function to turn JSON into CSV
	public static void turnJsonInfoToCsv(String Citystring, boolean exact)  throws Exception {
		
		//the API URL
		String theURL = "http://www.goeuro.com/GoEuroAPI/rest/api/v2/position/suggest/en/"+Citystring;
		
	    // Connect to theURL using the native java library
	    
		try{
			URL url = new URL(theURL);
		    HttpURLConnection request = (HttpURLConnection) url.openConnection();
		    request.connect();
	
		    // convert the input stream to a json element
		    JsonParser jsonparser = new JsonParser(); //from gson
		    JsonElement root = jsonparser.parse(new InputStreamReader((InputStream) request.getContent())); 
		    JsonArray rootArray = root.getAsJsonArray(); //may be an array, may be an object. 
		    
	    	//In case there are no search results try so long to shorten the Citystring until a result is found. 
		    if(rootArray.size()==0)
		    {
				//call again the function turnJsonInfoToCsv with a shortened Citystring. The boolean false shows that the Citystring has been changed since the Users input.
		    	if(Citystring.length()>1)
		    	{
			    	Citystring=Citystring.substring(0,Citystring.length()-1);
			    	turnJsonInfoToCsv(Citystring,false);
		    	}
	    		//if the search query even after shortening to 1 character doesn't return anything an error message is displayed.
		    	else
		    	{
		    		throw new Exception("There was nothing returned for your query.");
		    	}
		    }
		    else
		    {
	    		//if the Citystring was shortened and a result was found display the possibilities to the user. No csv file is created.
		    	if(!exact)
		    	{
		    		String ExceptionString="There was nothing returned for your search. Did you maybe mean ";
		    		 for (int i = 0; i < rootArray.size(); i++) 
		     	    {
		     	    	JsonObject rootobj = rootArray.get(i).getAsJsonObject();
		     	    	ExceptionString+="\r\n"+getKey(null,"name",rootobj);
		     	    }
		    		 throw new Exception(ExceptionString); 
		    	}
		    	else
		    	{
		    		//Declare which keys to fetch and if the key has a parent 
		    	    //for example latitude has the parent geo_position, it is a field in the geo_position object. 
		    	    //the name of the key is declared as the header for the resulting CSV file.
		    	    String[][] keyArray = new String[rootArray.size()+1][6];
		    	    String[] parentKey = new String[6];
		    	    
		    	    keyArray[0][0]="_type";
		    	    keyArray[0][1]="_id";
		    	    keyArray[0][2]="name";
		    	    keyArray[0][3]="type";
		    	    keyArray[0][4]="latitude";
		    	    parentKey[4]="geo_position";
		    	    keyArray[0][5]="longitude";
		    	    parentKey[5]="geo_position";

		    	    //loop over all resulting Elements for the search item Citystring
		    	    for (int i = 0; i < rootArray.size(); i++) 
		    	    {
		    	    	JsonObject rootobj = rootArray.get(i).getAsJsonObject();
		    	    	
		    	    	//loop over all keys that we want to catch and write the result into an 2-dimensional Array.
		    	    	for(int j = 0; j < keyArray[0].length; j++)
		    	    	{
		    	       		//call to a function getKey which returns the content of a certain key if it exists
		    	       		keyArray[i+1][j]=getKey(parentKey[j],keyArray[0][j],rootobj);
		    	    	}
		    	    }
		    	    
		    	    //create the CSV file from the 2-dimensional Array.
		    	    //The file is named after the search query Citystring
		    	    generateCsvFileFrom2DArray(Citystring+".csv",keyArray);
		    	    //Success message. CSV file is only created if the query was successful.
					System.out.println("Success! "+Citystring+".csv created");
		    	}
   
		    }
		}
		catch (IOException e)
		{
			System.out.println("Connection problem: ");
			e.printStackTrace();
		}
	}
	
	private static String getKey(String parent,String keyname, JsonObject rootobj)
	{
		String key="";
		
		//check if the key has a parent
		if(parent==null)
		{
			key=checkIfKeynameExists(keyname,rootobj);
		}
		else
		{
			//if the key has a parent resolve the object to look inside for the keyname
			JsonElement parentelement = rootobj.get(parent);
	        JsonObject parentobj = parentelement.getAsJsonObject();
	        key=checkIfKeynameExists(keyname,parentobj);
		}
		return key;
	}
	
	private static String checkIfKeynameExists(String keyname, JsonObject parentobj)
	{
		String key="";
		
		//check if the required keyname even exists in the object.
		if(parentobj.has(keyname))
	    {
			//return the contents of the field keyname as String.
			key=parentobj.get(keyname).getAsString();
	    }
		else
		{
			//return error message if keyname doesn't exist in this object.
			key="Key "+keyname+" not found";
		}
		return key;
	}
	
	//function to write the CSV file from the 2D Array with the native Java class BufferedWriter
	private static void generateCsvFileFrom2DArray(String sFileName,String[][] array)throws IOException
	{
	    BufferedWriter br = new BufferedWriter(new FileWriter(sFileName));
	    StringBuilder sb = new StringBuilder();
	    
	    //loop through the Elements and declare a new line for each including the header line with the keynames.
	    for(int i = 0; i < array.length; i++)
	    {
	    //in order to not have a trailing comma, declare prefix. 
	    String prefix = "";	
	    for (String content : array[i]) {	
	    	sb.append(prefix);
	    	prefix = ",";
	    	//seperate columns for each key in the CSV file.  
	    	sb.append(content);	
	    }
	    //new line for each Element in the CSV file.  
	    sb.append("\n\r");
	    }
	    br.write(sb.toString());
	    br.close();
	}
}