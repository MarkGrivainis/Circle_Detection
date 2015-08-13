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
    int width = 0;
    int height = 0;
    BufferedImage img, greyScale, filtered, sobel, nonMax;
    float[][] sX, sY;

    double thresholdLow = 40, thresholdHigh = 150;

    Button b, c;

    final float GX[] = {-1f,0f,1f,
                        -2f,0f,2f,
                        -1f,0f,1f};
    final float GY[] = {-1f,-2f,-1f,
                        0f,0f,0f,
                        1f,2f,1f};
    private static final float[] sobel1 = { 1.0f, 0.0f, -1.0f};
    private static final float[] sobel2 = { 1.0f, 2.0f,  1.0f};
    final float gaus[] =  {2f/115,4f/115,5f/115,4f/115,2f/115,
                            4f/115,9f/115,12f/115,9f/115,4f/115,
                            5f/115,12f/115,15f/115,12f/115,5f/115,
                            4f/115,9f/115,12f/115,9f/115,4f/115,
                            2f/115,4f/115,5f/115,4f/115,2f/115};


    int[] tmp255 = {255};
    int[] tmp128 = {128};
    int[] tmp000 = {0};
    int[] tmpPixel = {0};

    String path = "pic_1.gif";

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
        c = new Button("filter");
        add(b);
        add(c);
        loadImage();
        add(lbl1);
        add(lbl2);
        b.addActionListener(this);
        c.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e){
        if(e.getSource() == b) {
            JFrame frame2 = new JFrame();
            JFileChooser chooser = new JFileChooser();
            int option = chooser.showOpenDialog(frame2); // parentComponent must a component like JFrame, JDialog...
            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                path = selectedFile.getAbsolutePath();
            }
            try {
                loadImage();
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
        } else if (e.getSource() == c)
        {

            gaussianBlur();
            sobel();
            //nonMax();
            //overlay();
        }

    }

    public void windowClosing(WindowEvent e) {
        dispose();
        System.exit(0);
    }

    public void overlay()
    {
        for (int y = 0;y < height;++y)
        {
            for (int x = 0;x < width;++x)
            {
                //System.out.println(nonMax.getRGB(x,y));
                if (nonMax.getRGB(x,y) != -1)
                {
                    int rgb = img.getRGB(x,y);
                    Color color = new Color(rgb);
                    Color res = new Color(255, color.getGreen(), color.getBlue());
                    img.setRGB(x,y,res.getRGB());
                }
            }
        }
        ImageIcon icon1=new ImageIcon(img);
        lbl1.setIcon(icon1);
    }

    public void loadImage() throws IOException
    {
        nonMax = ImageIO.read(new File(path));
        width = nonMax.getWidth();
        height = nonMax.getHeight();
        img = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.drawImage(nonMax, 0, 0, null);
        g.dispose();
        greyScale = copyImage(nonMax);
        ImageIcon icon=new ImageIcon(img);
        ImageIcon icon2= new ImageIcon(greyScale);
        lbl1.setIcon(icon);
        lbl2.setIcon(icon2);
    }

    public void gaussianBlur()
    {
        filtered = null;
        Kernel kernel = new Kernel(5, 5, gaus);
        ConvolveOp op = new ConvolveOp(kernel);
        filtered = op.filter(img, null);
//        int[] value = new int[1];
//        double r = 1.4;
//        int rs = 2;     // significant radius
//            for(int i=0; i<height; i++)
//                for(int j=0; j<width; j++)
//                {
//                    float val = 0, wsum = 0;
//                    for(int iy = i-rs; iy<i+rs+1; iy++)
//                        for(int ix = j-rs; ix<j+rs+1; ix++)
//                        {
//                            int x = Math.min(width-1, Math.max(0, ix));
//                            int y = Math.min(height-1, Math.max(0, iy));
//                            float dsq = (ix-j)*(ix-j)+(iy-i)*(iy-i);
//                            //double wght = Math.exp( -dsq / (2*r*r) ) / (Math.PI*2*r*r);
//                            double wght = Math.pow(Math.E, (-dsq))/(2 * (r * r));
//                            int gray= img.getRGB(x, y)& 0xFF;
//                            val += gray * wght;
//                            wsum += wght;
//                        }
//                    value[0] = Math.round(val/wsum);
//                    filtered.getRaster().setPixel(j, i, value);
//                }
        ImageIcon icon2=new ImageIcon(filtered);
        lbl2.setIcon(icon2);
    }

    private double G(int x, int y)
    {
        //the minimum value has to be 0, and the maximum must be 16777215 (hexidecimal of black is 000000 and white is ffffff. I just used the calculator to find it out)
        int derp = this.sobel.getRGB(x,y);
        int herp = this.nonMax.getRGB(x,y);

        //maximum possible for result: 23726565.  Minimum == 0.
        double result = Math.sqrt(Math.pow(derp, 2.0) + Math.pow(herp, 2.0));
        return result;
    }

    public void sobel()
    {
        sX = new float[height][width];
        sY = new float[height][width];

        RenderingHints hints =
                new RenderingHints(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);

        sobel = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = sobel.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        nonMax = null;
        Kernel kernelX = new Kernel(3, 3, GX);
        Kernel kernelY = new Kernel(3, 3, GY);
        ConvolveOp opX = new ConvolveOp(kernelX, ConvolveOp.EDGE_NO_OP, hints);
        ConvolveOp opY = new ConvolveOp(kernelY, ConvolveOp.EDGE_NO_OP, hints);
        sobel = opX.filter(img, null);
        nonMax = opY.filter(img, null);

        ImageIcon icon2=new ImageIcon(sobel);
        lbl2.setIcon(icon2);

        ImageIcon icon1=new ImageIcon(nonMax);
        lbl1.setIcon(icon1);

      //  for (int x = 1; x < width; ++x) {
       // for (int y = 1; y < height; ++y) {
       //     int xPixel = sobel.getRGB(x,y) & 0XFF;
       //     int yPixel = nonMax.getRGB(x,y) & 0XFF;
       //     double[] a = {Math.hypot(xPixel, yPixel)};
       //     img.getRaster().setPixel(x, y, a);
        //}
   // }
        //ImageIcon icon1=new ImageIcon(img);
        //lbl1.setIcon(icon1);

//        for (int i=0; i<img.getWidth(); i++) {
//            for (int j=0; j<img.getHeight(); j++) {
//                double result = G(i,j);
//                //using a floating point to change everything to the right values.
//                float greyscaleValue = (float)(result/23777215);
//                greyscaleValue = 1-greyscaleValue;
//                // System.out.println("Result: " + result + "  max double: " + max + " Grayscale value: " + greyscaleValue);
//                // System.out.println("Gray -- R: " + Color.gray.getRed() + " G: " + Color.gray.getGreen() + " B: " + Color.gray.getBlue() );
//                float red =  255 * greyscaleValue;
//                float blue = 255 * greyscaleValue;
//                float green = 255 * greyscaleValue;
//                Color gray2 = new Color((int)red,(int)green,(int)blue);
//                img.setRGB(i,j,gray2.getRGB());
//            }
//        }
//
//        ImageIcon icon1=new ImageIcon(img);
//        lbl1.setIcon(icon1);
//
//        for(int i = 0; i < height; i++)
//           for(int j = 0; j < width; j++)
//           {
//               sY[i][j] = sobel.getRGB(j, i) & 0xFF;
//               sY[i][j] = nonMax.getRGB(j, i) & 0xFF;
//           }
//        sobel = null;
//
//        Kernel kernelX1 = new Kernel(1, 3, sobel1);
//        ConvolveOp opX1 = new ConvolveOp(kernelX1, ConvolveOp.EDGE_ZERO_FILL, null);
//
//        Kernel kernelX2 = new Kernel(3, 1, sobel2);
//        ConvolveOp opX2 = new ConvolveOp(kernelX2, ConvolveOp.EDGE_ZERO_FILL, null);
//
//        sobel = opX1.filter(filtered, null);
//        //sobel = opX2.filter(filtered, null);
//
//
//        //for(int i = 0; i < height; i++)
//        //    for(int j = 0; j < width; j++)
//        //        sX[i][j] = sobel.getRGB(j, i) & 0xFF;
//        ImageIcon icon2=new ImageIcon(sobel);
//        lbl2.setIcon(icon2);
//        nonMax = null;
//
//        Kernel kernelY1 = new Kernel(1, 3, sobel2);
//        ConvolveOp opY1 = new ConvolveOp(kernelY1, ConvolveOp.EDGE_ZERO_FILL, null);
//
//        Kernel kernelY2 = new Kernel(3, 1, sobel1);
//        ConvolveOp opY2 = new ConvolveOp(kernelY2, ConvolveOp.EDGE_ZERO_FILL, null);
//
//        nonMax = opY1.filter(filtered, null);
//        //nonMax = opY2.filter(filtered, null);
//
//        ImageIcon icon1=new ImageIcon(nonMax);
//        lbl1.setIcon(icon1);
        //sobel = null;
        //kernel = new Kernel(3, 3, GY);
        //op = new ConvolveOp(kernel);
        //sobel = op.filter(filtered, null);
        //for(int i = 0; i < height; i++)
        //   for(int j = 0; j < width; j++)
        //        sY[i][j] = sobel.getRGB(j, i) & 0xFF;
//        sX = new float[height][width];
//        sY = new float[height][width];
//        int gray;
//        sobel = copyImage(filtered);
//        double SUM = 0;
//        double[] value = new double[1];
//        for (int y = 0;y <= height-1;y++)
//        {
//            for(int x = 0;x<=width-1;x++)
//            {
//                float sumX = 0;
//                float sumY = 0;
//                //double magnitude;
//                if (y > 0 && y < height-1 && x > 0 && x < width-1)
//                {
//                    for (int i = -1; i <= 1; i++) {
//                        for (int j = -1; j <= 1; j++) {
//                            gray = filtered.getRGB(x + j, y + i) & 0xFF;
//                            sumX += GX[j + 1][i + 1] * gray;
//                            sumY += GY[j + 1][i + 1] * gray;
//                        }
//                    }
//                }
//                //magnitude = Math.abs(sumX) + Math.abs(sumY);
//
//                //if (magnitude < 0) magnitude = 0;
//                //if (magnitude > 255) magnitude = 255;
//                sX[y][x] = sumX;
//                sY[y][x] = sumY;
//                int brightness = (int)Math.ceil(Math.sqrt(sumX*sumX + sumY*sumY));
//                //brightness = brightness < 1 ? 0 : 255;
//                //System.out.println(brightness);
//                value[0] = brightness;
//                sobel.getRaster().setPixel(x, y, value);
////                if (brightness == 0)
////                {
////                    int rgb = img.getRGB(x,y);
////                    Color color = new Color(rgb);
////                    Color res = new Color(255, 0, 0);
////                    img.setRGB(x,y,res.getRGB());
////                }
//
//            }
//        }
////        ImageIcon icon1=new ImageIcon(img);
////        lbl1.setIcon(icon1);
//        ImageIcon icon2=new ImageIcon(sobel);
//        lbl2.setIcon(icon2);
    }

    public void nonMax()
    {
        nonMax = new BufferedImage ( width, height, BufferedImage.TYPE_BYTE_GRAY );
        Graphics2D g = nonMax.createGraphics();
        g.setColor( new Color ( 0, 0, 0, 0 ));
        g.fillRect(0, 0, width, height);
        g.dispose();
        double[][] gd = new double[height][width];
        double[][] gm = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //System.out.println(x + ", " + y + " | " + width +", " + height);
                // setting gradient magnitude and gradient direction
                if (sX[y][x] != 0) {
                    gd[y][x] = Math.atan(sY[y][x] / sX[y][x]);
                } else {
                    gd[y][x] = Math.PI / 2d;
                }
                gm[y][x] = Math.abs(sY[y][x]) + Math.abs(sX[y][x]);//Math.sqrt(sY[y][x] * sY[y][x] + sX[y][x] * sX[y][x]);
//                gm[x][y] = Math.hypot(gy[x][y], gx[x][y]);
            }
        }
        for (int x = 0; x < width; x++) {
            nonMax.getRaster().setPixel(x, 0, new int[]{255});
            nonMax.getRaster().setPixel(x, height - 1, new int[]{255});
        }
        for (int y = 0; y < height; y++) {
            nonMax.getRaster().setPixel(0, y, new int[]{255});
            nonMax.getRaster().setPixel(width - 1, y, new int[]{255});
        }
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
               // System.out.println(x + ", " + y + " | " + width +", " + height);
                if (gd[y][x] < (Math.PI / 8d) && gd[y][x] >= (-Math.PI / 8d)) {
                    // check if pixel is a local maximum ...
                    if (gm[y][x] > gm[y + 1][x] && gm[y][x] > gm[y - 1][x])
                        setPixel(x, y, nonMax, gm[y][x]);
                    else
                        nonMax.getRaster().setPixel(x, y, tmp255);
                } else if (gd[y][x] < (3d * Math.PI / 8d) && gd[y][x] >= (Math.PI / 8d)) {
                    // check if pixel is a local maximum ...
                    if (gm[y][x] > gm[y - 1][x - 1] && gm[y][x] > gm[y + 1][x + 1])
                        setPixel(x, y, nonMax, gm[y][x]);
                    else
                        nonMax.getRaster().setPixel(x, y, tmp255);
                } else if (gd[y][x] < (-3d * Math.PI / 8d) || gd[y][x] >= (3d * Math.PI / 8d)) {
                    if (gm[y][x] > gm[y][x + 1] && gm[y][x] > gm[y][x - 1])
                        setPixel(x, y, nonMax, gm[y][x]);
                    else
                        nonMax.getRaster().setPixel(x, y, tmp255);
                } else if (gd[y][x] < (-Math.PI / 8d) && gd[y][x] >= (-3d * Math.PI / 8d)) {
                    if (gm[y][x] > gm[y + 1][x - 1] && gm[y][x] > gm[y - 1][x + 1])
                        setPixel(x, y, nonMax, gm[y][x]);
                    else
                        nonMax.getRaster().setPixel(x, y, tmp255);
                } else {
                    nonMax.getRaster().setPixel(x, y, tmp255);
                }
            }
        }
