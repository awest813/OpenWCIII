package com.etheller.warsmash.viewer5.handlers.w3x.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;

public final class GameCameraManager extends CameraManager {
	private static final float TWO_PI = (float) Math.PI * 2;
	private static final CameraRates INFINITE_CAMERA_RATES = new CameraRates(Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY);
	private final CameraPreset[] presets;
	private final CameraSetup[] presetCameras;
	private final CameraSetup[] presetCamerasInsert;
	private final CameraSetup[] presetCamerasDelete;
	private final CameraRates cameraRates;
	public final CameraPanControls cameraPanControls;
	private Rectangle cameraBounds;
	private int currentPreset = 0;
	private float fov;
	private float targetZOffset = 0.0f;
	private RenderUnit targetControllerUnit;
	private float targetControllerXOffset;
	private float targetControllerYOffset;
	private boolean targetControllerInheritOrientation;
	private CustomCameraSetup customSetup;
	private CameraRates customCameraRates;
	private Vector2 panDestination;
	private Vector2 panRate;
	private Vector2 quickPosition;
	private float setupHeight;
	private Float setupHeightDestination;
	private float setupHeightRate;
	private Float zOffsetDestination;
	private float zOffsetRate;
	private float targetNoiseMag;
	private float targetNoiseVel;
	private float sourceNoiseMag;
	private float sourceNoiseVel;
	private float noiseTime;
	private float noiseOffsetX;
	private float noiseOffsetY;

	public GameCameraManager(final CameraPreset[] presets, final CameraRates cameraRates) {
		this.presets = presets;
		this.setupHeight = presets[0].getHeight();
		this.cameraRates = new CameraRates(cameraRates.aoa, cameraRates.fov, cameraRates.rotation * 3,
				cameraRates.distance, cameraRates.forward, cameraRates.strafe);
		this.cameraPanControls = new CameraPanControls();

		this.presetCameras = new CameraSetup[presets.length];
		this.presetCamerasInsert = new CameraSetup[presets.length];
		this.presetCamerasDelete = new CameraSetup[presets.length];
		for (int i = 0; i < presets.length; i++) {
			this.presetCameras[i] = getSetup(presets[i], false, false);
			this.presetCamerasInsert[i] = getSetup(presets[i], true, false);
			this.presetCamerasDelete[i] = getSetup(presets[i], false, true);
		}
	}

	private CameraSetup getSetup(final CameraPreset preset, final boolean insert, final boolean delete) {
		return new CameraSetup(preset.getAoa(), preset.getFov(), preset.getRotation(insert, delete), 0,
				Math.max(1200, preset.getDistance()), preset.getFarZ(), preset.getNearZ(), preset.getHeight());
	}

	public void setCameraBounds(final Rectangle cameraBounds) {
		this.cameraBounds = cameraBounds;
	}

	@Override
	public void updateCamera() { updateCamera(Gdx.graphics.getDeltaTime()); }

	public void updateCamera(final float deltaTime) {
		if (this.setupHeightDestination != null) {
			final float previousHeight = this.setupHeight;
			this.setupHeight = applyAtRate(this.setupHeight, this.setupHeightDestination, this.setupHeightRate, deltaTime);
			this.target.z += this.setupHeight - previousHeight;
			if (Math.abs(this.setupHeight - this.setupHeightDestination) <= 0.01f) {
				this.target.z += this.setupHeightDestination - this.setupHeight;
				this.setupHeight = this.setupHeightDestination;
				this.setupHeightDestination = null;
			}
		}
		final CameraSetup setup = getCurrentSetup();
		final CameraRates cameraRate = getCurrentRates();
		updateCamera(setup, cameraRate, deltaTime);
		if (this.panDestination != null) {
			this.target.x = applyAtRate(this.target.x, this.panDestination.x, this.panRate.x, deltaTime);
			this.target.y = applyAtRate(this.target.y, this.panDestination.y, this.panRate.y, deltaTime);
			if (Math.abs(this.target.x - this.panDestination.x) <= 1.0f
					&& Math.abs(this.target.y - this.panDestination.y) <= 1.0f) {
				this.target.x = this.panDestination.x;
				this.target.y = this.panDestination.y;
				this.panDestination = null;
				this.panRate = null;
			}
		}
		if (this.zOffsetDestination != null) {
			this.targetZOffset = applyAtRate(this.targetZOffset, this.zOffsetDestination, this.zOffsetRate, deltaTime);
			if (Math.abs(this.targetZOffset - this.zOffsetDestination) <= 0.01f) {
				this.targetZOffset = this.zOffsetDestination;
				this.zOffsetDestination = null;
			}
		}
		updateNoise(deltaTime);
	}

