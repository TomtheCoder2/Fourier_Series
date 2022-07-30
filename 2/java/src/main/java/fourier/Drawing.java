package fourier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Drawing {
    /**
     * Read the contents of the file with the given name, and return
     * it as a string
     *
     * @param fileName The name of the file to read
     * @return The contents of the file
     */
    public static String readFile(String fileName) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = Drawing.class.getResourceAsStream(fileName);
            InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            for (String line; (line = reader.readLine()) != null; ) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * convert your svgs to paths <a href="https://shinao.github.io/PathToPoints/">here</a>*/
    public static double[][] getDrawing(String fileName) {
        String s = readFile(fileName);
        s = s.replaceAll("#\n*", "");
        s = s.replace("{", "");//replacing all [ to ""
//        s = s.substring(0, s.length() - 2);//ignoring last two ]]
        s = s.replace("}}", "");
        String[] s1 = s.split("},");//separating all by "],"
        if (s1.length == 1) {
            s1 = s.split("\n");
        }
//        System.out.println(s1.length);

        String[][] my_matrics = new String[s1.length][2];//declaring two dimensional matrix for input

        for (int i = 0; i < s1.length; i++) {
            s1[i] = s1[i].trim();//ignoring all extra space if the string s1[i] has
            String[] single_int = s1[i].split(", *");//separating integers by ", "

            //adding single values
            System.arraycopy(single_int, 0, my_matrics[i], 0, single_int.length);
        }
//        System.out.println(Arrays.deepToString(my_matrics));

        double[][] output = new double[my_matrics.length][my_matrics[0].length];
        for (int i = 0; i < my_matrics.length; i++) {
            for (int j = 0; j < my_matrics[i].length; j++) {
//                System.out.println(my_matrics[i][j]);
                output[i][j] = Double.parseDouble(my_matrics[i][j]);
            }
        }

//        System.out.println(Arrays.deepToString(my_matrics));
        return output;
    }
}
