package net.cozystudios.rainbowbridge.client;

import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class PetListWidget extends ElementListWidget<PetListWidget.PetEntry> {
    private final RosterScreen parent;
    private Object hoveredEntry;
    private boolean renderHorizontalShadows;
    private int selectedIndex = -1;

    public PetListWidget(RosterScreen parent, MinecraftClient client, int width, int height, int top, int bottom,
            int itemHeight) {
        super(client, width, height, top, bottom, itemHeight);
        this.parent = parent;
    }

    public void setPets(List<ClientPetData> pets) {
        clearEntries();
        for (int i = 0; i < pets.size(); i++) {
            addEntry(new PetEntry(pets.get(i), i, parent));
        }
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override
    protected int getScrollbarPositionX() {
        int ogScroll = this.width / 2 + 124;
        return this.left + this.width - 6;
    }

    /**
     * I ripped this method from ElementListWidget because I needed to stop the
     * background from rendering
     * while still rendering the list entries and scrollbar.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        // horrid hack
        int i = this.getScrollbarPositionX();
        int j = i + 6;
        hoveredEntry = this.isMouseOver(mouseX, mouseY)
                ? this.getEntryAtPosition(mouseX, mouseY)
                : null;

        int k = this.getRowLeft();
        int l = this.top + 4 - (int) this.getScrollAmount();
        this.enableScissor(context);
        this.renderHeader(context, k, l);

        this.renderList(context, mouseX, mouseY, delta);
        context.disableScissor();
        if (this.renderHorizontalShadows) {
            context.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
            context.drawTexture(Screen.OPTIONS_BACKGROUND_TEXTURE, this.left, 0, 0.0F, 0.0F, this.width, this.top, 32,
                    32);
            context.drawTexture(Screen.OPTIONS_BACKGROUND_TEXTURE, this.left, this.bottom, 0.0F, (float) this.bottom,
                    this.width, this.height - this.bottom, 32, 32);
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            context.fillGradient(RenderLayer.getGuiOverlay(), this.left, this.top, this.right, this.top + 4, -16777216,
                    0, 0);
            context.fillGradient(RenderLayer.getGuiOverlay(), this.left, this.bottom - 4, this.right, this.bottom, 0,
                    -16777216, 0);
        }

        int m = this.getMaxScroll();
        if (m > 0) {
            int n = (int) ((float) ((this.bottom - this.top) * (this.bottom - this.top))
                    / (float) this.getMaxPosition());
            n = MathHelper.clamp(n, 32, this.bottom - this.top - 8);
            int o = (int) this.getScrollAmount() * (this.bottom - this.top - n) / m + this.top;
            if (o < this.top) {
                o = this.top;
            }

            context.fill(i, this.top, j, this.bottom, -16777216);
            context.fill(i, o, j, o + n, -8355712);
            context.fill(i, o, j - 1, o + n - 1, -4144960);
        }

        this.renderDecorations(context, mouseX, mouseY);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderBackground(DrawContext context) {
        // no-op to avoid the default shaded backdrop
    }

    public static class PetEntry extends ElementListWidget.Entry<PetEntry> {
        private final ClientPetData pet;
        private final int index;
        private final RosterScreen parent;

        public PetEntry(ClientPetData pet, int index, RosterScreen parent) {
            this.pet = pet;
            this.index = index;
            this.parent = parent;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                int mouseX, int mouseY, boolean hovered, float tickDelta) {

            boolean selected = parent.petList.getSelectedIndex() == this.index;

            // Background highlight
            int padding = 1;
            int backgroundColor = hovered ? 0x553366FF : 0x22000000;
            backgroundColor = selected ? 0xAA3366FF : backgroundColor;
            context.fill(x - padding, y - padding, x + entryWidth + padding, y + entryHeight + padding,
                    backgroundColor);

            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int leftPadding = 6;
            int rightPadding = 6;

            // Compute actual drawable width in GUI pixels
            int availableWidth = entryWidth - leftPadding - rightPadding;

            // Trim pet name to fit the width
            String trimmedName = tr.trimToWidth(pet.name, availableWidth - tr.getWidth("…"));
            if (!trimmedName.equals(pet.name))
                trimmedName += "…";
            Text name = Text.literal(trimmedName).styled(s -> s.withColor(0xFFFFFF));

            int textY = y + (entryHeight - tr.fontHeight) / 2;
            context.drawText(tr, name,  x, textY, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isMouseOver(mouseX, mouseY)) {
                parent.setCurrentPage(index);
                parent.petList.setSelectedIndex(index);
                return true;
            }
            return false;
        }

        // Return selectable children (widgets contained inside this entry). We don't
        // have any, so empty list.
        @Override
        public List<? extends Selectable> selectableChildren() {
            return Collections.emptyList();
        }

        // Return children (elements participating in event handling). Also empty.
        @Override
        public List<? extends Element> children() {
            return Collections.emptyList();
        }

    }
}
