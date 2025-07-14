package com.AS91897;

import java.io.IOException;
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        new TerminalHandler();
        // while (true) {
        //     AtomicBoolean sizeChanged = new AtomicBoolean(true);
        //     TerminalHandler.terminal.handle(Terminal.Signal.WINCH, signal -> sizeChanged.set(true));

        //     if (sizeChanged.getAndSet(false)) {
        //         terminal.puts(Capability.clear_screen);
        //         terminal.flush();
        //     }
        // }
    }

}
