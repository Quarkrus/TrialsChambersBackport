package net.salju.trialstowers.events;

import net.minecraftforge.fml.LogicalSide;

import net.minecraft.client.Minecraft;


public class TrialsClientManager {
	@Nullable
	public static Player getPlayer(LogicalSide side) {
		if (side.isClient()) {
			return Minecraft.getInstance().player;
		} else {
			return null;
		}
	}

	public static void applyKnockback(Player player, double y) {
		player.setDeltaMovement(player.getDeltaMovement().x(), (y * 0.15), player.getDeltaMovement().z());
	}
}