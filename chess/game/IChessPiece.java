/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.game;

/**
 * Information about the type and location of a specific chess piece on the game board.
 * @author yu-chi
 */
public interface IChessPiece {
    /** @return The class and color of this chess piece */
    chess.game.ChessPieceType getType();
    /** @return The location of this chess piece on the game board */
    chess.game.Coordinate getCoordinate();
    
    /** @return A chess piece representing an empty tile */
    public static IChessPiece emptyPiece(Coordinate at) {
        return new IChessPiece() {
            @Override
            public ChessPieceType getType() {
                return ChessPieceType.EMPTY;
            }

            @Override
            public Coordinate getCoordinate() {
               return at;
            }
        };
    }
    
    public static IChessPiece placeholder(ChessPieceType type, Coordinate at) {
        return new IChessPiece() {
            @Override
            public ChessPieceType getType() {
                return type;
            }

            @Override
            public Coordinate getCoordinate() {
                return at;
            }
        };
    }
}
