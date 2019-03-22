/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import java.io.Serializable;

/**
 *
 * @author yu-chi
 */
public class Message implements Serializable {
    
    // Identifies the sender of a message. Often can be left null.
    public String sender = null;
    
    // The opcode of the message. Can't be NULL if you want the message to be used
    // for anything.
    public String opcode = null;
    
    // The message's body. This can be a wide variety of stuff. Basically it's
    // whatever you want the message to say.
    public String body = null;
    
    // An array of timestamps used to maintain causal order for chat messaging.
    public int[] timestamps = null;
   
    /**
     * Constructor for non-chat variants of the message.
     * 
     * @param senderName Name of the sender, or null.
     * @param p_opcode   Opcode of the message.
     * @param msg        Body of the message.
     */
    public Message (String senderName, String p_opcode, String msg) {
        sender = senderName;
        opcode = p_opcode;
        body = msg;
        timestamps = null;
    } 
    
    /**
     * Constructor for chat messages.
     * 
     * @param senderName Name of the sender, or null.
     * @param p_opcode   Opcode of the message.
     * @param msg        Body of the message.
     * @param tstamps    Timestamps.
     */
    public Message (String senderName, String p_opcode, String msg, int[] tstamps) {
        sender = senderName;
        opcode = p_opcode;
        body = msg;
        if (tstamps == null)
            timestamps = null;
        else
            timestamps = tstamps.clone();
    } 
}
