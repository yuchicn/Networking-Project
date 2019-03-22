/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui;

import chess.game.Alignment;
import chess.game.Coordinate;

/**
 *
 * @author yu-chi
 */
public interface IGUIActionListener extends IJoinGameListener {
    
    void gui_reconnect();
    
    void gui_changeState(GUIState newState);
    void gui_sendMessage(String message);
    
    void gui_registerClient(String name);
    
    void gui_unregisterClient();
    void gui_hostSession();
    void gui_listPlayers();
    
    void gui_switchColors(Alignment color);
    void gui_toggleReady(boolean readyState);
    void gui_leaveSession();
    void gui_startGame();
    
    void gui_draw(chess.gui.AgreementState action);
    void gui_forfeit();
    void gui_move(Coordinate from, Coordinate to);
    void gui_ackMove(String player, Coordinate from, Coordinate to);
}
