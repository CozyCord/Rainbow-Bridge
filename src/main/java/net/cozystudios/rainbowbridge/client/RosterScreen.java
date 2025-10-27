package net.cozystudios.rainbowbridge.client;

import java.util.List;

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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class RosterScreen extends BaseUIModelScreen<StackLayout> {
    private int currentPetIndex;

    private List<ClientPetData> pets = null;

    private DynamicEntityComponent entityBox;

    private @Nullable StackLayout entityBoxContainer;

    public RosterScreen() {
        super(StackLayout.class, DataSource.asset(new Identifier("rainbowbridge", "roster")));
    }

    @Override
    protected void build(StackLayout rootComponent) {
        rootComponent.childById(ButtonComponent.class, "summon-button").onPress(button -> {
            System.out.println("click");
        });

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

        if (this.uiAdapter == null)
            return;

        this.entityBoxContainer = this.uiAdapter.rootComponent.childById(StackLayout.class,
                "entity-box-container");
    }

    // Update the current pet
    protected void updateCurrentPet(ClientPetData newPet) {

        assert pets != null;

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
        this.pets = pets;
        // petList.setPets(pets);
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

}
