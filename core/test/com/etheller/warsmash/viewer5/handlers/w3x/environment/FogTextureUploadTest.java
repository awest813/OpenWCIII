package com.etheller.warsmash.viewer5.handlers.w3x.environment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.graphics.GL30;

class FogTextureUploadTest {
    @Test void oddWidthVisibilityRowsReachTheGpuWithoutPaddingOrStateLeaks() {
        final int width = 129, height = 3;
        ByteBuffer source = ByteBuffer.allocateDirect(width * height);
        byte[] expected = new byte[width * height];
        for (int i = 0; i < expected.length; i++) expected[i] = (byte) (i * 17);
        source.put(expected).flip();
        byte[] uploaded = new byte[expected.length];
        int[] alignment = { 8 };
        GL30 gl = (GL30) Proxy.newProxyInstance(GL30.class.getClassLoader(), new Class<?>[] { GL30.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "glGetIntegerv":
                        assertTrue(((IntBuffer) args[1]).remaining() >= 16, "LWJGL 2 query buffer requirement");
                        ((IntBuffer) args[1]).put(0, alignment[0]);
                        break;
                    case "glPixelStorei": alignment[0] = (Integer) args[1]; break;
                    case "glTexImage2D":
                        ByteBuffer data = (ByteBuffer) args[8];
                        int stride = ((width + alignment[0] - 1) / alignment[0]) * alignment[0];
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) uploaded[y * width + x] = data.get(y * stride + x);
                        }
                        break;
                    default: throw new AssertionError(method.getName());
                    }
                    return null;
                });
        new FogTextureUpload().upload(gl, width, height, source);
        assertArrayEquals(expected, uploaded, "Visibility must stay on its original map row");
        assertEquals(8, alignment[0], "Other texture uploads retain their unpack state");
        assertEquals(0, source.position());
    }
}
