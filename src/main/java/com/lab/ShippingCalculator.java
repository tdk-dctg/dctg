package com.lab;

public class ShippingCalculator {

    public double calculate(double weight, String type) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        if (type.equals("EXPRESS")) return weight * 5000 + 20000;
        if (type.equals("STANDARD")) return weight * 3000;
        throw new IllegalArgumentException("Unknown type: " + type);
    }
}

