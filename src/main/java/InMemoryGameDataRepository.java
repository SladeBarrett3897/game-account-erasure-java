import java.util.ArrayList;
import java.util.List;

public final class InMemoryGameDataRepository implements GameDataRepository {
    private final List<PlayerAsset> assets = new ArrayList<>();
    private final List<LiveEvent> events = new ArrayList<>();
    private final List<ModerationQueueItem> moderationQueue = new ArrayList<>();

    public InMemoryGameDataRepository seed(String playerId) {
        assets.add(new PlayerAsset("asset-7", playerId));
        events.add(new LiveEvent("event-4", playerId));
        moderationQueue.add(new ModerationQueueItem("review-9", playerId));
        return this;
    }

    @Override
    public DeletionCounts deleteOwnedData(String playerId) {
        int assetsDeleted = remove(assets, playerId);
        int eventsDeleted = remove(events, playerId);
        int reviewsDeleted = remove(moderationQueue, playerId);
        return new DeletionCounts(assetsDeleted, eventsDeleted, reviewsDeleted);
    }

    private static int remove(List<? extends OwnedRecord> records, String playerId) {
        int before = records.size();
        records.removeIf(record -> record.playerId().equals(playerId));
        return before - records.size();
    }

    private interface OwnedRecord { String playerId(); }
    private record PlayerAsset(String id, String playerId) implements OwnedRecord {}
    private record LiveEvent(String id, String playerId) implements OwnedRecord {}
    private record ModerationQueueItem(String id, String playerId) implements OwnedRecord {}
}
