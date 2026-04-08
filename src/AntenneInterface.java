package src;

import java.awt.*;

public interface AntenneInterface {

	/*
	 * An antenna can send message over the network.
	 * It does not know to who it is sent.
	 * The endpoint of the send communication is 
	 * actually defined at configuration.
	 * 
	 * It just take a packet in argument and
	 * send it over the network.
	 */
    void sendMessage(Packet p);

    /*
     * Idle method that listen on its receiving socket.
     * The receive socket is as the send one defined at
     * configuration.
     */
    void receiveMessage();

    /*
     * If the destination of the message is not this
     * antenna, then relay to all of the accessible 
     * antenna
     */
    
    //TODO may recall this method broadcast
    void relayMessage(Packet p);

    /*
     * Not sure of the usefullness o this method 
     */
    boolean isNeighbor(Point p1, Point p2);
}
