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

	@Test
	public void quickPositionOnlyMovesCameraWhenSpaceIsPressed() {
		final GameCameraManager camera = createCameraManager();
		camera.setTarget(10, 20);
		camera.panToTimed(110, 120, 2);
		camera.setQuickPosition(500, 600);
		assertEquals(10, camera.target.x); assertEquals(20, camera.target.y);
		assertNotNull(camera.getPanDestination());
		camera.updateCamera(0.5f);
		assertEquals(35, camera.target.x); assertEquals(45, camera.target.y);
		org.junit.jupiter.api.Assertions.assertTrue(camera.keyDown(com.badlogic.gdx.Input.Keys.SPACE));
		assertEquals(500, camera.target.x); assertEquals(600, camera.target.y);
		assertNull(camera.getPanDestination());
	}

	@Test
	public void checkpointPreservesCameraBookmark() throws Exception {
		final GameCameraManager original = createCameraManager();
		original.setTarget(10, 20); original.setQuickPosition(500, 600);
		final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		original.writeCheckpoint(new java.io.DataOutputStream(bytes));
		final GameCameraManager resumed = createCameraManager();
		resumed.readCheckpoint(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
		assertEquals(10, resumed.target.x); assertEquals(20, resumed.target.y);
		resumed.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
		assertEquals(500, resumed.target.x); assertEquals(600, resumed.target.y);
	}

	@Test
	public void cinematicSetupHeightKeepsLowAngleCameraAboveTerrainAndResetRestoresPreset() {
		final GameCameraManager camera = createCameraManager();
		// Retail Human02Interlude's opening tower shot looks upward from ground level.
		final CustomCameraSetup setup = new CustomCameraSetup(4.8f, 90, 167.1f, 0, 3348.6f, 4000, 100, 377.4f);
		camera.applyCameraSetupForceDuration(setup, false, 0);
		camera.updateTargetZ(512); camera.updateCamera(0.05f);
		assertEquals(889.4f, camera.target.z, 0.001f);
		org.junit.jupiter.api.Assertions.assertTrue(camera.position.z > 512, "Camera eye must stay above the terrain");
		camera.resetToGameCamera(0); camera.updateTargetZ(512);
		assertEquals(512, camera.target.z, 0.001f);
	}

	@Test
	public void checkpointPreservesPanAndHeightInterpolationThroughArrival() throws Exception {
		final GameCameraManager original = createCameraManager();
		original.panToTimed(600, 300, 2);
		original.setTargetZOffset(150, 2);
		for (int i = 0; i < 12; i++) original.updateCamera();
		final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		original.writeCheckpoint(new java.io.DataOutputStream(bytes));
		final GameCameraManager resumed = createCameraManager();
		resumed.readCheckpoint(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
		assertNotNull(resumed.getPanDestination());
		for (int i = 0; i < 40; i++) {
			original.updateCamera(); resumed.updateCamera();
			assertEquals(original.target, resumed.target);
			assertEquals(original.getTargetZOffset(), resumed.getTargetZOffset());
		}
		assertNull(resumed.getPanDestination());
		assertEquals(150, resumed.getTargetZOffset(), 0.001);
	}

	@Test
	public void towerDescentInterpolatesHeightAndResumesWithoutDroppingBelowTerrain() throws Exception {
		final GameCameraManager original = createCameraManager();
		final CustomCameraSetup high = new CustomCameraSetup(51.7f, 70, 165.8f, 0, 3348.6f, 4000, 100, 3000);
		final CustomCameraSetup low = new CustomCameraSetup(4.8f, 90, 167.1f, 0, 3348.6f, 4000, 100, 377.4f);
		original.applyCameraSetupForceDuration(high, false, 0);
		original.applyCameraSetupForceDuration(low, false, 10);
		original.updateTargetZ(512); original.updateCamera(2);
		assertEquals(512 + 3000 - (3000 - 377.4f) * 0.2f, original.target.z, 0.001f);
		final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		original.writeCheckpoint(new java.io.DataOutputStream(bytes));
		final GameCameraManager resumed = createCameraManager();
		// The mission replay reconstructs the setup handle before presentation restoration.
		resumed.applyCameraSetupForceDuration(low, false, 0);
		resumed.readCheckpoint(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
		for (int i = 0; i < 10; i++) {
			original.updateTargetZ(512); resumed.updateTargetZ(512);
			original.updateCamera(1); resumed.updateCamera(1);
			assertEquals(original.target.z, resumed.target.z, 0.001f);
			assertEquals(original.position.z, resumed.position.z, 0.001f);
			org.junit.jupiter.api.Assertions.assertTrue(original.position.z > 512, "Descent must keep the eye above terrain");
		}
		assertEquals(889.4f, original.target.z, 0.001f);
	}

	@Test
	public void checkpointRejectsInvalidCameraCoordinates() throws Exception {
		final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		final java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
		out.writeInt(0); out.writeFloat(Float.NaN);
		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class, () -> createCameraManager()
				.readCheckpoint(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))));
	}

	@Test
	public void zeroDeltaAfterLoadingKeepsSavedPanAndHeightProgress() throws Exception {
		final GameCameraManager camera = createCameraManager();
		camera.panToTimed(100, 100, 2); camera.setTargetZOffset(150, 2);
		camera.updateCamera(0.25f);
		assertEquals(12.5f, camera.target.x); assertEquals(18.75f, camera.getTargetZOffset());
		camera.updateCamera(0);
		assertEquals(12.5f, camera.target.x); assertEquals(18.75f, camera.getTargetZOffset());
		assertNotNull(camera.getPanDestination());
		camera.updateCamera(0.25f);
		assertEquals(25f, camera.target.x); assertEquals(37.5f, camera.getTargetZOffset());
	}
}
