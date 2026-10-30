package dev.amethyst.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

/**
 * World-space drawing helpers for 1.21.11's lines pipeline. All coordinates
 * passed to these methods are absolute world coordinates; the utility
 * subtracts the camera position so callers never have to think about the
 * camera-relative frame the world renderer uses.
 */
public final class RenderUtil {
	/** Updated by the render dispatcher every frame before modules draw. */
	public static final class Frame {
		public MatrixStack matrices;
		public Vec3d cameraPos = Vec3d.ZERO;
		public float tickDelta;

		public boolean valid() {
			return matrices != null;
		}
	}

	public static final Frame FRAME = new Frame();

	private RenderUtil() {
	}

	private static VertexConsumerProvider.Immediate immediate() {
		return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
	}

	private static VertexConsumer lines() {
		return immediate().getBuffer(RenderLayers.LINES);
	}

	/** Flushes the Amethyst line buffer; called by the dispatcher after all modules drew. */
	public static void flush() {
		immediate().draw(RenderLayers.LINES);
	}

	/** Draws the outline of an axis-aligned box in world space. */
	public static void drawBox(Box box, int argb, float lineWidth) {
		if (!FRAME.valid()) {
			return;
		}
		MatrixStack.Entry entry = FRAME.matrices.peek();
		VertexRendering.drawOutline(FRAME.matrices, lines(),
			VoxelShapes.cuboid(box),
			box.minX - FRAME.cameraPos.x, box.minY - FRAME.cameraPos.y, box.minZ - FRAME.cameraPos.z,
			argb, lineWidth);
	}

	/** Draws the outline of a box specified by two world-space corners. */
	public static void drawBox(double minX, double minY, double minZ,
	                           double maxX, double maxY, double maxZ,
	                           int argb, float lineWidth) {
		drawBox(new Box(minX, minY, minZ, maxX, maxY, maxZ), argb, lineWidth);
	}

	/** Draws the 12 edges of a box using individual segments. */
	public static void drawBoxEdges(double minX, double minY, double minZ,
	                                double maxX, double maxY, double maxZ,
	                                int argb, float lineWidth) {
		// bottom
		line(minX, minY, minZ, maxX, minY, minZ, argb, lineWidth);
		line(maxX, minY, minZ, maxX, minY, maxZ, argb, lineWidth);
		line(maxX, minY, maxZ, minX, minY, maxZ, argb, lineWidth);
		line(minX, minY, maxZ, minX, minY, minZ, argb, lineWidth);
		// top
		line(minX, maxY, minZ, maxX, maxY, minZ, argb, lineWidth);
		line(maxX, maxY, minZ, maxX, maxY, maxZ, argb, lineWidth);
		line(maxX, maxY, maxZ, minX, maxY, maxZ, argb, lineWidth);
		line(minX, maxY, maxZ, minX, maxY, minZ, argb, lineWidth);
		// verticals
		line(minX, minY, minZ, minX, maxY, minZ, argb, lineWidth);
		line(maxX, minY, minZ, maxX, maxY, minZ, argb, lineWidth);
		line(maxX, minY, maxZ, maxX, maxY, maxZ, argb, lineWidth);
		line(minX, minY, maxZ, minX, maxY, maxZ, argb, lineWidth);
	}

	/** Draws a single world-space line segment. */
	public static void line(Vec3d from, Vec3d to, int argb, float lineWidth) {
		line(from.x, from.y, from.z, to.x, to.y, to.z, argb, lineWidth);
	}

	public static void line(double x1, double y1, double z1,
	                        double x2, double y2, double z2,
	                        int argb, float lineWidth) {
		if (!FRAME.valid()) {
			return;
		}
		MatrixStack.Entry entry = FRAME.matrices.peek();
		VertexConsumer consumer = lines();
		float dx = (float) (x2 - x1);
		float dy = (float) (y2 - y1);
		float dz = (float) (z2 - z1);
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float nx = 0f, ny = 1f, nz = 0f;
		if (length > 1.0e-4f) {
			nx = dx / length;
			ny = dy / length;
			nz = dz / length;
		}
		float x1r = (float) (x1 - FRAME.cameraPos.x);
		float y1r = (float) (y1 - FRAME.cameraPos.y);
		float z1r = (float) (z1 - FRAME.cameraPos.z);
		float x2r = (float) (x2 - FRAME.cameraPos.x);
		float y2r = (float) (y2 - FRAME.cameraPos.y);
		float z2r = (float) (z2 - FRAME.cameraPos.z);
		consumer.vertex(entry, x1r, y1r, z1r).color(argb).normal(entry, nx, ny, nz).lineWidth(lineWidth);
		consumer.vertex(entry, x2r, y2r, z2r).color(argb).normal(entry, nx, ny, nz).lineWidth(lineWidth);
	}

	/** Draws a polyline through world-space points. */
	public static void path(Iterable<Vec3d> points, int argb, float lineWidth) {
		Vec3d previous = null;
		for (Vec3d point : points) {
			if (previous != null) {
				line(previous, point, argb, lineWidth);
			}
			previous = point;
		}
	}

	/** Draws a flat horizontal ring in world space (used by JumpCircles / TargetEsp). */
	public static void ring(Vec3d center, double radius, int segments, int argb, float lineWidth) {
		Vec3d previous = null;
		for (int i = 0; i <= segments; i++) {
			double angle = (Math.PI * 2 * i) / segments;
			Vec3d point = new Vec3d(
				center.x + Math.cos(angle) * radius,
				center.y,
				center.z + Math.sin(angle) * radius);
			if (previous != null) {
				line(previous, point, argb, lineWidth);
			}
			previous = point;
		}
	}

	/** Draws a filled-looking translucent box by layering slightly offset outlines. */
	public static void glowBox(double minX, double minY, double minZ,
	                           double maxX, double maxY, double maxZ,
	                           int argb, int shells, float lineWidth) {
		double grow = 0.06;
		for (int i = 0; i < shells; i++) {
			double inset = grow * i;
			int color = dev.amethyst.client.util.ColorUtil.withAlpha(argb,
				Math.max(20, (dev.amethyst.client.util.ColorUtil.alpha(argb) * (shells - i)) / shells));
			drawBoxEdges(
				minX - inset, minY - inset, minZ - inset,
				maxX + inset, maxY + inset, maxZ + inset,
				color, lineWidth);
		}
	}
}
