package jff;

import obj.Question;

import static constants.JsonKeywords.UPLOAD;

public class JffQuestion extends Question {
    private double each, output, total;
    private String jffType;

    public JffQuestion(int id) {
        super(id, UPLOAD);
        each = output = total = 0.0;
        jffType = "";
    }

    @Override
    public void setContent(String content) {
        super.setContent(content);
        setJffType(content);
        if (content.contains("test cases")
                && content.contains("each is worth "))
            setPoints(content);
    }

    private void setPoints(String content) {
        String keyword = "each is worth ";
        int index = content.indexOf(keyword) + keyword.length();
        String result = content.substring(index, index + 4).trim();
        setEach(Double.parseDouble(result));
        if (content.contains("ransducer")
                && content.contains(" for correct output")) {
            keyword = " for correct output";
            index = content.indexOf(keyword);
            result = content.substring(index - 4, index).trim();
            setOutput(Double.parseDouble(result));
        }
    }

    public String getJffType() {
        return jffType;
    }

    private void setJffType(String content) {
        String keyword = "esign a ";
        content = content.replace("<strong>", "");
        int index = content.indexOf(keyword) + keyword.length();
        content = content.substring(index, index + 20).trim();
        if (content.contains("TM"))
            jffType = "turing";
        else if (content.contains("DFA"))
            jffType = "dfa";
        else if (content.contains("NFA"))
            jffType = "fa";
        else if (content.contains("PDA"))
            jffType = "pda";
        else jffType = content.toLowerCase();
    }

    public double getEach() {
        return each;
    }

    public void setEach(double each) {
        this.each = each;
    }

    public double getOutput() {
        return output;
    }

    public void setOutput(double output) {
        this.output = output;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
