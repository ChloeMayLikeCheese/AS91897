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
            File curDir = new File(".");
            File[] dirList = curDir.listFiles(new FilenameFilter() {
                public boolean accept(File current, String name) {
                    return new File(current, name).isDirectory();
                }
            });

            File[] fileList = curDir.listFiles(new FilenameFilter() {
                public boolean accept(File current, String name) {
                    return new File(current, name).isFile();
                }
            });
            File[] fileAndDirList = new File[dirList.length + fileList.length];

            for (int i = 0; i < dirList.length; i++) {
                fileAndDirList[i] = dirList[i];
            }
            for (int i = 0; i < fileList.length; i++) {
                fileAndDirList[dirList.length + i] = fileList[i];
            }
            
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
                    terminal.puts(Capability.clear_screen);
                    terminal.writer().println("Directories:");
                    for (int i = 0; i < fileAndDirList.length; i++) {
                        if (i == fileList.length) {
                            terminal.writer().println("Files:");
                        }
                        if (i == selectedIndex) {
                            if (selectedIndex >= dirList.length) {
                                terminal.writer().println(" > File: " + fileAndDirList[i].getName());
                            }else{
                                terminal.writer().println(" > Dir: " + fileAndDirList[i].getName());
                            }
                            
                        } else {
                            if (i >= dirList.length) {
                                terminal.writer().println("  File: " + fileAndDirList[i].getName());
                            }else{
                                terminal.writer().println("  Dir: " + fileAndDirList[i].getName());
                            }
                        }
                    }


                    terminal.writer().println("Press arrow up/down/left/right, a/b/c/d, or 'q' to quit.");
                    terminal.writer().flush();

                    Operation op = bindingReader.readBinding(keyMap, null, false);
                    if (op != null) {
                        switch (op) {
                            case UP:
                                if (selectedIndex > 0){
                                    selectedIndex--;
                                }
                                    
                                break;
                            case DOWN:
                                if (selectedIndex < fileAndDirList.length - 1){
                                    selectedIndex++;
                                }
                                    
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
}