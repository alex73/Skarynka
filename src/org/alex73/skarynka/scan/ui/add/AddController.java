package org.alex73.skarynka.scan.ui.add;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.process.PageFileInfo;
import org.alex73.skarynka.scan.process.ProcessDaemon;
import org.alex73.skarynka.scan.ui.book.PanelEditController;
import org.apache.commons.io.FileUtils;

public class AddController {
    private static File currentDir = new File(Context.getBookDir());

    // add all new books and image files
    public static void addAll() {
        LongProcessAllBooks process = new LongProcessAllBooks();
        process.execute();
        process.showDialog();
        DataStorage.refreshBookPanels(false);
        DataStorage.activateTab(0);
    }

    // add image file into one book only
    public static void add(PanelEditController panelController) {
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(true);
        fc.setCurrentDirectory(currentDir);
        fc.addChoosableFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return Messages.getString("ADD_FILTER_IMAGES");
            }

            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                return Book2.IMAGE_FILE.matcher(pathname.getName()).matches();
            }
        });
        fc.setAcceptAllFileFilterUsed(false);

        int returnVal = fc.showOpenDialog(DataStorage.mainFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentDir = fc.getCurrentDirectory();
            LongProcess process = new LongProcess(panelController, fc.getSelectedFiles());
            process.execute();
            process.showDialog();
        }
    }

    /** Show glass pane. */
    protected static void showGlassPane() {
        ((RootPaneContainer) DataStorage.mainFrame).getGlassPane().setVisible(true);
    }

    /** Hide glass pane. */
    protected static void hideGlassPane() {
        ((RootPaneContainer) DataStorage.mainFrame).getGlassPane().setVisible(false);
    }

    abstract static class ProcessPages extends SwingWorker<Void, Void> {
        protected final AddPages dialog;
        private boolean stop;

        public ProcessPages() {
            dialog = new AddPages(DataStorage.mainFrame, true);
            dialog.btnCancel.addActionListener(e -> {
                stop = true;
            });
            dialog.setLocationRelativeTo(DataStorage.mainFrame);
        }

        protected void addPagesToBook(Book2 book, File[] files) throws Exception {
            String nextPage;
            List<String> ps = book.listPages();
            if (ps.isEmpty()) {
                nextPage = Book2.formatPageNumber("1");
            } else {
                nextPage = Book2.incPage(Book2.simplifyPageNumber(ps.get(ps.size() - 1)), 1);
            }
            String[] pages = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                pages[i] = nextPage;
                nextPage = Book2.incPage(nextPage, 1);
            }
            for (int i = 0; i < files.length; i++) {
                String fn = files[i].getName();
                String ext = fn.substring(fn.lastIndexOf('.') + 1).toLowerCase();
                File fo = new File(book.getBookDir(), pages[i] + '.' + ext);
                if (fo.exists()) {
                    throw new Exception("File " + fo + " already exist !");
                }
            }
            for (int i = 0; i < files.length; i++) {
                if (stop) {
                    throw new InterruptedException("Спынена");
                }
                String fn = files[i].getName();
                dialog.text.setText(Messages.getString("DIALOG_ADDPAGE_TEXT", fn));
                String ext = fn.substring(fn.lastIndexOf('.') + 1).toLowerCase();
                File fo = new File(book.getBookDir(), pages[i] + '.' + ext);

                try {
                    FileUtils.moveFile(files[i], fo);
                } catch (Exception ex) {
                    throw new Exception("Error rename " + files[i] + " to " + fo + " !");
                }

                Book2.PageInfo pi = book.new PageInfo(pages[i]);

                try (ImageInputStream iis = ImageIO.createImageInputStream(fo)) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                    if (!readers.hasNext()) {
                        throw new Exception("Error read image file meta: " + fo.getAbsolutePath());
                    }
                    ImageReader rd = readers.next();
                    rd.setInput(iis, true);
                    pi.imageSizeX = rd.getWidth(0);
                    pi.imageSizeY = rd.getHeight(0);
                }

                pi.pageOriginalFileExt = ext;
                book.addPage(pi);
                ProcessDaemon.createPreviewIfNeed(book, pages[i]);
                dialog.progress.setValue(i + 1);
                book.save();
            }
        }
    }

    /**
     * SwingWorker extension for controllers.
     */
    public static class LongProcessAllBooks extends ProcessPages {
        boolean empty;

        public void showDialog() {
            dialog.setVisible(true);
        }

        @Override
        protected Void doInBackground() throws Exception {
            File[] dirs = new File(Context.getBookDir()).listFiles(new java.io.FileFilter() {
                public boolean accept(File p) {
                    return p.isDirectory();
                }
            });
            List<Book2> books = new ArrayList<>();
            List<List<String>> bookFiles = new ArrayList<>();
            int totalNewPagesCount = 0;
            for (File d : dirs) {
                Book2 book = DataStorage.openBook(d.getName(), false);
                List<String> newPageFiles = new ArrayList<>();
                for (File f : d.listFiles()) {
                    if (f.isDirectory()) {
                        continue;
                    }
                    Matcher m = Book2.RE_PAGE_IMAGE_FILE.matcher(f.getName());
                    if (m.matches()) {
                        Book2.PageInfo pi = book.getPageInfo(m.group(1));
                        if (pi == null) {
                            throw new Exception("No page in book " + d.getName() + " from file: " + f.getName());
                        }
                        if (!new PageFileInfo(book, m.group(1)).getOriginalFile().getName().equals(f.getName())) {
                            throw new Exception(
                                    "Wrong page extension in book " + d.getName() + " for file: " + f.getName());
                        }
                    } else {
                        String ext = f.getName().substring(f.getName().lastIndexOf('.') + 1).toLowerCase();
                        for (String te : Book2.IMAGE_EXTENSIONS) {
                            if (ext.equals(te)) {
                                newPageFiles.add(f.getName());
                                break;
                            }
                        }
                    }
                }
                if (!newPageFiles.isEmpty()) {
                    Collections.sort(newPageFiles);
                    books.add(book);
                    bookFiles.add(newPageFiles);
                    totalNewPagesCount += newPageFiles.size();
                }
            }
            empty = books.isEmpty();

            dialog.progress.setMaximum(totalNewPagesCount);
            dialog.progress.setValue(0);

            for (int i = 0; i < books.size(); i++) {
                Book2 b = books.get(i);
                List<String> newPageFiles = bookFiles.get(i);
                File[] files = new File[newPageFiles.size()];
                for (int j = 0; j < files.length; j++) {
                    files[j] = new File(b.getBookDir(), newPageFiles.get(j));
                }
                addPagesToBook(b, files);
            }

            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                if (empty) {
                    JOptionPane.showMessageDialog(DataStorage.mainFrame, "Няма файлаў для імпарту", "Дадаць файлы",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(DataStorage.mainFrame, "Усе файлы імпартаваныя", "Дадаць файлы",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (InterruptedException ex) {
                JOptionPane.showMessageDialog(DataStorage.mainFrame, "Interrupted: " + ex.getMessage(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
            } catch (ExecutionException e) {
                Throwable ex = e.getCause();
                ex.printStackTrace();
                JOptionPane.showMessageDialog(DataStorage.mainFrame, "Error: " + ex.getMessage(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
            }
            dialog.dispose();
        }
    }

    /**
     * SwingWorker extension for controllers.
     */
    public static class LongProcess extends ProcessPages {
        private final PanelEditController panelController;
        private final File[] files;

        public LongProcess(PanelEditController panelController, File[] files) {
            this.panelController = panelController;
            this.files = files;
        }

        public void showDialog() {
            dialog.setVisible(true);
        }

        @Override
        protected Void doInBackground() throws Exception {
            dialog.progress.setMaximum(files.length);
            dialog.progress.setValue(0);

            addPagesToBook(panelController.getBook(), files);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                JOptionPane.showMessageDialog(DataStorage.mainFrame, "Файлы імпартаваныя", "Дадаць файлы",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (InterruptedException ex) {
                JOptionPane.showMessageDialog(DataStorage.mainFrame, "Interrupted: " + ex.getMessage(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
            } catch (ExecutionException e) {
                Throwable ex = e.getCause();
                ex.printStackTrace();
                JOptionPane.showMessageDialog(DataStorage.mainFrame, "Error: " + ex.getMessage(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
            }
            dialog.dispose();
            panelController.show();
        }
    }
}
