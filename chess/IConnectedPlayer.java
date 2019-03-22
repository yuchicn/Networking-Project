/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import java.io.IOException;

/**
 *
 * @author yu-chi
 */
public interface IConnectedPlayer {
    /**
     * Ensures that the thread has an active connection.
     * 
     * @return boolean
     */
    boolean isConnected();
    
    /**
     * Checks if the player has readied up.
     * 
     * @return boolean
     */
    boolean isReady();
    
    /**
     * Nulls out any game the player is a part of. This is a cleanup function
     * to handle quits/disconnects.
     */
    void nullGame();
    
    /**
     * Returns the name of the player.
     * 
     * @return String 
     */
    String getName();
    
    /**
     * Sends a Message to the player.
     * 
     * @param message       The message to send.
     * @throws IOException 
     */
    void send(Message message) throws IOException;
}
