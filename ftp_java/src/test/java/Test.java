import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Test {

    public static void main(String[] args) throws IOException {
        StringBuilder str = new StringBuilder();
        Files.list(Paths.get("")).forEach(obj -> str.append(obj.getFileName().toString()).append('\n'));
        System.out.println(str);
    }
}
