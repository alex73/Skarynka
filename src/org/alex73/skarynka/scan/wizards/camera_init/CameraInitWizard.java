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
package org.alex73.skarynka.scan.wizards.camera_init;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.camera.CameraFactory;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.ServiceException;
import org.alex73.skarynka.scan.devices.CHDKCameras;
import org.alex73.skarynka.scan.wizards.Wizard;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.javax.UsbHacks;

public class CameraInitWizard extends Wizard {
    private static Logger LOG = LoggerFactory.getLogger(CameraInitWizard.class);

    private CHDKCameras dev;

    public CameraInitWizard() {
        super(Messages.getString("CAMERA_INIT_TITLE"));

        addImage("file://camera-on.png", getPackagePath() + "/camera-on.png");
        addImage("file://camera-power.png", getPackagePath() + "/camera-power.png");
        addImage("file://camera-usb.png", getPackagePath() + "/camera-usb.png");
    }

    public void process() {
        if (DataStorage.device != null) {
            DataStorage.device.close();
            DataStorage.device = null;
        }
        showStep(Messages.getFile(getPackagePath() + "/instruction.html"),
                Messages.getString("CAMERA_INIT_PROCESS"), initCameras);
    }

    Runnable initCameras = new Runnable() {
        @Override
        public void run() {
            try {
                boolean oneBus = false;
                boolean differentCameras = false;
                String manufacturer = null, product = null;

                Set<Integer> usedBuses = new TreeSet<>();
                List<Camera> cameras = CameraFactory.findCameras();
                for (Camera c : cameras) {
                    int bus = UsbHacks.getDeviceId(c.getDevice()).getBusNumber();
                    if (!usedBuses.add(bus)) {
                        oneBus = true;
                    }
                    if (manufacturer == null || product == null) {
                        manufacturer = c.getDevice().getManufacturerString();
                        product = c.getDevice().getProductString();
                    } else if (!StringUtils.equals(manufacturer, c.getDevice().getManufacturerString())
                            || !StringUtils.equals(product, c.getDevice().getProductString())) {
                        differentCameras = true;
                    }
                }

                String m = Messages.getFile(getPackagePath() + "/instruction-done.html");
                switch (cameras.size()) {
                case 0:
                    LOG.info("No cameras");
                    throw new ServiceException("CHDK_NO_CAMERAS");
                case 1:
                    LOG.info("One camera");
                    m = m.replace("{0}", "Падлучана адна камэра.");
                    break;
                case 2:
                    LOG.info("Two cameras");
                    m = m.replace("{0}", "Падлучана дзьве камэры.");
                    break;
                default:
                    LOG.info("Many cameras");
                    throw new ServiceException("CHDK_TOO_MANY_CAMERAS");
                }

                m = m.replace("{1}",
                        oneBus ? "Камэры падлучаныя да адной USB шыны. Гэта зьменьшыць хуткасьць сканаваньня старонак недзе на сэкунду."
                                : "");
                m = m.replace("{2}", differentCameras
                        ? "Розныя мадэлі камэр. Вельмі пажадана выкарыстоўваць аднолькавыя камэры." : "");

                dev = new CHDKCameras(cameras);
                dev.connect();

                showLastStep(m);
            } catch (ServiceException ex) {
                String m = Messages.getFile(getPackagePath() + "/instruction-error.html");
                m = m.replace("{0}", ex.getMessage());
                showLastStep(m);
            } catch (Throwable ex) {
                LOG.error("Error find camera", ex);
                String m = Messages.getFile(getPackagePath() + "/instruction-error.html");
                m = m.replace("{0}", ex.getClass().getSimpleName() + ": " + ex.getMessage());
                showLastStep(m);
            }
        }
    };

    @Override
    protected void done() {
        DataStorage.device = dev;
        DataStorage.focused = false;
    }

    @Override
    public void cancel() {
        dispose();
    }
}
