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
import java.awt.color.ColorSpace;

public class Display extends Frame implements WindowListener,ActionListener {

    static ColorConvertOp grayscale = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    JLabel lbl1=new JLabel();
    JLabel lbl2=new JLabel();
    int width = 0;
    int height = 0;
    BufferedImage img, greyScale, filtered, sobel, nonMax;
    float[][] sX, sY;

    double thresholdLow = 50, thresholdHigh = 150;

    Button b, c;

    final int GX[][] = {{-1,0,1},
                          {-2,0,2},
                          {-1,0,1}};
    final int GY[][] = {{-1,-2,-1},
                          {0,0,0},
                          {1,2,1}};
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

    int[] acc;
    int accSize=5;
    int[] results;

    int r = 30;

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
        } else if (e.getSource() == c) {

            gaussianBlur();
            filtered = grayscale.filter(filtered, null);
            sobel();
            nonMax();
            //overlay();
            Hough();
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

        sobel = new BufferedImage ( width, height, filtered.getType() );
        Graphics2D g = nonMax.createGraphics();
        g.setColor( new Color ( 0, 0, 0, 0 ));
        g.fillRect(0, 0, width, height);
        g.dispose();

        int[] tmp = new int[1];
        sX = new float[height][width];
        sY = new float[height][width];
        for (int y = 1;y < height-1;++y)
            for (int x = 1; x < width-1;++x)
            {
                float Xvalue = 0;
                float Yvalue = 0;
                for (int j = -1;j <=1;++j)
                    for (int i = -1;i <=1;++i)
                    {

                        Xvalue += GX[1+j][1+i] * filtered.getRaster().getPixel(x+i,y+j, tmp)[0];
                        Yvalue += GY[1+j][1+i] * filtered.getRaster().getPixel(x+i,y+j, tmp)[0];
                    }
                sX[y][x] = Xvalue;
                sY[y][x] = Yvalue;
                float[] a = {Math.abs(sX[y][x]) + Math.abs(sY[y][x])};
                if (a[0] > 255)
                    a[0] = 255;
                if (a[0] < 0)
                    a[0] = 0;
                sobel.getRaster().setPixel(x, y, a);
            }

        ImageIcon icon2=new ImageIcon(sobel);
        lbl2.setIcon(icon2);
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
                gm[y][x] = Math.sqrt(sY[y][x] * sY[y][x] + sX[y][x] * sX[y][x]);
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
        int[] tmp = {0};
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (nonMax.getRaster().getPixel(x, y, tmp)[0] < 50) {
                    // It's a strong pixel, lets find the neighbouring weak ones.
                    trackWeakOnes(x, y, nonMax);
                }
            }
        }
        // removing the single weak pixels.
        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                if (nonMax.getRaster().getPixel(x, y, tmp)[0] > 50) {
                    nonMax.getRaster().setPixel(x, y, tmp255);
                }
            }
        }
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

    public void Hough()
    {
        // for polar we need accumulator of 180degress * the longest length in the image
        int rmax = (int)Math.sqrt(width*width + height*height);
        acc = new int[width * height];
        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {
                acc[x*width+y] =0 ;
            }
        }
        int x0, y0;
        double t;
        int[] val = new int[1];
        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {

                if ((nonMax.getRaster().getPixel(x, y, val)[0])== 0) {

                    for (int theta=0; theta<360; theta++) {
                        t = (theta * 3.14159265) / 180;
                        x0 = (int)Math.round(x - r * Math.cos(t));
                        y0 = (int)Math.round(y - r * Math.sin(t));
                        if(x0 < width && x0 > 0 && y0 < height && y0 > 0) {
                            acc[x0 + (y0 * width)] += 1;
                        }
                    }
                }
            }
        }
        // now normalise to 255 and put in format for a pixel array
        int max=0;

        // Find max acc value
        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {

                if (acc[x + (y * width)] > max) {
                    max = acc[x + (y * width)];
                }
            }
        }

        //System.out.println("Max :" + max);

        // Normalise all the values
        int value;
        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {
                value = (int)(((double)acc[x + (y * width)]/(double)max)*255.0);
                acc[x + (y * width)] = 0xff000000 | (value << 16 | value << 8 | value);
            }
        }

        sobel = new BufferedImage ( width, height, BufferedImage.TYPE_BYTE_GRAY );
        Graphics2D g = sobel.createGraphics();
        g.setColor( new Color ( 0, 0, 0, 0 ));
        g.fillRect(0, 0, width, height);
        g.dispose();

        findMaxima();

        System.out.println("done");
    }

    private void findMaxima() {
        results = new int[accSize*3];


        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {
                int value = (acc[x + (y * width)] & 0xff);

                // if its higher than lowest value add it and then sort
                if (value > results[(accSize-1)*3]) {

                    // add to bottom of array
                    results[(accSize-1)*3] = value;
                    results[(accSize-1)*3+1] = x;
                    results[(accSize-1)*3+2] = y;

                    // shift up until its in right place
                    int i = (accSize-2)*3;
                    while ((i >= 0) && (results[i+3] > results[i])) {
                        for(int j=0; j<3; j++) {
                            int temp = results[i+j];
                            results[i+j] = results[i+3+j];
                            results[i+3+j] = temp;
                        }
                        i = i - 3;
                        if (i < 0) break;
                    }
                }
            }
        }

        double ratio=(double)(width/2)/accSize;
        System.out.println("top "+accSize+" matches:");
        for(int i=accSize-1; i>=0; i--){
            //System.out.println("value: " + results[i*3] + ", r: " + results[i*3+1] + ", theta: " + results[i*3+2]);
            drawCircle(results[i*3], results[i*3+1], results[i*3+2]);
        }
        ImageIcon icon1=new ImageIcon(img);
        lbl1.setIcon(icon1);
    }

    private void setPixel(int value, int xPos, int yPos) {
        int rgb = img.getRGB(xPos,yPos);
        Color color = new Color(rgb);
        Color res = new Color(255, color.getGreen(), color.getBlue());
        img.setRGB(xPos, yPos, res.getRGB());
    }

    private void drawCircle(int pix, int xCenter, int yCenter) {
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillOval(xCenter-r, yCenter-r, r*2, r*2);
//        pix = 250;
//
//        int x, y, r2;
//        int radius = r;
//        r2 = r * r;
//        setPixel(pix, xCenter, yCenter + radius);
//        setPixel(pix, xCenter, yCenter - radius);
//        setPixel(pix, xCenter + radius, yCenter);
//        setPixel(pix, xCenter - radius, yCenter);
//
//        y = radius;
//        x = 1;
//        y = (int) (Math.sqrt(r2 - 1) + 0.5);
//        while (x < y) {
//            setPixel(pix, xCenter + x, yCenter + y);
//            setPixel(pix, xCenter + x, yCenter - y);
//            setPixel(pix, xCenter - x, yCenter + y);
//            setPixel(pix, xCenter - x, yCenter - y);
//            setPixel(pix, xCenter + y, yCenter + x);
//            setPixel(pix, xCenter + y, yCenter - x);
//            setPixel(pix, xCenter - y, yCenter + x);
//            setPixel(pix, xCenter - y, yCenter - x);
//            x += 1;
//            y = (int) (Math.sqrt(r2 - x*x) + 0.5);
//        }
//        if (x == y) {
//            setPixel(pix, xCenter + x, yCenter + y);
//            setPixel(pix, xCenter + x, yCenter - y);
//            setPixel(pix, xCenter - x, yCenter + y);
//            setPixel(pix, xCenter - x, yCenter - y);
//        }
    }

    public void windowOpened(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}

}
