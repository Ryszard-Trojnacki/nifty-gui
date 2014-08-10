package de.lessvoid.nifty.internal.render.batch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import de.lessvoid.nifty.api.NiftyColor;
import de.lessvoid.nifty.internal.math.Mat4;
import de.lessvoid.nifty.internal.math.Vec4;
import de.lessvoid.nifty.spi.NiftyRenderDevice;

/**
 * A ColorQuadBatch stores 2d quads with vertex color at each vertex.
 */
public class ColorQuadBatch implements Batch<Void> {
  private final static int NUM_PRIMITIVES = 100;
  public final static int PRIMITIVE_SIZE = 6 * 6;

  private final FloatBuffer b;

  // Vec4 buffer data
  private final Vec4 vsrc = new Vec4();
  private final Vec4 vdst = new Vec4();

  public ColorQuadBatch() {
    this.b = createBuffer(NUM_PRIMITIVES * PRIMITIVE_SIZE);
  }

  public boolean add(
      final double x0,
      final double y0,
      final double x1,
      final double y1,
      final NiftyColor c1,
      final NiftyColor c2,
      final NiftyColor c3,
      final NiftyColor c4,
      final Mat4 mat) {
    // first
    addTransformed(x0, y0, mat, c1);
    addTransformed(x0, y1, mat, c3);
    addTransformed(x1, y0, mat, c2);

    // second
    addTransformed(x0, y1, mat, c3);
    addTransformed(x1, y1, mat, c4);
    addTransformed(x1, y0, mat, c2);
    return true;
  }

  @Override
  public void render(final NiftyRenderDevice renderDevice) {
    renderDevice.renderColorQuads(b);
  }

  @Override
  public boolean requiresNewBatch(final Void param) {
    return (b.remaining() < PRIMITIVE_SIZE);
  }

  private void addTransformed(final double x, final double y, final Mat4 mat, final NiftyColor color) {
    vsrc.x = (float) x;
    vsrc.y = (float) y;
    vsrc.z = 0.0f;
    Mat4.transform(mat, vsrc, vdst);
    b.put(vdst.x);
    b.put(vdst.y);
    b.put((float) color.getRed());
    b.put((float) color.getGreen());
    b.put((float) color.getBlue());
    b.put((float) color.getAlpha());
  }

  private FloatBuffer createBuffer(final int size) {
    return ByteBuffer.allocateDirect(size << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
  }
}