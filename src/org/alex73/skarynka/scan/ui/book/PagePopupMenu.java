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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.Book2.PageInfo;
import org.alex73.skarynka.scan.ui.page.EditPageController;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.process.PageFileInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Popup menu for books list panel.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
@SuppressWarnings("serial")
public class PagePopupMenu extends JPopupMenu {
    private static Logger LOG = LoggerFactory.getLogger(PagePopupMenu.class);

    private final PanelEditController controller;
    private final Book2 book;
    private List<Integer> selectedIndexes;
    private List<String> selectedPages;
    // private final String startSelection, endSelection;

    public PagePopupMenu(PanelEditController controller) {
        this.controller = controller;
        this.book = controller.getBook();

        selectedIndexes = controller.selection.getSelected();
        selectedPages = new ArrayList<>();
        for (int p : selectedIndexes) {
            selectedPages.add(controller.getPageByIndex(p));
        }

        JMenuItem remove;
        if (selectedIndexes.size() == 1) {
            remove = new JMenuItem(Messages.getString("PAGE_POPUP_REMOVE",
                    Book2.simplifyPageNumber(controller.getPageByIndex(selectedIndexes.get(0)))));
        } else {
            remove = new JMenuItem(Messages.getString("PAGE_POPUP_REMOVES", selectedIndexes.size()));
        }

        JMenuItem m1 = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_M1"));
        JMenuItem p1 = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_P1"));
        JMenuItem ma = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_MA"));
        JMenuItem pa = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_PA"));
        JMenuItem m2 = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_M2"));
        JMenuItem p2 = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_P2"));
        JMenuItem mb = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_MB"));
        JMenuItem pb = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_PB"));

        JMenuItem editSelected = new JMenuItem(Messages.getString("PAGE_POPUP_EDIT_SELECTED"));
        editSelected.addActionListener(editSelectedAction);
        JMenuItem rotateLeft = new JMenuItem(Messages.getString("PAGE_POPUP_ROTATE_LEFT"));
        rotateLeft.addActionListener(rotateLeftAction);
        JMenuItem rotateRight = new JMenuItem(Messages.getString("PAGE_POPUP_ROTATE_RIGHT"));
        rotateRight.addActionListener(rotateRightAction);

        JMenuItem rename = new JMenuItem(Messages.getString("PAGE_POPUP_RENAME"));
        rename.setEnabled(selectedIndexes.size() == 1);

        rename.addActionListener(renameAction);
        remove.addActionListener(removeAction);
        new MoveActionListener(false, -1, m1);
        new MoveActionListener(false, 1, p1);
        new MoveActionListener(true, -1, ma);
        new MoveActionListener(true, 1, pa);
        new MoveActionListener(false, -2, m2);
        new MoveActionListener(false, 2, p2);
        new MoveActionListener(true, -2, mb);
        new MoveActionListener(true, 2, pb);

        add(editSelected);
        add(rotateLeft);
        add(rotateRight);
        add(rename);
        add(m1);
        add(p1);
        add(ma);
        add(pa);
        add(m2);
        add(p2);
        add(mb);
        add(pb);
        add(remove);

        for (Map.Entry<String, String> en : Context.getPageTags().entrySet()) {
            JMenuItem add = new JMenuItem(Messages.getString("PAGE_POPUP_ADD_TAG", en.getValue()));
            add.addActionListener(new TagActionListener(en.getKey(), true));
            add(add);
        }
        for (Map.Entry<String, String> en : Context.getPageTags().entrySet()) {
            JMenuItem rem = new JMenuItem(Messages.getString("PAGE_POPUP_REMOVE_TAG", en.getValue()));
            rem.addActionListener(new TagActionListener(en.getKey(), false));
            add(rem);
        }
    }

    boolean isMovePossible(boolean letter, int count) {
        List<String> pagesList = book.listPages();
        Set<String> pagesSet = new HashSet<>(pagesList);
        List<String> movedPages = new ArrayList<>();
        for (String p : pagesList) {
            if (selectedPages.contains(p)) {
                pagesSet.remove(p);
                movedPages.add(p);
            }
        }
        for (int i = 0; i < movedPages.size(); i++) {
            movedPages.set(i, Book2.incPagePos(movedPages.get(i), letter, count));
        }
        for (String p : movedPages) {
            if (pagesSet.contains(p)) {
                return false;
            }
        }
        return true;
    }

