/**************************************************************************
 Skarynka - software for scan, process scanned images and build books

 Copyright (C) 2016 Aleś Bułojčyk

 This file is part of Skarynka.

 Skarynka is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Skarynka is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/
package org.alex73.skarynka.scan.common;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Widget for display page with ability to proportional scale, crop, rotate,
 * etc.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
@SuppressWarnings("serial")
public class ImageViewPane extends JComponent {
    private static Logger LOG = LoggerFactory.getLogger(ImageViewPane.class);

    private volatile BufferedImage img;
    private volatile String pageNumber;
    private int rotation;
    private boolean mirrorHorizontal, mirrorVertical, inverted;
    private boolean strikeout;
    double sourceAspectWidth = 1;
    double sourceAspectHeight = 1;
    private AffineTransform transform;
    private Dimension imageSize;
    private List<Rectangle2D.Double> crops = new ArrayList<>();

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int r) {
        this.rotation = r;
        repaint();
    }

    public void setMirrorHorizontal(boolean mirrorHorizontal) {
        this.mirrorHorizontal = mirrorHorizontal;
        repaint();
    }

    public void setMirrorVertical(boolean mirrorVertical) {
        this.mirrorVertical = mirrorVertical;
        repaint();
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
        repaint();
    }

    public boolean getStrikeOut() {
        return strikeout;
    }

    public void setStrikeout(boolean strikeout) {
        this.strikeout = strikeout;
        repaint();
    }

    public void displayImage(BufferedImage image, double aspectWidth, double aspectHeight) {
        this.img = image;
        sourceAspectWidth = aspectWidth;
        sourceAspectHeight = aspectHeight;
        repaint();
    }

    public BufferedImage getImage() {
        return img;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
        repaint();
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // need to remember image locally because other thread can replace it
        BufferedImage image = inverted ? invert(img) : img;
        if (image != null) {
            paintImage(g2, image, sourceAspectWidth, sourceAspectHeight, rotation);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));

        String p = pageNumber;
        if (p != null && p.length() > 0) {
            g2.setColor(Color.RED);
            paintPageNumber(g2, p);
        }
        for(Rectangle2D.Double crop:crops) {
            drawCropRectangle(g2, img, crop, transform);
        }
        if (strikeout) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(10));
            g2.drawLine(0, 0, getWidth(), getHeight());
            g2.drawLine(getWidth(), 0, 0, getHeight());
        }
    }

    /**
     * Draw image to center of panel with scale for maximaze with aspect ratio
     * preserve.
     *
     * @param g2
     *            output graphics
     * @param img
     *            source image
     * @param sourceScaleWidth
     *            source image width scale factor
     * @param sourceScaleHeight
     *            source image height scale factor
     * @param rotation
     *            number of 90 gradus rotation clockwise, as for {see
     *            java.awt.geom.AffineTransform#quadrantRotate(int
     *            numquadrants)}
     */
    private void paintImage(Graphics2D g2, BufferedImage img, double sourceScaleWidth, double sourceScaleHeight,
            int rotation) {

        transform = getAffineTransform(sourceScaleWidth, sourceScaleHeight, rotation, mirrorHorizontal, mirrorVertical, img, getWidth(), getHeight());
        imageSize = new Dimension(img.getWidth(), img.getHeight());

        // draw
        AffineTransform prev = g2.getTransform();
        try {
            AffineTransform c = new AffineTransform(prev);
            c.concatenate(transform);
            g2.setTransform(c);
            g2.drawImage(img, 0, 0, null);
        } finally {
            g2.setTransform(prev);
        }
    }

    public static void drawCropRectangle(Graphics2D g2, BufferedImage image, Rectangle2D.Double crop,
            AffineTransform transform) {
        try {
            double x1, y1, x2, y2;
            x1 = crop.getMinX() * image.getWidth();
            y1 = crop.getMinY() * image.getHeight();
            x2 = crop.getMaxX() * image.getWidth();
            y2 = crop.getMaxY() * image.getHeight();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Crop rectangle in image pixels: " + x1 + "," + y1 + " - " + x2 + "," + y2);
            }
            Point2D.Double p1 = new Point2D.Double();
            Point2D.Double p2 = new Point2D.Double();
            transform.transform(new Point2D.Double(x1, y1), p1);
            transform.transform(new Point2D.Double(x2, y2), p2);
            Rectangle2D.Double drawRect = new Rectangle2D.Double(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
            if (drawRect.width < 0) {
                drawRect.width = -drawRect.width;
                drawRect.x -= drawRect.width;
            }
            if (drawRect.height < 0) {
                drawRect.height = -drawRect.height;
                drawRect.y -= drawRect.height;
            }
            if (crop.getMinX() < 0 || crop.getMinY() < 0 || crop.getMaxX() > 1 || crop.getMaxY() > 1) {
                // out of real image
                g2.setColor(Color.YELLOW);
            } else {
                g2.setColor(Color.RED);
            }
            int x = (int) Math.round(drawRect.x);
            int y = (int) Math.round(drawRect.y);
            int w = (int) Math.round(drawRect.width);
            int h = (int) Math.round(drawRect.height);
            g2.drawRect(x, y, w, h);
        } catch (Exception ex) {
            LOG.error("Error convert image to mouse coordinates", ex);
        }
    }

    public static BufferedImage invert(BufferedImage img) {
        byte reverse[] = new byte[256];
        for (int i = 0; i < 256; i++) {
            reverse[i] = (byte) (255 - i);
        }
        ByteLookupTable lookupTable = new ByteLookupTable(0, reverse);
        LookupOp lop = new LookupOp(lookupTable, null);
        return lop.filter(img, null);
    }

    private void paintPageNumber(Graphics2D g2, String page) {
        Font font = new Font(Font.SERIF, Font.PLAIN, 120);
        int baseLine = font.getBaselineFor(page.charAt(0));
        g2.setFont(font);
        Rectangle2D bounds = g2.getFontMetrics().getStringBounds(page, g2);
        g2.drawString(page, Math.round((getWidth() - bounds.getWidth()) / 2), getHeight() - baseLine - 20);
    }

    public static AffineTransform getAffineTransform(double sourceScaleWidth, double sourceScaleHeight, int rotation, boolean mirrorHorizontal, boolean mirrorVertical,
            BufferedImage img, int outputWidth, int outputHeight) {
        if (sourceScaleWidth < 0.01 || sourceScaleWidth > 100) {
            throw new IllegalArgumentException("Wrong source scale width");
        }
        if (sourceScaleHeight < 0.01 || sourceScaleHeight > 100) {
            throw new IllegalArgumentException("Wrong source scale height");
        }
        if (rotation < 0 || rotation > 3) {
            throw new IllegalArgumentException("Wrong rotation");
        }

        // define width and height for image with source scale factor and
        // rotation
        double willWidth = 0, willHeight = 0;
        switch (rotation) {
        case 0:
        case 2:
            willWidth = img.getWidth() * sourceScaleWidth;
            willHeight = img.getHeight() * sourceScaleHeight;
            break;
        case 1:
        case 3:
            willWidth = img.getHeight() * sourceScaleHeight;
            willHeight = img.getWidth() * sourceScaleWidth;
            break;
        }

        // define scale factor for maximize image in panel
        double scaleForMaximizeInside = Math.min(outputWidth / willWidth, outputHeight / willHeight);
        willWidth *= scaleForMaximizeInside;
        willHeight *= scaleForMaximizeInside;

        // define offset for place image to center of panel
        double offX = (outputWidth - willWidth) / 2;
        double offY = (outputHeight - willHeight) / 2;

        // create transformation
        AffineTransform at = new AffineTransform();

        at.translate(willWidth / 2 + offX, willHeight / 2 + offY);
        at.quadrantRotate(rotation);
        at.scale(sourceScaleWidth * scaleForMaximizeInside, sourceScaleHeight * scaleForMaximizeInside);
        at.translate(-img.getWidth() / 2, -img.getHeight() / 2);
        if (mirrorHorizontal) {
            at.scale(-1, 1);
            at.translate(-img.getWidth(), 0);
        }
        if (mirrorVertical) {
            at.scale(1, -1);
            at.translate(0, -img.getHeight());
        }
        return at;
    }

    public Point mouseToImage(Point mouse, int offX, int offY, Dimension fullImageSize) {
        try {
            Point2D p = new Point2D.Double();
            Point m = new Point(mouse.x - offX, mouse.y - offY);
            transform.inverseTransform(m, p);
            int x = (int) Math.round(p.getX() / imageSize.width * fullImageSize.width);
            int y = (int) Math.round(p.getY() / imageSize.height * fullImageSize.height);
            if (x < 0) {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            }
            if (x > fullImageSize.width) {
                x = fullImageSize.width;
            }
            if (y > fullImageSize.height) {
                y = fullImageSize.height;
            }
            return new Point(x, y);
        } catch (Exception ex) {
            LOG.error("Error convert mouse to image coordinates", ex);
            return null;
        }
    }

    public List<Rectangle2D.Double> getCrops() {
        return crops;
    }

    public Rectangle getImageRect(Rectangle2D.Double crop) {
        double x1, y1, x2, y2;
        x1 = crop.getMinX() * img.getWidth();
        y1 = crop.getMinY() * img.getHeight();
        x2 = crop.getMaxX() * img.getWidth();
        y2 = crop.getMaxY() * img.getHeight();
        Rectangle2D.Double drawRect = new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
        if (drawRect.width < 0) {
            drawRect.width = -drawRect.width;
            drawRect.x -= drawRect.width;
        }
        if (drawRect.height < 0) {
            drawRect.height = -drawRect.height;
            drawRect.y -= drawRect.height;
        }
        return drawRect.getBounds();
    }

    public Rectangle2D.Double screen2image(Rectangle cropRectangle, Dimension fullImageSize) {
        Rectangle2D.Double crop = new Rectangle2D.Double();
        crop.x = cropRectangle.getX() / fullImageSize.width;
        crop.y = cropRectangle.getY() / fullImageSize.height;
        crop.width = (cropRectangle.getX() + cropRectangle.getWidth()) / fullImageSize.width - crop.x;
        crop.height = (cropRectangle.getY() + cropRectangle.getHeight()) / fullImageSize.height - crop.y;
        if (LOG.isTraceEnabled()) {
            LOG.info("Crop rectangle in 0..1: " + crop);
        }
        return crop;
    }
}
