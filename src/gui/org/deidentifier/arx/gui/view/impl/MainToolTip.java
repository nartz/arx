/*
 * ARX: Efficient, Stable and Optimal Data Anonymization
 * Copyright (C) 2012 - 2014 Florian Kohlmayer, Fabian Prasser
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.deidentifier.arx.gui.view.impl;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;

/**
 * This class implements a global tool tip
 * @author Fabian Prasser
 */
public class MainToolTip {

    private final ToolTip tip;
    private String        text        = null;
    private Rectangle     bounds      = null;
    private boolean       visible     = false;
    private int           currentX    = -1;
    private int           currentY    = -1;
    private long          currentTime = System.currentTimeMillis();

    private long          THRESHOLD   = 1000;
    private long          WAIT        = 100;

    /**
     * Creates a new instance
     * @param parent
     */
    public MainToolTip(final Shell parent) {

        tip = new ToolTip(parent, SWT.ICON_INFORMATION);
        tip.setAutoHide(false);
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (this) {
                        if (!visible && bounds != null) {
                            Point p = MouseInfo.getPointerInfo().getLocation();
                            if (p.x != currentX || p.y != currentY) {
                                currentTime = System.currentTimeMillis();
                                currentX = p.x;
                                currentY = p.y;
                            } else {
                                if (System.currentTimeMillis() - currentTime > THRESHOLD) {
                                    if (bounds.contains(currentX, currentY)) {
                                        show();
                                    }
                                }
                            }
                        }
                        if (visible && bounds != null) {
                            Point p = MouseInfo.getPointerInfo().getLocation();
                            if (!bounds.contains(p)) {
                                hide();
                            }
                        }
                    }
                    try {
                        Thread.sleep(WAIT);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private synchronized void show() {
        if (this.text != null) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    tip.setMessage(text);
                    tip.setVisible(true);
                    visible = true;
                }
            });
        }
    }

    protected synchronized void hide() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                tip.setVisible(false);
                visible = false;
                text = null;
                bounds = null;
                currentX = -1;
                currentY = -1;
            }
        });
    }

    public synchronized void setText(String text, org.eclipse.swt.graphics.Rectangle bounds) {
        if (this.visible) return;
        this.text = text;
        this.bounds = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
