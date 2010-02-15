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
package org.vectomatic.svg.chess;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vectomatic.dom.svg.OMSVGAnimatedString;
import org.vectomatic.dom.svg.OMSVGDocument;
import org.vectomatic.dom.svg.OMSVGGElement;
import org.vectomatic.dom.svg.OMSVGMatrix;
import org.vectomatic.dom.svg.OMSVGPoint;
import org.vectomatic.dom.svg.OMSVGRectElement;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.OMSVGUseElement;

import com.alonsoruibal.chess.Board;
import com.alonsoruibal.chess.Move;
import com.alonsoruibal.chess.bitboard.BitboardUtils;
import com.alonsoruibal.chess.movegen.LegalMoveGenerator;
import com.alonsoruibal.chess.movegen.MoveGenerator;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Class to update the SVG chess board
 * Representations used for the chessboard:
 * <dl>
 * <dt>index</dt><dd>int 0 ... 63</dd>
 * <dt>algebraic</dt><dd>string: a1 ... h8</dd>
 * <dt>square</dt><dd>long: (1 bit per square)</dd>
 * <dt>coords</dt><dd>0 &lt;= x &lt;= 7 ; 0 &lt;= y &lt= 7</dd>
 * </dl>
 * @author Lukas Laag (laaglu@gmail.com)
 */
public class ChessBoard implements MouseDownHandler, MouseUpHandler, MouseMoveHandler {

	private ChessCss css;
	private OMSVGSVGElement svgElt;
	private OMSVGGElement boardElt;
	private OMSVGDocument boardDoc;
	private OMSVGUseElement targetPiece;
	private int sqWidth;
	private int sqHeight;

	private Board board;
	private MoveGenerator legalMoveGenerator;
	private enum BoardMode {
		SRC_MODE,
		DEST_MODE
	};
	
	/**
	 * The current move number
	 */
	private int moveNumber;
	/**
	 * Returns the legal destination index
	 * from a source index;
	 */
	private Map<Integer, Set<Integer>> srcToDestIndex;
	/**
	 * Source index of the current move
	 */
	private int srcIndex;
	/**
	 * Destination index of the current move
	 */
	private int destIndex;
	/**
	 * Coordinates of the mousedown origin point
	 */
	private OMSVGPoint mouseDownCoords;
	/**
	 * Current UI mode: select either source or dest index
	 */
	private BoardMode mode;
	/**
	 * Maps algebraic coordinates to board squares
	 */
	private Map<String, OMSVGRectElement> algebraicToRects;
	/**
	 * Maps algebraic coordinates to chess pieces
	 */
	private Map<String, OMSVGUseElement> algebraicToPieces;

	private Main main;
	
