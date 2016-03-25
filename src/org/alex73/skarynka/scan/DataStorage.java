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
package org.alex73.skarynka.scan;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.alex73.skarynka.scan.devices.ISourceDevice;
import org.alex73.skarynka.scan.hid.HIDScanController;
import org.alex73.skarynka.scan.process.ProcessDaemon;
import org.alex73.skarynka.scan.ui.MainFrame;
import org.alex73.skarynka.scan.ui.book.PanelEditController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage for devices and some methods for UI control.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class DataStorage {
    private static Logger LOG = LoggerFactory.getLogger(DataStorage.class);

    public static final int INITIAL_PREVIEW_MAX_WIDTH = 70;
    public static final int INITIAL_PREVIEW_MAX_HEIGHT = 100;

    public enum SHOW_TYPE {
        ALL, SEQ, CROP_ERRORS, TAG
    };

    public static MainFrame mainFrame;
    public static ISourceDevice device;
    public static boolean focused;
    public static int previewMaxWidth = INITIAL_PREVIEW_MAX_WIDTH;
    public static int previewMaxHeight = INITIAL_PREVIEW_MAX_HEIGHT;

    // public static Book2 book;
    // public static PagePreviewer pagePreviewer;

    private static int tabSelectedIndex = -1;
    private static final List<ITabController> tabControllers = new ArrayList<>();

    // public static PanelEditController panelEdit;
    public static SHOW_TYPE view = SHOW_TYPE.ALL;
    public static String viewTag;
    public static ProcessDaemon daemon;
    public static HIDScanController hidScanDevice;

    public static synchronized PanelEditController getOpenBookController(String bookName) {
        for (ITabController c : tabControllers) {
            if (c instanceof PanelEditController) {
                PanelEditController cc = (PanelEditController) c;
                if (bookName.equals(cc.getBook().getName())) {
                    // already open
                    return cc;
                }
            }
        }
        return null;
    }

    public static synchronized void closeTab(ITabController tab) {
        tab.deactivate();
        tabControllers.remove(tab);
        mainFrame.tabs.remove(tab.getTabComponent());
        tab.close();
    }

    public static void addTab(ITabController tab) {
        tabControllers.add(tab);
        mainFrame.tabs.add(tab.getTabName(), tab.getTabComponent());
        mainFrame.tabs.setSelectedIndex(tabControllers.size() - 1);
    }

    public synchronized static void activateTab(int tabIndex) {
        if (tabSelectedIndex >= 0 && tabSelectedIndex < tabControllers.size()) {
            tabControllers.get(tabSelectedIndex).deactivate();
        }
        tabSelectedIndex = tabIndex;
        tabControllers.get(tabSelectedIndex).activate();
    }

    /**
     * If book already open - just activate it.
     */
    public static synchronized Book2 openBook(String bookName, boolean showUI) {
        PanelEditController ce = getOpenBookController(bookName);
        if (ce != null) {
            if (showUI) {
                ce.activate();
            }
            return ce.getBook();
        }

        try {
            File bookDir = new File(Context.getBookDir(), bookName);
            Book2 book = new Book2(bookDir);
            if (!book.getErrors().isEmpty()) {
                throw new Exception(book.getErrors().toString());
            }

            if (!showUI) {
                return book;
            }

            LOG.info("Open book " + bookName);

            if (device != null) {
                device.setPreviewPanels();
            }

            PanelEditController c = new PanelEditController(book);
            addTab(c);

            return c.getBook();
        } catch (Exception ex) {
            LOG.error("Error open book '" + bookName + "'", ex);
            JOptionPane.showMessageDialog(mainFrame,
                    Messages.getString("ERROR_BOOK_OPEN", bookName, ex.getMessage()),
                    Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public static synchronized void refreshBookPanels(boolean resetAllPreviews) {
        for (ITabController c : tabControllers) {
            if (c instanceof PanelEditController) {
                PanelEditController cc = (PanelEditController) c;
                if (resetAllPreviews) {
                    cc.resetAllPreviews();
                }
                cc.show();
            }
        }
    }

    public synchronized static ITabController getActiveTab() {
        int sel = mainFrame.tabs.getSelectedIndex();
        return sel < 0 ? null : tabControllers.get(sel);
    }

    public static synchronized void iterateByBooks(Set<String> bookNames, IBookIterator iterator) {
        File[] ls = new File(Context.getBookDir()).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory() && new File(pathname, "book.properties").isFile();
            }
        });
        if (ls == null) {
            ls = new File[0];
        }
        Arrays.sort(ls);
        try {
            for (File f : ls) {
                if (bookNames != null && !bookNames.contains(f.getName().toLowerCase())) {
                    continue;
                }
                PanelEditController c = getOpenBookController(f.getName().toLowerCase());
                if (c != null) {
                    iterator.onBook(c.getBook());
                } else {
                    Book2 b = new Book2(f);
                    iterator.onBook(b);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static void finish() {
        for (ITabController c : tabControllers) {
            c.close();
        }

        LOG.info("Skarynka shutdown");
        if (hidScanDevice != null) {
            hidScanDevice.finish();
        }

        if (daemon != null) {
            daemon.finish();
            try {
                daemon.join();
            } catch (Exception ex) {
            }
        }
        mainFrame.dispose();
    }
}
