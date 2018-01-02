package company.server;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class fileOps {
    Path rot = Paths.get("root/");

    public fileOps() {
    }
    public static boolean removeFile(){
        return false;
    }
    public static boolean removeDir() {
        return false;
    }
    public static boolean listLongDir() {
        return false;
    }
}