public class FileSystemEvent implements Serializable {

    public String fileName;
    private FileSystemEventType type;
    private int pageNumber;
    private byte[] data;
    private String author;

    public FileSystemEvent() {
    }

    public FileSystemEvent(FileSystemEventType type, String fileName, int pageNumber, byte[] data, String author) {
        this.type = type;
        this.fileName = fileName;
        this.pageNumber = pageNumber;
        this.data = data;
        this.author = author;
    }

    static byte[] Serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    static Object Deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    String getFileName() {
        return this.fileName;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

    FileSystemEventType getType() {
        return this.type;
    }

    byte[] getData() {
        return this.data;
    }

    public String toString() {
        return this.type.toString() + " : " + fileName;
    }
}