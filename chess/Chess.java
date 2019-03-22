/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import chess.gui.GameProperties;
import chess.gui.GameWindow;
import chess.gui.ServerWindow;
import chess.gui.state.RegisterState;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Usage: Chess server|username
 * Will remove the username parameter after UI is up and running.
 * 
 * @author yu-chi
 */
public class Chess {
    private final static String HOST = "uw1-320-04.uwb.edu";
    private final static int PORT = 3546;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // read args:
            boolean noServer = !getArg(args, "server");                 //  /server             start a game server
            int clients = getArgInt(args, "clients", noServer ? 1 : 0); //  -clients [count]    number of game clients to open (default is 1 if no server, else 0)
            boolean autoLoad = getArg(args, "auto");                    //  /auto               automatically register each game client
            boolean noGraphics = getArg(args, "nographics");            //  /nographics         Run the server without a window
            
            int port = GameProperties.serverPort(); 
            
            if (!noServer) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!noGraphics)
                            new ServerWindow().setVisible(true); // show a window so the server can be terminated
                        Server server = new Server();
                        server.start(port, 10);
                    }
                }).start();
            
                try {
                    Thread.sleep(1000); // wait for the server to start
                } catch (InterruptedException ex) {
                    Logger.getLogger(Chess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            List<GameWindow> windows = new LinkedList<>();
            
            String title = "Chess";
            for (int i = 1; i <= clients; ++i) {
                String name = "Test" + i;
                GameWindow window = 
                        autoLoad ? 
                            new GameWindow (RegisterState.class, name) : // launch to register state
                            new GameWindow(); // default launch
                title += " ";
                window.setTitle(title);
                window.setVisible(true);
                if (autoLoad) {
                    window.gui_reconnect();
                    window.gui_registerClient(name);
                }
                windows.add(window);
            }
            
            System.out.println("Type STOP to stop:");
            Scanner scanner = new Scanner(System.in);
            String line = "";
            while (!line.equals("STOP"))
                line = scanner.nextLine();
            System.out.println("Closing all servers and clients");
            
            System.exit(0);
            
        } catch (IOException ex) {
            Logger.getLogger(Chess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static boolean getArg(String[] args, String key) {
        String match = "/" + key;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals(match))
                return true;
        }
        return false;
    }
    
    public static String getArgValue(String[] args, String key, String defaultValue) {
        String match = "-" + key;
        for (int i = 0; i < args.length - 1; ++i) {
            if (args[i].equals(match))
                return args[i+1];
        }
        return defaultValue;
    }
    
    public static int getArgInt(String[] args, String key, int defaultValue) {
        String value = getArgValue(args, key, null);
        if (value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
    
}
