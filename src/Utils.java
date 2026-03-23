import java.awt.*;

public class Utils {
    
    public static double distance(Point p1, Point p2) {

      return Math.sqrt( Math.pow((p2.x - p1.x),2) +  Math.pow((p2.y - p1.y),2));
    }

    public static boolean IsCircleIntersection(Point c1, Point c2, int r1, int r2 ){
            return (distance(c1,c2) <= r1 + r2);
    }




//    public static [][] construireMatriceAdj (String[] ArgsMatrice ){
//        int [][] M = new int[][]
//        for (int i =0 ; i < ArgsMatrice.length ; i++){
//            for (int j = i; j< ArgsMatrice.length; j++ )
//
//                M[i][j] = IsCircleIntersection(   )
//        }

    }

