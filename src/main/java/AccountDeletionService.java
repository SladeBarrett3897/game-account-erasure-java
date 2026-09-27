import java.io.IOException;
import java.util.List;

public final class AccountDeletionService {
    private final GameDataRepository gameData;
    private final IdentityControlPlane identity;

    public AccountDeletionService(GameDataRepository gameData, IdentityControlPlane identity) {
        this.gameData = gameData;
        this.identity = identity;
    }

    public DeletionReceipt delete(DeleteAccountCommand command) throws IOException, InterruptedException {
        GameDataRepository.DeletionCounts counts = gameData.deleteOwnedData(command.playerId());
        List<String> sessions = identity.listSessionIds(command.playerId());
        for (String sessionId : sessions) identity.revokeSession(sessionId);
        identity.revokeAccountKey(command.credentialId());
        return new DeletionReceipt(command.playerId(), counts, sessions.size(), true);
    }

    public record DeleteAccountCommand(String playerId, String credentialId) {
        public DeleteAccountCommand {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
            if (credentialId == null || credentialId.isBlank()) throw new IllegalArgumentException("credentialId is required");
        }
    }

    public record DeletionReceipt(String playerId, GameDataRepository.DeletionCounts deleted,
                                  int sessionsRevoked, boolean credentialRevoked) {}
}
