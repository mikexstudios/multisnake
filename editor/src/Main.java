/*
 *  Editor launcher
 */

import editor.*;

public class Main {

  public static void main (String[] argv) {
    
    SplashScreen ss = new SplashScreen();
    Editor e = new Editor();
    
    e.setVisible(true);
    ss.show(2500);
    
  }
  
}