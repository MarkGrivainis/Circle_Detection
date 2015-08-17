import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.color.ColorSpace;
import java.util.Arrays;

public class Display extends Frame implements WindowListener,ActionListener, ChangeListener {

    static ColorConvertOp grayscale = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    private final static float GAUSSIAN_CUT_OFF = 0.005f;
    private final static float MAGNITUDE_SCALE = 100F;
    private final static float MAGNITUDE_LIMIT = 1000F;
    private final static int MAGNITUDE_MAX = (int) (MAGNITUDE_SCALE * MAGNITUDE_LIMIT);

    JLabel lbl1=new JLabel();
    JLabel lbl2=new JLabel();

    JRadioButton filterBtn;
    JRadioButton sobelBtn;
    JRadioButton nonMaxBtn;
    JRadioButton accumulator;
    JSlider whichRadius;
    JTextField inputCircles;
    Button findCircles;

    int width = 0;
    int height = 0;
    BufferedImage img, greyScale, filtered, sobel, nonMax, accImage, res;
    double[][] sX, sY;

    double thresholdLow = 60, thresholdHigh = 100;

    Button b, c;

    final int GX[][] = {{-1,0,1},
                        {-2,0,2},
                        {-1,0,1}};
    final int GY[][] = {{-1,-2,-1},
                        {0,0,0},
                        {1,2,1}};

    final float gaus[] =  {
            2/159f,4/159f,5/159f,4/159f,2/159f,
            4/159f, 9/159f, 12/159f, 9/159f, 4/159f,
            5/159f,12/159f,15/159f,12/159f,5/159f,
            4/159f,9/159f,12/159f,9/159f,4/159f,
            2/159f,4/159f,5/159f,4/159f,2/159f
    };


    int[] tmp255 = {255};
    int[] tmp128 = {128};
    int[] tmp000 = {0};
    int[] tmpPixel = {0};

    String path = "pic_2.gif";

    int[][] acc;
    int accSize=12;
    int[] results;
    int[][] binary;

    int[] data;
    int[] magnitude;

    //int r = 14;
    int rmax;
    int offset = 2;
    int accRMax;
    int rmin = 10;



    public static void main(String[] args) throws IOException {
        Display myWindow = new Display("My first window");
        myWindow.setSize(800, 600);
        myWindow.setVisible(true);
    }

