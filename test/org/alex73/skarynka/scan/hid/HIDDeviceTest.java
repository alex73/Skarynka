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

import javax.usb.util.UsbUtil;

import org.alex73.skarynka.scan.hid.HIDScanController;

public class HIDDeviceTest {

    public static void main(String[] args) throws Exception {
        HIDScanController p = new HIDScanController("093a:2510", null);

        byte[] buffer = new byte[UsbUtil
                .unsignedInt(p.usbPipe.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];

        while (true) {
            int length = p.usbPipe.syncSubmit(buffer);
            if (length > 0 && buffer[0] > 0) {
                System.out.println("HID Scan button #" + buffer[0]);
                for(int i=0;i<length;i++) {
                    System.out.print(Integer.toHexString(buffer[i])+" ");
                }
                System.out.println();
            }
        }
    }
}
