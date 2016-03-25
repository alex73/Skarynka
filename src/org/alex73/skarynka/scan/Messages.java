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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.apache.commons.io.IOUtils;

/**
 * Message bundle handler.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class Messages {
    static ResourceBundle messages;

    static {
        messages = ResourceBundle.getBundle("org.alex73.skarynka.scan.messages", new UTF8Control());
    }

    public static String getString(String key) {
        return messages.getString(key);
    }

    public static String getString(String key, Object... param) {
        String pattern = messages.getString(key);
        return MessageFormat.format(pattern, param);
    }

    public static String getFile(String name) {
        Locale loc1 = Locale.getDefault();
        Locale loc2 = new Locale(loc1.getLanguage(), loc1.getCountry());
        Locale loc3 = new Locale(loc1.getLanguage());
        for (Locale loc : new Locale[] { loc1, loc2, loc3 }) {
            URL r = Messages.class.getResource(name.replaceAll("(\\.[a-zA-Z0-9]+)$", "_" + loc + "$1"));
            if (r != null) {
                try {
                    return IOUtils.toString(r, "UTF-8");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        URL r = Messages.class.getResource(name);
        if (r != null) {
            try {
                return IOUtils.toString(r, "UTF-8");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    public static class UTF8Control extends Control {
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            // The below is a copy of the default implementation.
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    // Only this line is changed to make it to read properties files as UTF-8.
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
