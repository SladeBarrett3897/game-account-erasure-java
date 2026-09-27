import java.util.ArrayList;
import java.util.List;

public final class AccountDeletionServiceTest {
    public static void main(String[] args) throws Exception {
        List<String> audit = new ArrayList<>();
        GameDataRepository repository = playerId -> {
            audit.add("delete-domain:" + playerId);
            return new GameDataRepository.DeletionCounts(2, 1, 3);
        };
        IdentityControlPlane identity = new IdentityControlPlane() {
            public List<String> listSessionIds(String userId) {
                audit.add("list-sessions:" + userId);
                return List.of("session-a", "session-b");
            }
            public void revokeSession(String sessionId) { audit.add("revoke-session:" + sessionId); }
            public void revokeAccountKey(String keyId) { audit.add("revoke-key:" + keyId); }
        };

        AccountDeletionService.DeletionReceipt receipt = new AccountDeletionService(repository, identity)
                .delete(new AccountDeletionService.DeleteAccountCommand("player-42", "key-record-8"));

        List<String> expected = List.of("delete-domain:player-42", "list-sessions:player-42",
                "revoke-session:session-a", "revoke-session:session-b", "revoke-key:key-record-8");
        if (!audit.equals(expected)) throw new AssertionError("unexpected deletion order: " + audit);
        if (receipt.sessionsRevoked() != 2 || !receipt.credentialRevoked()) throw new AssertionError("bad receipt");
        System.out.println("PASS account data precedes two session revocations and credential revocation");
    }
}
