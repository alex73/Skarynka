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
package org.alex73.skarynka.scan.hid;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.usb.UsbConst;
import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbInterface;
import javax.usb.UsbPipe;
import javax.usb.util.UsbUtil;

import org.alex73.UsbUtils;
import org.alex73.skarynka.scan.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver for HID (mouse or pedal) devices for initiate scan.
 * 
 * usb4java works with HID devices not so good. Usually, it throws exception after several usages. Need to
 * change to usb HID implementation or find other way. Before that, 'hidscan-keys' parameter usage
 * recommended.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class HIDScanController extends Thread {
    private static Logger LOG = LoggerFactory.getLogger(HIDScanController.class);

    protected UsbPipe usbPipe;
    protected String hidDeviceUsbId;
    protected ScanListener testListener;
    private long lastSendTime;
    private boolean finished;

    public HIDScanController(String hidDeviceUsbId, String interfaceProtocol) {
        install(hidDeviceUsbId, interfaceProtocol);
    }

    public boolean isConnected() {
        return usbPipe != null;
    }

    public String getHIDDeviceUsbId() {
        return hidDeviceUsbId;
    }

    public void setTestListener(ScanListener testListener) {
        this.testListener = testListener;
    }

    @Override
    public void run() {
        if (usbPipe == null) {
            return;
        }
        byte[] buffer = new byte[UsbUtil
                .unsignedInt(usbPipe.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];

        while (!finished) {
            try {
                Arrays.fill(buffer, (byte) 0);
                int length = usbPipe.syncSubmit(buffer);
                if (length > 0) {
                    if (lastSendTime + 1000 > System.currentTimeMillis()) {
                        // one-second delay before next sending. Used because some definces sends many equals
                        // packets
                        continue;
                    }
                    lastSendTime = System.currentTimeMillis();
                    if (testListener != null) {
                        displayKey(buffer);
                    } else {
                        sendKey(buffer);
                    }
                }
            } catch (Throwable ex) {
                LOG.error("Unable to receive data from HID device : ", ex);
            }
        }
    }

    private void displayKey(byte[] buffer) throws Exception {
        String k = getConfigKey(buffer);

        String button = Context.getSettings().get(k);
        if (button == null) {
            button = "<" + k.replace("hidscan-button.", "") + ">";
        }
        testListener.pressed(button);
    }

    private void sendKey(byte[] buffer) {
        // Get key code from config.xml by data buffer.
        String configKey = getConfigKey(buffer);
        String key = Context.getSettings().get(configKey);
        int keyCode = getKeyCode(key);
        if (keyCode != 0) {
            LOG.trace("HID device keyCode " + keyCode);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        Robot robot = new Robot();
                        robot.keyPress(keyCode);
                        robot.keyRelease(keyCode);
                    } catch (Throwable ex) {
                        LOG.error("Can't send key to application : ", ex);
                    }
                }
            });
        }
    }

    /**
     * Convert 'VK_..' key name into int value.
     */
    public static int getKeyCode(String key) {
        try {
            if (key == null) {
                return 0;
            }
            Field f = KeyEvent.class.getField(key);
            if (f == null) {
                LOG.error("Field " + key + " not defined");
                return 0;
            }
            return f.getInt(KeyEvent.class);
        } catch (Throwable ex) {
            LOG.error("Can't get key : ", ex);
            return 0;
        }
    }

    private String getConfigKey(byte[] data) {
        StringBuilder s = new StringBuilder(100);
        s.append("hidscan-button");
        for (int i = 0; i < data.length; i++) {
            s.append('.').append(Integer.toHexString(data[i]));
        }
        return s.toString();
    }

    private void dumpInfo(String hidDeviceUsbId) {
        try {
            for (UsbDevice dev : UsbUtils.listAllUsbDevices()) {
                String usbId = UsbUtils.hex4(dev.getUsbDeviceDescriptor().idVendor()) + ':'
                        + UsbUtils.hex4(dev.getUsbDeviceDescriptor().idProduct());
                if (hidDeviceUsbId.equals(usbId)) {
                    LOG.info("Found USB device: " + hidDeviceUsbId);
                    for (UsbInterface uintf : (List<? extends UsbInterface>) dev.getActiveUsbConfiguration()
                            .getUsbInterfaces()) {
                        LOG.info("  Found USB interface: " + hidDeviceUsbId + " with protocol="
                                + uintf.getUsbInterfaceDescriptor().bInterfaceProtocol());
                        for (UsbEndpoint e : (List<UsbEndpoint>) uintf.getUsbEndpoints()) {
                            LOG.info("    Found USB endpoint with type=" + e.getType() + " and direction="
                                    + e.getDirection());
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            LOG.info("Error dump usb devices", ex);
        }
    }

    private void install(String hidDeviceUsbId, String interfaceProtocol) {
        dumpInfo(hidDeviceUsbId);
        UsbInterface u = null;
        try {
            UsbDevice d = null;
            for (UsbDevice dev : UsbUtils.listAllUsbDevices()) {
                String usbId = UsbUtils.hex4(dev.getUsbDeviceDescriptor().idVendor()) + ':'
                        + UsbUtils.hex4(dev.getUsbDeviceDescriptor().idProduct());
                if (hidDeviceUsbId.equals(usbId)) {
                    d = dev;
                    break;
                }
            }
            if (d == null) {
                LOG.info("There is no HID Scan defined device, driver will not be installed");
                return;
            }

            List<? extends UsbInterface> interfaces = d.getActiveUsbConfiguration().getUsbInterfaces();
            if (interfaces.isEmpty()) {
                LOG.info("HID Scan device doesn't contain interfaces, driver will not be installed");
                return;
            }

            for (UsbInterface uintf : interfaces) {
                u = uintf;
                if (interfaceProtocol != null) {
                    int proto = u.getUsbInterfaceDescriptor().bInterfaceProtocol();
                    if (!interfaceProtocol.equals(Integer.toString(proto))) {
                        continue;
                    }
                }
                u.claim();

                UsbEndpoint usbEndpoint = null;
                for (UsbEndpoint e : (List<UsbEndpoint>) u.getUsbEndpoints()) {
                    if (UsbConst.ENDPOINT_TYPE_INTERRUPT == e.getType()
                            && UsbConst.ENDPOINT_DIRECTION_IN == e.getDirection()) {
                        usbEndpoint = e;
                        break;
                    }
                }

                if (usbEndpoint == null) {
                    LOG.info("There is no endpoint in HID Scan, driver will not be installed");
                    return;
                }

                usbPipe = usbEndpoint.getUsbPipe();

                usbPipe.open();

                this.hidDeviceUsbId = hidDeviceUsbId;
            }
            if (usbPipe == null) {
                LOG.info("There is no interface in HID Scan, driver will not be installed");
                return;
            }
        } catch (Throwable ex) {
            LOG.info("Error install HID Scan driver", ex);
            usbPipe = null;
            if (u != null) {
                try {
                    u.release();
                } catch (Throwable e) {
                }
            }
        }
    }

    public void finish() {
        finished = true;
        if (usbPipe != null) {
            try {
                usbPipe.abortAllSubmissions();
            } catch (Throwable ex) {
            }
            try {
                usbPipe.close();
            } catch (Throwable ex) {
            }
        }
    }

    public interface ScanListener {
        void pressed(String key);
    }
}
