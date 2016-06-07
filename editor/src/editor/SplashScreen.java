/*
 *  Startup splash screen for map editor
 */

package editor;

import java.awt.*;
import javax.swing.*;

public class SplashScreen extends JWindow {
  private static final long serialVersionUID = 50108090449L;
  private final String IMAGE_PATH = "msnake_logo3_w373.png";
  
  public SplashScreen () {
    setLayout(new FlowLayout());
    
    Icon logo = createImageIcon(IMAGE_PATH, "Snake logo");
    JLabel lblLogo = new JLabel(logo);
    add(lblLogo);
    
    pack();
    setLocationRelativeTo(null);
  }
  
  /** Returns an ImageIcon, or null if the path was invalid. */
  protected ImageIcon createImageIcon (String path, String description) {
    java.net.URL imgURL = getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL, description);
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }
  
  public void show (final int millis) {
    Runnable r = new Runnable () {
      public void run () {
        setVisible(true);
        try {
          Thread.sleep(millis);
        } catch (InterruptedException e) {
          System.err.println(e);
        }
        setVisible(false);
      }
    };
    (new Thread(r)).start();
  }
  
}
