package org.example;

import javafx.application.Application;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Starts the desktop interface from Maven or the self-contained Windows launcher. */
public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(LedgerDesktopApp.class, configureLedgerPath(args));
    }

    /** Keeps portable installations separate from a developer's working directory. */
    static String[] configureLedgerPath(String[] args) {
        List<String> applicationArguments = new ArrayList<>();
        String selectedFile = null;
        for (String argument : args) {
            if (argument.startsWith("--ledger-file=")) {
                if (selectedFile != null) {
                    throw new IllegalArgumentException("Use --ledger-file only once.");
                }
                String value = argument.substring("--ledger-file=".length()).trim();
                if (value.isEmpty() || !Path.of(value).isAbsolute()) {
                    throw new IllegalArgumentException("--ledger-file must contain an absolute file path.");
                }
                selectedFile = Path.of(value).normalize().toString();
            } else {
                applicationArguments.add(argument);
            }
        }

        // An explicit launcher argument takes priority over a configured JVM property.
        if (selectedFile != null) {
            System.setProperty("ledger.file", selectedFile);
        } else if (Boolean.getBoolean("ledger.packaged") && System.getProperty("ledger.file") == null) {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path userData = localAppData == null || localAppData.isBlank()
                    ? Path.of(System.getProperty("user.home"), "AppData", "Local")
                    : Path.of(localAppData);
            System.setProperty("ledger.file", userData.resolve("PotaraFusionLedger")
                    .resolve("transactions.csv").toAbsolutePath().normalize().toString());
        }
        return applicationArguments.toArray(String[]::new);
    }
}
