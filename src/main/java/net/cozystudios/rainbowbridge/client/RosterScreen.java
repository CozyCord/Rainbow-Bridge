package net.cozystudios.rainbowbridge.client;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.cozystudios.rainbowbridge.homeblock.HomeBlockUpdateEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RosterScreen extends BaseUIModelScreen<StackLayout> {
    private int currentPetIndex;

    private DynamicEntityComponent entityBox;

    private @Nullable StackLayout entityBoxContainer;

    private ClientPetData currentPet;

    private @Nullable ButtonComponent homeButton; // Component for the send pet to home button
    private @Nullable LabelComponent homeLabel; // Component for the coordinates
    private BiConsumer<UUID, BlockPos> homeUpdateListener;

    public RosterScreen() {
        super(StackLayout.class, DataSource.asset(new Identifier("rainbowbridge", "roster")));
    }

    @Override
    protected void build(StackLayout rootComponent) {

        // Summon button
        rootComponent.childById(ButtonComponent.class, "summon-button").onPress(button -> {
            assert currentPet != null;

            // Get player position
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null)
                return;
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            boolean shouldSit = false;

            // Send teleport packet
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeUuid(currentPet.entity.getUuid());
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeBoolean(shouldSit);

            ClientPlayNetworking.send(RainbowBridgePackets.REQUEST_PET_TELEPORT, buf);
        });

        // Home button
        this.homeButton = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "home-button");
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {

            var homePos = ClientHomeBlock.get();
            this.homeButton.visible = homePos != null;
            this.homeButton.onPress(button -> {
                if (homePos == null) {
                    return;
                }

                assert currentPet != null;

                double x = homePos.getX();
                double y = homePos.getY();
                double z = homePos.getZ();
                boolean shouldSit = true;

                // Send teleport packet
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeUuid(currentPet.entity.getUuid());
                buf.writeDouble(x);
                buf.writeDouble(y);
                buf.writeDouble(z);
                buf.writeBoolean(shouldSit);

                ClientPlayNetworking.send(RainbowBridgePackets.REQUEST_PET_TELEPORT, buf);
            });
        }

        StackLayout container = rootComponent.childById(StackLayout.class, "entity-box-container");

        // Remove old entity component if present
        if (!container.children().isEmpty()) {
            container.removeChild(container.children().get(0));
        }

        // Insert the new one
        if (entityBox != null) {
            entityBox.lookAtCursor(true);
            entityBox.scaleToFit(true);
            container.child(entityBox);
        }

    }

    // @Override
    protected void init() {
        super.init();

        // Request pets from server
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ClientPlayNetworking.send(RainbowBridgePackets.REQUEST_PET_TRACKER, buf);

        this.homeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class,
                "home-position-label");

        this.homeButton = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "home-button");

        homeUpdateListener = (uuid, newHomePos) -> {
            if (uuid.equals(MinecraftClient.getInstance().player.getUuid())) {
                homeLabel.text(Text.literal(newHomePos.toShortString()));
                homeButton.visible = true;
            }
        };

        HomeBlockUpdateEvents.subscribe(homeUpdateListener);

        // Get home position
        BlockPos home = ClientHomeBlock.get();
        if (home != null) {
            homeLabel.text(Text.literal(home.toShortString()));
            homeButton.visible = true;
        }

        // Subscribe for live updates
        ClientPetList.addListener(this::refreshPetList);

        this.entityBoxContainer = this.uiAdapter.rootComponent.childById(StackLayout.class,
                "entity-box-container");

        client.execute(() -> {
            List<ClientPetData> pets = ClientPetList.getAllPets();
            if (pets != null && !pets.isEmpty()) {
                setPets(pets);
            }
        });
    }

    // Update the current pet
    protected void updateCurrentPet(ClientPetData newPet) {

        List<ClientPetData> pets = ClientPetList.getAllPets();
        assert pets != null;

        this.currentPetIndex = pets.indexOf(newPet);
        this.currentPet = newPet;

        // Update entity box
        entityBoxContainer.removeChild(entityBox);
        DynamicEntityComponent newEntityBox = DynamicEntityComponent.fromPet(newPet, 50);
        newEntityBox.lookAtCursor();
        newEntityBox.scaleToFit();
        entityBox = newEntityBox;
        entityBoxContainer.child(entityBox);

        // Update name
        LabelComponent nameLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class, "pet-name-label");
        String clipped = MinecraftClient.getInstance().textRenderer.trimToWidth(
                Text.literal(newPet.name).getString(),
                nameLabel.maxWidth());
        nameLabel.text(Text.literal(clipped));
        nameLabel.tooltip(Text.literal(newPet.name));

        // Update position
        LabelComponent positionLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class,
                "pet-position-label");
        positionLabel.text(Text.literal(newPet.position));
    }

    public void setPets(List<ClientPetData> pets) {
        currentPetIndex = 0;
        updateCurrentPet(pets.get(currentPetIndex));

        // Find the container
        FlowLayout petListContainer = uiAdapter.rootComponent.childById(FlowLayout.class, "pet-list");

        // Clear old buttons if necessary
        petListContainer.clearChildren();

        var i = 0;
        // Generate buttons for all pets
        for (ClientPetData pet : pets) {

            int normalBg = (0x00000000); // transparent
            int hoverBg = (0xFFFFFFFF); // white

            Color normalText = Color.ofDye(DyeColor.WHITE);
            Color hoverText = Color.ofDye(DyeColor.BROWN);

            StackLayout wrapper = Containers.stack(Sizing.fill(95), Sizing.content());
            wrapper.surface(Surface.flat(normalBg));

            LabelComponent label = (LabelComponent) Components.label(Text.literal(pet.name))
                    .cursorStyle(CursorStyle.HAND)
                    .horizontalSizing(Sizing.fill(100))
                    .margins(Insets.vertical(2));

            label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    currentPetIndex = i;
                    updateCurrentPet(pet);
                    MinecraftClient.getInstance().getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
                return false;
            });

            label.mouseEnter().subscribe(() -> {
                label.color(hoverText);
                wrapper.surface(Surface.flat(hoverBg));
            });

            label.mouseLeave().subscribe(() -> {
                label.color(normalText);
                wrapper.surface(Surface.flat(normalBg));
            });

            wrapper.child(label);

            petListContainer.child(wrapper);
        }

    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        super.close();
        ClientPetList.removeListener(this::refreshPetList);
        HomeBlockUpdateEvents.unsubscribe(homeUpdateListener);
    }

    private void refreshPetList() {
        List<ClientPetData> pets = ClientPetList.getAllPets();
        setPets(pets);
    }

}
