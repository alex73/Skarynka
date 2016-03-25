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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.lua.ChdkPtpJ;
import org.alex73.skarynka.scan.common.ImageViewPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for external scan devices. It allows parallel scan from multiple devices, send common commands,
 * etc.
 * 
 * Parallel execution required for scan with more than one cameras faster. For example, if one camera requires
 * 2 seconds for scanning and transfer data, then two cameras will require 4 seconds if they will not work in
 * parallel. It's too much for massive scanning. Instead, all commands will be transferred to cameras in
 * parallel. There is one possible issue: most time required for transfer data from camera to computer. If
 * cameras connected to the one USB hub, they will transfer data like successive. But even in this case you
 * will see a little speed improvement.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class CameraWorker {
    private static Logger LOG = LoggerFactory.getLogger(CameraWorker.class);

    private static final int CHDK_ASPECT_WIDTH = 1;
    private static final int CHDK_ASPECT_HEIGHT = 2;

    private final List<CameraThread> threads = new ArrayList<>();

    public CameraWorker(Camera... cameras) throws Exception {
        int i = 1;
        for (Camera c : cameras) {
            if (c != null) {
                CameraThread th = new CameraThread(i - 1, c, "Camera " + i);
                i++;
                th.start();
                threads.add(th);
            }
        }
    }

    public CameraWorker(List<Camera> cameras) throws Exception {
        int i = 1;
        for (Camera c : cameras) {
            if (c != null) {
                CameraThread th = new CameraThread(i - 1, c, "Camera " + i);
                i++;
                th.start();
                threads.add(th);
            }
        }
    }

    public boolean[] setPreviewPanels(ImageViewPane... panels) {
        boolean[] r = new boolean[panels.length];
        for (int i = 0; i < Math.min(threads.size(), panels.length); i++) {
            threads.get(i).previewPanel = panels[i];
            r[i] = true;
        }
        for (int i = panels.length; i < threads.size(); i++) {
            threads.get(i).previewPanel = null;
        }
        return r;
    }

    public void swap() {
        if (threads.size() == 2) {
            ImageViewPane p0 = threads.get(0).previewPanel;
            ImageViewPane p1 = threads.get(1).previewPanel;
            Collections.reverse(threads);
            setPreviewPanels(p0, p1);
            threads.get(0).workerIndex = 0;
            threads.get(1).workerIndex = 1;
        }
    }

    public int getCamerasCount() {
        return threads.size();
    }

    public String[] getCamerasIds(Properties props) {
        String[] result = new String[threads.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = threads.get(i).getSerial();
        }
        return result;
    }

    public void exec(CameraCommand command) throws Exception {
        final AtomicInteger countForWait = new AtomicInteger(threads.size());
        AtomicReference<Exception> error = new AtomicReference<Exception>();
        // send command to all cameras
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).exec(new CameraCommand() {
                @Override
                void exec(int workerIndex, Camera camera, ChdkPtpJ lua) throws Exception {
                    try {
                        command.exec(workerIndex, camera, lua);
                    } catch (Exception ex) {
                        LOG.warn("Error execute camera command", ex);
                        error.set(ex);
                    } catch (Throwable ex) {
                        LOG.warn("Error execute camera command", ex);
                        error.set(new Exception(ex));
                    }
                    int c = countForWait.decrementAndGet();
                    if (c == 0) {
                        synchronized (command) {
                            command.notifyAll();
                        }
                    }
                }

                @Override
                public void exec(Camera camera) {
                    try {
                        command.exec(camera);
                    } catch (Exception ex) {
                        LOG.warn("Error execute camera command", ex);
                        error.set(ex);
                    } catch (Throwable ex) {
                        LOG.warn("Error execute camera command", ex);
                        error.set(new Exception(ex));
                    }
                    int c = countForWait.decrementAndGet();
                    if (c == 0) {
                        synchronized (command) {
                            command.notifyAll();
                        }
                    }
                }
            });
        }

        // wait for finish all commands
        try {
            synchronized (command) {
                while (countForWait.get() > 0) {
                    command.wait(10000);
                }
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }

        // check for exception
        if (error.get() != null) {
            throw error.get();
        }
    }

    public static class CameraCommand {
        void exec(int workerIndex, Camera camera, ChdkPtpJ lua) throws Exception {
            exec(camera);
        }

        void exec(Camera camera) throws Exception {
        }
    }

    public static class CameraThread extends Thread {
        private final Camera camera;
        private final String serial;
        private final String threadName;
        private int workerIndex;
        protected ImageViewPane previewPanel;
        private LinkedList<CameraCommand> queue = new LinkedList<>();
        private ChdkPtpJ lua;

        public CameraThread(int workerIndex, Camera camera, String threadName) throws Exception {
            this.camera = camera;
            this.serial = camera.getDevice().getSerialNumberString().trim();
            this.threadName = threadName;
            this.workerIndex = workerIndex;

            camera.connect();
            lua = new ChdkPtpJ(camera, "lua-orig");
        }

        @Override
        public void run() {
            Thread.currentThread().setName(threadName);
            try {
                while (true) {
                    CameraCommand cmd;
                    synchronized (queue) {
                        cmd = queue.poll();
                    }
                    if (cmd != null) {
                        // command ready for execute
                        processCommand(cmd);
                    } else {
                        // no command - show preview if need
                        processDefault();
                        // wait for next command
                        synchronized (queue) {
                            if (queue.isEmpty()) {
                                queue.wait(100);
                            }
                        }
                    }
                }
            } catch (InterruptedException ex) {
            }
        }

        public void exec(CameraCommand command) {
            synchronized (queue) {
                queue.add(command);
                queue.notifyAll();
            }
        }

        protected void processCommand(CameraCommand cmd) {
            try {
                cmd.exec(workerIndex, camera, lua);
            } catch (Throwable ex) {
                LOG.warn("Error execute camera command", ex);
            }
        }

        protected void processDefault() {
            ImageViewPane p = previewPanel;
            if (p != null) {
                try {
                    BufferedImage img = camera.getPreview();
                    p.displayImage(img, CHDK_ASPECT_WIDTH, CHDK_ASPECT_HEIGHT);
                } catch (Exception ex) {
                    LOG.info("Error retrieve preview", ex);
                }
            }
        }

        public String getSerial() {
            return serial;
        }
    }
}
