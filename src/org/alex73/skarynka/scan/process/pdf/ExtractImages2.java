package org.alex73.skarynka.scan.process.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

public class ExtractImages2 {
    public static final Path DIR = Paths.get("/home/alex/b/");
    public static final Path OUT_DIR = Paths.get("/home/alex/b/out/");

    public static void main(String[] args) throws Exception {
        Files.find(DIR, 10, (p, a) -> p.getFileName().toString().endsWith(".jpg")).sorted().forEach(p -> process(p));
        Files.find(DIR, 10, (p, a) -> p.getFileName().toString().endsWith(".pdf")).sorted().forEach(p -> process(p));
    }

    static final Pattern RE_SH = Pattern
            .compile("([0-9]+\\.[0-9]+\\.[0-9]+a?)/сш\\.([0-9]+)/сканирование([0-9]+)\\.pdf");
    static final Pattern RE_SH_JPG = Pattern
            .compile("([0-9]+\\.[0-9]+\\.[0-9]+a?)/сш\\.([0-9]+)/сканирование([0-9]+)\\.jpg");
    static final Pattern RE_SH2 = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+a?)/сканирование([0-9]+)\\.pdf");
    static final Pattern RE_T3 = Pattern
            .compile("([0-9]+\\.[0-9]+\\.[0-9]+a?)/(T_)?([0-9]+\\.[0-9]+\\.[0-9]+a?)\\.pdf");
    static final Pattern RE_T4 = Pattern
            .compile("([0-9]+\\.[0-9]+\\.[0-9]+a?)/(T_)?([0-9]+\\.[0-9]+\\.[0-9]+a?+\\.[0-9,]+)\\.pdf");

    static Path outDir;
    static int pageIndex;

    static void process(Path pdf) {
        String p = DIR.relativize(pdf).toString().replace(" ", "");
        Matcher m;
        String dir = null, fn = null, page = null;
        if ((m = RE_SH.matcher(p)).matches()) {
            dir = m.group(1);
            fn = m.group(1) + '.' + m.group(2);
            page = m.group(3);
        } else if ((m = RE_SH_JPG.matcher(p)).matches()) {
            dir = m.group(1);
            fn = m.group(1) + '.' + m.group(2);
            page = m.group(3);
        } else if ((m = RE_SH2.matcher(p)).matches()) {
            dir = m.group(1);
            fn = m.group(1);
            page = m.group(2);
        } else if ((m = RE_T3.matcher(p)).matches()) {
            dir = m.group(1);
            fn = m.group(3);
            page = null;
        } else if ((m = RE_T4.matcher(p)).matches()) {
            dir = m.group(1);
            fn = m.group(3);
            page = null;
        } else {
            dir = p.replaceAll("\\.[a-z]+$", "");
            fn = dir;
        }
        if (!fn.startsWith(dir)) {
            throw new RuntimeException(p);
        }
        outDir = OUT_DIR.resolve(fn);
        if (page == null) {
            if (Files.isDirectory(outDir)) {
                System.out.println("Already exist: " + outDir);
            }
        }
        try {
            Files.createDirectories(outDir);

            if (p.toString().endsWith(".jpg")) {
                pageIndex = Integer.parseInt(page);
                Path jpeg = outDir.resolve("page-" + PFMT.format(pageIndex) + ".jpg");
                Files.copy(pdf, jpeg);
                return;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        count = 0;
        count(pdf);
        if (page == null) {
            if (count < 3) {
                throw new RuntimeException(p);
            }
            pageIndex = 1;
        } else {
            if (count != 1) {
                throw new RuntimeException(p);
            }
            pageIndex = Integer.parseInt(page);
        }
        wr(pdf);
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

    static final DecimalFormat PFMT = new DecimalFormat("0000");

    static void wr(Path pdf) {
        System.out.println("Parse " + pdf);
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
                                Path jpeg = outDir.resolve("page-" + PFMT.format(pageIndex) + ".jpg");
                                if (Files.exists(jpeg)) {
                                    throw new RuntimeException(jpeg.toString());
                                }
                                pageIndex++;
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
