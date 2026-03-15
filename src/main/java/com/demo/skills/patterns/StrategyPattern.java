package com.demo.skills.patterns;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Strategy pattern for payment processing.
 *
 * Different payment strategies implement the same interface, allowing the
 * payment processor to work with any method without conditional logic.
 */
public class StrategyPattern {

    // --- Payment result ---

    record PaymentResult(boolean success, String transactionId, String message) {}

    // --- Strategy interface ---

    interface PaymentStrategy {
        PaymentResult pay(double amount);
        String methodName();
    }

    // --- Concrete strategies ---

    static class CreditCardPayment implements PaymentStrategy {
        private final String cardNumber;
        private final String cardHolder;

        CreditCardPayment(String cardNumber, String cardHolder) {
            this.cardNumber = cardNumber;
            this.cardHolder = cardHolder;
        }

        @Override
        public PaymentResult pay(double amount) {
            String masked = "****-" + cardNumber.substring(cardNumber.length() - 4);
            String txId = "CC-" + System.nanoTime();
            System.out.printf("  Processing credit card %s for %s...%n", masked, cardHolder);
            return new PaymentResult(true, txId,
                    String.format("Charged $%.2f to card %s", amount, masked));
        }

        @Override
        public String methodName() { return "CreditCard"; }
    }

    static class BankTransferPayment implements PaymentStrategy {
        private final String iban;

        BankTransferPayment(String iban) {
            this.iban = iban;
        }

        @Override
        public PaymentResult pay(double amount) {
            String txId = "BT-" + System.nanoTime();
            System.out.printf("  Initiating bank transfer from IBAN %s...%n", iban);
            return new PaymentResult(true, txId,
                    String.format("Transferred $%.2f from %s", amount, iban));
        }

        @Override
        public String methodName() { return "BankTransfer"; }
    }

    static class CryptoPayment implements PaymentStrategy {
        private final String walletAddress;
        private final String currency;

        CryptoPayment(String walletAddress, String currency) {
            this.walletAddress = walletAddress;
            this.currency = currency;
        }

        @Override
        public PaymentResult pay(double amount) {
            String txId = "CRYPTO-" + System.nanoTime();
            System.out.printf("  Sending %s payment from wallet %s...%n",
                    currency, walletAddress.substring(0, 8) + "...");
            return new PaymentResult(true, txId,
                    String.format("Sent %.4f %s (equiv. $%.2f)", amount / 50000.0, currency, amount));
        }

        @Override
        public String methodName() { return "Crypto(" + currency + ")"; }
    }

    // --- Context: PaymentProcessor ---

    static class PaymentProcessor {
        private PaymentStrategy strategy;

        PaymentProcessor(PaymentStrategy strategy) {
            this.strategy = strategy;
        }

        void setStrategy(PaymentStrategy strategy) {
            this.strategy = strategy;
        }

        void processPayment(String orderId, double amount) {
            System.out.printf("Processing order %s ($%.2f) via %s%n",
                    orderId, amount, strategy.methodName());
            PaymentResult result = strategy.pay(amount);

            if (result.success()) {
                System.out.printf("  SUCCESS | TxID: %s | %s%n%n", result.transactionId(), result.message());
            } else {
                System.out.printf("  FAILED  | %s%n%n", result.message());
            }
        }
    }

    // --- Demo ---

    public static void main(String[] args) {
        System.out.println("=== Strategy Pattern (Payments) Demo ===\n");

        PaymentProcessor processor = new PaymentProcessor(
                new CreditCardPayment("4111111111111234", "Alice Johnson"));

        processor.processPayment("ORD-001", 59.99);

        // Switch strategy at runtime
        processor.setStrategy(new BankTransferPayment("DE89370400440532013000"));
        processor.processPayment("ORD-002", 1250.00);

        processor.setStrategy(new CryptoPayment("0xABCDEF1234567890ABCDEF", "BTC"));
        processor.processPayment("ORD-003", 499.95);

        System.out.println("Done.");
    }
}
