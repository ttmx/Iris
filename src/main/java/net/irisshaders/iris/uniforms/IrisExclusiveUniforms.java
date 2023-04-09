package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformCreator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Objects;
import java.util.stream.StreamSupport;

public class IrisExclusiveUniforms {
	public static void addIrisExclusiveUniforms(UniformCreator uniforms) {
		WorldInfoUniforms.addWorldInfoUniforms(uniforms);

		//All Iris-exclusive uniforms (uniforms which do not exist in either OptiFine or ShadersMod) should be registered here.
		uniforms.registerFloatUniform(true, "thunderStrength", IrisExclusiveUniforms::getThunderStrength);
		uniforms.registerFloatUniform(true, "currentPlayerHealth", IrisExclusiveUniforms::getCurrentHealth);
		uniforms.registerFloatUniform(true, "maxPlayerHealth", IrisExclusiveUniforms::getMaxHealth);
		uniforms.registerFloatUniform(true, "currentPlayerHunger", IrisExclusiveUniforms::getCurrentHunger);
		uniforms.registerFloatUniform(true, "maxPlayerHunger", () -> 20);
		uniforms.registerFloatUniform(true, "currentPlayerAir", IrisExclusiveUniforms::getCurrentAir);
		uniforms.registerFloatUniform(true, "maxPlayerAir", IrisExclusiveUniforms::getMaxAir);
		uniforms.registerBooleanUniform(true, "firstPersonCamera", IrisExclusiveUniforms::isFirstPersonCamera);
		uniforms.registerBooleanUniform(true, "isSpectator", IrisExclusiveUniforms::isSpectator);
		uniforms.registerVector3Uniform(true, "eyePosition", IrisExclusiveUniforms::getEyePosition);
		uniforms.registerFloatUniform(true, "cloudTime", CapturedRenderingState.INSTANCE::getCloudTime);
		Vector4f zero = new Vector4f(0, 0, 0, 0);
		uniforms.registerVector4Uniform(true, "lightningBoltPosition", () -> {
			if (Minecraft.getInstance().level != null) {
				return StreamSupport.stream(Minecraft.getInstance().level.entitiesForRendering().spliterator(), false).filter(bolt -> bolt instanceof LightningBolt).findAny().map(bolt -> {
					Vector3f unshiftedCameraPosition = CameraUniforms.getUnshiftedCameraPosition();
					Vec3 vec3 = bolt.getPosition(Minecraft.getInstance().getDeltaFrameTime());
					return new Vector4f((float) (vec3.x - unshiftedCameraPosition.x), (float) (vec3.y - unshiftedCameraPosition.y), (float) (vec3.z - unshiftedCameraPosition.z), 1);
				}).orElse(zero);
			} else {
				return zero;
			}
		});
	}

	private static float getThunderStrength() {
		// Note: Ensure this is in the range of 0 to 1 - some custom servers send out of range values.
		return Math.clamp(0.0F, 1.0F,
			Minecraft.getInstance().level.getThunderLevel(CapturedRenderingState.INSTANCE.getTickDelta()));
	}

	private static float getCurrentHealth() {
		if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
			return -1;
		}

		return Minecraft.getInstance().player.getHealth() / Minecraft.getInstance().player.getMaxHealth();
	}

	private static float getCurrentHunger() {
		if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
			return -1;
		}

		return Minecraft.getInstance().player.getFoodData().getFoodLevel() / 20f;
	}

	private static float getCurrentAir() {
		if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
			return -1;
		}

		return (float) Minecraft.getInstance().player.getAirSupply() / (float) Minecraft.getInstance().player.getMaxAirSupply();
	}

	private static float getMaxAir() {
		if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
			return -1;
		}

		return Minecraft.getInstance().player.getMaxAirSupply();
	}

	private static float getMaxHealth() {
		if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
			return -1;
		}

		return Minecraft.getInstance().player.getMaxHealth();
	}

	private static boolean isFirstPersonCamera() {
		// If camera type is not explicitly third-person, assume it's first-person.
		switch (Minecraft.getInstance().options.getCameraType()) {
			case THIRD_PERSON_BACK:
			case THIRD_PERSON_FRONT:
				return false;
			default:
				return true;
		}
	}

	private static boolean isSpectator() {
		return Minecraft.getInstance().gameMode.getPlayerMode() == GameType.SPECTATOR;
	}

	private static Vector3f getEyePosition() {
		Objects.requireNonNull(Minecraft.getInstance().getCameraEntity());
		Vec3 pos = Minecraft.getInstance().getCameraEntity().getEyePosition(CapturedRenderingState.INSTANCE.getTickDelta());
		return new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);
	}

	public static class WorldInfoUniforms {
		public static void addWorldInfoUniforms(UniformCreator uniforms) {
			ClientLevel level = Minecraft.getInstance().level;
			// TODO: Use level.dimensionType() coordinates for 1.18!
			uniforms.registerIntegerUniform(true, "bedrockLevel", () -> {
				if (level != null) {
					return level.dimensionType().minY();
				} else {
					return 0;
				}
			});
			uniforms.registerIntegerUniform(true, "heightLimit", () -> {
				if (level != null) {
					return level.dimensionType().height();
				} else {
					return 256;
				}
			});
			uniforms.registerIntegerUniform(true, "logicalHeightLimit", () -> {
				if (level != null) {
					return level.dimensionType().logicalHeight();
				} else {
					return 256;
				}
			});
			uniforms.registerBooleanUniform(true, "hasCeiling", () -> {
				if (level != null) {
					return level.dimensionType().hasCeiling();
				} else {
					return false;
				}
			});
			uniforms.registerBooleanUniform(true, "hasSkylight", () -> {
				if (level != null) {
					return level.dimensionType().hasSkyLight();
				} else {
					return true;
				}
			});
			uniforms.registerFloatUniform(true, "ambientLight", () -> {
				if (level != null) {
					return level.dimensionType().ambientLight();
				} else {
					return 0f;
				}
			});

		}
	}
}
