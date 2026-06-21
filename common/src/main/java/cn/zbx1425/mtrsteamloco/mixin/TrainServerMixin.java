package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.TrainServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(TrainServer.class)
public class TrainServerMixin {
    @Redirect(method = "simulateTrain", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object simulateTrainNewHashMap(Map<Object, Object> instance, Object k, Object v) {
        return instance.put(k, Collections.synchronizedList((List<?>) v));
    }
}
