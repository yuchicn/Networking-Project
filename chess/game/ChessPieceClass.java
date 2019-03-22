/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.game;

/**
 * The class of a chess piece, which defines the piece's movement pattern an role in the game.
 * @author yu-chi
 */
public enum ChessPieceClass {
    PAWN, ROOK, BISHOP, KNIGHT, KING, QUEEN, UNMOVED_PAWN("pawn"), EMPTY(null), OUT_OF_BOUNDS(null);
    
    /** the name of the image file for this piece type */
    public final String filename;
    
    ChessPieceClass() {
        filename = name().toLowerCase();
    }
    
    ChessPieceClass(String name) {
        filename = name;
    }

    @Override
    public String toString() {
        return filename.toUpperCase();
    }
    
    public boolean isKing() {
        return this == KING;
    }
}
