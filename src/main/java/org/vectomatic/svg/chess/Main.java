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

import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.utils.OMSVGParser;

import com.alonsoruibal.chess.Move;
import com.alonsoruibal.chess.StaticConfig;
import com.alonsoruibal.chess.bitboard.JSONAttackGenerator;
import com.alonsoruibal.chess.book.JSONBook;
import com.alonsoruibal.chess.search.SearchEngine;
import com.alonsoruibal.chess.search.SearchObserver;
import com.alonsoruibal.chess.search.SearchStatusInfo;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SplitPanelHelper;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.widgetideas.client.HSliderBar;
import com.google.gwt.widgetideas.client.SliderBar;
import com.google.gwt.widgetideas.client.SliderListenerAdapter;
import com.google.gwt.widgetideas.client.SliderBar.LabelFormatter;

/**
 * Main class. Instantiates the UI and runs the game loop
 */
public class Main implements EntryPoint, SearchObserver {
	interface MainBinder extends UiBinder<DecoratorPanel, Main> {
	}
	private static MainBinder mainBinder = GWT.create(MainBinder.class);

	@UiField(provided=true)
	ChessConstants constants = ChessConstants.INSTANCE;
	@UiField(provided=true)
	ChessCss style = Resources.INSTANCE.getCss();

	@UiField
	HorizontalSplitPanel splitPanel;
	@UiField
	HTML boardContainer;
	@UiField
	Button restartButton;
	@UiField
	Button fenButton;
	
	@UiField
	TabPanel tabPanel;
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
	 * The Carballo chess engine
	 */
	private SearchEngine engine;
	/**
	 * The SVG chess board
	 */
	private ChessBoard board;
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
	 * True if the handler for the browser back/forward button is activated, false otherwise
	 */
	private boolean undoActivated;
	/**
	 * Number of items in the browser undo stack
	 */
	private int undoableMoveCount;

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
		// Inject CSS in the document headers
		StyleInjector.inject(Resources.INSTANCE.getCss().getText());
		
		// Create a Carballo chess engine
		engine = new SearchEngine(new StaticConfig(), new JSONBook(), new JSONAttackGenerator());
		engine.setObserver(this);
		moveTimeIndex = 0;

		// Instantiate UI
		DecoratorPanel binderPanel = mainBinder.createAndBindUi(this);
		confirmBox = ConfirmBox.createConfirmBox(this);
		advancedPanel.getHeaderTextAccessor().setText(constants.advanced());
		
		modeListBox.addItem(ChessMode.whitesVsComputer.getDescription(), ChessMode.whitesVsComputer.name());
		modeListBox.addItem(ChessMode.blacksVsComputer.getDescription(), ChessMode.blacksVsComputer.name());
		modeListBox.addItem(ChessMode.whitesVsBlacks.getDescription(), ChessMode.whitesVsBlacks.name());
		modeListBox.addItem(ChessMode.computerVsComputer.getDescription(), ChessMode.computerVsComputer.name());
		modeListBox.setSelectedIndex(0);
		
		tabPanel.getTabBar().setTabText(0, ChessConstants.INSTANCE.settingsTab());
		tabPanel.getTabBar().setTabText(1, ChessConstants.INSTANCE.infoTab());
		tabPanel.getTabBar().setTabText(2, ChessConstants.INSTANCE.aboutTab());
		tabPanel.selectTab(1);
		about.setHTML(ChessConstants.INSTANCE.about());
		RootPanel.get("uiRoot").add(binderPanel);
		
		// Parse the SVG chessboard and insert it in the HTML UI
		// Note that the elements must be imported in the UI since they come from another XML document
		boardDiv = boardContainer.getElement();
		boardElt = OMSVGParser.parse(Resources.INSTANCE.getBoard().getText());
		boardDiv.appendChild(boardElt.getElement());

		// Create the object to animate the SVG chessboard
		board = new ChessBoard(engine.getBoard(), boardElt, this);

