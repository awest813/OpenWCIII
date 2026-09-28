package com.etheller.warsmash.viewer5;

import com.badlogic.gdx.graphics.Color;
import com.etheller.warsmash.units.Element;

public class FogSettings {
	public FogStyle style = FogStyle.NONE;
	public Color color = Color.BLACK;
	public float density;
	public float start;
	public float end;

	public void setStyleByIndex(final int styleValue) {
		this.style = ((styleValue >= 0) && (styleValue < FogStyle.values().length)) ? FogStyle.values()[styleValue]
				: FogStyle.NONE;
	}

	@Override
	public String toString() {
		return "FogSettings [style=" + this.style + ", color=" + this.color + ", density=" + this.density + ", start="
				+ this.start + ", end=" + this.end + "]";
	}

	public static FogSettings parse(final Element zFogElement, final int index) {
		final FogSettings newFogSettings = new FogSettings();
		final int styleValue = zFogElement.getFieldAsInteger("Style", fieldIndex(zFogElement, "Style", index)) + 1;
		newFogSettings.setStyleByIndex(styleValue);
		newFogSettings.start = zFogElement.getFieldAsFloat("Start", fieldIndex(zFogElement, "Start", index));
		newFogSettings.end = zFogElement.getFieldAsFloat("End", fieldIndex(zFogElement, "End", index));
		newFogSettings.density = zFogElement.getFieldAsFloat("Density", fieldIndex(zFogElement, "Density", index));
		// DefaultZFog has one record even in TFT. MenuZFog can have a record
		// for each game version; an explicitly authored zero is still a value.
		final int colorIndex = hasIndex(zFogElement, "Color", index * 4 + 3) ? index * 4 : 0;
		final float a = zFogElement.getFieldAsFloat("Color", colorIndex) / 255f;
		final float r = zFogElement.getFieldAsFloat("Color", 1 + colorIndex) / 255f;
		final float g = zFogElement.getFieldAsFloat("Color", 2 + colorIndex) / 255f;
		final float b = zFogElement.getFieldAsFloat("Color", 3 + colorIndex) / 255f;
		newFogSettings.color = new Color(r, g, b, a);
		return newFogSettings;
	}

	private static int fieldIndex(final Element element, final String field, final int index) {
		return hasIndex(element, field, index) ? index : 0;
	}

	private static boolean hasIndex(final Element element, final String field, final int index) {
		final java.util.List<String> values = element.getFieldAsList(field);
		return values != null && index >= 0 && index < values.size() && !values.get(index).isEmpty();
	}
}
