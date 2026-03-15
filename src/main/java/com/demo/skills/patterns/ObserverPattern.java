package com.demo.skills.patterns;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Observer pattern implemented as a typed EventBus.
 *
 * Listeners register for specific event types and receive only events
 * of that type, avoiding the need for instanceof checks or casting.
 */
public class ObserverPattern {

    // --- Event definitions ---

    sealed interface AppEvent permits UserRegisteredEvent, OrderPlacedEvent, PaymentReceivedEvent {}

    record UserRegisteredEvent(String username, String email) implements AppEvent {}
    record OrderPlacedEvent(String orderId, double amount) implements AppEvent {}
    record PaymentReceivedEvent(String orderId, String method) implements AppEvent {}

    // --- EventBus ---

    static class EventBus {
        private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

        /**
         * Subscribe to a specific event type with a typed handler.
         */
        public <T extends AppEvent> void subscribe(Class<T> eventType, Consumer<T> handler) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                     .add(handler);
        }

        /**
         * Publish an event to all registered handlers for its type.
         */
        @SuppressWarnings("unchecked")
        public <T extends AppEvent> void publish(T event) {
            List<Consumer<?>> handlers = listeners.get(event.getClass());
            if (handlers != null) {
                for (Consumer<?> handler : handlers) {
                    ((Consumer<T>) handler).accept(event);
                }
            }
        }
    }

    // --- Example listeners (simulate real services) ---

    static class EmailService {
        void register(EventBus bus) {
            bus.subscribe(UserRegisteredEvent.class, event ->
                    System.out.printf("  [EmailService] Sending welcome email to %s (%s)%n",
                            event.username(), event.email()));

            bus.subscribe(PaymentReceivedEvent.class, event ->
                    System.out.printf("  [EmailService] Sending receipt for order %s via %s%n",
                            event.orderId(), event.method()));
        }
    }

    static class AnalyticsService {
        void register(EventBus bus) {
            bus.subscribe(UserRegisteredEvent.class, event ->
                    System.out.printf("  [Analytics] Tracking signup: %s%n", event.username()));

            bus.subscribe(OrderPlacedEvent.class, event ->
                    System.out.printf("  [Analytics] Tracking order: %s ($%.2f)%n",
                            event.orderId(), event.amount()));
        }
    }

    static class InventoryService {
        void register(EventBus bus) {
            bus.subscribe(OrderPlacedEvent.class, event ->
                    System.out.printf("  [Inventory] Reserving stock for order %s%n", event.orderId()));
        }
    }

    // --- Demo ---

    public static void main(String[] args) {
        System.out.println("=== Observer Pattern (EventBus) Demo ===\n");

        EventBus bus = new EventBus();

        // Register listeners
        new EmailService().register(bus);
        new AnalyticsService().register(bus);
        new InventoryService().register(bus);

        // Publish events
        System.out.println("Publishing: UserRegisteredEvent");
        bus.publish(new UserRegisteredEvent("jdoe", "jdoe@example.com"));

        System.out.println("\nPublishing: OrderPlacedEvent");
        bus.publish(new OrderPlacedEvent("ORD-5001", 149.99));

        System.out.println("\nPublishing: PaymentReceivedEvent");
        bus.publish(new PaymentReceivedEvent("ORD-5001", "CreditCard"));

        System.out.println("\nDone.");
    }
}
