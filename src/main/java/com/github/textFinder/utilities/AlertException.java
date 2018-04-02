package com.github.textFinder.utilities;

import javafx.beans.NamedArg;
import javafx.scene.control.Alert;

/**
 * Created by maxtar.
 */
public class AlertException extends Alert {

    public AlertException(@NamedArg("alertType") AlertType alertType, String alertText) {
        super(alertType, alertText);
        setHeaderText(null);
        setTitle(alertType.toString());
        showAndWait();
    }
}
