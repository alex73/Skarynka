package org.alex73.skarynka.scan.process.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class ExtractImages2 {
    public static void main(String[] args) throws Exception {
        Files.find(Paths.get("."), 10, (p, a) -> p.getFileName().toString().matches("сш\\.[0-9]")).sorted()
                .forEach(p -> p(p));
    }

    static void p(Path dir) {
        try {
            Files.find(dir, 10, (p, a) -> p.getFileName().toString().endsWith(".pdf")).sorted()
                    .forEach(p -> process(p));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static void process(Path pdf) {
        count = 0;
        count(pdf);
        if (count == 1) {
            String p = pdf.toString().replaceAll("\\.pdf$", ".jpg");
            wr(pdf, Paths.get(p));
        }
    }

    static int count;

    static void count(Path pdf) {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdf.toFile()))) {
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                new PdfCanvasProcessor(new IEventListener() {
                    @Override
                    public Set<EventType> getSupportedEvents() {
                        return null;
                    }

                    @Override
                    public void eventOccurred(IEventData data, EventType type) {
                        if (type == EventType.RENDER_IMAGE) {
                            count++;
                        }
                    }
                }).processPageContent(page);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static void wr(Path pdf, Path jpeg) {
        System.out.println(pdf);
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdf.toFile()))) {
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                new PdfCanvasProcessor(new IEventListener() {

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
                                Files.write(jpeg, image);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                }).processPageContent(page);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
