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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Manifest;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.alex73.skarynka.scan.devices.Stub;
import org.alex73.skarynka.scan.hid.HIDScanController;
import org.alex73.skarynka.scan.process.ProcessDaemon;
import org.alex73.skarynka.scan.ui.MainFrame;
import org.alex73.skarynka.scan.ui.ToolsPedalController;
import org.alex73.skarynka.scan.ui.add.AddController;
import org.alex73.skarynka.scan.ui.book.BooksController;
import org.alex73.skarynka.scan.ui.book.PanelEditController;
import org.alex73.skarynka.scan.ui.scan.ScanDialogController;
import org.alex73.skarynka.scan.wizards.camera_badpixels.CameraBadPixelsWizard;
import org.alex73.skarynka.scan.wizards.camera_focus.CameraFocusController;
import org.alex73.skarynka.scan.wizards.camera_init.CameraInitWizard;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for execution.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class Scan2 {
    private static Logger LOG = LoggerFactory.getLogger(Scan2.class);

    public static void main(String[] args) {
        try {
            File lockFile = new File("scan.lock");
            FileChannel lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            FileLock lock = lockChannel.tryLock();
            if (lock == null) {
                JOptionPane.showMessageDialog(null, Messages.getString("FRAME_ALREADY_RUNNING"),
                        Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            PropertyConfigurator.configure("log4j.properties");

            String version = readVersion();
            LOG.info("Skarynka start version " + version);
            Context.load();

            String locale_language = Context.getSettings().get("locale_language");
            String locale_country = Context.getSettings().get("locale_country");
            String locale_variant = Context.getSettings().get("locale_variant");
            if (locale_language != null) {
                if (locale_country != null) {
                    if (locale_variant != null) {
                        Locale.setDefault(new Locale(locale_language, locale_country, locale_variant));
                    } else {
                        Locale.setDefault(new Locale(locale_language, locale_country));
                    }
                } else {
                    Locale.setDefault(new Locale(locale_language));
                }
            }

            DataStorage.mainFrame = new MainFrame();
            DataStorage.mainFrame.setSize(700, 500);
            DataStorage.mainFrame
                    .setExtendedState(DataStorage.mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

            if (Context.getPermissions().ProcessingBooks || Context.getPermissions().ProcessingControls) {
                DataStorage.daemon = new ProcessDaemon();
                DataStorage.daemon.start();
            } else {
                DataStorage.mainFrame.cbProcess.setVisible(false);
            }
            setup();

            DataStorage.addTab(new BooksController());

            if (Context.getSettings().get("mousepedal-usbid") != null) {
                DataStorage.hidScanDevice = new HIDScanController(Context.getSettings().get("mousepedal-usbid"),
                        Context.getSettings().get("mousepedal-usbInterfaceProtocol"));
                DataStorage.hidScanDevice.start();
            }

            DataStorage.mainFrame.setTitle(Messages.getString("FRAME_TITLE", version));

            DataStorage.mainFrame.setVisible(true);
        } catch (Throwable ex) {
            LOG.error("Error startup", ex);
            JOptionPane.showMessageDialog(null, "Error: " + ex.getClass() + " / " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    static void setup() {

        if (System.getProperty("STUB") != null) {
            DataStorage.mainFrame.cameraMenu.setVisible(false);
            DataStorage.device = new Stub("/data/tmp/scan/stub");
        }

        DataStorage.mainFrame.cameraBadPixels.setVisible(Context.getPermissions().CameraBadPixels);
        DataStorage.mainFrame.cameraMenu.setVisible(Context.getPermissions().ShowDevices);
        DataStorage.mainFrame.processScan.setVisible(Context.getPermissions().ShowDevices);
        DataStorage.mainFrame.processAdd.setVisible(Context.getPermissions().ShowManualAdd);
        DataStorage.mainFrame.addAllFiles.setVisible(Context.getPermissions().ShowManualAdd);

        DataStorage.mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DataStorage.finish();
            }
        });
        if (DataStorage.daemon != null) {
            DataStorage.mainFrame.cbProcess.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DataStorage.daemon.setPaused(DataStorage.mainFrame.cbProcess.isSelected());
                }
            });
        }

        DataStorage.mainFrame.cameraInit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CameraInitWizard().process();
            }
        });

        DataStorage.mainFrame.cameraFocus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.device.setPreviewPanels();
                new CameraFocusController().show();
            }
        });

        DataStorage.mainFrame.cameraBadPixels.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CameraBadPixelsWizard().process();
            }
        });

        DataStorage.mainFrame.cameraMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                DataStorage.mainFrame.cameraFocus.setEnabled(DataStorage.device != null);
                DataStorage.mainFrame.cameraBadPixels.setEnabled(DataStorage.device != null);
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        DataStorage.mainFrame.addAllFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddController.addAll();
            }
        });

        DataStorage.mainFrame.processScan.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScanDialogController.show((PanelEditController) DataStorage.getActiveTab());
            }
        });
        DataStorage.mainFrame.processAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddController.add((PanelEditController) DataStorage.getActiveTab());
            }
        });

        DataStorage.mainFrame.workMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                boolean deviceReady = DataStorage.device != null && DataStorage.device.readyForScan();
                DataStorage.mainFrame.processScan.setEnabled(deviceReady);
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        DataStorage.mainFrame.itemAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.view = DataStorage.SHOW_TYPE.ALL;
                DataStorage.refreshBookPanels(false);
            }
        });
        DataStorage.mainFrame.itemSeq.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.view = DataStorage.SHOW_TYPE.SEQ;
                DataStorage.refreshBookPanels(false);
            }
        });
        DataStorage.mainFrame.itemOdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.view = DataStorage.SHOW_TYPE.ODD;
                DataStorage.refreshBookPanels(false);
            }
        });
        DataStorage.mainFrame.itemEven.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.view = DataStorage.SHOW_TYPE.EVEN;
                DataStorage.refreshBookPanels(false);
            }
        });
        DataStorage.mainFrame.itemCropErr.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.view = DataStorage.SHOW_TYPE.CROP_ERRORS;
                DataStorage.refreshBookPanels(false);
            }
        });
        DataStorage.mainFrame.viewMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                boolean bookOpened = true;// TODO DataStorage.book != null;
                for (Enumeration<AbstractButton> en = DataStorage.mainFrame.viewButtonsGroup.getElements(); en
                        .hasMoreElements();) {
                    en.nextElement().setEnabled(bookOpened);
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        DataStorage.mainFrame.viewInc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.previewMaxHeight = Math.round(DataStorage.previewMaxHeight * 1.1f);
                DataStorage.previewMaxWidth = Math.round(DataStorage.previewMaxHeight * 0.7f);
                DataStorage.refreshBookPanels(true);
            }
        });
        DataStorage.mainFrame.viewDec.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.previewMaxHeight = Math.round(DataStorage.previewMaxHeight * 0.9f);
                DataStorage.previewMaxWidth = Math.round(DataStorage.previewMaxHeight * 0.7f);
                DataStorage.refreshBookPanels(true);
            }
        });

        DataStorage.mainFrame.closeBook.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                DataStorage.closeTab(DataStorage.getActiveTab());
            }
        });
        
        DataStorage.mainFrame.toolsPedal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ToolsPedalController.show();
            }
        });

        for (Map.Entry<String, String> en : Context.getPageTags().entrySet()) {
            String label = Messages.getString("MENU_VIEW_TAG", en.getValue());
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
            item.setName(en.getKey());
            DataStorage.mainFrame.viewButtonsGroup.add(item);
            DataStorage.mainFrame.viewMenu.add(item);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DataStorage.view = DataStorage.SHOW_TYPE.TAG;
                    DataStorage.viewTag = ((JRadioButtonMenuItem) e.getSource()).getName();
                    DataStorage.refreshBookPanels(false);
                }
            });
        }

        DataStorage.mainFrame.tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int sel = DataStorage.mainFrame.tabs.getSelectedIndex();
                if (sel < 0) {
                    return;
                }
                DataStorage.activateTab(sel);
            }
        });
    }

    public static String readVersion() throws Exception {
        String className = Scan2.class.getSimpleName() + ".class";
        String classPath = Scan2.class.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            // Class not from JAR
            return "dev";
        }
        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
                + "/META-INF/MANIFEST.MF";
        Manifest manifest;
        try (InputStream in = new URL(manifestPath).openStream()) {
            manifest = new Manifest(in);
        }
        return manifest.getMainAttributes().getValue("Build");
    }
}
