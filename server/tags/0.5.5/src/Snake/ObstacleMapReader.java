/**
* Eugene Marinelli
* ObstacleMapReader - reads an obstacle map from a raw image file.
*/

package Snake;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class ObstacleMapReader {
  // PPM map reader
  public static char[][] read(String filename) {
    FileReader fin = null;

    try {
      fin = new FileReader(new File(filename));
    } catch (FileNotFoundException e) {
      System.out.println("File \"" + filename + "\" not found.");
      return null;
    }

    char[][] map = null;

    try {
      BufferedReader br = new BufferedReader(fin);
      
      String line = br.readLine();
      if (!line.equals("P3")) {
        System.out.println("Invalid PPM header.");
        return null;
      }

      line = br.readLine();
      int space_index = line.indexOf(" ");
      int cols = Integer.parseInt(line.substring(0, space_index));
      int rows = Integer.parseInt(line.substring(space_index+1));
      
      map = new char[rows][cols];
      
      line = br.readLine();
      int maxval = Integer.parseInt(line);

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
    } catch (IOException e) {}

    return map;
  }

/*
  // Attempt at a windows bmp reader...
  public static char[][] read(String filename) {
    File img = new File(filename);
    FileInputStream fin = null;
    
    try {
      fin = new FileInputStream(img);
    } catch (FileNotFoundException e) {
      System.out.println("File \"" + filename + "\" not found.");
    }

    char[][] map = null;

    try {
      if (fin.read() != 66 || fin.read() != 77) {
        System.out.println("Invalid file header.");
        return null;
      }

      // Read past unneeded info.
      fin.read(new byte[8]);

      int bmpoffset = readMultibyteValue(fin, 4);
      
      fin.read(new byte[4]);
      
      int cols = readMultibyteValue(fin, 4);
      int rows = readMultibyteValue(fin, 4);
      map = new char[rows][cols];

      fin.read(new byte[114]);  //magic number

      // Read number of offset bytes into a buffer;
      int b;
      int r = 0, c = 0;
      while ((b = fin.read()) != -1) {
        System.out.println("setting map(" + r + "," + c + ") to " + b);
        map[r][c] = (char)b;
        c = (c + 1) % cols;
        if (c == 0) {
          r++;
        }
      }
    } catch (IOException e) {}

    return map;
  }
*/
}
