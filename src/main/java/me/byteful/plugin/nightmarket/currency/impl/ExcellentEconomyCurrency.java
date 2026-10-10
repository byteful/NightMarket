package me.byteful.plugin.nightmarket.currency.impl;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.byteful.plugin.nightmarket.currency.Currency;
import me.byteful.plugin.nightmarket.currency.CurrencyRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class ExcellentEconomyCurrency implements Currency {
    private static final String PLUGIN_NAME = "ExcellentEconomy";
    private static final String ID_PREFIX = "excellenteconomy:";
    private static final String API_CLASS = "su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI";
    private static final String CURRENCY_CLASS = "su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency";
    private static final String OPERATION_CONTEXT_CLASS = "su.nightexpress.excellenteconomy.api.currency.operation.OperationContext";
    private static final String NOTIFICATION_TARGET_CLASS = "su.nightexpress.excellenteconomy.api.currency.operation.NotificationTarget";

    private final ExcellentEconomyApi api;
    private final Object adapter;

    private ExcellentEconomyCurrency(ExcellentEconomyApi api, Object adapter) {
        this.api = api;
        this.adapter = adapter;
    }

    @Override
    public String getId() {
        return ID_PREFIX + this.api.getCurrencyId(this.adapter);
    }

    @Override
    public void load() {

    }

    @Override
    public boolean canLoad() {
        return this.adapter != null && this.api != null && Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    @Override
    public boolean canPlayerAfford(UUID player, double price) {
        return this.api.canPerformOperations() && this.api.getBalance(player, this.adapter) >= price;
    }

    @Override
    public void withdraw(UUID player, double amount) {
        if (!this.tryWithdraw(player, amount)) {
            throw new IllegalStateException("ExcellentEconomy rejected withdraw operation for " + player);
        }
    }

    @Override
    public boolean tryWithdraw(UUID player, double amount) {
        return this.api.withdraw(player, this.adapter, amount);
    }

    @Override
    public String getName(double amount) {
        return this.api.format(this.adapter, amount);
    }

    public static class ExcellentEconomyCurrencyHandler {
        private final CurrencyRegistry registry;

        public ExcellentEconomyCurrencyHandler(CurrencyRegistry registry) {
            this.registry = registry;
        }

        public boolean registerAll() {
            if (!Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
                return false;
            }

            final ExcellentEconomyApi api = ExcellentEconomyApi.resolve();
            if (api == null) {
                return false;
            }

            this.registry.getCurrencies().keySet().removeIf(id -> id.startsWith(ID_PREFIX));
            boolean anyRegistered = false;
            for (Object adapter : api.getCurrencies()) {
                this.registry.register(new ExcellentEconomyCurrency(api, adapter));
                anyRegistered = true;
            }

            return anyRegistered;
        }
    }

    private static final class ExcellentEconomyApi {
        private final Object provider;
        private final Object context;
        private final Method canPerformOperations;
        private final Method getCurrencies;
        private final Method getCurrencyId;
        private final Method format;
        private final Method getCachedBalance;
        private final Method getAsyncBalance;
        private final Method withdrawCached;
        private final Method withdrawAsync;

        private ExcellentEconomyApi(
            Object provider,
            Object context,
            Method canPerformOperations,
            Method getCurrencies,
            Method getCurrencyId,
            Method format,
            Method getCachedBalance,
            Method getAsyncBalance,
            Method withdrawCached,
            Method withdrawAsync
        ) {
            this.provider = provider;
            this.context = context;
            this.canPerformOperations = canPerformOperations;
            this.getCurrencies = getCurrencies;
            this.getCurrencyId = getCurrencyId;
            this.format = format;
            this.getCachedBalance = getCachedBalance;
            this.getAsyncBalance = getAsyncBalance;
            this.withdrawCached = withdrawCached;
            this.withdrawAsync = withdrawAsync;
        }

        private static ExcellentEconomyApi resolve() {
            try {
                final Class<?> apiClass = Class.forName(API_CLASS);
                final Class<?> currencyClass = Class.forName(CURRENCY_CLASS);
                final Class<?> operationContextClass = Class.forName(OPERATION_CONTEXT_CLASS);
                final Class<?> notificationTargetClass = Class.forName(NOTIFICATION_TARGET_CLASS);
                final RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
                if (registration == null) {
                    return null;
                }

                final Object context = createOperationContext(operationContextClass, notificationTargetClass);
                return new ExcellentEconomyApi(
                    registration.getProvider(),
                    context,
                    apiClass.getMethod("canPerformOperations"),
                    apiClass.getMethod("getCurrencies"),
                    currencyClass.getMethod("getId"),
                    currencyClass.getMethod("format", double.class),
                    apiClass.getMethod("getBalance", Player.class, currencyClass),
                    apiClass.getMethod("getBalanceAsync", UUID.class, currencyClass),
                    apiClass.getMethod("withdraw", Player.class, currencyClass, double.class, operationContextClass),
                    apiClass.getMethod("withdrawAsync", UUID.class, currencyClass, double.class, operationContextClass)
                );
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                Bukkit.getLogger().warning("Failed to resolve ExcellentEconomy API for NightMarket: " + e.getMessage());
                return null;
            }
        }

        private static Object createOperationContext(Class<?> operationContextClass, Class<?> notificationTargetClass)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            final Object context = operationContextClass.getMethod("custom", String.class).invoke(null, "NightMarket");
            final Object targets = Array.newInstance(notificationTargetClass, 3);
            Array.set(targets, 0, notificationTarget(notificationTargetClass, "USER"));
            Array.set(targets, 1, notificationTarget(notificationTargetClass, "EXECUTOR"));
            Array.set(targets, 2, notificationTarget(notificationTargetClass, "CONSOLE_LOGGER"));
            operationContextClass.getMethod("silentFor", targets.getClass()).invoke(context, targets);
            return context;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Enum<?> notificationTarget(Class<?> notificationTargetClass, String name) {
            return Enum.valueOf((Class<? extends Enum>) notificationTargetClass.asSubclass(Enum.class), name);
        }

        private boolean canPerformOperations() {
            return this.invokeBoolean(this.provider, this.canPerformOperations);
        }

        private Set<?> getCurrencies() {
            return this.invoke(this.provider, this.getCurrencies, Set.class);
        }

        private String getCurrencyId(Object currency) {
            return this.invoke(currency, this.getCurrencyId, String.class);
        }

        private String format(Object currency, double amount) {
            return this.invoke(currency, this.format, String.class, amount);
        }

        private double getBalance(UUID player, Object currency) {
            final Player onlinePlayer = Bukkit.getPlayer(player);
            if (onlinePlayer != null) {
                return this.invokeDouble(this.provider, this.getCachedBalance, onlinePlayer, currency);
            }

            final CompletableFuture<?> future = this.invoke(this.provider, this.getAsyncBalance, CompletableFuture.class, player, currency);
            final Object result = future.join();
            if (!(result instanceof Number)) {
                throw new IllegalStateException("ExcellentEconomy returned non-numeric balance for " + player);
            }

            return ((Number) result).doubleValue();
        }

        private boolean withdraw(UUID player, Object currency, double amount) {
            final Player onlinePlayer = Bukkit.getPlayer(player);
            if (onlinePlayer != null) {
                return this.invokeBoolean(this.provider, this.withdrawCached, onlinePlayer, currency, amount, this.context);
            }

            final CompletableFuture<?> future = this.invoke(this.provider, this.withdrawAsync, CompletableFuture.class, player, currency, amount,
                this.context);
            final Object result = future.join();
            return this.isOperationSuccessful(result);
        }

        private boolean isOperationSuccessful(Object result) {
            try {
                final Method success = result.getClass().getMethod("success");
                final Object successResult = success.invoke(result);
                return successResult instanceof Boolean && (Boolean) successResult;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("ExcellentEconomy returned an unreadable operation result.", e);
            }
        }

        private boolean invokeBoolean(Object target, Method method, Object... args) {
            final Object result = this.invoke(target, method, Object.class, args);
            if (!(result instanceof Boolean)) {
                throw new IllegalStateException("ExcellentEconomy returned non-boolean result from " + method.getName());
            }

            return (Boolean) result;
        }

        private double invokeDouble(Object target, Method method, Object... args) {
            final Object result = this.invoke(target, method, Object.class, args);
            if (!(result instanceof Number)) {
                throw new IllegalStateException("ExcellentEconomy returned non-numeric result from " + method.getName());
            }

            return ((Number) result).doubleValue();
        }

        private <T> T invoke(Object target, Method method, Class<T> expectedType, Object... args) {
            try {
                final Object result = method.invoke(target, args);
                return expectedType.cast(result);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to access ExcellentEconomy method " + method.getName(), e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("ExcellentEconomy method failed: " + method.getName(), e.getCause());
            }
        }
    }
}
