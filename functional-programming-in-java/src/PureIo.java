import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Supplier;

public class PureIo {
    static Supplier<String> getLine = () -> new Scanner(System.in).nextLine();
    static Function<String, Supplier<Void>> putStrLn = s -> () -> { System.out.println(s); return null; };
    
    public static void main(String[] args) {
        putStrLn.apply("이름은??").get();
        String name = getLine.get();
        putStrLn.apply("내 이름은: " + name).get();
    }
}