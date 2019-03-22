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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author yu-chi
 */
public class ChessBoard {
    ChessPieceType[][] board;
    
    public ChessBoard() {
        reset();
    }
    
    public ChessBoard(ChessBoard copy) {
        board = new ChessPieceType[8][8];
        for (int r = 0; r < 8; ++r) {
            for (int c = 0; c < 8; ++c) {
                board[r][c] = copy.board[r][c];
            }
        }
    }
    
    public final void reset() {
        board = new ChessPieceType[8][8];
        String template = 
                  "rhbkqbhr"
                + "pppppppp"
                + "........"
                + "........"
                + "........"
                + "........"
                + "PPPPPPPP"
                + "RHBKQBHR";
        int i = 0;
        for (int r = 7; r >= 0; --r) {
            for (int c = 0; c < 8; ++c) {
                switch (template.charAt(i++)) {
                    case '.': board[r][c] = ChessPieceType.EMPTY; break;
                    case 'p': board[r][c] = ChessPieceType.BLACK_UNMOVED_PAWN; break;
                    case 'P': board[r][c] = ChessPieceType.WHITE_UNMOVED_PAWN; break;
                    case 'r': board[r][c] = ChessPieceType.BLACK_ROOK; break;
                    case 'R': board[r][c] = ChessPieceType.WHITE_ROOK; break;
                    case 'h': board[r][c] = ChessPieceType.BLACK_KNIGHT; break;
                    case 'H': board[r][c] = ChessPieceType.WHITE_KNIGHT; break;
                    case 'b': board[r][c] = ChessPieceType.BLACK_BISHOP; break;
                    case 'B': board[r][c] = ChessPieceType.WHITE_BISHOP; break;
                    case 'k': board[r][c] = ChessPieceType.BLACK_KING; break;
                    case 'K': board[r][c] = ChessPieceType.WHITE_KING; break;
                    case 'q': board[r][c] = ChessPieceType.BLACK_QUEEN; break;
                    case 'Q': board[r][c] = ChessPieceType.WHITE_QUEEN; break;
                }
            }
        } 
    }
    
    public final ChessPieceType getPiece(int row, int column) {
        if (row < 1 || row > 8 || column < 1 || column > 8)
            return ChessPieceType.OUT_OF_BOUNDS; // out of bounds
        return board[row - 1][column - 1];
    }
    
    public final ChessPieceType getPiece(Coordinate at) {
        if (at == null)
            return ChessPieceType.OUT_OF_BOUNDS;
        return getPiece(at.row, at.column);
    }
    
    public final void setPiece(int row, int column, ChessPieceType piece) {
        board[row - 1][column - 1] = piece;
    }
    
    public final void setPiece(Coordinate at, ChessPieceType piece) {
        setPiece(at.row, at.column, piece);
    }
    
    public final void movePiece(Coordinate from, Coordinate to) {
        if (to == null)
            throw new NullPointerException("null 'to' coordinate");
        if (from == null)
            throw new NullPointerException("null 'from' coordinate");
        if (board == null)
            throw new IllegalStateException("board has not been initialized");
        ChessPieceType piece = getPiece(from);
        if (piece.type == ChessPieceClass.PAWN && // moving a pawn
                to.row == (piece.color == Alignment.WHITE ? 8 : 1)) { // destination is the far side of the board
            board[to.row - 1][to.column - 1] = piece.color == Alignment.WHITE ? ChessPieceType.WHITE_QUEEN : ChessPieceType.BLACK_QUEEN; // promote the pawn to a queen
        } else if (piece.type == ChessPieceClass.UNMOVED_PAWN) { // moving a pawn for the first time
            board[to.row - 1][to.column - 1] = piece.color == Alignment.WHITE ? ChessPieceType.WHITE_PAWN : ChessPieceType.BLACK_PAWN; // the pawn has made its first move
        } else {
            board[to.row - 1][to.column - 1] = piece; // move the piece
        }
        board[from.row - 1][from.column - 1] = ChessPieceType.EMPTY; // clear the tile that the piece has left
    }
    
