/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import java.util.Arrays;

/**
 *
 * @author yu-chi
 */
public class GameSession {
    
    public enum GameState { 
        Lobby("In a game lobby"), Play("Playing chess"), End("Finishing a game"); 
        
        final String status;
        
        GameState(String status) {
            this.status = status;
        }
        
        public static GameState parseStatus(String status) {
            for (GameState s : values())
                if (s.status.equals(status))
                    return s;
            return null;
        }
        
        public static int compare(GameState state1, GameState state2) {
            int order1 = state1 == null ? 0 : Arrays.binarySearch(values(), state1) + 1;
            int order2 = state2 == null ? 0 : Arrays.binarySearch(values(), state2) + 1;
            return Integer.compare(order1, order2);
        }

        @Override
        public String toString() {
            return status;
        }
    }
    
    private GameState state = GameState.Lobby;
    private IConnectedPlayer[] players = new IConnectedPlayer[2];
    
    public GameSession(IConnectedPlayer host) {
        players[0] = host;
    }
    
    public int getPlayerCount() {
        return ((players[0] != null && players[0].isConnected()) ? 1 : 0) +
               ((players[1] != null && players[1].isConnected()) ? 1 : 0);
    }
    
    public IConnectedPlayer getPlayer(int playerNumber) {
        return players[playerNumber];
    }
    
    public boolean addPlayer(IConnectedPlayer player) {
        if (state == GameState.Lobby && player != null && !isFull()) {
            if (players[1] == null)
                players[1] = player;
            else if (players[0] == null)
                players[0] = player;
            else
                return false;
            
            return true;
        }
        return false;
    }
    
    public boolean removePlayer(IConnectedPlayer player) {
        if (state == GameState.Lobby) {
            if (players[1] == player)
                players[1] = null;
            else if (players[0] == player)
                players[0] = null;
            else
                return false;
            
            return true;
        }
        return false;
    }
    
    public boolean isFull() {
        return getPlayerCount() == players.length;
    }
    
    public GameState getState() {
        return state;
    }
    
    public IConnectedPlayer getOtherPlayer(IConnectedPlayer me) {
        if (getPlayer(0).getName().equals(me.getName())) /* me.name matches player0.name */
            return getPlayer(1);
        if (getPlayer(1).getName().equals(me.getName())) /* me.name matches player1.name */
            return getPlayer(0);
        return null; /* neither player matches */
    }
    
    public boolean startGame() {
        if (!isFull() || state != GameState.Lobby || !getPlayer(0).isReady() || !getPlayer(1).isReady())
            return false;
        state = GameState.Play;
        return true;
    }
}
