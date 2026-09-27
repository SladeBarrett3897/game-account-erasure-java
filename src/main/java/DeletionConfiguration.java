public final class DeletionConfiguration {
    private DeletionConfiguration() {}

    public static AccountDeletionService service(GameDataRepository repository) {
        String key = requiredEnvironment("INFRAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("INFRAI_BASE_URL", InfraiClient.DEFAULT_BASE_URL);
        return new AccountDeletionService(repository, new InfraiClient(baseUrl, key));
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }
}
