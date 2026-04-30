package me.byteful.plugin.nightmarket.shop.player;

public record PurchaseResult(boolean success, String messageKey, String[] replacements) {
    public static PurchaseResult success(String messageKey, String... replacements) {
        return new PurchaseResult(true, messageKey, replacements);
    }

    public static PurchaseResult failure(String messageKey, String... replacements) {
        return new PurchaseResult(false, messageKey, replacements);
    }
}
