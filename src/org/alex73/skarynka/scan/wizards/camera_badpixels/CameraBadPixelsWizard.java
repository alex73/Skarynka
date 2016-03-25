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
package org.alex73.skarynka.scan.wizards.camera_badpixels;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.camera.CameraFactory;
import org.alex73.chdkptpj.lua.ChdkPtpJ;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.DataStorage;
import org.alex73.skarynka.scan.Messages;
import org.alex73.skarynka.scan.ServiceException;
import org.alex73.skarynka.scan.wizards.Wizard;
import org.apache.commons.io.IOUtils;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bad/hot pixels detection wizard.
 * 
 * By https://www.cybercom.net/~dcoffin/dcraw/.badpixels: Always use "dcraw -d -j -t 0" when locating bad
 * pixels!! Format is: pixel column, pixel row, UNIX time of death
 * 
 * By http://chdk.wikia.com/wiki/CHDK_1.3.0_User_Manual: To create this file, you will need to capture a
 * "dark frame" image by shooting an image with the lens completely capped. For shutter speeds longer than 2
 * seconds, you may want to keep a collection of "dark frame" image on hand for each exposure length that you
 * will be using in the future, as more warm and hot-pixels appear with extended shutter speeds.
 */
public class CameraBadPixelsWizard extends Wizard {
    private static Logger LOG = LoggerFactory.getLogger(CameraBadPixelsWizard.class);

    private Camera camera;
    private ChdkPtpJ lua;
    private StringBuilder out = new StringBuilder();
    private long date = System.currentTimeMillis();

    public CameraBadPixelsWizard() {
        super(Messages.getString("CAMERA_BADPIXELS_TITLE"));

        addImage("file://camera-on.png", getPackagePath() + "/camera-on.png");
        addImage("file://camera-power.png", getPackagePath() + "/camera-power.png");
        addImage("file://camera-usb.png", getPackagePath() + "/camera-usb.png");
    }

    public void process() {
        if (DataStorage.device != null) {
            DataStorage.device.close();
            DataStorage.device = null;
        }

        showStep(Messages.getFile(getPackagePath() + "/instruction-connect.html"),
                Messages.getString("CAMERA_INIT_PROCESS"), scanIntro);
    }

    Runnable scanIntro = new Runnable() {
        @Override
        public void run() {
            try {
                List<Camera> cameras = CameraFactory.findCameras();

                switch (cameras.size()) {
                case 0:
                    LOG.info("No cameras");
                    throw new ServiceException("CHDK_NO_CAMERAS");
                case 1:
                    LOG.info("One camera");
                    camera = cameras.get(0);
                    break;
                default:
                    LOG.info("Many cameras");
                    throw new ServiceException("CHDK_TOO_MANY_CAMERAS");
                }
                camera.connect();
                camera.setRecordMode();
                lua = new ChdkPtpJ(camera, "lua-orig");

                showStep(Messages.getFile(getPackagePath() + "/instruction-black.html"),
                        Messages.getString("CAMERA_BADPIXELS_PROCESS"), scanBlack);

            } catch (ServiceException ex) {
                String m = Messages.getFile(getPackagePath() + "/instruction-connect-error.html");
                m = m.replace("{0}", ex.getMessage());
                showLastStep(m);
            } catch (Throwable ex) {
                LOG.error("Error find cameras", ex);
                String m = Messages.getFile(getPackagePath() + "/instruction-connect-error.html");
                m = m.replace("{0}", ex.getClass().getSimpleName() + ": " + ex.getMessage());
                showLastStep(m);
            }
        }
    };

    Runnable scanBlack = new Runnable() {
        @Override
        public void run() {
            String path = Context.getBookDir();
            try {
                LuaTable p = new LuaTable();
                p.set("jpg", LuaValue.valueOf(false));
                p.set("raw", LuaValue.valueOf(true));
                p.set("tv", LuaValue.valueOf(1));
                p.set("sd", LuaValue.valueOf("200mm"));

                shoot(p, path + "/black1");
                shoot(p, path + "/black2");
                shoot(p, path + "/black3");

                showStep(Messages.getFile(getPackagePath() + "/instruction-white.html"),
                        Messages.getString("CAMERA_BADPIXELS_PROCESS"), scanWhite);
            } catch (Exception ex) {
                LOG.warn("Error scan for badpixels", ex);
                String m = Messages.getFile(getPackagePath() + "/instruction-error.html");
                m = m.replace("{0}", ex.getClass().getSimpleName() + ": " + ex.getMessage());
                showLastStep(m);
            }
        }
    };

    Runnable scanWhite = new Runnable() {
        @Override
        public void run() {
            String path = Context.getBookDir();
            try {
                LuaTable p = new LuaTable();
                p.set("jpg", LuaValue.valueOf(false));
                p.set("raw", LuaValue.valueOf(true));
                p.set("tv", LuaValue.valueOf(5));
                p.set("sd", LuaValue.valueOf("200mm"));

                shoot(p, path + "/white1");
                shoot(p, path + "/white2");
                shoot(p, path + "/white3");

                finish();
            } catch (Exception ex) {
                LOG.warn("Error scan for badpixels", ex);
                String m = Messages.getFile(getPackagePath() + "/instruction-error.html");
                m = m.replace("{0}", ex.getClass().getSimpleName() + ": " + ex.getMessage());
                showLastStep(m);
            }
        }
    };

