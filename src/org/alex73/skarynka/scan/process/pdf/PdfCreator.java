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
package org.alex73.skarynka.scan.process.pdf;

import java.io.File;
import java.io.FileOutputStream;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Jpeg;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfCreator {

    public static void create(File outFile, File[] jpegs) throws Exception {
        Document pdf = new Document();
        pdf.setMargins(0, 0, 0, 0);

        Image image0 = Jpeg.getInstance(jpegs[0].getPath());
        pdf.setPageSize(new Rectangle(0, 0, image0.getScaledWidth(), image0.getScaledHeight()));

        PdfWriter writer = PdfWriter.getInstance(pdf, new FileOutputStream(outFile));

        pdf.open();
        float minWidth = Float.MAX_VALUE, maxWidth = Float.MIN_VALUE, minHeight = Float.MAX_VALUE,
                maxHeight = Float.MIN_VALUE;
        for (File jpeg : jpegs) {

            Image image = Jpeg.getInstance(jpeg.getPath());

            float width, height;

            width = image.getScaledWidth();
            height = image.getScaledHeight();
            minWidth = Math.min(minWidth, width);
            maxWidth = Math.max(maxWidth, width);
            minHeight = Math.min(minHeight, height);
            maxHeight = Math.max(maxHeight, height);

            pdf.setPageSize(new Rectangle(0, 0, width, height));
            pdf.newPage();
            pdf.add(image);
        }

        pdf.close();
        writer.flush();
        writer.close();
    }
}