		// Handle resizing issues.
		ResizeHandler resizeHandler = new ResizeHandler() {
			@Override
			public void onResize(ResizeEvent event) {
				String width = (Window.getClientWidth() - 20) + "px";
				String height = getHeight() + "px";
				splitPanel.setSize(width, height);
				updateSplitPanel();
			}
		};
		Window.addResizeHandler(resizeHandler);
		resizeHandler.onResize(null);
		// Hack the HorizontalSplitPanel to generate an event when
		// the splitter element is moved
		SplitPanelHelper.addHandler(splitPanel, new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				if (splitPanel.isResizing()) {
					updateSplitPanel();
				}
			}
		}, MouseMoveEvent.getType());
		
		// Add undo-redo support through the browser back/forward buttons
		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				GWT.log("Main.undo(" + getCurrentToken() + ", " + undoActivated + ")", null);
				if (undoActivated) {
					engine.getBoard().undoMove(getCurrentToken());
					board.update(false);
					updateUI();
					nextMove();
				}
			}
		});
		undoActivated = true;
		restart();
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
			moveNumber = Integer.parseInt(token);
		} catch (NumberFormatException e) {
		}
		return moveNumber;
	}
	
	/**
	 * Method invoked when the HorizontalSplitPanel splitter is moved.
	 * Resizes the SVG chessboard.
	 */
	private void updateSplitPanel() {
		Style style = SplitPanelHelper.getStyle(splitPanel);
		String rawWidth = style.getWidth();
		GWT.log("Main.updateSplitPanel(" + rawWidth + ")", null);
		if (rawWidth != null && rawWidth.length() > 0) {
			// Process events with size in pixels only
			int index = rawWidth.indexOf(Style.Unit.PX.name().toLowerCase());
			if (index != -1) {
				try {
					int width = Integer.valueOf(rawWidth.substring(0, index));
					int height = getHeight();
					int size = Math.min(width, height);
					boardDiv.getStyle().setWidth(size, Style.Unit.PX);
					boardDiv.getStyle().setHeight(size, Style.Unit.PX);
					boardElt.getStyle().setWidth(size, Style.Unit.PX);
					boardElt.getStyle().setHeight(size, Style.Unit.PX);
				} catch(NumberFormatException e) {
					GWT.log("Incorrect width: " + rawWidth, e);
				}
			}
		}
	}

	/**
	 * Refresh the non SVG elements of the UI (list of moves, current player, FEN) 
	 */
	private void updateUI() {
		StringBuffer buffer = new StringBuffer();
		int[] moves = engine.getBoard().getMoveHistory();
		int count = engine.getBoard().getMoveNumber();
		for (int i = 0; i < count; i++) {
			String move = Move.toStringExt(moves[i]);
			if (i > 0) {
				buffer.append("\n");
			}
			buffer.append((i + 1) + ". " + move);
		}
		historyArea.setVisibleLines(count);
		historyArea.setText(buffer.toString());
		fenArea.setText(engine.getBoard().getFen());
		currentPlayerValueLabel.setText(engine.getBoard().getTurn() ? ChessConstants.INSTANCE.white() : ChessConstants.INSTANCE.black());
	}
	
	/**
	 * Invoked to make the game advance to the next move
	 */
	public void nextMove() {
		updateUI();
		switch (engine.getBoard().isEndGame()) {
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
				ChessMode mode = ChessMode.valueOf(modeListBox.getValue(modeListBox.getSelectedIndex()));
				switch(mode) {
					case whitesVsBlacks:
						break;
					case whitesVsComputer:
						if (!engine.getBoard().getTurn()) {
							computerMove();
						}
						break;
					case blacksVsComputer:
						if (engine.getBoard().getTurn()) {
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
		DeferredCommand.addCommand(new Command() {
			@Override
			public void execute() {
				engine.go(moveTimes[moveTimeIndex]);
			}
		});
	}

	/**
	 * Invoked by the carballo engine when the search is done
	 */
	public void bestMove(int bestMove, int ponder) {
		GWT.log("Main.bestMove(" + Move.toStringExt(bestMove) + ", " + Move.toStringExt(ponder) + ")", null);
		engine.getBoard().doMove(bestMove);
		board.update(false);
		nextMove();
	}

	/**
	 * Unused carballo chess engine event handler
	 */
	public void info(SearchStatusInfo info) {
		GWT.log("Main.info(" + info + ")", null);
	}
	
	/**
	 * Add an event to the browser undo/redo stack
	 */
	public void addUndoableMove() {
		History.newItem(Integer.toString(engine.getBoard().getMoveNumber()), false);
		undoableMoveCount++;
	}

	/**
	 * Start a new game
	 */
	public void restart() {
		undoActivated = false;
		for (int i = 0; i < undoableMoveCount; i++) {
			History.back();
		}
		undoActivated = true;
		undoableMoveCount = 0;
		engine.getBoard().startPosition();
		board.update(false);
		addUndoableMove();
		nextMove();
	}
	
	@UiHandler("fenButton")
	public void updateFen(ClickEvent event) {
		GWT.log("Main.updateFen(" + fenArea.getText() + ")", null);
		undoActivated = false;
		for (int i = 0; i < undoableMoveCount; i++) {
			History.back();
		}
		undoActivated = true;
		undoableMoveCount = 0;
		engine.getBoard().setFen(fenArea.getText());
		board.update(true);
		addUndoableMove();
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
	
}
