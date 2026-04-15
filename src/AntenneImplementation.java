import com.rabbitmq.client.Channel;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;


public class AntenneImplementation implements AntenneInterface {

	/*
	 * Define the position of the antenna in the space
	 */
    private Integer x;
    private Integer y;
    private Integer r;

	private Integer id;

	/*
	 * A matrix explicit which antennas can communicate
	 * in line and columns we have a, b, c, ...
	 * Matrix has true if the communication can be done
	 * false otherwise
	 *
	 * How it is filled ?
	 * See later
	 */
    private Boolean[][] matrice;
	private ArrayList<Integer> neighbours;

	/*
	 * Global IDs of phones covered by this antenna.
	 * A phone's global ID = nAntennas + localPhoneId.
	 */
	private ArrayList<Integer> localPhoneIds;
	private int nAntennas;

	/*
	 * RabbitMQ attributes
	 */
	private Connection connection;
	private Channel channel;
	/*
	 * Tracks already seen packets to prevent broadcast loops.
	 * A packet is identified by "from-msgId".
	 */
	private Set<String> seenMessages = ConcurrentHashMap.newKeySet();


    public AntenneImplementation(Integer x, Integer y, Integer r, Integer idAntenne, Boolean[][] matNeighbours, int nAntennas, int[][] phones) throws IOException, TimeoutException {

    	/*
    	 * Fill the basic attributes
    	 */
    	this.x = x;
		this.y = y;
		this.r = r;
    	this.id = idAntenne;
    	this.matrice = matNeighbours;
    	this.nAntennas = nAntennas;

    	neighbours = new ArrayList<Integer>();
    	localPhoneIds = new ArrayList<Integer>();
		generateNeighbours();
		findLocalPhones(phones);

		/*
		 * Connect to RabbitMQ
		 */
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");

		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
		} catch (IOException | TimeoutException e) {
			System.err.println("[!] Impossible de se connecter à RabbitMQ : " + e.getMessage());
			throw e;
		}

		boolean durable = false;    // no survive a server restart
		boolean exclusive = false;  // do we restricted the queue to this connection ? We do not care
		boolean autoDelete = false; // auto-delete queue (server will delete it when no longer in use)

		/*
		 * Declare this antenna's fanout exchange and its personal queue
		 */
		channel.exchangeDeclare("ex-" + this.id, "fanout");
		channel.queueDeclare("queue-" + this.id, durable, exclusive, autoDelete, null);

		/*
		 * Bind this antenna's queue to each neighbour's exchange
		 * so that when a neighbour broadcasts, we receive it
		 */
		for (int n : neighbours) {
			channel.exchangeDeclare("ex-" + n, "fanout");
			channel.queueBind("queue-" + this.id, "ex-" + n, "");
		}

		/*
		 * Declare queues for each local phone:
		 *   queue-phone-{id}    : delivery (antenna -> phone)
		 *   queue-phone-{id}-in : send     (phone -> antenna)
		 */
		for (int globalPhoneId : localPhoneIds) {
			channel.queueDeclare("queue-phone-" + globalPhoneId, durable, exclusive, autoDelete, null);
			channel.queueDeclare("queue-phone-" + globalPhoneId + "-in", durable, exclusive, autoDelete, null);
		}

		initCommunication();
    }

	@Override
	public void initCommunication() {
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			Packet p;
			try {
				p = Packet.deserialize(delivery.getBody());

				/*
				 * First check if the message has already been seen once
				 */
				String msgKey = p.getFrom() + "-" + p.getMsgId();
				if (!seenMessages.add(msgKey)) {
					System.out.println("[~] Duplicate dropped : " + msgKey);
					return;
				}

				/*
				 * Then checks the destination of the packet
				 */
				if (p.getTo() == this.id) {
					System.out.println("[x] Received : " + p);
				} else if (p.getTo() >= this.nAntennas) {
					// Destination is a phone
					int destId = p.getTo();
					if (localPhoneIds.contains(destId)) {
						channel.basicPublish("", "queue-phone-" + destId, null, Packet.serialize(p));
						System.out.println("[>] Livré au téléphone " + (destId - nAntennas) + " : \"" + p.getMessage() + "\"");
					} else {
						broadcastMessage(p);
						System.out.println("[-] Relaying " + p);
					}
				} else {
					broadcastMessage(p);
					System.out.println("[-] Relaying " + p);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        };

		/*
		 * Single listening thread on this antenna's queue
		 */
		Thread listener = new Thread(() -> {
		    try {
		        channel.basicConsume("queue-" + this.id, false, deliverCallback, consumertag -> {});
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		});
		listener.setName("listener-" + this.id);
		listener.setDaemon(true);
		listener.start();

		/*
		 * One listening thread per local phone's inbound queue
		 */
		for (int globalPhoneId : localPhoneIds) {
			String phoneInQueue = "queue-phone-" + globalPhoneId + "-in";
			Thread phoneListener = new Thread(() -> {
				try {
					channel.basicConsume(phoneInQueue, false, deliverCallback, tag -> {});
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			phoneListener.setDaemon(true);
			phoneListener.start();
		}
	}

	@Override
	public void broadcastMessage(Packet p) {
		seenMessages.add(p.getFrom() + "-" + p.getMsgId());
		try {
			channel.basicPublish("ex-" + this.id, "", null, Packet.serialize(p)); // we publish onto the exchange, rmq does the job for us
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Find phones whose position falls within this antenna's range.
	 */
	private void findLocalPhones(int[][] phones) {
		for (int i = 0; i < phones.length; i++) {
			Point phonePos   = new Point(phones[i][0], phones[i][1]);
			Point antennaPos = new Point(this.x, this.y);
			if (Utils.distance(antennaPos, phonePos) <= this.r) {
				localPhoneIds.add(nAntennas + i);
			}
		}
	}

	/*
	 * This method fills the derived attribute
	 * neighbours from the matrix
	 */
	private void generateNeighbours(){
		for (int i = 0; i < matrice.length; i++){
			if (i != id && matrice[id][i] == true)
				neighbours.add(i);
		}
	}

	public static void main(String[] argv) throws Exception {
		int id = Integer.parseInt(argv[0]);
		String configFile = argv[1];

		Utils.Config cfg = Utils.readConfig(configFile);

		int x = cfg.antennas[id][0];
		int y = cfg.antennas[id][1];
		int r = cfg.antennas[id][2];

		AntenneImplementation antenne = new AntenneImplementation(x, y, r, id, cfg.matrix, cfg.nAntennas, cfg.phones);

		System.out.println("[" + id + "] Antenne prête. Format : <id_destinataire> <message>");

		Scanner scanner = new Scanner(System.in);
		int msgId = 0;
		while (true) {
			System.out.print("antenne> ");
			String line = scanner.nextLine().trim();
			if (line.isEmpty()) continue;

			String[] parts = line.split(" ", 2);

			if (parts.length < 2) {
				switch (parts[0]){
					case "quit":
						System.out.println("Destruction de l'antenne");
						System.exit(0);
				}

				System.out.println("[!] Format attendu : <id_destinataire> <message>");
				continue;
			}
			int to = Integer.parseInt(parts[0]);
			Packet p = new Packet(id, to, parts[1], msgId++);
			if (to == id) {
				System.out.println("[x] Received : " + p); // antenna send message to itslef without broadcasting
			} else {
				antenne.broadcastMessage(p);
			}
		}
    }
}
