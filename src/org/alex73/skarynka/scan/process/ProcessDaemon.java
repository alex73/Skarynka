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
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.alex73.skarynka.scan.Book2;
import org.alex73.skarynka.scan.Context;
import org.alex73.skarynka.scan.process.pdf.PdfCreator;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDaemon extends Thread {
    private static Logger LOG = LoggerFactory.getLogger(ProcessDaemon.class);
    private static Charset UTF8 = Charset.forName("UTF-8");

    private boolean finish;
    private boolean paused;

    private Script currentScript;

    public void finish() {
        synchronized (this) {
            finish = true;
            notifyAll();
        }
    }

    public void setPaused(boolean p) {
        paused = p;
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        LOG.info("ProcessDaemon started");
        while (!finish) {
            try {
                boolean processed = process();
                if (!finish) {
                    synchronized (this) {
                        wait(processed ? 50 : 10000);
                    }
                }
            } catch (Throwable ex) {
                LOG.error("Error process", ex);
            }
        }
    }

    boolean process() throws Exception {
        LOG.debug("check for processing...");
        if (Context.getPermissions().ProcessingControls) {
            // process control files
            LOG.trace("check for control dir " + Context.getControlDir());
            File[] controls = new File(Context.getControlDir()).listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isFile() && file.getName().endsWith(".do");
                }
            });
            if (controls != null) {
                LOG.trace("control files found: " + controls.length);
                for (File c : controls) {
                    LOG.trace("check for control file " + c);
                    File errorFile = new File(c.getPath() + ".err");
                    File outFile = new File(c.getPath() + ".out");
                    if (errorFile.exists() || outFile.exists()) {
                        continue;
                    }
                    try {
                        String cmd = FileUtils.readFileToString(c, "UTF-8");
                        String result = ProcessCommands.call(cmd);
                        FileUtils.writeStringToFile(outFile, result, "UTF-8");
                    } catch (Throwable ex) {
                        FileUtils.writeStringToFile(errorFile, ex.getMessage(), "UTF-8");
                    }
                }
            }
        }
        if (paused) {
            return false;
        }
        if (Context.getPermissions().ProcessingBooks) {
            // process books
            LOG.trace("check for book dir " + Context.getBookDir());
            File[] ls = new File(Context.getBookDir()).listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && new File(pathname, "book.properties").isFile();
                }
            });

            if (ls == null) {
                LOG.trace("books not found");
                return false;
            }

            int count = 0;
            for (File d : ls) {
                LOG.trace("check for book dir " + d);
                File processFile = new File(d, ".process");
                if (!processFile.exists()) {
                    // processing wasn't started yet
                    continue;
                }
                File processDoneFile = new File(d, ".process.done");
                if (processDoneFile.exists()) {
                    // processing finished
                    continue;
                }
                File errorFile = new File(d, ".errors");
                if (errorFile.exists()) {
                    // if book contains error files - skip this book
                    continue;
                }
                LOG.debug("Process book " + d);
                Book2 book = new Book2(d);
                if (!book.getErrors().isEmpty()) {
                    FileUtils.writeStringToFile(errorFile, "Error read book: " + book.getErrors(), UTF8);
                    continue;
                }
                String command = FileUtils.readFileToString(processFile, UTF8);

                Script newScript;
                try {
                    // update compiled script if script file was updated
                    newScript = new Script(command);
                    if (currentScript == null || !currentScript.theSame(newScript)) {
                        currentScript = newScript;
                        currentScript.compile();
                    }
                } catch (Throwable ex) {
                    FileUtils.writeStringToFile(errorFile, "Error get script: " + ex.getMessage(), UTF8);
                    continue;
                }
                boolean wasError = false;
                for (String p : book.listPages()) {
                    if (finish) {
                        return false;
                    }
                    // PageFileInfo pfi = new PageFileInfo(book, p);
                    try {
                        if (currentScript.pageResultExist(book, p)) {
                            continue;
                        }
                        currentScript.pageExecute(book, p);
                        count++;
                    } catch (Throwable ex) {
                        LOG.info("Error process page " + p, ex);
                        FileUtils.writeStringToFile(errorFile,
                                "Error process page #" + p + ": " + ex.getMessage(), UTF8);
                        wasError = true;
                        break;
                    }
                    if (count >= 5) {
                        return true;
                    }
                }
                // pages process was finished - need to finish book
                try {
                    if (!wasError) {
                        if (!currentScript.bookResultExist(book)) {
                            currentScript.bookExecute(book);
                        }
                    }
                } catch (Throwable ex) {
                    LOG.info("Error process book", ex);
                    FileUtils.writeStringToFile(errorFile, "Error process book: " + ex.getMessage(), UTF8);
                    wasError = true;
                    break;
                }

                if (!wasError) {
                    // book finished without errors
                    processFile.renameTo(processDoneFile);
                }
            }
        }
        return false;
    }

    public static class PageContext {
        private final Book2 book;
        private final String page;
        private final Book2.PageInfo pi;
        private final Map<String, Boolean> tags;

        public PageContext(Book2 book, String page) {
            this.book = book;
            this.page = Book2.formatPageNumber(page);
            this.pi = book.getPageInfo(this.page);
            Map<String, Boolean> tags = new TreeMap<>();
            for (String t : pi.tags) {
                tags.put(t, true);
            }
            this.tags = Collections.unmodifiableMap(tags);
        }

        public String getNumber() {
            return page;
        }

        public Map<String, Boolean> getTags() {
            return tags;
        }

        public int getCropPosX() {
            return pi.cropPosX;
        }

        public int getCropPosY() {
            return pi.cropPosY;
        }

        public int getRotate() {
            return pi.rotate;
        }

        public String getCamera() {
            return pi.camera;
        }
    }

    public static class BookContext {
        private final Book2 book;

        public BookContext(Book2 book) {
            this.book = book;
        }

        public String getName() {
            return book.getName();
        }

        public int getScale() {
            return book.scale;
        }

        public int getDpi() {
            return book.dpi;
        }

        public int getCropSizeX() {
            return book.cropSizeX;
        }

        public int getCropSizeY() {
            return book.cropSizeY;
        }

        public int getImageSizeX() {
            return book.imageSizeX;
        }

        public int getImageSizeY() {
            return book.imageSizeY;
        }
        
        public List<String> getPages() {
            return book.listPages();
        }
    }

    public static class CommandContext {
        private final Book2 book;
        private final Bindings bindings;

        public CommandContext(Book2 book, Bindings bindings) {
            this.book = book;
            this.bindings = bindings;
        }

        public boolean fileExist(String name) {
            return new File(book.getBookDir(), name).exists();
        }

        public boolean fileRemove(String name) {
            return new File(book.getBookDir(), name).delete();
        }

        static final Pattern RE_VAR = Pattern.compile("\\$\\{.+?\\}");

        public void exec(String cmd) throws Exception {
            StringBuilder cmdo = new StringBuilder();
            Matcher m = RE_VAR.matcher(cmd);
            int prev = 0;
            while (m.find(prev)) {
                cmdo.append(cmd.substring(prev, m.start()));
                String var = cmd.substring(m.start() + 2, m.end() - 1);
                cmdo.append(BeanUtils.getProperty(bindings, var));
                prev = m.end();
            }
            cmdo.append(cmd.substring(prev));
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
            LOG.debug("Execute for book " + book.getName() + ": " + cmdo);
            Process process = Runtime.getRuntime().exec(cmda, null, book.getBookDir());
            int r = process.waitFor();
            LOG.debug("Execution result: " + r);
            if (r != 0) {
                String err = IOUtils.toString(process.getErrorStream(),
                        Context.getSettings().get("command_charset"));
                throw new Exception("Error execution " + cmd + ": " + r + " / " + err);
            }
        }
    	public void pdf(String bookOutPath, String bookName) throws Exception {
    		File bookDir = new File(Context.getBookDir(), bookName);
    		File outFile = new File(bookDir, bookOutPath);
    		File[] jpegs = bookDir.listFiles(new FileFilter() {
    			@Override
    			public boolean accept(File f) {
    				return f.isFile() && f.getName().endsWith(".jp2");
    			}
    		});
    		Arrays.sort(jpegs);
    		PdfCreator.create(outFile, jpegs);
    	}
    }

    public static class Script {
        public final String script;
        public final String command;
        public ScriptEngine engine;
        public CompiledScript csPageResultExists;
        public CompiledScript csPageExecute;
        public CompiledScript csBookResultExists;
        public CompiledScript csBookExecute;

        public Script(String command) throws Exception {
            this.script = FileUtils.readFileToString(new File("process.js"), "UTF-8");
            this.command = command;
        }

        public boolean theSame(Script obj) {
            return script.equals(obj.script) && command.equals(obj.command);
        }

        public void compile() throws Exception {
            engine = new ScriptEngineManager().getEngineByName("nashorn");
            csPageResultExists = ((Compilable) engine).compile(script + "\n exist_" + command + "();");
            csPageExecute = ((Compilable) engine).compile(script + "\n execute_" + command + "();");
            csBookResultExists = ((Compilable) engine).compile(script + "\n bookexist_" + command + "();");
            csBookExecute = ((Compilable) engine).compile(script + "\n bookexecute_" + command + "();");
        }

        public boolean pageResultExist(Book2 book, String page) throws Exception {
            Bindings bindings = engine.createBindings();
            bindings.put("settings", Context.getSettings());
            bindings.put("page", new PageContext(book, page));
            bindings.put("book", new BookContext(book));
            bindings.put("cmd", new CommandContext(book, bindings));
            return (boolean) csPageResultExists.eval(bindings);
        }

        public void pageExecute(Book2 book, String page) throws Exception {
            Bindings bindings = engine.createBindings();
            bindings.put("settings", Context.getSettings());
            bindings.put("page", new PageContext(book, page));
            bindings.put("book", new BookContext(book));
            bindings.put("cmd", new CommandContext(book, bindings));
            csPageExecute.eval(bindings);
        }

        public boolean bookResultExist(Book2 book) throws Exception {
            Bindings bindings = engine.createBindings();
            bindings.put("settings", Context.getSettings());
            bindings.put("book", new BookContext(book));
            bindings.put("cmd", new CommandContext(book, bindings));
            return (boolean) csBookResultExists.eval(bindings);
        }

        public void bookExecute(Book2 book) throws Exception {
            Bindings bindings = engine.createBindings();
            bindings.put("settings", Context.getSettings());
            bindings.put("book", new BookContext(book));
            bindings.put("cmd", new CommandContext(book, bindings));
            csBookExecute.eval(bindings);
        }
    }
}
