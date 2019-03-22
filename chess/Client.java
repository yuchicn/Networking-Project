/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

// NOTE ON THE DIFFERENCE BETWEEN COMMUNICATION WITH THE SERVER AND OPPONENT
// All communication with the server will have a response from the server,
// usually with "SUCCESS" if it was successful. When speaking with an opponent,
// messages are simply forwarded. There will be no SUCCESS case for this. Instead
// messages showing up from the opponent will be handled as the default case
// for each case.

import chess.game.Alignment;
import chess.game.Coordinate;
import chess.game.Reference;
import chess.gui.GameProperties;
import chess.gui.IGUIEventListener;
import chess.gui.StateProperties;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author yu-chi
 */
public class Client {
    /**
     * @var socket  Client's TCP socket.
     */
    private Socket socket = null;
    
    /**
     * @var listener  Client's thread to listen for communication from the server.
     */
    private Thread listener = null;
    
    /**
     * @var processor  Client's thread to process messages from listener
     */
    private Thread processor = null;
    
    /**
     * @var istream  Client's input stream.
     */
    private InputStream istream = null;
    
    /**
     * @var obj_istream  Client's object input stream for Java Serialization.
     */
    private ObjectInputStream obj_istream = null;
    
    /**
     * @var ostream     Client's output stream.
     */
    private OutputStream ostream = null;
    
    /**
     * @var obj_ostream Client's object output stream.
     */
    private ObjectOutputStream obj_ostream = null;
    
    /**
     * 
     */
    Reference<IGUIEventListener> gui = IGUIEventListener.getNullListener();
    
    /**
     * 
     */
    BlockingQueue<Message> received = new LinkedBlockingDeque<>();

    public Client(String host, int port, Reference<IGUIEventListener> gui) throws IOException {   
        this(host, port);
        this.gui = gui;
    }
    
    public Client(String host, int port) throws IOException {
        connect(host, port, true);
    }
    
