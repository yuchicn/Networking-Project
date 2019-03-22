/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui;

import chess.game.Alignment;
import chess.game.Coordinate;
import chess.game.Reference;
import java.util.function.Predicate;

/**
 *
 * @author yu-chi
 */
public interface IGUIEventListener {
    void displayError(String message);
    void clearError(String replaceMessage);
    void setProperty(String propertyName, Object value);
    
    void registrationApproved();
    void registrationConflict();
    void registrationInvalid();
    
    void chatMessage(String sender, String message);
    
    void pushSessions(String[] sessions);
    void pushSession(String sessionName, int playerCount);
    void popSession(Predicate<String> sessionNameMatcher);
    
    void enterSession(String error);
    void opponentJoined(String name);
    void opponentLeft();
    
    void switchColor(Alignment color);
    void startGame();
    
    void syncNotInGame();
    void logDroppedEvent(String opcode, String opcase);
    
    void movePiece(String player, Coordinate from, Coordinate to);
    
    <T> T getProperty(String key);
    
    void connectionFailed(String reason);
    
    /**
     *
     */
    static final IGUIEventListener nullListener = new IGUIEventListener() {
        @Override
        public void displayError(String message) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void clearError(String replaceMessage) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void setProperty(String propertyName, Object value) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void chatMessage(String sender, String message) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void pushSession(String sessionName, int playerCount) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void popSession(Predicate<String> sessionNameMatcher) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void registrationApproved() {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void registrationConflict() {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void registrationInvalid() {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void pushSessions(String[] sessions) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void enterSession(String error) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void opponentJoined(String name) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void switchColor(Alignment color) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void logDroppedEvent(String opcode, String opcase) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void syncNotInGame() {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void startGame() {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void movePiece(String player, Coordinate from, Coordinate to) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public <T> T getProperty(String key) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
            return null;
        }

        @Override
        public void opponentLeft() {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }

        @Override
        public void connectionFailed(String reason) {
            new UnsupportedOperationException("Not supported yet.").printStackTrace();
        }
    };
    
    public static Reference<IGUIEventListener> getNullListener() {
        return () -> nullListener;
    }
}
