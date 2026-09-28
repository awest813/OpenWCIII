package com.etheller.warsmash.viewer5.handlers.w3x;

import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxComplexInstance;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;

/** Visual occlusion only: never changes pathing, fog, selection, or authored vertex colors. */
public final class TreeOcclusion {
	private final List<RenderUnit> visibleUnits = new ArrayList<>();
	private final BoundingBox bounds = new BoundingBox();
	private final Vector3 target = new Vector3();
	private final Vector3 hit = new Vector3();
	private final Ray ray = new Ray();
	private float elapsed = 1;

	public static boolean blocks(final Vector3 camera, final Vector3 target, final BoundingBox bounds,
			final Ray ray, final Vector3 hit) {
		ray.set(camera, hit.set(target).sub(camera).nor());
		return Intersector.intersectRayBounds(ray, bounds, hit)
				&& camera.dst2(hit) < camera.dst2(target) - 1;
	}

	public static float fade(final float current, final boolean occluded, final float dt) {
		final float target = occluded ? 0.35f : 1;
		final float step = Math.max(0, dt) * 4;
		return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
	}

	public void update(final War3MapViewer viewer, final float dt) {
		final boolean enabled = viewer.isOcclusionEnabled() && OptionsSettingsStore.get().isOcclusion();
		this.elapsed += Math.max(0, dt);
		final boolean sample = this.elapsed >= 1f / 15f;
		if (sample) {
			this.elapsed = 0;
			this.visibleUnits.clear();
			if (enabled) {
				for (final RenderUnit unit : viewer.units) {
					if (!unit.instance.hidden() && !unit.getSimulationUnit().isDead()
							&& unit.getSimulationUnit().isVisible(viewer.simulation, viewer.getLocalPlayerIndex())
							&& unit.instance.isVisible(viewer.worldScene.camera)) this.visibleUnits.add(unit);
				}
			}
		}
		for (final RenderWidget widget : viewer.widgets) {
			if (!(widget instanceof RenderDestructable)) continue;
			final RenderDestructable tree = (RenderDestructable) widget;
			final MdxComplexInstance instance = tree.getInstance();
			if (!enabled || tree.getSimulationDestructable().isDead()) instance.occluded = false;
			else if (sample) {
				instance.occluded = false;
				final float height = tree.getSimulationDestructable().getOccluderHeight();
				if (height > 0 && !instance.hidden() && instance.isVisible(viewer.worldScene.camera)
						&& instance.model.bounds.getBoundingBox() != null) {
					this.bounds.set(instance.model.bounds.getBoundingBox());
					this.bounds.max.z = Math.max(this.bounds.min.z, height);
					this.bounds.set(this.bounds.min, this.bounds.max).mul(instance.worldMatrix);
					for (final RenderUnit unit : this.visibleUnits) {
						this.target.set(unit.getX(), unit.getY(), unit.getZ()
								+ Math.max(16, unit.instance.model.bounds.z * Math.abs(unit.instance.localScale.z)));
						if (blocks(viewer.worldScene.camera.location, this.target, this.bounds, this.ray, this.hit)) {
							instance.occluded = true;
							break;
						}
					}
				}
			}
			instance.occlusionAlpha = fade(instance.occlusionAlpha, instance.occluded, dt);
		}
	}
}
