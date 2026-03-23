public class AntenneImpl implements AntenneInterface {
    Integer x;
    Integer y;
    Integer r;
	Integer id;
    Integer[][] matrice;
	Integer[] neighbours;
	Integer[] acknowledgeMsg;
    public AntenneImpl() {
		channel.basicConsume(QUEUE_NAME, true, receivmessage, consumerTag -> { });
	}


	@Override
	public void sendMessage(Packet p) {
		// TODO Auto-generated method stub

			for (n: Integer in neighbours) {
				QUEUE_NAME = "queue" + id.toString() + n.toString()
				channel.basicPublish("", QUEUE_NAME, null, p.getBytes(StandardCharsets.UTF_8));

			}
	}
		throw new UnsupportedOperationException("Unimplemented method 'sendMessage'");
	}
	@Override
	public void receiveMessage() {
		// TODO Auto-generated method stub

			String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
			if (p.to = this.id) {
				sendMessage
			}
	}
		throw new UnsupportedOperationException("Unimplemented method 'receiveMessage'");
	}
	@Override
	public void relayMessage(Packet p) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'relayMessage'");
	}

	public void generateNeighbours(){

		for (int i = 0; i< matrice.length){
			if (i != id ){
				if ( matrice[id][j] == 1)
				neighbours.append(j)
			}
		}
	}

}
