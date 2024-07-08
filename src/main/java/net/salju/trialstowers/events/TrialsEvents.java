package net.salju.trialstowers.events;

import net.salju.trialstowers.network.ApplyKnockback;
import net.salju.trialstowers.item.MaceItem;
import net.salju.trialstowers.init.TrialsModSounds;
import net.salju.trialstowers.init.TrialsItems;
import net.salju.trialstowers.init.TrialsEnchantments;
import net.salju.trialstowers.init.TrialsEffects;
import net.salju.trialstowers.block.TrialSpawnerEntity;
import net.salju.trialstowers.TrialsMod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Containers;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

@Mod.EventBusSubscriber
public class TrialsEvents {
	@SubscribeEvent
	public static void onHurt(LivingHurtEvent event) {
		if (event.getEntity() != null) {
			LivingEntity target = event.getEntity();
			if (target.level() instanceof ServerLevel lvl) {
				if (target.hasEffect(TrialsEffects.INFESTED.get()) && Mth.nextInt(target.level().getRandom(), 1, 10) == 10) {
					int e = (Mth.nextInt(target.level().getRandom(), 1, 3) * (target.getEffect(TrialsEffects.INFESTED.get()).getAmplifier() + 1));
					for (int i = 0; i != e; ++i) {
						Silverfish fish = EntityType.SILVERFISH.create(lvl);
						fish.moveTo(Vec3.atBottomCenterOf(target.blockPosition()));
						lvl.addFreshEntity(fish);
					}
				}
				if (event.getSource().getEntity() != null && event.getSource().getEntity() instanceof LivingEntity meanie) {
					if (meanie.hasEffect(TrialsEffects.WEAVE.get()) && target.level().isEmptyBlock(target.blockPosition()) && Mth.nextInt(target.level().getRandom(), 1, 10) == 10) {
						target.level().setBlock(target.blockPosition(), Blocks.COBWEB.defaultBlockState(), 3);
					}
				}
			}
			if (event.getSource().getDirectEntity() != null && event.getSource().getDirectEntity() instanceof LivingEntity meanie) {
				if (meanie.getMainHandItem().getItem() instanceof MaceItem mace) {
					mace.setMaceDamage(event.getAmount());
				}
			}
		}
	}

