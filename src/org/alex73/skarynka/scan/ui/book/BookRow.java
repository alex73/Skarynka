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
package org.alex73.skarynka.scan.ui.book;

import org.alex73.skarynka.scan.Book2;

/**
 * Bean for store book information for display in the books list.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class BookRow {
    public final String bookName;
    public final int pagesCount;
    public final String status;
    public final String info;
    public final boolean local;

    public BookRow(Book2 book) {
        bookName = book.getName();
        pagesCount = book.getPagesCount();
        status = book.getStatusText();
        local = book.local;

        StringBuilder s = new StringBuilder();
        int dpi = book.dpi != 0 ? book.dpi : 100;
        int w = Math.round(2.54f * book.cropSizeX / dpi);
        int h = Math.round(2.54f * book.cropSizeY / dpi);
        s.append(w).append('x').append(h).append("cm : ");
        s.append(book.cropSizeX).append('x').append(book.cropSizeY).append(" ");
        s.append(dpi).append("dpi ");
        if (book.scale != 100) {
            s.append(book.scale).append("%");
        }
        s.append(" zoom=").append(book.zoom);
        s.append("-> " + (book.cropSizeX * book.scale / 100) + "x" + (book.cropSizeY * book.scale / 100));
        info = s.toString();
    }
}
