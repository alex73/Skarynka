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
import org.alex73.skarynka.scan.Book2.PageInfo;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
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
    private final String startSelection, endSelection;

    public PagePopupMenu(PanelEditController controller, String fromPage, String toPage) {
        this.controller = controller;
        this.book = controller.getBook();
        if (fromPage == null) {
            fromPage = toPage;
        }
        if (fromPage.compareTo(toPage) <= 0) {
            startSelection = fromPage;
            endSelection = toPage;
        } else {
            startSelection = toPage;
            endSelection = fromPage;
        }

        JMenuItem remove;
        if (startSelection.equals(endSelection)) {
            remove = new JMenuItem(
                    Messages.getString("PAGE_POPUP_REMOVE", Book2.simplifyPageNumber(endSelection)));
        } else {
            remove = new JMenuItem(Messages.getString("PAGE_POPUP_REMOVES",
                    Book2.simplifyPageNumber(startSelection), Book2.simplifyPageNumber(endSelection)));
        }

        JMenuItem m1 = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_M1"));
        JMenuItem p1 = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_P1"));
        JMenuItem ma = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_MA"));
        JMenuItem pa = new JMenuItem(Messages.getString("PAGE_POPUP_CHANGE_PA"));

        JMenuItem rename = new JMenuItem(Messages.getString("PAGE_POPUP_RENAME"));
        rename.setEnabled(startSelection.equals(endSelection));

        rename.addActionListener(renameAction);
        remove.addActionListener(removeAction);
        new MoveActionListener(false, -1, m1);
        new MoveActionListener(false, 1, p1);
        new MoveActionListener(true, -1, ma);
        new MoveActionListener(true, 1, pa);

        add(rename);
        add(m1);
        add(p1);
        add(ma);
        add(pa);
        add(remove);
    }

    boolean isPossible(boolean letter, int count) {
        List<String> pagesList = book.listPages();
        Set<String> pagesSet = new HashSet<>(pagesList);
        List<String> movedPages = new ArrayList<>();
        for (String p : pagesList) {
            if (startSelection.compareTo(p) <= 0 && p.compareTo(endSelection) <= 0) {
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
            if (startSelection.compareTo(p) <= 0 && p.compareTo(endSelection) <= 0) {
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
                    Messages.getString("PAGE_POPUP_CONFIRM_REMOVE", startSelection, endSelection), "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            LOG.info("Remove pages from " + startSelection + " to " + endSelection);
            try {
                List<String> pagesList = book.listPages();
                for (String p : pagesList) {
                    if (startSelection.compareTo(p) <= 0 && p.compareTo(endSelection) <= 0) {
                        book.removePage(p);
                        File jpg = new File(book.getBookDir(), p + ".jpg");
                        File raw = new File(book.getBookDir(), p + ".raw");
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
                JOptionPane.showMessageDialog(null, "Error: " + ex.getClass() + " / " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            controller.show();
        }
    };

    String showPageNumberDialog() {
        StringBuilder result = new StringBuilder();
        PageNumber dialog = new PageNumber(DataStorage.mainFrame, true);
        dialog.setTitle(Messages.getString("PAGE_NUMBER_TITLE", Book2.simplifyPageNumber(startSelection)));
        dialog.errorLabel.setText(" ");
        dialog.renameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result.append(dialog.txtNumber.getText().trim());
                dialog.dispose();
            }
        });

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
                            dialog.errorLabel.setText(Messages.getString("PAGE_NUMBER_ERROR_EXIST",
                                    Book2.simplifyPageNumber(newText)));
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

    ActionListener renameAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String newPage = showPageNumberDialog();
            newPage = Book2.formatPageNumber(newPage);
            if (StringUtils.isEmpty(newPage)) {
                return;
            }
            LOG.info("Rename page " + startSelection + " to " + newPage);
            try {
                PageInfo pi = book.removePage(startSelection);
                if (pi == null) {
                    throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", startSelection));
                }
                pi.pageNumber = newPage;
                book.addPage(pi);

                File jpg = new File(book.getBookDir(), startSelection + ".jpg");
                File raw = new File(book.getBookDir(), startSelection + ".raw");
                File newJpg = new File(book.getBookDir(), newPage + ".jpg");
                File newRaw = new File(book.getBookDir(), newPage + ".raw");
                if (!jpg.renameTo(newJpg)) {
                    throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", startSelection));
                }
                if (!raw.renameTo(newRaw)) {
                    throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", startSelection));
                }

                book.save();
            } catch (Throwable ex) {
                LOG.error("Error rename : ", ex);
                JOptionPane.showMessageDialog(null, "Error: " + ex.getClass() + " / " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            controller.show();
        }
    };

    class MoveActionListener implements ActionListener {
        private final boolean letter;
        private final int count;

        public MoveActionListener(boolean letter, int count, JMenuItem menuItem) {
            this.letter = letter;
            this.count = count;

            boolean enabled = isPossible(letter, count);
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
            LOG.info("Move pages from " + startSelection + " to " + endSelection + ": letter=" + letter
                    + " count=" + count);
            try {
                File tempDir = new File(book.getBookDir(), "move-temp.dir");
                if (tempDir.exists()) {
                    throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE_PREVIOUS"));
                }
                tempDir.mkdirs();

                Map<String, PageInfo> movedPages = new TreeMap<>();
                List<String> pagesList = book.listPages();

                for (String p : pagesList) {
                    if (startSelection.compareTo(p) <= 0 && p.compareTo(endSelection) <= 0) {
                        File jpg = new File(book.getBookDir(), p + ".jpg");
                        File raw = new File(book.getBookDir(), p + ".raw");
                        File tempJpg = new File(tempDir, p + ".jpg");
                        File tempRaw = new File(tempDir, p + ".raw");
                        if (!jpg.renameTo(tempJpg)) {
                            throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", p));
                        }
                        if (!raw.renameTo(tempRaw)) {
                            throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", p));
                        }
                    }
                }

                for (String p : pagesList) {
                    if (startSelection.compareTo(p) <= 0 && p.compareTo(endSelection) <= 0) {
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
                    File tempJpg = new File(tempDir, p + ".jpg");
                    File tempRaw = new File(tempDir, p + ".raw");

                    String newPage = Book2.incPagePos(p, letter, count);

                    File jpg = new File(book.getBookDir(), newPage + ".jpg");
                    File raw = new File(book.getBookDir(), newPage + ".raw");
                    if (!tempJpg.renameTo(jpg)) {
                        throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", p));
                    }
                    if (!tempRaw.renameTo(raw)) {
                        throw new Exception(Messages.getString("PAGE_POPUP_ERROR_MOVE", p));
                    }
                }

                tempDir.delete();
            } catch (Throwable ex) {
                LOG.error("Error remove : ", ex);
                JOptionPane.showMessageDialog(null, "Error: " + ex.getClass() + " / " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            controller.show();
        }
    }
}
