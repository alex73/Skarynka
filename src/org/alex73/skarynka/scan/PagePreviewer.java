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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alex73.skarynka.scan.process.PageFileInfo;

/**
 * Page preview handler.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class PagePreviewer {

    private final Book2 book;

    private Map<String, BufferedImage> cache = new HashMap<>();
    private Map<String, IPagePreviewChanged> listeners = new HashMap<>();
    private ExecutorService queue = Executors.newFixedThreadPool(1);

    public PagePreviewer(Book2 book) {
        this.book = book;
    }

    public void finish() {
        queue.shutdownNow();
    }

    public void updatePreview(String pageNumber) {
        String page = Book2.formatPageNumber(pageNumber);
        IPagePreviewChanged listener;
        synchronized (listeners) {
            listener = listeners.get(page);
        }
        if (listener != null) {
            showEmpty(listener);
            queue(page);
        }
    }

    public void reset() {
        queue.shutdownNow();
        synchronized (listeners) {
            listeners.clear();
        }
        synchronized (cache) {
            cache.clear();
        }
        queue = Executors.newFixedThreadPool(1);
    }

    public void setPreview(String pageNumber, IPagePreviewChanged listener) {
        String page = Book2.formatPageNumber(pageNumber);
        BufferedImage stored;
        synchronized (listeners) {
            listeners.put(page, listener);
        }
        synchronized (cache) {
            stored = cache.get(page);
        }
        if (stored != null) {
            listener.show(stored);
        } else {
            showEmpty(listener);
            queue(page);
        }
    }

    private void showEmpty(IPagePreviewChanged listener) {
        listener.show(new BufferedImage(DataStorage.previewMaxWidth, DataStorage.previewMaxHeight,
                BufferedImage.TYPE_INT_ARGB));
    }

    private void queue(String page) {
        queue.submit(new Runnable() {
            @Override
            public void run() {
                PageFileInfo pfi = new PageFileInfo(book, page);
                BufferedImage result = pfi.constructPagePreview(DataStorage.previewMaxWidth,
                        DataStorage.previewMaxHeight);
                synchronized (cache) {
                    cache.put(page, result);
                }
                IPagePreviewChanged listener;
                synchronized (listeners) {
                    listener = listeners.get(page);
                }
                if (listener != null) {
                    listener.show(result);
                }
            }
        });
    }
}
