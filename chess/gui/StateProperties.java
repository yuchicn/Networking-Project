/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui;

/**
 *
 * @author yu-chi
 */
public final class StateProperties {
    
    private StateProperties() { } // do not instantiate
    
    /** ( type = String )The property key for the display name of the user */
    public static final String DISPLAY_NAME = "DISPLAY_NAME";
    
    /** ( type = String ) The property key for the display name of the opponent */
    public static final String OPPONENT_NAME = "OPPONENT_NAME";
    
    /** ( type = Alignment ) The property key for the color of the user in the game session */
    public static final String ALIGNMENT_COLOR = "ALIGNMENT_COLOR";
    
    /** ( type = boolean ) The property key for the ready state of the user in the session lobby */
    public static final String MY_READY_STATE = "MY_READY_STATE";
    
    /** ( type = boolean ) The property key for the ready state of the opponent in the session lobby */
    public static final String OTHER_READY_STATE = "OTHER_READY_STATE";
    
    /** The message for the ready state of a player in the session lobby */
    public static final String READY_STATE_TEXT(boolean ready) { return ready ? "READY" : "NOT READY"; }
    
    /** The op data for the ready state of a player in the session lobby */
    public static final String READY_STATE_CODE(boolean ready) { return ready ? "YES" : "NO"; }
    
    /** The ready status represented by a message */
    public static final boolean READY_TEXT_STATE(String status) { return status.equals("READY") || status.equals("YES"); }
    
    /** ( type = String[] )The property key for the list of online players */
    public static final String PLAYERS_LIST = "PLAYERS_LIST";
    
    /** ( type = String ) The property key for the name of the current game session */
    public static final String SESSION_NAME = "SESSION_NAME";
    
    /** ( type = String[] ) The property key for the list of recent winners */
    public static final String SCOREBOARD = "SCOREBOARD";
    
    /** ( type = boolean) The property key for whether or not the opponent has requested a draw  */
    public static final String I_AM_DRAWING = "I_AM_DRAWING";
    
    /** ( type = boolean) The property key for whether or not the opponent has requested a draw  */
    public static final String OPPONENT_IS_DRAWING = "OPPONENT_IS_DRAWING";
    
    /** ( type = boolean) The property key for whether or not the opponent has requested a draw  */
    public static final String MY_TURN = "MY_TURN";
    
}
