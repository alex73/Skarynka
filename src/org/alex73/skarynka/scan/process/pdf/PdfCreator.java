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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Book2.PageInfo;
import org.alex73.skarynka.scan.process.PageFileInfo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

public class PdfCreator {

    public static void create(File outFile, File[] jpegs) throws Exception {
        PdfWriter writer = new PdfWriter(outFile);

        PdfDocument pdf = new PdfDocument(writer);

        Document doc = new Document(pdf);

        for (File jpeg : jpegs) {
            ImageData imgData = ImageDataFactory.create(jpeg.toURI().toURL());

//            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imgData.getData()));
//            ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
//            int orientation = 0;
//            try {
//                if (exifIFD0Directory != null) {
//                    orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
//                }
//            } catch (MetadataException ex) {
//            }

            addPage(imgData, 0/*orientation*/, jpeg, pdf);
        }

        doc.close();
        writer.flush();
        writer.close();
    }

    private static void addPage(ImageData imgData, int orientation, File file, PdfDocument pdf) {
        Image image = new Image(imgData);
        float width, height;
        AffineTransform at=null;
        switch (orientation) {
        case 0:
        case 1:
            width = image.getImageWidth();
            height = image.getImageHeight();
            break;
        case 3:
            width = image.getImageWidth();
            height = image.getImageHeight();
            at = new AffineTransform();
            rotate(2, at, image);
            break;
        case 6:
            width = image.getImageHeight();
            height = image.getImageWidth();
            at = new AffineTransform();
            rotate(1, at, image);
            break;
        case 8:
            width = image.getImageHeight();
            height = image.getImageWidth();
            at = new AffineTransform();
            rotate(3, at, image);
            break;
        default:
            throw new RuntimeException("Wrong orientation for " + file.getAbsolutePath() + ": " + orientation);
        }
        PageSize ps = new PageSize(width, height);
        PdfPage page = pdf.addNewPage(ps);
        PdfCanvas canvas = new PdfCanvas(page);

        if (at != null) {
            canvas.concatMatrix(at);
        }
        canvas.addImageAt(imgData, 0, 0, false);
    }

    private final PdfWriter writer;
    private final PdfDocument pdf;
    private final Document doc;

    public PdfCreator(File outFile) throws Exception {
        writer = new PdfWriter(outFile);
        pdf = new PdfDocument(writer);
        doc = new Document(pdf);
    }

    public void addPage(Book2 book, String pageNum) throws Exception {
        PageFileInfo fi = new PageFileInfo(book, pageNum);
        ImageData imgData = ImageDataFactory.create(fi.getOriginalFile().toURI().toURL());
        PageInfo pi = fi.getPageInfo();
        if (pi.inverted) {
            imgData.setDecode(new float[] { 1, 0, 1, 0, 1, 0, 1, 0 });
        }
        Image image = new Image(imgData);

        float width = pi.rotate % 2 == 0 ? image.getImageWidth() : image.getImageHeight();
        float height = pi.rotate % 2 == 0 ? image.getImageHeight() : image.getImageWidth();

        PageSize ps = new PageSize(width, height);
        PdfPage page = pdf.addNewPage(ps);

        PdfCanvas canvas = new PdfCanvas(page);

        AffineTransform at = new AffineTransform();
        rotate(pi.rotate, at, image);
        if (pi.mirrorHorizontal) {
            at.scale(-1, 1);
            at.translate(-image.getImageWidth(), 0);
        }
        if (pi.mirrorVertical) {
            at.scale(1, -1);
            at.translate(0, -image.getImageHeight());
        }

        canvas.concatMatrix(at);
        canvas.addImageAt(imgData, 0, 0, false);
    }

    public void close() throws Exception {
        doc.close();
        writer.flush();
        writer.close();
    }

    private static void rotate(int rotation, AffineTransform tr, Image image) {
        switch (rotation) {
        case 0:
            break;
        case 1:
            tr.rotate(Math.PI * 3 / 2);
            tr.translate(-image.getImageWidth(), 0);
            break;
        case 2:
            tr.rotate(Math.PI);
            tr.translate(-image.getImageWidth(), -image.getImageHeight());
            break;
        case 3:
            tr.rotate(Math.PI / 2);
            tr.translate(0, -image.getImageHeight());
            break;
        default:
            throw new RuntimeException("rotation=" + rotation);
        }
    }

    public static void main(String[] args) throws Exception {
        Files.find(Paths.get("."), Integer.MAX_VALUE, (p, a) -> {
            if (!a.isDirectory()) {
                return false;
            }
            try {
                boolean r = Files.list(p).filter(pj -> isJpeg(pj)).findFirst().isPresent();
                return r;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }).sorted().forEach(p -> {
            Path pdf = p.getParent().resolve(p.getFileName()+ ".pdf");
            if (Files.exists(pdf)) {
                return;
            }
            Path temp = p.getParent().resolve(p.getFileName()+ ".pdf.temp");
            File[] jpegs = p.toFile().listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return isJpeg(f.toPath());
                }
            });
            Arrays.sort(jpegs);
            try {
                System.out.println(pdf);
                create(temp.toFile(), jpegs);
                Files.move(temp, pdf);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    static boolean isJpeg(Path f) {
        String fn = f.getFileName().toString().toLowerCase();
        return fn.endsWith(".jp2") || fn.endsWith(".jpg") || fn.endsWith(".jpeg");
    }
}
