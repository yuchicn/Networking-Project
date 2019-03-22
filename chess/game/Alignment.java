/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.game;

import chess.gui.GameProperties;
import java.awt.Color;

/**
 * The color alignment (WHITE or BLACK) of a player or game piece.
 * @author yu-chi
 */
public enum Alignment {
    BLACK, WHITE;
    
    public Alignment other() {
        switch (this) {
            case BLACK: return WHITE;
            case WHITE: return BLACK;
            default: throw new IllegalStateException(); // this should never happen
        }
    }
    
    public boolean isBlack() {
        return this == Alignment.BLACK;
    }
    
    public boolean isWhite() {
        return this == Alignment.WHITE;
    }
    
    public Color asColor() {
        return this.isBlack() ? GameProperties.themeBlack() : GameProperties.themeWhite();
    }
    
    public Color highlightColor() {
        return this.isBlack() ? GameProperties.themeBlack().brighter().brighter() : GameProperties.themeWhite().darker().darker();
    }
    
    public Color highlightColor(Color mix) {
        Color c = asColor();
        Color h = new Color((c.getRed() + mix.getRed()) / 2, (c.getGreen() + mix.getGreen()) / 2, (c.getBlue() + mix.getBlue()) / 2);
        return h;
    }
}
