import com.rabbitmq.client.Channel;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
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
	private ArrayList<String> inQueues;
	private ArrayList<String> outQueues;
	
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


    public AntenneImplementation(Integer x, Integer y, Integer r, Integer idAntenne, Boolean[][] matNeighbours) throws IOException, TimeoutException {
		
    	/*
    	 * Fill the basic attributes
    	 */
    	this.x = x;
		this.y = y;
		this.r = r;
    	this.id = idAntenne;
    	this.matrice = matNeighbours;
    	
    	/*
    	 * Fill the neighbors and queues lists
    	 */
    	
    	neighbours = new ArrayList<Integer>();
    	inQueues = new ArrayList<String>();
    	outQueues = new ArrayList<String>();
    	
		generateNeighbours();
		generateInQueues();
		generateOutQueues();
		
		/*
		 * Activate the queues in RabbitMQ
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
			
			String queue = null;
			boolean durable = false; // no survive a server restart
			boolean exclusive = false; // do we restricted the queue to this connection ? We do not care
			boolean autoDelete = false; // auto-delete queue (server will delete it when no longer in use)
			Map<String, Object> arguments = null;
			
			/*
			 * Declare all queues
			 */
			for (String q : inQueues) {
				queue = q;
				channel.queueDeclare(queue, durable, exclusive, autoDelete, arguments);
			}
			
			for (String q : outQueues) {
				queue = q;
				channel.queueDeclare(queue, durable, exclusive, autoDelete, arguments);
			}
			
			/*
			 * Callback that handle the packet
			 */
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false); // not sure of that
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
					 * Then checks the own of the message
					 */
					if (p.getTo() == this.id) {
						System.out.println("[x] Received : " + p);
					} else {
						relayMessage(p);
						System.out.println("[-] Relaying ");
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        };
	        
			/*
			 * Create a single listening thread for each inQueue
			 */
			boolean autoAck = false;
			for (String inQueue : inQueues) {
			    Thread listener = new Thread(() -> {
			        try {
			            channel.basicConsume(inQueue, autoAck, deliverCallback, consumertag -> {});
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
			    });
			    listener.setName("listener-" + inQueue);
			    listener.setDaemon(true);
			    listener.start();
			}

    }

	@Override
	public void sendMessage(Packet p) {
		relayMessage(p);
	}

	@Override
	public void receiveMessage() {
		// TODO Auto-generated method stub
	}

	@Override
	public void relayMessage(Packet p) {
		int from = p.getFrom();
		String QUEUE_FROM_OUT = "queue-" + this.id + "-" + from;
		for (String outQueue : outQueues) {
			if (!outQueue.equals(QUEUE_FROM_OUT)) {
				try {
					channel.basicPublish("", outQueue, null, Packet.serialize(p));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean isNeighbor(Point p1, Point p2) {
		return false;
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
	
	/*
	 * Generate all the in queues that point to the antenna.
	 */
	private void generateInQueues() {
		String QUEUE_NAME_SUBSCRIBE;
		for (Integer n : neighbours) {
			// generate queue of the form "queue-2-1" if 1 and 2 can communicate
			QUEUE_NAME_SUBSCRIBE = "queue-" + n + "-" + Integer.toString(id);
			inQueues.add(QUEUE_NAME_SUBSCRIBE);
		}
	}
	
	/*
	 * Generate all the queues that point out of the antenna
	 */
	private void generateOutQueues() {
		String QUEUE_NAME_PUBLISH;
		for (Integer n : neighbours) {
			// generate queue of the form "queue-1-2" if 1 and 2 can communicate
			QUEUE_NAME_PUBLISH = "queue-" + Integer.toString(id) + "-" + n;
			outQueues.add(QUEUE_NAME_PUBLISH);
		}
	}
	
	public static void main(String[] argv) throws Exception {
		int id = Integer.parseInt(argv[0]);
		String configFile = argv[1];

		Utils.Config cfg = Utils.readConfig(configFile);

		int x = cfg.antennas[id][0];
		int y = cfg.antennas[id][1];
		int r = cfg.antennas[id][2];

		AntenneImplementation antenne = new AntenneImplementation(x, y, r, id, cfg.matrix);

		System.out.println("[" + id + "] Antenne prête. Format : <id_destinataire> <message>");

		Scanner scanner = new Scanner(System.in);
		int msgId = 0;
		while (true) {
			System.out.print("> ");
			String line = scanner.nextLine().trim();
			if (line.isEmpty()) continue;
			String[] parts = line.split(" ", 2);
			if (parts.length < 2) {
				System.out.println("[!] Format attendu : <id_destinataire> <message>");
				continue;
			}
			int to = Integer.parseInt(parts[0]);
			antenne.sendMessage(new Packet(id, to, parts[1], msgId++));
		}
	}
}
