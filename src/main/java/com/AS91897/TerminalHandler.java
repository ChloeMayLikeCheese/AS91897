package com.AS91897;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import sun.misc.Signal;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
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
    Map<File, Integer> selectionHistory = new HashMap<>();
    int selectedIndex;

    enum Operation {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        ENTER,
        CREATE,
        RENAME,
        DELETE,
        SEARCH,
        EXIT,
        HELP,
        REFRESH
    }

    public TerminalHandler() throws IOException, InterruptedException {
        boolean hasPrintedWelcome = false;
        curDir = new File("./").getAbsoluteFile().getParentFile();
        updateFilesAndDirs();

        while (true) {
            selectedIndex = selectionHistory.getOrDefault(curDir, 0);
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
                keyMap.bind(Operation.CREATE, "c");
                keyMap.bind(Operation.RENAME, "r");
                keyMap.bind(Operation.DELETE, "d", "\u007f");
                keyMap.bind(Operation.EXIT, "q");
                keyMap.bind(Operation.HELP, "h");
                keyMap.bind(Operation.SEARCH, "s");
                keyMap.bind(Operation.REFRESH, "f");
                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .build();

                boolean isReading = true;
                while (isReading) {
                    int terminalHeight = terminal.getHeight();

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
                                    selectionHistory.put(curDir, selectedIndex);
                                    curDir = parentDir;
                                    updateFilesAndDirs();
                                    selectedIndex = selectionHistory.getOrDefault(curDir, 0);
                                }

                                break;
                            case RIGHT:
                            case ENTER:

                                if (fileAndDirList != null) {
                                    if (fileAndDirList.length != 0) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile != null) {
                                            if (selectedFile.canRead()) {
                                                if (selectedFile.isDirectory()) {
                                                    selectionHistory.put(curDir, selectedIndex);
                                                    curDir = selectedFile;
                                                    updateFilesAndDirs();
                                                    selectedIndex = selectionHistory.getOrDefault(curDir, 0);
                                                }
                                            } else {

                                            }

                                        }
                                    }
                                }

                                break;
                            case CREATE:
                                try {
                                    String fileIn = inputReader(terminal, lineReader,
                                            "Press CRTL+C to quit, Press enter to confirm, type a '/' at the end to make it a directory\nEnter file name: ",
                                            "Exited file creation");
                                    if (fileIn != "" && fileIn != null) {
                                        if (fileIn.endsWith("/")) {
                                            File dir = new File(curDir.getAbsolutePath() + "/" + fileIn);
                                            if (!dir.exists()) {
                                                dir.mkdirs();
                                                updateFilesAndDirs();
                                            } else {
                                                printError(terminal, "File or directory already exsists");
                                            }
                                        } else {
                                            File file = new File(curDir.getAbsolutePath() + "/" + fileIn);
                                            if (!file.exists()) {
                                                file.createNewFile();
                                                updateFilesAndDirs();
                                            } else {
                                                printError(terminal, "File or directory already exsists");
                                            }

                                        }

                                    }

                                } catch (IOException e) {
                                    // Do nothing
                                }

                                break;
                            case RENAME:
                                if (fileAndDirList.length != 0) {
                                    if (fileAndDirList[selectedIndex] != null) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile != null) {
                                            String fileIn = inputReader(terminal, lineReader,
                                                    "Press CRTL+C to quit, Press enter to confirm\nEnter what you want to rename the file/folder to (Current name: "
                                                            + selectedFile.getAbsolutePath() + "): ",
                                                    "Exited file rename");
                                            if (fileIn != null) {
                                                String[] path = selectedFile.getAbsolutePath().split(File.separator);
                                                path[path.length - 1] = fileIn;
                                                String joinedPath = String.join(",", path);
                                                joinedPath = joinedPath.replaceAll(",", "/");

                                                File targetFile = new File(joinedPath);
                                                if (fileIn != "") {
                                                    if (targetFile.exists()) {
                                                        printError(terminal,
                                                                "Failed to rename: '" + selectedFile.getAbsoluteFile()
                                                                        + "' File already exsists");

                                                    } else {
                                                        selectedFile.renameTo(new File(joinedPath));
                                                        updateFilesAndDirs();

                                                    }

                                                }
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
                                            if (selectedFile.isDirectory()
                                                    && selectedFile.listFiles() != null) {

                                                String deleteConfirm = inputReader(terminal, lineReader,
                                                        "Press CRTL+C to quit, Press enter to confirm\nAre you sure? this will permanently delete '"
                                                                + selectedFile.getName()
                                                                + "' and all its contents(y/n)",
                                                        "Exited file deletion");
                                                if (deleteConfirm != null) {
                                                    if (deleteConfirm.equals("y")
                                                            || deleteConfirm.equals("yes")) {
                                                        deleteDir(selectedFile, terminal);
                                                    }
                                                }

                                            } else {
                                                String deleteConfirm = inputReader(terminal, lineReader,
                                                        "Press CRTL+C to quit, Press enter to confirm\nAre you sure? this will permanently delete '"
                                                                + selectedFile.getName() + "'(y/n)",
                                                        "Exited file deletion");
                                                if (deleteConfirm != null) {
                                                    if (deleteConfirm.equals("y") || deleteConfirm.equals("yes")) {
                                                        if (selectedFile.delete()) {
                                                            updateFilesAndDirs();
                                                            printError(terminal, selectedFile.getAbsoluteFile()
                                                                    + " deleted succsessfully");
                                                        } else {
                                                            printError(terminal, "Failed to delete: "
                                                                    + selectedFile.getAbsoluteFile());
                                                        }

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            case SEARCH:
                                boolean searching = true;
                                while (searching) {
                                    search(lineReader, terminal);
                                    searching = false;
                                }
                                break;
                            case EXIT:
                                System.exit(0);
                                break;
                            case HELP:
                                terminal.puts(Capability.clear_screen);
                                terminal.writer().println(SetColour.set(
                                        "Help:\n Press c to create a file, put a / at the end of the name to make it a directory\n Press h for help\n Press r to rename a file or folder\n Press left or right arrows to navigate to the previous or next directory\n Press up or down to navigate up or down the folder list\n Press Backspace or d to delete a file or folder\n Press s to search and f to refresh folder list\nPress Crtl+C to exit menus like this or q to exit the program",
                                        203, 166, 247));
                                terminal.writer().flush();
                                AtomicBoolean sleeping = new AtomicBoolean(true);
                                Object sleeper = new Object();
                                Signal sig = new Signal("INT");
                                sun.misc.SignalHandler oldHandler = Signal.handle(sig, signal -> {
                                    sleeping.set(false);
                                    try {
                                        printError(terminal, "Exited help menu");
                                    } catch (InterruptedException e) {
                                    }
                                    synchronized (sleeper) {
                                        sleeper.notify();
                                    }
                                });

                                while (sleeping.get()) {
                                    synchronized (sleeper) {
                                        sleeper.wait();
                                    }
                                }

                                Signal.handle(sig, oldHandler);

                                break;
                            case REFRESH:
                                updateFilesAndDirs();
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
        if (dirList == null)
            dirList = new File[0];

        fileList = curDir.listFiles(new FilenameFilter() {
            public boolean accept(File current, String name) {
                return new File(current, name).isFile();
            }
        });
        if (fileList == null)
            dirList = new File[0];

        ArrayList<String> dirSorter = new ArrayList<String>();
        for (int i = 0; i < dirList.length; i++) {
            dirSorter.add(dirList[i].toString());
        }
        dirSorter.sort(null);
        for (int i = 0; i < dirList.length; i++) {
            dirList[i] = new File(dirSorter.get(i));
        }

        ArrayList<String> fileSorter = new ArrayList<String>();
        for (int i = 0; i < fileList.length; i++) {
            fileSorter.add(fileList[i].toString());
        }
        fileSorter.sort(null);
        for (int i = 0; i < fileList.length; i++) {
            fileList[i] = new File(fileSorter.get(i));
        }

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
            String[] allFilesArray = allFiles.split(File.separator);
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
        file.delete();
        updateFilesAndDirs();
    }

    public void search(LineReader lineReader, Terminal terminal) throws InterruptedException {
        selectedIndex = 0;
        String searchIn = inputReader(terminal, lineReader, "Enter file name to search: ", "Exited search");
        updateFilesAndDirs();
        if (searchIn != null) {
            dirList = curDir.listFiles(new FilenameFilter() {
                public boolean accept(File current, String name) {
                    if (!name.contains(searchIn)) {
                        name = null;
                    }
                    if (name != null) {
                        return new File(current, name).isDirectory();
                    } else {
                        return false;
                    }

                }
            });

            fileList = curDir.listFiles(new FilenameFilter() {
                public boolean accept(File current, String name) {
                    if (!name.contains(searchIn)) {
                        name = null;
                    }
                    if (name != null) {
                        return new File(current, name).isFile();
                    } else {
                        return false;
                    }

                }
            });

            if (dirList == null)
                dirList = new File[0];
            if (fileList == null)
                dirList = new File[0];

            ArrayList<String> dirSorter = new ArrayList<String>();
            for (int i = 0; i < dirList.length; i++) {
                dirSorter.add(dirList[i].toString());
            }
            dirSorter.sort(null);
            for (int i = 0; i < dirList.length; i++) {
                dirList[i] = new File(dirSorter.get(i));
            }

            ArrayList<String> fileSorter = new ArrayList<String>();
            for (int i = 0; i < fileList.length; i++) {
                fileSorter.add(fileList[i].toString());
            }
            fileSorter.sort(null);
            for (int i = 0; i < fileList.length; i++) {
                fileList[i] = new File(fileSorter.get(i));
            }

            fileAndDirList = new File[dirList.length + fileList.length];
            for (int i = 0; i < dirList.length; i++) {
                fileAndDirList[i] = dirList[i];
            }
            for (int i = 0; i < fileList.length; i++) {
                fileAndDirList[dirList.length + i] = fileList[i];
            }
        }

    }

    public void printError(Terminal terminal, String message) throws InterruptedException {
        terminal.writer().println(SetColour.set(message, 243, 139, 168));
        terminal.writer().flush();
        Thread.sleep(500);
    }

    public String inputReader(Terminal terminal, LineReader lineReader, String prompt, String exitMessage)
            throws InterruptedException {
        String in = null;
        try {
            in = lineReader.readLine(SetColour.set(prompt, 203, 166, 247)).strip();
        } catch (UserInterruptException e) {
            printError(terminal, exitMessage);
        }
        return in;

    }

}