	private void updateNoise(final float dt) {
		final float mag = Math.max(this.targetNoiseMag, this.sourceNoiseMag);
		final float vel = Math.max(this.targetNoiseVel, this.sourceNoiseVel);
		if (mag <= 0f) {
			this.noiseOffsetX = 0f;
			this.noiseOffsetY = 0f;
			return;
		}
		this.noiseTime += dt * Math.max(0.1f, vel);
		this.noiseOffsetX = (float) (Math.sin(this.noiseTime * 7.1) * mag);
		this.noiseOffsetY = (float) (Math.cos(this.noiseTime * 5.3) * mag);
		this.target.x += this.noiseOffsetX * dt;
		this.target.y += this.noiseOffsetY * dt;
	}

	public void setTargetNoise(final float magnitude, final float velocity) {
		this.targetNoiseMag = Math.max(0f, magnitude);
		this.targetNoiseVel = Math.max(0f, velocity);
		if (magnitude <= 0f) {
			this.noiseOffsetX = 0f;
			this.noiseOffsetY = 0f;
		}
	}

	public void setSourceNoise(final float magnitude, final float velocity) {
		this.sourceNoiseMag = Math.max(0f, magnitude);
		this.sourceNoiseVel = Math.max(0f, velocity);
		if (magnitude <= 0f) {
			this.noiseOffsetX = 0f;
			this.noiseOffsetY = 0f;
		}
	}

	public void stopCamera() {
		clearPan();
		this.setupHeightDestination = null;
		this.zOffsetDestination = null;
		this.customCameraRates = null;
		setTargetNoise(0f, 0f);
		setSourceNoise(0f, 0f);
		setTargetController(null, 0f, 0f, false);
	}

	private CameraRates getCurrentRates() {
		if (this.customCameraRates != null) {
			return this.customCameraRates;
		}
		return this.cameraRates;
	}

	private CameraSetup getCurrentSetup() {
		CameraSetup setup;
		if (this.customSetup != null) {
			setup = this.customSetup;
		}
		else if (this.cameraPanControls.insertDown && !this.cameraPanControls.deleteDown) {
			setup = this.presetCamerasInsert[this.currentPreset];
		}
		else if (!this.cameraPanControls.insertDown && this.cameraPanControls.deleteDown) {
			setup = this.presetCamerasDelete[this.currentPreset];
		}
		else {
			setup = this.presetCameras[this.currentPreset];
		}
		return setup;
	}

	private void updateCamera(final CameraSetup cameraPreset, final CameraRates cameraRate) { updateCamera(cameraPreset, cameraRate, Gdx.graphics.getDeltaTime()); }

