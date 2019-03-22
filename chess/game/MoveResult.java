/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess.game;

/**
 *  
 * @author yu-chi
 */
public enum MoveResult {
                                 /* valid|capture| check | promote */
    /** The move is allowed */      
    VALID_MOVE                      (true,  false,  false,  false),
    /** The move is valid, an enemy unit will be captured as a result of the move, and the enemy king will be in check. */
    CHECK_OPPONENT_AND_CAPTURE      (true,  true,   true,   false),
    /** The move is valid, and an enemy unit will be captured as a result of the move */
    CAPTURE                         (true,  true,   false,  false), 
    /** The move is allowed, and will place the opponent's king in check */
    CHECK_OPPONENT                  (true,  false,  true,   false), 
    /** The move is allowed and will promote a pawn to a queen */ 
    PROMOTION                       (true,  false,  false,  true), 
    /** The move is allowed and will promote a pawn to a queen and will capture an enemy */ 
    PROMOTION_AND_CAPTURE           (true,  true,   false,  true), 
    /** The move is allowed and will promote a pawn to a queen and will place the enemy king in check */ 
    PROMOTION_AND_CHECK             (true,  false,  true,   true), 
    /** The move is allowed and will promote a pawn to a queen and will capture an enemy and place the enemy king in check */ 
    PROMOTION_AND_CAPTURE_AND_CHECK (true,  true,   true,   true),
                                 /* valid|capture| check | promote */
    /** The move is not allowed because the from and to tile coordinates are the same. (The piece would not actually move)*/      
    NO_MOVEMENT                     (false, false,  false,  false),
    /** The move is not allowed because the game piece being moved is not the same color as the player moving it. */      
    EMEMY_UNIT                      (false, false,  false,  false), 
    /** The move is not allowed */  
    INVALID_MOVE                    (false, false,  false,  false), 
    /** The move is not allowed because it would place the player's own king in check */ 
    CHECK_SELF                      (false, false,  true,  false), 
    /** The move is not allowed because no piece is selected */ 
    NO_TARGET                       (false, false,  false,  false);
    
    /** True, if the movement is legal */
    public final boolean isValid;
    /** True, if the movement results in capturing an enemy unit */
    public final boolean capturesPiece;
    /** True, if the movement places the enemy king in check */
    public final boolean declareCheck;
    /** True, if the movement promotes a pawn to a queen */
    public final boolean promotePawn;
    
    MoveResult(boolean valid, boolean capture, boolean check, boolean promote) {
        this.isValid = valid;
        this.capturesPiece = capture;
        this.declareCheck = check;
        this.promotePawn = promote;
    }
    
    public final int hash() {
        return MoveResult.flagsHash(isValid, capturesPiece, declareCheck, promotePawn);
    }
    
    public static int flagsHash(boolean valid, boolean capture, boolean check, boolean promote) {
        return (valid ? 1 : 0) +
                (capture ? 2 : 0) + 
                (check ? 4 : 0) + 
                (promote ? 8 : 0);
    }
    
    public static MoveResult withFlags(boolean valid, boolean capture, boolean check, boolean promote) {
        int h = MoveResult.flagsHash(valid, capture, check, promote);
        if (h == MoveResult.VALID_MOVE.hash()) 
            return MoveResult.VALID_MOVE;
        else if (h == MoveResult.CHECK_OPPONENT_AND_CAPTURE.hash()) 
            return MoveResult.CHECK_OPPONENT_AND_CAPTURE;
        else if (h == MoveResult.CAPTURE.hash()) 
            return MoveResult.CAPTURE;
        else if (h == MoveResult.CHECK_OPPONENT.hash()) 
            return MoveResult.CHECK_OPPONENT;
        else if (h == MoveResult.PROMOTION.hash()) 
            return MoveResult.PROMOTION;
        else if (h == MoveResult.PROMOTION_AND_CAPTURE.hash()) 
            return MoveResult.PROMOTION_AND_CAPTURE;
        else if (h == MoveResult.PROMOTION_AND_CHECK.hash()) 
            return MoveResult.PROMOTION_AND_CHECK;
        else if (h == MoveResult.PROMOTION_AND_CAPTURE_AND_CHECK.hash()) 
            return MoveResult.PROMOTION_AND_CAPTURE_AND_CHECK;
        else if (h == MoveResult.CHECK_SELF.hash())
            return MoveResult.CHECK_SELF;
        return MoveResult.INVALID_MOVE;
    }
}
