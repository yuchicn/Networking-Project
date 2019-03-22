/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gamelogic;

import chess.game.Alignment;
import chess.game.ChessPieceClass;
import chess.game.ChessPieceType;
import chess.game.Coordinate;
import chess.game.MoveResult;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yu-chi
 */
public class GameLogic implements IGameLogic {
    ChessBoard board;

    public GameLogic() {
        board = new ChessBoard();
    }
    
    @Override
    public void initialize() {
        Logger.getLogger(IGameLogic.class.getName()).log(Level.WARNING, "Not supported yet.");
        board.reset();
    }

    private ChessPieceType getPiece(int row, int column) {
        return board.getPiece(row, column);
    }

    /**
     * @param from After moving a piece from this tile
     * @param to After moving the piece to this tile
     * @return The color of the king that will be in check after this move is made, or null if no king will be in check
     */
    private Alignment testCheckAfterMove(Coordinate from, Coordinate to) {
        
        ChessPieceType moving = getPiece(from);
        ChessPieceType replacing = getPiece(to);
        
        ChessBoard tempBoard = new ChessBoard(this.board);
        tempBoard.movePiece(from, to); // apply the movement to the board, temporarily
        
        Alignment checkState = null;

        Function<Coordinate,Alignment> testAt = (c) -> {
            Alignment result = null;
            Collection<Coordinate> moves = tempBoard.getPossibleMoves(c);
            for (Coordinate t : moves) {
                ChessPieceType tpiece = tempBoard.getPiece(t);
                if (tpiece.isKing()) {
                    if (tpiece.color == moving.color)
                        return moving.color;
                    if (result == null)
                        result = tpiece.color;
                }
            }
            return result;
        };

        //TODO: Evaluate tempBoard for check
        // Check the possible moves of every enemy game piece on the board and see if any of them intersect the allied king
        for (int c = 1; c <= 8; ++c) {
            for (int r = 1; r <= 8; ++r) {
                Coordinate here = new Coordinate(r, c);
                ChessPieceType at = tempBoard.getPiece(here);
                if (at.color != moving.color) {
                    Alignment check;
                    check = testAt.apply(here);
                    if (check == moving.color)
                        return check;
                    checkState = check;
                }
            }
        }
            
        return checkState; // no king is in check
    }

    @Override
    public MoveResult canMovePiece(Alignment player, Coordinate from, Coordinate to) {
        Logger.getLogger(IGameLogic.class.getName()).log(Level.WARNING, "Not supported yet.");
        ChessPieceType moving = getPiece(from);
        ChessPieceType at = getPiece(to);

        // Test piece ownership:
        if (moving.type != ChessPieceClass.EMPTY && moving.color != player)
            return MoveResult.EMEMY_UNIT; // cannot move enemy unit

        // Test for an allied unit at the destination:
        if (at.type != ChessPieceClass.EMPTY && at.color == player)
            return MoveResult.INVALID_MOVE; // blocked by an allied unit

        // Test the movement pattern specific to this type of chess piece:
        switch (moving.type) {

            case EMPTY: // moving an empty tile:
            case OUT_OF_BOUNDS: { // moving an out of bounds tile
                return MoveResult.NO_TARGET; // not moving a piece
            }

            case PAWN: { // moving a pawn (contains special logic for promotion)
                boolean promote = (to.row == (moving.color == Alignment.WHITE ? 8 : 1)); // the destination is the far side of the board

                for (Coordinate c : getPossibleMoves(from)) {// for every possible move for this piece
                    if (Objects.equals(to, c)) {// if the destination is the target destination
                        Alignment check = testCheckAfterMove(from, to); // test for check
                        return MoveResult.withFlags(
                                check != moving.color,  // not putting self in check?
                                this.getPiece(c).isEnemyOf(player), //capturing enemy piece?
                                check == moving.color.other(),  // put opponent in check?
                                promote); // promoting a pawn?
                    }
                }
                return MoveResult.INVALID_MOVE;// no possible destination matched the target destination
            }

            case UNMOVED_PAWN:
            case KNIGHT:
            case BISHOP:
            case ROOK: 
            case QUEEN:
            case KING: { // moving any other piece (all special logic implemented in the getPossibleMoves() function
                for (PossibleMove c : getPossibleMoves(from)) {// for every possible move for this piece
                    if (Objects.equals(to, c)) {// if any possible destination is the target destination
                        Alignment check = testCheckAfterMove(from, to); // test for check
                        return MoveResult.withFlags(
                                check != moving.color,  // not putting self in check?
                                getPiece(c).isEnemyOf(player), //capturing enemy piece?
                                check == moving.color.other(),  // put opponent in check?
                                false); // rook cannot be promoted
                    }
                }
                return MoveResult.INVALID_MOVE; // no possible destination matched the target destination
            }

            default: {
                throw new IllegalStateException("The move was not evaluated for moving a " + moving.type); // The selected piece type was not evaluated
            }

        }
    }

    @Override
    public void movePiece(Coordinate from, Coordinate to) {
        board.movePiece(from, to);
    }

    @Override
    public PossibleMove[] getPossibleMoves(Coordinate from) {
        ChessPieceType moving = getPiece(from);
        Collection<Coordinate> allMoves = board.getPossibleMoves(from);
        List<PossibleMove> safeMoves = new LinkedList<>();
        allMoves
            .stream()
            .filter((m)-> m != null)
            .forEach((m) -> safeMoves.add(
                    new PossibleMove(m, testCheckAfterMove(from, m) == moving.color)));
        return safeMoves.toArray(new PossibleMove[0]);
    }

    @Override
    public ChessPieceType getPiece(Coordinate at) {
        return board.getPiece(at);
    }
}
