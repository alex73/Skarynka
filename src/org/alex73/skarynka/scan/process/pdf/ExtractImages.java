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

public class ExtractImages {
    public static final Path DIR = Paths.get(".");
    public static final String OUT = "%04d%s.jpg";
    static int outp = 0;

    public static void main(String[] args) throws Exception {
        Files.find(DIR, 10, (p, a) -> p.getFileName().toString().toLowerCase().endsWith(".pdf")).sorted()
                .forEach(p -> process(p));
    }

    static void process(Path p) {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(p.toFile()))) {
            int prevp = 0;
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
                            if (ii != prevp) {
                                outp++;
                                suffix = "";
                            }
                            ImageRenderInfo ri = (ImageRenderInfo) data;
                            byte[] image = ri.getImage().getImageBytes();
                            try {
                                Path out = p.getParent().resolve(String.format(OUT, outp, suffix));
                                Files.write(out, image);
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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
