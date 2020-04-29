package com.qiuxs.application;

import java.io.IOException;
import java.util.Iterator;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class Main extends Application {

	private static class MyChangeListener implements ChangeListener<State> {

		private WebEngine webEngine = null;
		private Stage stage;

		public MyChangeListener(WebEngine webEngine, Stage stage) {
			this.webEngine = webEngine;
			this.stage = stage;
		}

		@Override
		public void changed(ObservableValue<? extends State> observable, State oldState, State newState) {
			JSObject windowObj = (JSObject) webEngine.executeScript("window");
			windowObj.setMember("jsBridge", new JSBridge(stage));
			windowObj.setMember("console", new JSConsole());
		}

	}

	@Override
	public void start(Stage stage) {
		try {
			Scene scene = new Scene(new VBox(), 650, 600);
			VBox box = ((VBox) scene.getRoot());
			ButtonBar buttonBar = new ButtonBar();
			Button btnSubmitReview = new Button("提交Review");
			Button btnSetting = new Button("设置");
			ButtonBar.setButtonData(btnSubmitReview, ButtonData.LEFT);
			ButtonBar.setButtonData(btnSetting, ButtonData.LEFT);
			buttonBar.getButtons().add(btnSubmitReview);
			buttonBar.getButtons().add(btnSetting);
			box.getChildren().add(buttonBar);

			this.setReviewWebView(box, stage, "index.html");

			btnSetting.setOnAction(event -> {
				Main.this.setReviewWebView(box, stage, "config.html");
			});
			btnSubmitReview.setOnAction(event -> {
				Main.this.setReviewWebView(box, stage, "index.html");
			});

			stage.setTitle("ReviewBordTool");
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private void setReviewWebView(VBox box, Stage stage, String page) {
		final WebView browser = new WebView();
		final WebEngine webEngine = browser.getEngine();
		// scene.setRoot(browser);

		webEngine.getLoadWorker().stateProperty()
				.addListener(new MyChangeListener(webEngine, stage));
		webEngine.load(this.getClass().getResource("/com/qiuxs/html/" + page).toExternalForm());
		ObservableList<Node> children = box.getChildren();
		for (Iterator<Node> iter = children.iterator(); iter.hasNext();) {
			Node node = iter.next();
			if (node instanceof WebView) {
				iter.remove();
			}
		}
		children.add(browser);
	}

	public static void main(String[] args) throws IOException {
//		File logDir = new File("./logs");
//		if (!logDir.exists()) {
//			logDir.mkdirs();
//		}
//		File logFile = new File("./logs/log.txt");
//		if (!logFile.exists()) {
//			logFile.createNewFile();
//		}
//		File errFile = new File("./logs/err.txt");
//		if (!errFile.exists()) {
//			errFile.createNewFile();
//		}
//		System.setOut(new PrintStream(logFile));
//		System.setErr(new PrintStream(errFile));
		launch(args);
	}
}
