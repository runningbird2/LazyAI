package com.pixelindiedev.lazy_ai_pixelindiedev.mixin;

import com.pixelindiedev.lazy_ai_pixelindiedev.Lazy_ai_pixelindiedev;
import com.pixelindiedev.lazy_ai_pixelindiedev.interfaces.VillagerCacheAccessor;
import com.pixelindiedev.lazy_ai_pixelindiedev.mixin.integration.VillagerEntityAccessor;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityInteraction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.pixelindiedev.lazy_ai_pixelindiedev.LazyAI$BlockChecker.hasSolidCollision;
import static com.pixelindiedev.lazy_ai_pixelindiedev.Lazy_ai_pixelindiedev.getOptimalizationType;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin implements VillagerCacheAccessor {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("LazyAI");
    @Unique
    private final static int[] cooldowns = {50, 90, 150};  // Cooldowns from close to far, in ticks
    @Unique
    private final static int[] cooldownsAgressive = {70, 115, 250};
    @Unique
    private final static int[] cooldownsMinimal = {30, 70, 110};
    @Unique
    private final Long2BooleanOpenHashMap cachedBlockPos = new Long2BooleanOpenHashMap(9);
    @Unique
    private final Direction[] directionsDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    @Unique
    private BlockPos.Mutable reusableSide = new BlockPos.Mutable();
    @Unique
    private VillagerEntity villager;
    @Unique
    private boolean isInTradingHall;
    @Unique
    private boolean shouldRefreshTradingHall;
    @Unique
    private BlockPos lastStandingLocation;
    @Unique
    private int randomSelectedTick;
    @Unique
    private RegistryEntry<VillagerProfession> cachedProfessionEntry;
    @Unique
    private RegistryKey<VillagerProfession> cachedProfessionKey;
    @Unique
    private static boolean tradingHallOptimizationDisabledForSession;

    @Shadow
    protected abstract void resetCustomer();

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;Lnet/minecraft/registry/entry/RegistryEntry;)V", at = @At("RETURN"))
    private void captureMob(EntityType entityType, World world, RegistryEntry type, CallbackInfo ci) {
        villager = (VillagerEntity) (Object) this;
        isInTradingHall = false;
        shouldRefreshTradingHall = false;
        randomSelectedTick = villager.getId();
        cachedProfessionEntry = null;
        cachedProfessionKey = null;
        reusableSide = new BlockPos.Mutable();
    }

    @Override
    public void lazyai$invalidateBlockCache(BlockPos pos) {
        long key = pos.asLong();
        if (cachedBlockPos.containsKey(key)) {
            cachedBlockPos.remove(key);
            shouldRefreshTradingHall = true;
        }
    }

    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void skipIdleTradingHallTick(ServerWorld world, CallbackInfo ci) {
        if (!Lazy_ai_pixelindiedev.getEnableVillagerTradingHallOptimization() || tradingHallOptimizationDisabledForSession) {
            return;
        }
        if (villager == null || !villager.isAlive() || villager.isBaby() || villager.isPanicking()) return;
        try {
            if (!isInTradingCell(villager)) return;

            RegistryKey<VillagerProfession> villagerprof = getCachedProfession();
            if (villagerprof == VillagerProfession.NONE || villagerprof == VillagerProfession.NITWIT) return;

            if (villager.hasCustomer()) return;

            if (((villager.age + randomSelectedTick) & 31) != 0) {
                VillagerEntityAccessor accessor = (VillagerEntityAccessor) villager;

                int tempInt = accessor.getLevelUpTimer();
                if (!villager.hasCustomer() && tempInt > 0) {
                    accessor.setLevelUpTimer(tempInt - 1);

                    if (accessor.getLevelUpTimer() <= 0) {
                        if (accessor.isLevelingUp()) accessor.invokeLevelUp(world);
                        villager.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0));
                    }
                }

                PlayerEntity lastcust = accessor.getLastCustomer();
                if (lastcust != null) {
                    world.handleInteraction(EntityInteraction.TRADE, lastcust, villager);
                    world.sendEntityStatus(villager, (byte) 14);
                    accessor.setLastCustomer(null);
                }

                if (villager.getVillagerData().profession().matchesKey(VillagerProfession.NONE) && villager.hasCustomer())
                    resetCustomer();

                ci.cancel();
            }
        } catch (RuntimeException e) {
            disableTradingHallOptimization(e);
        }
    }

    @Unique
    private boolean isInTradingCell(VillagerEntity villager) {
        int[] cooldownList = getCooldownList();
        int cooldown = VillagerTradingHallOptimization.selectCooldown(cooldownList, Lazy_ai_pixelindiedev.getDistance(villager));

        if (Math.floorMod(villager.age + randomSelectedTick, cooldown) != 0) return isInTradingHall;

        final BlockPos center = villager.getBlockPos();
        //if block was changed near, or villager is no longer standing in the same spot
        if (shouldRefreshTradingHall || !center.equals(lastStandingLocation)) {
            shouldRefreshTradingHall = false;
            lastStandingLocation = center.toImmutable();

            final World world = villager.getEntityWorld();

            boolean[] baseSolid = new boolean[directionsDirections.length];
            boolean[] upperSolid = new boolean[directionsDirections.length];
            for (int index = 0; index < directionsDirections.length; index++) {
                Direction direction = directionsDirections[index];
                reusableSide.set(center, direction);
                baseSolid[index] = getCachedSolidBlock(world, reusableSide);
                reusableSide.move(Direction.UP);
                upperSolid[index] = getCachedSolidBlock(world, reusableSide);
            }

            reusableSide.set(center).move(Direction.UP, 2);
            return isInTradingHall = VillagerTradingHallOptimization.isTradingCell(baseSolid, upperSolid, getCachedSolidBlock(world, reusableSide));
        }

        //trading hall check does not need to refresh, so return the saved value
        return isInTradingHall;
    }

    @Unique
    private RegistryKey<VillagerProfession> getCachedProfession() {
        RegistryEntry<VillagerProfession> current = villager.getVillagerData().profession();
        if (current != cachedProfessionEntry) {
            cachedProfessionEntry = current;
            cachedProfessionKey = current.getKey().get();
        }
        return cachedProfessionKey;
    }

    @Unique
    private boolean getCachedSolidBlock(World world, BlockPos pos) {
        long key = pos.asLong();
        if (cachedBlockPos.containsKey(key)) return cachedBlockPos.get(key);
        else {
            BlockState state = world.getBlockState(pos);
            boolean isSolid = hasSolidCollision(state);
            cachedBlockPos.put(key, isSolid);
            return isSolid;
        }
    }

    @Unique
    private int[] getCooldownList() {
        return switch (getOptimalizationType()) {
            case Minimal -> cooldownsMinimal;
            case Agressive -> cooldownsAgressive;
            case null, default -> cooldowns;
        };
    }

    @Unique
    private void disableTradingHallOptimization(RuntimeException e) {
        isInTradingHall = false;
        shouldRefreshTradingHall = true;
        lastStandingLocation = null;

        if (tradingHallOptimizationDisabledForSession) {
            return;
        }

        tradingHallOptimizationDisabledForSession = true;
        LOGGER.error("Disabled villager trading-hall optimization for this session after an unexpected exception. Set EnableVillagerTradingHallOptimization=false in lazy-ai.json to keep it off across restarts.", e);
    }
}
