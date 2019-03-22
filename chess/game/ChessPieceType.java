/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.game;

/**
 *  Information about the class and color of a chess piece
 * @author yu-chi
 */
public enum ChessPieceType {
    
    BLACK_PAWN(ChessPieceClass.PAWN, Alignment.BLACK), 
    BLACK_UNMOVED_PAWN(ChessPieceClass.UNMOVED_PAWN, Alignment.BLACK), 
    BLACK_ROOK(ChessPieceClass.ROOK, Alignment.BLACK), 
    BLACK_BISHOP(ChessPieceClass.BISHOP, Alignment.BLACK), 
    BLACK_KNIGHT(ChessPieceClass.KNIGHT, Alignment.BLACK), 
    BLACK_KING(ChessPieceClass.KING, Alignment.BLACK), 
    BLACK_QUEEN(ChessPieceClass.QUEEN, Alignment.BLACK),
    WHITE_PAWN(ChessPieceClass.PAWN, Alignment.WHITE), 
    WHITE_UNMOVED_PAWN(ChessPieceClass.UNMOVED_PAWN, Alignment.WHITE), 
    WHITE_ROOK(ChessPieceClass.ROOK, Alignment.WHITE), 
    WHITE_BISHOP(ChessPieceClass.BISHOP, Alignment.WHITE), 
    WHITE_KNIGHT(ChessPieceClass.KNIGHT, Alignment.WHITE), 
    WHITE_KING(ChessPieceClass.KING, Alignment.WHITE), 
    WHITE_QUEEN(ChessPieceClass.QUEEN, Alignment.WHITE),
    EMPTY(ChessPieceClass.EMPTY, null),
    OUT_OF_BOUNDS(ChessPieceClass.OUT_OF_BOUNDS, null);
    
    public final ChessPieceClass type;
    public final Alignment color;
    
    ChessPieceType(ChessPieceClass type, Alignment color) {
        this.type = type;
        this.color = color;
    }
    
    /**
     * @param ofPlayer The player to evaluate this type for
     * @return True if this type is the opposite color of ofPlayer and not empty
     */
    public boolean isEnemyOf(Alignment ofPlayer) {
        return type != ChessPieceClass.EMPTY && color != ofPlayer;
    }
    
    /**
     * @param withPlayer The player to evaluate this type for
     * @return True if this type is the same color as withPlayer and not empty
     */
    public boolean isAlliedWith(Alignment withPlayer) {
        return type != ChessPieceClass.EMPTY && color == withPlayer;
    }
    
    /**
     * @param forPlayer The player to evaluate this type for
     * @return True if this type is empty or the opposite color of forPlayer
     */
    public boolean isValidDestinationFor(Alignment forPlayer) {
        return type == ChessPieceClass.EMPTY || color != forPlayer;
    }
    
    public Alignment getcolor() {
        return color;
    }
    
    public final String filename() {
        return color.name().toLowerCase() + "/" + type.filename;
    }
    
    public boolean isKing() {
        return this.type.isKing();
    }
    
    
}
