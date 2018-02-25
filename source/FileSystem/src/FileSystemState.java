

import com.swirlds.platform.*;
import org.apache.commons.lang3.SerializationUtils;

import javax.rmi.CORBA.Util;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    private List<FileSystemPage> pages = Collections
            .synchronizedList(new ArrayList<FileSystemPage>());


    /**
     * names and addresses of all members
     */
    private AddressBook addressBook;

    /**
     * @return all the pages received so far from the network
     */
    public synchronized List<FileSystemPage> getPages() {
        return pages;
    }

    /**
     * @return all the pages received so far from the network, concatenated into one
     */
    synchronized String getReceived() {

        StringBuilder sb = new StringBuilder();

        sb.append("FILE SYSTEM STATE\n");

        for (FileSystemPage page: getPages()) {
            sb.append("- " + page.getFileName() + "\n");
        }

        return sb.toString();
    }

    /**
     * @return the same as getReceived, so it returns the entire shared state as a single string
     */
    public String toString() {
        return pages.toString();
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
                    SerializationUtils.serialize(pages.toArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void copyFrom(FCDataInputStream inStream) {
        try {

            pages = new ArrayList<>();
            ObjectInputStream buffered = new ObjectInputStream(inStream);

            FileSystemPage page;

            for (page = (FileSystemPage) buffered.readObject(); page != null;) {
                pages.add(page);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void copyFrom(SwirldState old) {
        pages = Collections.synchronizedList(
                new ArrayList<>(((FileSystemState) old).pages));
        addressBook = ((FileSystemState) old).addressBook.copy();
    }

    @Override
    public synchronized void handleTransaction(long id, boolean consensus,
                                               Instant timeCreated, byte[] transaction, Address address) {
        try {
            pages.add((FileSystemPage) FileSystemPage.Deserialize(transaction));
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
}