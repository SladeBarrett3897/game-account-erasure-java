import java.io.IOException;
import java.util.List;

public interface IdentityControlPlane {
    List<String> listSessionIds(String userId) throws IOException, InterruptedException;
    void revokeSession(String sessionId) throws IOException, InterruptedException;
    void revokeAccountKey(String keyId) throws IOException, InterruptedException;
}
