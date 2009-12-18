/**********************************************
 * Copyright (C) 2009 Lukas Laag
 * This file is part of libgwtsvg-chess.
 * 
 * libgwtsvg-chess is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * libgwtsvg-chess is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with libgwtsvg-chess.  If not, see http://www.gnu.org/licenses/
 **********************************************/
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

public class SplitPanelHelper {
	public static final <H extends EventHandler> HandlerRegistration addHandler(SplitPanel splitPanel, H handler,
            GwtEvent.Type<H> type) {
		return splitPanel.addHandler(handler, type);
	}
	public static final Style getStyle(SplitPanel splitPanel) {
		return splitPanel.getElement(0).getStyle();
	}
}

