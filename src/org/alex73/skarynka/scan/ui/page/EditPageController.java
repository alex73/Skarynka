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
package org.alex73.skarynka.scan.ui.page;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.ui.book.PanelEditController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for edit page dialog.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class EditPageController {
    private static Logger LOG = LoggerFactory.getLogger(EditPageController.class);

    enum RECT_MODE {
        SIZE, POS
    };

    RECT_MODE currentRectMode;

    private EditPageDialog dialog;
    private final PanelEditController bookController;
    private final Book2 book;
    private final List<String> pages;
    private String page;
    private Dimension fullImageSize;

    public EditPageController(PanelEditController bookController, List<String> pages, String currentPageNumber) {
        this.bookController=bookController;
        this.book=bookController.getBook();
        this.pages = pages;
        page = Book2.formatPageNumber(currentPageNumber);
        Book2.PageInfo pi = book.getPageInfo(page);
        dialog = new EditPageDialog(DataStorage.mainFrame, true);
        fullImageSize = new Dimension(pi.imageSizeX, pi.imageSizeY);

        dialog.btnNext.addActionListener(actionNext);
        dialog.btnPrev.addActionListener(actionPrev);
        dialog.btnSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int c = 0;
                for (String page : pages) {
                    Book2.PageInfo pi = book.getPageInfo(page);
                    if (pi.cropPosX > 0 && pi.cropPosY > 0) {
                        c++;
                    }
                }
                if (c > 0 && JOptionPane.showConfirmDialog(DataStorage.mainFrame,
                        Messages.getString("PAGE_NEW_CROP", c), Messages.getString("ERROR_TITLE"),
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                    return;
                }
                for (String page : pages) {
                    Book2.PageInfo pi = book.getPageInfo(page);
                    pi.cropPosX = -1;
                    pi.cropPosY = -1;
                }

                dialog.btnPos.setSelected(false);
                currentRectMode = RECT_MODE.SIZE;
            }
        });
        dialog.btnPos.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentRectMode = dialog.btnPos.isSelected() ? RECT_MODE.POS : null;
            }
        });
        dialog.btnPosAll.addActionListener((e) -> {
            Book2.PageInfo pic = book.getPageInfo(page);
            for(String p:pages) {
                Book2.PageInfo pis = book.getPageInfo(p);
                pis.cropPosX=pic.cropPosX;
                pis.cropPosY=pic.cropPosY;
            }
        });
        // dialog.cbColor.addActionListener(actionColor);
        // dialog.cbEdit.addActionListener(actionEdit);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                bookSave();
                bookController.show();
            }
        });

        dialog.tags.setup(Context.getPageTags());
        dialog.tags.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Book2.PageInfo pi = book.getPageInfo(page);
                pi.tags.clear();
                pi.tags.addAll(dialog.tags.getValues());
            }
        });

        addAction(KeyEvent.VK_F1, actionPrev);
        addAction(KeyEvent.VK_F2, actionNext);
        addAction(KeyEvent.VK_UP, actionUp);
        addAction(KeyEvent.VK_DOWN, actionDown);
        addAction(KeyEvent.VK_LEFT, actionLeft);
        addAction(KeyEvent.VK_RIGHT, actionRight);
        addAction(KeyEvent.VK_ESCAPE, actionEsc);
        // addAction(KeyEvent.VK_C, actionColor);
        // addAction(KeyEvent.VK_E, actionEdit);
        
        

        dialog.getContentPane().addMouseListener(mouseListener);
        dialog.getContentPane().addMouseMotionListener(mouseMotionListener);

        showPage();
        if (fullImageSize.width == 0 && fullImageSize.height == 0) {
            fullImageSize.width = dialog.preview.getImage().getWidth();
            fullImageSize.height = dialog.preview.getImage().getHeight();
        }

        dialog.setSize(1000, 800);
        dialog.setLocationRelativeTo(DataStorage.mainFrame);
    }

    @SuppressWarnings("serial")
    Action actionEsc = new AbstractAction("esc") {
        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.dispose();
            bookSave();
            bookController.show();
        }
    };
    
    @SuppressWarnings("serial")
    Action actionPrev = new AbstractAction("prev") {
        @Override
        public void actionPerformed(ActionEvent e) {
            bookSave();
            int pos = pages.indexOf(page);
            if (pos >= 0 && pos > 0) {
                page = Book2.formatPageNumber(pages.get(pos - 1));
                showPage();
            }
        }
    };
    @SuppressWarnings("serial")
    Action actionNext = new AbstractAction("next") {
        @Override
        public void actionPerformed(ActionEvent e) {
            bookSave();
            int pos = pages.indexOf(page);
            if (pos >= 0 && pos < pages.size() - 1) {
                page = Book2.formatPageNumber(pages.get(pos + 1));
                showPage();
            }
        }
    };
    @SuppressWarnings("serial")
    Action actionUp = new AbstractAction("up") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentRectMode == RECT_MODE.POS && pressedPoint == null) {
                moveCrop(0, -16);
            }
        }
    };
    @SuppressWarnings("serial")
    Action actionDown = new AbstractAction("down") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentRectMode == RECT_MODE.POS && pressedPoint == null) {
                moveCrop(0, 16);
            }
        }
    };
    @SuppressWarnings("serial")
    Action actionLeft = new AbstractAction("left") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentRectMode == RECT_MODE.POS && pressedPoint == null) {
                moveCrop(-16, 0);
            }
        }
    };
    @SuppressWarnings("serial")
    Action actionRight = new AbstractAction("right") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentRectMode == RECT_MODE.POS && pressedPoint == null) {
                moveCrop(16, 0);
            }
        }
    };
    @SuppressWarnings("serial")
    Action actionTag = new AbstractAction("tag") {
        @Override
        public void actionPerformed(ActionEvent e) {
            // if (e.getSource() != dialog.cbColor) {
            // dialog.cbColor.setSelected(!dialog.cbColor.isSelected());
            // }
            Book2.PageInfo pi = book.getPageInfo(page);
            JCheckBox cb = (JCheckBox) e.getSource();
            if (cb.isSelected()) {
                pi.tags.add(cb.getName());
            } else {
                pi.tags.remove(cb.getName());
            }
        }
    };
    // @SuppressWarnings("serial")
    // Action actionEdit = new AbstractAction("edit") {
    // @Override
    // public void actionPerformed(ActionEvent e) {
    // if (e.getSource() != dialog.cbEdit) {
    // dialog.cbEdit.setSelected(!dialog.cbEdit.isSelected());
    // }
    // Book2.PageInfo pi = book.getPageInfo(page);
    // pi.needEdit = dialog.cbEdit.isSelected();
    // }
    // };

    void moveCrop(int dx, int dy) {
        int ddx = 0, ddy = 0;
        switch (dialog.preview.getRotation()) {
        case 0:
            ddx = dx;
            ddy = dy;
            break;
        case 1:
            ddx = dy;
            ddy = -dx;
            break;
        case 2:
            ddx = -dx;
            ddy = -dy;
            break;
        case 3:
            ddx = -dy;
            ddy = dx;
            break;
        }
        Book2.PageInfo pi = book.getPageInfo(page);
        pi.cropPosX += ddx;
        pi.cropPosY += ddy;
        dialog.preview.setCropRectangle(new Rectangle(pi.cropPosX, pi.cropPosY, book.cropSizeX,
                book.cropSizeY), fullImageSize);
        dialog.preview.repaint();
    }

    public static void show(PanelEditController bookController, List<String> pages, String page) {
        new EditPageController(bookController, pages, page).dialog.setVisible(true);
    }

    void showPage() {
        try {
            dialog.pageLabel.setText(Messages.getString("PAGE_NUMBER", Book2.simplifyPageNumber(page)));

            dialog.btnNext.setEnabled(pages.size() > 0 && !pages.get(pages.size() - 1).equals(this.page));
            dialog.btnPrev.setEnabled(pages.size() > 0 && !pages.get(0).equals(this.page));

            Book2.PageInfo pi = book.getPageInfo(page);
            fullImageSize = new Dimension(pi.imageSizeX, pi.imageSizeY);
            if (pi.cropPosX < 0 && pi.cropPosY < 0) {
                // not defined yet
                String prevPage = Book2.incPage(page, -book.pageStep);
                if (!prevPage.isEmpty()) {
                    Book2.PageInfo piPrev = book.getPageInfo(prevPage);
                    if (piPrev != null && piPrev.cropPosX >= 0 && piPrev.cropPosY >= 0) {
                        pi.cropPosX = piPrev.cropPosX;
                        pi.cropPosY = piPrev.cropPosY;
                    }
                }
            }
            dialog.preview.setRotation(pi.rotate);
            dialog.preview.setCropRectangle(new Rectangle(pi.cropPosX, pi.cropPosY,
                    book.cropSizeX, book.cropSizeY), fullImageSize);
            dialog.preview.displayImage(book.getImage(page), 1, 1);
            dialog.tags.setValues(pi.tags);
            dialog.errLabel.setText(" ");
        } catch (Exception ex) {
            LOG.warn("Error open image", ex);
            dialog.errLabel
                    .setText(Messages.getString("ERROR_READ_JPEG", ex.getClass().getName(), ex.getMessage()));
        }
    }

    void bookSave() {
        try {
            book.save();
        } catch (Exception ex) {
            LOG.warn("Error book save", ex);
            JOptionPane.showMessageDialog(dialog,
                    Messages.getString("ERROR_BOOK_SAVE", book.getName(), ex.getMessage()),
                    Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
        }
    }

    Point currentPoint, pressedPoint;

    MouseListener mouseListener = new MouseAdapter() {
        public void mouseReleased(MouseEvent e) {
            if (currentRectMode == null) {
                return;
            }

            currentPoint = dialog.preview.mouseToImage(e.getPoint(), dialog.preview.getX(), dialog.preview.getY(),
                    fullImageSize);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Mouse release " + e.getPoint() + " / " + currentPoint);
            }

            Rectangle cropRect = updateRect();

            if (currentRectMode == RECT_MODE.SIZE) {
                book.cropSizeX = cropRect.width;
                book.cropSizeY = cropRect.height;
                Book2.PageInfo pi = book.getPageInfo(page);
                pi.cropPosX = cropRect.x;
                pi.cropPosY = cropRect.y;
                currentRectMode = null;
                dialog.btnSize.getModel().setSelected(false);
            } else if (currentRectMode == RECT_MODE.POS) {
                Book2.PageInfo pi = book.getPageInfo(page);
                pi.cropPosX = cropRect.x;
                pi.cropPosY = cropRect.y;
            }
            bookSave();
            pressedPoint = null;
            currentPoint = null;
        }

        public void mousePressed(MouseEvent e) {
            if (currentRectMode == null) {
                return;
            }
            currentPoint = dialog.preview.mouseToImage(e.getPoint(), dialog.preview.getX(), dialog.preview.getY(),
                    fullImageSize);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Mouse press " + e.getPoint() + " / " + currentPoint);
            }
            if (currentRectMode == RECT_MODE.SIZE) {
                pressedPoint = currentPoint;
            } else if (currentRectMode == RECT_MODE.POS) {
                Book2.PageInfo pi = book.getPageInfo(page);
                pressedPoint = new Point(currentPoint.x - pi.cropPosX, currentPoint.y - pi.cropPosY);
            }
            updateRect();
        }
    };

    MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent e) {
            if (pressedPoint != null) {
                currentPoint = dialog.preview.mouseToImage(e.getPoint(), dialog.preview.getX(), dialog.preview.getY(),
                        fullImageSize);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Mouse drag " + e.getPoint() + " / " + currentPoint);
                }
                updateRect();
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (pressedPoint != null) {
                currentPoint = dialog.preview.mouseToImage(e.getPoint(), dialog.preview.getX(), dialog.preview.getY(),
                        fullImageSize);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Mouse move " + e.getPoint() + " / " + currentPoint);
                }
                updateRect();
            }
        }
    };

    Rectangle updateRect() {
        Rectangle rc = new Rectangle();

        if (currentRectMode == RECT_MODE.SIZE) {
            rc.x = pressedPoint.x;
            rc.y = pressedPoint.y;

            rc.width = currentPoint.x - pressedPoint.x;
            rc.height = currentPoint.y - pressedPoint.y;

            if (rc.width < 0) {
                rc.width = -rc.width;
                rc.x -= rc.width;
            }
            if (rc.height < 0) {
                rc.height = -rc.height;
                rc.y -= rc.height;
            }
        } else {
            rc.x = currentPoint.x - pressedPoint.x;
            rc.y = currentPoint.y - pressedPoint.y;
            rc.width = book.cropSizeX;
            rc.height = book.cropSizeY;
        }

        dialog.preview.setCropRectangle(rc, fullImageSize);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Draw crop " + rc);
        }
        dialog.preview.repaint();

        return rc;
    }

    void addAction(int keyCode, Action action) {
        InputMap im = dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStroke.getKeyStroke(keyCode, 0), action.getValue(Action.NAME));
        dialog.getRootPane().getActionMap().put(action.getValue(Action.NAME), action);
    }
}
