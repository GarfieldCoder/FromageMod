package fromage;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

final class PungentEffect extends MobEffect {
    PungentEffect() {
        super(MobEffectCategory.NEUTRAL, 0x70AA37);
    }

    static void apply(Player player) {
        player.addEffect(new MobEffectInstance(Fromage.PUNGENT, 20 * 20));
    }

    static void addAvoidance(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Animal animal)) return;
        EntityType<?> type = animal.getType();
        if (type != EntityType.COW && type != EntityType.PIG && type != EntityType.SHEEP && type != EntityType.CHICKEN) return;
        if (animal.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof AvoidAroma)) {
            animal.goalSelector.addGoal(2, new AvoidAroma(animal));
        }
    }

    private static final class AvoidAroma extends AvoidEntityGoal<Player> {
        AvoidAroma(Animal animal) {
            super(animal, Player.class, 5, 1.0, 1.15,
                entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()
                    && player.hasEffect(Fromage.PUNGENT));
        }
    }
}