    public final InetAddress connectLocal(int port) throws IOException {
        
        DatagramSocket broadcastSocket = new DatagramSocket();
        broadcastSocket.setBroadcast(true);
        broadcastSocket.setReuseAddress(true);
        broadcastSocket.setSoTimeout(300);
        
        for (int i = 0; i < 3; ++i) {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface neti = interfaces.nextElement();

                if (neti.isLoopback() || !neti.isUp())
                    continue;

                for (InterfaceAddress addr : neti.getInterfaceAddresses()) {   
                    try {
                        if (addr == null)
                            continue;

                        System.out.println("Searching for local server");
                        InetAddress broadcastAddress = addr.getBroadcast();
                        if (broadcastAddress == null)
                            continue;

                        String message = "ITACSIDH?"; // is there a chess server in da house?
                        byte[] buffer = message.getBytes();

                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
                        broadcastSocket.send(packet);
                        System.out.println("Broadcast sent");
                        broadcastSocket.receive(packet);
                        String response = new String(packet.getData());
                        String expected = "TIACSIDH!"; // there is a chess server in da house!
                        if (response.equals(expected)) {
                            System.out.println("Response received: " + packet.getAddress().toString());
                            return packet.getAddress();
                        } else {
                            System.out.println("False response received: " + response);
                        }
                    } catch (IOException e) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "{0} when connecting to {1}", new Object[] {e.getClass().getName(), addr});
                    }
                }
            }
        }
        throw new java.net.ConnectException("Unabled to find local hosts");
    }
    
    public final void connect(String host, int port, boolean searchLocal) throws IOException {
        if (host == null)
            throw new NullPointerException("host is null");
        
        if (socket != null) {
            disconnect();
            try {
                Thread.sleep(400);
            } catch (InterruptedException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        InetAddress addr = InetAddress.getByName(host);
        System.out.println("linklocal: " + addr.isLinkLocalAddress());
        System.out.println("anylocal: " + addr.isAnyLocalAddress());
        System.out.println("sitelocal: " + addr.isSiteLocalAddress());
        
        System.out.println("client connecting to " + host + ":" + port);
        if (!addr.isReachable(3000)) {
            InetAddress localaddr = connectLocal(port + 1);
            connect(localaddr.getHostAddress(), port, false);
            return;
        }
        
        System.out.println("making socket");
        socket = new Socket(host, port);
        ostream = socket.getOutputStream();
        obj_ostream = new ObjectOutputStream(ostream);
        istream = socket.getInputStream();
        obj_istream = new ObjectInputStream(istream);
        listener = new Thread(new ListenThread());
        listener.start();
        processor = new Thread(new ProcessThread());
        processor.start();
    }
    
    public void disconnect() {
        try {
            if (socket != null) {
                if (!socket.isInputShutdown())
                    socket.shutdownInput();
                if (!socket.isOutputShutdown())
                    socket.shutdownOutput();
                if (!socket.isClosed())
                    socket.close();
            }
            socket = null;
            
            if (processor != null) {
                processor.interrupt();
            }
            processor = null;
            
            if (listener != null)
                listener.interrupt();
            listener = null;
            
            ostream = null;
            istream = null;
            obj_istream = null;
            obj_ostream = null;
            
            received.clear();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean send(Message message) {
        if (message == null)
            return false;
        
        if (socket == null || !socket.isConnected() || socket.isInputShutdown() || socket.isOutputShutdown() || socket.isClosed())
            return false;
        
        try {
            obj_ostream.writeObject(message);
            obj_ostream.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public void register(String name) {
        System.out.println("Trying to register user: " + name);
        send(new Message(
            name,
            "REGISTER",
            null
        ));
    }
    
    public void list() {
        System.out.println("Asking the server for an updated list of users/games");
        send(new Message(
            null,
            "LIST",
            null
        ));
    }
    
    public void makeGame() {
        System.out.println("Telling the server I want to make a game");
        send(new Message(
            null,
            "MAKEGAME",
            null
        ));
    }
    
    public void joinGame(String name) {
        System.out.println("Asking to join game: " + name);
        send(new Message(
            null,
            "JOINGAME",
            name
        ));
    }
    
    public void pickColor(String color) {
        System.out.println("Picking the color " + color);
        send(new Message(
            null,
            "PICKCOLOR",
            color       
        )); 
    }
    
    public void ready(String status) {
        System.out.println("Setting ready status to " + status);
        send(new Message(
            null,
            "READY",
            status       
        )); 
    }
    
    public void startGame() {
        System.out.println("Asking server to start the game");
        send(new Message(
            null,
            "STARTGAME",
            null       
        )); 
    }
    
    public void chat(String message) {
        System.out.println("Sending a chat message: " + message);
        gui.ref().chatMessage(gui.ref().getProperty(StateProperties.DISPLAY_NAME), message);
        send(new Message(
            gui.ref().getProperty(StateProperties.DISPLAY_NAME),
            "CHAT",
            message
        ));
    }
    
    public void unregister() {
        System.out.println("Leaving main lobby");
        send(new Message(
            null,
            "UNREGISTER",
            null       
        )); 
    }
    
    public void leaveLobby() {
        System.out.println("Leaving session lobby");
        send(new Message(
            null,
            "EXITLOBBY",
            null       
        )); 

        // TODO: send leave message
    }
    
    public void forfeit() {
        System.out.println("Leaving game session");
        // TODO: send forfeit message
        send(new Message(
            null,
            "FORFEIT",
            null       
        )); 
    }
    
    public void requestDraw() {
        System.out.println("Requesting game draw");
        // TODO: send draw request message
        // Maybe server should handle draw negotiation/state?
       
        gui.ref().setProperty(StateProperties.I_AM_DRAWING, true);
        
        send(new Message(
            null,
            "DRAW",
            "REQUEST"       
        )); 
    }
    
    public void agreeOnDraw() {
        // Check that the opponent initiated a draw.
        if (gui.ref().<Boolean>getProperty(StateProperties.OPPONENT_IS_DRAWING) == false) {
            System.out.println("The opponent hasn't asked for a draw.");
            return;
        }
        
        System.out.println("Agreeing to the draw.");
        send(new Message(
            null,
            "DRAW",
            "AGREED"       
        )); 
    }
    
    public void declineDraw() {
        // Check that the opponent initiated a draw.
        if (gui.ref().<Boolean>getProperty(StateProperties.OPPONENT_IS_DRAWING) == false) {
            System.out.println("The opponent hasn't asked for a draw.");
            return;
        }
        
        System.out.println("Agreeing to the draw.");
        send(new Message(
            null,
            "DRAW",
            "DECLINED"       
        )); 
    }
    
    public void cancelDrawRequest() {
        System.out.println("Cancelling the draw request.");
        
        gui.ref().setProperty(StateProperties.I_AM_DRAWING, false);
        
        send(new Message(
            null,
            "DRAW",
            "CANCELLED"       
        )); 
    }
    
    public void move(Coordinate from, Coordinate to) {
        if (!gui.ref().<Boolean>getProperty(StateProperties.MY_TURN)) {
            System.out.println("Rejected movement. It's not my turn.");
            gui.ref().displayError("It is " + gui.ref().getProperty(StateProperties.OPPONENT_NAME) + "'s turn");
            return;
        }
        System.out.println(String.format("Moving chess piece from %s to %s", from , to));
        send(new Message(
            gui.ref().getProperty(StateProperties.DISPLAY_NAME),
            "MOVE",
            String.format("%s %s", from, to)
        ));
        gui.ref().setProperty(StateProperties.MY_TURN, false);
        // TODO: send move message
    }
    
    public void ackMove(String player, Coordinate from, Coordinate to) {
        if (gui.ref().<Boolean>getProperty(StateProperties.MY_TURN)) {
            System.out.println("Rejecte move from opponent because it's my turn.");
            send(new Message(
                player,
                "MOVE",
                "NOTYOURTURN"
            ));
            return;
        }
        System.out.println(String.format("Approving movement of chess piece from %s to %s", from , to));
        send(new Message(
            player,
            "MOVE",
            String.format("%s %s", from, to)
        ));
        gui.ref().setProperty(StateProperties.MY_TURN, true);
        // TODO: send movement approval message
    }
    
    public void askForUpdatedLeaderboard() {
        send(new Message(
            null,
            "LEADERBOARD",
            null
        ));
    }
    
    public void gracefulQuit() {
        send(new Message(
            null,
            "QUIT",
            null
        ));
    }
        
    public static int sortUsersByStatus(String status1, String status2) {
        // If either status is null, the non-null status is greater
        if (status1 == null)
            return status2 == null ? 0 : -1; 
        if (status2 == null)
            return 1;
        // Extract the status from the status messages
        final Pattern statusRegex = Pattern.compile("^([A-Za-z0-9]+)(?: \\(([^)]+)\\))$");
        Matcher match1 = statusRegex.matcher(status1);
        Matcher match2 = statusRegex.matcher(status2);
        boolean matches1 = match1.matches();
        boolean matches2 = match2.matches();
        GameSession.GameState state1 = (matches1 && match1.groupCount() > 1) ? 
                GameSession.GameState.parseStatus(match1.group(2)) : null;
        GameSession.GameState state2 = (matches2 && match2.groupCount() > 1) ? 
                GameSession.GameState.parseStatus(match2.group(2)) : null;
        // Compare state order
        int stateComparison = GameSession.GameState.compare(state1, state2);
        if (stateComparison != 0)
            return stateComparison;
        // continue if the states are the same
        // Extract the names from the status messages
        String name1 = (matches1 && match1.groupCount() > 0) ? match1.group(1) : status1;
        String name2 = (matches2 && match2.groupCount() > 0) ? match2.group(1) : status2;
        // compare the order of the status messages and then names
        return name1.compareTo(name2);
    }
    
    class ListenThread implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Message msg = null;

                    try {
                        Message m = receive();
                        if (m != null)
                            received.put(m);
                    } catch (SocketException e) {
                        gui.ref().connectionFailed(e.getMessage());
                    } catch (IOException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, ex.getClass().getName() + " occurred while receiving");
                    }
                } 
            } catch (InterruptedException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.WARNING, "client: listen thread interrupted");
            } finally {
                System.out.println("client ListenThread ended");
                disconnect();
                listener = null;
            }
        }
        
        Message receive() throws IOException {
            Object object = null;
            Message message = null;
            
            try {
                object = obj_istream.readObject();
                message = (Message)object;
                return message;
            } catch (EOFException e) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "End of stream - client will now disconnect", e);
                disconnect();
                Thread.currentThread().interrupt();
                return null;
            } catch (ClassNotFoundException | ClassCastException e) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, e.getClass().getName() + " while receiving");
                return null;
            }
        }
    }
        
    class ProcessThread implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                        Message msg = received.poll(2, TimeUnit.SECONDS);
                        processMessage(msg);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.WARNING, "client: process thread interrupted");
            } finally {
                System.out.println("client ProcessThread ended");
                disconnect();
                processor = null;
            }
        }
        
        
        void processMessage(Message message) {
            if (message == null)
                return;
            
            switch (message.opcode) {
                // All possible server responses with the "REGISTER" opcode.
                case "REGISTER":
                    switch (message.body) {
                        case "SUCCESS":
                            // You are now registered on the server. Add any code
                            // to handle this event here.
                            System.out.println("Received a successful acknowledgement from the server! This user is now registered.");
                            gui.ref().registrationApproved();
                            //Client.this.makeGame(); // this would create an infinite loop
                            break;
                    
                        case "NAME_IN_USE":
                            // The user entered a name already in use. Add any code
                            // to handle getting a new name from the user here.
                            gui.ref().registrationConflict();
                            break;
                    
                        case "INVALID_NAME":
                            // The user entered an invalid name. Add any code to
                            // handle getting a new name from the user here.
                            gui.ref().registrationInvalid();
                            break;
                            
                        default:
                            // The default case is the server sending a registration
                            // request. This means any type of mesage was sent to
                            // the server, but the user has not yet registered, so
                            // the server ignored it and sent a registration request.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;
                    }
                    break;
                
                case "UNREGISTER":    
                    switch (message.opcode) {
                        case "IN_GAME_CANT_UNREGISTER":
                            break;
                        case "SUCCESS":
                            break;
                    }
                    break;
                    
                // All possible responses from the server with the LIST opcode.
                case "LIST":
                    // Make sure there's a body to the message, or something went
                    // wrong.
                    if (message.body == null) {
                        break;
                    }

                    // Parse the message body. The lobby information will be sent as
                    // a string formatted as username1,username2,username3~gamename1:playerCount,gamename2:playercount
                    String[] data = message.body.split("~");

                    // Can either have 1 or 2 sets of data here.
                    if (data.length <= 0 || data.length > 2)
                        break;

                    // Get and sort all playerNames.
                    String[] players = data[0].split(",");
                    Arrays.sort(players, (n1, n2) -> sortUsersByStatus(n1, n2));
                    
                    System.out.println(Arrays.toString(players));
                    gui.ref().setProperty(StateProperties.PLAYERS_LIST, players);

                    // Try and ref info about games, if any was sent. If not,
                    // "games" will be null.
                    String[] gameData;
                        
                    LinkedList<String> gameNames = new LinkedList<>();

                    if (data.length == 2) {
                        gameData = data[1].split(",");
                        System.out.println("data[1]: " + data[1]);
                        for (String info : gameData) {
                            String name = info.substring(0, info.length() - 2);
                            gameNames.push(name);
                            gui.ref().pushSession(
                                    name, 
                                    Integer.parseInt(info.substring(info.length() - 1, info.length())));
                        }
                    }
                    gui.ref().popSession((name) -> !gameNames.contains(name)); // remove all sessions not in this list
                    
                    // Call some function to handle updating the interface. Your two variables
                    // are "players" for the names of all players and "games" for the list of all games + their player counts.


                    /*
                    // This code block shows you how to process info about the players/games.
                    // It is not meant to actually be run, as it will throw errors when games is
                    // null.
                    System.out.println("Current player list:");
                    for (String player : players) {
                        System.out.println(player);
                    }

                    if (games == null)
                        break;

                    System.out.println("Current games list:");

                    Enumeration<String> key_set = games.keys();
                    String currentGame;
                    String playerCount;
                    while (key_set.hasMoreElements()) {
                        currentGame = key_set.nextElement();
                        playerCount = games.ref(currentGame);
                        System.out.println(currentGame + " " + playerCount + "/2 players");
                    }*/

                    break;

                // All possible server responses to the opcode MAKEGAME
                case "MAKEGAME":
                    switch (message.body){
                        case "ALREADY_IN_GAME":
                            System.out.println("Sever said I was already in a game.");
                            break;
                        case "SUCCESS":
                            System.out.println("Successfully made a game.");
                            gui.ref().enterSession(null);
                            gui.ref().setProperty(StateProperties.OPPONENT_NAME, "(waiting)");
                            break;
                    }
                    break;

                // All possible server responses to the opcode JOINGAME
                case "JOINGAME":
                    switch (message.body) {
                        case "SUCCESS":
                            // Successfully joined the game.
                            System.out.println("Successfully joined the game.");
                            gui.ref().enterSession(null);
                            break;

                        case "GAME_IS_FULL":
                            // The game the user tried to join is full. This should
                            // not happen often. Maybe check the player count in the
                            // UI before sending the join request.
                            gui.ref().enterSession("Game session is full"); // display error, accept new join requests

                            break;

                        case "NO_GAME_FOUND":
                            // This happens if the user tries to join a game that
                            // can't be found. Maybe the host closed their game.
                            gui.ref().enterSession("Game session not found"); // display error, accept new join requests

                            break;

                        case "NO_GAME_SELECTED":
                            // This case may be meaningless if you implement the UI
                            // so a message is only send when a game is selected.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            // The UI does that ↑
                            break;

                        case "OPPONENT_JOINED":
                            // This informs us that an opponent has joined our game
                            // session. This requires us to notify them what color
                            // we have picked. I leave the color variable to you,
                            // but it must eventually be reduced to a String for
                            // the message.
                            
                            gui.ref().opponentJoined(message.sender); // how to get the opponent's name?
                            // pickColor(color);
                            break;
                    }
                    break;

                // ALl possible server responses to the opcode PICKCOLOR
                case "PICKCOLOR":
                    switch (message.body) {
                        case "NOT_IN_A_GAME":
                            // This case is for when the request is sent when the user
                            // has a null GameSession. Should be able to code the
                            // UI so the message is never sent in this case.
                            gui.ref().syncNotInGame();
                            break;

                        case "NOT_IN_LOBBY":
                            // This happens if the user tries to send a color request
                            // when the game has already started. Again, shouldn't
                            // ever happen.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;

                        case "NO_OPPONENT":
                            // This case should happen when a player changes their
                            // color, but they have no opponent. They should still
                            // send this message in case someone joins immediately.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;

                        default:
                            // This is the case where the opponent has sent a PICKCOLOR
                            // message and it's been forwarded to this client.
                            gui.ref().switchColor(Alignment.valueOf(message.body));
                            break;
                    }
                    break;

                // All possible server responses to the opcode READY
                case "READY":
                    switch (message.body) {
                        case "NOT_IN_A_GAME":
                            // Tried to ready up be this Client wasn't in a game.
                            // Should handle this through the UI, so this case
                            // should  never happen.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;

                        case "NOT_IN_LOBBY":
                            // Can only ready up when in lobby, not after game has
                            // started. Again, handle this in the UI probably.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;

                        case "GAME_NOT_FULL":
                            // Cannot ready up until you have an opponent.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;

                        // This is us receiving a ready message from the opponent.
                        // If the body equals YES, it means they moved into a ready
                        // state, otherwise it means they want to be marked unready.
                        default: 
                            if (message.body.equals("YES")) {
                                // Mark other player as ready.
                                gui.ref().setProperty(StateProperties.OTHER_READY_STATE, true);
                            } else {
                                // Mark other player as not ready.
                                gui.ref().setProperty(StateProperties.OTHER_READY_STATE, false);
                            }
                            break;
                    }
                    break;
                
                // All possible server responses to opcode STARTGAME
                // NOTE: MUST CHECK THAT COLORS ARE SELECTED CLIENT-SIDE
                // AS THE SERVER DOES NOT TRACK WHO IS BLACK/WHITE
                case "STARTGAME": {
                    switch (message.body) {
                        case "SUCCESS":
                            // Game was successfully started. Both players are
                            // notified.
                            gui.ref().startGame();
                            
                            // Assign myTurn based on what color we are.
                            if (Alignment.WHITE == gui.ref().getProperty(StateProperties.ALIGNMENT_COLOR))
                                gui.ref().setProperty(StateProperties.MY_TURN, true);
                            break;
                            
                        case "PLAYERS_NOT_READY":
                            // One or both players were not readied up.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;
                            
                        case "GAME_NOT_FULL":
                            // Can't start without a full game.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;
                            
                        case "NOT_IN_LOBBY":
                            // Should never happen, validate this on UI side.
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;
                            
                        case "NOT_IN_A_GAME":
                            gui.ref().syncNotInGame();
                            break;
                        }
                    }
                    break;
                    
                // All possible server responses to opcode MOVE
                case "MOVE":
                    switch (message.body) {
                        case "NOT_IN_A_GAME":
                        case "NOT_IN_PLAY_STATE":
                            // Can't make a move if not in a game. These 2
                            // cases are similar. NOT_IN_A_GAME = game session is null
                            // NOT_IN_PLAY_STATE = still in lobby
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;
                            
                        default:
                            // Default case is receiving message from opponent.
                            // Handle that here.
                            {
                                String[] fields = message.body.split(" ");
                                if (fields.length == 2) {
                                    Coordinate from = new Coordinate(fields[0]);
                                    Coordinate to = new Coordinate(fields[1]);
                                    gui.ref().movePiece(message.sender, from, to);
                                } else {
                                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "Invalid parameter count for MOVE: " + message.body);
                                }
                            }
                            break;
                    }
                    break;
                    
                // All possible server responses to opcode FORFEIT. This also
                // covers a checkmate. If FORFEIT happens while the game is
                // in a state of checkmate, treat it as a checkmate not forfeit.
                case "FORFEIT":
                    if (message.body == null) message.body = "";
                    switch (message.body) {
                        case "NOT_IN_A_GAME":
                        case "NOT_IN_PLAY_STATE":
                            // Can't forfeit if not in a game. These 2
                            // cases are similar. NOT_IN_A_GAME = game session is null
                            // NOT_IN_PLAY_STATE = still in lobby
                            gui.ref().logDroppedEvent(message.opcode, message.body); // log dropped event
                            break;
                            
                        default:
                            // Default case is receiving message from opponent.
                            // Handle that here.
                            gui.ref().opponentLeft();
                            
                            break;
                    }
                    break;
                    
                case "DRAW":
                    String body = (message.body == null) ? "" : message.body;
                    switch (body) {
                        case "NOT_IN_A_GAME":
                        case "GAME_IS_NOT_ACTIVE":
                            // This is a response to THIS client trying to ask
                            // for a draw when they aren't in a game.
                            gui.ref().displayError(message.body.toLowerCase().replaceAll("_", " "));
                            break;                        
                        
                        case "REQUEST":
                            // Opponent is requesting a draw.
                            gui.ref().setProperty(StateProperties.OPPONENT_IS_DRAWING, true);
                            gui.ref().chatMessage(
                                    String.format("(%s)", gui.ref().<String>getProperty(StateProperties.OPPONENT_NAME)),
                                    "Requesting a draw");
                            gui.ref().setProperty(StateProperties.OPPONENT_IS_DRAWING, true);
                            break;
                        
                        case "DECLINED":
                            // Opponent has declined this client's request for
                            // a draw.
                            //Client.this.gui.ref().setProperty(StateProperties.I_AM_DRAWING, false);
                            gui.ref().setProperty(StateProperties.OPPONENT_IS_DRAWING, false);
                            break;
                            
                        case "CANCELLED":
                            // Opponent cancelled their request for a draw.
                            gui.ref().<Boolean>setProperty(StateProperties.OPPONENT_IS_DRAWING, false);
                            gui.ref().setProperty(StateProperties.OPPONENT_IS_DRAWING, false);
                            gui.ref().chatMessage(
                                    String.format("(%s)", gui.ref().<String>getProperty(StateProperties.OPPONENT_NAME)),
                                    "Cancelled the draw");
                            
                            break;
                            
                        case "AGREED":
                            // Opponent has agreed to a draw. The server will
                            // have already killed the GameSession, so handle
                            // this on our side.
                            gui.ref().setProperty(StateProperties.OPPONENT_IS_DRAWING, true);
                            break;
                    }
                    break;                    
                    
                // All possible server responses to the opcode EXITLOBBY. This is
                // either going to be some form of error, or a confirmation that
                // the exit was successful. For the case where the host leaves
                // and it closes the lobby, see the 
                case "EXITLOBBY":
                    switch (message.body) {
                        case "NOT_IN_A_GAME":
                        case "NOT_IN_A_GAME_LOBBY":
                            // These two cases are for if a player tries to leave
                            // a game lobby when they either aren't in a game, or
                            // their game is not in the Lobby state. Might have to
                            // split them, but I don't think so.
                            gui.ref().displayError(message.body.toLowerCase().replaceAll("_", " "));               
                            break;
                            
                        case "OPPONENT_LEFT":
                            // The opponent left the lobby. This notifies the
                            // client that the opponent has left their lobby.
                            // This is for the case where this player is the
                            // host, so the lobby remains open.
                            gui.ref().opponentLeft();
                            
                            break;
                            
                        case "SUCCESS":
                            // This means this client's request to leave
                            // the lobby was successful.
                            
                            // The client has already left the game session
                            gui.ref().syncNotInGame();
                            gui.ref().chatMessage(GameProperties.INFO_SENDER, "Exited the lobby");
                            
                            break;                           
                    }
                    break;
                
                    
                // This is the server's response when the leaderboard is updated
                // or has been requested by the client.    
                case "LEADERBOARD":
                    if (message.body == null)
                        return;
                    
                    String[] recentWinners;
                    recentWinners = message.body.split(",");
                    
                    // reverse the array
                    for (int i = 0; i < recentWinners.length / 2; ++i) {
                        String swap = recentWinners[i];
                        recentWinners[i] = recentWinners[recentWinners.length - i - 1];
                        recentWinners[recentWinners.length - i - 1] = swap;
                    }
                    
                    // Add this to the GUI.
                    gui.ref().setProperty(StateProperties.SCOREBOARD, recentWinners);
                    
                    break;
                    
                // For whatever reason, the game session this client was a part
                // of has been closed by the server due to something happening
                // with their opponent.
                case "GAMECLOSEDBYSERVER":
                    
                    gui.ref().syncNotInGame();
                    break;
                
                // The case where the user receives a message from the server
                // with a new chat message to send.
                case "CHAT":
                    
                    Pattern chatPattern = Pattern.compile("(?:([^:]{3,15})[:]|)(.*)");
                    Matcher fields = chatPattern.matcher(message.body);
                    if (!fields.matches()) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "Invalid chat message format" + message.body);
                        break;
                    }
                    gui.ref().chatMessage(
                            fields.group(1),
                            fields.group(2));
                    
                    break;
                    
                default: break;
            }
        }
    }
}
