package org.vectomatic.svg.chess;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ConfirmBox {
	interface RestartBinder extends UiBinder<VerticalPanel, ConfirmBox> {
	}
	private static RestartBinder restartBinder = GWT.create(RestartBinder.class);
	
	@UiField(provided=true)
	ChessConstants constants = ChessConstants.INSTANCE;
	@UiField
	Button confirmYesButton;
	@UiField
	Button confirmNoButton;
	DialogBox confirmBox;
	Main main;
	
	public static DialogBox createConfirmBox(Main main) {
		return new ConfirmBox(main).confirmBox;
	}
	
	private ConfirmBox(Main main) {
		this.main = main;
		confirmBox = new DialogBox();
		confirmBox.setTitle(constants.restart());
		confirmBox.setWidget(restartBinder.createAndBindUi(this));
	}
	
	@UiHandler("confirmYesButton")
	public void confirmYes(ClickEvent event) {
		confirmBox.hide();
        main.restart();
	}
	
	@UiHandler("confirmNoButton")
	public void confirmNo(ClickEvent event) {
		confirmBox.hide();
	}

}