    boolean isAllLetters() {
        List<String> pagesList = book.listPages();
        for (String p : pagesList) {
            if (selectedPages.contains(p)) {
                char last = p.charAt(p.length() - 1);
                if (last >= '0' && last <= '9') {
                    return false;
                }
            }
        }
        return true;
    }

    ActionListener removeAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (JOptionPane.showConfirmDialog(DataStorage.mainFrame,
                    Messages.getString("PAGE_POPUP_CONFIRM_REMOVE", selectedIndexes.size()), "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            LOG.info("Remove pages " + selectedIndexes);
            try {
                List<String> pagesList = book.listPages();
                for (String p : pagesList) {
                    if (selectedPages.contains(p)) {
                        PageFileInfo pfi=new PageFileInfo(book, p);
                        book.removePage(p);
                        File jpg = pfi.getPreviewFile();
                        File raw = pfi.getOriginalFile();
                        if (jpg.exists() && !jpg.delete()) {
                            throw new Exception(Messages.getString("PAGE_POPUP_ERROR_REMOVE", p));
                        }
                        if (raw.exists() && !raw.delete()) {
                            throw new Exception(Messages.getString("PAGE_POPUP_ERROR_REMOVE", p));
                        }
                    }
                }
                book.save();
            } catch (Throwable ex) {
                LOG.error("Error remove : ", ex);
                JOptionPane.showMessageDialog(null, "Error: " + ex.getClass() + " / " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            controller.show();
        }
    };

    ActionListener editSelectedAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            EditPageController.show(controller, selectedPages, selectedPages.get(0));
        }
    };

