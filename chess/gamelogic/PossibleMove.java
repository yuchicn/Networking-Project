/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gamelogic;

import chess.game.Coordinate;

/**
 *
 * @author yu-chi
 */
public class PossibleMove extends Coordinate {
    
    public final boolean isBlocked;

    public PossibleMove(String text, boolean isBlocked) {
        super(text);
        this.isBlocked = isBlocked;
    }

    public PossibleMove(int row, char column, boolean isBlocked) {
        super(row, column);
        this.isBlocked = isBlocked;
    }

    public PossibleMove(int row, int column, boolean isBlocked) {
        super(row, column);
        this.isBlocked = isBlocked;
    }

    public PossibleMove(Coordinate copy, boolean isBlocked) {
        super(copy);
        this.isBlocked = isBlocked;
    }

    @Override
    public int hashCode() {
        return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
    }
}
