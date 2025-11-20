package app.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Map;

public class EnvProbeTest {

    @Test
    public void printEnvAndFallbackFile() throws Exception {

        // 1) Print relevant env vars (length only, to avoid leaking secrets)
        Map<String, String> env = System.getenv();
        String url = env.get("DATABASE_URL");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASSWORD");

        System.out.println("ENV DATABASE_URL  length=" + (url == null ? "null" : url.length()));
        System.out.println("ENV DATABASE_USER length=" + (user == null ? "null" : user.length()));
        System.out.println("ENV DATABASE_PASSWORD length=" + (pass == null ? "null" : pass.length()));

        boolean allPopulated = (url.length() > 0) && (user.length() > 0) && (pass.length() > 0);

        Assertions.assertTrue(allPopulated, "Expected all three env vars to be set and non-empty");
    }
}
