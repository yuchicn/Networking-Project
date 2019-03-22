/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yu-chi
 */
public class GameProperties {
    
    private static final String PATH = "/assets/config/chess.properties";
    
    private static Properties properties = null;
    
    private static final String 
            PROPERTY_ADDRESS = "server.address",
            PROPERTY_PORT = "server.port",
            PROPERTY_WHITE = "theme.white",
            PROPERTY_BLACK = "theme.black",
            PROPERTY_TEXT = "theme.text";
    
    /** The name of the sender for info chat messages  */
    public static final String INFO_SENDER = "*INFO*";
    
    private GameProperties() {
    }
    
    public URL url() {
        return getClass().getResource(PATH);
    }
    
    public final URI uri() {
        try {
            return url().toURI();
        } catch (URISyntaxException ex) {
            Logger.getLogger(GameProperties.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private static InputStream stream() {
        return GameProperties.class.getResourceAsStream(PATH);
    }
    
    private static Properties properties() {
        try {
            if (properties == null) {
                properties = new Properties();
                properties.load(stream());
            }
        } catch (IOException ex) {
            Logger.getLogger(GameProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        return properties;
    }
    
    public static String serverIP() {
        return properties().getProperty(PROPERTY_ADDRESS);
    }
    
    public static int serverPort() {
        return Integer.parseInt(properties().getProperty(PROPERTY_PORT));
    }
    
    public static java.awt.Color themeWhite() {
        return java.awt.Color.decode(properties().getProperty(PROPERTY_WHITE));
    }
    
    public static java.awt.Color themeBlack() {
        return java.awt.Color.decode(properties().getProperty(PROPERTY_BLACK));
    }
    
    public static java.awt.Color themeText() {
        return java.awt.Color.decode(properties().getProperty(PROPERTY_TEXT));
    }
    
    /* // Doesn't work when running as jar
    public void save() {
        OutputStream os = null;
        try {
            System.out.println(uri());
            File file = new File(uri());
            os = new FileOutputStream(file);
            store(os, getTimestamp());
        } catch (IOException ex) {
            Logger.getLogger(GameProperties.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(GameProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    */
    
}
