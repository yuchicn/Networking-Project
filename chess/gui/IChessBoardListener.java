/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui;

import chess.game.ChessPieceType;
import chess.game.Coordinate;
import chess.gamelogic.PossibleMove;

/**
 *
 * @author yu-chi
 */
public interface IChessBoardListener {
    void move(Coordinate from, Coordinate to);
    ChessPieceType getPiece(Coordinate at);
    PossibleMove[] getMoves(Coordinate from);
}
