import java.util.function.Function;
public class LambdaBetaReduction {
    static Function<Function<Integer,Integer>,
            Function<Function<Integer,Integer>,
                    Function<Integer,Integer>>> compose =
            f -> g -> x -> f.apply(g.apply(x));

    public static void main(String[] args) {
        int result = compose.apply(n -> n + 1)
                .apply(n -> n * 2)
                .apply(5);
        System.out.println(result);
    }
}
