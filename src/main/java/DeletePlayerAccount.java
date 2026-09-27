public final class DeletePlayerAccount {
    private DeletePlayerAccount() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: DeletePlayerAccount <player-id> <credential-id>");
            System.exit(2);
        }
        InMemoryGameDataRepository repository = new InMemoryGameDataRepository().seed(args[0]);
        AccountDeletionService.DeletionReceipt receipt = DeletionConfiguration.service(repository)
                .delete(new AccountDeletionService.DeleteAccountCommand(args[0], args[1]));
        System.out.printf("deleted player=%s assets=%d events=%d moderation=%d sessions=%d credentialRevoked=%s%n",
                receipt.playerId(), receipt.deleted().assets(), receipt.deleted().liveEvents(),
                receipt.deleted().moderationItems(), receipt.sessionsRevoked(), receipt.credentialRevoked());
    }
}
