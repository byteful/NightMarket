package me.byteful.plugin.nightmarket.shop.item;

public enum ConfirmPurchaseMode {
    DEFAULT,
    ENABLED,
    DISABLED;

    public static ConfirmPurchaseMode fromConfig(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT;
        }

        final String normalized = value.trim().replace("-", "_").toUpperCase();
        if ("TRUE".equals(normalized) || "YES".equals(normalized) || "ENABLED".equals(normalized)) {
            return ENABLED;
        }
        if ("FALSE".equals(normalized) || "NO".equals(normalized) || "DISABLED".equals(normalized)) {
            return DISABLED;
        }
        if ("DEFAULT".equals(normalized)) {
            return DEFAULT;
        }

        throw new IllegalArgumentException("Invalid confirm_purchase value: " + value);
    }
}
