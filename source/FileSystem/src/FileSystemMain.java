import com.swirlds.platform.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This HelloSwirld creates a single transaction, consisting of the string "Hello Swirld", and then goes
 * into a busy loop (checking once a second) to see when the state gets the transaction. When it does, it
 * prints it, too.
 */
public class FileSystemMain implements SwirldMain {


    private Platform platform;
    private int selfId;
    private Console console;
    private String threadName;
    private ArrayList<Path> published;

    private final int sleepPeriod = 1000;

    public ArrayList<FileSystemPage> pages = new ArrayList<>();

    public static void main(String[] args) {
        Browser.main(null);
    }


    @Override
    public void preEvent() {
    }

    @Override
    public void init(Platform platform, int id) {
        this.platform = platform;
        this.selfId = id;
        this.console = platform.createConsole(true); // create the window, make it visible
        platform.setAbout("File System \n"); // set the browser's "about" box
        platform.setSleepAfterSync(sleepPeriod);
        this.published = new ArrayList<>();
    }

    public ArrayList<Path> directoryScan() throws IOException {

        ArrayList<Path> scannedFiles = new ArrayList<>();
        Files.walk(Paths.get("files/" + this.threadName))
                .filter(Files::isRegularFile)
                .forEach(x -> scannedFiles.add(x));

        return scannedFiles;
    }

    private void addFileToNetwork(Path path) throws IOException {

        byte [] transaction = FileSystemPage.Serialize(new FileSystemPage(path.toString(), selfId, Files.readAllBytes(path)));
        platform.createTransaction(transaction, null);
    }

    @Override
    public void run() {
        this.threadName = platform.getState().getAddressBookCopy().getAddress(selfId).getSelfName();

        byte[] transaction;

        ArrayList<Path> unpublished = new ArrayList<>();
        Path localDirectory = Paths.get("files/" + this.threadName);

        // Create a file watcher for the file system.
        WatchService watcher = null;
        WatchKey watchKey = null;
        try {
            watcher = localDirectory.getFileSystem().newWatchService();
            localDirectory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            e.printStackTrace();
        }


        String lastReceived = "";
        boolean isRunning = true;

        while (isRunning) {

            // Read network for files.

            FileSystemState state = (FileSystemState) platform
                    .getState();
            String received = state.getReceived();

            if (!lastReceived.equals(received)) {
                lastReceived = received;
                for (FileSystemPage page : state.getPages()) {
                    console.out.println(received);
                }
            }


            // Scan directory for changes.
            try {
                unpublished = directoryScan();
            } catch (IOException e) {
                e.printStackTrace();
            }
            unpublished.removeAll(published);


            try {
                watchKey = watcher.poll(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (watchKey != null) {
                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent event : events) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {

                        console.out.println("Created: " + event.context().toString());
                        Path path = Paths.get("files/" + this.threadName + "/" + event.context().toString());

                        if (!Files.isDirectory(path)) {
                            try {
                                addFileToNetwork(path);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        console.out.println("Delete: " + event.context().toString());
                    }
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        console.out.println("Modify: " + event.context().toString());
                    }
                }
                watchKey.reset();
            }


            // Try to publish all new files.

//            try {
//
//                for (Path path : unpublished) {
//
//                    console.out.println("Found file: " + path.toString());
//
//                    // Given this unpublished path, create a new transaction
//
//
//                    // Place this transaction on the network
//                    platform.createTransaction(transaction, null);
//
//                    // This path has now been published to the network.
//                    published.add(path);
//
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            try {
                Thread.sleep(sleepPeriod);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public SwirldState newState() {
        return new FileSystemState();
    }
}