package org.alex73.skarynka.scan.ui.add;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.process.ProcessDaemon;
import org.alex73.skarynka.scan.ui.book.PanelEditController;
import org.apache.commons.io.FileUtils;

import com.itextpdf.text.Image;

public class AddController {
    private static File currentDir = new File(Context.getBookDir());

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
                String n = pathname.getName().toLowerCase();
                for (String ext : Book2.IMAGE_EXTENSIONS) {
                    if (n.endsWith('.' + ext)) {
                        return true;
                    }
                }
                return false;
            }
        });
        fc.setAcceptAllFileFilterUsed(false);

        int returnVal = fc.showOpenDialog(DataStorage.mainFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentDir = fc.getCurrentDirectory();
            new LongProcess(panelController, fc.getSelectedFiles()).execute();
        }
    }

    /** Show glass pane. */
    protected static void showProgress() {
        ((RootPaneContainer) DataStorage.mainFrame).getGlassPane().setVisible(true);
    }

    /** Hide glass pane. */
    protected static void hideProgress() {
        ((RootPaneContainer) DataStorage.mainFrame).getGlassPane().setVisible(false);
    }

    /**
     * SwingWorker extension for controllers.
     */
    public static class LongProcess extends SwingWorker<Void, Void> {
        private final PanelEditController panelController;
        private final File[] files;

        public LongProcess(PanelEditController panelController, File[] files) {
            this.panelController = panelController;
            this.files = files;
            showProgress();
        }

        @Override
        protected Void doInBackground() throws Exception {

            String[] pages = new String[files.length];

            DataStorage.mainFrame.progress.setMaximum(files.length);
            DataStorage.mainFrame.progress.setValue(0);

            String nextPage;
            List<String> ps = panelController.getBook().listPages();
            if (ps.isEmpty()) {
                nextPage = Book2.formatPageNumber("1");
            } else {
                nextPage = Book2.incPage(Book2.simplifyPageNumber(ps.get(ps.size() - 1)), 1);
            }
            for (int i = 0; i < files.length; i++) {
                pages[i] = nextPage;
                nextPage = Book2.incPage(nextPage, 1);
            }
            for (int i = 0; i < files.length; i++) {
                String fn = files[i].getName();
                String ext = fn.substring(fn.lastIndexOf('.') + 1).toLowerCase();
                File fo = new File(panelController.getBook().getBookDir(), pages[i] + '.' + ext);
                File fpreview = new File(fo.getAbsolutePath() + ".preview.jpg");
                if (fo.exists() || fpreview.exists()) {
                    throw new Exception("File " + fo + " already exist !");
                }
            }
            for (int i = 0; i < files.length; i++) {
                String fn = files[i].getName();
                String ext = fn.substring(fn.lastIndexOf('.') + 1).toLowerCase();
                File fo = new File(panelController.getBook().getBookDir(), pages[i] + '.' + ext);

                try {
                    FileUtils.moveFile(files[i], fo);
                } catch (Exception ex) {
                    throw new Exception("Error rename " + files[i] + " to " + fo + " !");
                }

                Book2.PageInfo pi = panelController.getBook().new PageInfo(pages[i]);
                BufferedImage img = ImageIO.read(fo);
                if (img != null) {
                    pi.imageSizeX = img.getWidth();
                    pi.imageSizeY = img.getHeight();
                } else {
                    Image o = Image.getInstance(fo.toURL());
                    pi.imageSizeX = Math.round(o.getWidth());
                    pi.imageSizeY = Math.round(o.getHeight());
                }
                panelController.getBook().addPage(pi);
                ProcessDaemon.createPreviewIfNeed(panelController.getBook(), pages[i]);
                DataStorage.mainFrame.progress.setValue(i + 1);
            }

            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                JOptionPane.showMessageDialog(DataStorage.mainFrame, "Файлы імпартаваныя", "Дадаць файлы",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(DataStorage.mainFrame, "Interrupted: " + ex.getMessage(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
            } catch (ExecutionException e) {
                Throwable ex = e.getCause();
                ex.printStackTrace();
                JOptionPane.showMessageDialog(DataStorage.mainFrame, "Error: " + ex.getMessage(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
            }
            hideProgress();
            panelController.show();
        }
    }
}
