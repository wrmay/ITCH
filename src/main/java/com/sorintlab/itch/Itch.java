package com.sorintlab.itch;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public class Itch {
    public static String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS z";

    public static Logger log;

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
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhx){
            System.out.println("WARNING: Could not determine host name.");
        }

        try {
            log = setupLogging(config, hostname);
        } catch (IOException e) {
            System.out.println("Could not set up logging due to: " + e.getMessage());
            System.exit(1);
        }

        InetSocketAddress []addresses = parseMemberNames(config.getMembers());
        int nullAddresses = 0;
        for(InetSocketAddress address: addresses){
            if (address == null) nullAddresses += 1;
        }
        if (nullAddresses > 0){
            Itch.log.severe("The system is exiting because " + nullAddresses + " of the provided addresses could not be parsed");
            System.exit(1);
        }

        try {
            Itch itch = new Itch(addresses, config.getPayloadBytes());
        } catch(RuntimeException x){
            Itch.log.log(Level.SEVERE, "An unexpected exception occurred", x);
            System.exit(1);
        }

        // note: the system will not exit because the socket acceptor thread is not a daemon
        //       I suppose it might make sense to just put that loop here
    }

    private static Logger setupLogging(Configuration config, String hostname) throws IOException {
        String logFileName = hostname == null ? "itch.log" : "itch_" + hostname + ".log";
        Logger result = Logger.getLogger("itch");
        FileHandler fileHandler = new FileHandler(logFileName, config.getMaxLogFileMegabytes() * 1024 * 1024, 1);

        Formatter formatter = new Formatter(){
            // TODO - research whether this instance can be called from multiple threads and
            // whether SimpleDateFormat is thread safe. If safe, make the SimpleDateFormat
            // object a member of this formatter

            @Override
            public String format(LogRecord record) {
                if (record.getThrown() == null) {
                    return new SimpleDateFormat(TIMESTAMP_FORMAT).format(record.getMillis()) + " "
                            + record.getLevel().toString() + " : " + record.getMessage() + "\n";
                } else {
                    return new SimpleDateFormat(TIMESTAMP_FORMAT).format(record.getMillis()) + " "
                            + record.getLevel().toString() + " : " + record.getMessage() + " : " + record.getThrown() + "\n";

                }
            }
        };

        result.setLevel(Level.INFO);  // change this for debug
        result.setUseParentHandlers(false);

        fileHandler.setFormatter(formatter);
        result.addHandler(fileHandler);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.WARNING);
        consoleHandler.setFormatter(formatter);
        result.addHandler(consoleHandler);

        System.out.println("logging  to " + new File(logFileName).getAbsolutePath());
        return result;
    }

    /**
     * Parses the YAML configuration file and returns a Configuration object.
     * This method will print a message and return null if the configuration cannot be parsed.
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
     * @return an array of InetSocketAddress the same size as the input. If any item in the input
     * cannot be parsed, a message will be logged and the corresponding item in the output will be null;
     */
    private static InetSocketAddress []parseMemberNames(String []memberNames){
        Pattern ipv4pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{2,5})");
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


    /// instance state 
    private SocketAcceptorThread socketAcceptor;
    private ServerSocketChannel serverSocketChannel;
    private InetSocketAddress []addresses;
    private int localAddressNum;
    private LinkedList<HeartBeatReader> heartbeatReaders;
    private LinkedList<HeartBeatWriter> heartbeatWriters;
    private ScheduledExecutorService executor;
    private HeartBeatFactory heartBeatFactory;

    public Itch(InetSocketAddress []addresses, int payloadBytes){
        this.heartBeatFactory = new HeartBeatFactory(payloadBytes);

        this.addresses = addresses;
        this.heartbeatReaders = new LinkedList<>();
        this.heartbeatWriters = new LinkedList<>();
        this.executor = Executors.newScheduledThreadPool(4);

        // set up the serverSocketChannel - this has to happen before attempting to open outbound sockets
        this.serverSocketChannel = openServerSocket();
        if (this.serverSocketChannel == null) {
            throw new RuntimeException("An erorr occurred while opening the ServerSocketChannel");
        }

        // set up the outbound hearbeat writers
        for(int i=0;i < addresses.length; ++i){
            if (i == localAddressNum) continue;

            try {
                SocketChannel channel = waitForConnection(addresses[i]);
                channel.configureBlocking(false);
                HeartBeatWriter hbWriter = new HeartBeatWriter(channel, heartBeatFactory);
                heartbeatWriters.add(hbWriter);
                executor.scheduleAtFixedRate(hbWriter, 1, 1, TimeUnit.SECONDS);
            } catch(IOException x){
                throw new RuntimeException("An error occurred while opening outbound connections: " + x.getMessage());
            }
        }

        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run(){
                close();
            }
        });

        // start accepting connections
        this.socketAcceptor = new SocketAcceptorThread();
        socketAcceptor.start();

        // start the late heartbeat monitor thread
        executor.scheduleAtFixedRate(new Runnable(){
            @Override
            public void run() {
                DateFormat fmt = new SimpleDateFormat(TIMESTAMP_FORMAT);
                long now = System.currentTimeMillis();
                for(HeartBeatReader reader: heartbeatReaders){
                    if (  now - reader.getLastHeartbeat() > 10000){
                        Itch.log.warning("there have been no heart beats from " + reader.getRemoteAddress() + " since " + fmt.format(reader.getLastHeartbeat()));
                    }
                }
            }
        }, 30, 10, TimeUnit.SECONDS);
    }

    private SocketChannel waitForConnection(InetSocketAddress address) throws IOException {
        int attemptLimit = 120;
        SocketChannel channel = SocketChannel.open();
        for(int attempt =0; attempt < attemptLimit; ++attempt){
            try {
                channel.connect(address);
                break; // BREAK
            } catch(IOException x){
                Itch.log.log(Level.INFO,"An error occurred during attempt " + (attempt + 1) + "/" +  attemptLimit + " to connect to " + address, x);
                if (attempt + 1 < attemptLimit){
                    Itch.log.info("Will try again in 1s");
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException ix){
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during wait between connection attempts");
                    }

                    // if the other side refuses the connection this channel will be closed and further
                    // connect attempts won't work
                    if (!channel.isOpen()) channel = SocketChannel.open();
                }
            }
        }

        Itch.log.info("Established connection to " + address);
        return channel;
    }

    private void close(){
        Itch.log.info("Itch is shutting down.");

        // shut down the socket acceptor
        socketAcceptor.interrupt();
        try {
            socketAcceptor.join(1000);
        } catch(InterruptedException ix){
            Itch.log.warning("The shutdown hook was interruped while waiting for the SocketAcceptorThread to stop");
        }

        // shut down executor threads 
        executor.shutdown();

        // shut down heartbeat readers
        Itch.log.info("Closing inbound connections");
        for(HeartBeatReader hbReader: heartbeatReaders){
            hbReader.close();
        }

        Itch.log.info("Closing outbound connections");
        for(HeartBeatWriter hbWriter: heartbeatWriters){
            hbWriter.close();
        }

        Itch.log.info("Shutdown is complete.");
    }

    private  ServerSocketChannel openServerSocket(){
        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
        } catch (IOException e) {
            Itch.log.severe("Failed to open server socket." + e);
            return null;  // RETURN
        }

        SocketAddress localAddress = null;
        for(int i=0; i < addresses.length; ++i){
            InetSocketAddress address = addresses[i];
            // these have already been checked for null

            try {
                localAddress = serverSocketChannel.getLocalAddress();
                if (localAddress == null){
                    serverSocketChannel.bind(address);
                    localAddress = serverSocketChannel.getLocalAddress();
                    localAddressNum = i;
                    break; // BREAK
                }
            } catch(IOException iox){
                // this is expected
            }
        }
        
        if (localAddress == null){
            Itch.log.severe("None of the members specify an interface on this host");
            return null;  // RETURN
        }

        Itch.log.info("listening on " + localAddress);
        return serverSocketChannel;
    }

    private class SocketAcceptorThread extends Thread {
        public SocketAcceptorThread(){
            super();
            this.setDaemon(false);
        }

        @Override
        public void run() {
            while(! isInterrupted()){
                try {
                    SocketChannel socket = serverSocketChannel.accept();
                    socket.configureBlocking(false);

                    Itch.log.info("Received a new connection from " + socket.getRemoteAddress());

                    HeartBeatReader hbReader = new HeartBeatReader(socket, heartBeatFactory);
                    heartbeatReaders.add(hbReader);
                    executor.scheduleAtFixedRate(hbReader, 1, 1, TimeUnit.SECONDS);
                } catch(ClosedByInterruptException cbix){
                    // this is expected during shutdown
                    break;
                } catch(IOException x){
                    Itch.log.log(Level.WARNING, "An error occurred while accepting a connection.", x);
                }
            }
            Itch.log.info("SocketAcceptorThread has finished");
        }
    }
}
