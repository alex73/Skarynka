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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceExceptionTest {
    @Test
    public void check() {
        String msg = new ServiceException("BOOK_OPEN", "aaaa", "bbbb").getMessage();

        assertTrue(msg.contains("aaaa"));
        assertTrue(msg.contains("bbbb"));
    }
}
