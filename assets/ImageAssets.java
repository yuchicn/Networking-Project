/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assets;

import chess.game.ChessPieceType;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author yu-chi
 */
public class ImageAssets {
    
    private static ImageAssets imageAssets;
    
    private Map<String, BufferedImage> loadedAssets;
    
    private ImageAssets() {
        loadedAssets = new HashMap<>();
    }
    
    private static BufferedImage getLoaded(String path) {
        if (imageAssets == null) {
            imageAssets = new ImageAssets();
            return null;
        }
        return imageAssets.loadedAssets.get(path);
    }
    
    private static void saveLoaded(String path, BufferedImage image) {
        if (imageAssets == null)
            imageAssets = new ImageAssets();
        imageAssets.loadedAssets.put(path, image);
    }
    
    public static BufferedImage loadImage(String path) {
        try {
            BufferedImage img = getLoaded(path);
            if (img != null)
                return img;
            img = ImageIO.read(ImageAssets.class.getResourceAsStream("/assets/" + path));
            saveLoaded(path, img);
            return img;
        } catch (IOException ex) {
            Logger.getLogger(ImageAssets.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static BufferedImage loadChessPieceIcon(ChessPieceType piece) {
        return loadImage(String.format("%s.png", piece.filename()));
    }
    
}
