package net.cozystudios.rainbowbridge.client;

import java.util.function.Consumer;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.util.NinePatchTexture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TooltipButtonComponent extends ButtonComponent {
    public boolean looksDisabled = false;

    public TooltipButtonComponent(Text message, Consumer<ButtonComponent> onPress) {
        super(message, onPress);
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier texture;
        if (looksDisabled) {
            texture = DISABLED_TEXTURE;
        } else if (this.hovered) {
            texture = HOVERED_TEXTURE;
        } else {
            texture = ACTIVE_TEXTURE;
        }

        NinePatchTexture.draw(texture, (OwoUIDrawContext) context, getX(), getY(), width, height);
        super.renderButton(context, mouseX, mouseY, delta); // optional: draws text + tooltip
    }
}