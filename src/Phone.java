import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/*
 * Terminal téléphone.
 *
 * Usage : java Phone <configFile>
 *
 * L'utilisateur choisit un téléphone dans la liste, puis envoie des messages :
 *   >> <msg> <destLocalPhoneId>
 */
public class Phone {

    public static void main(String[] argv) throws Exception {
        if (argv.length < 1) {
            System.err.println("Usage: java Phone <configFile>");
            System.exit(1);
        }

        Utils.Config cfg = Utils.readConfig(argv[0]);

        if (cfg.phones.length == 0) {
            System.err.println("[!] Aucun téléphone défini dans le fichier de configuration.");
            System.exit(1);
        }

        /*
         * Show available phones and let user pick one
         */
        System.out.println("Téléphones disponibles :");
        for (int i = 0; i < cfg.phones.length; i++) {
            System.out.println("  [" + i + "] position (" + cfg.phones[i][0] + ", " + cfg.phones[i][1] + ")");
        }

        Scanner scanner = new Scanner(System.in);
        int localPhoneId = -1;
        while (localPhoneId < 0 || localPhoneId >= cfg.phones.length) {
            System.out.print("Choisissez un téléphone : ");
            try {
                localPhoneId = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                // keep looping
            }
        }

        int px = cfg.phones[localPhoneId][0];
        int py = cfg.phones[localPhoneId][1];
        int antennaId = Utils.findNearestAntennaInRange(px, py, cfg.antennas);

        if (antennaId == -1) {
            System.err.println("[!] Ce téléphone est hors couverture, impossible de se connecter.");
            System.exit(1);
        }

        int globalPhoneId = cfg.nAntennas + localPhoneId;

        /*
         * Connect to RabbitMQ
         */
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection;
        Channel channel;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (IOException | TimeoutException e) {
            System.err.println("[!] Impossible de se connecter à RabbitMQ : " + e.getMessage());
            throw e;
        }

        /*
         * Listen on this phone's delivery queue
         */
        String myQueue = "queue-phone-" + globalPhoneId;
        channel.queueDeclare(myQueue, false, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            try {
                Packet p = Packet.deserialize(delivery.getBody());
                int senderLocalId = p.getFrom() - cfg.nAntennas;
                System.out.println("\n[SMS] Téléphone " + senderLocalId + " : \"" + p.getMessage() + "\"");
                System.out.print(">> ");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        };

        Thread listener = new Thread(() -> {
            try {
                channel.basicConsume(myQueue, false, deliverCallback, tag -> {});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listener.setDaemon(true);
        listener.start();

        System.out.println("[Téléphone " + localPhoneId + " @ antenne " + antennaId + "] Connecté. Format : <msg> <destId>");

        /*
         * Send loop
         */
        int msgId = 0;
        while (true) {
            System.out.print("phone> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (line.equals("quit")) {
                System.out.println("Déconnexion.");
                System.exit(0);
            }

            String[] parts = line.split(" ", 2);
            if (parts.length < 2) {
                System.out.println("[!] Format attendu : <msg> <destId>");
                continue;
            }

            int destLocalId;
            try {
                destLocalId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.out.println("[!] destId doit être un entier.");
                continue;
            }

            if (destLocalId < 0 || destLocalId >= cfg.phones.length) {
                System.out.println("[!] Téléphone destinataire inconnu : " + destLocalId);
                continue;
            }

            if (destLocalId == localPhoneId) {
                System.out.println("[SMS] Téléphone " + localPhoneId + " : \"" + parts[0] + "\"");
                continue;
            }

            int destGlobalId = cfg.nAntennas + destLocalId;
            Packet p = new Packet(globalPhoneId, destGlobalId, parts[0], msgId++);
            channel.basicPublish("ex-" + antennaId, "", null, Packet.serialize(p));
            System.out.println("[~] Envoyé via antenne " + antennaId);
        }
    }
}
