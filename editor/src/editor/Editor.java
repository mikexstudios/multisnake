/*
 *  Snake Map Editor 
 */

package editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;

public class Editor extends JFrame implements ActionListener, ChangeListener {
  
  private static final long serialVersionUID = 457493L;
  private static final int WIDTH = 800;
  private static final int HEIGHT = 600;
  
  JPanel pnlMap, pnlMapHolder;
  JButton btnNew, btnOpen, btnSave, btnProperties, btnQuit;
  JRadioButton radWall, radFood, radEraser;
  JRadioButton radOne, radTwo, radThree;
  JSlider sliZoom;
  JLabel lblZoom;
  JFileChooser fileChooser;
  Block [][] map;  // graphical representation of the map
  Map <String, String> properties;
  EditorMouseListener mouseListener;
  
  public Editor() {
    super("Snake Map Editor");
    setLayout(new BorderLayout());
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    // Create a single mouse listener for all blocks in map
    mouseListener = new EditorMouseListener(this);
    
    // Use single file chooser to open and save files (will remember path)
    fileChooser = new JFileChooser();
    fileChooser.addChoosableFileFilter(new MapFileFilter());
    
    // Create a "holder" panel to allow the contained map to resize itself
    pnlMapHolder = new JPanel();
    JScrollPane spMap = new JScrollPane(pnlMapHolder,
                                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    spMap.setBorder(BorderFactory.createTitledBorder("Map"));    
    
    // Side panel contains all controls
    JPanel pnlControl = new JPanel();
    pnlControl.setLayout(new GridLayout(4, 1));
    pnlControl.setBorder(BorderFactory.createTitledBorder("Options"));
    
    // Create all buttons
    JPanel pnlButtons = new JPanel();
    pnlButtons.setLayout(new GridLayout(4,1));
    btnNew = new JButton("New");
    btnOpen = new JButton("Open ...");
    btnSave = new JButton("Save ...");
    btnProperties = new JButton("Properties");
    btnQuit = new JButton("Quit");
    pnlButtons.add(btnNew);
    pnlButtons.add(btnOpen);
    pnlButtons.add(btnSave);
    pnlButtons.add(btnProperties);
    pnlButtons.add(btnQuit);
    getRootPane().setDefaultButton(btnNew);
    btnSave.setEnabled(false);
    btnProperties.setEnabled(false);
    btnNew.addActionListener(this);
    btnOpen.addActionListener(this);
    btnSave.addActionListener(this);
    btnProperties.addActionListener(this);
    btnQuit.addActionListener(this);
    
    // Create controls for tool size and tool type
    JPanel pnlToolType = new JPanel();
    pnlToolType.setLayout(new GridLayout(3,1));
    pnlToolType.setBorder(BorderFactory.createTitledBorder("Select Tool"));
    ButtonGroup bgToolType = new ButtonGroup();
    radWall = new JRadioButton("Wall");
    radFood = new JRadioButton("Food");
    radFood.setEnabled(false);
    radEraser = new JRadioButton("Eraser");
    bgToolType.add(radWall);
    bgToolType.add(radFood);
    bgToolType.add(radEraser);
    pnlToolType.add(radWall);
    pnlToolType.add(radFood);
    pnlToolType.add(radEraser);
    radWall.setSelected(true);
    JPanel pnlToolSize = new JPanel();
    pnlToolSize.setLayout(new GridLayout(3,1));
    pnlToolSize.setBorder(BorderFactory.createTitledBorder("Tool Size"));
    ButtonGroup bgToolSize = new ButtonGroup();
    radOne = new JRadioButton("One Block");
    radTwo = new JRadioButton("Two Blocks");
    radThree = new JRadioButton("Three Blocks");
    bgToolSize.add(radOne);
    bgToolSize.add(radTwo);
    bgToolSize.add(radThree);
    pnlToolSize.add(radOne);
    pnlToolSize.add(radTwo);
    pnlToolSize.add(radThree);
    radOne.setSelected(true);
    
    // Create zoom controller
    JPanel pnlZoom = new JPanel();
    pnlZoom.setLayout(new  GridLayout(2,1));
    pnlZoom.setBorder(BorderFactory.createTitledBorder("Zoom Level"));    
    sliZoom = new JSlider(5, 30, 10);
    sliZoom.setMajorTickSpacing(5);
    sliZoom.setPaintTicks(true);
    sliZoom.addChangeListener(this);
    lblZoom = new JLabel("  Block Size: " + sliZoom.getValue());
    pnlZoom.add(lblZoom);
    pnlZoom.add(sliZoom);
    
    // Add all controls to main interface
    pnlControl.add(pnlButtons);
    pnlControl.add(pnlToolType);
    pnlControl.add(pnlToolSize);
    pnlControl.add(pnlZoom);
    
    add(spMap, BorderLayout.CENTER);
    add(pnlControl, BorderLayout.EAST);
    
    setSize(WIDTH, HEIGHT);
    setLocationRelativeTo(null);  //center window on screen
    clearMap();
  }
  
  /*
   * Removes the current map from view and displays info message in its place.
   * 
   * TODO: prompt for save before closing, if a map is open.
   */
  private void clearMap () {
    map = null;
    pnlMap = new JPanel();
    pnlMapHolder.removeAll();
    pnlMapHolder.setLayout(new FlowLayout());
    JLabel lblClear = new JLabel("Select 'New' to create a new map or 'Open' to open an existing one");
    Font f = new Font("Arial", Font.PLAIN, 18);
    lblClear.setFont(f);
    pnlMapHolder.add(lblClear);
    properties = new HashMap <String, String> ();
    btnSave.setEnabled(false);
    btnProperties.setEnabled(false);
  }
  
  /*
   * Creates a new (empty) map, asking for user input to determine the size and properties.
   */
  private void createMap () {
    clearMap();
    PropertiesDialog.prompt(this);
  }
  
  /*
   * Generates a map from its byte representation.
   *
   * If a non-null MessageWindow is specified, the window will be updated as
   * the map is created.
   */
  private void createMap (ObstacleMap om, MessageWindow window) {
    char [][] newmap = om.getMap();
    pnlMap = new JPanel();
    pnlMap.setLayout(new GridLayout(newmap.length, newmap[0].length));
    pnlMapHolder.removeAll();
    pnlMapHolder.setLayout(new FlowLayout());
    pnlMapHolder.add(pnlMap);
    map = new Block[newmap.length][newmap[0].length];
    for (int i = 0; i < newmap.length; i++) {
      if (window != null) {
        int percent = 100 * i / newmap.length;
        if (window != null)
          window.setMessage("Creating map objects  " + percent + "% ...");
        Thread.yield();
      }
      for (int j = 0; j < newmap[0].length; j++) {
        map[i][j] = new Block(i, j, this);
        map[i][j].setType(newmap[i][j]);
        pnlMap.add(map[i][j]);
      }
    }
    pnlMapHolder.revalidate();
    pnlMapHolder.repaint();
    btnSave.setEnabled(true);
    btnProperties.setEnabled(true);
    properties = om.getProperties();
    Thread.yield();
  }
  
  /*
   * Resizes the map to fit the specified width and height.
   * Any part of the map that is "cut off" due to this resizing will be lost.
   * If no map is currently loaded, a new one one will be created.
   *
   * If a non-null MessageWindow is specified, the window will be updated as
   * the map is created.
   */
  public void resizeMap (Dimension dim, MessageWindow window) {
    // If the dimensions have not changed, do nothing
    if (map != null && map.length == dim.height && map[0].length == dim.width)
      return;
    
    // Get rid of the old map display
    pnlMapHolder.removeAll();
    pnlMapHolder.setLayout(new FlowLayout());
    pnlMap = new JPanel();
    pnlMap.setLayout(new GridLayout(dim.height, dim.width));
    
    if (map == null) {
      // Create new map
      map = new Block[dim.height][dim.width];
      for (int i = 0; i < dim.height; i++) {
        int percent = 100 * i / dim.height;
        if (window != null)
          window.setMessage("Creating map objects  " + percent + "% ...");
        for (int j = 0; j < dim.width; j++) {
          map[i][j] = new Block(i, j, this);
          pnlMap.add(map[i][j]);
        }
      }
    } else {
      // Create a new map and copy existing Blocks
      Block[][] newmap = new Block[dim.height][dim.width];
      for (int i = 0; i < dim.height; i++) {
        int percent = 100 * i / dim.height;
        if (window != null)
          window.setMessage("Resizing map " + percent + "% ...");
        for (int j = 0; j < dim.width; j++) {
          if (i < map.length && j < map[0].length) {
            // if the region is shared between old and new maps
            newmap[i][j] = map[i][j];
          } else {
            newmap[i][j] = new Block(i, j, this);
          }
          pnlMap.add(newmap[i][j]);
        }
      }
      map = newmap;
    }
    
    // Redraw new map
    pnlMapHolder.add(pnlMap);
    pnlMapHolder.revalidate();
    pnlMapHolder.repaint();
    btnSave.setEnabled(true);
    btnProperties.setEnabled(true);
    Thread.yield();
  }
  
  /*
   * Outputs the current map to a file.
   */
  private void saveMap (File file) {
    // Create char[][] of data
    char[][] cmap = new char[map.length][map[0].length];
    for (int i = 0; i < map.length; i++) {
      for (int j = 0; j < map[0].length; j++) {
        cmap[i][j] = (char) map[i][j].byteValue();
      }
    }
    ObstacleMap om = new ObstacleMap(cmap);
    om.setProperties(properties);
    //TODO: prompt user for properties, if not already specified
    try {
      om.write(file);
    } catch (FileNotFoundException e) {
      err("Could not save map. The error was:\n\n" + e);
    }
  }
  
  /*
   * Handles all button activity
   */
  public void actionPerformed (ActionEvent e) {
    (new ActionHandler(e)).start();
  }
  
  /*
   * Handles zoom slider adjustments
   */
  public void stateChanged (ChangeEvent e) {
    (new ChangeHandler(e)).start();
  }
  
  /*
   * Returns the single combined MouseListener and MouseMotionListener
   * associated with all map Blocks in this Editor.
   */
  public EditorMouseListener getMouseListener () {
    return mouseListener;
  }
  
  /*
   * Returns a Dimension representing the size of the currently loaded map.
   * If no map is loaded, this method returns null.
   */
  public Dimension getMapSize () {
    if (map == null)
      return null;
    return new Dimension(map[0].length, map.length);
  }
  
  /*
   * Returns the java.util.Map of the properties associated with the currently loaded map.
   * If no map is loaded, this method returns null.
   */
  public Map <String, String> getProperties () {
    return properties; 
  }

  /*
   * Notifies this Editor that a MouseEvent has occurred which may affect
   * the state of the map. This method is automatically called by the 
   * EditorMouseListener when user interaction with the map is detected.
   */
  public void reportMouseEvent (MouseEvent e) {
    if (! (e.getSource() instanceof Block))
      return;
    Block source = (Block) e.getSource();
    if (e.getButton() == MouseEvent.BUTTON1) {
      Collection <Block> c = getSurroundingBlocks(source, getToolSize());
      Block.Type toolType = getToolType();
      for (Block b : c)
        b.setType(toolType);
    }
  }
  
  /*
   * Returns the currently selected tool type as a Block.Type 
   */
  private Block.Type getToolType () {
    if (radWall.isSelected())
      return Block.Type.WALL;
    else if (radEraser.isSelected())
      return Block.Type.EMPTY;
    return Block.Type.WALL;
  }
  
  /*
   * Returns a Collection of Blocks which surround the source Block. 
   * The resulting collectin forms a square of width and height (in blocks)
   * specified by regionSize. The source Block is as close to the center of
   * the square as possible.
   *
   * If the calculated region falls outside the map area, only the Blocks 
   * that are both inside the map and inside the region are returned. Thus,
   * there is no guarantee of the size of the Collection based on the specified
   * region size.
   *
   */
  private Collection <Block> getSurroundingBlocks (Block source, int regionSize) {
    Collection <Block> c = new LinkedList <Block> ();
    int sx = source.getLocation().x;
    int sy = source.getLocation().y;
    int dl = (regionSize - 1) / 2;
    int dr = regionSize / 2;
    for (int i = sx - dl; i <= sx + dr; i++) {
      for (int j = sy - dl; j <= sy + dr; j++) {
        try { 
          c.add(map[i][j]);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
      }
    }
    return c;
  }
  
  /*
   * Returns the tool size (in blocks) based on the user's selection.
   */
  private int getToolSize () {
    if (radOne.isSelected())
      return 1;
    if (radTwo.isSelected())
      return 2;
    if (radThree.isSelected())
      return 3;
    return -1;
  }
  
  /*
   * Convenience method which allows the current Thread to sleep
   */ 
  private void safeSleep (int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }
  
  /*
   * Convenience method for displaying an error dialog
   */
  private void err (String msg) {
    JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    Thread.yield();
  }
  
  /*
   * Inner class which handles all controller state-change activity
   */
  class ChangeHandler extends Thread {
    ChangeEvent event;
    
    public ChangeHandler (ChangeEvent e) {
      this.event = e;
    }
    
    public void run () {
      Object source = event.getSource();
      //Zoom slider
      if (source == sliZoom) {
        if (map == null || sliZoom.getValueIsAdjusting())
          return;
        int size = sliZoom.getValue();
        MessageWindow mw = new MessageWindow("Resizing blocks, please wait");
        mw.setVisible(true);
        safeSleep(100);
        for (int i = 0; i < map.length; i++) {
          for (int j = 0; j < map[0].length; j++) {
            map[i][j].setPreferredSize(new Dimension(size, size));
            map[i][j].revalidate();
          }
        }
        safeSleep(100);
        mw.setVisible(false);
        lblZoom.setText("  Block size: " + sliZoom.getValue());
        
        pnlMapHolder.revalidate();
        repaint();
      }
    }
  }
  
  /*
   * Inner class which handles all button activity.
   */
  class ActionHandler extends Thread {
    ActionEvent event;
    
    public ActionHandler (ActionEvent e) {
      this.event = e;
    }
    
    public void run () {
      Object source = event.getSource();
      // Quit
      if (source == btnQuit) {
        System.exit(0);
      }
      // New Map
      else if (source == btnNew) {
        createMap();
      }
      // Open Map
      else if (source == btnOpen) {
        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();
          MessageWindow window = new MessageWindow("Opening " + file.getName() + " ...");
          window.setVisible(true);
          ObstacleMap om = new ObstacleMap(file);
          safeSleep(400);
          createMap(om, window);
          safeSleep(100);
          window.setVisible(false);
        }
      }
      // Properties
      else if (source == btnProperties) {
        PropertiesDialog.prompt(Editor.this);
      }
      // Save Map
      else if (source == btnSave) {
        int returnVal = fileChooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();
          // make sure to save as .ppm
          if (!file.getName().endsWith(".ppm") && !file.getName().endsWith(".PPM"))
            file = new File(file.getPath() + ".ppm");
          // save file
          MessageWindow window = new MessageWindow("Saving " + file.getName() + " ...");
          window.setVisible(true);
          saveMap(file);
          safeSleep(400);
          window.setVisible(false);
        }
      }
    }
  }
  
  
}

