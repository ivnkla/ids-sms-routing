import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Packet implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int from;
    private int to;
    private String message;
    private int msgId;

    public Packet(int from, int to, String message, int msgId) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.msgId = msgId;
    }
    
    public static byte[] serialize(Packet p) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new ObjectOutputStream(bos).writeObject(p);
        return bos.toByteArray();
    }

    public static Packet deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        return (Packet) new ObjectInputStream(bis).readObject();
    }

    public int getFrom() { return from; }
    public int getTo() { return to; }
    public String getMessage() { return message; }
    public int getMsgId() { return msgId; }

	@Override
	public String toString() {
		return "Packet [from=" + from + ", to=" + to + ", message=" + message + ", msgId=" + msgId + "]";
	}
}
