/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui.component;

import chess.gui.IJoinGameListener;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yu-chi
 */
public class SessionsListPanel extends AbstractListPanel<GameSessionPanel> {
    
    IJoinGameListener joinListener = null;
    
    public SessionsListPanel() {
        super(GameSessionPanel.class);
    }
    
    public void putSession(String name, int playerCount) {
        GameSessionPanel panel = get((GameSessionPanel p) -> p.getSessionName().equals(name));
        if (panel == null)
            put(new GameSessionPanel(name, playerCount) {
                @Override
                protected void joinSession(String sessionName) {
                    if (joinListener != null)
                        joinListener.joinGame(sessionName);
                    else
                        Logger.getLogger(SessionsListPanel.class.getName()).log(Level.SEVERE, "Listener for join event not set");
                }
            }, false);
        else
            panel.setPlayerCount(playerCount);
    }
    
    private boolean hasSession(String[] names, GameSessionPanel p) {
        for (String s : names)
            if (s.equals(p.getSessionName()))
                return true;
        return false;
    }
    
    public void cleanSessions(String[] names) {
        while (pop((p) -> !hasSession(names, p)) != null)
            System.out.print("");
    }
    
    public boolean popSession(Predicate<String> sessionNameMatcher) {
        System.out.println("popping sessions");
        return pop((GameSessionPanel p) -> sessionNameMatcher.test(p.getSessionName())) != null;
    }
    
    public void setJoinGameListener(IJoinGameListener listener) {
        this.joinListener = listener;
    }
    
    public GameSessionPanel getSession(String name) {
        return pop((GameSessionPanel p) -> p.getSessionName().equals(name));
    }
}
