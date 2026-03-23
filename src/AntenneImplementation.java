import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;


public class AntenneImpl implements AntenneInterface {
    Integer x;
    Integer y;
    double r;
	Integer id;
    Integer[][] matrice;
	ArrayList<Integer> neighbours;
	//Integer[] neighbours;
	Integer[] acknowledgeMsg;
	Channel channel;
	DeliverCallback deliverCallback;


    public AntenneImpl(int idAntenne,Integer[][] matNeighbours) throws IOException, TimeoutException {
		id = idAntenne;


		generateNeighbours();
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		String QUEUE_NAME_PUBLISH = "queue" + Integer.toString(id) + neighbours; //faut boucler sur les neighbours ;

		try (Connection connection = factory.newConnection()) {
			channel = connection.createChannel();
			channel.queueDeclare(QUEUE_NAME, true, false, false, Map.of("x-queue-type", "quorum"));
			deliverCallback = (consumerTag, delivery) -> {
				receiveMessage();
			};
		}
    }

	@Override
	public void sendMessage(Packet p) {
		// TODO Auto-generated method stub

			for (Integer n:  neighbours) {
				String QUEUE_NAME = "queue" + id.toString() + n.toString();
				channel.basicPublish("", QUEUE_NAME, null, p.getBytes(StandardCharsets.UTF_8));

			}
	}

	@Override
	public void receiveMessage() {
		// TODO Auto-generated method stub
			this.channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
			String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
			if (p.to = this.id) {
				sendMessage(p);
			}
	}

	@Override
	public void relayMessage(Packet p) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'relayMessage'");
	}

	@Override
	public boolean isNeighbor(Point p1, Point p2) {
		return false;
	}


	public void generateNeighbours(){

		for (int i = 0; i< matrice.length;i++){
			if (i != id ){
				if ( matrice[id][i] == 1)
					neighbours.add(i);
			}
		}
	}





}
