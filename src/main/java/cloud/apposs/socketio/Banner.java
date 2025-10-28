package cloud.apposs.socketio;

import java.io.PrintStream;

public class Banner {
    private static final String[] BANNER = {
            "________           ______      ___________________ \n" +
            "__  ___/______________  /________  /____  _/_  __ \\\n" +
            "_____ \\_  __ \\  ___/_  //_/  _ \\  __/__  / _  / / /\n" +
            "____/ // /_/ / /__ _  ,<  /  __/ /_ __/ /  / /_/ / \n" +
            "/____/ \\____/\\___/ /_/|_| \\___/\\__/ /___/  \\____/"
    };
    private static final String CLOUDX_BOOT = " :: CloudX SocketIO :: ";
    private static final int STRAP_LINE_SIZE = 38;

    public void printBanner(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAP_LINE_SIZE - (SocketIOConstants.VERSION.length() + CLOUDX_BOOT.length())) {
            padding.append(" ");
        }
        printStream.println(CLOUDX_BOOT + padding.toString() + SocketIOConstants.VERSION);
        printStream.println();
        printStream.flush();
    }
}