	private void updateCamera(final CameraSetup cameraPreset, final CameraRates cameraRate, final float deltaTime) {
		this.quatHeap2.idt();
		this.quatHeap.idt();
		final float newHorizontalAngle;
		if (this.targetControllerInheritOrientation && (this.targetControllerUnit != null)) {
			newHorizontalAngle = this.targetControllerUnit.getFacing();
		}
		else {
			newHorizontalAngle = (float) Math.toRadians(cameraPreset.getRotation() - 90);
		}
		this.horizontalAngle = applyAtRateAngle(this.horizontalAngle, newHorizontalAngle,
				(float) Math.toRadians(cameraRate.rotation), deltaTime);
		this.quatHeap.setFromAxisRad(0, 0, 1, this.horizontalAngle);
		this.distance = applyAtRate(this.distance, cameraPreset.getDistance(), cameraRate.distance, deltaTime);
		this.verticalAngle = applyAtRateAngle(this.verticalAngle, (float) Math.toRadians(cameraPreset.getAoa() - 270),
				(float) Math.toRadians(cameraRate.aoa), deltaTime);
		this.quatHeap2.setFromAxisRad(1, 0, 0, this.verticalAngle);
		this.quatHeap.mul(this.quatHeap2);

		this.position.set(0, 0, 1);
		this.quatHeap.transform(this.position);
		this.position.nor();
		this.position.scl(this.distance);
		this.position = this.position.add(this.target);
		this.fov = applyAtRate(this.fov, (float) Math.toRadians(cameraPreset.getFov() / 2),
				(float) Math.toRadians(cameraRate.fov), deltaTime);
		if (this.camera != null) {
			this.camera.perspective(this.fov, this.camera.getAspect(), cameraPreset.getNearZ(), cameraPreset.getFarZ());
			this.camera.moveToAndFace(this.position, this.target, this.worldUp);
		}
	}

	public static float applyAtRate(final float oldValue, final float newValue, final float rate) { return applyAtRate(oldValue, newValue, rate, Gdx.graphics.getDeltaTime()); }

	public static float applyAtRate(final float oldValue, final float newValue, float rate, final float deltaTime) {
		if (rate == Float.POSITIVE_INFINITY) return newValue;
		rate *= deltaTime;
		final float deltaDistance = newValue - oldValue;
		if (Math.abs(deltaDistance) < rate) {
			return newValue;
		}
		else {
			return oldValue + (Math.signum(deltaDistance) * rate);
		}
	}

	public static float applyAtRateAngle(final float oldValue, final float newValue, final float rate) { return applyAtRateAngle(oldValue, newValue, rate, Gdx.graphics.getDeltaTime()); }

	public static float applyAtRateAngle(final float oldValue, final float newValue, float rate, final float deltaTime) {
		if (rate == Float.POSITIVE_INFINITY) return newValue;
		rate *= deltaTime;
		final float deltaDistance = newValue - oldValue;
		final float absDistance = Math.abs(deltaDistance);
		if ((absDistance <= rate) || ((TWO_PI - absDistance) <= rate)) {
			return newValue;
		}
		else {
			float signum = Math.signum(deltaDistance);
			if (absDistance > Math.PI) {
				signum *= -1;
			}
			return (oldValue + (signum * rate)) % TWO_PI;
		}
	}

	public void resize(final Rectangle viewport) {
		this.camera.viewport(viewport);
	}

	public void applyVelocity(final float deltaTime, boolean up, boolean down, boolean left, boolean right) {
		if (this.targetControllerUnit != null) {
			this.target.x = this.targetControllerUnit.getX() + this.targetControllerXOffset;
			this.target.y = this.targetControllerUnit.getY() + this.targetControllerYOffset;
		}
		else {
			final float velocityX;
			final float velocityY;
			up |= this.cameraPanControls.up;
			down |= this.cameraPanControls.down;
			left |= this.cameraPanControls.left;
			right |= this.cameraPanControls.right;
			if (up) {
				if (down) {
					velocityY = 0;
				}
				else {
					velocityY = this.cameraRates.forward;
					clearPan();
				}
			}
			else if (down) {
				velocityY = -this.cameraRates.forward;
				clearPan();
			}
			else {
				velocityY = 0;
			}
			if (right) {
				if (left) {
					velocityX = 0;
				}
				else {
					velocityX = this.cameraRates.strafe;
					clearPan();
				}
			}
			else if (left) {
				velocityX = -this.cameraRates.strafe;
				clearPan();
			}
			else {
				velocityX = 0;
			}
			this.target.add(velocityX * deltaTime, velocityY * deltaTime, 0);
		}
		if (this.cameraBounds != null) {
			if (this.target.x < this.cameraBounds.x) {
				this.target.x = this.cameraBounds.x;
			}
			if (this.target.y < this.cameraBounds.y) {
				this.target.y = this.cameraBounds.y;
			}
			if (this.target.x > (this.cameraBounds.x + this.cameraBounds.width)) {
				this.target.x = this.cameraBounds.x + this.cameraBounds.width;
			}
			if (this.target.y > (this.cameraBounds.y + this.cameraBounds.height)) {
				this.target.y = this.cameraBounds.y + this.cameraBounds.height;
			}
		}

	}

