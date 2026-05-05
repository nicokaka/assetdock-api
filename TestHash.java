import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("Nicol@s123", "$2a$10$o/dE.OwpnW6P.z6q5NIFQ.cWE.TdI2CAc7hYc.QgjBwqIiGvNCpvK");
        System.out.println("Matches: " + matches);
    }
}
