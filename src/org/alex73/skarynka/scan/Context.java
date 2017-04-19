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

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;

import org.alex73.skarynka.scan.xsd.Allow;
import org.alex73.skarynka.scan.xsd.Command;
import org.alex73.skarynka.scan.xsd.Config;
import org.alex73.skarynka.scan.xsd.OS;
import org.alex73.skarynka.scan.xsd.Param;
import org.alex73.skarynka.scan.xsd.Permissions;
import org.alex73.skarynka.scan.xsd.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage for common parameters and info.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class Context {
    private static Logger LOG = LoggerFactory.getLogger(Context.class);

    private static final JAXBContext CONTEXT;

    public static final OS thisOS;
    public static final String thisHost;

    private static Map<String, String> settings;
    private static Perm permissions;
    private static Map<String, String> pageTags;
    private static Map<String, String> processCommands;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(Config.class);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }

        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            thisOS = OS.WINDOWS;
        } else if (osName.startsWith("Linux")) {
            thisOS = OS.LINUX;
        } else {
            throw new ExceptionInInitializerError("Unknown OS");
        }

        thisHost = System.getProperty("HOST");
        if (thisHost == null) {
            throw new ExceptionInInitializerError("'HOST' was not defined");
        }
    }

    public static void load() throws Exception {
        Config config = (Config) CONTEXT.createUnmarshaller().unmarshal(new File("config.xml"));

        Map<String, String> newSettings = new HashMap<>();

        for (Param p : config.getSettings().getParam()) {
            if (check(p.getOs(), p.getHost())) {
                if (newSettings.put(p.getId(), p.getValue()) != null) {
                    throw new RuntimeException("Param '" + p.getId() + "' was already defined");
                }
            }
        }

        Perm newPermissions = null;
        for (Permissions p : config.getPermissions()) {
            if (check(p.getOs(), p.getHost())) {
                if (newPermissions != null) {
                    throw new RuntimeException("Permissions was already defined");
                }
                newPermissions = new Perm();
                for (Allow a : p.getAllow()) {
                    Field f = newPermissions.getClass().getField(a.getId());
                    f.setBoolean(newPermissions, a.isValue());
                }
            }
        }

        Map<String, String> newPageTags = new TreeMap<>();
        for (Tag t : config.getPageTags().getTag()) {
            if (newPageTags.put(t.getName(), t.getTitle()) != null) {
                throw new RuntimeException("Page tag '" + t.getName() + "' defined twice");
            }
        }

        Map<String, String> newProcessCommands = new TreeMap<>();
        for (Command t : config.getProcessCommands().getCommand()) {
            if (newProcessCommands.put(t.getName(), t.getTitle()) != null) {
                throw new RuntimeException("Process command '" + t.getName() + "' defined twice");
            }
        }

        settings = Collections.unmodifiableMap(newSettings);
        permissions = newPermissions;
        pageTags = Collections.unmodifiableMap(newPageTags);
        processCommands = Collections.unmodifiableMap(newProcessCommands);
    }

    static boolean check(OS os, String host) {
        if (os != null && !thisOS.equals(os)) {
            return false;
        }
        if (host != null && !thisHost.equals(host)) {
            return false;
        }
        return true;
    }

    public static String getBookDir() {
        return settings.get("book-dir");
    }

    public static String getControlDir() {
        return settings.get("control-dir");
    }

    public static Map<String, String> getSettings() {
        return settings;
    }

    public static Perm getPermissions() {
        return permissions;
    }

    public static Map<String, String> getPageTags() {
        return pageTags;
    }

    public static Map<String, String> getProcessCommands() {
        return processCommands;
    }

    public static class Perm {
        public boolean CameraBadPixels, ProcessingBooks, ProcessingControls, ShowNonLocalBooks, BookControl, ShowDevices, ShowManualAdd;
    }

    public static class ScriptStorage {
        public String name;
        public String language;
        public String script;
    }
}
