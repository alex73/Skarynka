package org.alex73.skarynka.scan.process.pdf;

import java.text.DecimalFormat;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

public class PdfInsert {

    static String IN = "/home/alex/e/in.pdf";
    static String OUT = "/home/alex/e/out.pdf";
    static float scale = 0.25f;

    static PdfDocument pdfDocument;

    public static void main(String[] args) throws Exception {
        pdfDocument = new PdfDocument(new PdfReader(IN), new PdfWriter(OUT));
        Document completeDocument = new Document(pdfDocument);

        for (int i = 1; i <= 39; i++) {
            addPage("/home/alex/e/" + new DecimalFormat("00").format(i) + ".png.jpg", i + 1);
        }

        for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
            System.out.println(i + " " + pdfDocument.getPage(i).getPageSize());
        }
        completeDocument.close();
    }

    static void addPage(String file, int idx) throws Exception {
        ImageData imgData = ImageDataFactory.create(file);
        Image image = new Image(imgData);

        PageSize ps = new PageSize(image.getImageWidth() * scale, image.getImageHeight() * scale);
        PdfPage page = pdfDocument.addNewPage(idx, ps);
        PdfCanvas canvas = new PdfCanvas(page);
        canvas.addImageFittedIntoRectangle(imgData, ps, false);
    }
}
