package com.etheller.warsmash.viewer5.handlers.w3x.environment;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.BufferUtils;

/** Uploads tightly packed visibility rows, including map widths not divisible by four. */
final class FogTextureUpload {
    // LWJGL 2 requires capacity for a matrix even for scalar glGetInteger queries.
    private final IntBuffer previousAlignment = BufferUtils.newIntBuffer(16);

    void upload(GL30 gl, int width, int height, ByteBuffer pixels) {
        previousAlignment.clear();
        gl.glGetIntegerv(GL30.GL_UNPACK_ALIGNMENT, previousAlignment);
        gl.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, 1);
        try {
            gl.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_R8, width, height, 0,
                    GL30.GL_RED, GL30.GL_UNSIGNED_BYTE, pixels);
        }
        finally {
            gl.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, previousAlignment.get(0));
        }
    }
}