	public void clearPan() {
		this.panDestination = null;
		this.panRate = null;
		this.zOffsetDestination = null;
	}

	public Vector2 getPanDestination() {
		return this.panDestination;
	}

	public void updateTargetZ(final float groundHeight) {
		this.target.z = groundHeight + this.setupHeight + this.targetZOffset;
	}

	/** Script bookmark for Space; setting it must not move or interrupt the live camera. */
	public void setQuickPosition(final float x, final float y) {
		this.quickPosition = new Vector2(x, y);
	}

	public void scrolled(final int amount) {
		this.currentPreset -= amount;
		if (this.currentPreset < 0) {
			this.currentPreset = 0;
		}
		if (this.currentPreset >= this.presets.length) {
			this.currentPreset = this.presets.length - 1;
		}
		clearCustomSetup();
	}

	public boolean keyDown(final int keycode) {
		if (keycode == Input.Keys.SPACE && this.quickPosition != null) {
			setTarget(this.quickPosition.x, this.quickPosition.y);
			return true;
		}
		else if (keycode == Input.Keys.LEFT) {
			this.cameraPanControls.left = true;
			return true;
		}
		else if (keycode == Input.Keys.RIGHT) {
			this.cameraPanControls.right = true;
			return true;
		}
		else if (keycode == Input.Keys.DOWN) {
			this.cameraPanControls.down = true;
			return true;
		}
		else if (keycode == Input.Keys.UP) {
			this.cameraPanControls.up = true;
			return true;
		}
		else if (keycode == Input.Keys.INSERT) {
			this.cameraPanControls.insertDown = true;
			clearCustomSetup();
			return true;
		}
		else if (keycode == Input.Keys.FORWARD_DEL) {
			this.cameraPanControls.deleteDown = true;
			clearCustomSetup();
			return true;
		}
		return false;
	}

	public boolean keyUp(final int keycode) {
		if (keycode == Input.Keys.LEFT) {
			this.cameraPanControls.left = false;
			return true;
		}
		else if (keycode == Input.Keys.RIGHT) {
			this.cameraPanControls.right = false;
			return true;
		}
		else if (keycode == Input.Keys.DOWN) {
			this.cameraPanControls.down = false;
			return true;
		}
		else if (keycode == Input.Keys.UP) {
			this.cameraPanControls.up = false;
			return true;
		}
		else if (keycode == Input.Keys.INSERT) {
			this.cameraPanControls.insertDown = false;
			clearCustomSetup();
			return true;
		}
		else if (keycode == Input.Keys.FORWARD_DEL) {
			this.cameraPanControls.deleteDown = false;
			clearCustomSetup();
			return true;
		}
		return false;
	}

	private void clearCustomSetup() {
		clearCustomSetup(0);
	}

	private void clearCustomSetup(final float heightDuration) {
		this.customSetup = null;
		this.customCameraRates = null;
		clearPan();
		setSetupHeight(getCurrentSetup().getHeight(), heightDuration);
	}

	private void setSetupHeight(final float height, final float duration) {
		if (duration > 0) {
			this.setupHeightDestination = height;
			this.setupHeightRate = Math.abs(height - this.setupHeight) / duration;
		}
		else {
			this.target.z += height - this.setupHeight;
			this.setupHeight = height;
			this.setupHeightDestination = null;
		}
	}

	public Rectangle getCameraBounds() {
		return this.cameraBounds;
	}

	public void setTargetController(final RenderUnit targetControllerUnit, final float xoffset, final float yoffset,
			final boolean inheritOrientation) {
		this.targetControllerUnit = targetControllerUnit;
		this.targetControllerXOffset = xoffset;
		this.targetControllerYOffset = yoffset;
		this.targetControllerInheritOrientation = inheritOrientation;

	}

	public RenderUnit getTargetControllerUnit() {
		return this.targetControllerUnit;
	}

