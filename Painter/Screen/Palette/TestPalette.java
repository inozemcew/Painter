package Painter.Screen.Palette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by ainozemtsev on 22.03.16.
 */
public class TestPalette {
    static int luma = 128;
    static boolean clamp = false;

    public static void main(String[] argv) {
        JFrame frame = new JFrame("Palette test");
        frame.setLayout(new BorderLayout());
        JSlider slider = new JSlider(SwingConstants.VERTICAL);
        slider.setMaximum(255);
        slider.setValue(128);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(16);
        frame.add(slider,BorderLayout.LINE_START);

        YUVPane yuv = new YUVPane();
        frame.add(yuv,BorderLayout.CENTER);
        slider.addChangeListener(e -> yuv.setLuma(slider.getValue()));

        JCheckBox clampCheckBox = new JCheckBox("Clamp");
        clampCheckBox.setSelected(clamp);
        frame.add(clampCheckBox, BorderLayout.PAGE_START);
        clampCheckBox.addChangeListener(e -> {
            clamp = clampCheckBox.isSelected();
            yuv.repaint();
        });

        JComponent c = new Selected();
        c.setPreferredSize(new Dimension(64*9,512));
        frame.add(c,BorderLayout.LINE_END);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    static Color YUVtoColor(int y,int uu, int vv) {
        float v = (vv - 128)*0.77f;
        float u = (uu - 128) * 0.6f;
        float r = y + 1.412f * v ;
        float g = y - 0.354f * u  - 0.724f * v;
        float b = y + 1.782f * u ;
        //r =r/2+64;
        //b = b/2+64;
        //g = g/2+64;
        if (clamp) {
            int lo = 0, hi = 255;
            if (r < 0) r = lo;
            if (r > 255) r = hi;
            if (g < 0) g = lo;
            if (g > 255) g = hi;
            if (b < 0) b = lo;
            if (b > 255) b = hi;
        } else {
            if ((r < 0) || (r > 255) || (g < 0) || (g > 255) || (b < 0) || (b > 255)) {
                r = 0;
                b = 0;
                g = 0;
            }
        }
        return new Color(Math.round(r),Math.round(g),Math.round(b));
    }

    private static class YUVPane extends JComponent {
        int luma = 128;
        Point selected = null;

        public YUVPane() {
            setPreferredSize(new Dimension(512,576));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if(new Rectangle(0,0,510,510).contains(e.getPoint())) {
                        selected = e.getPoint();
                        repaint();
                    }
                }
            });
        }

        public void setLuma(int luma) {
            this.luma = luma;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int x = 0; x < 256; x++) {
                for (int y = 0; y < 256; y++) {
                    g.setColor(YUVtoColor(luma,x,255-y));
                    g.fillRect(x*2,y*2,2,2);
                }
            }
            g.setColor(new Color(128,128,128));
            for (int i = 0; i < 512; i+=51) {
                g.drawLine(i,0,i,510);
                g.drawLine(0,i,510,i);
            }
            g.drawOval(248,248,16,16);
            if (selected != null) {
                final Color color = YUVtoColor(luma, selected.x / 2, 255 - selected.y / 2);
                g.setColor(color);
                g.fillRect(0,512,64,64);
                g.setColor(Color.black);
                g.drawString(color.toString(),80,528);
            }
        }


    }

    private static class Selected extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            int[] uv = {32, 128, 160, 240};
            int[] U = {32, 128,  192, 32, 128, 192};
            int[] V = {32, 128,  224, 64, 128, 224};

            //int[] Y = {0,36,64,96,128,160,192,250};
            super.paintComponent(g);
            final int chromaCount = 3;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int x = j* chromaCount + i+4;
                    for (int y = 0; y < 8; y++) {
                    int k = (y>3)? 3:0;
                        g.setColor(YUVtoColor(255/7*y, 128 + (int)Math.round(i*56f*Math.cos((y-3.5f)/3.5*3.14)) , 128 +j*72 ));
                        g.fillRect(x * 64, y * 64, 64, 64);
                    }
                }
            }
        }
    }

}
