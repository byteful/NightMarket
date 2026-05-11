package me.byteful.plugin.nightmarket.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;

public final class WeightedPicker<T> {
    @NotNull
    private final Map<T, Double> weights = new LinkedHashMap<>();
    private double totalWeight;

    public static <T> WeightedPicker<T> fromCollection(@NotNull Collection<T> values, @NotNull ToDoubleFunction<T> weightFunction) {
        final WeightedPicker<T> picker = new WeightedPicker<>();
        for (T value : values) {
            final double weight = weightFunction.applyAsDouble(value);
            picker.add(value, weight);
        }

        return picker;
    }

    public void add(@NotNull T value, double weight) {
        Objects.requireNonNull(value, "value");
        if (weight <= 0 || !Double.isFinite(weight)) {
            return;
        }

        final Double previousWeight = this.weights.put(value, weight);
        if (previousWeight != null) {
            this.totalWeight -= previousWeight;
        }
        this.totalWeight += weight;
    }

    public boolean remove(T value) {
        final Double removedWeight = this.weights.remove(value);
        if (removedWeight == null) {
            return false;
        }

        this.totalWeight -= removedWeight;
        return true;
    }

    public @NotNull T roll() {
        if (this.weights.isEmpty() || this.totalWeight <= 0) {
            throw new IllegalStateException("Cannot roll from an empty weighted picker.");
        }

        double target = ThreadLocalRandom.current().nextDouble(this.totalWeight);
        for (Map.Entry<T, Double> entry : this.weights.entrySet()) {
            target -= entry.getValue();
            if (target < 0) {
                return entry.getKey();
            }
        }

        throw new IllegalStateException("Weighted picker total is out of sync.");
    }

    public int size() {
        return this.weights.size();
    }

    public boolean isEmpty() {
        return this.weights.isEmpty();
    }
}
