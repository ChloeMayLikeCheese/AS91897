package com.AS91897;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

public class TerminalHandler {
    Size terminalSize;
    File curDir;

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
        CREATE,
        EXIT,
        ESC
    }

    public TerminalHandler() throws IOException {
        curDir = new File("./").getAbsoluteFile();
        updateFilesAndDirs();
        while (true) {

            int selectedIndex = 0;
            int previousSelectedIndex = 0;
            int viewOffset = 0;

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
                keyMap.bind(Operation.COMMAND, "a", "b");
                keyMap.bind(Operation.CREATE, "c");
                keyMap.bind(Operation.EXIT, "q");
                keyMap.bind(Operation.ESC, "\033");

                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .build();

                boolean isReading = true;
                while (isReading) {
                    int terminalWidth = terminal.getWidth();
                    int terminalHeight = terminal.getHeight();

                    terminalSize = terminal.getSize();

                    terminal.puts(Capability.clear_screen);
                    terminal.writer().println(terminalWidth + "x" + terminalHeight);
                    terminalSizeCallBack(terminal);

                    int listHeight = terminalHeight - 3;
                    if (listHeight < 1) {
                        listHeight = 1;
                    }

                    if (selectedIndex < viewOffset) {
                        viewOffset = selectedIndex;
                    } else if (selectedIndex >= viewOffset + listHeight) {
                        viewOffset = selectedIndex - listHeight + 1;
                    }

                    int endIndex = Math.min(fileAndDirList.length, viewOffset + listHeight);

                    for (int i = viewOffset; i < endIndex; i++) {
                        if (i == selectedIndex) {
                            terminal.writer().print("\u001B[31m" + ">" + "\u001B[0m" + "\u001B[47m");
                        }
                        if (i >= dirList.length) {
                            if (i == selectedIndex) {
                                terminal.writer().println("\u001B[40m" + fileAndDirList[i].getName() + "\u001B[0m");
                            } else {
                                terminal.writer().println("\u001B[33m" + fileAndDirList[i].getName() + "\u001B[0m");
                            }

                        } else {
                            if (i == selectedIndex) {
                                terminal.writer().println("\u001B[40m" + fileAndDirList[i].getName() + "/ " + "\u001B[0m");
                            } else {
                                terminal.writer().println("\u001B[36m" + fileAndDirList[i].getName() + "/ " + "\u001B[0m");
                            }

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
                                File parentDir = curDir.getParentFile();
                                if (parentDir != null) {
                                    curDir = curDir.getParentFile();
                                    updateFilesAndDirs();
                                    selectedIndex = 0;
                                }
                                break;
                            case RIGHT:
                            case ENTER:
                                if (fileAndDirList.length != 0) {
                                    if (fileAndDirList[selectedIndex] != null) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile != null) {
                                            if (selectedFile.isDirectory()) {
                                                curDir = selectedFile;
                                                updateFilesAndDirs();
                                                selectedIndex = previousSelectedIndex;
                                            }
                                        }
                                    }
                                }

                                break;
                            case CREATE:
                            try {
                                String fileIn = lineReader.readLine("Enter File name: ");
                                if (Operation.ESC != null) {
                                    
                                }else{
                                    File file = new File(fileIn);
                                    file.createNewFile();
                                    updateFilesAndDirs();
                                }
                            } catch (Exception e) {

                            }

                                break;
                            case EXIT:
                                System.exit(0);
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
