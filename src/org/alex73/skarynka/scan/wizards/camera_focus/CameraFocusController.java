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
package org.alex73.skarynka.scan.wizards.camera_focus;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.common.ImageViewPane;
import org.alex73.skarynka.scan.devices.CHDKCameras;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraFocusController {
    private static Logger LOG = LoggerFactory.getLogger(CameraFocusController.class);

    private CHDKCameras cameras;
    private CameraFocusPanel dialog;
    private ImageViewPane previewLeft, previewRight;
    private SpinnerNumberModel model;

    public void show() {
        try {
            cameras = (CHDKCameras) DataStorage.device;
            dialog = new CameraFocusPanel(DataStorage.mainFrame, true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.btnClose.setEnabled(false);
            dialog.btnClose.addActionListener(closeActionListener);
            dialog.btnFocus.addActionListener(focusActionListener);
            dialog.btnRotateLeft.addActionListener(rotateLeft);
            dialog.btnRotateRight.addActionListener(rotateRight);
            dialog.btnSwap.addActionListener(swap);

            int maxZoom = cameras.getMaxZoom();
            int currentZoom = cameras.getZoom();
            int step = Math.round(maxZoom * 0.1f);
            dialog.lblZoom.setText(Messages.getString("CAMERA_FOCUS_ZOOM", maxZoom));

            // set equals zoom for all cameras
            cameras.setZoom(currentZoom);

            model = new SpinnerNumberModel(currentZoom, 0, maxZoom, step);
            model.addChangeListener(zoomChangeListener);
            dialog.spZoom.setModel(model);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;

            gbc.gridx = 0;
            previewLeft = new ImageViewPane();
            dialog.getContentPane().add(previewLeft, gbc);

            gbc.gridx = 1;
            previewRight = new ImageViewPane();
            dialog.getContentPane().add(previewRight, gbc);

            cameras.setPreviewPanels(previewLeft, previewRight);

            dialog.setSize(1000, 800);
            dialog.setLocationRelativeTo(DataStorage.mainFrame);
            dialog.setVisible(true);
        } catch (Throwable ex) {
            LOG.warn("Error initialize focus wizard", ex);
            JOptionPane.showMessageDialog(DataStorage.mainFrame, "Error: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    ActionListener rotateLeft = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            previewLeft.setRotation((previewLeft.getRotation() + 1) % 4);
        }
    };

    ActionListener rotateRight = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            previewRight.setRotation((previewRight.getRotation() + 1) % 4);
        }
    };

    ActionListener swap = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cameras.swap();
        }
    };

    ChangeListener zoomChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            try {
                int zoom = (int) model.getValue();
                cameras.setZoom(zoom);
            } catch (Exception ex) {
                LOG.warn("Error focus camera",ex);
                JOptionPane.showMessageDialog(
                        DataStorage.mainFrame,
                        Messages.getString("ERROR_FOCUS", ex.getClass().getName(), ex.getMessage(),
                                Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE));
            }
        }
    };
    ActionListener closeActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.dispose();
        }
    };
    ActionListener focusActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.lblStatus.setText(Messages.getString("CAMERA_FOCUS_PROCESSING"));
            new SwingWorker<List<Integer>, Object>() {
                @Override
                protected List<Integer> doInBackground() throws Exception {
                    cameras.setRotations(previewLeft.getRotation(), previewRight.getRotation());
                    return cameras.focus();
                }

                protected void done() {
                    try {
                        List<Integer> distances = get();
                        dialog.lblStatus.setText(Messages.getString("CAMERA_FOCUS_DISTANCE",
                                distances.toString()));
                        dialog.btnClose.setEnabled(true);
                    } catch (Exception ex) {
                        dialog.lblStatus.setText(Messages.getString("CAMERA_FOCUS_ERROR", ex.getMessage()));
                    }
                }
            }.execute();
        }
    };
}