    String showPageNumberDialog() {
        StringBuilder result = new StringBuilder();
        PageNumber dialog = new PageNumber(DataStorage.mainFrame, true);
        dialog.setTitle(Messages.getString("PAGE_NUMBER_TITLE", Book2.simplifyPageNumber(selectedPages.get(0))));
        dialog.errorLabel.setText(" ");
        dialog.renameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result.append(dialog.txtNumber.getText().trim());
                dialog.dispose();
            }
        });
        dialog.getRootPane().setDefaultButton(dialog.renameButton);

        dialog.txtNumber.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                check(e.getDocument());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                check(e.getDocument());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                check(e.getDocument());
            }

            void check(Document d) {
                dialog.errorLabel.setText(" ");
                try {
                    String newText = d.getText(0, d.getLength()).trim();
                    newText = Book2.formatPageNumber(newText);
                    if (StringUtils.isEmpty(newText)) {
                        dialog.renameButton.setEnabled(false);
                        dialog.errorLabel.setText(Messages.getString("PAGE_NUMBER_ERROR_WRONG"));
                    } else {
                        Book2.PageInfo pi = book.getPageInfo(newText);
                        if (pi != null) {
                            dialog.renameButton.setEnabled(false);
                            dialog.errorLabel.setText(
                                    Messages.getString("PAGE_NUMBER_ERROR_EXIST", Book2.simplifyPageNumber(newText)));
                        } else {
                            dialog.renameButton.setEnabled(true);
                        }
                    }
                } catch (BadLocationException ex) {
                    dialog.renameButton.setEnabled(false);
                }
            }
        });

        dialog.setLocationRelativeTo(DataStorage.mainFrame);
        dialog.setVisible(true);
        return result.toString();
    }

    ActionListener rotateLeftAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("Rotate left");
            for (String p : selectedPages) {
                PageInfo pi = book.getPageInfo(p);
                pi.rotate = (pi.rotate + 3) % 4;
                controller.updatePreview(p);
            }
        }
    };
    ActionListener rotateRightAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("Rotate right");
            for (String p : selectedPages) {
                PageInfo pi = book.getPageInfo(p);
                pi.rotate = (pi.rotate + 1) % 4;
                controller.updatePreview(p);
            }
        }
    };

    ActionListener renameAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String newPage = showPageNumberDialog();
            newPage = Book2.formatPageNumber(newPage);
            if (StringUtils.isEmpty(newPage)) {
                return;
            }
            String oldPage = selectedPages.get(0);
            LOG.info("Rename page " + oldPage + " to " + newPage);
            try {
                PageFileInfo pfi=new PageFileInfo(book, oldPage);
                PageInfo pi = book.removePage(oldPage);
                if (pi == null) {
                    throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", oldPage));
                }
                pi.pageNumber = newPage;
                book.addPage(pi);
                PageFileInfo pfo=new PageFileInfo(book, newPage);

                File jpg = pfi.getPreviewFile();
                File raw = pfi.getOriginalFile();
                File newJpg = pfo.getPreviewFile();
                File newRaw = pfo.getOriginalFile();
                if (!jpg.renameTo(newJpg)) {
                    throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", oldPage));
                }
                if (!raw.renameTo(newRaw)) {
                    throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", oldPage));
                }

                book.save();
            } catch (Throwable ex) {
                LOG.error("Error rename : ", ex);
                JOptionPane.showMessageDialog(null, "Error: " + ex.getClass() + " / " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            controller.show();
        }
    };

    class TagActionListener implements ActionListener {
        private final String tag;
        private final boolean add;
        public TagActionListener(String tag, boolean add) {
            this.tag=tag;
            this.add=add;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (String p : book.listPages()) {
                if (selectedPages.contains(p)) {
                    PageInfo pi = book.getPageInfo(p);
                    if (add) {
                        pi.tags.add(tag);
                    } else {
                        pi.tags.remove(tag);
                    }
                }
            }
        }
    }

    class MoveActionListener implements ActionListener {
        private final boolean letter;
        private final int count;

        public MoveActionListener(boolean letter, int count, JMenuItem menuItem) {
            this.letter = letter;
            this.count = count;

            boolean enabled = isMovePossible(letter, count);
            if (letter && !isAllLetters()) {
                enabled = false;
            }
            menuItem.setEnabled(enabled);
            if (enabled) {
                menuItem.addActionListener(this);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("Move pages");
            try {
                File tempDir = new File(book.getBookDir(), "move-temp.dir");
                if (tempDir.exists()) {
                    throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE_PREVIOUS"));
                }
                tempDir.mkdirs();

                Map<String, PageInfo> movedPages = new TreeMap<>();
                Map<String, PageFileInfo> movedPageFiles = new TreeMap<>();
                List<String> pagesList = book.listPages();

                for (String p : pagesList) {
                    if (selectedPages.contains(p)) {
                        PageFileInfo pfi=new PageFileInfo(book, p);
                        movedPageFiles.put(p, pfi);
                        File preview = pfi.getPreviewFile();
                        File orig = pfi.getOriginalFile();
                        File tempPreview = new File(tempDir, preview.getName());
                        File tempOrig = new File(tempDir, orig.getName());
                        if (!preview.renameTo(tempPreview)) {
                            throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", p));
                        }
                        if (!orig.renameTo(tempOrig)) {
                            throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", p));
                        }
                    }
                }

                for (String p : pagesList) {
                    if (selectedPages.contains(p)) {
                        PageInfo pi = book.removePage(p);
                        movedPages.put(p, pi);
                    }
                }
                for (Map.Entry<String, PageInfo> en : movedPages.entrySet()) {
                    String newPage = Book2.incPagePos(en.getKey(), letter, count);
                    en.getValue().pageNumber = newPage;
                    book.addPage(en.getValue());
                }
                book.save();

                for (String p : movedPages.keySet()) {
                    PageFileInfo pfi=movedPageFiles.get(p);
                    File tempPreview = new File(tempDir, pfi.getPreviewFile().getName());
                    File tempOrig = new File(tempDir, pfi.getOriginalFile().getName());

                    String newPage = Book2.incPagePos(p, letter, count);
                    PageFileInfo pfo=new PageFileInfo(book, newPage);

                    File preview = pfo.getPreviewFile();
                    File orig = pfo.getOriginalFile();
                    if (!tempPreview.renameTo(preview)) {
                        throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", p));
                    }
                    if (!tempOrig.renameTo(orig)) {
                        throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", p));
                    }
                }

                tempDir.delete();
            } catch (Throwable ex) {
                LOG.error("Error remove : ", ex);
                JOptionPane.showMessageDialog(null, "Error: " + ex.getClass() + " / " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            controller.resetAllPreviews();
            controller.show();
        }
    }
}
