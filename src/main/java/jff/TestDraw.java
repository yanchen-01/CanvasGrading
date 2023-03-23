package jff;

import helpers.Utils;
import obj.FileInfo;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class TestDraw {
    public static void main(String[] args) throws IOException {
        Scanner s = new Scanner(System.in);
        Utils.printPrompt("filename (with .jff)");
        String filename = s.nextLine();
        Utils_Draw.drawJff(new File(filename), new FileInfo(filename, "dfa"));
        Desktop.getDesktop().open(new File(filename + ".png"));
    }
}
