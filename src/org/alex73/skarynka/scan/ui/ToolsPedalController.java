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
package org.alex73.skarynka.scan.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.hid.HIDScanController;

public class ToolsPedalController {
    static ToolsPedal dialog;
    static Cleaner cleaner;
    static long cleanTime;

    public static void show() {
        dialog = new ToolsPedal(DataStorage.mainFrame, true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(DataStorage.mainFrame);

        if (DataStorage.hidScanDevice == null) {
            dialog.lblConnected.setText(Messages.getString("TOOLS_PEDAL_NOTDEFINED"));
        } else if (!DataStorage.hidScanDevice.isConnected()) {
            dialog.lblConnected.setText(Messages.getString("TOOLS_PEDAL_NOTCONNECTED"));
        } else {
            dialog.lblConnected.setText(
                    Messages.getString("TOOLS_PEDAL_CONNECTED", DataStorage.hidScanDevice.getHIDDeviceUsbId()));
        }
        DataStorage.hidScanDevice.setTestListener(hidListener);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleaner.finished = true;
                DataStorage.hidScanDevice.setTestListener(null);
            }
        });

        cleanTime = Long.MAX_VALUE;
        cleaner = new Cleaner();
        cleaner.start();

        dialog.setVisible(true);
    }

    static HIDScanController.ScanListener hidListener = new HIDScanController.ScanListener() {
        @Override
        public void pressed(String key) {
            dialog.lblKey.setText(dialog.lblKey.getText() + key + "\n");
            cleanTime = System.currentTimeMillis() + 3000;
        }
    };

    static class Cleaner extends Thread {
        boolean finished;

        @Override
        public void run() {
            try {
                while (!finished) {
                    Thread.sleep(1000);
                    if (cleanTime < System.currentTimeMillis()) {
                        dialog.lblKey.setText("");
                    }
                }
            } catch (Exception ex) {
            }
        }
    }
}
