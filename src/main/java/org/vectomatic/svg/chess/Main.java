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

import org.vectomatic.dom.svg.OMSVGDocument;
import org.vectomatic.dom.svg.OMSVGElement;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.gwt.SVGParser;

import com.alonsoruibal.chess.Move;
import com.alonsoruibal.chess.StaticConfig;
import com.alonsoruibal.chess.bitboard.JSONAttackGenerator;
import com.alonsoruibal.chess.book.JSONBook;
import com.alonsoruibal.chess.search.SearchEngine;
import com.alonsoruibal.chess.search.SearchObserver;
import com.alonsoruibal.chess.search.SearchStatusInfo;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
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

// TODO: define an GWT compatible API on OMSVGElement to manipulate CSS class and styles

public class Main implements EntryPoint, SearchObserver {
	interface Binder extends UiBinder<DecoratorPanel, Main> {
	}
	private static Binder binder = GWT.create(Binder.class);

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


	private SearchEngine engine;
	int moveTimeIndex;
	private int[] moveTimes = new int[] {3000, 10000, 30000, 60000, 180000, 600000};
	private ChessBoard board;
	private boolean undoActivated;
	private int undoableMoveCount;
	private DivElement boardDiv;
	private OMSVGElement boardElt;

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
		return (Window.getClientHeight() - 200);
	}
	
	public void onModuleLoad() {
		// Inject CSS in the document headers
		StyleInjector.inject(Resources.INSTANCE.getCss().getText());
		
		// Create a Carballo chess engine
		//new JSONAttackGenerator2().run();
		engine = new SearchEngine(new StaticConfig(), new JSONBook(), new JSONAttackGenerator());
		engine.setObserver(this);
		moveTimeIndex = 0;

		// Instantiate UI
		DecoratorPanel binderPanel = binder.createAndBindUi(this);
		advancedPanel.getHeaderTextAccessor().setText(constants.advanced());
		
		modeListBox.addItem(ChessMode.whitesVsComputer.getDescription(), ChessMode.whitesVsComputer.name());
		modeListBox.addItem(ChessMode.blacksVsComputer.getDescription(), ChessMode.blacksVsComputer.name());
		modeListBox.addItem(ChessMode.whitesVsBlacks.getDescription(), ChessMode.whitesVsBlacks.name());
		modeListBox.addItem(ChessMode.computerVsComputer.getDescription(), ChessMode.computerVsComputer.name());
		modeListBox.setSelectedIndex(0);
		
//		modeLabel.setText(ChessConstants.INSTANCE.mode());
//		reflectionLabel.setText(ChessConstants.INSTANCE.reflectionTime());;
//		fenLabel.setText(ChessConstants.INSTANCE.fen());;
//		currentPlayerLabel.setText(ChessConstants.INSTANCE.player());;
//		ellapsedLabel.setText(ChessConstants.INSTANCE.ellapsed());;
//		historyLabel.setText(ChessConstants.INSTANCE.history());;
		
//		undoButton.setText(ChessConstants.INSTANCE.undo());
//		redoButton.setText(ChessConstants.INSTANCE.redo());
//		suggestionButton.setText(ChessConstants.INSTANCE.suggestion());
//		restartButton.setText(ChessConstants.INSTANCE.restart());
//		fenButton.setText(ChessConstants.INSTANCE.setFen());

		tabPanel.getTabBar().setTabText(0, ChessConstants.INSTANCE.settingsTab());
		tabPanel.getTabBar().setTabText(1, ChessConstants.INSTANCE.infoTab());
		tabPanel.selectTab(1);
		RootPanel.get("uiRoot").add(binderPanel);
		

		// Parse the SVG chessboard and insert it in the HTML UI
		boardDiv = boardContainer.getElement().cast();
		OMSVGDocument boardDoc = SVGParser.parse(Resources.INSTANCE.getBoard().getText());
//		Element uiRoot = DOM.getElementById("uiRoot");
		Element boardElementTmp = boardDoc.getDocumentElement().cast();
		boardElt = importNode(boardDiv.getOwnerDocument(), boardElementTmp, true).cast();
//		boardDiv.setAttribute("style", "width:100%;height:100%");
		boardDiv.appendChild((Element)boardElt.cast());
//		DOM.getElementById("uiRoot").appendChild((Element)boardElt.cast());
//		Board b = new Board();
//		Window.alert("a1");
//		b.startPosition();
//		Window.alert("a2");
		
		board = new ChessBoard(engine.getBoard(), (OMSVGSVGElement)boardElt.cast(), this);

		// Handle resizing issues
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
		SplitPanelHelper.addHandler(splitPanel, new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				if (splitPanel.isResizing()) {
					updateSplitPanel();
				}
			}
		}, MouseMoveEvent.getType());

		
		// Initialize UI
		currentPlayerValueLabel.setText(ChessConstants.INSTANCE.white());
		
		//TODO
		// Mozilla bug: importNode will mess up xmlns:xlink attributes
		// and prefix binding
