package jff;

import obj.Question;

import static constants.JsonKeywords.UPLOAD;

public class JffQuestion extends Question {
    private double each, output, total;
    public JffQuestion(int id, String content) {
        super(id, UPLOAD);
        setContent(content);
        each = output = total = 0.0;
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
