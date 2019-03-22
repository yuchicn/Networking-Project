/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.gui;

/**
 *
 * @author yu-chi
 */
public interface IStateController {
    IGUIActionListener guiListener();
    GUIState guiState();
}
