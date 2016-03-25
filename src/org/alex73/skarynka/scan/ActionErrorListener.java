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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.slf4j.Logger;

/**
 * Action with error display on UI.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public abstract class ActionErrorListener implements ActionListener {
    private final Component parentComponent;
    private final String messageKey;
    private final Logger log;
    private final String logMessage;

    public ActionErrorListener(Component parentComponent, String messageKey, Logger log, String logMessage) {
        this.parentComponent = parentComponent;
        this.messageKey = messageKey;
        this.log = log;
        this.logMessage = logMessage;
    }

    abstract protected void action(ActionEvent e) throws Exception;

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            action(e);
        } catch (Throwable ex) {
            log.error(logMessage, ex);
            JOptionPane.showMessageDialog(parentComponent,
                    Messages.getString(messageKey) + ": " + ex.getClass() + " / " + ex.getMessage(),
                    Messages.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
        }
    }
}
