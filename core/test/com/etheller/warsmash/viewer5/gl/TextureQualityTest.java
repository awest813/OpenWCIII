package com.etheller.warsmash.viewer5.gl;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;

class TextureQualityTest {
	@Test void mipSelectionHandlesTinyAndNonSquareTextures() {
		assertEquals(2, TextureQuality.baseLevel(256, 128, 0));
		assertEquals(1, TextureQuality.baseLevel(256, 128, 1));
		assertEquals(0, TextureQuality.baseLevel(1, 1, 0));
		assertEquals(1, TextureQuality.baseLevel(2, 1, 0));
		assertEquals(0, TextureQuality.baseLevel(256, 128, 2));
	}

	@Test void qualitySwitchesGenerateOnceAndRestoreTheOriginalLevel() {
		final int previous = OptionsSettingsStore.get().getTextureQuality();
		final List<String> calls = new ArrayList<>();
		final GL20 gl = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class<?>[] { GL20.class },
				(proxy, method, args) -> {
					if (method.getName().equals("glGenerateMipmap")) calls.add("generate");
					else if (method.getName().equals("glTexParameteri")) {
						assertEquals(GL30.GL_TEXTURE_BASE_LEVEL, args[1]);
						calls.add("level " + args[2]);
					}
					else throw new AssertionError(method.getName());
					return null;
				});
		try {
			final TextureQuality quality = new TextureQuality(false);
			for (final int setting : new int[] { 0, 0, 1, 2, 0 }) {
				OptionsSettingsStore.get().setTextureQuality(setting);
				quality.apply(gl, GL20.GL_TEXTURE_2D, 256, 256);
			}
			assertEquals(Arrays.asList("level 0", "generate", "level 2", "level 1", "level 0", "level 2"), calls);
		}
		finally { OptionsSettingsStore.get().setTextureQuality(previous); }
	}
}
