/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import chess.gui.GameProperties;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yu-chi
 */
public class Server {
    
    /**
     * @var games   A hash map of all games, regardless of state. Keys are the 
     *              name of the player who created the game.
     */
    private ConcurrentHashMap<String, GameSession> games;
    
    /**
     * @var users   A hash map of all active users (ClientConnection). Keys are 
     *              the user's names.
     */
    private ConcurrentHashMap<String, ClientConnection> users;
    
    /**
     * @var winners A vector of the most recent winners.
     */ 
    private Vector<String> winners;
    
    /**
     * The interval between optimized update broadcasts, in milliseconds
     */
    private static final int OPTIMIZED_UPDATE_FREQUENCY = 400;
    private OptimizedUpdateThread optimizedUpdateThread;
    
    private boolean keepAlive = false;
    private boolean stopped = false;
    
    /**
     * Constructor initializes the hash maps.
     */
    public Server() {
        games = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
        winners = new Vector<>();
    }
    
    /**
     * Starts up the server. This function has an infinite loop that listens
     * for new connections and spawns a thread to deal with each one.
     * 
     * @param port      Port #.
     * @param backlog   
     * @return boolean  Shouldn't actually return, unless something goes wrong.
     */
    public boolean start(int port, int backlog) {
        // Prevent this server from being started multiple times, so the state is completely reset.
        if (stopped) // was started, is stopped
            throw new IllegalStateException("Server instance has been stopped. Please construct a new Server.");
        if (keepAlive) // was started, is running
            throw new IllegalStateException("Server has already been started.");
        
        ServerSocket server_sock;
        
        try {
            server_sock = new ServerSocket(port, backlog);
            server_sock.setReuseAddress(true);
        } catch (IOException e) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
        
        keepAlive = true;
        
        // Start an OptimizedUpdateThread that will reduce the overhead of 
        // broadcasting messages such as "LIST".
        {
            optimizedUpdateThread = 
                    new OptimizedUpdateThread(OPTIMIZED_UPDATE_FREQUENCY);
            Thread t = new Thread(optimizedUpdateThread);
            t.start();
        }
        
        // Start a thread for listening for pings from clients on the local network
        {
            Thread t = new Thread(new LocalListenerThread());
            t.start();
        }
        
        // Begin an infinite loop to listen for new connections. For each one,
        // spawn a thread to handle it.
        while (keepAlive) {
            try {
                Socket client_sock = server_sock.accept();
                System.out.println("server accepted connection from " + client_sock.getInetAddress());
                Thread t = new Thread(new ClientConnection(client_sock));
                t.start();
            } catch (IOException e) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
                System.out.println("Error: " + e);
            }
        }
        
