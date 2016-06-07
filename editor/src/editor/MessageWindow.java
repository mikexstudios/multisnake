/*
 *  MessageWindow
 * 
 *  Convenience class for showing splash-style info to the user.
 *  Useful for displaying information while the program is busy,
 *  otherwise preventing user interaction (e.g. loading or saving a file).
 *
 *  Use inherited setVisible(boolean b) to show and hide the window.
 */

package editor;

import javax.swing.*;
import java.awt.*;

public class MessageWindow extends JWindow {
  private static final long serialVersionUID = 98294049L;
  JLabel lblMessage;
  
  /*
   *  Creates a new MessageWindow.
   *  The window will not be shown until setVisible is called.
   */
  public MessageWindow (String s) {
    super();
    setLayout(new GridLayout(1,1));
    setSize(500, 100);
    setLocationRelativeTo(null);
    
    lblMessage = new JLabel(s, JLabel.CENTER);
    Font font = new Font("Arial", Font.PLAIN, 22);
    lblMessage.setFont(font);
    lblMessage.setVerticalTextPosition(JLabel.CENTER);
    add(lblMessage);    
  }
  
  /*
   *  Changes the message text in the window.
   */
  public void setMessage (final String s) {
    Runnable r = new Runnable () {
      public void run () {
        lblMessage.setText(s);
        validate();
        repaint();
      }
    };
    SwingUtilities.invokeLater(r);
    Thread.yield();
  }
  
}