	public ChessBoard(Board board, OMSVGSVGElement svgElt, Main main) {
		this.board = board;
		this.svgElt = svgElt;
		this.main = main;
		this.boardDoc = svgElt.getOwnerDocument();
		this.boardElt = (OMSVGGElement) boardDoc.getElementById("board");
		this.css = Resources.INSTANCE.getCss();
		this.srcToDestIndex = new HashMap<Integer, Set<Integer>>();
		this.mode = BoardMode.SRC_MODE;
		this.srcIndex = -1;
		this.destIndex = -1;
		this.algebraicToRects = new HashMap<String, OMSVGRectElement>();
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				int index = j + 8 * i;
				
				String squareId = BitboardUtils.index2Algebraic(index);
				OMSVGRectElement squareElt = (OMSVGRectElement) boardDoc.getElementById(squareId);
				algebraicToRects.put(squareId, squareElt);
			}
		}
		OMSVGRectElement sqElement = algebraicToRects.get("a1");
		this.sqWidth = (int)sqElement.getWidth().getBaseVal().getValue();
		this.sqHeight = (int)sqElement.getHeight().getBaseVal().getValue();
		this.algebraicToPieces = new HashMap<String, OMSVGUseElement>(); 

		// Legal moves logic
		legalMoveGenerator = new LegalMoveGenerator();
		moveNumber = -1;
		update(false);
		
		// Wire events
		boardElt.addMouseMoveHandler(this);
		boardElt.addMouseUpHandler(this);
	}
	
	/**
	 * Adds a new piece to the chessboard. Pieces are represented
	 * by svg &lt;use&gt; elements
	 * @param piece
	 * The piece to add
	 * @param algebraic
	 * The position
	 */
	public void addPiece(char piece, String algebraic) {
		if (piece != '.') {
			OMSVGRectElement squareElt = (OMSVGRectElement) boardDoc.getElementById(algebraic);
			OMSVGUseElement useElt = boardDoc.createSVGUseElement();
			useElt.getX().getBaseVal().setValue(squareElt.getX().getBaseVal().getValue());
			useElt.getY().getBaseVal().setValue(squareElt.getY().getBaseVal().getValue());
			useElt.getWidth().getBaseVal().setValue(sqWidth);
			useElt.getHeight().getBaseVal().setValue(sqHeight);
			useElt.getHref().setBaseVal("#" + Character.toString(piece));
			useElt.getStyle().setCursor(Cursor.MOVE);
			useElt.addMouseDownHandler(this);
			useElt.addMouseUpHandler(this);
			boardElt.appendChild(useElt);
			algebraicToPieces.put(algebraic, useElt);
		}
	}
	
	/**
	 * Removes a piece from the chessboard at the specified position
	 * @param algebraic
	 * The position
	 */
	public void removePiece(String algebraic) {
		OMSVGUseElement useElt = algebraicToPieces.remove(algebraic);
		if (useElt != null) {
			boardElt.removeChild(useElt);
		}
	}
	
	/**
	 * Returns the piece at the specified position
	 * @param algebraic
	 * The position
	 * @return
	 */
	public char getPiece(String algebraic) {
		OMSVGUseElement useElt = algebraicToPieces.get(algebraic);
		if (useElt != null) {
			OMSVGAnimatedString href = useElt.getHref();
			if (href != null) {
				String baseVal = href.getBaseVal();
				if (baseVal != null) {
					return baseVal.charAt(1);
				}
			}
		}
		return '.';
	}
	
	/**
	 * Update the chessboard
	 * @param force
	 * Force the recomputation of possible moves
	 */
	public void update(boolean force) {
		// If the move number has changed, update the possible
		// legal moves
		if (force || board.getMoveNumber() != moveNumber) {
			srcToDestIndex.clear();
			moveNumber = board.getMoveNumber();
			int[] moves = new int[64];
			int moveCount = legalMoveGenerator.generateMoves(board, moves, 0);
			for (int i = 0; i < moveCount; i++) {
				int srcIndex = Move.getFromIndex(moves[i]);
				Set<Integer> destIndices = srcToDestIndex.get(srcIndex);
				if (destIndices == null) {
					destIndices = new HashSet<Integer>();
					srcToDestIndex.put(srcIndex, destIndices);
				}
				destIndices.add(Move.getToIndex(moves[i]));
			}
		}
		
		Set<Integer> destIndices = srcToDestIndex.containsKey(srcIndex) ? srcToDestIndex.get(srcIndex) : Collections.<Integer>emptySet();
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				int index = j + 8 * i;
				
				String squareId = BitboardUtils.index2Algebraic(index);
				OMSVGRectElement squareElt = algebraicToRects.get(squareId);
				
				// Change the colors of the squares to highlight possible moves
				String className = ((j + i ) % 2) == 0 ? css.whiteSquare() : css.blackSquare();
				if (destIndices.contains(index)) {
					className = css.blueSquare();
				}
				if (mode == BoardMode.DEST_MODE && index == destIndex) {
					className = destIndices.contains(index) ? css.greenSquare() : css.redSquare();
				}
				if (index == srcIndex) {
					className = css.yellowSquare();
				}
				if (!className.equals(squareElt.getClassName().getBaseVal())) {
					squareElt.setClassNameBaseVal(className);
					//GWT.log("Setting: " + className, null);
				}

				// Update the piece on this square, if any
				char piece = board.pieceAt(BitboardUtils.index2Square((byte)index));
				if (getPiece(squareId) != piece) {
					removePiece(squareId);
					addPiece(piece, squareId);
				}
			}
		}
	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
