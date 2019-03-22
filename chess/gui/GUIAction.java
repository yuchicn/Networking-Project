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
public enum GUIAction {
    CONTINUE, // title, register, main, session
    ENTER_TEXT,  // register, main, session, play
    SUBMIT_TEXT, // register, main, session, play
    SELECT, // main, play
    TOGGLE, // session, play
    EXIT, // register, main, session, play
    CREATE // main
}
