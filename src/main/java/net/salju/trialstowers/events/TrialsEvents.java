package net.salju.trialstowers.events;

import net.salju.trialstowers.network.ApplyKnockback;
import net.salju.trialstowers.item.MaceItem;
import net.salju.trialstowers.init.TrialsTags;
import net.salju.trialstowers.init.TrialsModSounds;
import net.salju.trialstowers.init.TrialsItems;
import net.salju.trialstowers.init.TrialsEnchantments;
import net.salju.trialstowers.init.TrialsEffects;
import net.salju.trialstowers.block.TrialSpawnerEntity;
import net.salju.trialstowers.TrialsMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.Containers;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
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
				if (target.getPersistentData().contains("FallDamageImmunity") && event.getSource().is(DamageTypes.FALL)) {
					if (target.getY() >= target.getPersistentData().getDouble("FallDamageImmunity")) {
						target.getPersistentData().remove("FallDamageImmunity");
						event.setAmount(0.0F);
					}
				}
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
			if (player.fallDistance > 1.0F) {
				int i = EnchantmentHelper.getItemEnchantmentLevel(TrialsEnchantments.DENSITY.get(), player.getMainHandItem());
				float f = (player.fallDistance - 1.0F);
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
			if (e > 0) {
				for (LivingEntity targets : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.76F))) {
					if (targets.hasLineOfSight(player) && targets.isAlive()) {
						targets.fallDistance = 0.0F;
						double d = player.distanceTo(targets) * 0.65;
						int h = Mth.nextInt(targets.level().getRandom(), 2, 3);
						if (targets == player) {
							d = 0.0;
							h = 3;
							if (targets.getPersistentData().contains("FallDamageImmunity") && targets.getPersistentData().getDouble("FallDamageImmunity") > targets.getY()) {
								targets.getPersistentData().remove("FallDamageImmunity");
								targets.getPersistentData().putDouble("FallDamageImmunity", targets.blockPosition().below().getY());
							} else if (!targets.getPersistentData().contains("FallDamageImmunity")) {
								targets.getPersistentData().putDouble("FallDamageImmunity", targets.blockPosition().below().getY());
							}
						}
						double y = ((double) Math.max(0.0, (h * e) - d));
						if (targets instanceof ServerPlayer ply) {
							TrialsMod.sendToClientPlayer(new ApplyKnockback(y), ply);
						} else if (targets.getDeltaMovement().y() <= 5.0) {
							targets.addDeltaMovement(new Vec3(targets.getDeltaMovement().x(), (y * 0.15), targets.getDeltaMovement().z()));
						}
					}
				}
				BlockPos top = BlockPos.containing((player.getX() + 4), (player.getY() + 2), (player.getZ() + 4));
				BlockPos bot = BlockPos.containing((player.getX() - 4), (player.getY() - 2), (player.getZ() - 4));
				for (BlockPos pos : BlockPos.betweenClosed(top, bot)) {
					BlockState state = lvl.getBlockState(pos);
					if (state.getBlock() instanceof LeverBlock blok) {
						blok.pull(state, player.level(), pos);
					} else if (state.getBlock() instanceof ButtonBlock blok) {
						blok.press(state, player.level(), pos);
					} else if (state.getBlock() instanceof AbstractCandleBlock blok) {
						blok.extinguish(null, state, lvl, pos);
					} else if (state.getBlock() instanceof TrapDoorBlock && state.getBlock() != Blocks.IRON_TRAPDOOR) {
						lvl.setBlock(pos, state.cycle(TrapDoorBlock.OPEN), 2);
						lvl.playSound(null, pos, state.getValue(TrapDoorBlock.OPEN) ? SoundEvents.WOODEN_TRAPDOOR_CLOSE : SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, lvl.getRandom().nextFloat() * 0.1F + 0.9F);
						lvl.gameEvent(null, state.getValue(TrapDoorBlock.OPEN) ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos);
					} else if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && state.getBlock() != Blocks.IRON_DOOR) {
						lvl.setBlock(pos, state.cycle(DoorBlock.OPEN), 10);
						lvl.playSound(null, pos, state.getValue(DoorBlock.OPEN) ? SoundEvents.WOODEN_DOOR_CLOSE : SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, lvl.getRandom().nextFloat() * 0.1F + 0.9F);
						lvl.gameEvent(null, state.getValue(DoorBlock.OPEN) ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos);
					}
				}
				lvl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 8, 1.5, 0.15, 1.5, 0);
				lvl.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 4, 1.8, 0.15, 1.8, 0);
				lvl.playSound(null, player.blockPosition(), TrialsModSounds.WIND_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			}
		}
	}

	@SubscribeEvent
	public static void onTrades(VillagerTradesEvent event) {
		final var middle = event.getTrades().get(3);
		if (event.getType() == VillagerProfession.CARTOGRAPHER) {
			middle.add(new TrialsVillagerManager.TreasureMapForEmeralds(14, TrialsTags.TRIALS_MAPS, "filled_map.trials.chambers", MapDecoration.Type.TARGET_X, 12, 10));
			event.getTrades().put(3, middle);
		}
	}

	@SubscribeEvent
	public static void onEffect(MobEffectEvent.Applicable event) {
		if (event.getEntity() instanceof Player && event.getEffectInstance().getEffect() == MobEffects.BAD_OMEN && event.getEffectInstance().getDuration() == 120000) {
			event.setResult(Result.DENY);
		}
	}

	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingTickEvent event) {
		if (event.getEntity().getPersistentData().contains("FallDamageImmunity") && event.getEntity().onGround()) {
			event.getEntity().getPersistentData().remove("FallDamageImmunity");
		}
		if (event.getEntity() instanceof Skeleton kevin && event.getEntity().getPersistentData().getInt("TrialSpawned") > 0 && !event.getEntity().getPersistentData().getBoolean("TrialFreezing")) {
			if (kevin.isShaking()) {
				event.getEntity().getPersistentData().putBoolean("TrialFreezing", true);
				TrialsMod.queueServerWork(150, () -> {
					if (kevin != null && kevin.isAlive()) {
						if (kevin.isShaking()) {
							Stray frosty = kevin.convertTo(EntityType.STRAY, true);
							if (!kevin.isSilent()) {
								kevin.level().levelEvent(null, 1048, kevin.blockPosition(), 0);
							}
							if (frosty != null) {
								ForgeEventFactory.onLivingConvert(kevin, frosty);
							}
						} else {
							event.getEntity().getPersistentData().remove("TrialFreezing");
						}
					}
				});
			}
		}
	}

	@SubscribeEvent
	public static void onGrief(EntityMobGriefingEvent event) {
		if (event.getEntity() != null) {
			if (event.getEntity() instanceof Silverfish && event.getEntity().getPersistentData().getInt("TrialSpawned") > 0) {
				event.setResult(Result.DENY);
			}
		}
	}

	@SubscribeEvent
	public static void onConvert(LivingConversionEvent.Post event) {
		if (event.getEntity() != null && event.getOutcome() != null) {
			if (event.getEntity().getPersistentData().getInt("TrialSpawned") > 0) {
				event.getOutcome().getPersistentData().putInt("TrialSpawned", event.getEntity().getPersistentData().getInt("TrialSpawned"));
			}
		}
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() != null) {
			LivingEntity target = event.getEntity();
			if (target.level() instanceof ServerLevel lvl) {
				if (target.getPersistentData().getInt("TrialSpawned") > 0) {
					TrialSpawnerEntity block = TrialsManager.getSpawner(target, target.blockPosition(), lvl, 64);
					if (block != null) {
						block.setRemainingEnemies(block.getRemainingEnemies() - 1);
					}
				}
				if (target instanceof Raider && target.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.WHITE_BANNER) {
					Containers.dropItemStack(target.level(), target.getX(), target.getY(), target.getZ(), new ItemStack(TrialsItems.TRIAL_BOTTLE.get()));
				}
				if (target.hasEffect(TrialsEffects.WINDED.get())) {
					int e = (target.getEffect(TrialsEffects.WINDED.get()).getAmplifier() + 2);
					for (LivingEntity targets : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5.76F))) {
						if (targets.hasLineOfSight(target) && targets.isAlive()) {
							targets.fallDistance = 0.0F;
							double d = target.distanceTo(targets) * 0.65;
							double y = ((double) Math.max(0.0, (Mth.nextInt(target.level().getRandom(), 2, 3) * e) - d));
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
						} else if (state.getBlock() instanceof AbstractCandleBlock blok) {
							blok.extinguish(null, state, lvl, pos);
						} else if (state.getBlock() instanceof TrapDoorBlock && state.getBlock() != Blocks.IRON_TRAPDOOR) {
							lvl.setBlock(pos, state.cycle(TrapDoorBlock.OPEN), 2);
							lvl.playSound(null, pos, state.getValue(TrapDoorBlock.OPEN) ? SoundEvents.WOODEN_TRAPDOOR_CLOSE : SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, lvl.getRandom().nextFloat() * 0.1F + 0.9F);
							lvl.gameEvent(null, state.getValue(TrapDoorBlock.OPEN) ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos);
						} else if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && state.getBlock() != Blocks.IRON_DOOR) {
							lvl.setBlock(pos, state.cycle(DoorBlock.OPEN), 10);
							lvl.playSound(null, pos, state.getValue(DoorBlock.OPEN) ? SoundEvents.WOODEN_DOOR_CLOSE : SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, lvl.getRandom().nextFloat() * 0.1F + 0.9F);
							lvl.gameEvent(null, state.getValue(DoorBlock.OPEN) ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos);
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