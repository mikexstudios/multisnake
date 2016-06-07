/*
 * MapFileFilter provides a filter to the JFileChooser for opening and saving map files
 */

package editor;

public class MapFileFilter extends javax.swing.filechooser.FileFilter {
  
  public MapFileFilter () {
    super();
  }
  
  public boolean accept (java.io.File f) {
    String name = f.getName();
    if (f.isDirectory())
      return true;
    if (name.endsWith(".ppm") || name.endsWith(".PPM"))
      return true;
    return false;
  }
  
  public String getDescription () {
    return "Snake Map (PPM) file";
  }
  
}
