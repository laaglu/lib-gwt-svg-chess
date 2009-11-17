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
 * along with libgwtsvg-chess.  If not, see <http://www.gnu.org/licenses/>
 **********************************************/
package org.vectomatic.svg.chess;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

public interface ChessConstants extends Constants {
	public static final ChessConstants INSTANCE =  GWT.create(ChessConstants.class);
	public String whitesVsBlacks();
	public String whitesVsComputer();
	public String blacksVsComputer();
	public String computerVsComputer();
	
	public String white();
	public String black();
	
	public String infoTab();
	public String settingsTab();

	public String mode();
	public String reflectionTime();
	public String progress();
	public String player();
	public String history();
	public String fen();
	public String advanced();

	public String restart();
	public String setFen();

	public String whitesWin();
	public String blacksWin();
	public String draw();
	public String confirmRestart();
	public String confirmYes();
	public String confirmNo();

	public String mt3s();
	public String mt10s();
	public String mt30s();
	public String mt1m();
	public String mt3m();
	public String mt10m();
}
