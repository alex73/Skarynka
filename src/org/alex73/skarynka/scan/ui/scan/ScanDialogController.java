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
package org.alex73.skarynka.scan.ui.scan;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.common.ImageViewPane;
import org.alex73.skarynka.scan.hid.HIDScanController;
import org.alex73.skarynka.scan.ui.book.PanelEditController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for scan dialog.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class ScanDialogController {
    private static Logger LOG = LoggerFactory.getLogger(ScanDialogController.class);

    private final PanelEditController panelController;
    private final Book2 book;
    private ScanDialog dialog;
    private String prev1, prev2;
    private Dimension imageSize;

    public static void show(PanelEditController panelController) {
        new ScanDialogController(panelController);
    }

    private ScanDialogController(PanelEditController panelController) {
        this.panelController = panelController;
        this.book = panelController.getBook();
        int currentZoom = DataStorage.device.getZoom();

        Dimension[] deviceImageSizes = DataStorage.device.getImageSize();
        imageSize = deviceImageSizes[0];
        for (int i = 1; i < deviceImageSizes.length; i++) {
            if (!imageSize.equals(deviceImageSizes[i])) {
                JOptionPane.showMessageDialog(DataStorage.mainFrame,
                        Messages.getString("ERROR_WRONG_NOTEQUALSSIZE"), Messages.getString("ERROR_TITLE"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        int pagesCount = book.getPagesCount();
        if (pagesCount > 0) {
            int bookZoom = book.zoom;

            if (bookZoom != currentZoom) {
                if (JOptionPane.showConfirmDialog(DataStorage.mainFrame,
                        Messages.getString("ERROR_WRONG_ZOOM", pagesCount, bookZoom, currentZoom),
                        Messages.getString("ERROR_TITLE"), JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                    return;
                }
            }
        }

        book.zoom = currentZoom;
        String dpi = Context.getSettings().get("dpi." + book.zoom);
        if (dpi != null) {
            book.dpi = Integer.parseInt(dpi);
        } else {
            book.dpi = 300;
        }

        dialog = new ScanDialog(DataStorage.mainFrame, true);
        dialog.btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                DataStorage.device.setPreviewPanels();
                panelController.show();
            }
        });

        init(dialog.controlLeft, dialog.liveLeft);
        init(dialog.controlRight, dialog.liveRight);

        checkNumbers();
        showStatus();

        boolean[] visible = DataStorage.device.setPreviewPanels(dialog.liveLeft, dialog.liveRight);
        dialog.controlLeft.setVisible(visible[0]);
        dialog.controlRight.setVisible(visible[1]);
        dialog.liveLeft.setVisible(visible[0]);
        dialog.liveRight.setVisible(visible[1]);

        int[] rotations = DataStorage.device.getRotations();
        dialog.liveLeft.setRotation(rotations[0]);
        dialog.liveRight.setRotation(rotations[1]);

        dialog.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        dialog.validate();
        dialog.controlLeft.txtNumber.setVisible(false);
        dialog.controlLeft.txtNumber.setVisible(false);

        int keyCode = HIDScanController.getKeyCode(Context.getSettings().get("hidscan-keys"));
        if (keyCode != 0) {
            addAction(keyCode, actionScan);
        }
        if (keyCode != KeyEvent.VK_F1) {
            addAction(KeyEvent.VK_F1, actionScan);
        }
        dialog.btnScan.addActionListener(actionScan);
        addAction(KeyEvent.VK_F2, actionRescan);
        dialog.btnRescan.addActionListener(actionRescan);

        dialog.setVisible(true);
    }

    @SuppressWarnings("serial")
    Action actionScan = new AbstractAction("scan") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (dialog.btnScan.isEnabled()) {
                scan(dialog.liveLeft.getPageNumber(), dialog.liveRight.getPageNumber());
                showStatus();
                checkNumbers();
            }
        }
    };
    @SuppressWarnings("serial")
    Action actionRescan = new AbstractAction("rescan") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (prev1 == null && prev2 == null) {
                return;
            }
            if (dialog.btnRescan.isEnabled()) {
                scan(prev1, prev2);
                showStatus();
                checkNumbers();
            }
        }
    };

    String s(Dimension s) {
        return s.width + "x" + s.height;
    }

    void scan(String p1, String p2) {
        String bookPath = book.getBookDir().getAbsolutePath() + "/";

        String p1f = Book2.formatPageNumber(p1);
        String p2f = Book2.formatPageNumber(p2);

        prev1 = p1f;
        prev2 = p2f;
        dialog.btnRescan.setEnabled(true);

        String p1p = dialog.liveLeft.getStrikeOut() || !dialog.controlLeft.isVisible() ? null
                : bookPath + p1f;
        String p2p = dialog.liveRight.getStrikeOut() || !dialog.controlRight.isVisible() ? null
                : bookPath + p2f;

        try {
            String[] camerasIds = DataStorage.device.scan(p1p, p2p);
            if (p1p != null) {
                Book2.PageInfo pi = book.new PageInfo(p1f);
                pi.rotate = dialog.liveLeft.getRotation();
                pi.tags.clear();
                pi.tags.addAll(dialog.controlLeft.tags.getValues());
                pi.camera = camerasIds[0];
                pi.imageSizeX = imageSize.width;
                pi.imageSizeY = imageSize.height;
                pi.pageOriginalFileExt = DataStorage.device.getScannedFileExt();
                book.addPage(pi);
                panelController.updatePreview(p1f);
            }
            if (p2p != null) {
                Book2.PageInfo pi = book.new PageInfo(p2f);
                pi.rotate = dialog.liveRight.getRotation();
                pi.tags.clear();
                pi.tags.addAll(dialog.controlRight.tags.getValues());
                pi.camera = camerasIds[1];
                pi.imageSizeX = imageSize.width;
                pi.imageSizeY = imageSize.height;
                pi.pageOriginalFileExt = DataStorage.device.getScannedFileExt();
                book.addPage(pi);
                panelController.updatePreview(p2f);
            }
        } catch (Exception ex) {
            LOG.debug("Error scan", ex);
            JOptionPane.showMessageDialog(DataStorage.mainFrame,
                    Messages.getString("ERROR_SCAN", ex.getClass().getName(), ex.getMessage(),
                            Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE));
        }

        try {
            book.save();
        } catch (Exception ex) {
            LOG.debug("Error book save", ex);
            JOptionPane.showMessageDialog(DataStorage.mainFrame,
                    Messages.getString("ERROR_BOOK_SAVE", book.getName(), ex.getMessage()),
                    Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
        }

        dialog.liveLeft.setPageNumber(Book2.incPage(p1f, "".equals(p2f) ? 1 : 2));
        dialog.liveRight.setPageNumber(Book2.incPage(p2f, 2));
    }

    void showStatus() {
        try {
            String[] status = DataStorage.device.getStatus();
            if (status.length >= 1 && dialog.controlLeft.isVisible()) {
                dialog.controlLeft.labelInfo.setText(status[0]);
            }
            if (status.length >= 2 && dialog.controlRight.isVisible()) {
                dialog.controlRight.labelInfo.setText(status[1]);
            }
        } catch (Exception ex) {
            LOG.debug("Error show status", ex);
            JOptionPane.showMessageDialog(DataStorage.mainFrame,
                    Messages.getString("ERROR_SCAN", ex.getClass().getName(), ex.getMessage(),
                            Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE));
        }
    }

    void init(ScanControlPanel control, ImageViewPane live) {
        control.btnSkip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                live.setStrikeout(control.btnSkip.isSelected());
                checkNumbers();
            }
        });
        control.btnNumber.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                control.txtNumber.setVisible(true);
                control.txtNumber.setText(live.getPageNumber());
                control.txtNumber.requestFocus();
            }
        });
        control.txtNumber.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') { // ENTER
                    live.setPageNumber(control.txtNumber.getText());
                    control.txtNumber.setVisible(false);
                    control.btnNumber.requestFocus();
                    checkNumbers();
                } else if (e.getKeyChar() == KeyEvent.VK_ESCAPE) { // ESC
                    control.txtNumber.setVisible(false);
                    control.btnNumber.requestFocus();
                }
            }
        });
        control.tags.setup(Context.getPageTags());
    }

    void checkNumbers() {
        dialog.btnScan.setEnabled(false);
        dialog.btnRescan.setEnabled(false);

        dialog.lblStatus.setText(" ");

        prev1 = null;
        prev2 = null;
        boolean allowRescan = false;

        String p1 = dialog.liveLeft.getPageNumber();
        String p1f = Book2.formatPageNumber(p1);
        String p2 = dialog.liveRight.getPageNumber();
        String p2f = Book2.formatPageNumber(p2);

        if (dialog.controlLeft.isVisible() && !dialog.liveLeft.getStrikeOut()) {
            if (StringUtils.isEmpty(p1)) {
                dialog.lblStatus.setText(Messages.getString("SCAN_CONTROL_ERROR_NONE_PAGE"));
                return;
            }
            if (StringUtils.isEmpty(p1f)) {
                dialog.lblStatus.setText(Messages.getString("SCAN_CONTROL_ERROR_WRONG_PAGE"));
                return;
            }
            if (book.pageExist(p1)) {
                dialog.lblStatus.setText(Messages.getString("SCAN_CONTROL_ERROR_EXIST_PAGES", p1));
            }
        }
        if (dialog.controlRight.isVisible() && !dialog.liveRight.getStrikeOut()) {
            if (StringUtils.isEmpty(p2)) {
                dialog.lblStatus.setText(Messages.getString("SCAN_CONTROL_ERROR_NONE_PAGE"));
                return;
            }
            if (StringUtils.isEmpty(p2f)) {
                dialog.lblStatus.setText(Messages.getString("SCAN_CONTROL_ERROR_WRONG_PAGE"));
                return;
            }
            if (book.pageExist(p2)) {
                dialog.lblStatus.setText(Messages.getString("SCAN_CONTROL_ERROR_EXIST_PAGES", p2));
            }
        }
        if (!dialog.liveLeft.getStrikeOut() && !dialog.liveRight.getStrikeOut()) {
            if (StringUtils.equals(p1f, p2f)) {
                dialog.lblStatus.setText(Messages.getString("SCAN_CONTROL_ERROR_SAME_PAGES"));
                return;
            }
        }

        if (!dialog.liveLeft.getStrikeOut() || !dialog.liveRight.getStrikeOut()) {
            dialog.btnScan.setEnabled(true);
        }
    }

    void addAction(int keyCode, Action action) {
        InputMap im = dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStroke.getKeyStroke(keyCode, 0), action.getValue(Action.NAME));
        dialog.getRootPane().getActionMap().put(action.getValue(Action.NAME), action);
    }
}
