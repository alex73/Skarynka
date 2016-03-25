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

import org.alex73.skarynka.scan.common.ImageViewPane;

/**
 * Interface for one source device like cameras or scanner.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public interface ISourceDevice {
    boolean readyForScan();

    boolean[] setPreviewPanels(ImageViewPane... panels);

    int getZoom();

    Dimension[] getImageSize();

    int[] getRotations();

    /**
     * Some path can be null that means scan from this side should be skipped.
     */
    String[] scan(String... pathsToOutput) throws Exception;

    /**
     * Get device status text.
     */
    String[] getStatus() throws Exception;

    void close();
}
