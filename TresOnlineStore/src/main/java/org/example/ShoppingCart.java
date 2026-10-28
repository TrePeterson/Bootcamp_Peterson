package org.example;

import java.math.BigDecimal;
import java.util.ArrayList;

public final class ShoppingCart {
    private final ArrayList<CartItem> items = new ArrayList<>();

    public void addProduct(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        for (int index = 0; index < items.size(); index++) {
            CartItem item = items.get(index);
            if (item.getProduct().getSku().equals(product.getSku())) {
                int newQuantity;
                try {
                    newQuantity = Math.addExact(item.getQuantity(), quantity);
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("The requested quantity is too large.", exception);
                }
                items.set(index, new CartItem(item.getProduct(), newQuantity));
                return;
            }
        }

        items.add(new CartItem(product, quantity));
    }

    public boolean removeProduct(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        CartItem item = findItem(sku);
        if (item == null) {
            return false;
        }
        if (quantity > item.getQuantity()) {
            throw new IllegalArgumentException("Cannot remove more than the quantity in your cart.");
        }

        int index = items.indexOf(item);
        int remainingQuantity = item.getQuantity() - quantity;
        if (remainingQuantity == 0) {
            items.remove(index);
        } else {
            items.set(index, new CartItem(item.getProduct(), remainingQuantity));
        }
        return true;
    }

    public CartItem findItem(String sku) {
        if (sku == null) {
            return null;
        }
        String trimmedSku = sku.trim();
        for (CartItem item : items) {
            if (item.getProduct().getSku().equalsIgnoreCase(trimmedSku)) {
                return item;
            }
        }
        return null;
    }

    public ArrayList<CartItem> getItems() {
        return new ArrayList<>(items);
    }

    public long getItemCount() {
        long count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (CartItem item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }
}
