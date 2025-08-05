/* 
 * Author: Chloe T
 * Date: 05/08/25
 * Purpose of program: Set colour of printed text
 */
package com.AS91897;

public class SetColour {
    static String set(String str, int r, int g, int b) {
        return "\u001B[38;2;"+r+";"+g+";"+b+"m"+str+"\u001B[0m";

    }
    static String setBG(String str, int r, int g, int b){
        return "\u001B[48;2;"+r+";"+g+";"+b+"m"+str+"\u001B[0m";
    }
}
