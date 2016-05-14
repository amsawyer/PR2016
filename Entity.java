/* Recevoir les messages du serveur et les imprimer
 * à stdout
*/

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.DatagramChannel;
import java.nio.ByteBuffer;

public class Entity implements Runnable {
    private long id;
    private int port_ecoute; // port d'écoute
    private int port_tcp;
    private String adr_suiv;
    private int port_suiv;
    private String adr_diff; // adresse de multi-diffusion
    private int port_diff; // port de multi-diffusion 

    private ArrayList<String> deja_vus = new ArrayList<String>(); // messages que cette entité a déjà vu

    public Entity(long id, int pe, int pt, String as, int ps, String ad, int pd) {
        this.id = id;
        this.port_ecoute = pe;
        this.port_tcp = pt;
        this.adr_suiv = as;
        this.port_suiv = ps;
        this.adr_diff = ad;
        this.port_diff = pd;
    }

    /* Méthode pour recevoir un message de la machine précédente de l'anneau,
       puis le transmettre à la prochaine machine de l'anneau à la condition
       qu'il n'a pas encore fait le tour de l'anneau
    */
    public void run() {
        try {
            // commence un thread pour attendre l'entrée au stdin
            StartMessages startmess = new StartMessages(this.id, this.port_ecoute);
            Thread t_startmess = new Thread(startmess);
            t_startmess.start();

            System.setProperty("java.net.preferIPv4Stack" , "true");

            // ouvre une socket juste pour envoyer - pas nécessaire de spécifier le port
            DatagramSocket dso = new DatagramSocket();

            // ouvre des canaux
            ServerSocketChannel srv = ServerSocketChannel.open();
            DatagramChannel dc = DatagramChannel.open();
            srv.configureBlocking(false);
            dc.configureBlocking(false);

            // canal TCP pour attendre les demandes d'insertion
            srv.socket().bind(new InetSocketAddress(this.port_tcp));
            // canal UDP pour transmettre les messages dans l'anneau
            dc.bind(new InetSocketAddress(this.port_ecoute));
            Selector sel = Selector.open();
            srv.register(sel, SelectionKey.OP_ACCEPT);
            dc.register(sel, SelectionKey.OP_READ);

            while (true) {

                ByteBuffer buff = ByteBuffer.allocate(1024);

                sel.select();
                Iterator<SelectionKey> it = sel.selectedKeys().iterator();

                while(it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    // TCP CONNEXION (pour insérer nouvelle entité)
                    if (key.isAcceptable()) {
                        // accepte connexion de la part de la nouvelle entité
                        SocketChannel client = srv.accept();
                        client.configureBlocking(false);
                        // envoie le message WELC
                        String mess_welc = "WELC " + this.adr_suiv + " " +
                                this.port_suiv + " " + this.adr_diff + " " +
                                this.port_diff + "\n";
                        byte[] tcp_data = mess_welc.getBytes();
                        ByteBuffer source = ByteBuffer.wrap(tcp_data);
                        client.write(source);

                        // recoit le message NEWC
                        int bytes_read;
                        // attend l'envoi du message
                        while((bytes_read = client.read(buff)) == 0) {
                            ;
                        }
                        String mess_newc = new String(buff.array(), 0, buff.array().length);
                        String[] mess_newc_mots = mess_newc.trim().split(" "); 
                        System.out.println("Recu : " + mess_newc);
                        buff.clear();

                        String adr_new = mess_newc_mots[1]; // adresse de nouvelle entité
                        int port_ec_new = Integer.parseInt(mess_newc_mots[2]); // port d'écoute de nouvelle entité

                        // message commence avec "NEWC" comme attendu
                        if (mess_newc_mots[0].equals("NEWC")) {
                            client.write(ByteBuffer.wrap(("ACKC\n").getBytes()));
                            client.close();

                            // met nouvelle entité après cette entité
                            this.adr_suiv = adr_new;
                            this.port_suiv = port_ec_new;
                        }
                        // message est du mauvais format
                        else {
                            System.out.println("Message mal formé");
                        }
                    }

                    // UDP MESSAGE (pour faire le tour de l'anneau)
                    else if(key.isReadable() && key.channel() == dc) {
                        // recoit le message
                        dc.receive(buff);
                        String message = new String(buff.array(), 0, buff.array().length);
                        buff.clear();

                        // message divisé par les espaces
                        String[] mess_mots = message.split(" ");

                        // format incorrect du message
                        if (mess_mots.length < 2) {
                            System.out.println("Message mal formé - assurez-vous que le message " +
                                    "est d'un format précisé dans l'énoncé du projet");
                        }
                        // message est bon
                        else {
                            System.out.println("Recu : " + message);
                            String mess_id = mess_mots[1]; // message id

                            // voir si le message a déjà fait le tour de l'anneau
                            if (!(this.deja_vus).contains(mess_id)) {
                                (this.deja_vus).add(mess_id);

                                //data_send = message.getBytes();
                                System.out.println("En train d'envoyer... " + message); 
                                
                                // transmet le message à la prochaine machine
                                InetSocketAddress ia = new 
                                        InetSocketAddress(this.adr_suiv, this.port_suiv);
                                byte[] udp_data = message.getBytes();
                                DatagramPacket paquet_send = new DatagramPacket(udp_data, 
                                        udp_data.length, ia);

                                dso.send(paquet_send);
                            }
                        }
                    }
                    else {
                        System.out.println("Error");
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
