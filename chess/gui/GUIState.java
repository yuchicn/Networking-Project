/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui;

import chess.game.Alignment;
import chess.game.Coordinate;
import chess.gui.state.OfflineState;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author yu-chi
 */
public abstract class GUIState extends JPanel implements IStateController, IGUIEventListener {
    
    private Map<String, Object> properties;
    protected final IGUIActionListener controller;
    
    private GUIState() {
        throw new IllegalStateException("unused default constructor");
    }
    
    public abstract void initializeState();
    
    public GUIState(IStateController stateInfo) {
        if (stateInfo == null)
            throw new NullPointerException("null stateInfo");
        
        this.controller = stateInfo.guiListener();
        
        if (stateInfo.guiState() == null)
            this.properties = new HashMap<>();
        else
            this.properties = stateInfo.guiState().properties;
    }
    
    @Override
    public final void setProperty(String propertyName, Object value) {
        synchronized (this.controller) {
            Object oldValue = properties.put(propertyName, value);
            if (!Objects.equals(oldValue, value))
                onPropertyChanged(propertyName);
        }
    }
    
    @Override
    public final <T> T getProperty(String propertyName) {
        synchronized (this.controller) {
            return (T)properties.get(propertyName);
        }
    }
    
    public final <T> T getProperty(String propertyName, T defaultValue) {
        synchronized (this.controller) {
            Object r = properties.get(propertyName);
            return r != null ? (T)r : defaultValue;
        }
    }
    
    protected final void verifyParameters(Object[] actual, Class<?>... expected) throws IllegalArgumentException {
        if (actual.length != expected.length)
            throw new IllegalArgumentException("Incorrect number of parameters: " + actual.length);
        for (int i = 0; i < actual.length; ++i) {
            if (!expected[i].isInstance(actual[i]))
                throw new IllegalArgumentException("Incorrect argument type provided: expected" + expected[i]);
        }
    }
    
    public abstract void interact(GUIAction action, Object... parameters);
    
    protected final void interactWith(JButton button) {
        button.doClick();
    }

    @Override
    public final IGUIActionListener guiListener() {
        return controller;
    }

    @Override
    public final GUIState guiState() {
        return this;
    }
    
    protected final void logDroppedEvent() {
        System.err.println(
                String.format(
                        "Event dropped by %s: %s", 
                        getClass().getSimpleName(),
                        new RuntimeException().getStackTrace()[1]));
    }
    
    protected abstract void onPropertyChanged(String propertyName);
    
    public abstract void unloadState();
    
    /** @return True if this GUI can be resized, else false. */
    public abstract boolean isFlexible();

    @Override
    public void pushSession(String sessionName, int playerCount) {
        logDroppedEvent();
    }

    @Override
    public void popSession(Predicate<String> sessionNameMatcher) {
        logDroppedEvent();
    }

    @Override
    public void chatMessage(String sender, String message) {
        logDroppedEvent();
    }

    @Override
    public void displayError(String message) {
        logDroppedEvent();
    }

    @Override
    public void clearError(String replaceMessage) {
        logDroppedEvent();
    }

    @Override
    public void registrationApproved() {
        logDroppedEvent();
    }

    @Override
    public void registrationConflict() {
        logDroppedEvent();
    }

    @Override
    public void registrationInvalid() {
        logDroppedEvent();
    }

    @Override
    public void pushSessions(String[] sessions) {
        logDroppedEvent();
    }

    @Override
    public void enterSession(String error) {
        logDroppedEvent();
    }

    @Override
    public void opponentJoined(String name) {
        logDroppedEvent();
    }

    @Override
    public void switchColor(Alignment color) {
        logDroppedEvent();
    }

    @Override
    public void logDroppedEvent(String opcode, String opcase) {
        System.err.println(String.format("Dropped %s : %s", opcode, opcase));
    }

    @Override
    public void startGame() {
        logDroppedEvent();
    }

    @Override
    public void syncNotInGame() {
        logDroppedEvent();
    }

    @Override
    public void movePiece(String player, Coordinate from, Coordinate to) {
        logDroppedEvent();
    }

    @Override
    public void opponentLeft() {
        logDroppedEvent();
    }

    @Override
    public void connectionFailed(String reason) {
        controller.gui_reconnect();
    }
}
