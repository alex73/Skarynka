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

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.alex73.skarynka.scan.process.PageFileInfo;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Book info representation. This info stored in the <book>/book.properties file.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class Book2 {
    public static final String[] IMAGE_EXTENSIONS = new String[] { "tif", "tiff", "jpg", "jpeg", "png", "bmp" };

    private static Logger LOG = LoggerFactory.getLogger(Book2.class);

    static final Pattern RE_BOOK = Pattern.compile("book.([a-zA-Z]+)=(.+)");
    static final Pattern RE_PAGE = Pattern.compile("page.([0-9a-z]+).([a-zA-z]+)=(.+)");

    public class PageInfo {
        public String pageNumber;
        public int imageSizeX, imageSizeY;
        public int cropPosX = -1, cropPosY = -1;
        public Set<String> tags = new TreeSet<>();
        public int rotate;
        public String camera;
        public String pageOriginalFileExt;

        public PageInfo(String pageNumber) {
            this.pageNumber = pageNumber;
            for (String ext : Book2.IMAGE_EXTENSIONS) {
                File f = new File(bookDir, pageNumber + '.' + ext);
                if (f.exists()) {
                    pageOriginalFileExt = ext;
                    break;
                }
            }
            if (pageOriginalFileExt == null) {
                throw new RuntimeException("Page " + pageNumber + " file not found");
            }
        }
    }

    private final File bookDir, bookFile;
    public transient boolean local;
    private transient String localFor;

    public int scale = 100, dpi = 100;
    public int zoom;
    public int cropSizeX = -1, cropSizeY = -1;
    public int pageStep = 2;

    private Map<String, PageInfo> pages = new HashMap<>();
    private List<String> errors = new ArrayList<>();

    public Book2(File bookDir) throws Exception {
        this.bookDir = bookDir;
        this.bookFile = new File(bookDir, "book.properties");
        new File(bookDir, "preview").mkdirs();
        File localFile = new File(bookDir, ".local");
        if (bookFile.exists()) {
            for (String line : FileUtils.readLines(bookFile, "UTF-8")) {
                Matcher m;
                if ((m = RE_PAGE.matcher(line)).matches()) {
                    String page = formatPageNumber(m.group(1));
                    String fieldName = m.group(2);
                    String value = m.group(3);
                    PageInfo pi = pages.get(page);
                    if (pi == null) {
                        pi = new PageInfo(page);
                        pages.put(pi.pageNumber, pi);
                    }
                    set(pi, fieldName, value, line);
                } else if ((m = RE_BOOK.matcher(line)).matches()) {
                    String fieldName = m.group(1);
                    String value = m.group(2);
                    set(this, fieldName, value, line);
                }
            }
            if (localFile.exists()) {
                localFor = FileUtils.readFileToString(localFile, "UTF-8");
                local = Context.thisHost.equals(localFor);
            } else {
                local = false;
            }
        } else {
            FileUtils.writeStringToFile(localFile, Context.thisHost, "UTF-8");
            local = true;
            save();
        }
    }

    private void set(Object obj, String fieldName, String value, String debug) {
        try {
            Field f = obj.getClass().getField(fieldName);
            if (!Modifier.isPublic(f.getModifiers()) || Modifier.isStatic(f.getModifiers())
                    || Modifier.isTransient(f.getModifiers())) {
                errors.add("Field is not public for '" + debug + "'");
                return;
            }
            if (f.getType() == int.class) {
                f.setInt(obj, Integer.parseInt(value));
            } else if (f.getType() == boolean.class) {
                f.setBoolean(obj, Boolean.parseBoolean(value));
            } else if (f.getType() == String.class) {
                f.set(obj, value);
            } else if (Set.class.isAssignableFrom(f.getType())) {
                TreeSet<String> v = new TreeSet<>(Arrays.asList(value.split(";")));
                f.set(obj, v);
            } else {
                errors.add("Unknown field class for set '" + debug + "'");
                return;
            }
        } catch (NoSuchFieldException ex) {
            errors.add("Unknown field for set '" + debug + "'");
        } catch (IllegalAccessException ex) {
            errors.add("Wrong field for set '" + debug + "'");
        } catch (Exception ex) {
            errors.add("Error set value to field for '" + debug + "'");
        }
    }

    private void get(Object obj, String prefix, List<String> lines) throws Exception {
        for (Field f : obj.getClass().getFields()) {
            if (Modifier.isPublic(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())
                    && !Modifier.isTransient(f.getModifiers())) {
                if (f.getType() == int.class) {
                    int v = f.getInt(obj);
                    if (v != -1) {
                        lines.add(prefix + f.getName() + "=" + v);
                    }
                } else if (f.getType() == boolean.class) {
                    lines.add(prefix + f.getName() + "=" + f.getBoolean(obj));
                } else if (f.getType() == String.class) {
                    String s = (String) f.get(obj);
                    if (s != null) {
                        lines.add(prefix + f.getName() + "=" + s);
                    }
                } else if (Set.class.isAssignableFrom(f.getType())) {
                    Set<?> set = (Set<?>) f.get(obj);
                    StringBuilder t = new StringBuilder();
                    for (Object o : set) {
                        t.append(o.toString()).append(';');
                    }
                    if (t.length() > 0) {
                        t.setLength(t.length() - 1);
                    }
                    lines.add(prefix + f.getName() + "=" + t);
                } else {
                    throw new RuntimeException("Unknown field class for get '" + f.getName() + "'");
                }
            }
        }
    }

    public String getName() {
        return bookDir.getName().toLowerCase();
    }

    public File getBookDir() {
        return bookDir;
    }

    public int getPagesCount() {
        return pages.size();
    }

    public String getStatusText() {
        try {
            if (!errors.isEmpty()) {
                return Messages.getString("BOOK_STATUS_ERROR_READING");
            }
            if (new File(bookDir, ".errors").exists()) {
                return Messages.getString("BOOK_STATUS_ERROR_PROCESSING");
            }
            if (new File(bookDir, ".process").exists()) {
                String text = FileUtils.readFileToString(new File(bookDir, ".process"), "UTF-8");
                return Messages.getString("BOOK_STATUS_PROCESSING", text);
            }
            if (new File(bookDir, ".process.done").exists()) {
                String text = FileUtils.readFileToString(new File(bookDir, ".process.done"), "UTF-8");
                return Messages.getString("BOOK_STATUS_PROCESSED", text);
            }
            if (localFor != null) {
                return Messages.getString("BOOK_STATUS_LOCAL", localFor);
            }
            return Messages.getString("BOOK_STATUS_WAITING");
        } catch (Exception ex) {
            return Messages.getString("BOOK_STATUS_ERROR_READING");
        }
    }

    public synchronized boolean pageExist(String p) {
        return pages.containsKey(formatPageNumber(p));
    }

    public synchronized List<String> listPages() {
        List<String> result = new ArrayList<>(pages.keySet());
        Collections.sort(result);
        return result;
    }

    public synchronized PageInfo getPageInfo(String page) {
        return pages.get(page);
    }

    public List<String> getErrors() {
        return errors;
    }

    public synchronized void addPage(PageInfo pageInfo) {
        String page = formatPageNumber(pageInfo.pageNumber);
        pages.put(page, pageInfo);
    }

    public synchronized PageInfo removePage(String pageNumber) {
        String page = formatPageNumber(pageNumber);
        return pages.remove(page);
    }

    public BufferedImage getImage(String pageNumber) throws Exception {
        File f = new PageFileInfo(this, pageNumber).getPreviewFile();
        return ImageIO.read(f);
    }

    public synchronized void save() throws Exception {
        LOG.info("Save book " + bookDir);
        List<String> lines = new ArrayList<>();
        get(this, "book.", lines);
        for (Map.Entry<String, PageInfo> en : pages.entrySet()) {
            String p = en.getKey();
            PageInfo pi = en.getValue();
            get(pi, "page." + p + ".", lines);
        }
        Collections.sort(lines);

        File bookFileNew = new File(bookFile.getPath() + ".new");
        File bookFileBak = new File(bookFile.getPath() + ".bak");
        FileUtils.writeLines(bookFileNew, "UTF-8", lines, "\n");
        if (bookFileBak.exists()) {
            if (!bookFileBak.delete()) {
                throw new Exception("Can't delete .bak file");
            }
        }
        if (bookFile.exists()) {
            if (!bookFile.renameTo(bookFileBak)) {
                throw new Exception("Can't rename old file to .bak");
            }
        }
        if (!bookFileNew.renameTo(bookFile)) {
            throw new Exception("Can't rename new file to " + bookFile.getAbsolutePath());
        }
    }

    static final Pattern RE_PAGE_NUMBER = Pattern.compile("([0-9]+)([a-z]{0,2})");

    public static String formatPageNumber(String pageNumber) {
        if (pageNumber == null) {
            return "";
        }
        Matcher m = RE_PAGE_NUMBER.matcher(pageNumber);
        if (m.matches()) {
            String n = m.group(1);
            n = "00000".substring(n.length()) + n + m.group(2).toLowerCase();
            return n;
        } else {
            return "";
        }
    }

    public static String simplifyPageNumber(String pageNumber) {
        if (pageNumber == null) {
            return "";
        }
        return pageNumber.replaceAll("^0+([0-9])", "$1");
    }

    public static int comparePageNumbers(String p1, String p2) {
        Matcher m = RE_PAGE_NUMBER.matcher(p1);
        if (!m.matches()) {
            return 0;
        }

        int n1 = Integer.parseInt(m.group(1));
        String s1 = m.group(2).toLowerCase();

        m = RE_PAGE_NUMBER.matcher(p2);
        if (!m.matches()) {
            return 0;
        }

        int n2 = Integer.parseInt(m.group(1));
        String s2 = m.group(2).toLowerCase();

        if (n1 != n2) {
            return n1 - n2;
        }
        return s1.compareTo(s2);
    }

    public static String incPage(String pageNumber, int incCount) {
        if (Math.abs(incCount) > 4) {
            throw new IllegalArgumentException("Wrong inc: " + incCount);
        }
        Matcher m = RE_PAGE_NUMBER.matcher(pageNumber);
        if (!m.matches()) {
            return "";
        }
        int n = Integer.parseInt(m.group(1));
        String idx = m.group(2).toLowerCase();
        String r;
        switch (idx.length()) {
        case 0:
            r = Integer.toString(n + incCount);
            break;
        case 1:
            int i = idx.charAt(0);
            i += incCount;
            if (i >= 'a' && i <= 'z') {
                r = n + "" + ((char) i);
            } else {
                r = "";
            }
            break;
        case 2:
            int i1 = idx.charAt(0);
            int i2 = idx.charAt(1);
            i2 += incCount;
            if (i2 > 'z') {
                i1++;
                i2 = i2 - 'z' + 'a' - 1;
            } else if (i2 < 'a') {
                i1--;
                i2 = i2 + 'z' - 'a' + 1;
            }
            if (i1 >= 'a' && i1 <= 'z' && i2 >= 'a' && i2 <= 'z') {
                r = n + "" + ((char) i1) + ((char) i2);
            } else {
                r = "";
            }
            break;
        default:
            throw new RuntimeException("Too many letters in page number");
        }
        return formatPageNumber(r);
    }

    public static String incPagePos(String pageNumber, boolean letter, int count) {
        if (Math.abs(count) > 4) {
            throw new IllegalArgumentException("Wrong inc: " + count);
        }
        Matcher m = RE_PAGE_NUMBER.matcher(pageNumber);
        if (!m.matches()) {
            return "";
        }
        String result;
        int n = Integer.parseInt(m.group(1));
        String idx = m.group(2).toLowerCase();
        if (letter) {
            if (idx.isEmpty()) {
                result = null;
            } else {
                result = incPage(pageNumber, count);
            }
        } else {
            n += count;
            result = n + idx;
        }
        return formatPageNumber(result);
    }
}
