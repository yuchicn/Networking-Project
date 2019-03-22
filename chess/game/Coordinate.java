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
public class Coordinate {
    public final int row, column;
    
    /**
     * Initialize a coordinate <br>
     * WHITE starts on rows 1-2. <br>
     * BLACK starts on rows 7-8.
     * @param row The coordinate row (1-8). 
     * @param column The coordinate column (1-8 = A-H).
     */
    public Coordinate(int row, int column) {
        if (row < 1 || row > 8)
            throw new IllegalArgumentException(Integer.toString(row));
        if (column < 1 || column > 8)
            throw new IllegalArgumentException(Integer.toString(column));
        this.row = row;
        this.column = column;
    }
    
    /**
     * Initialize a coordinate <br>
     * WHITE starts on rows 1-2. <br>
     * BLACK starts on rows 7-8.
     * @param row The coordinate row (1-8). WHITE starts on rows 1-2, BLACK on rows 7-8.
     * @param column The coordinate column (A-H).
     */
    public Coordinate(int row, char column) {
        this(row, column - 'A' + 1);
    }
    
    /**
     * Initialize a coordinate <br>
     * WHITE starts on rows 1-2. <br>
     * BLACK starts on rows 7-8.
     * @param text A string in the format "A1" (A = column, 1 = row)
     */
    public Coordinate(String text) {
        this(
            Integer.parseInt(Character.toString(text.charAt(1))), 
            text.charAt(0));
    }
    
    
    public Coordinate(Coordinate copy) {
        this(copy.row, copy.column);
    }
    
    /**
     * Safely construct a new Coordinate <br>
     * WHITE starts on rows 1-2. <br>
     * BLACK starts on rows 7-8.
     * @param row The coordinate row (1-8). 
     * @param column The coordinate column (1-8 = A-H).
     * @return The constructed Coordinate, or null if the row or column is out of bounds.
     */
    public static Coordinate create(int row, int column) {
        if (row < 1 || row > 8 || column < 1 || column > 8)
            return null; // out of bounds
        return new Coordinate(row, column);
    }
        
    /**
     * @return The color of the game board tile at this coordinate
     */
    public Alignment getTileColor() {
        return row % 2 != column % 2 ? Alignment.WHITE : Alignment.BLACK;
    }

    /**
     * @return A string in the format "A1" (A = column, 1 = row)
     */
    @Override
    public String toString() {
        return String.format("%s%d", (char)(column + 'A' - 1), row);
    }

    @Override
    public boolean equals(Object obj) {
        if (!Coordinate.class.isInstance(obj))
            return false;
        Coordinate o = (Coordinate)obj;
        return o.row == this.row && o.column == this.column;
    }

    @Override
    public int hashCode() {
        return (column - 1) * 8 + (row - 1);
    }
    
    public final Coordinate offset(int rows, int columns) {
        return Coordinate.create(this.row + rows, this.column + columns);
    }
}
