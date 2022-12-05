package jff;

import helpers.Utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class TestDraw {
    public static void main(String[] args) throws IOException {
        Scanner s = new Scanner(System.in);
        Utils.printPrompt("filename (with .jff)");
        String filename = s.nextLine();
        Utils_Draw.drawJff(new File(filename), filename);
        Desktop.getDesktop().open(new File(filename + ".png"));
    }
}
