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

import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.utils.OMSVGParser;

import com.alonsoruibal.chess.Board;
import com.alonsoruibal.chess.Config;
import com.alonsoruibal.chess.Move;
import com.alonsoruibal.chess.search.SearchEngine;
import com.alonsoruibal.chess.search.SearchObserver;
import com.alonsoruibal.chess.search.SearchParameters;
import com.alonsoruibal.chess.search.SearchStatusInfo;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.widgetideas.client.HSliderBar;
import com.google.gwt.widgetideas.client.SliderBar;
import com.google.gwt.widgetideas.client.SliderBar.LabelFormatter;
import com.google.gwt.widgetideas.client.SliderListenerAdapter;

/**
 * Main class. Instantiates the UI and runs the game loop
 */
public class Main implements EntryPoint, SearchObserver {
	interface MainBinder extends UiBinder<SplitLayoutPanel, Main> {
	}
	private static MainBinder mainBinder = GWT.create(MainBinder.class);

	@UiField(provided=true)
	ChessConstants constants = ChessConstants.INSTANCE;
	@UiField(provided=true)
	ChessCss style = Resources.INSTANCE.getCss();

	@UiField
	HTML boardContainer;
	@UiField
	Button restartButton;
	@UiField
	Button fenButton;
	@UiField
	Button undoButton;
	@UiField
	Button redoButton;

	@UiField
	DisclosurePanel advancedPanel;
	
	@UiField
	Label modeLabel;
	@UiField
	Label reflectionLabel;
	@UiField
	Label fenLabel;
	@UiField
	Label currentPlayerLabel;
	@UiField
	Label historyLabel;
	
	@UiField
	ListBox modeListBox;
	@UiField
	HSliderBar reflectionSlider;
	@UiField
	TextArea fenArea;
	@UiField
	TextArea historyArea;
	@UiField
	Label currentPlayerValueLabel;
	@UiField
	HTML about;
	
	DialogBox confirmBox;

	/**
	 * The Carballo engine
	 */
	SearchEngine engine;
	/**
	 * The Carballo board
	 */
	Board board;
	/**
	 * The SVG chess board
	 */
	ChessBoard chessboard;
	/**
	 * A &lt;div&gt; element to contain the SVG root element
	 */
	private Element boardDiv;
	/**
	 * The root SVG element
	 */
	private OMSVGSVGElement boardElt;
	/**
	 * Array of preset Carballo move times in ms 
	 */
	private int[] moveTimes = new int[] {3000, 10000, 30000, 60000, 180000, 600000};
	/**
	 * Index of the currently selected move time in the array
	 */
	private int moveTimeIndex;
	/**
	 * To handle integration of undo/redo with the browser back/forward button, if supported
	 */
	private HistoryManager historyManager;
	/**
	 * Last move number
	 */
	int lastMoveNumber;
	/**
	 * First move number
	 */
	int firstMoveNumber;

	/**
	 * UiBinder factory method to instantiate HSliderBar 
	 * @return
	 */
	@UiFactory
	HSliderBar makeHSliderBar() {
		final String[] labels = {
			ChessConstants.INSTANCE.mt3s(),
			ChessConstants.INSTANCE.mt10s(),
			ChessConstants.INSTANCE.mt30s(),
			ChessConstants.INSTANCE.mt1m(),
			ChessConstants.INSTANCE.mt3m(),
			ChessConstants.INSTANCE.mt10m()
		};
		HSliderBar sliderBar = new HSliderBar(0, 5, new LabelFormatter() {
		    protected String formatLabel(SliderBar slider, double value) {
		    	return labels[(int)value];
		    }			
		});
		sliderBar.setStepSize(1);
		sliderBar.setNumTicks(5);
		sliderBar.setNumLabels(5);
		sliderBar.setCurrentValue(0);
		sliderBar.addSliderListener(new SliderListenerAdapter() {
			@Override
			public void onValueChanged(SliderBar slider, double curValue) {
				moveTimeIndex = (int)curValue;
			}			
		});
		return sliderBar;
	}
	
