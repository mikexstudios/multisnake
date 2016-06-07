/*
 * ObstacleMap
 */

package editor;

import java.io.*;
import java.util.*;

public class ObstacleMap {
  private static final int MAXVAL = 255;  // used when writing map to file
  private char[][] map;
  private Map <String, String> properties;
  
  /*
   * Creates a new ObstacleMap by performing a deep copy of the elements in
   * the specified char matrix.
   */
  public ObstacleMap (char [][] c) {
    map = new char[c.length][c[0].length];
    for (int i = 0; i < c.length; i++) {
      for (int j = 0; j < c[0].length; j++) {
        map[i][j] = c[i][j];
      }
    }
  }
  
  /*
   * Creates a new ObstacleMap by reading the file at the specified path
   * and saving the map in this object.
   *
   * File properties, if found, will automatically be saved in the proporties Map.
   */
  public ObstacleMap (String path) {
    this(new File(path));
  }
  
  /*
   * Creates a new ObstacleMap by reading the specified file 
   * and saving the map in this object.
   *
   * File properties, if found, will automatically be saved in the properties Map.
   */
  public ObstacleMap (File file) {
    try {
      read(file);
    } catch (FileNotFoundException e) {
      System.err.println("Could find map file: " + file);
    } catch (IOException e) {
      System.err.println(e);
    }
  }
  
  /*
   * Reads the map within the specified File and stores the data in this ObjectMap.
   *
   * @throws FileNotFoundException if the specified File cannot be accessed
   * @throws IOException if there is a problem while reading the file
   */
  private void read(File file) throws FileNotFoundException, IOException {
    map = null;
    properties = null;
    
    FileReader fr = new FileReader(file);
    BufferedReader br = new BufferedReader(fr);
      
    String header = br.readLine();
    if (header.equals("P2")) {
      readP2(br);
    }
    else if (header.equals("P3")) {
      readP3(br);
    }
    else {
      System.err.println("File format [" + header + "] is not a recognized map type.");
    }
  }
  
  /*
   * Reads a P2 file given a prepared BufferedReader which has already read in the 
   * first line of the file.
   *
   * @throws IOException if a problem is encountered while reading.
   */
  private void readP2 (BufferedReader br) throws IOException {
    String line;
    // Check for file properties on the second line
    line = br.readLine();
    if (line.startsWith("#")) {
      parseProperties(line);
      line = br.readLine();
    }
    // Get dimensions
    int space_index = line.indexOf(" ");
    int cols = Integer.parseInt(line.substring(0, space_index));
    int rows = Integer.parseInt(line.substring(space_index+1));
    map = new char[rows][cols];
    // Get max value
    line = br.readLine();
    int maxval = Integer.parseInt(line);
    // Read values (one ASCII byte value per map block)
    int i=0, j=0;
    while (true) {
      line = br.readLine();
      if (line == null) {
        break;
      }
      int val = Integer.parseInt(line);
      map[i][j] = (char) val;
      j = (j + 1) % cols;
      if (j == 0) {
        i++;
      }
    }
  }
  
  /*
   * Reads a P3 file given a prepared BufferedReader which has already read in the 
   * first line of the file.
   *
   * @throws IOException if a problem is encountered while reading.
   */
  private void readP3 (BufferedReader br) throws IOException {
    String line;
    // Check for file properties on the second line
    line = br.readLine();
    if (line.startsWith("#")) {
      parseProperties(line);
      line = br.readLine();
    }
    // Get dimensions
    int space_index = line.indexOf(" ");
    int cols = Integer.parseInt(line.substring(0, space_index));
    int rows = Integer.parseInt(line.substring(space_index+1));
    map = new char[rows][cols];
    // Get max value
    line = br.readLine();
    int maxval = Integer.parseInt(line);
    // Read values (three ASCII byte values per map block)
    int i=0, j=0;
    while (true) {
      line = br.readLine();
      if (line == null) {
        break;
      }
      int r = Integer.parseInt(line);
      line = br.readLine();
      int g = Integer.parseInt(line);
      line = br.readLine();
      int b = Integer.parseInt(line);
      
      map[i][j] = (char)((r+g+b) / 3);
      j = (j + 1) % cols;
      if (j == 0) {
        i++;
      }
    }
  }
  
