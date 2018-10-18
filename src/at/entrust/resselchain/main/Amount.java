/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.main;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.utils.TimeSlots;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

public class Amount {

    private static void showUsage() {
        System.out.println("Ressel Chain Node Amount (Amount)");
        System.out.println("Read Amount from a specified node.");
        System.out.println("Usage: Amount -h | (<Node Address> <Node Port>)");
        System.out.println("-h : display help");
        System.out.println("<Node Address> <Node Port> <assetName> <Date> : IP address and port of the node to request Amount from");
    }

    /* https://stackoverflow.com/questions/139076/how-to-pretty-print-xml-from-java */
    public static String format(String xml) throws ParsingException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer serializer = new Serializer(out);
        serializer.setIndent(4);
        serializer.write(new Builder().build(xml, null));
        return out.toString("UTF-8");
    }

    public static void main(String[] args) {

        Start.removeCryptographyRestrictions();

        if (args.length == 1 && args[0].equals("-h")) {
            showUsage();
            System.exit(0);
        }
        else if (args.length == 4) {
            try {
                // parse arguments
                String nodeAddress = args[0];
                int nodePort = Integer.valueOf(args[1]);
                String assetName = args[2];
                long date = Long.valueOf(args[3]);
                Element xmlRoot = new Element("GetAmount");
                Element xmlPvName = new Element("assetName");
                xmlPvName.appendChild(assetName);
                xmlRoot.appendChild(xmlPvName);


                Element xmlDate = new Element("Date");
                xmlDate.appendChild(date +"");
                xmlRoot.appendChild(xmlDate);

                Document doc = new Document(xmlRoot);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    Serializer serializer = new Serializer(out, "UTF-8");
                    serializer.setLineSeparator("\n");
                    serializer.write(doc);

                    String amountRequest = out.toString().replace('\n', ' ');


                    SSLSocket socket = (SSLSocket)SSLSocketFactory.getDefault().createSocket(nodeAddress, nodePort);
                    socket.setEnabledCipherSuites(new String[] {GlobalConfig.INSTANCE.SSL_SOCKET_CIPHER_SUITE});

                    PrintWriter output = new PrintWriter(socket.getOutputStream());
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    output.println(amountRequest);
                    output.flush();
                    String response = input.readLine();
                    System.out.println(format(response));

                    //output.println("EOL");
                    output.close();
                    input.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.out.println("Amount: One or more arguments are in invalid format. See usage below.\n");
                showUsage();
                System.exit(1);
            }

        }else {
            System.out.println("Amount: Invalid arguments. See usage below.\n");
            showUsage();
            System.exit(1);
        }
    }

}
