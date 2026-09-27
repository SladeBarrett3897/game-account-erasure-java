public interface GameDataRepository {
    DeletionCounts deleteOwnedData(String playerId);
    record DeletionCounts(int assets, int liveEvents, int moderationItems) {}
}
