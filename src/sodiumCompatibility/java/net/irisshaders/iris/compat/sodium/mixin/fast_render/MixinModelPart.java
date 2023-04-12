package net.irisshaders.iris.compat.sodium.mixin.fast_render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.client.render.RenderGlobal;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex;
import net.irisshaders.iris.vertices.ImmediateState;
import net.irisshaders.iris.vertices.NormalHelper;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Math;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public class MixinModelPart {
	private ModelCuboid[] sodium$cuboids;

	private static float rsqrt(float value) {
		if (value == 0.0f) {
			// You heard it here first, folks: 1 divided by 0 equals 1
			// In actuality, this is a workaround for normalizing a zero length vector (leaving it as zero length)
			return 1.0f;
		} else {
			return (float) (1.0 / Math.sqrt(value));
		}
	}

	private static boolean shouldExtend() {
		return IrisApi.getInstance().isShaderPackInUse() && ImmediateState.renderWithExtendedVertexFormat;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(List<ModelPart.Cube> cuboids, Map<String, ModelPart> children, CallbackInfo ci) {
		var copies = new ModelCuboid[cuboids.size()];

		for (int i = 0; i < cuboids.size(); i++) {
			var accessor = (ModelCuboidAccessor) cuboids.get(i);
			copies[i] = accessor.copy();
		}

		this.sodium$cuboids = copies;
	}

	private byte normalConvX, normalConvY, tangentX, tangentY;

	private void vec_to_oct(int value)
	{
		float valueX = Norm3b.unpackX(value);
		float valueY = Norm3b.unpackY(value);
		float valueZ = Norm3b.unpackZ(value);

		float invL1Norm = 1.0f / (Math.abs(valueX) + Math.abs(valueY) + Math.abs(valueZ));
		float resX = 0.0f, resY = 0.0f;
		if (valueZ < 0.0f) {
			resX = (1.0f - Math.abs(valueY * invL1Norm)) * Mth.sign(valueX);
			resY = (1.0f - Math.abs(valueX * invL1Norm)) * Mth.sign(valueY);
		} else {
			resX = valueX * invL1Norm;
			resY = valueY * invL1Norm;
		}

		normalConvX = floatToSnorm8(resX);
		normalConvY = floatToSnorm8(resY);
	}
	private static float bias = 1.0f / (2^(8 - 1) - 1);

	private static float unpackW(int norm) {
		return (float)((byte)(norm >> 24 & 255)) * 0.007874016F;
	}

	private static byte floatToSnorm8( float v )
	{
		//According to D3D10 rules, the value "-1.0f" has two representations:
		//  0x1000 and 0x10001
		//This allows everyone to convert by just multiplying by 32767 instead
		//of multiplying the negative values by 32768 and 32767 for positive.
		return (byte) Math.clamp( v >= 0.0f ?
				(v * 127.0f + 0.5f) :
				(v * 127.0f - 0.5f),
			-128.0f,
			127.0f );
	}


	private void vec_to_tangent(float valueX, float valueY, float valueZ, float valueW) {
		//encode to octahedron, result in range [-1, 1]
		float invL1Norm = 1.0f / (Math.abs(valueX) + Math.abs(valueY) + Math.abs(valueZ));
		float resX = 0.0f, resY = 0.0f;
		if (valueZ < 0.0f) {
			resX = (1.0f - Math.abs(valueY * invL1Norm)) * Mth.sign(valueX);
			resY = (1.0f - Math.abs(valueX * invL1Norm)) * Mth.sign(valueY);
		} else {
			resX = valueX * invL1Norm;
			resY = valueY * invL1Norm;
		}

		// map y to always be positive
		resY = resY * 0.5f + 0.5f;

		// add a bias so that y is never 0 (sign in the vertex shader)
		if (resY < bias)
			resY = bias;

		// Apply the sign of the binormal to y, which was computed elsewhere
		if (valueW < 0)
			resY *= -1;

		tangentX = floatToSnorm8(resX);
		tangentY = floatToSnorm8(resY);
	}

	/**
	 * @author JellySquid
	 * @reason Use optimized vertex writer, avoid allocations, use quick matrix transformations
	 */
	@Overwrite
	private void compile(PoseStack.Pose matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		var writer = VertexBufferWriter.of(vertexConsumer);
		int color = ColorABGR.pack(red, green, blue, alpha);
		byte overlayX = (byte) (overlay & 0xFFFF);
		byte overlayY = (byte) (overlay >> 16 & 0xFFFF);
		byte lightX = (byte) (light & 0xFFFF);
		byte lightY = (byte) (light >> 16 & 0xFFFF);
		boolean extend = shouldExtend();
		for (ModelCuboid cuboid : this.sodium$cuboids) {
			cuboid.updateVertices(matrices.pose());


			try (MemoryStack stack = RenderGlobal.VERTEX_DATA.push()) {
				long buffer = stack.nmalloc(4 * 6 * (extend ? EntityVertex.STRIDE : ModelVertex.STRIDE));
				long ptr = buffer;

				for (ModelCuboid.Quad quad : cuboid.quads) {
					if (quad == null) continue;
					var normal = quad.getNormal(matrices.normal());

					float midU = 0, midV = 0;

					if (extend) {
						for (int i = 0; i < 4; i++) {
							midU += quad.textures[i].x;
							midV += quad.textures[i].y;
						}

						midU *= 0.25;
						midV *= 0.25;

						getTangent(normal, quad.positions[0].x, quad.positions[0].y, quad.positions[0].z, quad.textures[0].x, quad.textures[0].y,
							quad.positions[1].x, quad.positions[1].y, quad.positions[1].z, quad.textures[1].x, quad.textures[1].y,
							quad.positions[2].x, quad.positions[2].y, quad.positions[2].z, quad.textures[2].x, quad.textures[2].y
						);
					}

					for (int i = 0; i < 4; i++) {
						var pos = quad.positions[i];
						var tex = quad.textures[i];

						if (extend) {
							EntityVertex.write(ptr, pos.x, pos.y, pos.z, color, tex.x, tex.y, midU, midV, overlayX, overlayY, lightX, lightY, normalConvX, normalConvY, tangentX, tangentY);
						} else {
							ModelVertex.write(ptr, pos.x, pos.y, pos.z, color, tex.x, tex.y, light, overlay, normal);
						}

						ptr += extend ? EntityVertex.STRIDE : ModelVertex.STRIDE;
					}
				}

				writer.push(stack, buffer, 4 * 6, extend ? EntityVertex.FORMAT : ModelVertex.FORMAT);
			}
		}
	}

	private void getTangent(int normal, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2) {
		// Capture all of the relevant vertex positions

		float normalX = Norm3b.unpackX(normal);
		float normalY = Norm3b.unpackY(normal);
		float normalZ = Norm3b.unpackZ(normal);

		float edge1x = x1 - x0;
		float edge1y = y1 - y0;
		float edge1z = z1 - z0;

		float edge2x = x2 - x0;
		float edge2y = y2 - y0;
		float edge2z = z2 - z0;

		float deltaU1 = u1 - u0;
		float deltaV1 = v1 - v0;
		float deltaU2 = u2 - u0;
		float deltaV2 = v2 - v0;

		float fdenom = deltaU1 * deltaV2 - deltaU2 * deltaV1;
		float f;

		if (fdenom == 0.0) {
			f = 1.0f;
		} else {
			f = 1.0f / fdenom;
		}

		float tangentx = f * (deltaV2 * edge1x - deltaV1 * edge2x);
		float tangenty = f * (deltaV2 * edge1y - deltaV1 * edge2y);
		float tangentz = f * (deltaV2 * edge1z - deltaV1 * edge2z);
		float tcoeff = rsqrt(tangentx * tangentx + tangenty * tangenty + tangentz * tangentz);
		tangentx *= tcoeff;
		tangenty *= tcoeff;
		tangentz *= tcoeff;

		float bitangentx = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
		float bitangenty = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
		float bitangentz = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
		float bitcoeff = rsqrt(bitangentx * bitangentx + bitangenty * bitangenty + bitangentz * bitangentz);
		bitangentx *= bitcoeff;
		bitangenty *= bitcoeff;
		bitangentz *= bitcoeff;

		// predicted bitangent = tangent × normal
		// Compute the determinant of the following matrix to get the cross product
		//  i  j  k
		// tx ty tz
		// nx ny nz

		// Be very careful when writing out complex multi-step calculations
		// such as vector cross products! The calculation for pbitangentz
		// used to be broken because it multiplied values in the wrong order.

		float pbitangentx = tangenty * normalZ - tangentz * normalY;
		float pbitangenty = tangentz * normalX - tangentx * normalZ;
		float pbitangentz = tangentx * normalY - tangenty * normalX;

		float dot = (bitangentx * pbitangentx) + (bitangenty * pbitangenty) + (bitangentz * pbitangentz);
		float tangentW;

		if (dot < 0) {
			tangentW = -1.0F;
		} else {
			tangentW = 1.0F;
		}

		vec_to_oct(normal);
		vec_to_tangent(tangentx, tangenty, tangentz, tangentW);
	}
}
