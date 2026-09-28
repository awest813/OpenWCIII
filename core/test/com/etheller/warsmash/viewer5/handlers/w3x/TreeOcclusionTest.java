package com.etheller.warsmash.viewer5.handlers.w3x;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

class TreeOcclusionTest {
	@Test void onlyTreesBetweenCameraAndUnitBlockTheView() {
		final BoundingBox tree = new BoundingBox(new Vector3(-1, 4, -1), new Vector3(1, 6, 1));
		final Vector3 camera = new Vector3();
		final Ray ray = new Ray();
		final Vector3 hit = new Vector3();
		assertTrue(TreeOcclusion.blocks(camera, new Vector3(0, 10, 0), tree, ray, hit));
		assertFalse(TreeOcclusion.blocks(camera, new Vector3(0, 2, 0), tree, ray, hit));
		assertFalse(TreeOcclusion.blocks(camera, new Vector3(0, -10, 0), tree, ray, hit));
		assertFalse(TreeOcclusion.blocks(camera, new Vector3(10, 10, 0), tree, ray, hit));
	}

	@Test void fadingIsSmoothAndReversibleWithoutOvershooting() {
		assertEquals(0.8f, TreeOcclusion.fade(1, true, 0.05f), 0.00001f);
		assertEquals(0.35f, TreeOcclusion.fade(1, true, 1));
		assertEquals(1, TreeOcclusion.fade(0.35f, false, 1));
		assertEquals(0.35f, TreeOcclusion.fade(0.35f, true, 10));
	}
}