    public Display(String title) throws IOException{

        super(title);
        setLayout(new GridLayout(1, 3));
        JPanel options = new JPanel(new GridLayout(5,1));
        JPanel numCircles = new JPanel((new GridLayout(1, 2)));
        addWindowListener(this);
        b = new Button("Load Image");
        c = new Button("filter");
        findCircles = new Button("Find");

        inputCircles = new JTextField("12");
        numCircles.add(inputCircles);
        numCircles.add(findCircles);
        options.add(b);
        options.add(c);
        add(options);

        filterBtn = new JRadioButton("Filtered");
        sobelBtn = new JRadioButton("Sobel");
        nonMaxBtn = new JRadioButton("Non Maximal");
        accumulator = new JRadioButton("Accumulator");
        whichRadius = new JSlider(JSlider.HORIZONTAL, rmin, 125, 14);


        whichRadius.setMajorTickSpacing(30);
        whichRadius.setMinorTickSpacing(1);
        whichRadius.setPaintTicks(true);
        whichRadius.setPaintLabels(true);
        whichRadius.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 10, 0));
        Font font = new Font("Serif", Font.ITALIC, 6);
        whichRadius.setFont(font);
        
        ButtonGroup rGroup = new ButtonGroup();
        rGroup.add(filterBtn);
        rGroup.add(sobelBtn);
        rGroup.add(nonMaxBtn);
        rGroup.add(accumulator);

        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(filterBtn);
        radioPanel.add(sobelBtn);
        radioPanel.add(nonMaxBtn);
        radioPanel.add(accumulator);

        options.add(radioPanel);
        options.add(numCircles);
        options.add(whichRadius);
        loadImage();
        add(lbl1);
        add(lbl2);
        b.addActionListener(this);
        c.addActionListener(this);

        filterBtn.addActionListener(this);
        sobelBtn.addActionListener(this);
        nonMaxBtn.addActionListener(this);
        findCircles.addActionListener(this);
        accumulator.addActionListener(this);

        whichRadius.addChangeListener(this);
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

            greyScale = copyImage(img);
            gaussianBlur();
            //filtered = grayscale.filter(filtered, null);
            sobel();
            //thinImage();
            nonMax();
            float lowThreshold = 2.5f;
            float highThreshold = 7.5f;
            int low = Math.round(lowThreshold * MAGNITUDE_SCALE);
            int high = Math.round( highThreshold * MAGNITUDE_SCALE);
            performHysteresis(low, high);
            thresholdEdges();
            writeEdges(data);
            Hough();
            ImageIcon icon1=new ImageIcon(res);
            lbl1.setIcon(icon1);
            ImageIcon icon2=new ImageIcon(filtered);
            lbl2.setIcon(icon2);
            filterBtn.setSelected(true);
        }if(e.getSource() == findCircles) {
            try {
                accSize = Integer.parseInt(inputCircles.getText());
                res = new BufferedImage(
                        width, height, BufferedImage.TYPE_INT_RGB);
                Graphics g = res.getGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                findMaxima();
                ImageIcon icon1=new ImageIcon(res);
                lbl1.setIcon(icon1);
            }catch (Exception err)
            {
                System.out.println("Not a valid integer");
            }
        }
        else if (e.getSource() == filterBtn) {
            ImageIcon icon2=new ImageIcon(filtered);
            lbl2.setIcon(icon2);
        } else if (e.getSource() == sobelBtn) {
            ImageIcon icon2=new ImageIcon(sobel);
            lbl2.setIcon(icon2);
        }else if (e.getSource() == nonMaxBtn) {
            ImageIcon icon2=new ImageIcon(nonMax);
            lbl2.setIcon(icon2);
        }else if (e.getSource() == accumulator) {
            buildAccumulator(whichRadius.getValue());
        }

    }

    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if (!source.getValueIsAdjusting()) {
            System.out.println("adjusting");
            buildAccumulator(source.getValue());
            accumulator.setSelected(true);
        }
    }

    private  void buildAccumulator(int r)
    {
        accImage = new BufferedImage ( width, height, filtered.getType() );
        Graphics2D g = accImage.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);
        g.dispose();
        int max = 0;

        double[] a = new double[1];
        for (int y = 0;y < height;++y)
            for (int x = 0; x < width;++x) {
                a[0] = acc[y * width + x][r-rmin]&0xFF;
                accImage.getRaster().setPixel(x, y, a);
            }
        ImageIcon icon2=new ImageIcon(accImage);
        lbl2.setIcon(icon2);
    }

    private void performHysteresis(int low, int high) {
        //NOTE: this implementation reuses the data array to store both
        //luminance data from the image, and edge intensity from the processing.
        //This is done for memory efficiency, other implementations may wish
        //to separate these functions.
        data = new int[width * height];
        Arrays.fill(data, 0);

        int hoffset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (data[hoffset] == 0 && magnitude[hoffset] >= high) {
                    follow(x, y, hoffset, low);
                }
                hoffset++;
            }
        }
    }

    private void follow(int x1, int y1, int i1, int threshold) {
        int x0 = x1 == 0 ? x1 : x1 - 1;
        int x2 = x1 == width - 1 ? x1 : x1 + 1;
        int y0 = y1 == 0 ? y1 : y1 - 1;
        int y2 = y1 == height -1 ? y1 : y1 + 1;

        data[i1] = magnitude[i1];
        for (int x = x0; x <= x2; x++) {
            for (int y = y0; y <= y2; y++) {
                int i2 = x + y * width;
                if ((y != y1 || x != x1)
                        && data[i2] == 0
                        && magnitude[i2] >= threshold) {
                    follow(x, y, i2, threshold);
                    return;
                }
            }
        }
    }

    private void thresholdEdges() {
        for (int i = 0; i < width*height; i++) {
            data[i] = data[i] > 0 ? -1 : 0xff000000;
        }
    }

    private void writeEdges(int pixels[]) {
        //NOTE: There is currently no mechanism for obtaining the edge data
        //in any other format other than an INT_ARGB type BufferedImage.
        //This may be easily remedied by providing alternative accessors.
        //if (nonMax == null) {
            nonMax = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        //}
        nonMax.getWritableTile(0, 0).setDataElements(0, 0, width, height, pixels);
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
        rmax = width > height ? height/2 : width/2;
        accRMax = (rmax + offset - 1)/offset;
        whichRadius.setMaximum(accRMax);
        img = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.drawImage(nonMax, 0, 0, null);
        g.dispose();
        res = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB);
        g = res.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        greyScale = copyImage(nonMax);
        ImageIcon icon=new ImageIcon(img);
        ImageIcon icon2= new ImageIcon(greyScale);
        lbl1.setIcon(icon);
        lbl2.setIcon(icon2);
    }

    public static float[] makeGaussianKernel(int radius, float sigma) {
        float[] kernel = new float[radius * radius];
        float sum = 0;
        for (int y = 0; y < radius; y++) {
            for (int x = 0; x < radius; x++) {
                int goffset = y * radius + x;
                int i = x - radius / 2;
                int j = y - radius / 2;
                kernel[goffset] = (float) Math.pow(Math.E, -(i * i + j * j)
                        / (2 * (sigma * sigma)));
                sum += kernel[goffset];
            }
        }
        for (int i = 0; i < kernel.length; i++)
            kernel[i] /= sum;
        return kernel;
    }

    public void gaussianBlur()
    {
        filtered = null;
        Kernel kernel = new Kernel(5, 5, makeGaussianKernel(5, 1.4f));
        //Kernel kernel = new Kernel(5, 5, gaus);
        ConvolveOp op = new ConvolveOp(kernel);
        filtered = op.filter(greyScale, null);
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

    public void sobel()
    {
        sobel = new BufferedImage ( width, height, filtered.getType() );
        Graphics2D g = sobel.createGraphics();
        g.setColor( new Color ( 0, 0, 0, 0 ));
        g.fillRect(0, 0, width, height);
        g.dispose();

        int[] tmp = new int[1];
        sX = new double[height][width];
        sY = new double[height][width];
        double maxX = 0;
        double maxY = 0;
        for (int y = 1;y < height-1;++y)
            for (int x = 1; x < width-1;++x)
            {
                double Xvalue = 0;
                double Yvalue = 0;
                for (int j = -1;j <=1;++j)
                    for (int i = -1;i <=1;++i)
                    {

                        Xvalue += GX[1+j][1+i] * filtered.getRaster().getPixel(x+i,y+j, tmp)[0];
                        Yvalue += GY[1+j][1+i] * filtered.getRaster().getPixel(x+i,y+j, tmp)[0];
                    }
                if (Xvalue > maxX)
                    maxX = Xvalue;
                if (Yvalue > maxY)
                    maxY = Yvalue;
                sX[y][x] = Xvalue;
                sY[y][x] = Yvalue;

            }

        for (int y = 1;y < height-1;++y)
            for (int x = 1; x < width-1;++x) {
                double[] a = {(Math.abs((sX[y][x]/maxX*255)) + Math.abs((sY[y][x]/maxY)*255))};
                //if (a[0] > 0) binary[y][x] = 1;
                //if (a[0] <= 0) binary[y][x] = 0;

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


         magnitude = new int[height * width];
        for (int y = 1; y < height-1; y++) {
            for (int x = 1; x < width-1;x++) {




                double xGrad = sX[y][x];
                double yGrad = sY[y][x];
                double gradMag = Math.hypot(xGrad, yGrad);

                //perform non-maximal supression
                double nMag = Math.hypot(sX[y-1][x], sY[y-1][x]);
                double sMag = Math.hypot(sX[y+1][x], sY[y+1][x]);
                double wMag = Math.hypot(sX[y][x-1], sY[y][x-1]);
                double eMag = Math.hypot(sX[y][x+1], sY[y][x+1]);
                double neMag = Math.hypot(sX[y-1][x+1], sY[y-1][x+1]);
                double seMag = Math.hypot(sX[y+1][x+1], sY[y+1][x+1]);
                double swMag = Math.hypot(sX[y+1][x-1], sY[y+1][x-1]);
                double nwMag = Math.hypot(sX[y-1][x-1], sY[y-1][x-1]);
                double tmp;
				/*
				 * An explanation of what's happening here, for those who want
				 * to understand the source: This performs the "non-maximal
				 * supression" phase of the Canny edge detection in which we
				 * need to compare the gradient magnitude to that in the
				 * direction of the gradient; only if the value is a local
				 * maximum do we consider the point as an edge candidate.
				 *
				 * We need to break the comparison into a number of different
				 * cases depending on the gradient direction so that the
				 * appropriate values can be used. To avoid computing the
				 * gradient direction, we use two simple comparisons: first we
				 * check that the partial derivatives have the same sign (1)
				 * and then we check which is larger (2). As a consequence, we
				 * have reduced the problem to one of four identical cases that
				 * each test the central gradient magnitude against the values at
				 * two points with 'identical support'; what this means is that
				 * the geometry required to accurately interpolate the magnitude
				 * of gradient function at those points has an identical
				 * geometry (upto right-angled-rotation/reflection).
				 *
				 * When comparing the central gradient to the two interpolated
				 * values, we avoid performing any divisions by multiplying both
				 * sides of each inequality by the greater of the two partial
				 * derivatives. The common comparand is stored in a temporary
				 * variable (3) and reused in the mirror case (4).
				 *
				 */
                if (xGrad * yGrad <= (float) 0 /*(1)*/
                        ? Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
                        ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * neMag - (xGrad + yGrad) * eMag) /*(3)*/
                        && tmp > Math.abs(yGrad * swMag - (xGrad + yGrad) * wMag) /*(4)*/
                        : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * neMag - (yGrad + xGrad) * nMag) /*(3)*/
                        && tmp > Math.abs(xGrad * swMag - (yGrad + xGrad) * sMag) /*(4)*/
                        : Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
                        ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * seMag + (xGrad - yGrad) * eMag) /*(3)*/
                        && tmp > Math.abs(yGrad * nwMag + (xGrad - yGrad) * wMag) /*(4)*/
                        : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * seMag + (yGrad - xGrad) * sMag) /*(3)*/
                        && tmp > Math.abs(xGrad * nwMag + (yGrad - xGrad) * nMag) /*(4)*/
                        ) {
                    magnitude[y*width + x] = gradMag >= MAGNITUDE_LIMIT ? MAGNITUDE_MAX : (int) (MAGNITUDE_SCALE * gradMag);
                    //NOTE: The orientation of the edge is not employed by this
                    //implementation. It is a simple matter to compute it at
                    //this point as: Math.atan2(yGrad, xGrad);
                } else {
                    magnitude[y * width + x] = 0;
                }
            }
        }
