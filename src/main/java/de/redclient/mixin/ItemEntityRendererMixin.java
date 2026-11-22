package de.redclient.mixin;

import de.redclient.DroppedItemTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dropped Item Tweaks: size per item type, and optional 2D mode (items turn flat towards the player). */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    private static boolean redclient$flat;

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
        redclient$flat = DroppedItemTweaks.flat();
    }

    /** Replaces the vanilla spin: in 2D mode the item faces the camera and is flattened. */
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V"),
            require = 0)
    private void redclient$spin(MatrixStack stack, Quaternionf spin) {
        if (!redclient$flat) {
            stack.multiply(spin);
            return;
        }
        float yaw = MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
        stack.multiply(new Quaternionf().rotationY((float) Math.toRadians(180.0 - yaw)));
        stack.scale(1f, 1f, 0.1f);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("RETURN"))
    private void redclient$popScale(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                    CameraRenderState cameraState, CallbackInfo ci) {
        matrices.pop();
        redclient$flat = false;
    }
}
