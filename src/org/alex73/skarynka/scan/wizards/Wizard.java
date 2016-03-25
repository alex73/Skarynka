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
package org.alex73.skarynka.scan.wizards;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;

import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Wizard extends JDialog {
    private static Logger LOG = LoggerFactory.getLogger(Wizard.class);

    private JTextPane area;
    private JScrollPane sc;
    private JButton btnNext;
    private JButton btnCancel;
    private Runnable next;
    private String nextText;

    public Wizard(String title) {
        super(DataStorage.mainFrame, title, true);
        getContentPane().setLayout(new BorderLayout());

        area = new JTextPane();
        area.setEditable(false);
        area.setContentType("text/html");
        sc = new JScrollPane(area);
        getContentPane().add(sc, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout());
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        btnNext = new JButton(Messages.getString("WIZARD_NEXT"));
        buttonsPanel.add(btnNext);
        btnCancel = new JButton(Messages.getString("CANCEL"));
        buttonsPanel.add(btnCancel);

        setSize(1000, 800);
        setLocationRelativeTo(DataStorage.mainFrame);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (btnCancel.isEnabled()) {
                    cancel();
                }
            }
        });

        btnNext.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeNext();
            }
        });
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
    }

    protected void addImage(String url, String imagePath) {
        Dictionary<URL, Image> cache = (Dictionary<URL, Image>) area.getDocument().getProperty("imageCache");
        if (cache == null) {
            cache = new Hashtable<URL, Image>();
            area.getDocument().putProperty("imageCache", cache);
        }

        try {
            Image image = ImageIO.read(Wizard.class.getResource(imagePath));
            cache.put(new URL(url), image);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void showStep(String text, String nextText, Runnable clickNext) {
        next = clickNext;
        this.nextText = nextText;
        area.setText(text);
        sc.scrollRectToVisible(new Rectangle());
        if (!isVisible()) {
            setVisible(true);
        }
    }

    protected void showLastStep(String text) {
        btnNext.setText(Messages.getString("OK"));
        btnCancel.setVisible(false);
        next = new Runnable() {
            @Override
            public void run() {
                done();
                dispose();
            }
        };
        area.setText(text);
        sc.scrollRectToVisible(new Rectangle());
    }

    protected void executeNext() {
        btnNext.setEnabled(false);
        btnCancel.setEnabled(false);
        area.setText(nextText);
        sc.scrollRectToVisible(new Rectangle());
        new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                next.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    btnNext.setEnabled(true);
                    btnCancel.setEnabled(true);
                } catch (Throwable ex) {
                    LOG.error("Error in wizard", ex);
                    area.setText("ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    protected String getPackagePath() {
        String p = '/' + this.getClass().getPackage().getName();
        p = p.replace('.', '/');
        return p;
    }

    abstract protected void cancel();

    abstract protected void done();
}
