package net.cozystudios.rainbowbridge.client;

import java.util.ArrayList;
import java.util.List;

import net.cozystudios.rainbowbridge.petdatabase.PetListUpdateEvents;

public class ClientPetList {

    private static final List<ClientPetData> pets = new ArrayList<>();
    private static final List<Runnable> listeners = new ArrayList<>();

    static {
        // Subscribe to server updates once when class is loaded
        PetListUpdateEvents.subscribe((playerUuid, newPetList) -> {
            setPets(newPetList);
        });
    }

    public static synchronized void setPets(List<ClientPetData> newPets) {
        pets.clear();
        pets.addAll(newPets);
        notifyListeners();
    }

    public static synchronized List<ClientPetData> getAllPets() {
        return List.copyOf(pets);
    }

    // --- subscription methods ---
    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