    public final Collection<Coordinate> getPossibleMoves(Coordinate from) {
        List<Coordinate> moves = new LinkedList<>();
        ChessPieceType piece = getPiece(from);
        Alignment alliedColor = piece.color;
        switch (piece.type) {
            case UNMOVED_PAWN:{
                int direction = piece.color == Alignment.WHITE ? 1 : -1;
                Coordinate to;
                if (getPiece(to = from.offset(direction, 0)).isValidDestinationFor(alliedColor) &&  to != null) {
                    moves.add(to); // can move one space forward
                    if (getPiece(to = from.offset(direction * 2, 0)).isValidDestinationFor(alliedColor) &&  to != null)
                        moves.add(to); // can also move two spaces forward
                }
                break;
            }
            case PAWN: {
                int direction = piece.color == Alignment.WHITE ? 1 : -1;
                Coordinate to;
                if (getPiece(to = from.offset(direction, 0)).isValidDestinationFor(alliedColor) &&  to != null)
                    moves.add(to);
                if (getPiece(to = from.offset(direction, 1)).isEnemyOf(alliedColor) &&  to != null)
                    moves.add(to);
                if (getPiece(to = from.offset(direction, -1)).isEnemyOf(alliedColor) &&  to != null)
                    moves.add(to);
                break;
            }
            case ROOK: {
                int[][] directions = { // The directions this piece type can move ({ {row direction, column direction} })
                    { 1, 0 }, // move horizontally right (+row)
                    {-1, 0 }, // move horizontally left  (-row)
                    { 0,-1 }, // move vertically up (-column)
                    { 0, 1 }  // move vertically down (+column)
                };
                for (int[] direction : directions) { // for every direction this piece can move
                    for (int d = 1; d <= 8; d++) { // test moving this piece in that direction, for a maximum of 8 tiles
                        Coordinate to = from.offset(direction[0] * d, direction[1] * d); // testing moving d tiles away, toward direction
                        ChessPieceType at = getPiece(to); // test what piece we will collide with at this tile
                        if (!at.isValidDestinationFor(alliedColor))
                            break; // stop when another piece is blocking this tile
                        moves.add(to);
                        if (at.isEnemyOf(alliedColor))
                            break; // only penetrate one enemy piece, then stop
                    }
                }
                break;
            }
            case BISHOP: {
                int[][] directions = { // The directions this piece type can move ({ {row direction, column direction} })
                    { 1, 1 }, // move up-right
                    {-1, -1 }, // move down-left
                    { 1,-1 }, // move down-right
                    { -1, 1 }  // move up-left
                };
                 for (int[] direction : directions) { // for every direction this piece can move
                    for (int d = 1; d <= 8; d++) { // test moving this piece in that direction, for a maximum of 8 tiles
                        Coordinate to = from.offset(direction[0] * d, direction[1] * d); // testing moving d tiles away, toward direction
                        ChessPieceType at = getPiece(to); // test what piece we will collide with at this tile
                        if (!at.isValidDestinationFor(alliedColor))
                            break; // stop when another piece is blocking this tile
                        moves.add(to);
                        if (at.isEnemyOf(alliedColor))
                            break; // only penetrate one enemy piece, then stop
                    }
                }

                break;
            }
            case KNIGHT: {
                int[][] directions = { // The directions this piece type can move ({ {row direction, column direction} })
                    { 1, 2 },
                    {-1, 2 },
                    { 1,-2 },
                    { -1, -2 },
                    { 2, 1 },
                    { 2, -1 },
                    { -2, 1 },
                    { -2, -1}
                };
                 for (int[] direction : directions) { // for every direction this piece can move
                    for (int d = 1; d < 2; d++) { // test moving this piece in that direction, for a maximum of 8 tiles
                        Coordinate to = from.offset(direction[0] * d, direction[1] * d); // testing moving d tiles away, toward direction
                        ChessPieceType at = getPiece(to); // test what piece we will collide with at this tile
                        if (!at.isValidDestinationFor(alliedColor))
                            break; // stop when another piece is blocking this tile
                        moves.add(to);
                        if (at.isEnemyOf(alliedColor))
                            break; // only penetrate one enemy piece, then stop
                    }
                }

                break;
            }
            case KING: {

                int[][] directions = { // The directions this piece type can move ({ {row direction, column direction} })
                    { 1, 1 }, // move up-right
                    {-1, -1 }, // move down-left
                    { 1,-1 }, // move down-right
                    { -1, 1 },  // move up-left
                    { 1, 0 }, // move horizontally right (+row)
                    {-1, 0 }, // move horizontally left  (-row)
                    { 0,-1 }, // move vertically up (-column)
                    { 0, 1 }  // move vertically down (+column)
                };
                for (int[] direction : directions) { // for every direction this piece can move
                    for (int d = 1; d <= 1; d++) { // test moving this piece in that direction, for a maximum of 1 tiles
                        Coordinate to = from.offset(direction[0] * d, direction[1] * d); // testing moving d tiles away, toward direction
                        ChessPieceType at = getPiece(to); // test what piece we will collide with at this tile
                        if (!at.isValidDestinationFor(alliedColor))
                            break; // stop when another piece is blocking this tile
                        moves.add(to);
                        if (at.isEnemyOf(alliedColor))
                            break; // only penetrate one enemy piece, then stop
                    }
                }

                break;
            }
            case QUEEN: {
                int[][] directions = { // The directions this piece type can move ({ {row direction, column direction} })
                    { 1, 1 }, // move up-right
                    {-1, -1 }, // move down-left
                    { 1,-1 }, // move down-right
                    { -1, 1 },  // move up-left
                    { 1, 0 }, // move horizontally right (+row)
                    {-1, 0 }, // move horizontally left  (-row)
                    { 0,-1 }, // move vertically up (-column)
                    { 0, 1 }  // move vertically down (+column)
                };
                for (int[] direction : directions) { // for every direction this piece can move
                    for (int d = 1; d <= 8; d++) { // test moving this piece in that direction, for a maximum of 8 tiles
                        Coordinate to = from.offset(direction[0] * d, direction[1] * d); // testing moving d tiles away, toward direction
                        ChessPieceType at = getPiece(to); // test what piece we will collide with at this tile
                        if (!at.isValidDestinationFor(alliedColor))
                            break; // stop when another piece is blocking this tile
                        moves.add(to);
                        if (at.isEnemyOf(alliedColor))
                            break; // only penetrate one enemy piece, then stop
                    }
                }

                break;
            }
        }
        return moves;
    }
}
