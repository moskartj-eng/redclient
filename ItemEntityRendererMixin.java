package de.redclient.mixin;

import de.redclient.DroppedItemTweaks;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skaliert gedroppte Items (nur die, die im Dropped-Item-Tweaks-Menue gewaehlt wurden). */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
            at = @At("TAIL"))
    private void redclient$storeScale(ItemEntity entity, ItemEntityRenderState state, float tickProgress, CallbackInfo ci) {
        ((ItemScaleAccess) state).redclient$setScale(DroppedItemTweaks.scaleFor(entity.getStack()));
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"))
    private void redclient$pushScale(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                     CameraRenderState cameraState, CallbackInfo ci) {
        matrices.push();
        float s = ((ItemScaleAccess) state).redclient$getScale();
        if (s != 1f) matrices.scale(s, s, s);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("RETURN"))
    private void redclient$popScale(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                    CameraRenderState cameraState, CallbackInfo ci) {
        matrices.pop();
    }
}
