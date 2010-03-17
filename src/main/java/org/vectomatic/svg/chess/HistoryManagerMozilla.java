/**********************************************
 * Copyright (C) 2009 Lukas Laag
 * This file is part of lib-gwt-svg-chess.
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
package org.vectomatic.svg.chess;

import com.alonsoruibal.chess.Move;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;

public class HistoryManagerMozilla extends HistoryManager {
	/**
	 * True if the handler for the browser back/forward button is activated, false otherwise
	 */
	private boolean undoActivated;
	
	/**
	 * The main application
	 */
	private Main main;
	
	@Override
	public void initialize(Main mainApp) {
		main = mainApp;
		// Add undo-redo support through the browser back/forward buttons
		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				GWT.log("Main.onValueChange(" + History.getToken() + ", " + undoActivated + ")", null);
				if (undoActivated) {
					int targetMoveNumber = getCurrentToken();
					if (targetMoveNumber != -1) {
						int moveNumber = main.board.getMoveNumber();
						switch(main.getMode()) {
							case whitesVsBlacks:
								break;
							case whitesVsComputer:
								if (targetMoveNumber < moveNumber) {
									if (targetMoveNumber % 2 == 1) {
										targetMoveNumber--;
										History.back();
									}
								} else if (targetMoveNumber > moveNumber) {
									if (targetMoveNumber % 2 == 1 && targetMoveNumber < main.lastMoveNumber) {
										targetMoveNumber++;
										History.forward();
									}
								}
								break;
							case blacksVsComputer:
								if (targetMoveNumber < moveNumber) {
									if (targetMoveNumber % 2 == 0) {
										targetMoveNumber--;
										History.back();
									}
								} else if (targetMoveNumber > moveNumber) {
									if (targetMoveNumber % 2 == 0 && targetMoveNumber < main.lastMoveNumber) {
										targetMoveNumber++;
										History.forward();
									}
								}
								break;
							case computerVsComputer:
								return;
						}
						assert targetMoveNumber >= -1;
						assert targetMoveNumber <= main.lastMoveNumber;
						if (moveNumber == -1) {
							main.board.startPosition();
						} else {
							main.board.undoMove(targetMoveNumber);
						}
						// Calls to nextMove will trigger a ValueChangeEvent due
						// to calls to History.back and History.forward
						// This event must be ignored
						main.chessboard.update(true);
						main.nextMove();
						return;
					}
				}
				undoActivated = true;
			}
		});
		History.fireCurrentHistoryState();
		undoActivated = true;
	}

	
	/**
	 * Return the carballo index of the current move as
	 * parsed from the browser undo/redo stack
	 * @return
	 */
	private int getCurrentToken() {
		int moveNumber = -1;
		try {
			String token = History.getToken();
			int index = token.indexOf('_');
			if (index != -1) {
				moveNumber = Integer.parseInt(token.substring(0, index));				
			}
		} catch (NumberFormatException e) {
		}
		return moveNumber;
	}
	
	@Override
	public void addMove() {
		int moveNumber = main.board.getMoveNumber();
		History.newItem(Integer.toString(moveNumber) + "_" + Move.toStringExt(main.board.getMoveHistory()[moveNumber - 1]), false);
	}

	@Override
	public void setMove(int moveNumber) {
		int moveCount = main.board.getMoveNumber() - (moveNumber == -1 ? 0 : moveNumber);
		undoActivated = false;
		if (moveCount > 0) {
			for (int i = 0; i < moveCount; i++) {
				History.back();
			}
		} else if (moveCount < 0) {
			for (int i = 0; i < -moveCount; i++) {
				History.forward();
			}
		}
	}

}
