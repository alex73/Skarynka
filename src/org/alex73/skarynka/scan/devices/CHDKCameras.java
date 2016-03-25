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
package org.alex73.skarynka.scan.devices;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.alex73.UsbUtils;
import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.lua.ChdkPtpJ;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.ServiceException;
import org.alex73.skarynka.scan.common.ImageViewPane;
import org.alex73.skarynka.scan.devices.CameraWorker.CameraCommand;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CHDK camera driver.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class CHDKCameras implements ISourceDevice {
    private static Logger LOG = LoggerFactory.getLogger(CHDKCameras.class);

    private final CameraWorker worker;
    private boolean focused;
    private int[] rotations;
    private int zoom;
    private final Properties props;
    private final int imageWidth, imageHeight;

    public CHDKCameras(List<Camera> cameras) throws Exception {
        worker = new CameraWorker(cameras);

        for (Camera c : cameras) {
            String vendorid = UsbUtils.hex4(c.getDevice().getUsbDeviceDescriptor().idVendor());
            String productid = UsbUtils.hex4(c.getDevice().getUsbDeviceDescriptor().idProduct());
            String required = Context.getSettings().get("cameras-usbid");
            if (required != null) {
                if (!required.equals(vendorid + ":" + productid)) {
                    throw new ServiceException("CAMERA_WRONG_ID", required, vendorid + ":" + productid);
                }
            }
        }
        props = new Properties();
        try (InputStream in = new FileInputStream(Context.getSettings().get("cameras-settings"))) {
            props.load(in);
        }
        String imgWidth = props.getProperty("image.width");
        String imgHeight = props.getProperty("image.height");
        if (imgWidth == null) {
            throw new Exception("image.width is not defined");
        }
        if (imgHeight == null) {
            throw new Exception("image.height is not defined");
        }
        imageWidth = Integer.parseInt(imgWidth);
        imageHeight = Integer.parseInt(imgHeight);
    }

    @Override
    public boolean readyForScan() {
        return focused;
    }

    @Override
    public String[] scan(String... pathsToOutput) throws Exception {
        LOG.info("Scan to " + Arrays.toString(pathsToOutput));
        long be=System.currentTimeMillis();
        worker.exec(new CameraCommand() {
            @Override
            public void exec(int workerIndex, Camera camera, ChdkPtpJ lua) throws Exception {
                String path = pathsToOutput[workerIndex];
                if (path == null) {
                    return;
                }
                try {
                    LuaTable p = new LuaTable();
                    p.set("jpg", LuaValue.valueOf(true));
                    p.set("raw", LuaValue.valueOf(true));
                    p.set(1, LuaValue.valueOf(path));
                    lua.executeCliFunction("remoteshoot", p);
                } catch (Throwable ex) {
                    new File(path + ".raw").delete();
                    new File(path + ".jpg").delete();
                    throw ex;
                }
            }
        });
        long af=System.currentTimeMillis();
        LOG.info("Scan time: "+(af-be)+"ms");
        return worker.getCamerasIds(props);
    }

    @Override
    public String[] getStatus() throws Exception {
        LOG.trace("getStatus()");
        String[] result = new String[worker.getCamerasCount()];
        worker.exec(new CameraCommand() {
            @Override
            public void exec(int workerIndex, Camera camera, ChdkPtpJ lua) throws Exception {
                Object tOpt = camera.executeLua("get_temperature(0)");
                Object tSens = camera.executeLua("get_temperature(1)");

                result[workerIndex] = Messages.getString("CAMERA_INFO", tOpt, tSens);
            }
        });
        return result;
    }

    public void connect() throws Exception {
        LOG.info("Connect to cameras...");

        worker.exec(new CameraCommand() {
            @Override
            public void exec(Camera camera) throws Exception {
                camera.setRecordMode();

                StringBuilder s = new StringBuilder();
                s.append("call_event_proc('UI.CreatePublic')\n");

                for (Map.Entry<String, String> en : (Set<Map.Entry<String, String>>) (Set) props.entrySet()) {
                    if (en.getKey().startsWith("prop.")) {
                        String k = en.getKey().substring(5);
                        if (en.getValue().trim().startsWith("-")) {
                            // skip values less than zero - it's impossible to set they
                            continue;
                        }
                        s.append("if call_event_proc('PTM_SetCurrentItem'," + k + "," + en.getValue()
                                + ") ~= 0 then\n");
                        s.append("  error('Error set " + k + "')\n");
                        s.append("end\n");
                    }
                }
                for (Map.Entry<String, String> en : (Set<Map.Entry<String, String>>) (Set) props.entrySet()) {
                    if (en.getKey().startsWith("prop.")) {
                        String k = en.getKey().substring(5);
                        String v = en.getValue().trim();
                        if (v.startsWith("-")) {
                            v = Integer.toString(65536 + Integer.parseInt(v));
                        }
                        s.append("if call_event_proc('PTM_GetCurrentItem'," + k + ") ~= " + v + " then\n");
                        s.append("  error('Wrong value in the " + k + ": expected is " + v
                                + " but really is '..call_event_proc('PTM_GetCurrentItem'," + k + "))\n");
                        s.append("end\n");
                    }
                }
                camera.executeLua(s.toString());
            }
        });
    }

    @Override
    public void close() {
        LOG.trace("disconnect()");
        try {
            worker.exec(new CameraCommand() {
                @Override
                public void exec(Camera camera) {
                    try {
                        camera.disconnect();
                    } catch (Exception ex) {
                        LOG.warn("Error disconnect from camera", ex);
                    }
                }
            });
        } catch (Exception ex) {
        }
    }

    @Override
    public int getZoom() {
        return zoom;
    }

    @Override
    public Dimension[] getImageSize() {
        Dimension[] sizes = new Dimension[2];
        sizes[0] = new Dimension(imageWidth, imageHeight);
        sizes[1] = new Dimension(imageWidth, imageHeight);
        return sizes;
    }

    public int getCurrentZoom() throws Exception {
        LOG.trace("getZoom()");
        AtomicInteger result = new AtomicInteger();
        worker.exec(new CameraCommand() {
            @Override
            public void exec(Camera camera) throws Exception {
                int zoom = camera.getScripting().get_zoom();
                result.set(zoom);
            }
        });
        return result.get();
    }

    public void setZoom(int zoom) throws Exception {
        LOG.trace("setZoom()");
        this.zoom = zoom;
        worker.exec(new CameraCommand() {
            @Override
            public void exec(Camera camera) throws Exception {
                camera.getScripting().set_zoom(zoom);
            }
        });
    }

    public int getMaxZoom() throws Exception {
        LOG.trace("getMaxZoom()");
        AtomicInteger result = new AtomicInteger();
        worker.exec(new CameraCommand() {
            @Override
            public void exec(Camera camera) throws Exception {
                int maxZoom = camera.getScripting().get_zoom_steps() - 1;
                result.set(maxZoom);
            }
        });
        return result.get();
    }

    public int[] getRotations() {
        return rotations;
    }

    public void setRotations(int... rotations) {
        this.rotations = rotations;
    }

    public List<Integer> focus() throws Exception {
        LOG.info("Focus cameras...");

        List<Integer> result = Collections.synchronizedList(new ArrayList<>());

        worker.exec(new CameraCommand() {
            @Override
            public void exec(Camera camera) throws Exception {
                LOG.info("SdOverMode = " + camera.getScripting().get_sd_over_modes());

                String manualFocusDistance = Context.getSettings().get("cameras-manual-focus-distance");
                if (manualFocusDistance != null) {
                    int distance = Integer.parseInt(manualFocusDistance);
                    camera.executeLua("set_aflock(0); set_focus(" + distance
                            + ");  sleep(500); press (\"shoot_half\"); sleep(1000); set_aflock(1); release (\"shoot_half\"); sleep(500);");
                } else {
                    // autofocus lock
                    camera.executeLua(
                            "set_aflock(0); press (\"shoot_half\"); sleep(5000); set_aflock(1); release (\"shoot_half\");");
                }

                LOG.info("Camera " + camera.getDevice().getSerialNumberString() + " focused");
                LOG.info("FocusMode = " + camera.getScripting().get_focus_mode());
                LOG.info("FocusDistance(mm) = " + camera.getScripting().get_focus());
                LOG.info("FocusState = " + camera.getScripting().get_focus_state());
                LOG.info("DofInfo :\n" + camera.getScripting().get_dofinfo());
                result.add(camera.getScripting().get_focus());
            }
        });
        focused = true;
        LOG.info("Cameras focused on " + result);
        return result;
    }

    public boolean[] setPreviewPanels(ImageViewPane... panels) {
        return worker.setPreviewPanels(panels);
    }

    public void swap() {
        worker.swap();
    }

    public Properties getCamerasProperties() {
        return props;
    }

    // public void o() {
    // System.out.println("get_av96 = " + cam.executeLuaQuery("return get_av96();"));
    // System.out.println("get_bv96 = " + cam.executeLuaQuery("return get_bv96();"));
    // System.out.println("get_ev = " + cam.executeLuaQuery("return get_ev();"));
    // System.out.println("get_iso_mode = " + cam.executeLuaQuery("return get_iso_mode();"));
    // // System.out.println("get_live_histo = "+cam.executeLuaQuery("return get_live_histo ();"));
    //
    // System.out.println("Temp(optical) = " + cam.executeLuaQuery("return get_temperature(0);"));
    // System.out.println("Temp(CCD) = " + cam.executeLuaQuery("return get_temperature(1);"));
    // System.out.println("Temp(battery) = " + cam.executeLuaQuery("return get_temperature(2);"));
    //
    // System.out.println("get_free_disk_space(MiB) = "
    // + ((int) cam.executeLuaQuery("return get_free_disk_space();") / 1024));
    //
    // cam.executeLuaQuery("return shoot();");
    // }
}
