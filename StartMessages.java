/* Recevoir les messages du serveur et les imprimer
 * à stdout
*/

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;

public class StartMessages implements Runnable {
    private long id;
    private int port_ecoute;

    private ArrayList<String> deja_vus = new ArrayList<String>(); // messages que cette entité a déjà vu

    public StartMessages(long id, int pe) {
        this.id = id;
        this.port_ecoute = pe;
    }

    /* Méthode pour attendre l'entrée d'un message au stdin par l'utilisateur,
       puis l'envoyer autour de l'anneau
    */
    public void run() {
        try {
            String[] mess_possibles = {"WELC", "NEWC", "ACKC", "APPL", "WHOS",
                    "MEMB", "GBYE", "EYBG", "TEST", "DOWN", "DUPL", "ACKD"};
    
            Scanner scan = new Scanner(System.in);
            // ouvre une socket juste pour envoyer - pas nécessaire de spécifier le port
            DatagramSocket dso = new DatagramSocket();

            while (true) {
                String message = scan.nextLine();
                // message divisé par les espaces
                String[] mess_mots = message.split(" ");

                // format incorrect du message
                if (!(Arrays.asList(mess_possibles).contains(mess_mots[0])) || mess_mots.length < 2) {
                    System.out.println("Message mal formé - assurez-vous que le message " +
                            "est d'un format précisé dans l'énoncé du projet");
                }
                // message est bon
                else {
                    String mess_id = mess_mots[1]; // message id

                    // voir si le message a déjà fait le tour de l'anneau
                    if (!(this.deja_vus).contains(mess_id)) {
                        (this.deja_vus).add(mess_id);

                        String mon_adr = InetAddress.getLocalHost().getHostAddress();
                        // envoie à son propre port UDP pour commencer le tour de l'anneau
                        InetSocketAddress ia = new 
                                InetSocketAddress(mon_adr, this.port_ecoute);
                        byte[] udp_data = message.getBytes();
                        DatagramPacket paquet_send = new DatagramPacket(udp_data, 
                                udp_data.length, ia);

                        dso.send(paquet_send);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
