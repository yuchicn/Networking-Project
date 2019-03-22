/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.game;

/**
 * The ref() method can be overridden to return the current value of a local variable, even if the instance or memory location changes.
 * @author yu-chi
 */
public interface Reference <T> {
    /** Get the referenced instance value */
    T ref();
}
