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
package org.alex73.skarynka.scan.devices;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.common.ImageViewPane;

/**
 * Stub device. It uses predefined images from files.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class Stub implements ISourceDevice {
    private final String dir;
    private ImageViewPane[] preview;

    private int c;

    public Stub(String dir) {
        this.dir = dir;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean[] setPreviewPanels(ImageViewPane... panels) {
        this.preview = panels;
        showPreview();

        return new boolean[] { true, true };
    }

    void showPreview() {
        int p = c + 1;
        try {
            BufferedImage p1 = ImageIO.read(new File("/home/alex/MyShare-Temp/c/c1/" + p + ".JPG"));
            BufferedImage p2 = ImageIO.read(new File("/home/alex/MyShare-Temp/c/c2/" + p + ".JPG"));
            preview[0].displayImage(p1, 1, 1);
            preview[1].displayImage(p2, 1, 1);
        } catch (Exception ex) {
        }
    }

    @Override
    public int getZoom() {
        return 0;
    }

    @Override
    public Dimension[] getImageSize() {
        Dimension[] sizes = new Dimension[2];
        sizes[0] = new Dimension(5248, 3920);
        sizes[1] = new Dimension(5248, 3920);
        return sizes;
    }

    @Override
    public int[] getRotations() {
        return new int[] { 3, 1 };
    }

    @Override
    public String[] scan(String... pathsToOutput) throws Exception {
        c++;
        Files.copy(Paths.get("/home/alex/MyShare-Temp/c/c1/" + c + ".JPG"),
                Paths.get(pathsToOutput[0] + ".JPG"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get("/home/alex/MyShare-Temp/c/c1/" + c + ".CRW"),
                Paths.get(pathsToOutput[0] + ".CRW"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get("/home/alex/MyShare-Temp/c/c2/" + c + ".JPG"),
                Paths.get(pathsToOutput[1] + ".JPG"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get("/home/alex/MyShare-Temp/c/c2/" + c + ".CRW"),
                Paths.get(pathsToOutput[1] + ".CRW"), StandardCopyOption.REPLACE_EXISTING);
        showPreview();

        return new String[] { "s1", "s2" };
    }

    @Override
    public String[] getStatus() throws Exception {
        return new String[] { Messages.getString("CAMERA_INFO", 20, 20),
                Messages.getString("CAMERA_INFO", 20, 20) };
    }

    @Override
    public boolean readyForScan() {
        return true;
    }
}