//		OMSVGElement boardElt = importNode(boardContainer.getOwnerDocument(), boardElementTmp, true).cast();
//		boardContainer.appendChild((Node)boardElt.cast());
//		board = new SVGBoard(engine, boardElt);
//		boardContainer.appendChild((Node)boardElementTmp.cast());
//		board = new ChessBoard(engine, (OMSVGSVGElement)boardElementTmp.cast());
		
		// Add undo-redo support through the browser back/forward buttons
		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				GWT.log("undo(" + getCurrentToken() + ", " + undoActivated + ")", null);
				if (undoActivated) {
					engine.getBoard().undoMove(getCurrentToken());
					board.update(false);
					updateHistory();
					nextMove();
				}
			}
		});
		undoActivated = true;
		restart(null);
	}
	
	public int getCurrentToken() {
		int moveNumber = -1;
		try {
			String token = History.getToken();
			moveNumber = Integer.parseInt(token);
		} catch (NumberFormatException e) {
		}
		return moveNumber;
	}
	
	private void updateSplitPanel() {
		Style style = SplitPanelHelper.getStyle(splitPanel);
		
		int width = Integer.valueOf(style.getWidth().substring(0, style.getWidth().indexOf(Style.Unit.PX.name().toLowerCase())));
		int height = getHeight();
		int size = Math.min(width, height);
		GWT.log("size = " + style.getWidth(), null);
		boardDiv.getStyle().setWidth(size, Style.Unit.PX);
		boardDiv.getStyle().setHeight(size, Style.Unit.PX);
		boardElt.setAttribute("width", size + Style.Unit.PX.name().toLowerCase());
		boardElt.setAttribute("height", size + Style.Unit.PX.name().toLowerCase());
//		div.getStyle().setWidth(value, unit);		
	}


	public final native Node importNode(Document doc, Node node, boolean deep) /*-{
		return doc.importNode(node, deep);
	}-*/;
	
	
	private void updateHistory() {
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
	}
	
	public void nextMove() {
		updateHistory();
		switch (engine.getBoard().isEndGame()) {
			case 1 :
				Window.alert(ChessConstants.INSTANCE.whitesWin());
				restart(null);
				break;
			case -1:
				Window.alert(ChessConstants.INSTANCE.blacksWin());
				restart(null);
				break;
			case 99:
				Window.alert(ChessConstants.INSTANCE.draw());
				restart(null);
				break;
			default:
				currentPlayerValueLabel.setText(engine.getBoard().getTurn() ? ChessConstants.INSTANCE.white() : ChessConstants.INSTANCE.black());
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
	
	private void computerMove() {
		DeferredCommand.addCommand(new Command() {
			@Override
			public void execute() {
				engine.go(moveTimes[moveTimeIndex]);
			}
		});
	}

	public void bestMove(int bestMove, int ponder) {
		GWT.log("SearchObserver.bestMove(" + Move.toStringExt(bestMove) + ", " + Move.toStringExt(ponder) + ")", null);
		engine.getBoard().doMove(bestMove);
//		History.newItem(Integer.toString(engine.getBoard().getMoveNumber()), false);
		board.update(false);
		nextMove();
	}

	public void info(SearchStatusInfo info) {
		GWT.log("SearchObserver.info(" + info + ")", null);
	}
	
	public void addUndoableMove() {
		History.newItem(Integer.toString(engine.getBoard().getMoveNumber()), false);
		undoableMoveCount++;
	}


	@UiHandler("restartButton")
	public void restart(ClickEvent event) {
		GWT.log("restart(" + event + ")", null);
		
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
		GWT.log("updateFen(" + event + ")", null);
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
		GWT.log("modeChange(" + event + ")", null);
		nextMove();
	}


}
