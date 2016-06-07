/*
 *  Represents a block on the map
 */ 

package editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class Block extends JPanel {
  private static final long serialVersionUID = 7038567L;
  public static final int DEFAULT_SIZE = 10;  // Size in pixels
  public static enum Type { EMPTY, WALL, FOOD };
  
  int x, y;  // Location in map measured left-to-right and top-to-bottom
  Type type;
  
  /*
   * Creates a new Block for the specified (x, y) location in the Editor (e)
   */
  public Block (int x, int y, Editor e) {
    super();
    this.x = x;
    this.y = y;
    this.type = Type.EMPTY;
    setBackground(getTypeColor(this.type));
    setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    addMouseListener(e.getMouseListener());
    addMouseMotionListener(e.getMouseListener());
    setPreferredSize(new Dimension(DEFAULT_SIZE, DEFAULT_SIZE));
  }
  
  /*
   * Returns the location of this Block in the map grid measured from the top-left corner
   */
  public Point getLocation () {
    return new Point(x, y);
  }
  
  /*
   * Returns an int representation of this Block object in the range 0-255
   */
  public int byteValue () {
    if (type == Type.EMPTY)
      return 255;
    return 0;
  }
  
  /*
   * Sets the Type of this Block (Wall, Food, Empty, etc)
   */
  public void setType (Type t) {
    this.type = t;
    repaint();
  }
  
  /*
   * Sets the Type of this block from a char value (0-255)
   */
  public void setType (char rgb) {
    if (rgb != 0)
      setType(Type.EMPTY);
    else if (rgb == 0)
      setType(Type.WALL);
    //else 
      //System.out.println("Read unknown block byte: " + (int)rgb);
  }
  
  /*
   * Translates a Type to its corresponding Color
   */
  private Color getTypeColor (Type t) {
    if (type == Type.EMPTY)
      return Color.WHITE;
    else if (type == Type.WALL)
      return Color.GRAY;
    else if (type == Type.FOOD)
      return Color.RED;
    else
      return Color.YELLOW;
  }
  
  public void paint (Graphics g) {
    setBackground(getTypeColor(this.type));
    super.paint(g);
  }
  
  
}
