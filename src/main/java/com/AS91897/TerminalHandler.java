package com.AS91897;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.utils.InfoCmp.Capability;

public class TerminalHandler {
    Size terminalSize;
    File curDir;
    // File parentDir = curDir.getParentFile();
    File[] dirList;
    File[] fileList;
    File[] fileAndDirList;

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
        curDir = new File("./").getAbsoluteFile();
        updateFilesAndDirs();
        while (true) {
            // updateFilesAndDirs();
            int selectedIndex = 0;

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

                boolean isReading = true;
                while (isReading) {
                    int terminalWidth = terminal.getWidth();
                    int terminalHeight = terminal.getHeight();

                    terminalSize = terminal.getSize();

                    terminal.puts(Capability.clear_screen);
                    terminal.writer().println(terminalWidth + "x" + terminalHeight);
                    terminalSizeCallBack(terminal);

                    // terminal.writer().println("Directories:");
                    for (int i = 0; i < fileAndDirList.length; i++) {
                        // if (i == fileList.length) {
                        // terminal.writer().println("Files:");
                        // }
                        if (i == selectedIndex) {
                            terminal.writer().print("\u001B[31m"+">"+"\u001B[0m"+"\u001B[47m");
                        }
                        if (i >= dirList.length) {
                            terminal.writer().println("\u001B[33m"+fileAndDirList[i].getName()+"\u001B[0m");
                        } else {
                            terminal.writer().println("\u001B[36m"+fileAndDirList[i].getName()+ "/ "+"\u001B[0m");
                        }
                    }

                    terminal.writer().println("Press arrow up/down/left/right, a/b/c/d, or 'q' to quit.");
                    terminal.writer().flush();

                    Operation op = bindingReader.readBinding(keyMap, null, false);
                    if (op != null) {
                        switch (op) {
                            case UP:
                                if (selectedIndex > 0) {
                                    selectedIndex--;
                                }

                                break;
                            case DOWN:
                                if (selectedIndex < fileAndDirList.length - 1) {
                                    selectedIndex++;
                                }

                                break;
                            case LEFT:
                                curDir = curDir.getParentFile();
                                updateFilesAndDirs();
                                selectedIndex = 0;
                                break;
                            case EXIT:
                                isReading = false;
                                break;
                            default:

                                break;
                        }
                    }

                }

            }
        }

    }

    public void setTerminalSizeVariable(Size terminalSize) {
        this.terminalSize = terminalSize;

    }

    public void terminalSizeCallBack(Terminal terminal) {
        terminal.puts(Capability.clear_screen);
        terminal.writer().println("DEBUG: Screen cleared(Screen size update)");
    }

    public void updateFilesAndDirs() {
        dirList = curDir.listFiles(new FilenameFilter() {
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });

        fileList = curDir.listFiles(new FilenameFilter() {
            public boolean accept(File current, String name) {
                return new File(current, name).isFile();
            }
        });
        fileAndDirList = new File[dirList.length + fileList.length];

        for (int i = 0; i < dirList.length; i++) {
            fileAndDirList[i] = dirList[i];
        }
        for (int i = 0; i < fileList.length; i++) {
            fileAndDirList[dirList.length + i] = fileList[i];
        }

    }
}