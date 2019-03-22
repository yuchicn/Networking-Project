/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui.component;

import chess.game.Alignment;
import chess.game.ChessPieceType;
import chess.game.Coordinate;

/**
 *
 * @author yu-chi
 */
public class MoveHistoryPanel extends AbstractListPanel<MoveRecordPanel> {
    
    public MoveHistoryPanel() {
        super(MoveRecordPanel.class);
    }
    
    public void logMove(ChessPieceType moving, Coordinate from, Coordinate to) {
        put(MoveRecordPanel.movement(moving.color, moving.type, from, to), true);
    }
    
    public void logCapture(ChessPieceType captured, Coordinate at) {
        put(MoveRecordPanel.capture(captured.color.other(), captured.type, at), true);
    }
    
    public void logPromotion(Alignment color, Coordinate at) {
        put(MoveRecordPanel.promote(color, at), true);
    }
    
    public void logCheck(Alignment color) {
        put(MoveRecordPanel.check(color), true);
    }
    
}
