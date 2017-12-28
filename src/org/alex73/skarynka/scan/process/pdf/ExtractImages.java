package org.alex73.skarynka.scan.process.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

public class ExtractImages {
    public static final String PDF = "/tmp/01.pdf";
    public static final String OUT = "/tmp/01-%04d%s.jpg";

    public static void main(String[] args) throws Exception {
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(PDF));

        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            final int ii = i;
            PdfPage page = pdfDoc.getPage(i);
            new PdfCanvasProcessor(new IEventListener() {
                String suffix = "";

                @Override
                public Set<EventType> getSupportedEvents() {
                    return null;
                }

                @Override
                public void eventOccurred(IEventData data, EventType type) {
                    if (type == EventType.RENDER_IMAGE) {
                        ImageRenderInfo ri = (ImageRenderInfo) data;
                        byte[] image = ri.getImage().getImageBytes();
                        try {
                            Files.write(Paths.get(String.format(OUT, ii, suffix)), image);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        if (suffix.isEmpty()) {
                            suffix = "a";
                        } else {
                            suffix = Character.toString((char) (suffix.charAt(0) + 1));
                        }
                    }
                }
            }).processPageContent(page);
        }

        pdfDoc.close();
    }
}
