import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JFileChooser;

public class Display extends Frame implements WindowListener,ActionListener {
    JLabel lbl1=new JLabel();
    JLabel lbl2=new JLabel();
    Button b;

    public static void main(String[] args) throws IOException {
        Display myWindow = new Display("My first window");
        myWindow.setSize(800, 600);
        myWindow.setVisible(true);
    }

    public Display(String title) throws IOException{

        super(title);
        setLayout(new FlowLayout());
        addWindowListener(this);
        b = new Button("Load Image");
        add(b);
        BufferedImage img = ImageIO.read(new File("pic_1.gif"));
        BufferedImage greyScale = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = greyScale.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        int[] tmp = new int[3];
        greyScale.getRaster().getPixel(0,0,tmp);
        System.out.println(tmp[0] + ", " +  tmp[1] + ", " + tmp[2]);
        ImageIcon icon=new ImageIcon(greyScale);
        lbl1.setIcon(icon);
        lbl2.setIcon(icon);
        add(lbl1);
        add(lbl2);
        b.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        JFrame frame2=new JFrame();
        String path = "";
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showOpenDialog(frame2); // parentComponent must a component like JFrame, JDialog...
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            path = selectedFile.getAbsolutePath();
        }
        try
        {
            BufferedImage img = ImageIO.read(new File(path));
            ImageIcon icon=new ImageIcon(img);
            lbl1.setIcon(icon);
            lbl2.setIcon(icon);
        }catch (IOException ioe) {
        }

    }

    public void windowClosing(WindowEvent e) {
        dispose();
        System.exit(0);
    }

    public void windowOpened(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}

}
