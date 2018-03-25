import com.swirlds.platform.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * This holds the current state of the swirld. For this simple "hello swirld" code, each transaction is just
 * a string, and the state is just a list of the strings in all the transactions handled so far, in the
 * order that they were handled.
 */
public class FileSystemState implements SwirldState {

    /**
     * The shared state is just a list of the strings in all transactions, listed in the order received
     * here, which will eventually be the consensus order of the community.
     */
    private List<FileSystemEvent> events = Collections
            .synchronizedList(new ArrayList<FileSystemEvent>());

    private AddressBook addressBook;

    /**
     * @return all the pages received so far from the network
     */
    public synchronized List<FileSystemEvent> getEvents() {
        return events;
    }

    /**
     * @return all the pages received so far from the network, concatenated into one
     */
    synchronized String getReceived() {

        StringBuilder sb = new StringBuilder();

        sb.append("FILE SYSTEM HISTORY\n");

        Integer timestamp = 1;
        for (FileSystemEvent event : getEvents()) {
            sb.append(timestamp)
                    .append(" - ")
                    .append(event.getType().toString())
                    .append(" : ")
                    .append(event.getFileName())
                    .append("\n");
            timestamp += 1;
        }
        return sb.toString();
    }

    /**
     * @return the same as getReceived, so it returns the entire shared state as a single string
     */
    public String toString() {
        return events.toString();
    }

    @Override
    public synchronized AddressBook getAddressBookCopy() {
        return addressBook.copy();
    }

    @Override
    public synchronized FastCopyable copy() {
        FileSystemState copy = new FileSystemState();
        copy.copyFrom(this);
        return copy;
    }

    @Override
    public void copyTo(FCDataOutputStream outStream) {
        try {
            Utilities.writeByteArray(outStream,
                    SerializationUtils.serialize(events.toArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void copyFrom(FCDataInputStream inStream) {
        try {

            events = new ArrayList<>();
            ObjectInputStream buffered = new ObjectInputStream(inStream);

            FileSystemEvent event;

            for (event = (FileSystemEvent) buffered.readObject(); event != null; ) {
                events.add(event);
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void copyFrom(SwirldState old) {
        events = Collections.synchronizedList(new ArrayList<>(((FileSystemState) old).events));
        addressBook = ((FileSystemState) old).addressBook.copy();
    }

    @Override
    public synchronized void handleTransaction(long id, boolean consensus,
                                               Instant timeCreated, byte[] transaction, Address address) {
        try {
            events.add((FileSystemEvent) FileSystemEvent.Deserialize(transaction));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void noMoreTransactions() {
    }

    @Override
    public synchronized void init(Platform platform, AddressBook addressBook) {
        this.addressBook = addressBook;
    }

    List<FileSystemPage> getFileSystem() {

        // We obtain a map, which maps file names to their ordered events.
        Map<String, List<FileSystemEvent>> fileEvents =
                events.stream().collect(Collectors.groupingBy(x -> x.fileName));

        List<FileSystemPage> files = new ArrayList<>();

        // We use this map to construct a current state of the filesystem by file.
        for (Map.Entry<String, List<FileSystemEvent>> pair : fileEvents.entrySet()) {
            List<FileSystemEvent> events = pair.getValue();

            FileSystemEvent lastEvent = events.get(events.size() - 1);

            // If the last event was not a delete, then file currently exists in the system.
            if (lastEvent.getType() != FileSystemEventType.DELETE) {
                files.add(new FileSystemPage(
                        lastEvent.getFileName(),
                        1,
                        events.get(events.size() - 1).getData())
                );
            }
        }
        return files;
    }
}