package net.coderbot.iris.mixin.devenvironment;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.coderbot.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses Minecraft's authentication check in development environments. It's unnecessary log spam, and there's no
 * need to send off a network request to Microsoft telling them that we're using Fabric/Quilt every time we launch the
 * game in the development environment.
 *
 * <p>This also disables telemetry as a side-effect.</p>
 */
@Mixin(QuickPlay.class)
public class MixinQuickPlay {
	@Inject(method = "joinSingleplayerWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;<init>()V", ordinal = 0), cancellable = true)
	private static void iris$createQuickplayWorld(Minecraft pMinecraft0, String pString1, CallbackInfo ci) {
		ci.cancel();
		pMinecraft0.createWorldOpenFlows().createFreshLevel(pString1, new LevelSettings(pString1, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), new WorldDataConfiguration(DataPackConfig.DEFAULT, FeatureFlagSet.of(FeatureFlags.VANILLA))),
			WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions);
	}
}
