package com.sorintlab.itch;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public class Itch {
    public static void main(String []args){
        File configFile = new File("itch.yaml");
        if (!configFile.exists()){
            System.out.println("Configuration file \"itch.yaml\" does not exist.  Exiting.");
            System.exit(1);
        }

        Configuration config = readConfig(configFile);
        if (config == null) {
            System.out.println("System will exit.");
            System.exit(1);
        }

        InetSocketAddress []addresses = parseMemberNames(config.getMembers());

        ServerSocketChannel serverSocketChannel = openServerSocket(addresses);
        if (serverSocketChannel == null){
            System.out.println("System will exit.");
            System.exit(1);
        }

        LinkedList<SocketChannel> inboundSockets = new LinkedList<>();

        try {
            while(true){
                // TODO - need to make this interruptible
                try {
                    SocketChannel socket = serverSocketChannel.accept();
                    System.out.println("Received new connection from " + socket.getRemoteAddress());
                    socket.close();
                } catch(IOException x){
                    System.out.println("An error occurred while accepting a connection: " + x.getMessage());
                }
            }
        } finally {
            try {
                serverSocketChannel.close();
            } catch(IOException iox){
                // nothing to do about this
            }
        }
    }

    /**
     * Parses the YAML configuration file and returns a Configuration object.
     * This method will print a message and return null if the configuration cannot be parsed.
     * @param configFile
     * @return
     */
    private static Configuration readConfig(File configFile){
        Configuration result = null;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            result = mapper.readValue(configFile, Configuration.class);
        } catch (JsonParseException e) {
            System.out.println("Could not parse configuration file");
        } catch (JsonMappingException e) {
            System.out.println("Configuration file does not have the expected format");
        } catch (IOException e) {
            System.out.println("Could not read the configuration file");
        }
        return result;
    }

    /**
     * Converts the member names in nnn.nnn.nnn.nnn:pppp format into InetSocketAddress instances.
     * 
     * @param memberNames
     * @return an array of InetSocketAddress the same size as the input. If any item in the input
     * cannot be parsed, a message will be logged and the corresponding item in the output will be null;
     */
    private static InetSocketAddress []parseMemberNames(String []memberNames){
        Pattern ipv4pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\:(\\d{2,5})");
        InetSocketAddress []addresses = new InetSocketAddress[memberNames.length];
        int i = 0;
        for(String address: memberNames){
            Matcher matcher = ipv4pattern.matcher(address);
            if (!matcher.matches()){
                System.out.println("\"" + address + "\" is an invalid member format. All members must be specified in the form of a numeric IPv4 address and a port number separate by a colon. ");
                i++;
            } else {
                InetAddress ip;
                try {
                    ip = InetAddress.getByName(matcher.group(1));
                    addresses[i++] =  new InetSocketAddress(ip, Integer.parseInt(matcher.group(2)));               
                } catch (UnknownHostException e) {
                    System.out.println("An error occurred while trying to create an InetAddress from " + matcher.group(1));
                    i++;
                }
            }
        }

        return addresses;
    }

    private static ServerSocketChannel openServerSocket(InetSocketAddress [] addresses){
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
        } catch (IOException e) {
            System.out.println("An error occurred in ServerSocketChannel.open: " + e.getMessage());
            return null;  // RETURN
        }

        SocketAddress localAddress = null;
        for(InetSocketAddress address: addresses){
            if (address == null){
                System.out.println("At least one member address could not be read.");
                return null; //RETURN
            }

            try {
                localAddress = serverSocketChannel.getLocalAddress();
                if (localAddress == null){
                    serverSocketChannel.bind(address);
                    localAddress = serverSocketChannel.getLocalAddress();
                }
            } catch(IOException iox){
                // this is expected
            }
        }
        
        if (localAddress == null){
            System.out.println("None of the members specify an interface on this host");
            return null;  // RETURN
        }

        System.out.println("listening on " + localAddress); 
        return serverSocketChannel;
    }
}