	@SubscribeEvent
	public static void onDamage(LivingDamageEvent event) {
		if (event.getEntity() != null) {
			LivingEntity target = event.getEntity();
			if (event.getSource().getDirectEntity() != null && event.getSource().getDirectEntity() instanceof LivingEntity meanie) {
				if (meanie.getMainHandItem().getItem() instanceof MaceItem mace) {
					int i = EnchantmentHelper.getItemEnchantmentLevel(TrialsEnchantments.BREACH.get(), meanie.getMainHandItem());
					if (i > 0) {
						event.setAmount(mace.getMaceDamage(event.getSource(), event.getEntity(), i));
					}
					mace.setMaceDamage(0.0F);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onCritical(CriticalHitEvent event) {
		Player player = event.getEntity();
		Entity target = event.getTarget();
		if (event.isVanillaCritical() && player.getMainHandItem().getItem() instanceof MaceItem && player.level() instanceof ServerLevel lvl) {
			int e = EnchantmentHelper.getItemEnchantmentLevel(TrialsEnchantments.WIND.get(), player.getMainHandItem());
			if (e > 0) {
				for (LivingEntity targets : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5.76F))) {
					if (targets.hasLineOfSight(target)) {
						targets.fallDistance = 0.0F;
						double d = target.distanceTo(targets) * 0.65;
						if (targets == player) {
							d = 0;
						}
						double y = (((double) Mth.nextInt(targets.level().getRandom(), 2, 3) * e) - d);
						if (targets instanceof ServerPlayer ply) {
							TrialsMod.sendToClientPlayer(new ApplyKnockback(y), ply);
						} else if (targets.getDeltaMovement().y() <= 5.0) {
							targets.addDeltaMovement(new Vec3(targets.getDeltaMovement().x(), (y * 0.15), targets.getDeltaMovement().z()));
						}
					}
				}
				BlockPos top = BlockPos.containing((target.getX() + 4), (target.getY() + 2), (target.getZ() + 4));
				BlockPos bot = BlockPos.containing((target.getX() - 4), (target.getY() - 2), (target.getZ() - 4));
				for (BlockPos pos : BlockPos.betweenClosed(top, bot)) {
					BlockState state = lvl.getBlockState(pos);
					if (state.getBlock() instanceof LeverBlock blok) {
						blok.pull(state, target.level(), pos);
					} else if (state.getBlock() instanceof ButtonBlock blok) {
						blok.press(state, target.level(), pos);
					}
				}
				lvl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 8, 1.5, 0.15, 1.5, 0);
				lvl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 4, 1.8, 0.15, 1.8, 0);
				lvl.playSound(null, target.blockPosition(), TrialsModSounds.WIND_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			}
			if (player.fallDistance > 3.0F) {
				int i = EnchantmentHelper.getItemEnchantmentLevel(TrialsEnchantments.DENSITY.get(), player.getMainHandItem());
				float f = (player.fallDistance - 3.0F);
				if (i > 0) {
					f = (f * (i + 1.0F));
				}
				if (target.onGround()) {
					if (player.fallDistance > 10.0F) {
						lvl.playSound(null, target.blockPosition(), TrialsModSounds.MACE_SMASH_GROUND_HEAVY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
					} else {
						lvl.playSound(null, target.blockPosition(), TrialsModSounds.MACE_SMASH_GROUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
					}
				} else {
					lvl.playSound(null, target.blockPosition(), TrialsModSounds.MACE_SMASH_AIR.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
				}
				player.fallDistance = 0.0F;
				event.setDamageModifier(event.getDamageModifier() + (0.1F * f));
			}
		}
	}

	@SubscribeEvent
	public static void onEffect(MobEffectEvent.Applicable event) {
		if (event.getEntity() instanceof Player && event.getEffectInstance().getEffect() == MobEffects.BAD_OMEN && event.getEffectInstance().getDuration() == 120000) {
			event.setResult(Result.DENY);
		}
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() != null) {
			LivingEntity target = event.getEntity();
			if (target.level() instanceof ServerLevel lvl) {
				TrialSpawnerEntity block = TrialsManager.getSpawner(target, target.blockPosition(), lvl, 64);
				if (block != null) {
					block.setRemainingEnemies(block.getRemainingEnemies() - 1);
				}
				if (target instanceof Raider && target.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.WHITE_BANNER) {
					Containers.dropItemStack(target.level(), target.getX(), target.getY(), target.getZ(), new ItemStack(TrialsItems.TRIAL_BOTTLE.get()));
				}
				if (target.hasEffect(TrialsEffects.WINDED.get())) {
					int e = (target.getEffect(TrialsEffects.WINDED.get()).getAmplifier() + 2);
					for (LivingEntity targets : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5.76F))) {
						if (targets.hasLineOfSight(target)) {
							targets.fallDistance = 0.0F;
							double d = target.distanceTo(targets) * 0.65;
							double y = (((double) Mth.nextInt(targets.level().getRandom(), 2, 3) * e) - d);
							if (targets instanceof ServerPlayer ply) {
								TrialsMod.sendToClientPlayer(new ApplyKnockback(y), ply);
							} else if (targets.getDeltaMovement().y() <= 5.0) {
								targets.addDeltaMovement(new Vec3(targets.getDeltaMovement().x(), (y * 0.15), targets.getDeltaMovement().z()));
							}
						}
					}
					BlockPos top = BlockPos.containing((target.getX() + 4), (target.getY() + 2), (target.getZ() + 4));
					BlockPos bot = BlockPos.containing((target.getX() - 4), (target.getY() - 2), (target.getZ() - 4));
					for (BlockPos pos : BlockPos.betweenClosed(top, bot)) {
						BlockState state = lvl.getBlockState(pos);
						if (state.getBlock() instanceof LeverBlock blok) {
							blok.pull(state, target.level(), pos);
						} else if (state.getBlock() instanceof ButtonBlock blok) {
							blok.press(state, target.level(), pos);
						}
					}
					lvl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 8, 1.5, 0.15, 1.5, 0);
					lvl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 4, 1.8, 0.15, 1.8, 0);
					lvl.playSound(null, target.blockPosition(), TrialsModSounds.WIND_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
				}
				if (target.hasEffect(TrialsEffects.OOZE.get())) {
					int e = (2 * (target.getEffect(TrialsEffects.OOZE.get()).getAmplifier() + 1));
					for (int i = 0; i != e; ++i) {
						Slime slim = EntityType.SLIME.create(lvl);
						slim.setSize(2, true);
						slim.moveTo(Vec3.atBottomCenterOf(target.blockPosition()));
						lvl.addFreshEntity(slim);
					}
				}
			}
		}
	}
}