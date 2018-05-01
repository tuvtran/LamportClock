public class Event {

    public int type;
    public long senderId;
    public long receiverId;
    public int localTime;
    public String content;

    public Event(int type, long senderId, long receiverId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.localTime = 0;
    }

    public Event(int type, long senderId,
    long receiverId, int localTime, String content) {
        this(type, senderId, receiverId, content);
        this.localTime = localTime;
    }

}