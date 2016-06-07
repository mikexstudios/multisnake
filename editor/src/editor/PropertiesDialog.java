/*
 *  PropertiesDialog
 *  Provides options for specifying map properties
 */

package editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class PropertiesDialog extends JFrame implements ActionListener {
  private static final long serialVersionUID = 4934499640L;
  private static final int MIN_DIM = 30;
  private static final int MAX_DIM = 300;
  
  Editor editor;
  boolean accepted, canceled;
  JButton btnCancel, btnOK;
  JTextField txtWidth, txtHeight, txtTitle, txtAuthor;
  
  /*
   * Creates and displays a dialog, then waits for user input.
   * Other classes must use this static method rather than instantiating dialogs manually.
   *
   * An Editor must be specified in order for the PropertiesDialog to make changes to the
   * properties. This can be changed in the future to use an interface instead of the 
   * Editor class if necessary.
   */
  public static void prompt (Editor e) {
    if (e == null)
      return;
    PropertiesDialog dialog = new PropertiesDialog(e);
    dialog.myPrompt();
  }
  
  /*
   * Creates a new dialog but does not display it
   */
  private PropertiesDialog (Editor e) {
    super("Properties");
    this.editor = e;
    canceled = false;
    accepted = false;
    
    setSize(500, 200);
    setLayout(new GridLayout(6,1));
    setLocationRelativeTo(null);
    
    // Create dialog title inside frame
    Font dialogTitleFont = new Font("Arial", Font.PLAIN, 18);
    JLabel lblDialogTitle = new JLabel("Set Map Properties", JLabel.CENTER);
    lblDialogTitle.setFont(dialogTitleFont);
    
    // Dimension panel
    JPanel pnlDimension = new JPanel();
    pnlDimension.setLayout(new FlowLayout());
    Font labelFont = new Font("Arial", Font.PLAIN, 14);
    JLabel lblHeight = new JLabel("       Height    ");
    JLabel lblWidth = new JLabel("      Width    ");
    lblHeight.setFont(labelFont);
    lblWidth.setFont(labelFont);
    txtHeight = new JTextField("30");
    txtWidth = new JTextField("30");
    Dimension txtDim = new Dimension(60, 25);
    txtHeight.setPreferredSize(txtDim);
    txtWidth.setPreferredSize(txtDim);
    pnlDimension.add(lblHeight);
    pnlDimension.add(txtHeight);
    pnlDimension.add(lblWidth);
    pnlDimension.add(txtWidth);
    
    // Title panel
    JPanel pnlTitle = new JPanel();
    pnlTitle.setLayout(new BorderLayout());
    JLabel lblTitle = new JLabel("     Title (optional)     ");
    lblTitle.setFont(labelFont);
    txtTitle = new JTextField();
    txtTitle.setPreferredSize(new Dimension(300, 30));
    pnlTitle.add(lblTitle, BorderLayout.WEST);
    pnlTitle.add(txtTitle, BorderLayout.CENTER);
    
    // Author panel
    JPanel pnlAuthor = new JPanel();
    pnlAuthor.setLayout(new BorderLayout());
    JLabel lblAuthor = new JLabel("     Author (optional)     ");
    lblAuthor.setFont(labelFont);
    txtAuthor = new JTextField();
    txtAuthor.setPreferredSize(new Dimension(300, 30));
    pnlAuthor.add(lblAuthor, BorderLayout.WEST);
    pnlAuthor.add(txtAuthor, BorderLayout.CENTER);
    
    // Buttons panel
    JPanel pnlButtons = new JPanel();
    btnCancel = new JButton("Cancel");
    btnOK = new JButton("  OK  ");
    pnlButtons.add(btnCancel);
    pnlButtons.add(btnOK);
    getRootPane().setDefaultButton(btnOK);
    btnCancel.addActionListener(this);
    btnOK.addActionListener(this);
    
    add(lblDialogTitle);
    add(pnlDimension);
    add(pnlTitle);
    add(pnlAuthor);
    add(new JLabel(" "));
    add(pnlButtons);
    
    pack();
    
    // Put existing values into input fields
    openProperties();
  }
  
  
  /*
   *  Displays this dialog and waits for the user to respond.
   *  
   *  If the user enters invalid input, an error message will be displayed
   *  and this dialog will remain open. 
   */
  private void myPrompt () {
    canceled = false;
    accepted = false;
    setVisible(true);
    while(true) {
      while (!canceled && !accepted)  // cheap way to wait for the user to click a button
        Thread.yield();
      if (canceled) {
        setVisible(false);
        return;
      }
      else if (accepted) {
        if (saveProperties()) {
          setVisible(false);
          return;
        }
        // if saveProperties returns false, continue to wait in the loop
      }
      canceled = false;
      accepted = false;
    }
  }
  
  /*
   * Obtains existing properties from the saved Editor and fills in 
   * the input fields of this dialog accordingly.
   */
  private void openProperties () {
    // Get map size
    Dimension dim = editor.getMapSize();
    if (dim == null)
      dim = new Dimension(50, 50); //offer some default values
    txtWidth.setText(dim.width + "");
    txtHeight.setText(dim.height + "");
    // Get properties
    Map <String, String> properties = editor.getProperties();
    if (properties == null)
      return;
    String title = properties.get("title");
    if (title != null)
      txtTitle.setText(title);
    String author = properties.get("author");
    if (author != null)
      txtAuthor.setText(author);
  }
  
  /*
   * Stores the values from this dialog's input fields in the saved Editor.
   *
   * Returns true if all inputs are vaild and were saved correctly; false otherwise
   */
  private boolean saveProperties () {
    // Check all inputs first
    Dimension dim;
    try {
      int width = Integer.parseInt(txtWidth.getText());
      int height = Integer.parseInt(txtHeight.getText());
      dim = new Dimension(width, height);
      // force user to enter valid numbers
      if (!isValidDim(dim)) {
        err("Please enter dimensions between " + MIN_DIM + " and " + MAX_DIM);
        return false;
      }
    } catch (Exception e) {
      err("Please enter dimensions between " + MIN_DIM + " and " + MAX_DIM);
      return false;
    }
    // check for bad strings
    if (!isValidString(txtTitle.getText()) || !isValidString(txtAuthor.getText())) {
      err("Text cannot contain '&' or '='");
      return false;
    }
    
    // Save size
    MessageWindow mw = new MessageWindow("Saving size");
    mw.setVisible(true);
    editor.resizeMap(dim, mw);
    mw.setVisible(false);
    
    // Save properties
    Map <String, String> properties = editor.getProperties();
    if (properties == null)
      return false;
    properties.put("title", txtTitle.getText());
    properties.put("author", txtAuthor.getText());
    return true;
  }
  
  /*
   *  Determines whether a Dimension is within the min and max bounds specified
   *  as constants in this class. Returns false if the Dimension is null.
   */
  private boolean isValidDim (Dimension dim) {
    return (dim != null &&
            dim.width >= MIN_DIM && dim.width <= MAX_DIM &&
            dim.height >= MIN_DIM && dim.height <= MAX_DIM);
  }
  
  /*
   *  Determines whether a property String is valid. This is based on the need to 
   *  read and write the properties within map files. The only restriction is 
   *  that Strings cannot contain delimiters used by the ObstacleMap class ('&' and '=').
   */
  private boolean isValidString (String s) {
    return !(s.contains("&") || s.contains("="));
  }
  
  /*
   *  Handles button activity
   */
  public void actionPerformed (ActionEvent e) { 
    Object source = e.getSource();
    if (source == btnCancel) {
      canceled = true;
    }
    else if (source == btnOK) {
      accepted = true;
    }
  }
  
  /*
   * Convenience method for displaying an error dialog
   */
  private void err (String msg) {
    JOptionPane.showMessageDialog(null, msg + "   ", "Error", JOptionPane.ERROR_MESSAGE);
    Thread.yield();
  }
    
}
