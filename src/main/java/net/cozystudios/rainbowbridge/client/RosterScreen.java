package net.cozystudios.rainbowbridge.client;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
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

    /** The width of the book GUI */
    private static final int TEXTURE_WIDTH = 292;
    /** The height of the book GUI */
    private static final int TEXTURE_HEIGHT = 180;

    private static final int MARGIN_X = 10;
    private static final int PAGE_GAP_X = 10;
    private static final int PAGE_WIDTH = (TEXTURE_WIDTH / 2) - (MARGIN_X * 2) - PAGE_GAP_X;

    private static final int MARGIN_Y = MARGIN_X;
    private static final int PAGE_HEIGHT = TEXTURE_HEIGHT - (MARGIN_Y * 2);

    /** @return The x position of the right page including page margin */
    private int getRightPageX(int width) {
        return ((width - TEXTURE_WIDTH) / 2) + (MARGIN_X) + PAGE_WIDTH + PAGE_GAP_X;
    }

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
        // The left edge of the GUI
        int x = (width - TEXTURE_WIDTH) / 2;
        // The top edge of the GUI
        int y = (height - TEXTURE_HEIGHT) / 2;

        int navButtonWidth = 30;
        int navButtonHeight = 20;
        // The distance between the two buttons
        int navButtonGap = 70;
        prevButton = addDrawableChild(ButtonWidget.builder(Text.of("<"), button -> {
            if (currentPage > 0) {
                currentPage--;
                updateButtons();
            }
        }).dimensions(getRightPageX(width), y + TEXTURE_HEIGHT - MARGIN_Y - navButtonHeight, navButtonWidth,
                navButtonHeight).build());

        nextButton = addDrawableChild(ButtonWidget.builder(Text.of(">"), button -> {
            if (currentPage < renderedPages.size() - 1) {
                currentPage++;
                updateButtons();
            }
        }).dimensions(getRightPageX(width) + navButtonGap, y + TEXTURE_HEIGHT - MARGIN_Y - navButtonHeight,
                navButtonWidth,
                navButtonHeight).build());

        int buttonWidth = 12;
        int buttonHeight = 12;
        // Set the close button in the top right corner partway in the margin
        closeButton = addDrawableChild(ButtonWidget.builder(Text.of("X"), button -> close())
                .dimensions(x + TEXTURE_WIDTH - (MARGIN_X / 4) - buttonWidth, y + (MARGIN_Y / 4), buttonWidth,
                        buttonHeight)
                .build());

        updateButtons();

        int listLeft = x + 20;
        int listTop = y + MARGIN_Y + 25;
        int listBottom = y + TEXTURE_HEIGHT - 40;
        int entryHeight = 12;

        petList = new PetListWidget(this, client, PAGE_WIDTH, PAGE_HEIGHT, listTop, listBottom, entryHeight);

        petList.setLeftPos(listLeft);

        this.addSelectableChild(petList);

        // Request pets from server
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ClientPlayNetworking.send(RainbowBridgePackets.REQUEST_PET_TRACKER, buf);
    }

    private void updateButtons() {
        if (petList != null)
            petList.setSelectedIndex(currentPage);
        prevButton.active = currentPage > 0;
        nextButton.active = renderedPages.size() > 1 && currentPage < renderedPages.size() - 1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = (width - TEXTURE_WIDTH) / 2;
        int y = (height - TEXTURE_HEIGHT) / 2;

        context.drawTexture(BOOK_TEXTURE, x, y, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (pets == null) {
            context.drawText(textRenderer, "Loading pets...", x + 40, y + 80, 0x888888, false);
            return;
        }

        /** ==================== */
        /** Left Page */

        // Draw the title
        context.drawText(textRenderer, "Pets", x + MARGIN_X, y + MARGIN_Y, 0x888888, false);

        // Draw the list
        if (this.petList != null) {
            this.petList.render(context, mouseX, mouseY, delta);
        }

        /** End Left Page */
        /** ==================== */

        /** ==================== */
        /** Right Page */

        // Draw current pet’s entity model
        int petModelWidth = 80;
        if (pets != null && currentPage < pets.size()) {
            ClientPetData currentPet = pets.get(currentPage);
            int entityX = getRightPageX(width);
            int entityY = y + MARGIN_Y + 12;
            int size = petModelWidth / 2; // Render size of the entity

            // Compute relative mouse offset
            float relMouseX = entityX + (size / 2) - mouseX;
            float relMouseY = entityY - mouseY;

            if (currentPet.entity != null) {
                InventoryScreen.drawEntity(
                        context,
                        entityX + (size / 2), // x for entity is at left of box
                        entityY + (size / 2), // y for entity is at bottom of box
                        size,
                        relMouseX,
                        relMouseY,
                        (LivingEntity) currentPet.entity);
            }
        }

        // Draw current pet's text lines
        if (currentPage >= 0 && currentPage < renderedPages.size()) {
            Text[] lines = renderedPages.get(currentPage);
            int lineHeight = 10;
            int textY = y + 20;

            var tr = MinecraftClient.getInstance().textRenderer;

            // First line (title)
            String trimmedName = tr.trimToWidth(lines[1], PAGE_WIDTH - tr.getWidth("…")).getString();
            if (!trimmedName.equals(lines[0].getString())) {
                trimmedName += "…";
            }
            Text lineText = Text.literal(trimmedName).styled(s -> s.withColor(0xFFFFFF));
            context.drawText(textRenderer, lineText, getRightPageX(width) + (petModelWidth / 2),
                    textY + 1 * lineHeight,
                    0x303030, false);

            // Second line (position)
            context.getMatrices().push(); // Push the current matrix stack so we don't affect it
            float scale = 0.8f;
            context.getMatrices().scale(scale, scale, 1.0f);
            context.drawText(textRenderer, lineText, getRightPageX(width) + (petModelWidth / 2),
                    textY + 0 * lineHeight,
                    0x303030, false);
            context.getMatrices().pop(); // Restore the previous matrix stack

            // Third, fourth, etc line(s)
            for (int i = 2; i < lines.length; i++) {
                String tn = tr.trimToWidth(lines[i], PAGE_WIDTH - tr.getWidth("…")).getString();
                if (!tn.equals(lines[i].getString())) {
                    tn += "…";
                }
                Text lt = Text.literal(tn).styled(s -> s.withColor(0xFFFFFF));
            }
        }
        /** End Right Page */
        /** ==================== */

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
                    Text.literal("Equip a collar on a tamed pet to register it!")
            });
        } else {
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
