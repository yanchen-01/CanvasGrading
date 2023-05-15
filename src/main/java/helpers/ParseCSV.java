package helpers;

import java.util.List;

/**
 * Interface for parsing a CSV file as the way needed
 */
public interface ParseCSV {
    /**
     * Parse the content got from a CSV file
     *
     * @param content the content of the file
     */
    void parse(List<String[]> content) throws Exception;
}