	public int getHeight() {
		return (Window.getClientHeight() - 150);
	}
	
	  
	/**
	 * GWT entry point
	 */
	public void onModuleLoad() {
		final DecoratedPopupPanel initBox = new DecoratedPopupPanel();
		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.add(new Image(Resources.INSTANCE.getWaitImage()));
		hpanel.add(new Label(ChessConstants.INSTANCE.waitMessage()));
		initBox.add(hpanel);
		initBox.center();
		initBox.show();
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {	
			@Override
			public void execute() {
				// Inject CSS in the document headers
				StyleInjector.inject(Resources.INSTANCE.getCss().getText());
				
				// Create a Carballo chess engine
				Config config = new Config();
				config.setBook(new JSONBook());
				engine = new SearchEngine(config);
				engine.setObserver(Main.this);
				board = engine.getBoard();
		
				// Instantiate UI
				SplitLayoutPanel binderPanel = mainBinder.createAndBindUi(Main.this);
				confirmBox = ConfirmBox.createConfirmBox(Main.this);
				advancedPanel.getHeaderTextAccessor().setText(constants.advanced());
				
				modeListBox.addItem(ChessMode.whitesVsComputer.getDescription(), ChessMode.whitesVsComputer.name());
				modeListBox.addItem(ChessMode.blacksVsComputer.getDescription(), ChessMode.blacksVsComputer.name());
				modeListBox.addItem(ChessMode.whitesVsBlacks.getDescription(), ChessMode.whitesVsBlacks.name());
				modeListBox.addItem(ChessMode.computerVsComputer.getDescription(), ChessMode.computerVsComputer.name());
				modeListBox.setSelectedIndex(0);
				
				about.setHTML(ChessConstants.INSTANCE.about());
				RootLayoutPanel.get().add(binderPanel);
				
				// Parse the SVG chessboard and insert it in the HTML UI
				// Note that the elements must be imported in the UI since they come from another XML document
				boardDiv = boardContainer.getElement();
				boardElt = OMSVGParser.parse(Resources.INSTANCE.getBoard().getText());
				boardDiv.appendChild(boardElt.getElement());
		
				// Create the SVG chessboard. Use a temporary chessboard
				// until the engine has been initialized
				chessboard = new ChessBoard(board, boardElt, Main.this);
		
				// Add undo-redo support through the browser back/forward buttons
				historyManager = GWT.create(HistoryManager.class);
				historyManager.initialize(Main.this);
		
				moveTimeIndex = 0;
				restart();
				initBox.hide();
			}
		});
	}

	/**
	 * Refresh the non SVG elements of the UI (list of moves, current player, FEN) 
	 */
	private void updateUI() {
		StringBuffer buffer = new StringBuffer();
		int count = board.getMoveNumber();
		for (int i = firstMoveNumber; i < count; i++) {
			String move = board.getSanMove(i);
			assert move != null;
			if (i > 0) {
				buffer.append("\n");
			}
			buffer.append((i + 1) + ". " + move);
		}
		historyArea.setVisibleLines(count == 0 ? 1 : count);
		historyArea.setText(buffer.toString());
		fenArea.setText(board.getFen());
		currentPlayerValueLabel.setText(board.getTurn() ? ChessConstants.INSTANCE.white() : ChessConstants.INSTANCE.black());
		int moveNumber = board.getMoveNumber();
		
		int firstPossibleMove = firstMoveNumber;
		int lastPossibleMove = lastMoveNumber;
		switch(getMode()) {
			case whitesVsBlacks:
				break;
			case whitesVsComputer:
				if (firstMoveNumber % 2 == 1) {
					firstPossibleMove++;
				}
				if (lastPossibleMove % 2 == 1) {
					lastPossibleMove--;
				}
				break;
			case blacksVsComputer:
				if (firstMoveNumber % 2 == 0) {
					firstPossibleMove++;
				}
				if (lastPossibleMove % 2 == 0) {
					lastPossibleMove--;
				}
				break;
			case computerVsComputer:
				firstPossibleMove = Integer.MAX_VALUE;
				lastPossibleMove = Integer.MIN_VALUE;
				break;
		}
		undoButton.setEnabled(moveNumber > firstPossibleMove);
		redoButton.setEnabled(moveNumber < lastPossibleMove);
	}
	ChessMode getMode() {
		return ChessMode.valueOf(modeListBox.getValue(modeListBox.getSelectedIndex()));
	}
	
	/**
	 * Invoked to make the game advance to the next move
	 */
	public void nextMove() {
		updateUI();
		switch (board.isEndGame()) {
			case 1 :
				Window.alert(ChessConstants.INSTANCE.whitesWin());
				restart();
				break;
			case -1:
				Window.alert(ChessConstants.INSTANCE.blacksWin());
				restart();
				break;
			case 99:
				Window.alert(ChessConstants.INSTANCE.draw());
				restart();
				break;
			default:
				switch(getMode()) {
					case whitesVsBlacks:
						break;
					case whitesVsComputer:
						if (!board.getTurn()) {
							computerMove();
						}
						break;
					case blacksVsComputer:
						if (board.getTurn()) {
							computerMove();
						}
						break;
					case computerVsComputer:
						computerMove();
						break;
				}
				break;
		}

	}
	
	/**
	 * Invoked to make the computer play the next move
	 */
	private void computerMove() {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				engine.go(SearchParameters.get(moveTimes[moveTimeIndex]));
			}
			
		});
	}

	/**
	 * Invoked by the carballo engine when the search is done
	 */
	public void bestMove(int bestMove, int ponder) {
		GWT.log("Main.bestMove(" + Move.toStringExt(bestMove) + ", " + Move.toStringExt(ponder) + ")", null);
		board.doMove(bestMove);
		addMove();
		chessboard.update(false);
		nextMove();
	}

	/**
	 * Unused carballo chess engine event handler
	 */
	public void info(SearchStatusInfo info) {
		GWT.log("Main.info(" + info + ")", null);
	}
	
	/**
	 * Start a new game
	 */
	public void restart() {
		chessboard.update(true);
		lastMoveNumber = 0;
		historyManager.setMove(0);
		board.startPosition();
		firstMoveNumber = board.getMoveNumber();
		chessboard.update(true);
		nextMove();
	}
	
	@UiHandler("fenButton")
	public void updateFen(ClickEvent event) {
		GWT.log("Main.updateFen(" + fenArea.getText() + ")", null);
		historyManager.setMove(0);
		board.setFen(fenArea.getText());
		lastMoveNumber = firstMoveNumber = board.getMoveNumber();
		chessboard.update(true);
		nextMove();
	}

	@UiHandler("modeListBox")
	public void modeChange(ChangeEvent event) {
		GWT.log("Main.modeChange(" + modeListBox.getSelectedIndex() + ")", null);
		nextMove();
	}
	
	@UiHandler("restartButton")
	public void confirmRestart(ClickEvent event) {
		GWT.log("Main.confirmRestart()", null);
        confirmBox.center();
        confirmBox.show();
    }

	@UiHandler("undoButton")
	public void undo(ClickEvent event) {
		GWT.log("Main.undo()", null);
		int moveNumber = board.getMoveNumber();
		switch(getMode()) {
			case whitesVsBlacks:
				setMove(moveNumber - 1);
				break;
			case whitesVsComputer:
			case blacksVsComputer:
				setMove(moveNumber - 2);
				break;
			case computerVsComputer:
				break;
		}
		nextMove();
   }

	@UiHandler("redoButton")
	public void redo(ClickEvent event) {
		GWT.log("Main.redo()", null);
		int moveNumber = board.getMoveNumber();
		switch(getMode()) {
			case whitesVsBlacks:
				setMove(moveNumber + 1);
				break;
			case whitesVsComputer:
			case blacksVsComputer:
				setMove(moveNumber + 2);
				break;
			case computerVsComputer:
				break;
		}
		nextMove();
    }
	
	/**
	 * Add an event to the browser undo/redo stack
	 */
	public void addMove() {
		historyManager.addMove();
		lastMoveNumber = board.getMoveNumber();
	}

	private void setMove(int moveNumber) {
		historyManager.setMove(moveNumber);
		board.undoMove(moveNumber);
		chessboard.update(true);
	}
}