//        double[][] gd = new double[height][width];
//        double[][] gm = new double[height][width];
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//
//                // setting gradient magnitude and gradient direction
//                if (sX[y][x] != 0) {
//                    if (sY[y][x] == 0)
//                        if (sX[y][x] > 0)
//                            gd[y][x]= 0;
//                        else
//                            gd[y][x] = Math.PI;
//                    gd[y][x] = Math.atan(sY[y][x] / sX[y][x]);
//                } else {
//                    System.out.println(sX[y][x] + ", " + sY[y][x] + " | " + width +", " + height);
//                    if (sY[y][x] > 0)
//                        gd[y][x] = Math.PI / 2d;
//                    else
//                        gd[y][x] = 3d*Math.PI / 2d;
//                }
//                gm[y][x] = Math.sqrt(sY[y][x] * sY[y][x] + sX[y][x] * sX[y][x]);
//                if (gm[y][x] > 255)  gm[y][x] = 255;
//          //      if (gm[y][x] < 0)  gm[y][x] = 0;
//                gm[y][x] = Math.hypot(sY[y][x], sX[y][x]);
//            }
//        }
//        for (int x = 0; x < width; x++) {
//            nonMax.getRaster().setPixel(x, 0, new int[]{255});
//            nonMax.getRaster().setPixel(x, height - 1, new int[]{255});
//        }
//        for (int y = 0; y < height; y++) {
//            nonMax.getRaster().setPixel(0, y, new int[]{255});
//            nonMax.getRaster().setPixel(width - 1, y, new int[]{255});
//        }
//        for (int y = 1; y < height - 1; y++) {
//            for (int x = 1; x < width - 1; x++) {
//
//               // System.out.println(x + ", " + y + " | " + width +", " + height);
//                //if (gd[y][x] < (Math.PI / 8d) && gd[y][x] >= (-Math.PI / 8d)) {//22.5 |  -22.5
//                if (gd[y][x] < (Math.PI / 8d) && gd[y][x] >= (-Math.PI / 8d)) {
//                    // check if pixel is a local maximum ...
//                    if (gm[y][x] > gm[y + 1][x] && gm[y][x] > gm[y - 1][x])
//                        setPixel(x, y, nonMax, gm[y][x]);
//                    else
//                        nonMax.getRaster().setPixel(x, y, tmp255);
//                } else if (gd[y][x] < (3d * Math.PI / 8d) && gd[y][x] >= (Math.PI / 8d)) { //22.5 | 67.5
//                    // check if pixel is a local maximum ...
//                    if (gm[y][x] > gm[y - 1][x - 1] && gm[y][x] > gm[y + 1][x + 1])
//                        setPixel(x, y, nonMax, gm[y][x]);
//                    else
//                        nonMax.getRaster().setPixel(x, y, tmp255);
//                } else if (gd[y][x] < (-3d * Math.PI / 8d) || gd[y][x] >= (3d * Math.PI / 8d)) {//-67.5 | 67.5
//                    if (gm[y][x] > gm[y][x + 1] && gm[y][x] > gm[y][x - 1])
//                        setPixel(x, y, nonMax, gm[y][x]);
//                    else
//                        nonMax.getRaster().setPixel(x, y, tmp255);
//                } else if (gd[y][x] < (-Math.PI / 8d) && gd[y][x] >= (-3d * Math.PI / 8d)) {//-22.5 | 67.5
//                    if (gm[y][x] > gm[y + 1][x - 1] && gm[y][x] > gm[y - 1][x + 1])
//                        setPixel(x, y, nonMax, gm[y][x]);
//                    else
//                        nonMax.getRaster().setPixel(x, y, tmp255);
//                } else {
//                    nonMax.getRaster().setPixel(x, y, tmp255);
//                }
//            }
//        }
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

    public void Hough()
    {
        // for polar we need accumulator of 180degress * the longest length in the image
        //rmax = (int)Math.sqrt(width*width + height*height);

        System.out.println("w: " + width + " h: " + height + " rmax: " + rmax );
        acc = new int[width * height][accRMax];
        int rOffset;
        for(int x=0;x<height;x++) {
            for(int y=0;y<width;y++) {
                rOffset = 0;
                for (int r = rmin; r < rmax;r+=offset) {
                    acc[x * width + y][rOffset++] = 0;
                }
            }
        }
        System.out.println("accumulating");
        int x0, y0;
        double t;
        int[] val = new int[1];
        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {

                //if ((nonMax.getRaster().getPixel(x, y, val)[0])== -1) {
                if ((data[y*width + x] & 0xFF) == 255){
                    rOffset = 0;
                    for (int r = rmin; r < rmax;r+=offset) {
                        for (int theta = 0; theta < 360; theta+=2) {
                            t = (theta * 3.14159265) / 180;
                            x0 = (int) Math.round(x - r * Math.cos(t));
                            y0 = (int) Math.round(y - r * Math.sin(t));
                            if (x0 < width && x0 > 0 && y0 < height && y0 > 0) {
                                acc[x0 + (y0 * width)][rOffset] += 1;
                            }
                        }
                        rOffset++;
                    }

                }
            }
        }
        // now normalise to 255 and put in format for a pixel array
        int max=0;
        // Find max acc value
        System.out.println("Finding Max");
        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {
                rOffset = 0;
                for (int r = rmin; r < rmax;r+=offset) {
                    if (acc[x + (y * width)][rOffset] > max) {
                        max = acc[x + (y * width)][rOffset];
                    }
                    rOffset++;
                }
            }
        }

        //System.out.println("Max :" + max);

        // Normalise all the values
        int value;
        System.out.println("Normalising");
        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {
                rOffset = 0;
                for (int r = rmin; r < rmax;r+=offset) {
                    value = (int) (((double) acc[x + (y * width)][rOffset] / (double) max) * 255.0);
                    acc[x + (y * width)][rOffset] = 0xff000000 | (value << 16 | value << 8 | value);
                    rOffset++;
                }
            }
        }

        //sobel = new BufferedImage ( width, height, BufferedImage.TYPE_BYTE_GRAY );
        //Graphics2D g = sobel.createGraphics();
        //g.setColor( new Color ( 0, 0, 0, 0 ));
        //g.fillRect(0, 0, width, height);
        //g.dispose();

        findMaxima();

        System.out.println("done");
    }

    private void findMaxima() {
        System.out.println("Finding Circles");
        results = new int[accSize*4];
        int rOffset;
        for(int x=0;x<width;x++) {
            for(int y=0;y<height;y++) {
                rOffset = 0;
                for (int r = rmin;r<rmax;r+=offset) {
                    int value = (acc[x + (y * width)][rOffset] & 0xff);

                    // if its higher than lowest value add it and then sort
                    if (value > results[(accSize - 1) * 4]) {
                        boolean gandalf = false;
                        for (int mj = -2; mj <= 2; mj++)
                            for (int mi = -2; mi <= 2; mi++)
                                for (int mr = -1; mr <= 1; mr++)
                                {
                                    if (x > 1 && x < width - 1 && y > 1 && y < (height - 1) && rOffset > 1) {
                                        if ((acc[((y + mj) * width) + x + mi][rOffset+mr] & 0xff) > (acc[(y * width) + x][rOffset] & 0xFF)) {

                                            //System.out.println("gandalf value : " + (acc[((y + mj) * width) + x + mi][r] & 0xff) + "(" + x + "," + y + ")");
                                            gandalf = true;
                                        }
                                }
                            }
                        if (!gandalf) {
                            //System.out.println("(" + x + ", " + y + ") : (" + results[acc * 4 + 1] + "," + results[acc * 4 + 2] + ")");
                            // add to bottom of array
                            results[(accSize - 1) * 4] = value;
                            results[(accSize - 1) * 4 + 1] = x;
                            results[(accSize - 1) * 4 + 2] = y;
                            results[(accSize - 1) * 4 + 3] = r;

                            // shift up until its in right place
                            int i = (accSize - 2) * 4;
                            while ((i >= 0) && (results[i + 4] > results[i])) {
                                for (int j = 0; j < 4; j++) {
                                    int temp = results[i + j];
                                    results[i + j] = results[i + 4 + j];
                                    results[i + 4 + j] = temp;
                                }
                                i = i - 4;
                                if (i < 0) break;
                            }
                        }
                    }
                    rOffset++;
                }
            }
        }

        double ratio=(double)(width/2)/accSize;
        System.out.println("top "+accSize+" matches:");
        for(int i=accSize-1; i>=0; i--){
            System.out.println("value: " + results[i*4] + ", x: " + results[i*4+1] + ", y: " + results[i*4+2] + ", r: " + results[i*4+3]);
            drawCircle(results[i*4], results[i*4+1], results[i*4+2], results[i*4+3]);
        }
        ImageIcon icon1=new ImageIcon(img);
        lbl1.setIcon(icon1);
    }

    private void setPixel2(int value, int xPos, int yPos) {
        int rgb = img.getRGB(xPos,yPos);
        Color color = new Color(rgb);
        Color res = new Color(value, color.getGreen(), color.getBlue());
        img.setRGB(xPos, yPos, res.getRGB());
    }

    private void drawCircle(int pix, int xCenter, int yCenter, int r) {
        Graphics2D g = res.createGraphics();
        g.setColor(Color.RED);
        g.drawOval(xCenter - r, yCenter-r, r*2, r*2);
        g.dispose();
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
