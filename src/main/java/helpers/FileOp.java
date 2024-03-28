package helpers;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Interface to assign different file operation for each file.
 */
public interface FileOp {
    /**
     * An operation on each file.
     *
     * @param input the file being checked.
     */
    void operate(File input) throws FileNotFoundException;
}