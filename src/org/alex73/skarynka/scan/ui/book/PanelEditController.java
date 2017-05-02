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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.ITabController;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.PagePreviewer;
import org.alex73.skarynka.scan.common.ImageViewPane;
import org.alex73.skarynka.scan.process.ProcessDaemon;
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
    protected final ImageViewPane previewPage;

    protected SelectionController selection;
    protected PagePreviewer previewer;
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
        selection = new SelectionController(panel.pagesPanel);

        panel.pagesScrollBar.getVerticalScrollBar().setUnitIncrement(20);

        panel.pagesScrollBar.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resize();
            }
        });

        previewPage = new ImageViewPane();
        panel.scrollPreview.setViewportView(previewPage);
        panel.previewOrig.addActionListener((e) -> {
            BufferedImage image = previewPage.getImage();
            previewPage.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            panel.scrollPreview.revalidate();
        });
        panel.previewFull.addActionListener((e) -> {
            previewPage.setPreferredSize(new Dimension(50, 50));
            panel.scrollPreview.revalidate();
        });

        previewer = new PagePreviewer(book);

        List<String> pages = book.listPages();
        for (String p : pages) {
            ProcessDaemon.createPreviewIfNeed(book, p);
        }

        show();
    }

    KeyListener KEY_LISTENER = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            Point p = getFocusPosition();
            if (p == null) {
                return;
            }
            boolean moved = false;
            switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                p.y--;
                moved = true;
                break;
            case KeyEvent.VK_DOWN:
                p.y++;
                moved = true;
                break;
            case KeyEvent.VK_LEFT:
                p.x--;
                moved = true;
                break;
            case KeyEvent.VK_RIGHT:
                p.x++;
                moved = true;
                break;
            case KeyEvent.VK_A:
                if (e.isControlDown()) {
                    selection.addSelectionInterval(0, panel.pagesPanel.getComponentCount());
                    e.consume();
                }
                break;
            }
            if (moved) {
                PageComponent c = setFocusPosition(p);
                if (c != null) {
                    if (!e.isShiftDown()) {
                        selection.clear();
                        selection.setStart(getPointIndex(p));
                    }
                    selection.setEnd(getPointIndex(p));
                    if (c != null) {
                        c.scrollRectToVisible(c.getBounds());
                    }
                }
                e.consume();
            }
        }
    };

    Point getFocusPosition() {
        int idx = -1;
        for (int i = 0; i < panel.pagesPanel.getComponentCount(); i++) {
            if (panel.pagesPanel.getComponent(i).isFocusOwner()) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return null;
        }
        GridLayout grid = (GridLayout) panel.pagesPanel.getLayout();
        Point r = new Point();
        r.x = idx % grid.getColumns();
        r.y = idx / grid.getColumns();
        return r;
    }

    int getPointIndex(Point p) {
        GridLayout grid = (GridLayout) panel.pagesPanel.getLayout();
        return p.x + grid.getColumns() * p.y;
    }

    PageComponent setFocusPosition(Point p) {
        int idx = getPointIndex(p);
        if (idx >= 0 && idx < panel.pagesPanel.getComponentCount()) {
            PageComponent c = (PageComponent) panel.pagesPanel.getComponent(idx);
            c.requestFocus();
            return c;
        } else {
            return null;
        }
    }

    @SuppressWarnings("serial")
    void bind(int vk, ActionListener listener) {
        panel.pagesPanel.getInputMap().put(KeyStroke.getKeyStroke(vk, 0), "ACTION_KEY_" + vk);
        panel.pagesPanel.getActionMap().put("ACTION_KEY_" + vk, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.actionPerformed(e);
            }
        });
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
        panel.pagesPanel.removeAll();
        selection.reset();

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
                prevLabel = createPage(page, false);
                prev = prevLabelStart = page;
            }
            if (prevLabel != null && !prev.equals(prevLabelStart)) {
                prevLabel.setText(prevLabel.getText() + ".." + Book2.simplifyPageNumber(prev));
            }
            break;
        case ALL:
            // show all pages
            for (String page : pages) {
                createPage(page, true);
            }
            break;
        case CROP_ERRORS:
            for (String page : pages) {
                Book2.PageInfo pi = book.getPageInfo(page);
                if (pi.cropPosX < 0 || pi.cropPosY < 0) {
                    createPage(page, false);
                }
            }
            break;
        case ODD:
            for (String page : pages) {
                char e = page.charAt(page.length() - 1);
                if (e % 2 == 1) {
                    createPage(page, true);
                }
            }
            break;
        case EVEN:
            for (String page : pages) {
                char e = page.charAt(page.length() - 1);
                if (e % 2 == 0) {
                    createPage(page, true);
                }
            }
            break;
        case TAG:
            for (String page : pages) {
                Book2.PageInfo pi = book.getPageInfo(page);
                if (pi.tags.contains(DataStorage.viewTag)) {
                    createPage(page, false);
                }
            }
            break;
        }
        for (int i = 0; i < panel.pagesPanel.getComponentCount(); i++) {
            panel.pagesPanel.getComponent(i).addKeyListener(KEY_LISTENER);
        }

        resize();
        panel.pagesPanel.revalidate();
        panel.pagesPanel.repaint();

        return panel;
    }

    PageComponent createPage(String page, boolean selectionEnabled) {
        PageComponent p = new PageComponent(page, this, selectionEnabled);
        panel.pagesPanel.add(p);
        return p;
    }

    void resize() {
        int width = panel.pagesScrollBar.getViewport().getWidth();
        GridLayout layout = (GridLayout) panel.pagesPanel.getLayout();
        int perRow = width / (DataStorage.previewMaxWidth + layout.getHgap());
        if (perRow == 0) {
            perRow = 1;
        }
        layout.setColumns(perRow);
        panel.pagesPanel.revalidate();
    }

    String getPageByIndex(int index) {
        return ((PageComponent) panel.pagesPanel.getComponent(index)).getName();
    }
}
