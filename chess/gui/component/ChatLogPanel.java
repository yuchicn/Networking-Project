/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui.component;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author yu-chi
 */
public class ChatLogPanel extends AbstractListPanel<ChatMessagePanel> {
    /**
     * Creates new form ChatHistoryPanel
     */
    public ChatLogPanel() {
        super(ChatMessagePanel.class);
    }
    
    public void logMessage(String name, String message) {
        put(new ChatMessagePanel(name, message), true);
    }
    
    public void logMessage(String name, String message, long timeout) {
        ChatMessagePanel p = new ChatMessagePanel(name, message);
        put(p, true);
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                pop((c) -> c.equals(p));
            }
        }, timeout);
    }
}
