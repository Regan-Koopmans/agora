import com.swirlds.platform.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemMain implements SwirldMain {

    private final int sleepPeriod = 500;
    public ArrayList<FileSystemPage> pages = new ArrayList<>();
    private Platform platform;
    private int selfId;
    private Console console;
    private String threadName;
    private ArrayList<Path> published;

    ReentrantLock eventLock = new ReentrantLock();

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
        //this.console = platform.createConsole(false); // create the window, make it visible
        platform.setAbout("File System \n"); // set the browser's "about" box
        platform.setSleepAfterSync(sleepPeriod);
        this.published = new ArrayList<>();
    }

    private ArrayList<Path> directoryScan() throws IOException {

        ArrayList<Path> scannedFiles = new ArrayList<>();
        Files.walk(Paths.get("files/" + this.threadName)).filter(Files::isRegularFile).forEach(scannedFiles::add);
        return scannedFiles;
    }

    private void updateFileInNetwork(Path path) throws IOException {
        byte[] transaction = FileSystemEvent.Serialize(new FileSystemEvent(
                FileSystemEventType.MODIFY,
                path.toString().replace("files\\" + this.threadName + "\\", ""),
                1,
                Files.readAllBytes(path),
                this.threadName));
        platform.createTransaction(transaction, null);
    }

    // Event handler for delete file
    private void removeFileFromNetwork(Path path) throws IOException {
        byte[] transaction = FileSystemEvent.Serialize(new FileSystemEvent(
                FileSystemEventType.DELETE,
                path.toString().replace("files\\" + this.threadName + "\\", ""),
                1,
                null,
                this.threadName));
        platform.createTransaction(transaction, null);
    }


    // Event handler for new file
    private void addFileToNetwork(Path path) throws IOException {
        byte[] transaction = FileSystemEvent.Serialize(new FileSystemEvent(
                FileSystemEventType.CREATE,
                path.toString().replace("files\\" + this.threadName + "\\", ""),
                1,
                Files.readAllBytes(path),
                this.threadName));
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
            FileSystemState state = (FileSystemState) platform.getState();
            String received = state.getReceived();

            if (!lastReceived.equals(received)) {
                lastReceived = received;

                eventLock.lock();
                try {
                    watcher.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (FileSystemEvent event : state.getEvents()) {
                    File file = new File("files/" + this.threadName + "/" + event.fileName);

                    if (event.getType().equals(FileSystemEventType.CREATE) ||
                            event.getType().equals(FileSystemEventType.MODIFY)) {

                        try {

                            file.createNewFile();
                            FileWriter writer = new FileWriter(file);
                            writer.write(new String(event.getData()));
                            writer.close();
                        } catch (IOException e) {
                            System.out.println(e);
                        }
                    } else {
                        file.delete();
                    }
                }

                try {
                    watcher = localDirectory.getFileSystem().newWatchService();
                    localDirectory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

                } catch (Exception e) {

                }

                eventLock.unlock();
            }

            try {
                watchKey = watcher.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (watchKey != null) {
                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent event : events) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
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
                        Path path = Paths.get("files/" + this.threadName + "/" + event.context().toString());
                        if (!Files.isDirectory(path)) {
                            try {
                                removeFileFromNetwork(path);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path path = Paths.get("files/" + this.threadName + "/" + event.context().toString());
                        if (!Files.isDirectory(path)) {
                            try {
                                updateFileInNetwork(path);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                watchKey.reset();
            }

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