//        int[] tmp = {0};
//        for (int x = 1; x < width - 1; x++) {
//            for (int y = 1; y < height - 1; y++) {
//                if (nonMax.getRaster().getPixel(x, y, tmp)[0] < 50) {
//                    // It's a strong pixel, lets find the neighbouring weak ones.
//                    trackWeakOnes(x, y, nonMax);
//                }
//            }
//        }
//        // removing the single weak pixels.
//        for (int x = 2; x < width - 2; x++) {
//            for (int y = 2; y < height - 2; y++) {
//                if (nonMax.getRaster().getPixel(x, y, tmp)[0] > 50) {
//                    nonMax.getRaster().setPixel(x, y, tmp255);
//                }
//            }
//        }
        ImageIcon icon2=new ImageIcon(nonMax);
        lbl2.setIcon(icon2);
    }

    private void trackWeakOnes(int x, int y, BufferedImage gray) {
        for (int xx = x - 1; xx <= x + 1; xx++)
            for (int yy = y - 1; yy <= y + 1; yy++) {
                if (isWeak(xx, yy, gray)) {
                    gray.getRaster().setPixel(xx, yy, tmp000);
                    trackWeakOnes(xx, yy, gray);
                }
            }
    }

    private boolean isWeak(int x, int y, BufferedImage gray) {
        return (gray.getRaster().getPixel(x, y, tmpPixel)[0] > 0 && gray.getRaster().getPixel(x, y, tmpPixel)[0] < 255);
    }

    private void setPixel(int x, int y, BufferedImage gray, double v) {
        if (v > thresholdLow) gray.getRaster().setPixel(x, y, tmp000);
        else if (v > thresholdHigh) gray.getRaster().setPixel(x, y, tmp128);
        else gray.getRaster().setPixel(x, y, tmp255);
    }

    public static BufferedImage copyImage(BufferedImage source){
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    public void windowOpened(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}

}
