package de.redclient.mixin;

import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import de.redclient.MixinMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemScaleAccess, MixinMarker {
    @Unique
    private float redclient$scale = 1f;

    @Override
    public float redclient$getScale() {
        return redclient$scale;
    }

    @Override
    public void redclient$setScale(float scale) {
        this.redclient$scale = scale;
    }
}
