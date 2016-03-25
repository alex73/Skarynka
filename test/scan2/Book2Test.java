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
package scan2;

import org.alex73.skarynka.scan.Book2;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Book2Test {

    @Test
    public void checkIncPage() {
        assertEquals("00002", Book2.incPage("1", 1));
        assertEquals("00003", Book2.incPage("1", 2));
        assertEquals("00011", Book2.incPage("9", 2));
        assertEquals("00002", Book2.incPage("5", -3));
        assertEquals("", Book2.incPage("2", -3));
        assertEquals("00001b", Book2.incPage("00001a", 1));
        assertEquals("00001c", Book2.incPage("00001a", 2));
        assertEquals("00001z", Book2.incPage("00001y", 1));
        assertEquals("", Book2.incPage("00001a", -1));
        assertEquals("00001a", Book2.incPage("1b", -1));
        assertEquals("00001a", Book2.incPage("1d", -3));
        assertEquals("00001ab", Book2.incPage("00001aa", 1));
        assertEquals("00001ac", Book2.incPage("00001aa", 2));
        assertEquals("00001ba", Book2.incPage("00001az", 1));
        assertEquals("00001bb", Book2.incPage("00001az", 2));
        assertEquals("00001mo", Book2.incPage("00001mn", 1));
        assertEquals("00001zz", Book2.incPage("00001zy", 1));
        assertEquals("", Book2.incPage("00001aa", -1));
        assertEquals("00001ax", Book2.incPage("00001az", -2));
        assertEquals("00001az", Book2.incPage("00001ba", -1));
        assertEquals("00001ax", Book2.incPage("00001ba", -3));
        assertEquals("", Book2.incPage("00001zy", 2));
    }

    @Test
    public void checkIncPagePos() {
        assertEquals("00002", Book2.incPagePos("1", false, 1));
        assertEquals("00003", Book2.incPagePos("1", false, 2));
        assertEquals("00011", Book2.incPagePos("9", false, 2));
        assertEquals("00002", Book2.incPagePos("5", false, -3));
        assertEquals("", Book2.incPagePos("2", false, -3));
        assertEquals("00001b", Book2.incPagePos("00001a", true, 1));
        assertEquals("00002a", Book2.incPagePos("00001a", false, 1));
        assertEquals("00001c", Book2.incPagePos("00001a", true, 2));
        assertEquals("00003a", Book2.incPagePos("00001a", false, 2));
        assertEquals("00001z", Book2.incPagePos("00001y", true, 1));
        assertEquals("", Book2.incPagePos("00001a", true, -1));
        assertEquals("00001a", Book2.incPagePos("1b", true, -1));
        assertEquals("00000a", Book2.incPagePos("1a", false, -1));
        assertEquals("", Book2.incPagePos("1a", false, -2));
        assertEquals("00001a", Book2.incPagePos("1d", true, -3));
        assertEquals("00001ab", Book2.incPagePos("00001aa", true, 1));
        assertEquals("00001ac", Book2.incPagePos("00001aa", true, 2));
        assertEquals("00001ba", Book2.incPagePos("00001az", true, 1));
        assertEquals("00001bb", Book2.incPagePos("00001az", true, 2));
        assertEquals("00001mo", Book2.incPagePos("00001mn", true, 1));
        assertEquals("00001zz", Book2.incPagePos("00001zy", true, 1));
        assertEquals("", Book2.incPagePos("00001aa", true, -1));
        assertEquals("00001ax", Book2.incPagePos("00001az", true, -2));
        assertEquals("00001az", Book2.incPagePos("00001ba", true, -1));
        assertEquals("00001ax", Book2.incPagePos("00001ba", true, -3));
        assertEquals("", Book2.incPagePos("00001zy", true, 2));
    }
}