    void finish() {
        try {
            String badBlack = calc(128, 256, read( "black1.raw"), read( "black2.raw"),
                    read( "black3.raw"));
            String badWhite = calc(0, 128, read( "white1.raw"), read( "white2.raw"),
                    read( "white3.raw"));

            if (badBlack == null || badWhite == null) {
                String m = Messages.getFile(getPackagePath() + "/instruction-error.html");
                m = m.replace("{0}", out.toString());
                showLastStep(m);
            } else {
                out.setLength(0);
                out.append(Messages.getString("CAMERA_BADPIXELS_RESULT",
                        camera.getDevice().getSerialNumberString().trim())).append("\n");
                out.append("=======================================\n");
                out.append("# hot pixels from black image\n");
                out.append(badBlack);
                out.append("# bad pixels from white image\n");
                out.append(badWhite);

                String m = Messages.getFile(getPackagePath() + "/instruction-done.html");
                m = m.replace("{0}", out.toString());
                showLastStep(m);
            }
        } catch (Throwable ex) {
            LOG.warn("Error scan for badpixels", ex);
            String m = Messages.getFile(getPackagePath() + "/instruction-error.html");
            m = m.replace("{0}", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            showLastStep(m);
        }
    }

    @Override
    protected void done() {

    }

    @Override
    public void cancel() {
        dispose();
    }

    void shoot(LuaTable p, String file) throws Exception {
        p.set(1, LuaValue.valueOf(file));
        Varargs r = lua.executeCliFunction("remoteshoot", p);
        r.arg1().checkboolean();
        if (!((LuaBoolean) r.arg1()).booleanValue()) {
            throw new Exception(r.arg(2).tojstring());
        }
    }

    BufferedImage read(String file) throws Exception {
        out.append("\n");
        out.append(Messages.getString("CAMERA_BADPIXELS_ANALYZE", file)).append("\n");

        String fileBMP = file.replaceAll("\\.[a-zA-Z0-9]{3}", ".bmp");
       String cmdo= Context.getSettings().get("path_dcraw")+" -c -d -j -t 0 -W "+ file+" | "+Context.getSettings().get("path_convert")+" - "+fileBMP;
       
        String[] cmda;
        switch (Context.thisOS) {
        case LINUX:
            cmda = new String[] { "nice", "ionice", "-c3", "sh", "-c", cmdo.toString() };
            break;
        case WINDOWS:
            cmda = new String[] { "nice", "cmd.exe", "/c", cmdo.toString() };
            break;
        default:
            throw new Exception("Unknown OS");
        }
        LOG.debug("Execute : " + cmdo);
        Process process = Runtime.getRuntime().exec(cmda, null, new File(Context.getBookDir()));
        int r = process.waitFor();
        LOG.debug("Execution result: " + r);
        if (r != 0) {
            String err = IOUtils.toString(process.getErrorStream(),
                    Context.getSettings().get("command_charset"));
            throw new Exception("Error execution : " + r + " / " + err);
        }

        return ImageIO.read(new File(Context.getBookDir(), fileBMP));
    }

    static final int THRESHOLD = 128;

    String calc(int cFrom, int cTo, BufferedImage... images) {
        for (int i = 1; i < images.length; i++) {
            if (images[i].getWidth() != images[0].getWidth()
                    || images[i].getHeight() != images[0].getHeight()) {
                throw new RuntimeException("Wrong images size");
            }
        }
        StringBuilder dump = new StringBuilder();
        int count = 0;
        for (int x = 0; x < images[0].getWidth(); x++) {
            for (int y = 0; y < images[0].getHeight(); y++) {
                boolean bad = false;
                for (int i = 0; i < images.length; i++) {
                    int rgb = images[i].getRGB(x, y);
                    int r = rgb & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = (rgb >> 16) & 0xff;
                    int c = (r + g + b) / 3;
                    if (c >= cFrom && c <= cTo) {
                        bad = true;
                        count++;
                        break;
                    }
                }

                if (bad && count <= 2000) {
                    dump.append("  " + coord(x) + " " + coord(y) + "  " + date + " #");
                    for (int i = 0; i < images.length; i++) {
                        int rgb = images[i].getRGB(x, y);
                        int r = rgb & 0xff;
                        int g = (rgb >> 8) & 0xff;
                        int b = (rgb >> 16) & 0xff;
                        int c = (r + g + b) / 3;
                        dump.append(" " + c);
                    }
                    dump.append("\n");
                }
            }
        }
        if (count > 2000) {
            // more than 0.1%
            out.append(Messages.getString("CAMERA_BADPIXELS_TOO_MANY", count)).append("\n");
            return null;
        }
        return dump.toString();
    }

    String coord(int p) {
        String r = Integer.toString(p);
        return "      ".substring(r.length()) + r;
    }
}
