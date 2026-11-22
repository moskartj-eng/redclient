package de.redclient.mixin;

import de.redclient.MixinMarker;
import de.redclient.TeamView;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TeamView: the name of marked players is shown in the chosen color (bold). */
@Mixin(Entity.class)
public class EntityTeamViewMixin implements MixinMarker {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void redclient$name(CallbackInfoReturnable<Text> cir) {
        Integer c = TeamView.colorOf((Entity) (Object) this);
        if (c == null || cir.getReturnValue() == null) return;
        final int rgb = c;
        cir.setReturnValue(cir.getReturnValue().copy().styled(s -> s.withColor(rgb).withBold(true)));
    }
}
