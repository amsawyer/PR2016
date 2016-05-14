/* Basic starting point for ring network 
 * 1 entity with randomly generated ID
 *     - UDP port 10001
 *     - TCP port 19001
 *     - Multicast address: 127.0.0.1
 *     - Multicast port: 8888
 *
 * To insert new entity, run:
 * java InsertEntity <mon_port_udp> <mon_port_tcp> <adresse_pour_se_connecter> <port_tcp_pour_se_connecter>
 *
 * TEST BY INSERTING MORE ENTITIES, 
 * THEN ENTER MESSAGES BY USING NETCAT OR 
 * BY TYPING THEM ON STDIN
 *
 * FOR NOW, ALL MACHINES ARE ON LOCALHOST (127.0.0.1)
*/

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.UUID;
import java.lang.Math;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class AnneauMain {

	public static void main(String[] args) {
		try {
            long id = createID();
            String local_adr = InetAddress.getLocalHost().getHostAddress();

            Entity ent = new Entity(id, // id
                    10001, // port d'écoute
                    19001, // port TCP
                    local_adr, // adresse de machine suivante
                    10001, // port d'écoute de la machine suivante
                    local_adr, // adresse de multi-diffusion
                    8888 // port de multi-diffusion
            );            

            // créer & commencer les threads
            Thread t = new Thread(ent);
            t.start();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private static long createID() {
        UUID u = UUID.randomUUID();
        long ul = u.getLeastSignificantBits(); // 8 octets
        long ul_abs = Math.abs(ul); // valeur absolue

        return ul_abs;
    }
}