	public void setTargetZOffset(final float targetZOffset) {
		this.zOffsetDestination = null;
		this.targetZOffset = targetZOffset;
	}

	public float getTargetZOffset() {
		return this.targetZOffset;
	}

	public void setTargetZOffset(float targetZOffset, float duration) {
		final float rate = Math.abs((targetZOffset - this.targetZOffset) / duration);
		this.zOffsetDestination = targetZOffset;
		this.zOffsetRate = rate;
	}

	public void applyCameraSetupForceDuration(final CustomCameraSetup cameraSetup, final boolean doPan,
			final float forceDuration) {
		final CameraSetup previousSetup = getCurrentSetup();
		this.customSetup = cameraSetup;
		setSetupHeight(cameraSetup.getHeight(), forceDuration);
		if (forceDuration > 0) {
			final float aoaRate = (cameraSetup.getAoa() - previousSetup.getAoa()) / forceDuration;
			final float fovRate = (cameraSetup.getFov() - previousSetup.getFov()) / forceDuration;
			final float rotationRate = (cameraSetup.getRotation() - previousSetup.getRotation()) / forceDuration;
			final float distanceRate = (cameraSetup.getDistance() - previousSetup.getDistance()) / forceDuration;
			this.customCameraRates = new CameraRates(Math.abs(aoaRate), Math.abs(fovRate), Math.abs(rotationRate),
					Math.abs(distanceRate), this.cameraRates.forward, this.cameraRates.strafe);
			if (doPan) {
				panToTimed(cameraSetup.getDestPositionX(), cameraSetup.getDestPositionY(), forceDuration);
			}
		}
		else {
			updateCamera(this.customSetup, CameraRates.INFINITY);
			if (doPan) {
				setTarget(cameraSetup.getDestPositionX(), cameraSetup.getDestPositionY());
			}
		}
	}

	public void applyCameraSetup(CustomCameraSetup cameraSetup, boolean doPan, boolean panTimed) {
		this.customSetup = cameraSetup;
		setSetupHeight(cameraSetup.getHeight(), 0);
		if (doPan) {
			if (panTimed) {
				panTo(cameraSetup.getDestPositionX(), cameraSetup.getDestPositionY());
			}
			else {
				setTarget(cameraSetup.getDestPositionX(), cameraSetup.getDestPositionY());
			}
		}
	}

	public void resetToGameCamera(float duration) {
		final CameraSetup previousSetup = getCurrentSetup();
		clearCustomSetup(duration);
		if (duration == 0) {
			this.customCameraRates = new CameraRates(9999, 9999, 9999, 9999, this.cameraRates.forward,
					this.cameraRates.strafe);
		}
		else {
			final CameraSetup cameraSetup = getCurrentSetup();
			final float aoaRate = (cameraSetup.getAoa() - previousSetup.getAoa()) / duration;
			final float fovRate = (cameraSetup.getFov() - previousSetup.getFov()) / duration;
			float rotationDistance = Math.abs(cameraSetup.getRotation() - previousSetup.getRotation());
			rotationDistance = Math.min(rotationDistance, TWO_PI - rotationDistance);
			final float rotationRate = rotationDistance / duration;
			final float distanceRate = (cameraSetup.getDistance() - previousSetup.getDistance()) / duration;
			this.customCameraRates = new CameraRates(Math.abs(aoaRate), Math.abs(fovRate), Math.abs(rotationRate),
					Math.abs(distanceRate), this.cameraRates.forward, this.cameraRates.strafe);
		}
	}

	public void panToTimed(float x, float y, float duration) {
		if (duration == 0) {
			setTarget(x, y);
		}
		else {
			this.panDestination = new Vector2(x, y);
			final float yRate = (y - this.target.y) / duration;
			final float xRate = (x - this.target.x) / duration;
			this.panRate = new Vector2(Math.abs(xRate), Math.abs(yRate));
		}
	}

	public void setTarget(float x, float y) {
		clearPan();
		this.target.x = x;
		this.target.y = y;
	}

	public void panTo(float x, float y) {
		clearPan();
		this.panDestination = new Vector2(x, y);
		this.panRate = new Vector2(this.cameraRates.strafe, this.cameraRates.forward);
	}

