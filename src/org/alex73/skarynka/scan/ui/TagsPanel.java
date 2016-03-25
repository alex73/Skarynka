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
package org.alex73.skarynka.scan.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Tags panel widget for display page tags from config.xml.
 * 
 * @author Aleś Bułojčyk <alex73mail@gmail.com>
 */
public class TagsPanel extends JPanel {
    public TagsPanel() {
        super(new GridBagLayout());
        add(new JLabel("tags panel"));
    }

    public void setup(Map<String, String> tags) {
        removeAll();

        GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(2, 3, 2, 3), 0, 0);
        for (Map.Entry<String, String> en : tags.entrySet()) {
            JCheckBox cb = new JCheckBox(en.getValue());
            cb.setName(en.getKey());
            // cb.addActionListener(actionTag);
            add(cb, gbc);
            gbc.gridy++;
        }
    }

    public void setValues(Set<String> tags) {
        for (int i = 0; i < getComponentCount(); i++) {
            JCheckBox cb = (JCheckBox) getComponent(i);
            cb.setSelected(tags.contains(cb.getName()));
        }
    }

    public Set<String> getValues() {
        Set<String> result = new TreeSet<>();
        for (int i = 0; i < getComponentCount(); i++) {
            JCheckBox cb = (JCheckBox) getComponent(i);
            if (cb.isSelected()) {
                result.add(cb.getName());
            }
        }
        return result;
    }
    
    public void addActionListener(ActionListener listener) {
        for (int i = 0; i < getComponentCount(); i++) {
            JCheckBox cb = (JCheckBox) getComponent(i);
            cb.addActionListener(listener);
        }
    }
}
