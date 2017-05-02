package org.alex73.skarynka.scan.ui.book;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Book2.PageInfo;
import org.alex73.skarynka.scan.IPagePreviewChanged;
import org.alex73.skarynka.scan.ui.page.EditPageController;

public class PageComponent extends JLabel {
    private static final Border borderNone = BorderFactory.createLineBorder(new Color(0, 0, 0, 0));
    private static final Border borderFocused = BorderFactory.createLineBorder(Color.RED);
    private static final Border borderSelected = BorderFactory.createLineBorder(Color.BLUE);

    private final PanelEditController controller;

    public PageComponent(String page, PanelEditController controller, boolean selectionEnabled) {
        super(Book2.simplifyPageNumber(page));
        this.controller = controller;
        setName(page);
        setBorder(borderNone);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);

        updateImage();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    switch (e.getClickCount()) {
                    case 1:
                        int index=getIndex();
                        if (e.isControlDown()) {
                            controller.selection.change(index);
                        } else if (e.isShiftDown()) {
                            controller.selection.setEnd(index);
                            System.out.println("shift " + controller.selection);
                        } else {
                            controller.selection.clear();
                            controller.selection.setStart(index);
                            controller.selection.setEnd(index);
                        }
                        requestFocus();
                        break;
                    case 2:
                        EditPageController.show(controller, controller.getBook().listPages(), getName());
                        break;
                    }
                }
                if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1) {
                    if (selectionEnabled && !controller.selection.getSelected().isEmpty()) {
                        new PagePopupMenu(controller).show(PageComponent.this, e.getX(), e.getY());
                    }
                }
                e.consume();
            }
        });
        if (selectionEnabled) {
            setFocusable(true);
            addFocusListener(new FocusListener() {
                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusGained(FocusEvent e) {
                    PageInfo pi = controller.getBook().getPageInfo(getName());
                    Dimension fullImageSize = new Dimension(pi.imageSizeX, pi.imageSizeY);
                    controller.previewPage.setRotation(pi.rotate);
                    controller.previewPage.setCropRectangle(new Rectangle(pi.cropPosX, pi.cropPosY,
                            controller.getBook().cropSizeX, controller.getBook().cropSizeY), fullImageSize);
                    try {
                        controller.previewPage.displayImage(controller.getBook().getImage(getName()), 1, 1);
                    } catch (Exception ex) {
                    }
                }
            });
        }
    }

    protected void updateImage() {
        controller.previewer.setPreview(getName(), new IPagePreviewChanged() {
            @Override
            public void show(Image image) {
                setIcon(new ImageIcon(image));
            }
        });
    }

    private int getIndex() {
        for (int i = 0; i < getParent().getComponentCount(); i++) {
            if (getParent().getComponent(i) == this) {
                return  i;
            }
        }
        return -1;
    }
    @Override
    protected void paintBorder(Graphics g) {
        int index=getIndex();
        if (isFocusOwner()) {
            setBorder(borderFocused);
        } else if (controller.selection.isSelected(index)) {
            setBorder(borderSelected);
        } else {
            setBorder(borderNone);
        }
        super.paintBorder(g);
    }
}
