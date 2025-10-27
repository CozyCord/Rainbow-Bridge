package net.cozystudios.rainbowbridge.client;

import java.util.ArrayList;
import java.util.List;

public class ClientPetCache {

    private final static List<ClientPetData> pets = new ArrayList<>();

    private ClientPetCache() {}

    public synchronized static void setPets(List<ClientPetData> newPets) {
        pets.clear();
        for (ClientPetData pet : newPets) {
            pets.add(pet);
        }
    }

    public synchronized static List<ClientPetData> getAllPets() {
        return pets;
    }
}