	/** Frame-clock camera progress cannot be reconstructed by accelerated simulation ticks. */
	public void writeCheckpoint(final java.io.DataOutputStream out) throws java.io.IOException {
		out.writeInt(this.currentPreset);
		for (final float value : new float[] { this.target.x, this.target.y, this.target.z, this.horizontalAngle,
				this.verticalAngle, this.distance, this.fov, this.targetZOffset, this.zOffsetRate,
				this.targetNoiseMag, this.targetNoiseVel, this.sourceNoiseMag, this.sourceNoiseVel,
				this.noiseTime, this.noiseOffsetX, this.noiseOffsetY }) out.writeFloat(value);
		out.writeBoolean(this.panDestination != null);
		if (this.panDestination != null) {
			out.writeFloat(this.panDestination.x); out.writeFloat(this.panDestination.y);
			out.writeFloat(this.panRate.x); out.writeFloat(this.panRate.y);
		}
		out.writeBoolean(this.zOffsetDestination != null);
		if (this.zOffsetDestination != null) out.writeFloat(this.zOffsetDestination);
		out.writeBoolean(this.customCameraRates != null);
		if (this.customCameraRates != null) for (final float value : new float[] { this.customCameraRates.aoa,
				this.customCameraRates.fov, this.customCameraRates.rotation, this.customCameraRates.distance,
				this.customCameraRates.forward, this.customCameraRates.strafe }) out.writeFloat(value);
		out.writeBoolean(this.quickPosition != null);
		if (this.quickPosition != null) { out.writeFloat(this.quickPosition.x); out.writeFloat(this.quickPosition.y); }
		out.writeFloat(this.setupHeight); out.writeFloat(this.setupHeightRate);
		out.writeBoolean(this.setupHeightDestination != null);
		if (this.setupHeightDestination != null) out.writeFloat(this.setupHeightDestination);
		// Setup and controller handles are reconstructed by their original script calls.
	}

	public void readCheckpoint(final java.io.DataInputStream in) throws java.io.IOException {
		final int preset = in.readInt();
		if (preset < 0 || preset >= this.presets.length) throw new java.io.IOException("Invalid saved camera preset");
		this.currentPreset = preset;
		this.target.set(readFinite(in), readFinite(in), readFinite(in));
		this.horizontalAngle = readFinite(in); this.verticalAngle = readFinite(in);
		this.distance = readFinite(in); this.fov = readFinite(in); this.targetZOffset = readFinite(in);
		this.zOffsetRate = readFinite(in); this.targetNoiseMag = readFinite(in); this.targetNoiseVel = readFinite(in);
		this.sourceNoiseMag = readFinite(in); this.sourceNoiseVel = readFinite(in);
		this.noiseTime = readFinite(in); this.noiseOffsetX = readFinite(in); this.noiseOffsetY = readFinite(in);
		this.panDestination = null; this.panRate = null;
		if (in.readBoolean()) {
			this.panDestination = new Vector2(readFinite(in), readFinite(in));
			this.panRate = new Vector2(readFinite(in), readFinite(in));
		}
		this.zOffsetDestination = in.readBoolean() ? readFinite(in) : null;
		this.customCameraRates = in.readBoolean() ? new CameraRates(readRate(in), readRate(in), readRate(in),
				readRate(in), readRate(in), readRate(in)) : null;
		this.quickPosition = in.readBoolean() ? new Vector2(readFinite(in), readFinite(in)) : null;
		this.setupHeight = readFinite(in); this.setupHeightRate = readRate(in);
		this.setupHeightDestination = in.readBoolean() ? readFinite(in) : null;
	}

	private static float readFinite(final java.io.DataInputStream in) throws java.io.IOException {
		final float value = in.readFloat();
		if (!Float.isFinite(value)) throw new java.io.IOException("Invalid saved camera value");
		return value;
	}
	private static float readRate(final java.io.DataInputStream in) throws java.io.IOException {
		final float value = in.readFloat();
		if (Float.isNaN(value) || value < 0) throw new java.io.IOException("Invalid saved camera rate");
		return value;
	}
}
