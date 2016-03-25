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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.alex73.skarynka.scan.ActionErrorListener;
import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.IBookIterator;
import org.alex73.skarynka.scan.ITabController;
import org.alex73.skarynka.scan.Messages;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for books list panel.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class BooksController implements ITabController {
    private static Logger LOG = LoggerFactory.getLogger(BooksController.class);

    private BooksPanel panel;
    private List<BookRow> books = new ArrayList<>();

    private Set<String> currentBooksNames = new HashSet<>();

    @Override
    public String getTabName() {
        return Messages.getString("PANEL_BOOK_TITLE");
    }

    @Override
    public Component getTabComponent() {
        return panel;
    }

    @Override
    public void activate() {
        DataStorage.mainFrame.workMenu.setEnabled(false);
        DataStorage.mainFrame.viewMenu.setEnabled(false);
        refresh();
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void close() {
    }

    public void refresh() {
        int selected = panel.table.getSelectedRow();

        try {
            listScanDirs();
        } catch (Throwable ex) {
            LOG.error("Error list books", ex);
            JOptionPane.showMessageDialog(DataStorage.mainFrame,
                    "Error: " + ex.getClass() + " / " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        ((AbstractTableModel) panel.table.getModel()).fireTableDataChanged();
        if (selected >= 0) {
            panel.table.setRowSelectionInterval(selected, selected);
        }
    }

    public BooksController() {
        try {
            panel = new BooksPanel();
            ((AbstractDocument) panel.txtNewName.getDocument()).setDocumentFilter(bookNameFilter);

            listScanDirs();

            panel.table.setModel(model());
            panel.table.setRowSelectionAllowed(true);
            panel.table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                        BookRow b = books.get(panel.table.getSelectedRow());
                        DataStorage.openBook(b.bookName, true);
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        for (int i = 0; i < panel.menuProcess.getComponentCount(); i++) {
                            Component item = panel.menuProcess.getComponent(i);
                            if ((item instanceof JMenuItem) && (item.getName() != null)) {
                                panel.menuProcess.remove(i);
                                i--;
                            }
                        }
                        if (Context.getPermissions().BookControl) {
                            for (int scale : new int[] { 25, 50, 75, 100, 200 }) {
                                JMenuItem item = new JMenuItem(scale + "%");
                                item.setName(scale + "%");
                                item.addActionListener(new ChangeScale(scale));
                                panel.menuProcess.add(item);
                            }
                        }

                        currentBooksNames.clear();
                        int[] selected = panel.table.getSelectedRows();
                        boolean allLocals = true;
                        boolean processAllowed = Context.getPermissions().BookControl;
                        for (int row : selected) {
                            BookRow b = books.get(panel.table.convertRowIndexToModel(row));
                            currentBooksNames.add(b.bookName);
                            if (!b.local) {
                                allLocals = false;
                            }
                        }
                        panel.itemFinish.setVisible(allLocals);
                        if (processAllowed) {
                            for (Map.Entry<String, String> en : Context.getProcessCommands().entrySet()) {
                                JMenuItem item = new JMenuItem(en.getValue());
                                item.setName(en.getKey());
                                item.addActionListener(commandListener);
                                panel.menuProcess.add(item);
                            }
                        }
                        panel.menuProcess.show(panel.table, e.getX(), e.getY());
                    }
                }
            });

            panel.btnCreate.setEnabled(false);
            panel.btnCreate.addActionListener(
                    new ActionErrorListener(panel, "ERROR_BOOK_CREATE", LOG, "Error create book") {
                        protected void action(ActionEvent e) throws Exception {
                            File bookDir = new File(Context.getBookDir(), panel.txtNewName.getText());
                            if (bookDir.exists()) {
                                JOptionPane.showMessageDialog(panel,
                                        Messages.getString("PANEL_BOOK_NEW_BOOK_EXIST"),
                                        Messages.getString("PANEL_BOOK_TITLE"), JOptionPane.WARNING_MESSAGE);
                                return;
                            }

                            DataStorage.openBook(panel.txtNewName.getText(), true);
                        }
                    });

            setMenuListeners();
        } catch (Throwable ex) {
            LOG.error("Error list books", ex);
            JOptionPane.showMessageDialog(DataStorage.mainFrame,
                    "Error: " + ex.getClass() + " / " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    ActionListener commandListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            String command = ((JMenuItem) e.getSource()).getName();
            DataStorage.iterateByBooks(currentBooksNames, new IBookIterator() {
                @Override
                public void onBook(Book2 currentBook) throws Exception {
                    try {
                        File done = new File(currentBook.getBookDir(), ".process.done");
                        if (done.exists() && !done.delete()) {
                            throw new Exception("Error delete .done file");
                        }
                        File errors = new File(currentBook.getBookDir(), ".errors");
                        if (errors.exists() && !errors.delete()) {
                            throw new Exception("Error delete .errors file");
                        }
                        FileUtils.writeStringToFile(new File(currentBook.getBookDir(), ".process"), command,
                                "UTF-8");
                    } catch (Throwable ex) {
                        LOG.error("Error set comand to book", ex);
                        JOptionPane.showMessageDialog(DataStorage.mainFrame,
                                "Error: " + ex.getClass() + " / " + ex.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            refresh();
        }
    };

    class ChangeScale implements ActionListener {
        private final int scale;

        public ChangeScale(int scale) {
            this.scale = scale;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DataStorage.iterateByBooks(currentBooksNames, new IBookIterator() {

                @Override
                public void onBook(Book2 currentBook) throws Exception {
                    try {
                        currentBook.scale = scale;
                        currentBook.save();
                    } catch (Throwable ex) {
                        LOG.error("Error set scale to book", ex);
                        JOptionPane.showMessageDialog(DataStorage.mainFrame,
                                "Error: " + ex.getClass() + " / " + ex.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            refresh();
        }
    };

    void setMenuListeners() {
        panel.itemFinish.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.iterateByBooks(currentBooksNames, new IBookIterator() {

                    @Override
                    public void onBook(Book2 currentBook) throws Exception {
                        new File(currentBook.getBookDir(), ".local").delete();
                        currentBook.local = false;
                        try {
                            currentBook.save();
                        } catch (Exception ex) {
                            LOG.error("Error save book '" + currentBook.getName() + "'", ex);
                            JOptionPane.showMessageDialog(DataStorage.mainFrame,
                                    Messages.getString("ERROR_BOOK_SAVE", currentBook.getName(),
                                            ex.getMessage()),
                                    Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                panel.table.repaint();
            }
        });
        // panel.itemEdit.addActionListener(new ActionListener() {
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // try {
        // z
        // } catch (Exception ex) {
        // LOG.error("Error save book '" + currentBook.getName() + "'", ex);
        // JOptionPane.showMessageDialog(DataStorage.mainFrame,
        // Messages.getString("ERROR_BOOK_SAVE", currentBook.getName(), ex.getMessage()),
        // Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
        // }
        // }
        // });
    }

    TableModel model() {
        return new DefaultTableModel() {
            @Override
            public int getColumnCount() {
                return Context.getPermissions().BookControl ? 4 : 3;
            }

            @Override
            public int getRowCount() {
                return books.size();
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                case 0:
                    return Messages.getString("PANEL_BOOK_TABLE_NAME");
                case 1:
                    return Messages.getString("PANEL_BOOK_TABLE_PAGES");
                case 2:
                    return Messages.getString("PANEL_BOOK_TABLE_PAGE_SIZE");
                case 3:
                    return Messages.getString("PANEL_BOOK_TABLE_STATUS");
                }
                return null;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Object getValueAt(int row, int column) {
                BookRow b = books.get(row);
                switch (column) {
                case 0:
                    return b.bookName;
                case 1:
                    return b.pagesCount;
                case 2:
                    return b.info;
                case 3:
                    return b.status;
                }
                return null;
            }
        };
    }

    public void listScanDirs() throws Exception {
        books.clear();

        DataStorage.iterateByBooks(null, new IBookIterator() {
            @Override
            public void onBook(Book2 book) throws Exception {
                if (Context.getPermissions().ShowNonLocalBooks) {
                    books.add(new BookRow(book));
                } else {
                    if (book.local) {
                        books.add(new BookRow(book));
                    }
                }
            }
        });
    }

    static final Pattern RE_NAME = Pattern.compile("[A-Za-z0-9_\\-]+");

    DocumentFilter bookNameFilter = new DocumentFilter() {

        protected String check(String data) {
            return RE_NAME.matcher(data).matches() ? data.toLowerCase() : null;
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
            update(fb);
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            string = check(string);
            if (string != null) {
                super.insertString(fb, offset, string, attr);
                update(fb);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            text = check(text);
            if (text != null) {
                super.replace(fb, offset, length, text, attrs);
                update(fb);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        void update(FilterBypass fb) {
            panel.btnCreate.setEnabled(fb.getDocument().getLength() > 0);
        }
    };
}
