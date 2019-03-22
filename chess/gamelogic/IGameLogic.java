/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gamelogic;

import chess.game.Alignment;
import chess.game.ChessPieceType;
import chess.game.Coordinate;
import chess.game.MoveResult;

/**
 *
 * @author yu-chi
 */
public interface IGameLogic {
    /**
     * Set up the game tempBoard for a new game, as follows:<pre>
     * <b    >  A B C D E F G H </b>
     * <b>8</b> r h b q k b h r (black)
     * <b>7</b> p p p p p p p p (black)
     * <b>6</b> . . . . . . . .    <i>r = rook     p = pawn</i>
     * <b>5</b> . . . . . . . .    <i>h = knight   b = bishop</i>
     * <b>4</b> . . . . . . . .    <i>q = queen    k = king</i>
     * <b>3</b> . . . . . . . .
     * <b>2</b> p p p p p p p p (white)
     * <b>1</b> r h b q k b h r (white)</pre>
     */
    public void initialize();
    
    /** 
     * Check if a move is valid, without actually moving a chess piece
     * @param player The color of the player moving the game piece
     * @param from The game tempBoard tile to move a piece from
     * @param to The game tempBoard tile to move the piece to
     * @return A MoveResult representing whether or not the move is valid, and why it is/not valid.
     */
    public MoveResult canMovePiece(Alignment player, Coordinate from, Coordinate to);
    
    /**
     * Move a chess piece, without checking if the move is valid.
     * @param from The game tempBoard tile to move a piece from
     * @param to The game tempBoard tile to move the piece to
     */
    public void movePiece(Coordinate from, Coordinate to);
    
    /**
     * @param at The specified game tempBoard tile
     * @return the game piece at the specified coordinate on the game tempBoard.
     */
    public ChessPieceType getPiece(Coordinate at);
    
    /**
     * Get the set of possible moves for a game piece, according to the current state of the game tempBoard.
     * @param from The game tempBoard tile of the piece that will be moved.
     * @return An array of destinations that the game piece is allowed to move to.
     */
    public PossibleMove[] getPossibleMoves(Coordinate from);
        
    /**
     * @return An instance of the game logic to be used in the actual game
     */
    public static IGameLogic defaultGameLogic() {
        return new GameLogic(); 
    }
    
}
