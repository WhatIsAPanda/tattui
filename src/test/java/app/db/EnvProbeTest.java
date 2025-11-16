package app.db;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class EnvProbeTest {

    @Test
    public void printEnvAndFallbackFile() throws Exception {
        // 1) Print relevant env vars (length only, to avoid leaking secrets)
        Map<String, String> env = System.getenv();
        String url  = env.get("DATABASE_URL");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASSWORD");

        System.out.println("ENV DATABASE_URL  length=" + (url  == null ? "null" : url.length()));
        System.out.println("ENV DATABASE_USER length=" + (user == null ? "null" : user.length()));
        System.out.println("ENV DATABASE_PASSWORD length=" + (pass == null ? "null" : pass.length()));

        // 2) Try to open the teammate fallback file exactly as DatabaseConnector does
        String path = "C:\\SchoolProjects\\keys.txt";
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            props.load(in);
        }

        String fUrl  = props.getProperty("url");
        String fUser = props.getProperty("user");
        String fPass = props.getProperty("password");

        System.out.println("FILE url  length=" + (fUrl  == null ? "null" : fUrl.length()));
        System.out.println("FILE user length=" + (fUser == null ? "null" : fUser.length()));
        System.out.println("FILE password length=" + (fPass == null ? "null" : fPass.length()));
    }
}