        return true;
    }
    
    /**
     * Stops the server and discontinues the associated threads.
     */
    public void stop() {
        System.out.println("Stopping the Server");
        keepAlive = false; // stop the threads
        stopped = true; // prevent this server instance from starting again
    }
    
    /**
     * 
     * @param name
     * @throws java.io.IOException
     */
    public void recordWinner(String name) throws IOException {
        if (name == null)
            return;
        
        if (winners.size() >= 10)
            winners.remove(0);
        
        winners.add(name);
               
        String leaderboard = "";
        
        for (int i = 0; i < winners.size(); i++) {
            leaderboard += winners.get(i);
            if (i < winners.size() - 1)
                leaderboard += ",";
        }
        
        // Build a reply message.
        Message reply = new Message(
            "SERVER",
            "LEADERBOARD",
            leaderboard
        );
        
        Enumeration<String> key_set = users.keys();
        key_set = users.keys();
        ClientConnection current;
        while (key_set.hasMoreElements()) {
            current = users.get(key_set.nextElement());
            if (current.game == null)
                current.send(reply);
        }
    }
    
    public void getLeaderboard(ClientConnection requester) throws IOException {
        String leaderboard = "";
        
        for (int i = 0; i < winners.size(); i++) {
            leaderboard += winners.get(i);
            if (i < winners.size() - 1)
                leaderboard += ",";
        }
        
        // Build a reply message.
        Message reply = new Message(
            "SERVER",
            "LEADERBOARD",
            leaderboard
        );
        
        requester.send(reply);
    }
    
    public static String playerStatusText(String playerName, GameSession.GameState state) {
        String status = playerName;
        if (state != null) {
            status += " (" + state + ")";
        }
        return status;
    }
    
    private String buildPlayersList() {
        // Holds the key set for each iteration of one of the hash maps. (They
        // both use string keys)
        Enumeration<String> key_set = users.keys();
        
        // Holds the lobby info, will be added to on each loop iteration.
        String lobby = "";
        
        // List all players and their current status.
        while (key_set.hasMoreElements()) {
            String name = key_set.nextElement();
            GameSession session = users.get(name).game;
            GameSession.GameState state = session == null ? null : session.getState();
            String status = playerStatusText(name, state);
            lobby += status;
            if (key_set.hasMoreElements())
                lobby += ",";
        }
        
        // The delimiter is a ~
        lobby += "~";
        
        return lobby;
    }
    
    private String buildGamesList(String playersList) {
        
        String lobby = playersList;
        
        // Now get the keys for the games.
        Enumeration<String> key_set = games.keys();
        GameSession curr_gs;
        String curr_key;
        
        // Each game's name should be added, and thne a color, followed by
        // the number of players in the game (which will always be 1 or 2).
        while (key_set.hasMoreElements()) {
            curr_key = key_set.nextElement();
            curr_gs = games.get(curr_key);
            lobby += curr_key;
            lobby += ":";
            lobby += curr_gs.getPlayerCount();
            if (key_set.hasMoreElements())
                lobby += ",";
        }
        
        return lobby;
    }
    
    private boolean needsLobbyUpdate(ClientConnection user) {
        return user != null && (user.game == null || user.game.getState() != GameSession.GameState.Play);
    }
    
    /**
     * Sends out an update to all clients not currently in a game with the
     * list of players logged in and a list of all active games. The array
     * format is as follows:
     * 
     * Player names first, separated by commas.
     * A ~ will separate player names from games.
     * Games will be listed by their name, a colon, their player count, 
     * and then separated by a comma.
     * 
     * player1,player2~game1:1,game2:1,game3:2
     * 
     * @throws java.io.IOException
     */
    public void updateLobby() throws IOException {
           
        String sessionLobby = buildPlayersList(); // create a list of online players
        String mainLobby = buildGamesList(sessionLobby); // create a list of online players and game sessions
                
        // Build a reply message for players in the main lobby
        Message replyMain = new Message(
            "SERVER",
            "LIST",
            mainLobby
        );
        
        // Build a reply message for players in a session lobby
        Message replySession = new Message(
            "SERVER",
            "LIST",
            sessionLobby
        );
        
        // Now send the reply to every user that's not in a game. Users who
        // are in games don't need to be updated here, as they can't see
        // the lobby anyway.
        Enumeration<String> key_set = users.keys();
        ClientConnection current;
        while (key_set.hasMoreElements()) {
            current = users.get(key_set.nextElement());
            if (needsLobbyUpdate(current))
                current.send(current.game == null ? replyMain : replySession);
        }
    }
    
    /**
     * This function is called when a client specifically requests the updated
     * status of the lobby. It does not multicast.
     * 
     * @param requester     The client who requested the update.
     * @throws IOException 
     */
    public void updateLobby(ClientConnection requester) throws IOException {
        
        // Do not reply to individual lobby update requests when a lobby update is
        // scheduled to broadcast. There's no need to rush.
        if (requester.getName() != null && users.containsKey(requester.getName()) && 
                optimizedUpdateThread.updateLobbyFlag && needsLobbyUpdate(requester))
            return; 
            
        String lobby = buildPlayersList(); // create a list of online players
        
        if (users.get(requester.getName()).game == null) // if the requester is not in a game
            lobby = buildGamesList(lobby); // append the list of game sessions
        
        // Build a reply message.
        Message reply = new Message(
            "SERVER",
            "LIST",
            lobby
        );
        
        // Send the reply to the client who requested it.
        requester.send(reply);
    }

    public void chatToLobby(String name, String message) throws IOException {
        Message reply = new Message(
            "SERVER",
            "CHAT",
            message
        );

        Enumeration<String> key_set = users.keys();
        ClientConnection current;
        while (key_set.hasMoreElements()) {
            current = users.get(key_set.nextElement());
            if (current.game == null && !current.getName().equals(name)) {
                current.send(reply);
            }
        }
    }
    
    private class LocalListenerThread implements Runnable {

        @Override
        public void run() {
            DatagramSocket localSocket = null;
            String message = "TIACSIDH!"; // there is a chess server in da house!
            byte[] buffer = new byte[message.length()];
            try {
                localSocket = new DatagramSocket(GameProperties.serverPort() + 1);
                DatagramPacket packet = new DatagramPacket(buffer, message.length());
                while (true) {
                    localSocket.receive(packet);
                    if (new String(packet.getData()).equals("ITACSIDH?")) { // is there a chess server in da house?
                        System.out.println("Server received local broadcast from " + packet.getAddress());
                        packet.setData(message.getBytes(), 0, buffer.length);
                        localSocket.send(packet);
                    }
                }
            } catch (Exception e) {
                Logger.getLogger(Server.class.getName()).log(Level.WARNING, null, e);
            } finally {
                if (localSocket != null)
                    localSocket.close();
            }
        }
        
    }
    
    /**
     * Optimizes broadcasting some Server events by combining all broadcasts
     * during a time interval into a single broadcast.
     */
    private class OptimizedUpdateThread implements Runnable {
        
        private final long interval;
        
        private boolean updateLobbyFlag = false;
        
        /**
         * Construct and start a new OptimizedUpdateThread
         * @param updateFrequency 
         */
        public OptimizedUpdateThread(long updateFrequency) {
            this.interval = updateFrequency;
        }
        
        /**
         * Tell the thread to updateLobby() in the next performUpdate() call
         */
        public void flagLobbyUpdate() {
            this.updateLobbyFlag = true;
        }
        
        private void performUpdate() {
            // update the players and games list
            if (updateLobbyFlag) {
                updateLobbyFlag = false;
                try {
                    updateLobby();
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        @Override
        public void run() {
            if (!keepAlive || stopped)
                throw new IllegalStateException("Server must be running when the OptimizedUpdateThread is started");
            while (keepAlive) {
                try {
                    Thread.sleep(interval); // delay
                } catch (InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.WARNING, null, ex);
                }
                performUpdate();
            }
        }
        
    }

    private class ClientConnection implements Runnable, IConnectedPlayer {

        /**
         * @var client_sock The client's TCP socket.
         */
        Socket client_sock;
        
        /**
         * @var istream     The client's input stream.
         */
        InputStream istream;
        
        /**
         * @var obj_istream An object-based input stream for Java Serialization.
         */ 
        ObjectInputStream obj_istream;
        
        /**
         * @var ostream     The client's output stream.
         */
        OutputStream ostream;
        
        /**
         * @var obj_ostream An object-based output stream for Java Serialization.
         */
        ObjectOutputStream obj_ostream;
        
        /**
         * @var game        A reference to a GameSession the client is in.
         */
        GameSession game = null;
        
        /**
         * @var playerName  The name of the client.
         */
        String playerName;
        
        /**
         * @var ready       Tracks whether this client has been marked as ready.
         *                  A game cannot start unless both clients have readied
         *                  themselves.
         */
        boolean ready = false;
        
        /**
         * Constructor for a ClientConnection. Builds object streams for
         * use with serialization.
         * 
         * @param client_sock   Client's TCP socket.
         * @throws IOException 
         */
        public ClientConnection(Socket client_sock) throws IOException {
            this.client_sock = client_sock;
            ostream = client_sock.getOutputStream();
            obj_ostream = new ObjectOutputStream(ostream);
            istream = client_sock.getInputStream();
            obj_istream = new ObjectInputStream(istream);
        }
        
        @Override
        public boolean isReady() {
            return this.ready;
        }
        
        @Override
        public void run() {
            // Constantly read messages as long as the socket is connected.
            while (isConnected()) {
                try {
                    Message msg = (Message)obj_istream.readObject();
                    if (msg != null)
                        processMessage(msg);
                    
                } catch (EOFException e) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "Client connection closed");
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "exception while receiving message from " + playerName, e);
                    
                    // If we catch an exception here, it usually means we want to
                    // close this connection and remove all references to it
                    // from the server.
                    try {
                        // Can immediately close the socket.
                        this.client_sock.close();
                        
                        // Remove ourselves from the registered users.
                        Server.this.users.remove(playerName);
                        
                        // Now cleanup by calling the destroyGame function.
                        destroyGame();
                    } catch (IOException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return;
                }
            }
            System.out.println("End of server thread with " + playerName);
        }
        
        @Override
        public void nullGame() {
            game = null;
        }
        
        @Override
        public boolean isConnected() {
            return client_sock.isConnected();
        }

        @Override
        public String getName() {
            return playerName;
        }
        
        @Override
        public void send(Message message) throws IOException {
            synchronized (this) { // Prevent messages from being sent simultaneously
                try {
                    if (isConnected()) {
                        obj_ostream.writeObject(message);
                        obj_ostream.flush();
                    }
                } catch (SocketException e) {
                    
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, 
                            "Failed to {0}: {1}", new Object[]{playerName, e.getClass().getName()});
                    
                    // unregister this user
                    Server.this.users.remove(this.playerName);
                    
                    // Now cleanup by calling the destroyGame function.
                    destroyGame();
                    
                    // Can immediately close the socket.
                    this.client_sock.close();
                }
            }
        }

        /**
         * Destroys all references to a game that this client is a part of. Also
         * sends a message to the opponent, if one exists, informing them that 
         * the game was killed by this client.
         */
        public void destroyGame() throws IOException {
            if (this.game != null) {
                // Get a reference to the other player before we start doing
                // anything to the game. Doesn't matter if it's null.
                IConnectedPlayer other = game.getOtherPlayer(this);
                
                // Remove the game from the active list on the server.
                Server.this.games.remove(playerName);
                
                // Null out the reference
                nullGame();
                            
                send(new Message(
                    "SERVER",
                    "GAMECLOSEDBYSERVER",
                    null
                ));
                
                // Skip this if the other player was not found.
                if (other != null) {
                    // Null out the opponent's stuff too.
                    Server.this.games.remove(other.getName());
                    other.nullGame();
                    // Notify the other player that we've closed down the game.
                    // This will allow their UI to handle the situation. They will
                    // either be presented with a victory by forfeit screen if the
                    // game had started, or a "the lobby has been closed" screen
                    // if the game had not started.
                    other.send(new Message(
                        "SERVER",
                        "GAMECLOSEDBYSERVER",
                        null
                    ));
                }
                
                // The lobby has obviously changed, given that our game has
                // been removed, so call update.
                Server.this.optimizedUpdateThread.flagLobbyUpdate();
            }
        }
        
        /**
         * This function determines the opcode of the received message and 
         * calls the appropriate handler for it.
         * 
         * @param message       The received message.
         * @throws IOException 
         */
        void processMessage(Message message) throws IOException {            
            // Opcode determines what the server does with the message.
            switch(message.opcode) {
                
                // User registers for the service. This involves sending a unique
                // username to identify their session. Until this case happens,
                // none of the other cases can run. Absolute rules for usernames:
                // 1) Cannot be "SERVER" or any variation of the capitalization.
                // 2) Should not include special characters, only a-Z and 0-9
                // 3) Should be a reasonable length.
                //
                // I leave this up to the client code to handle.
                case "REGISTER":
                    register(message);
                    break;
    
                // Unregister's the client's name, but maintains their connection.
                case "UNREGISTER":
                    unregister(message);
                    break;                    
                    
                // This client requested an updated lobby state. This is the
                // single-cast version of updateLobby, only sending the updated
                // status to the user who requested it. This command is only
                // between the client and server, and no other clients.
                case "LIST":
                    updateLobby(this);
                    break;
                
                // This client sent a chat message.
                // TODO: implement this
                case "CHAT":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    chat(message);
                    break;
                
                // This client asked to create a game lobby. This command only
                // interacts with the server, and is not forwarded to any other
                // clients.
                case "MAKEGAME":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    makeGame(message);
                    break;    
                
                // This client asked to join a game. This command is only
                // between the client and server, but the server will inform
                // other people in the game that the new player has joined.
                // Also, if successful, the response to the client will be
                // the currently selected color of the game's host.
                case "JOINGAME":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    joinGame(message);
                    break;
                
                // This client selected a color. This information is passed
                // to the other player in the game, if one exists. The server
                // itself does nothing with the information. The client-side
                // application should validate that the color is available
                // before sending this message. Players should communicate
                // through chat if their color is already taken to resolve 
                // the situation.
                case "PICKCOLOR":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    pickColor(message);
                    break;
                
                // The client has declared that they are ready for the game 
                // to start. This should be originally validated on the client-
                // side, as a client cannot ready up unless the following
                // conditions are met:
                // 1) Two players have joined the game
                // 2) Both players have chosen a color.
                case "READY":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    ready(message);
                    break;
                
                // The client has asked to start the game. This requires that
                // both players are in the "ready" state. This request to start
                // is passed to the opponent if they are not readied up, so
                // they can see that this client tried to start the game. The
                // client-side app should prevent a STARTGAME request from being
                // sent unless that client has already readied up, to minimize
                // wasted traffic.
                case "STARTGAME":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    startGame(message);
                    optimizedUpdateThread.flagLobbyUpdate();
                    break;
                
                // The client has submitted a move. This is passed directly
                // to the client's opponent, assuming a game is active. Clients
                // are responsible for all validation of this message. The server
                // does absolutely nothing other than forward it.
                case "MOVE":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    move(message);
                    break;
                
                // The client is submitting their desire to forfeit the game.
                // This command covers three main cases:
                // 1) Checkmate. A "FORFEIT" message can be sent when the
                //    client is in checkmate, as an acknowledgement that the
                //    game is over.
                // 2) The client wishes to exit the game early. 
                // 3) The client actually wishes to forfeit the game.
                //
                // Cases 2 and 3 should be handled the same. The client is
                // allowed to leave/forfeit, and their opponent is notified
                // that they are the winner due to their opponent leaving.
                case "FORFEIT":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    forfeit(message);
                    break;
                
                // One player has suggested a draw. This message is forwarded
                // to the opponent, who can accept or decline it. Each client
                // should track this behavior, and perhaps limit it to once
                // per turn to avoid spamming draw requests. A draw can be
                // verified if one client sends the message, and then the other
                // responds with the same message. 
                case "DRAW":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    draw(message);
                    break;
                
                // One player has asked to leave the lobby. If the player was
                // the host, the entire lobby is disbanded, as the lobby was
                // named after that player. If it was the second player, the
                // lobby can remain.
                case "EXITLOBBY":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
 
                    exitLobby(message);
                    break;

                case "LEADERBOARD":
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    
                    getLeaderboard(this);
                    break;
                    
                // This is the full quit, as in the game itself is being closed.
                // If this is sent while in a game, a FORFEIT notification will
                // be sent to the opponent, and then the game will be destroyed
                // and the client who sent QUIT will be erased from existence.
                case "QUIT":
                    quit(message);                    
                    break; 
                
                // This should not ever happen, but might as well request
                // registration if the user sends an invalid opcode. IDK.
                default:
                    if (playerName == null) {
                        requestRegistration(false);
                        break;
                    }
                    break;
            }
        }

        /**
         * This function is called whenever any opcode other than REGISTER
         * is used without the user being registered. It sends the user a
         * message asking them to register. If the boolean parameter is true,
         * it will indicate that the received name is not valid.
         * 
         * @param invalidName   Boolean to indicate the previously sent name
         *                      is not valid.
         * @throws IOException 
         */
        void requestRegistration(boolean invalidName) throws IOException {
            Message reply = new Message(
                "SERVER",
                "REGISTER",
                null
            );

            // Add the INVALID_NAME string if the parameter was true.
            if (invalidName) {
                reply.body = "INVALID_NAME";
            }

            send(reply);
        }

        /**
         * Attempts to register a user with the server.
         * 
         * @param message
         * @throws IOException 
         */
        void register(Message message) throws IOException {
            // REGISTER messages will have the player's name in the
            // message's sender field, and nothing else of interest.
            playerName = message.sender;
            
            // Can't have a null name, obviously.
            if (playerName == null) {
                requestRegistration(true);
                return;
            }

            // Cannot use "SERVER" as a name.
            if (playerName.toUpperCase().equals("SERVER")) {
                requestRegistration(true);
                return;
            }

            // Name must not already be in use.
            if (users.containsKey(playerName)) {
                send(new Message(
                    "SERVER",
                    "REGISTER",
                    "NAME_IN_USE"
                ));
                return;
            }
            
            // Name is now assumed to be valid. This means validation for
            // length & non-alphanumeric characters should be done client-side.
            
            // Register the user.
            Server.this.users.put(playerName, this);
            
            // Send a successful reply.
            send(new Message(
                "SERVER",
                "REGISTER",
                "SUCCESS"
            ));
            
            // Update the lobby status for everyone, as someone new just joined.
            Server.this.optimizedUpdateThread.flagLobbyUpdate();
        }
        
        /**
         * Unregisters the usernaame, maintains the client socket.
         * 
         * @param message
         * @throws IOException 
         */
        void unregister(Message message) throws IOException {
            if (game != null) {
                send(new Message(
                    "SERVER",
                    "EXITLOBBY",
                    "OPPONENT_LEFT"
                ));
                return;
            }
            
            // Now just remove the player from the list of players connected.
            Server.this.users.remove(this.playerName);

            this.playerName = null;
            
            Server.this.optimizedUpdateThread.flagLobbyUpdate();
        }       
            
        /**
         * Attempts to make a game in the user's name.
         * 
         * @param message
         * @throws IOException 
         */
        void makeGame(Message message) throws IOException {           
            // The client must not already have an active GameSession if they
            // intend to create a new one. If they are a part of a GameSession,
            // inform them. In theory, this should not be necessary, as the UI
            // shouldn't really allow someone to create a game if they are
            // already in one.
            if (game != null) {
                send(new Message(
                    "SERVER",
                    "MAKEGAME",
                    "ALREADY_IN_GAME"
                ));
                return;
            }

            // Create a new GameSession for this client.
            GameSession gs = new GameSession(this);
            this.game = gs;
            
            // Add it to the list of games. 
            games.put(playerName, gs);
            
            
            // Send a successful reply. No other information is needed, as the
            // state of a fresly created game should be known to the client.
            send(new Message(
                "SERVER",
                "MAKEGAME",
                "SUCCESS"
            ));

            // Since a new game was created, the lobby status must be updated. 
            Server.this.optimizedUpdateThread.flagLobbyUpdate();
        }
        
        /**
         * The client wants to join an existing GameSession. This will require
         * verifying the game is joinable, and then actually joining it. After
         * joining, updates will need to be sent to both players in the game,
         * informing them of the new status of the lobby.
         * 
         * @param message
         * @throws IOException 
         */
        void joinGame(Message message) throws IOException {
            // Cannot join a game if you are already in one. Again, this should
            // not be possible through the UI.
            if (game != null) {
                send(new Message(
                    "SERVER",
                    "JOINGAME",
                    "ALREADY_IN_GAME"
                ));
                return;
            }

            // The message's body has the name of the game to join.
            String gameName = message.body;

            // Ensure a name was actually passed.
            if (gameName == null) {
                send(new Message(
                    "SERVER",
                    "JOINGAME",
                    "NO_GAME_SELECTED"
                ));
                return;
            }

            // Ensure the name is of an active game.
            if (!games.containsKey(gameName)) {
                send(new Message(
                    "SERVER",
                    "JOINGAME",
                    "NO_GAME_FOUND"
                ));
                return;
            }

            // Attempt to get the game the user wants to join.
            GameSession gs = games.get(gameName);
            
            // Check if that game is full. Should not be, but there is a
            // definite chance that two users try to join at once, so only
            // one can get in.
            if (gs.isFull()) {
                send(new Message(
                    "SERVER",
                    "JOINGAME",
                    "GAME_IS_FULL"
                ));
                return;
            }

            // Everything is in order. Join the game.
            gs.addPlayer(this);
            this.game = gs;
            
            // Send a message to the client notifying them that they joined the
            // game. Because the server and GameSession do not track the
            // color of the users, we'll have to leave it up to the clients
            // to exchange information about this. (The host of the game will
            // send the client a message telling them what color they are).
            // Also, notice the sender is the name of the game's owner, not
            // SERVER. Technically, this is unnecessary, but it should be
            // easier to handle on the client-side when the information
            // is fed to you, instead of the client having to track what game
            // they tried to join.
            send(new Message(
                gs.getOtherPlayer(this).getName(),
                "JOINGAME",
                "SUCCESS"
            ));
            
            // Notify the host that a new player has joined. Notice the
            // sender of the message is not the SERVER here, but the name
            // of the player who just joined.
            gs.getOtherPlayer(this).send(new Message(
                playerName,
                "JOINGAME",
                "OPPONENT_JOINED"
            ));
            
            // Also, since a player joined a game, the lobby status for
            // everyone else must be updated.
            Server.this.optimizedUpdateThread.flagLobbyUpdate();
        }
        
        /**
         * A message allowing a client to notify their opponent that
         * they chose a color. This is used to both prepare for the
         * game, and to update the lobby status when this setting
         * is changed.
         * 
         * @param message
         * @throws IOException 
         */
        void pickColor(Message message) throws IOException {
            // Cannot pick a color if the client isn't even in a game.
            if (game == null) {
                send(new Message(
                    "SERVER",
                    "PICKCOLOR",
                    "NOT_IN_A_GAME"
                ));
                return;
            }
            
            // Also must be in the game lobby, not playing.
            if (game.getState() != GameSession.GameState.Lobby) {
                send(new Message(
                    "SERVER",
                    "PICKCOLOR",
                    "NOT_IN_LOBBY"
                ));
                return;
            }
            
            // Get the other player. Remember, there is no guarantee
            // a second player actually exists at this point, as a player
            // can select their color while alone in the lobby.
            IConnectedPlayer other = game.getOtherPlayer(this);
            
            // Check if a player was found. If not, send a message to the
            // client notifying them that there's no opponent. This isn't
            if (other == null) {
                send(new Message(
                    "SERVER",
                    "PICKCOLOR",
                    "NO_OPPONENT"
                ));
                return;
            }

            // Now that another player is confirmed to exist, pass the
            // message on.
            other.send(message);
        }
        
        /**
         * Marks a player as ready or not ready. Also passes the
         * message to the opponent in the lobby, if they exist.
         * 
         * IMPORTANT: This requires the client to ensure that both
         * players have picked colors before sending a READY message,
         * because the server has no way of verifying this. That also
         * means the first 3 if statements in this function are very 
         * redundant, but they can stay.
         * 
         * @param message
         * @throws IOException 
         */
        void ready(Message message) throws IOException {
            // Must have a GameSession to ready up.
            if (game == null) {
                send(new Message(
                    "SERVER",
                    "READY",
                    "NOT_IN_A_GAME"
                ));
                return;
            }
            
            // Must be in the lobby to ready up.
            if (game.getState() != GameSession.GameState.Lobby) {
                send(new Message(
                    "SERVER",
                    "READY",
                    "NOT_IN_LOBBY"
                ));
                return;
            }
            
            // Game must be full to ready up.
            if (!game.isFull()) {
                send(new Message(
                    "SERVER",
                    "READY",
                    "GAME_NOT_FULL"
                ));
                return;
            }
            
            // Ready up if the message is "YES", otherwise mark as not ready.
            ready = message.body.equals("YES");
            
            // Notify the opponent.
            game.getOtherPlayer(this).send(message);
        }
        
        /**
         * One client has requested that the game starts.
         * 
         * @param message
         * @throws IOException 
         */
        void startGame(Message message) throws IOException {
            // Can't start a game if one doesn't exist.
            if (game == null) {
                send(new Message(
                    "SERVER",
                    "STARTGAME",
                    "NOT_IN_A_GAME"
                ));
                return;
            }
            
            // Can't start a game that's already started.
            if (game.getState() != GameSession.GameState.Lobby) {
                send(new Message(
                    "SERVER",
                    "STARTGAME",
                    "NOT_IN_LOBBY"
                ));
                return;
            }
            
            // Game needs both players to start.
            if (!game.isFull()) {
                send(new Message(
                    "SERVER",
                    "STARTGAME",
                    "GAME_NOT_FULL"
                ));
                return;
            }

            // If startGame() returns true, notify both players that their game
            // has started.
            if (game.startGame()) {
                send(new Message(
                    "SERVER",
                    "STARTGAME",
                    "SUCCESS"       
                ));
                game.getOtherPlayer(this).send(new Message(
                    "SERVER",
                    "STARTGAME",
                    "SUCCESS"       
                ));
                
            // startGame() returned false, so notify the client that players
            // weren't ready, and notify the other player that someone tried
            // to start.
            } else {
                send(new Message(
                    "SERVER",
                    "STARTGAME",
                    "PLAYERS_NOT_READY"       
                ));
                game.getOtherPlayer(this).send(new Message(
                    playerName,
                    "STARTGAME",
                    "TRIED_TO_START"    
                ));
                
            }   
        }
        
        /**
         * Forwards a MOVE message to the opponent.
         * 
         * @param message
         * @throws IOException 
         */
        void move(Message message) throws IOException {
            // Need a game to make a move.
            if (game == null) {
                send(new Message(
                    "SERVER",
                    "MOVE",
                    "NOT_IN_A_GAME"
                ));
                return;
            }
            
            // Need to be playing the game to make a move.
            if (game.getState() != GameSession.GameState.Play) {
                send(new Message(
                    "SERVER",
                    "MOVE",
                    "NOT_IN_PLAY_STATE"
                ));
                return;
            }
            
            // If we're in play state, guaranteed to have an opponent.
            // Forward the message.
            game.getOtherPlayer(this).send(message);     
        }
        
        /**
         * Notifies the opponent that a forfeit has been called by this client.
         * 
         * @param message
         * @throws IOException 
         */
        void forfeit(Message message) throws IOException {
            // Can't forfeit without a game.
            if (game == null) {
                send(new Message(
                    "SERVER",
                    "CHECKMATE",
                    "NOT_IN_A_GAME"
                ));
                return;
            }
            
            // Can't forfeit a game that hasn't started.
            if (game.getState() != GameSession.GameState.Play) {
                send(new Message(
                    "SERVER",
                    "CHECKMATE",
                    "NOT_IN_PLAY_STATE"
                ));
                return;
            }
            
            // If we're in play state, guaranteed to have an opponent.
            // Forward the message. Game clients will handle this.
            game.getOtherPlayer(this).send(message);
            
            // Record the other player's name as the winner.
            recordWinner(game.getOtherPlayer(this).getName());
            
            // Dismantle the game.
            destroyGame(); 
        }
        
        /**
         * Forward the message to the opponent if a game is active.
         * 
         * @param message
         * @throws IOException 
         */
        void draw(Message message) throws IOException {
            // Obviously need to be in a game.
            if (game == null) {
                send(new Message(
                    "SERVER",
                    "DRAW",
                    "NOT_IN_A_GAME"
                ));
                return;
            }
            
            // Game needs to be in Play state.
            if (game.getState() != GameSession.GameState.Play) {
                send(new Message(
                    "SERVER",
                    "DRAW",
                    "GAME_IS_NOT_ACTIVE"
                ));
                return;
            }
            
            // Forward the message to the opponent.
            message.sender = "SERVER";
            game.getOtherPlayer(this).send(message);
            
            if (message.body.equals("AGREED")) {
                // Destroy the game
                destroyGame();
            }           
        }
        
        /**
         * Exits a player from the lobby. If the player was the host, the lobby
         * closes.
         * 
         * @param message
         * @throws IOException 
         */
        void exitLobby(Message message) throws IOException {
            // Obviously need to be in a game.
            if (game == null) {
                send(new Message(
                    "SERVER",
                    "EXITLOBBY",
                    "NOT_IN_A_GAME"
                ));
                return;
            }
            
            // Can't exit a lobby if the game isn't in lobby state.
            if (game.getState() != GameSession.GameState.Lobby) {
                send(new Message(
                    "SERVER",
                    "EXITLOBBY",
                    "NOT_IN_A_GAME_LOBBY"
                ));
                return;
            }
            
            // Check if the person is the host or the second player.
            if (game.getPlayer(0) == this) {
                // destroyGame will notify the other user that the session
                // was terminated.
                destroyGame();
                
                send(new Message(
                    "SERVER",
                    "EXITLOBBY",
                    "SUCCESS"
                ));
                return;
            }

            // The person leaving the lobby is not the host.
            game.removePlayer(this);
            
            // Notify the host.
            game.getPlayer(0).send(new Message(
                "SERVER",
                "EXITLOBBY",
                "OPPONENT_LEFT"
            ));
            
            // Remove reference to the game from this player.
            game = null;
                              
            // Send success message.
            send(new Message(
                "SERVER",
                "EXITLOBBY",
                "SUCCESS"
            ));
            
            // Update the lobby.
            Server.this.optimizedUpdateThread.flagLobbyUpdate();
        }
            
        /**
         * This is sent by a client when they terminate their application. It
         * allows the server to gracefully remove them from any active
         * roster.
         * 
         * @param message
         * @throws IOException 
         */
        void quit(Message message) throws IOException {
            // If the player is in a game that needs to be handled.
            if (game != null) {
                // If the game is currently active or the player is the host,
                // nuke the entire thing.
                if (game.getState() == GameSession.GameState.Play || game.getPlayer(0) == this) {
                    destroyGame();
                
                // Game is still in lobby, and this player is not the host. Just
                // remove them from the lobby.
                } else {
                    game.removePlayer(this);
                    
                    // Notify the host.
                    game.getPlayer(0).send(new Message(
                        "SERVER",
                        "EXITLOBBY",
                        "OPPONENT_LEFT"
                    ));
                }
                game = null;        
            }
            
            // Now just remove the player from the list of players connected.
            Server.this.users.remove(this.playerName);
            
            // And disconnect the socket.
            client_sock.close();
            
            Server.this.optimizedUpdateThread.flagLobbyUpdate();
        }
        
        void chat(Message message) throws IOException {
            // Need a sender + body.
            if (message.sender == null || message.body == null)
                return;
                      
            // If lobby, send to everyone.
            if (game == null) {
                Server.this.chatToLobby(message.sender, message.sender + ": " + message.body);
                return;
            }
            
            // In game, so only send to opponent.
            IConnectedPlayer opponent = game.getOtherPlayer(this);
            if (opponent != null)
                opponent.send(new Message(
                    "SERVER",
                    "CHAT",
                    message.sender + ": " + message.body
                ));
        }
    }
}
