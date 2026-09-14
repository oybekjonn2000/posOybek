package com.restaurantpos.delivery.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeliveryProviderRegistry {

    private final Map<String, DeliveryProvider> providerMap = new ConcurrentHashMap<>();

    public DeliveryProviderRegistry(List<DeliveryProvider> providers) {
        for (DeliveryProvider p : providers) {
            providerMap.put(p.getProviderCode().toUpperCase(), p);
        }
    }

    public Optional<DeliveryProvider> getProvider(String code) {
        if (code == null) return Optional.empty();
        DeliveryProvider provider = providerMap.get(code.toUpperCase());
        if (provider != null) return Optional.of(provider);

        // Fallback checks e.g. YANDEX -> YANDEX_EATS
        for (Map.Entry<String, DeliveryProvider> entry : providerMap.entrySet()) {
            if (entry.getKey().startsWith(code.toUpperCase()) || code.toUpperCase().startsWith(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public List<DeliveryProvider> getAllProviders() {
        return List.copyOf(providerMap.values());
    }
}
