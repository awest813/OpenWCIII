package com.etheller.warsmash.viewer5.handlers.w3x.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

public class GameCameraManagerTest {

	private static final float DELTA_TIME = 0.05f;

	@BeforeAll
	public static void setUpGraphics() {
		if (Gdx.graphics == null) {
			Gdx.graphics = (Graphics) Proxy.newProxyInstance(
					Graphics.class.getClassLoader(),
					new Class<?>[] { Graphics.class },
					(proxy, method, args) -> {
						if ("getDeltaTime".equals(method.getName())) return DELTA_TIME;
						if ("getWidth".equals(method.getName())) return 1920;
						if ("getHeight".equals(method.getName())) return 1080;
						return null;
					});
		}
	}

	private GameCameraManager createCameraManager() {
		final CameraPreset[] presets = new CameraPreset[] {
				new CameraPreset(304, 70, 90, 90, 90, 1650, 5000, 100, 0, 0, 0)
		};
		final CameraRates rates = new CameraRates(100, 100, 100, 100, 2000, 2000);
		return new GameCameraManager(presets, rates);
	}

	@Test
	public void testPanClearsUponArrival() {
		final GameCameraManager camera = createCameraManager();
		camera.target.x = 0;
		camera.target.y = 0;

		// Pan to (100, 100) over 0.1s
		camera.panToTimed(100f, 100f, 0.1f);
		assertNotNull(camera.getPanDestination(), "panDestination should be active while panning");

		// Simulate frames until destination reached
		for (int i = 0; i < 10; i++) {
			camera.updateCamera();
		}

		assertEquals(100f, camera.target.x, 1.0f);
		assertEquals(100f, camera.target.y, 1.0f);
		assertNull(camera.getPanDestination(), "panDestination must be cleared upon arrival to prevent lock");
	}

	@Test
	public void testSetTargetClearsPan() {
		final GameCameraManager camera = createCameraManager();
		camera.target.x = 0;
		camera.target.y = 0;
		camera.panToTimed(500f, 500f, 5.0f);
		assertNotNull(camera.getPanDestination());

		// User or script manually sets target position
		camera.setTarget(250f, 250f);
		assertNull(camera.getPanDestination(), "setTarget must immediately clear panDestination");
		assertEquals(250f, camera.target.x, 0.01f);
		assertEquals(250f, camera.target.y, 0.01f);
	}

	@Test
	public void testUserVelocityClearsPan() {
		final GameCameraManager camera = createCameraManager();
		camera.target.x = 0;
		camera.target.y = 0;
		camera.panToTimed(1000f, 1000f, 10.0f);
		assertNotNull(camera.getPanDestination());

		// User moves camera via keyboard arrow or edge pan
		camera.applyVelocity(DELTA_TIME, true, false, false, false);
		assertNull(camera.getPanDestination(), "Moving camera via user input must clear panDestination");
	}
}
