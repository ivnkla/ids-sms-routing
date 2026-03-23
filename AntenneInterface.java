public interface AntenneInterface {

    void sendMessage(Packet p);

    void receiveMessage();

    void relayMessage(Packet p);

    boolean isNeighbor(Point p1, Point p2);
}