//		GWT.log("onMouseDown(" + toString(event) + "))", null);
		onMouseDown_(event);
	}
	
	private void onMouseDown_(MouseEvent event) {
		String algebraic = getAlgebraic(event);
		if (targetPiece == null) {
			targetPiece = algebraicToPieces.get(algebraic);
			if (targetPiece != null) {
				this.destIndex = BitboardUtils.algebraic2Index(algebraic);
				mode = BoardMode.DEST_MODE;
				mouseDownCoords = getLocalCoordinates(event);
				update(false);
			}
		} else {
			int index = algebraic != null ? BitboardUtils.algebraic2Index(algebraic) : -1;
			if (index == srcIndex) {
				targetPiece = null;
			}
		}
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
//		GWT.log("onMouseUp(" + toString(event) + "))", null);

		if (targetPiece != null) {
			mode = BoardMode.SRC_MODE;
			Set<Integer> destIndices = srcToDestIndex.containsKey(srcIndex) ? srcToDestIndex.get(srcIndex) : Collections.<Integer>emptySet();
			if (destIndices.contains(destIndex)) {
				final int move = Move.getFromString(board, BitboardUtils.index2Algebraic(srcIndex) + BitboardUtils.index2Algebraic(destIndex));
				board.doMove(move);
				GWT.log("newItem(" + board.getMoveNumber() +  ")", null);
				main.addMove();
				DeferredCommand.addCommand(new Command() {
					@Override
					public void execute() {
						main.nextMove();
					}
					
				});
				event.stopPropagation();
			} else {
				targetPiece.getX().getBaseVal().setValue(getX(srcIndex));
				targetPiece.getY().getBaseVal().setValue(getY(srcIndex));
			}
			targetPiece = null;
			update(false);
		} else {
			onMouseDown_(event);
		}
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		String algebraic = getAlgebraic(event);
		//GWT.log("onMouseMove(" + algebraic + "))", null);
		int index = algebraic != null ? BitboardUtils.algebraic2Index(algebraic) : -1;
		if (mode == BoardMode.SRC_MODE) {
			if (srcIndex != index) {
				srcIndex = index;
				update(false);
			}
		} else {
			// Compute the delta from the mousedown point.
			OMSVGPoint p = getLocalCoordinates(event);
			targetPiece.getX().getBaseVal().setValue(getX(srcIndex) + p.getX() - mouseDownCoords.getX());
			targetPiece.getY().getBaseVal().setValue(getY(srcIndex) + p.getY() - mouseDownCoords.getY());
			if (destIndex != index) {
				destIndex = index;
				update(false);
			}
		}
		event.stopPropagation();
	}
	
	public OMSVGPoint getLocalCoordinates(MouseEvent e) {
		OMSVGPoint p = svgElt.createSVGPoint(e.getClientX(), e.getClientY());
		OMSVGMatrix m = boardElt.getScreenCTM().inverse();
		return p.matrixTransform(m);
	}

	public int getX(int index) {
		return sqWidth * (7 - (index % 8));
	}
	public int getY(int index) {
		return sqHeight * (7 - (index / 8));
	}

	/**
	 * Returns the algebraic corresponding to a mouse event, or null if
	 * there is no square
	 * @param event
	 * The mouse event
	 * @return
	 * The algebraic corresponding to a mouse event
	 */
	public String getAlgebraic(MouseEvent event) {

		OMSVGPoint p = getLocalCoordinates(event);
		int x = (int)(p.getX() / sqWidth);
		int y = (int)(p.getY() / sqHeight);
		if (x >= 0 && x <= 7 && y >= 0 && y <= 7) {
			char algebraic[] = new char[2];
			algebraic[0] = (char)('a' + x);
			algebraic[1] = (char)('8' - y);
			return new String(algebraic);
		}
		return null;
	}
//
//	public String toString(MouseEvent e) {
//		String width = svgElt.getStyle().getWidth();
//		float r = (Integer.parseInt(width.substring(0, width.length() - 2 /* 2 == "px".length() */))) / svgElt.getBBox().getWidth();
//		StringBuffer buffer = new StringBuffer();
//		buffer.append(" e=");
//		buffer.append(e.getRelativeElement());
//		buffer.append(" t=");
//		buffer.append(e.getNativeEvent().getEventTarget());
//		buffer.append(" cet=");
//		buffer.append(e.getNativeEvent().getCurrentEventTarget());
//		buffer.append(" cx=");
//		buffer.append(e.getClientX());
//		buffer.append(" cy=");
//		buffer.append(e.getClientY());
//		buffer.append(" scx=");
//		buffer.append((int)((e.getClientX())/ r));
//		buffer.append(" scy=");
//		buffer.append((int)((e.getClientY()) / r));
//		buffer.append(" rx=");
//		buffer.append(e.getRelativeX(svgElt.getElement()));
//		buffer.append(" ry=");
//		buffer.append(e.getRelativeY(svgElt.getElement()));
//		buffer.append(" x=");
//		buffer.append(e.getX());
//		buffer.append(" y=");
//		buffer.append(e.getY());
//		buffer.append(" sx=");
//		buffer.append(e.getScreenX());
//		buffer.append(" sy=");
//		buffer.append(e.getScreenY());
//		return buffer.toString();
//	}
//	private void log(String s) {
//		Text t = (Text) DOM.getElementById("title").getFirstChild();
//		t.setData(s);
//	}
}
