/* 
 * Author: Chloe T
 * Date: 07/08/2025
 * Purpose of program: Manage files and directorys
 */
package com.AS91897;

//Imports
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

public final class Main {
    static File curDir; // Declare the current directory variable
    static File[] dirList; // Declare file arrays
    static File[] fileList;
    static File[] fileAndDirList;
    static Map<File, Integer> selectionHistory = new HashMap<>(); // Create the HashMap for selection history
    static int selectedIndex; // Declare int for the selected index in the file list

    // Declare keybinds in enum
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

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean hasPrintedWelcome = false; // Boolean for knowing when the welcome message has been printed
        curDir = new File("./").getAbsoluteFile().getParentFile(); // Get the current directory
        updateFilesAndDirs(); // Update the list of files and directorys

        while (true) {
            selectedIndex = selectionHistory.getOrDefault(curDir, 0); // Remember the selected index
            int viewOffset = 0; // Declare the variable for the veiw offset

            try (Terminal terminal = TerminalBuilder.builder() // Create the terminal
                    .name("File Manager") // Name the terminal
                    .jansi(true) // Enable jansi for more compatibility with ANSI
                    .build()) { // Build the terminal
                terminal.enterRawMode(); // Set the terminal to raw

                BindingReader bindingReader = new BindingReader(terminal.reader()); // Set up the BindingReader
                KeyMap<Operation> keyMap = new KeyMap<>(); // Set up the KeyMap
                // Set up keys in the KeyMap
                keyMap.bind(Operation.UP, "\033[A", "i");
                keyMap.bind(Operation.DOWN, "\033[B", "k");
                keyMap.bind(Operation.LEFT, "\033[D", "j");
                keyMap.bind(Operation.RIGHT, "\033[C", "l");
                keyMap.bind(Operation.ENTER, "\r", "\n");
                keyMap.bind(Operation.CREATE, "c");
                keyMap.bind(Operation.RENAME, "r");
                keyMap.bind(Operation.DELETE, "d", "\u007f");
                keyMap.bind(Operation.EXIT, "q");
                keyMap.bind(Operation.HELP, "h");
                keyMap.bind(Operation.SEARCH, "s");
                keyMap.bind(Operation.REFRESH, "f");
                LineReader lineReader = LineReaderBuilder.builder() // Set up the LineReader for reading input
                        .terminal(terminal)
                        .build();

                boolean isReading = true; // Loop for reading input
                while (isReading) {

                    int terminalHeight = terminal.getHeight(); // Get the terminal height

                    terminal.puts(Capability.clear_screen); // Clear the screen

                    if (!hasPrintedWelcome) {
                        terminal.writer().println(SetColour.setBG(SetColour.set( // Print the welcome message
                                "Welcome! Press 'h' for help , or 'q' to quit.",
                                49, 50, 68), 186, 192, 222));
                        hasPrintedWelcome = true; // Make sure it doesn't get printed again
                    }

                    int listHeight = terminalHeight - 3; // Calculate how many items can fit on screen
                    if (listHeight < 1) {
                        listHeight = 1;// Make sure at least 1 is visible
                    }

                    // Scroll view up or down depending on where the selection is
                    if (selectedIndex < viewOffset) {
                        viewOffset = selectedIndex;
                    } else if (selectedIndex >= viewOffset + listHeight) {
                        viewOffset = selectedIndex - listHeight + 1;
                    }

                    int endIndex = Math.min(fileAndDirList.length, viewOffset + listHeight); // Prevent array overflow

                    for (int i = viewOffset; i < endIndex; i++) {
                        if (i == selectedIndex) {
                            terminal.writer()
                                    .print(SetColour.set(" >", 234, 139, 168)); // Draw selection arrow
                        }
                        if (i >= dirList.length) { // Display files after directories
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
                            if (i == selectedIndex) { // Display directories first
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

                    terminal.writer().println(directoryBar()); // Print the directory bar
                    terminal.writer()
                            .println(SetColour.setBG(
                                    SetColour.set(String.valueOf(selectedIndex + 1) + "/" + fileAndDirList.length, 49,
                                            50, 68),
                                    186, 192, 222)); // Print the file counter
                    terminal.writer().flush();

                    Operation op = bindingReader.readBinding(keyMap, null, false); // Read the binding
                    if (op != null) {
                        switch (op) {
                            case UP -> {
                                if (selectedIndex > 0) { // Move the selected index up if it is not 0
                                    selectedIndex--;
                                }
                            }
                            case DOWN -> {
                                if (selectedIndex < fileAndDirList.length - 1) { // Move the selected index down
                                    selectedIndex++;
                                }
                            }
                            case LEFT -> {
                                File parentDir = curDir.getParentFile();
                                if (parentDir != null) { // Move up to the parent directory if it isn't null
                                    selectionHistory.put(curDir, selectedIndex);
                                    curDir = parentDir;
                                    updateFilesAndDirs(); // Update to ensure that files print properly
                                    selectedIndex = selectionHistory.getOrDefault(curDir, 0); // Remember selected index
                                }
                            }
                            case RIGHT, ENTER -> {
                                if (fileAndDirList != null) { // Check if the directory is null
                                    if (fileAndDirList.length != 0) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile != null) { // Check it again
                                            if (selectedFile.canRead()) { // Check if you can read the directory before
                                                                          // entering
                                                if (selectedFile.isDirectory()) {
                                                    selectionHistory.put(curDir, selectedIndex);
                                                    curDir = selectedFile;
                                                    updateFilesAndDirs();
                                                    selectedIndex = selectionHistory.getOrDefault(curDir, 0); // Remember
                                                                                                              // selected
                                                                                                              // index
                                                                                                              // I love
                                                                                                              // autoformatting
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                            case CREATE -> {
                                try {
                                    String fileIn = inputReader(terminal, lineReader,
                                            "Press CRTL+C to quit, Press enter to confirm, type a '/' at the end to make it a directory\nEnter file name: ",
                                            "Exited file creation");
                                    if (!"".equals(fileIn) && fileIn != null) {
                                        if (fileIn.endsWith("/")) { // Create directory
                                            File dir = new File(curDir.getAbsolutePath() + "/" + fileIn);
                                            if (!dir.exists()) { // Make sure the directory doesn't exsist already
                                                                 // before creating it
                                                dir.mkdirs();
                                                updateFilesAndDirs();
                                            } else {
                                                printError(terminal, "File or directory already exsists");
                                            }
                                        } else { // Create file
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
                                }
                            }
                            case RENAME -> {
                                if (fileAndDirList.length != 0) { // Check if the directory is empty or null
                                    if (fileAndDirList[selectedIndex] != null) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile != null) {
                                            String fileIn = inputReader(terminal, lineReader,
                                                    "Press CRTL+C to quit, Press enter to confirm\nEnter what you want to rename the file/folder to (Current name: "
                                                            + selectedFile.getAbsolutePath() + "): ",
                                                    "Exited file rename");
                                            if (fileIn != null) {
                                                // Change replace the old name with the new one in the path and then
                                                // rejoin the path and the file
                                                String[] path = selectedFile.getAbsolutePath()
                                                        .split(Pattern.quote(File.separator));
                                                path[path.length - 1] = fileIn;
                                                String joinedPath = String.join(",", path);
                                                joinedPath = joinedPath.replaceAll(",", "/");

                                                File targetFile = new File(joinedPath);
                                                if (!"".equals(fileIn)) {
                                                    if (targetFile.exists()) { // Check if the file you want to rename
                                                                               // to already exsists
                                                        printError(terminal,
                                                                "Failed to rename: '" + selectedFile.getAbsoluteFile()
                                                                        + "' File already exsists");

                                                    } else {
                                                        selectedFile.renameTo(new File(joinedPath)); // Rename the file
                                                        updateFilesAndDirs();

                                                    }

                                                }
                                            }

                                        }
                                    }
                                }
                            }
                            case DELETE -> {
                                if (fileAndDirList.length != 0) { // Check if null (same as the past like 3 times)
                                    if (fileAndDirList[selectedIndex] != null) {
                                        File selectedFile = fileAndDirList[selectedIndex];
                                        if (selectedFile != null) {
                                            if (selectedFile.isDirectory()
                                                    && selectedFile.listFiles() != null) { // Check if it's a directory
                                                                                           // and if it has stuff in it

                                                String deleteConfirm = inputReader(terminal, lineReader,
                                                        "Press CRTL+C to quit, Press enter to confirm\nAre you sure? this will permanently delete '" // Print
                                                                                                                                                     // the
                                                                                                                                                     // directory
                                                                                                                                                     // specific
                                                                                                                                                     // message
                                                                + selectedFile.getName()
                                                                + "' and all its contents(y/n)",
                                                        "Exited file deletion");
                                                if (deleteConfirm != null) {
                                                    if (deleteConfirm.equals("y")
                                                            || deleteConfirm.equals("yes")) {
                                                        deleteDir(selectedFile, terminal); // Delete the directory
                                                    }
                                                }

                                            } else {
                                                String deleteConfirm = inputReader(terminal, lineReader,
                                                        "Press CRTL+C to quit, Press enter to confirm\nAre you sure? this will permanently delete '" // Message
                                                                                                                                                     // for
                                                                                                                                                     // files
                                                                                                                                                     // and
                                                                                                                                                     // empty
                                                                                                                                                     // directoriess
                                                                + selectedFile.getName() + "'(y/n)",
                                                        "Exited file deletion");
                                                if (deleteConfirm != null) {
                                                    if (deleteConfirm.equals("y") || deleteConfirm.equals("yes")) {
                                                        if (selectedFile.delete()) { // Delete the file
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
                            }
                            case SEARCH -> {
                                boolean searching = true;
                                while (searching) {
                                    search(lineReader, terminal); // Search the file list
                                    searching = false;
                                }
                            }
                            case EXIT -> System.exit(0); // Exit the program
                            case HELP -> { // Print the help menu
                                terminal.puts(Capability.clear_screen);
                                terminal.writer().println(SetColour.set(
                                        "Help:\n Create file = 'c' (Put a / at the end to make it a directory)\n Help: = 'h'\n Rename = 'r'\n Previous directory = Left arrow or 'j' on windows\n Enter selected directory = Right arrow or 'l' on windows\n Navigate up directory list = Up arrow or 'i'\n Navigate down directory list = Down arrow or 'k' on windows \n Delete = Backspace or 'd' on windows\n Search = 's'\n Refresh = 'f'\nPress Crtl+C to exit file creation, renaming and deletion or 'q' to quit the program or help menu",
                                        203, 166, 247));
                                terminal.writer().flush();
                                Operation exitHelp = bindingReader.readBinding(keyMap, null, true);
                                if (exitHelp != null)
                                    ;

                            }
                            case REFRESH -> updateFilesAndDirs(); // Refresh the folder list
                        }
                    }

                }

            }
        }

    }

    //Function for updating the file list
    public static void updateFilesAndDirs() {

        dirList = curDir.listFiles((File current, String name) -> new File(current, name).isDirectory()); //List all directories
        if (dirList == null)
            dirList = new File[0]; //list none if there are none

        fileList = curDir.listFiles((File current, String name) -> new File(current, name).isFile()); //List all files
        if (fileList == null)
            dirList = new File[0]; //List none if there are none

        ArrayList<String> dirSorter = new ArrayList<>(); //Sort the directories
        for (File dir : dirList) {
            dirSorter.add(dir.toString());
        }
        dirSorter.sort(null);
        for (int i = 0; i < dirList.length; i++) {
            dirList[i] = new File(dirSorter.get(i));
        }

        ArrayList<String> fileSorter = new ArrayList<>(); //Sort the files
        for (File file : fileList) {
            fileSorter.add(file.toString());
        }
        fileSorter.sort(null);
        for (int i = 0; i < fileList.length; i++) {
            fileList[i] = new File(fileSorter.get(i));
        }

        fileAndDirList = new File[dirList.length + fileList.length]; //Add them together to create a complete list
        System.arraycopy(dirList, 0, fileAndDirList, 0, dirList.length);
        System.arraycopy(fileList, 0, fileAndDirList, dirList.length, fileList.length);

    }

    //Function for printing the directory bar
    public static String directoryBar() {
        String path;

        if (curDir.getParentFile() != null) { 
            path = curDir.getAbsolutePath();
            String[] pathArray = path.split(Pattern.quote(File.separator)); //Split the path
            pathArray[pathArray.length - 1] = SetColour //Highlight the current directory
                    .setBG(SetColour.set(pathArray[pathArray.length - 1], 49, 50, 68), 186, 192, 222);
            for (int i = 0; i < pathArray.length - 1; i++) { //Add back in the /
                pathArray[i] = SetColour.set(pathArray[i] + "/", 203, 166, 247);

            }
            path = Arrays.toString(pathArray);
            path = path.replaceAll(", ", ""); //Remove all the commas from the array
        } else {
            path = SetColour.set("/", 203, 166, 247); //For the / directory
        }

        return path;
    }
    //Function for deleting directorys
    public static void deleteDir(File file, Terminal terminal) throws InterruptedException {
        File[] contents = file.listFiles();
        if (contents != null) { //Check if the contents are null
            for (File f : contents) {
                deleteDir(f, terminal); //If not, delete
            }
        }
        file.delete(); //The actual deletion goes on here, it just recusivley does it until no files are left
        updateFilesAndDirs();
    }

    //Function for searching the file list
    public static void search(LineReader lineReader, Terminal terminal) throws InterruptedException {
        selectedIndex = 0;
        String searchIn = inputReader(terminal, lineReader, "Enter file name to search: ", "Exited search").replace("/",
                "");
        updateFilesAndDirs();
        if (searchIn != null) { //check if null

            dirList = curDir.listFiles((File current, String name) -> { //List the directories
                if (!name.contains(searchIn)) { // if the name doesnt contain the search, set it to null
                    name = null;
                }
                if (name != null) {//If the name is not null(meaning it contains the search) return it
                    return new File(current, name).isDirectory(); 
                } else { //if not, return false
                    return false;
                }
            });
            //Repeat for files
            fileList = curDir.listFiles((File current, String name) -> {
                if (!name.contains(searchIn)) {
                    name = null;
                }
                if (name != null) {
                    return new File(current, name).isFile();
                } else {
                    return false;
                }
            });

            //If it is null, list no files
            if (dirList == null)
                dirList = new File[0];
            if (fileList == null)
                dirList = new File[0];

            //Sorting
            ArrayList<String> dirSorter = new ArrayList<>();
            for (File dir : dirList) {
                dirSorter.add(dir.toString());
            }
            dirSorter.sort(null);
            for (int i = 0; i < dirList.length; i++) {
                dirList[i] = new File(dirSorter.get(i));
            }

            ArrayList<String> fileSorter = new ArrayList<>();
            for (File file : fileList) {
                fileSorter.add(file.toString());
            }
            fileSorter.sort(null);
            for (int i = 0; i < fileList.length; i++) {
                fileList[i] = new File(fileSorter.get(i));
            }
            //Adding together
            fileAndDirList = new File[dirList.length + fileList.length];
            System.arraycopy(dirList, 0, fileAndDirList, 0, dirList.length);
            System.arraycopy(fileList, 0, fileAndDirList, dirList.length, fileList.length);
        }
    }
    //Function to print errors
    public static void printError(Terminal terminal, String message) throws InterruptedException {
        terminal.writer().println(SetColour.set(message, 243, 139, 168));
        terminal.writer().flush();
        Thread.sleep(500);
    }
    //Function to read input
    public static String inputReader(Terminal terminal, LineReader lineReader, String prompt, String exitMessage)
            throws InterruptedException {
        String in = null;
        try {
            in = lineReader.readLine(SetColour.set(prompt, 203, 166, 247)).strip(); //Read the input with the prompt
        } catch (UserInterruptException e) { //On Crtl+C, exit the lineReader
            printError(terminal, exitMessage);
        }
        if (in != null) { //Make sure the input is not null before returning
            return in;
        } else {
            return "";
        }

    }

}
