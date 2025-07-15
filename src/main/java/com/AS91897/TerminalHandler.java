package com.AS91897;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import sun.misc.Signal;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Attributes.ControlChar;
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
        CREATE,
        RENAME,
        DELETE,
        EXIT,
        HELP
    }

    public TerminalHandler() throws IOException, InterruptedException {
        boolean hasPrintedWelcome = false;
        curDir = new File("./").getAbsoluteFile().getParentFile();
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
                // Attributes a = terminal.getAttributes();
                // a.setControlChar(ControlChar.VMIN, 0);
                // a.setControlChar(ControlChar.VTIME, 1);
                // terminal.setAttributes(a);

                BindingReader bindingReader = new BindingReader(terminal.reader());
                KeyMap<Operation> keyMap = new KeyMap<>();
                keyMap.bind(Operation.UP, "\033[A");
                keyMap.bind(Operation.DOWN, "\033[B");
                keyMap.bind(Operation.LEFT, "\033[D");
                keyMap.bind(Operation.RIGHT, "\033[C");
                keyMap.bind(Operation.ENTER, "\r", "\n");
                keyMap.bind(Operation.CREATE, "c");
                keyMap.bind(Operation.RENAME, "r");
                keyMap.bind(Operation.EXIT, "q");
                keyMap.bind(Operation.HELP, "h");
                keyMap.bind(Operation.DELETE, "d", "\u007f");
                keyMap.setAmbiguousTimeout(50);
                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .build();

                boolean isReading = true;
                while (isReading) {
                    // int terminalWidth = terminal.getWidth();
                    int terminalHeight = terminal.getHeight();
                    // terminalSize = terminal.getSize();

                    terminal.puts(Capability.clear_screen);

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
                            terminal.writer()
                                    .print(SetColour.set(" >", 234, 139, 168));
                        }
                        if (i >= dirList.length) {
                            if (i == selectedIndex) {
                                terminal.writer().println(SetColour
                                        .setBG(SetColour.set(fileAndDirList[i].getName(), 49, 50, 68),
                                                186, 192,
                                                222));
                            } else {
                                terminal.writer().println(
                                        SetColour.set(fileAndDirList[i].getName(), 245, 194, 231));
                            }

                        } else {
                            if (i == selectedIndex) {
                                terminal.writer()
                                        .println(SetColour
                                                .setBG(SetColour.set(fileAndDirList[i].getName() + "/", 49, 50, 68),
                                                        186, 192,
                                                        222));
                            } else {
                                terminal.writer()
                                        .println(SetColour.set(fileAndDirList[i].getName() + "/", 180, 190, 245));
                            }

                        }
                    }

                    if (!hasPrintedWelcome) {
                        terminal.writer().println(SetColour.set(
                                "Welcome! Press arrow up/down/left/right to navigate, h for help , or 'q' to quit.",
                                203, 166, 247));
                        hasPrintedWelcome = true;
                    }

                    terminal.writer().println(getAllFiles());
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
                                    previousSelectedIndex = selectedIndex;
                                    selectedIndex = 0;

                                }

                                break;
                            case RIGHT:
                            case ENTER:
                                if (fileAndDirList.length != 0) {
                                    if (fileAndDirList[selectedIndex] != null) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile.isDirectory()) {
                                            curDir = selectedFile;
                                            updateFilesAndDirs();
                                            selectedIndex = previousSelectedIndex;
                                        }
                                    }
                                }

                                break;
                            case CREATE:
                                try {
                                    String fileIn = lineReader.readLine(
                                            SetColour.set(
                                                    "Press CRTL+C to quit, Press enter to confirm, type a '/' at the end to make it a directory\nEnter file name: ",
                                                    203, 166, 247))
                                            .strip();
                                    if (fileIn != "") {
                                        if (fileIn.endsWith("/")) {
                                            File dir = new File(curDir.getAbsolutePath() + "/" + fileIn);
                                            if (!dir.exists()) {
                                                dir.mkdirs();
                                                updateFilesAndDirs();
                                            } else {
                                                terminal.writer()
                                                        .println(SetColour.set("File or directory already exsists", 243,
                                                                139, 168));
                                                terminal.writer().flush();
                                                Thread.sleep(500);
                                            }
                                        } else {
                                            File file = new File(curDir.getAbsolutePath() + "/" + fileIn);
                                            if (!file.exists()) {
                                                file.createNewFile();
                                                updateFilesAndDirs();
                                            } else {
                                                terminal.writer()
                                                        .println(SetColour.set("File or directory already exsists", 243,
                                                                139, 168));
                                                terminal.writer().flush();
                                                Thread.sleep(500);
                                            }

                                        }

                                    }

                                } catch (UserInterruptException e) {
                                    terminal.writer()
                                            .println(SetColour.set("Exited file creation", 243, 139, 168));
                                    terminal.writer().flush();
                                    Thread.sleep(500);

                                } catch (IOException e) {
                                    // Do nothing
                                }

                                break;
                            case RENAME:
                                if (fileAndDirList.length != 0) {
                                    if (fileAndDirList[selectedIndex] != null) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile != null) {
                                            try {
                                                String fileIn = lineReader.readLine(
                                                        SetColour.set(
                                                                "Press CRTL+C to quit, Press enter to confirm\nEnter what you want to rename the file/folder to (Current name: "
                                                                        + selectedFile.getName() + "):",
                                                                203, 166, 247))
                                                        .strip();
                                                if (fileIn != "") {
                                                    selectedFile.renameTo(new File(fileIn));
                                                    updateFilesAndDirs();

                                                }

                                            } catch (UserInterruptException e) {
                                                terminal.writer()
                                                        .println(SetColour.set("Exited file rename", 243, 139, 168));
                                                terminal.writer().flush();
                                                Thread.sleep(500);

                                            }
                                        }
                                    }
                                }
                                break;
                            case DELETE:
                                if (fileAndDirList.length != 0) {
                                    if (fileAndDirList[selectedIndex] != null) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile != null) {
                                            try {
                                                if (selectedFile.isDirectory()
                                                        && selectedFile.listFiles() != null) {
                                                    String deleteConfirm = lineReader.readLine(
                                                            SetColour.set(
                                                                    "Press CRTL+C to quit, Press enter to confirm\nAre you sure? this will permanently delete '"
                                                                            + selectedFile.getName()
                                                                            + "' and all its contents(y/n)",
                                                                    203, 166, 247))
                                                            .strip().toLowerCase();
                                                    if (deleteConfirm.equals("y")
                                                            || deleteConfirm.equals("yes")) {
                                                        deleteDir(selectedFile, terminal);
                                                    }

                                                } else {
                                                    String deleteConfirm = lineReader.readLine(
                                                            SetColour.set(
                                                                    "Press CRTL+C to quit, Press enter to confirm\nAre you sure? this will permanently delete '"
                                                                            + selectedFile.getName() + "'(y/n)",
                                                                    203, 166, 247))
                                                            .strip().toLowerCase();
                                                    if (deleteConfirm.equals("y") || deleteConfirm.equals("yes")) {
                                                        if (selectedFile.delete()) {
                                                            updateFilesAndDirs();
                                                            terminal.writer()
                                                                    .println(SetColour.set(
                                                                            selectedFile.getAbsoluteFile()
                                                                                    + " deleted succsessfully",
                                                                            243, 139, 168));
                                                            terminal.writer().flush();
                                                            Thread.sleep(500);
                                                        } else {
                                                            terminal.writer()
                                                                    .println(SetColour.set(
                                                                            "Failed to delete: "
                                                                                    + selectedFile.getAbsoluteFile(),
                                                                            243, 139, 168));
                                                            terminal.writer().flush();
                                                            Thread.sleep(500);
                                                        }

                                                    }
                                                }
                                            } catch (UserInterruptException e) {
                                                terminal.writer()
                                                        .println(SetColour.set("Exited file deletion", 243, 139, 168));
                                                terminal.writer().flush();
                                                Thread.sleep(500);

                                            }
                                        }
                                    }
                                }
                                break;
                            case EXIT:
                                System.exit(0);
                                break;
                            case HELP:

                                AtomicBoolean sleeping = new AtomicBoolean(true);
                                Object sleeper = new Object();
                                terminal.puts(Capability.clear_screen);
                                terminal.writer().println(SetColour.set(
                                        "Help:\n Press c to create a file\n Press h for help\n Press left or right arrows to navigate to the previous or next directory\n Press up or down to navigate up or down the folder list\n Press Crtl+C to exit menus like this or q to exit the program",
                                        203, 166, 247));
                                terminal.writer().flush();

                                Signal sig = new Signal("INT");
                                sun.misc.SignalHandler oldHandler = Signal.handle(sig, signal -> {
                                    sleeping.set(false);
                                    terminal.writer().println(SetColour.set("Exited help menu", 243, 139, 168));
                                    terminal.writer().flush();
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {

                                    }
                                    synchronized (sleeper) {
                                        sleeper.notify();
                                    }
                                });

                                while (sleeping.get()) {
                                    synchronized (sleeper) {
                                        try {
                                            sleeper.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace(); // for degug dont forgor to remove to not spam errors
                                        }
                                    }
                                }

                                Signal.handle(sig, oldHandler);

                                break;
                            default:
                                break;
                        }
                    }

                }

            }
        }

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

    public String getAllFiles() {
        String allFiles = null;

        if (curDir.getParentFile() != null) {
            allFiles = curDir.getAbsolutePath();
            String[] allFilesArray = allFiles.split("/");
            allFilesArray[allFilesArray.length - 1] = SetColour
                    .setBG(SetColour.set(allFilesArray[allFilesArray.length - 1], 49, 50, 68), 186, 192, 222);
            for (int i = 0; i < allFilesArray.length - 1; i++) {
                allFilesArray[i] = SetColour.set(allFilesArray[i] + "/", 203, 166, 247);

            }
            allFiles = Arrays.toString(allFilesArray);
            allFiles = allFiles.replaceAll(", ", "");
        } else {
            allFiles = SetColour.set("/", 203, 166, 247);
        }

        return allFiles;
    }

    public void deleteDir(File file, Terminal terminal) throws InterruptedException {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f, terminal);
            }
        }
        if (file.delete()) {
            terminal.writer()
                    .println(SetColour.set("Deleted file: '" + file.getAbsolutePath() + "'", 243, 139, 168));
            terminal.writer().flush();
            Thread.sleep(500);
        }
        updateFilesAndDirs();
    }

}
