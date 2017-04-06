import static spark.Spark.get;
import static spark.Spark.port;

public class Main {
    public static void main(String[] args) {
        port(Integer.valueOf(System.getenv("PORT")));
        get("/", (req, res) -> "Hello World");
    }
}