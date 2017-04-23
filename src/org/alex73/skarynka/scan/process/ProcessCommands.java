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
package org.alex73.skarynka.scan.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessCommands {
	private static Logger LOG = LoggerFactory.getLogger(ProcessCommands.class);

	static final Pattern DO_LS = Pattern.compile("ls\\s+(.+)");
	static final Pattern DO_DF = Pattern.compile("df\\s+(.+)");
	static final Pattern DO_RM = Pattern.compile("rm\\s+(.+)");
	static final Pattern DO_CP = Pattern.compile("cp\\s+(.+)\\s+(.+)");
	static final Pattern DO_MVDIR = Pattern.compile("mvdir\\s+(.+)\\s+(.+)");
	static final Pattern DO_SCANS = Pattern.compile("scans");
	static final Pattern DO_EXEC = Pattern.compile("exec\\s+(.+)");

	public static String call(String s) throws Exception {
		LOG.info("Parse command: " + s);
		StringBuilder out = new StringBuilder();
		Matcher m;
		if ((m = DO_LS.matcher(s)).matches()) {
			LOG.info("Command ls " + m.group(1));
			List<File> files = new ArrayList<>(FileUtils.listFiles(new File(m.group(1)), null, true));
			Collections.sort(files);
			out.append(s + ":\n");
			for (File f : files) {
				out.append("        " + f.getPath() + "  " + f.length() + "\n");
			}
		} else if ((m = DO_DF.matcher(s)).matches()) {
			LOG.info("Command df " + m.group(1));
			File f = new File(m.group(1));
			long gb = f.getFreeSpace() / 1024 / 1024 / 1024;
			out.append(s + ": " + gb + " gb\n");
		} else if ((m = DO_RM.matcher(s)).matches()) {
			LOG.info("Command rm " + m.group(1));
			File f = new File(m.group(1));
			if (f.isDirectory()) {
				FileUtils.deleteDirectory(f);
			} else {
				f.delete();
			}
		} else if ((m = DO_CP.matcher(s)).matches()) {
			LOG.info("Command cp " + m.group(1) + " " + m.group(2));
			FileUtils.copyFile(new File(m.group(1)), new File(m.group(2)));
		} else if ((m = DO_MVDIR.matcher(s)).matches()) {
			LOG.info("Command mvdir " + m.group(1) + " " + m.group(2));
			FileUtils.moveDirectoryToDirectory(new File(m.group(1)), new File(m.group(2)), true);
		} else {
			throw new Exception("Unknown command: " + s);
		}
		return out.toString();
	}
}
