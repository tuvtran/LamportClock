public class Event {

    public int type;
    public long senderId;
    public long receiverId;
    public int localTime;
    public String content;

    public Event(int type, long senderId, long receiverId, int localTime, String content) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.localTime = localTime;
        this.content = content;
    }

}