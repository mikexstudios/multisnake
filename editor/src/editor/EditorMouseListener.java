/*
 *  Mouse Listener for Blocks in Map Editor
 */

package editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class EditorMouseListener implements MouseListener, MouseMotionListener {
  
  private Editor editor;
  
  public EditorMouseListener (Editor e) {
    this.editor = e;
  }
  
  public void mouseEntered (MouseEvent e) {
    editor.reportMouseEvent(e);
  }
  public void mouseExited (MouseEvent e) {}
  public void mouseClicked (MouseEvent e) {}
  public void mouseMoved (MouseEvent e) {}
  public void mousePressed (MouseEvent e) {
    editor.reportMouseEvent(e);
  }
  public void mouseReleased (MouseEvent e) {}
  public void mouseDragged (MouseEvent e) {}
  
}