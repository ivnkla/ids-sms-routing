import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Utils {

    public static class Config {
        public Boolean[][] matrix;
        public int[][] antennas; // [id] -> [x, y, r]
    }

    /*
     * The read file is of the form 
     * 
     * 3
     * 0 0 2
     * 3 0 2
     * 6 0 2
     * 
     * Where the first line is the number of antennas
     * Next lines are relative position of antennas [x, y, r]
     */
    public static Config readConfig(String filename) throws IOException {
        Config cfg = new Config();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
        	
            /*
             * We first save the relative positions for future
             */
            int n = Integer.parseInt(br.readLine().trim());
            cfg.antennas = new int[n][3];
            int i = 0;
            for (i = 0; i < n; i++) {
                String[] parts = br.readLine().trim().split("\\s+");
                if (parts.length != 3){
                    throw new IOException("Erreur format du fichier");
                }
                cfg.antennas[i][0] = Integer.parseInt(parts[0]); // x
                cfg.antennas[i][1] = Integer.parseInt(parts[1]); // y
                cfg.antennas[i][2] = Integer.parseInt(parts[2]); // r
            }


            /*
             * We then build the adjacent matrix
             */
            cfg.matrix = new Boolean[n][n];
            for (i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        cfg.matrix[i][j] = false;
                    } else {
                        Point p1 = new Point(cfg.antennas[i][0], cfg.antennas[i][1]);
                        Point p2 = new Point(cfg.antennas[j][0], cfg.antennas[j][1]);
                        cfg.matrix[i][j] = IsCircleIntersection(p1, p2, cfg.antennas[i][2], cfg.antennas[j][2]);
                    }
                }
            }
        } catch (NullPointerException e){
            throw new NullPointerException("Erreur nombre d'antennes déclariées différent du nombre d'antennes défini ");
        }

        return cfg;
    }
    
    /*
     * This is to find distance between two points.
     */
    public static double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
    }

    /*
     * This is to find if two antennas have a ray
     * large enough to communicate.
     */
    public static boolean IsCircleIntersection(Point c1, Point c2, int r1, int r2) {
        return (distance(c1, c2) <= r1 + r2);
    }
}
