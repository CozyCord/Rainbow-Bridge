package net.cozystudios.rainbowbridge.client;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RosterScreen extends Screen {
    private static final Identifier BOOK_TEXTURE = new Identifier("rainbowbridge", "textures/gui/book_spread.png");
    private static final int TEXTURE_WIDTH = 296;
    private static final int TEXTURE_HEIGHT = 180;

    public PetListWidget petList;

    private List<ClientPetData> pets = null;
    private final List<Text[]> renderedPages = new ArrayList<>(); // precomputed pages
    private int currentPage = 0;

    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private ButtonWidget closeButton;

    public RosterScreen() {
        super(Text.of("Book"));
    }

    @Override
    protected void init() {
        int x = (width - TEXTURE_WIDTH) / 2;
        int y = (height - TEXTURE_HEIGHT) / 2;

        prevButton = addDrawableChild(ButtonWidget.builder(Text.of("<"), button -> {
            if (currentPage > 0) {
                currentPage--;
                updateButtons();
            }
        }).dimensions(x + 20, y + TEXTURE_HEIGHT - 28, 50, 20).build());

        nextButton = addDrawableChild(ButtonWidget.builder(Text.of(">"), button -> {
            if (currentPage < renderedPages.size() - 1) {
                currentPage++;
                updateButtons();
            }
        }).dimensions(x + TEXTURE_WIDTH - 70, y + TEXTURE_HEIGHT - 28, 50, 20).build());

        closeButton = addDrawableChild(ButtonWidget.builder(Text.of("X"), button -> close())
                .dimensions(x + TEXTURE_WIDTH - 18, y + 6, 12, 12).build());

        updateButtons();

        int listLeft = x + TEXTURE_WIDTH / 2 + 20;
        int listWidth = TEXTURE_WIDTH / 2 - 30;
        int listTop = y + 24;
        int listBottom = y + TEXTURE_HEIGHT - 40;
        int entryHeight = 14;

        petList = new PetListWidget(this, client, listWidth, this.height, listTop, listBottom, entryHeight);

        petList.setLeftPos(listLeft);


        this.addSelectableChild(petList);

        // Request pets from server
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ClientPlayNetworking.send(RainbowBridgePackets.REQUEST_PET_TRACKER, buf);
    }

    private void updateButtons() {
        if (petList != null) petList.setSelectedIndex(currentPage);
        prevButton.active = currentPage > 0;
        nextButton.active = renderedPages.size() > 1 && currentPage < renderedPages.size() - 1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = (width - TEXTURE_WIDTH) / 2;
        int y = (height - TEXTURE_HEIGHT) / 2;

        context.drawTexture(BOOK_TEXTURE, x, y, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (pets != null && currentPage < pets.size()) {
            ClientPetData currentPet = pets.get(currentPage);
            if (currentPet.entity != null) {
                InventoryScreen.drawEntity(
                        context,
                        x + 80,
                        y + 130,
                        40,
                        x - mouseX,
                        y - mouseY,
                        (LivingEntity) currentPet.entity
                );
            }
        }


        if (pets == null) {
            context.drawText(textRenderer, "Loading pets...", x + 40, y + 80, 0x888888, false);
            return;
        }

        // Draw the list
        if (this.petList != null) {
            this.petList.render(context, mouseX, mouseY, delta);
        }

        // Draw current page’s text lines
        if (currentPage >= 0 && currentPage < renderedPages.size()) {
            Text[] lines = renderedPages.get(currentPage);
            int marginX = 20;
            int lineHeight = 10;
            int textY = y + 20;

            for (int i = 0; i < lines.length; i++) {
                context.drawText(textRenderer, lines[i], x + marginX, textY + i * lineHeight, 0x303030, false);
            }
        }

        // Draw buttons and other elements
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.Drawable) {
                ((net.minecraft.client.gui.Drawable) child).render(context, mouseX, mouseY, delta);
            }
        }
    }

    public void setPets(List<ClientPetData> pets) {
        this.pets = pets;
        this.renderedPages.clear();

        if (pets.isEmpty()) {
            renderedPages.add(new Text[] {
                    Text.literal("You have no pets tracked."),
                    Text.literal("Equip a collar to register one!")
            });
        } else {
            // Page 0: list
            // List<Text> listPage = new ArrayList<>();
            // listPage.add(Text.literal("Your Pets:"));
            // for (ClientPetData pet : pets) {
            // listPage.add(Text.literal(" - " + pet.name)
            // .styled(s -> s.withColor(0x3366ff).withUnderline(true)));
            // }
            // renderedPages.add(listPage.toArray(Text[]::new));

            // Additional pages for each pet
            for (ClientPetData pet : pets) {
                renderedPages.add(new Text[] {
                        Text.literal(pet.name).styled(s -> s.withBold(true)),
                        Text.literal("Position: " + pet.position)
                });
            }
        }

        petList.setPets(pets);

        currentPage = 0;
        updateButtons();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }


    public void setCurrentPage(int i) {
        if (i < 0 || i >= renderedPages.size())
            return;
        currentPage = i;
        updateButtons();
    }
}
