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
public enum AgreementState {
    REQUEST, RETRACT, ACCEPT, DECLINE;
    
    public static AgreementState when(boolean iAmDrawing, boolean theyAreDrawing) {
        if (theyAreDrawing) {
            return iAmDrawing ? ACCEPT : DECLINE;
        } else {
            return iAmDrawing ? REQUEST : RETRACT;
        }
    }
}
