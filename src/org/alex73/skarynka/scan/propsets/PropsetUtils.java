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
package org.alex73.skarynka.scan.propsets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * Util for load propset values from propsetN.lua file.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class PropsetUtils {
    static final Pattern RE_PROP = Pattern.compile("\\s*([A-Z][A-Z_0-9]*)\\s*=\\s*([0-9]+)\\s*,.*");

    public static Map<String, Integer> getPropset(int propsetNumber) throws IOException {
        Map<String, Integer> result = new TreeMap<>();
        try (InputStream in = PropsetUtils.class.getResourceAsStream("propset" + propsetNumber + ".lua")) {
            for (String line : IOUtils.readLines(in, "UTF-8")) {
                Matcher m = RE_PROP.matcher(line);
                if (m.matches()) {
                    result.put(m.group(1), Integer.parseInt(m.group(2)));
                }
            }
        }
        return result;
    }
}