  /*
   * Saves this map as a P2 file.
   * 
   * @throws FileNotFoundException if the specified File cannot be created or modified.
   */
  public void write (File file) throws FileNotFoundException {
    PrintWriter writer = new PrintWriter(file); 
    // Write header
    writer.println("P2");
    writeProperties(writer);
    writer.println(map.length + " " + map[0].length);
    writer.println(MAXVAL + "");
    // Write map data
    for (int i = 0; i < map.length; i++) {
      for (int j = 0; j < map[0].length; j++) {
        int val = (int) map[i][j];
        writer.println(val);
      }
    }
    writer.close();
  }
  
  /*
   * Writes file properties of this ObstacleMap to the specified open PrintWriter.
   * If no properties are saved as instance data, an empty comment line, consisting of
   * only the "#" character, will be written.
   */
  private void writeProperties (PrintWriter writer) {
    writer.print("#");
    if (properties == null) {
      writer.println();
      return;
    }
    Iterator <Map.Entry <String, String>> it = properties.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry <String, String> entry = it.next();
      writer.print(entry.getKey() + "=" + entry.getValue());
      if (it.hasNext())
        writer.print("&");
    }
    writer.println();
  }
  
  /*
   * Parses a line of file properties and saves desired fields in the properties Map.
   */
  private void parseProperties (String line) {
    if (!line.startsWith("#"))
      return;
    this.properties = getPropertyMap(line);
  }

  /*
   * Returns a Map representing the properties found in a String. Keys and values
   * can contain whitespace and most non-alphanemuric characters except '&' 
   * and '=' which act as delimiters. Whitespace outside keys and values will be
   * ignored. However, a '#' must be the first character in the String -- no whitespace
   * may come before it.
   * 
   * Format:
   * #property1=value1&property2=value2&...&propertyn=valuen
   * or
   * # property1 = value1 & property2 = value2 & ... & propertyn = valuen
   * 
   * Whitespace will be ignored via the trim() method. Thus, the line
   * #    title=     My Map   &    creation date       =   1 Jan 2008
   * will result in the same key and value Strings after parsing as if it were written as 
   * #title=My Map&creation date=1 Jan 2008
   */
  private Map <String, String> getPropertyMap (String line) {
    Map <String, String> map = new HashMap <String, String> ();
    line = line.substring(1);  // skip over # character
    String [] pairs = line.split("&");
    for (String p : pairs) {
      int equalsIndex = p.indexOf("=");
      String key = p.substring(0, equalsIndex).trim();
      String value = p.substring(equalsIndex + 1, p.length()).trim();
      map.put(key, value);
    }
    return map;
  }
  
  /*
   * Returns the char matrix representation of this ObstacleMap
   */
  public char[][] getMap () {
    return map;
  }
  
  /*
   * Returns the entire Map of properties for this ObstacleMap. Changes made
   * to the returned Map will be saved as part of this object. 
   */
  public Map <String, String> getProperties () {
    return properties;
  }
  
  /*
   * Returns the requested property of this ObstacleMap, or null if the property
   * has not been set.
   */
  public String getProperty (String key) {
    if (properties != null)
      return properties.get(key);
    return null;
  }
  
  /*
   * Sets the Map of properties for this ObstacleMap. This will erase any 
   * existing properties. Only a shallow copy of the Map is made.
   */
  public void setProperties (Map <String, String> m) {
    this.properties = m;
  }
  
  /*
   * Sets the specified property for this ObstacleMap. This is a convenience method.
   * All properties in the Map will be written to the output file when write() is 
   * called on this object. 
   */
  public void setProperty (String key, String value) {
    if (properties == null)
      properties = new HashMap <String, String> ();
    properties.put(key, value);
  }
  
  
}