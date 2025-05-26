package com.AS91897;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

public class TerminalHandler {
    enum Operation {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        ENTER,
        COMMAND,
        EXIT
    }

    public TerminalHandler() throws IOException {
        while (true) {
            
            try (Terminal terminal = TerminalBuilder.builder()
                    .name("File Manager")
                    .jna(true)
                    .build()) {
                terminal.enterRawMode();


                BindingReader bindingReader = new BindingReader(terminal.reader());
                KeyMap<Operation> keyMap = new KeyMap<>();
                keyMap.bind(Operation.UP, "\033[A");
                keyMap.bind(Operation.DOWN, "\033[B");
                keyMap.bind(Operation.LEFT, "\033[D");
                keyMap.bind(Operation.RIGHT, "\033[C");
                keyMap.bind(Operation.ENTER, "\r", "\n");
                keyMap.bind(Operation.COMMAND, "a", "b", "c", "d");
                keyMap.bind(Operation.EXIT, "q");
    
                terminal.writer().println("Press arrow up/down/left/right, a/b/c/d, or 'q' to quit.");
                terminal.writer().flush();
                boolean isReading = true;
                while (isReading) {
                    Operation op = bindingReader.readBinding(keyMap, null, false);
                    if (op != null) {
                        if (op == Operation.EXIT) {
                            System.exit(0);
                        }
                        terminal.puts(Capability.clear_screen);
                        terminal.writer().println("read op: " + op);
                        terminal.writer().flush();
                        isReading = false;
                    }

                }
            }
            

        }

    }



}