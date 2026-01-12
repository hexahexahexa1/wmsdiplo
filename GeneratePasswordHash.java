import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String password = "admin";
        String hash = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("\nSQL UPDATE command:");
        System.out.println("UPDATE users SET password_hash = '" + hash + "' WHERE username = 'admin';");
    }
}
