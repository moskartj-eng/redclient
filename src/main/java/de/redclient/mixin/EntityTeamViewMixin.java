package de.redclient.mixin;

import de.redclient.TeamView;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TeamView: marked players get the vanilla glow outline in the chosen color (visible through walls). */
@Mixin(Entity.class)
public class EntityTeamViewMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void redclient$glow(CallbackInfoReturnable<Boolean> cir) {
        if (TeamView.colorOf((Entity) (Object) this) != null) cir.setReturnValue(true);
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void redclient$color(CallbackInfoReturnable<Integer> cir) {
        Integer c = TeamView.colorOf((Entity) (Object) this);
        if (c != null) cir.setReturnValue(c);
    }
}
