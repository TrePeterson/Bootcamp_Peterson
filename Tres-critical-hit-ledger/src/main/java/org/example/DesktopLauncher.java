package org.example;

import javafx.application.Application;

/** Standard Java entry point for starting the desktop interface from IntelliJ or Maven. */
public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(LedgerDesktopApp.class, args);
    }
}
