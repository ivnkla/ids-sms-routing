import java.awt.*;

public interface AntenneInterface {
	/*
	 * An antenna can send message over the network.
	 * It does not know to who it is sent.
	 * The endpoint of the send communication is 
	 * actually defined at configuration.
	 * 
	 * If the destination of the message is not this
     * antenna, then relay to all of the accessible 
     * antenna
	 * 
	 * All in all just take a packet in argument and
	 * send it over its neighbor list
	 */
	void broadcastMessage(Packet p);

    /*
     * Method that launch all the outgoing
     * sockets. After that, each listening sockets
     * is well mapped with one thread.
     */
    void initCommunication();
}
