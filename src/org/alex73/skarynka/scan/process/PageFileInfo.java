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
package org.alex73.skarynka.scan.process;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.common.ImageViewPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//cmd.add("-adaptive-resize");
// dcraw -W -b value   - for custom brightness
public class PageFileInfo {
    private static Logger LOG = LoggerFactory.getLogger(PageFileInfo.class);

    private final Book2 book;
    private final Book2.PageInfo pi;
    private final String page;

    public PageFileInfo(Book2 book, String page) {
        this.book = book;
        this.page = Book2.formatPageNumber(page);
        pi = book.getPageInfo(this.page);
        if (pi == null) {
            throw new RuntimeException("Page not found");
        }
    }

    public Book2.PageInfo getPageInfo() {
        return pi;
    }

    public File getPreviewFile() {
        return new File(book.getBookDir(), page + '.' + pi.pageOriginalFileExt + ".preview.jpg");
    }

    public File getOriginalFile() {
        return new File(book.getBookDir(), page + '.' + pi.pageOriginalFileExt);
    }

    public File getRawFile() {
        return new File(book.getBookDir(), page + ".raw");
    }

    public File getEditFile() {
        return new File(book.getBookDir(), page + "-edit.png");
    }

    public File getDjvuFile() {
        return new File(book.getBookDir(), page + ".djvu");
    }

    public File getJp2File() {
        return new File(book.getBookDir(), page + ".jp2");
    }

    void addRotate(List<String> cmdConvert, int rotate) {
        switch (rotate) {
        case 0:
            break;
        case 1:
            cmdConvert.add("-rotate");
            cmdConvert.add("90");
            break;
        case 2:
            cmdConvert.add("-rotate");
            cmdConvert.add("180");
            break;
        case 3:
            cmdConvert.add("-rotate");
            cmdConvert.add("270");
            break;
        default:
            throw new RuntimeException("Unknown rotate: " + rotate);
        }
    }

    void addResize(List<String> cmdConvert, int scale) {
        if (scale != 0) {
            cmdConvert.add("-adaptive-resize");
            cmdConvert.add(scale + "%");
        }
    }

    public BufferedImage constructPagePreview(int maxWidth, int maxHeight) {

        BufferedImage img;

        File jpeg = getPreviewFile();
        if (!jpeg.exists()) {
            return createErrorImage(maxWidth, maxHeight);
        }
        try {
            img = ImageIO.read(jpeg);
        } catch (Exception ex) {
            LOG.info("Error read page for preview: " + page, ex);
            return createErrorImage(maxWidth, maxHeight);
        }

        BufferedImage result = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();

        AffineTransform transform = ImageViewPane.getAffineTransform(1, 1, pi.rotate, img, maxWidth,
                maxHeight);

        AffineTransform prev = g.getTransform();
        try {
            g.setTransform(transform);
            g.drawImage(img, 0, 0, null);
        } finally {
            g.setTransform(prev);
        }

        Point2D.Double crop1 = new Point2D.Double();
        Point2D.Double crop2 = new Point2D.Double();
        crop1.x = 1.0 * pi.cropPosX / pi.imageSizeX;
        crop1.y = 1.0 * pi.cropPosY / pi.imageSizeY;
        crop2.x = 1.0 * (pi.cropPosX + book.cropSizeX) / pi.imageSizeX;
        crop2.y = 1.0 * (pi.cropPosY + book.cropSizeY) / pi.imageSizeY;
        ImageViewPane.drawCropRectangle(g, img, crop1, crop2, transform);
        g.dispose();

        return result;
    }

    public static BufferedImage scale(BufferedImage orig, int maxWidth, int maxHeight) {
        BufferedImage result = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();

        AffineTransform transform = ImageViewPane.getAffineTransform(1, 1, 0, orig, maxWidth, maxHeight);

        AffineTransform prev = g.getTransform();
        try {
            g.setTransform(transform);
            g.drawImage(orig, 0, 0, null);
        } finally {
            g.setTransform(prev);
        }
        g.dispose();

        return result;
    }

    static BufferedImage createErrorImage(int maxWidth, int maxHeight) {
        BufferedImage result = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = result.createGraphics();
        try {
            g.setColor(Color.RED);
            g.setFont(new Font("Sans Serif", Font.BOLD, 36));
            g.drawString("E", 20, 60);
            g.drawRect(0, 0, maxWidth - 1, maxHeight - 1);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static void exec(List<String> cmd) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Exec: " + cmd);
        }
        Process process = Runtime.getRuntime().exec(cmd.toArray(new String[0]));
        int r = process.waitFor();
        LOG.debug("Result: " + r);
        if (r != 0) {
            throw new Exception("Error execution " + cmd + ": " + r);
        }
    }

    private static Process execNoWait(String cmd) throws Exception {
        LOG.debug("Exec: " + cmd);
        return Runtime.getRuntime().exec(cmd);
    }
}
