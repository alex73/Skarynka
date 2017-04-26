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
package org.alex73.skarynka.scan.ui.book;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.IPagePreviewChanged;
import org.alex73.skarynka.scan.ITabController;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.PagePreviewer;
import org.alex73.skarynka.scan.process.ProcessDaemon;
import org.alex73.skarynka.scan.ui.page.EditPageController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for edit book panel.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class PanelEditController implements ITabController {
    private static Logger LOG = LoggerFactory.getLogger(PanelEditController.class);

    private final Book2 book;
    private final PanelEdit panel;

    private boolean selectionEnabled;
    private String currentSelection, startSelection;
    private Border borderNone, borderFocused, borderSelected;
    private PagePreviewer previewer;
    private List<JMenuItem> menuItems = new ArrayList<>();

    @Override
    public String getTabName() {
        return book.getName();
    }

    @Override
    public Component getTabComponent() {
        return panel;
    }

    public PanelEditController(Book2 book) throws Exception {
        this.book = book;
        panel = new PanelEdit();
        panel.setName(book.getName());

        panel.pagesScrollBar.getVerticalScrollBar().setUnitIncrement(20);

        panel.pagesScrollBar.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resize();
            }
        });

        previewer = new PagePreviewer(book);

        List<String> pages = book.listPages();
        for (String p : pages) {
            ProcessDaemon.createPreviewIfNeed(book, p);
        }

        show();
    }

    public Book2 getBook() {
        return book;
    }

    public PanelEdit getPanel() {
        return panel;
    }

    @Override
    public void close() {
        previewer.finish();
        try {
            book.save();
        } catch (Exception ex) {
            LOG.error("Error save book '" + book.getName() + "'", ex);
            JOptionPane.showMessageDialog(DataStorage.mainFrame,
                    Messages.getString("ERROR_BOOK_SAVE", book.getName(), ex.getMessage()),
                    Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    @Override
    public void deactivate() {
        for (JMenuItem item : menuItems) {
            DataStorage.mainFrame.workMenu.remove(item);
        }
        menuItems.clear();
        DataStorage.mainFrame.workMenu.setEnabled(false);
        DataStorage.mainFrame.viewMenu.setEnabled(false);
    }

    @Override
    public void activate() {
        DataStorage.mainFrame.workMenu.setEnabled(true);
        DataStorage.mainFrame.viewMenu.setEnabled(true);

        Set<String> cameras = new TreeSet<>();
        for (String page : book.listPages()) {
            Book2.PageInfo pi = book.getPageInfo(page);
            if (pi.camera != null) {
                cameras.add(pi.camera);
            }
        }

        for (String camera : cameras) {
            JMenuItem item = new JMenuItem(Messages.getString("MENU_BOOK_ROTATE", camera));
            item.setName("MENU_BOOK_ROTATE");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (String page : book.listPages()) {
                        Book2.PageInfo pi = book.getPageInfo(page);
                        if (camera.equals(pi.camera)) {
                            pi.rotate = (pi.rotate + 1) % 4;
                        }
                    }
                    try {
                        book.save();
                    } catch (Exception ex) {
                        LOG.error("Error save book '" + book.getName() + "'", ex);
                        JOptionPane.showMessageDialog(DataStorage.mainFrame,
                                Messages.getString("ERROR_BOOK_SAVE", book.getName(), ex.getMessage()),
                                Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
                    }
                    resetAllPreviews();
                    show();
                }
            });
            menuItems.add(item);
            DataStorage.mainFrame.workMenu.add(item);
        }
    }

    public void resetAllPreviews() {
        previewer.reset();
    }

    public void updatePreview(String page) {
        previewer.updatePreview(page);
    }

    public synchronized PanelEdit show() {
        borderNone = BorderFactory.createLineBorder(panel.getBackground());
        borderFocused = BorderFactory.createLineBorder(Color.RED);
        borderSelected = BorderFactory.createLineBorder(Color.BLUE);

        panel.pagesPanel.removeAll();
        selectionEnabled = false;
        startSelection = null;
        currentSelection = null;

        List<String> pages = book.listPages();
        switch (DataStorage.view) {
        case SEQ:
            // show only sequences
            String prev = "", prevLabelStart = null;
            JLabel prevLabel = null;
            for (String page : pages) {
                String prevInc = Book2.incPage(prev, 1);
                if (page.equals(prevInc)) {
                    // sequence
                    prev = prevInc;
                    continue;
                }
                if (prevLabel != null && !prev.equals(prevLabelStart)) {
                    prevLabel.setText(prevLabel.getText() + ".." + Book2.simplifyPageNumber(prev));
                }
                panel.pagesPanel.add(prevLabel = createLabel(page));
                prev = prevLabelStart = page;
            }
            if (prevLabel != null && !prev.equals(prevLabelStart)) {
                prevLabel.setText(prevLabel.getText() + ".." + Book2.simplifyPageNumber(prev));
            }
            break;
        case ALL:
            selectionEnabled = true;
            // show all pages
            for (String page : pages) {
                panel.pagesPanel.add(createLabel(page));
            }
            break;
        case CROP_ERRORS:
            for (String page : pages) {
                Book2.PageInfo pi = book.getPageInfo(page);
                if (pi.cropPosX < 0 || pi.cropPosY < 0) {
                    panel.pagesPanel.add(createLabel(page));
                }
            }
            break;
        case TAG:
            for (String page : pages) {
                Book2.PageInfo pi = book.getPageInfo(page);
                if (pi.tags.contains(DataStorage.viewTag)) {
                    panel.pagesPanel.add(createLabel(page));
                }
            }
            break;
        }

        resize();
        panel.pagesPanel.revalidate();
        panel.pagesPanel.repaint();

        return panel;
    }

    JLabel createLabel(String page) {
        JLabel pageLabel = new JLabel(Book2.simplifyPageNumber(page));
        pageLabel.setName(page);
        previewer.setPreview(page, new IPagePreviewChanged() {
            @Override
            public void show(Image image) {
                pageLabel.setIcon(new ImageIcon(image));
            }
        });
        pageLabel.setBorder(borderNone);
        pageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        pageLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        pageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    switch (e.getClickCount()) {
                    case 1:
                        if (selectionEnabled) {
                            pageLabel.requestFocusInWindow();
                            if (e.isShiftDown()) {
                                startSelection = currentSelection;
                            } else {
                                startSelection = null;
                            }
                        }
                        break;
                    case 2:
                        EditPageController.show(PanelEditController.this, pageLabel.getName());
                        break;
                    }
                }
                if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1) {
                    if (selectionEnabled && currentSelection != null) {
                        new PagePopupMenu(PanelEditController.this, startSelection, currentSelection)
                                .show(pageLabel, e.getX(), e.getY());
                    }
                }
            }
        });

        if (selectionEnabled) {
            pageLabel.setFocusable(true);
            pageLabel.addFocusListener(new FocusListener() {

                @Override
                public void focusLost(FocusEvent e) {
                }

                @Override
                public void focusGained(FocusEvent e) {
                    currentSelection = pageLabel.getName();
                    markSelected(startSelection, currentSelection);
                    pageLabel.setBorder(borderFocused);
                }
            });
        }
        return pageLabel;
    }

    void markSelected(String fromPage, String toPage) {
        for (int i = 0; i < panel.pagesPanel.getComponentCount(); i++) {
            JLabel c = (JLabel) panel.pagesPanel.getComponent(i);
            c.setBorder(borderNone);
        }
        if (fromPage == null || toPage == null) {
            return;
        }
        if (fromPage.compareTo(toPage) > 0) {
            String s = fromPage;
            fromPage = toPage;
            toPage = s;
        }
        for (int i = 0; i < panel.pagesPanel.getComponentCount(); i++) {
            JLabel c = (JLabel) panel.pagesPanel.getComponent(i);
            if (fromPage.compareTo(c.getName()) <= 0) {
                c.setBorder(borderSelected);
            }
            if (c.getName().equals(toPage)) {
                break;
            }
        }
    }

    void resize() {
        int width = panel.pagesScrollBar.getViewport().getWidth();
        GridLayout layout = (GridLayout) panel.pagesPanel.getLayout();
        int perRow = width / (DataStorage.previewMaxWidth + layout.getHgap());
        if (perRow == 0) {
            perRow = 1;
        }
        layout.setColumns(perRow);
        layout.setRows((panel.pagesPanel.getComponentCount() + perRow - 1) / perRow);
        panel.pagesPanel.revalidate();
    }
}
