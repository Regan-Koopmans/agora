import java.io.*;

public class FileSystemPage implements Serializable {
	private String fileName;
	private int pageNumber;
	private byte[] data;

	public FileSystemPage() {}

	public FileSystemPage(String fileName, int pageNumber, byte[] data) {
		this.fileName = fileName;
		this.pageNumber = pageNumber;
		this.data = data;
	}

	public static byte[] Serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}
	public static Object Deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}

	public String getFileName() {
		return this.fileName;
	}

	public int getPageNumber() {
		return this.pageNumber;
	}

	public String toString() {
		return fileName + " : " + pageNumber + " " + (new String(data));
	}

}
