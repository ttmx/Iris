package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.uniform.UniformCreator;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Objects;

public final class WorldTimeUniforms {
	private WorldTimeUniforms() {
	}

	/**
	 * Makes world time uniforms available to the given program
	 *
	 * @param uniforms the program to make the uniforms available to
	 */
	public static void addWorldTimeUniforms(UniformCreator uniforms) {
		uniforms
			.registerIntegerUniform(true, "worldTime", WorldTimeUniforms::getWorldDayTime)
			.registerIntegerUniform(true, "worldDay", WorldTimeUniforms::getWorldDay)
			.registerIntegerUniform(true, "moonPhase", () -> getWorld().getMoonPhase());
	}

	static int getWorldDayTime() {
		long timeOfDay = getWorld().getDayTime();

		if (Iris.getCurrentDimension() == DimensionId.END || Iris.getCurrentDimension() == DimensionId.NETHER) {
			// If the dimension is the nether or the end, don't override the fixed time.
			// This was an oversight in versions before and including 1.2.5 causing inconsistencies, such as Complementary's ender beams not moving.
			return (int) (timeOfDay % 24000L);
		}

		long dayTime = getWorld().dimensionType().fixedTime()
			.orElse(timeOfDay % 24000L);

		return (int) dayTime;
	}

	private static int getWorldDay() {
		long timeOfDay = getWorld().getDayTime();
		long day = timeOfDay / 24000L;

		return (int) day;
	}

	private static ClientLevel getWorld() {
		return Objects.requireNonNull(Minecraft.getInstance().level);
	}
}
