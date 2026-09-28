package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.MpqDataSourceDescriptor;
import com.etheller.warsmash.testutil.RetailTestData;

class RetailVideoOptionsTest {
	@Test void retailVideoControlsAndHelpAreAvailable() throws Exception {
		final List<DataSource> sources = new ArrayList<>();
		for (final String path : RetailTestData.requireAll()) sources.add(new MpqDataSourceDescriptor(path).createDataSource());
		final DataSource source = new CompoundDataSource(sources);
		try {
			try (InputStream input = source.getResourceAsStream("UI\\FrameDef\\Glue\\OptionsMenu.fdf")) {
				final String fdf = new String(input.readAllBytes(), StandardCharsets.UTF_8);
				for (final String control : new String[] { "ModelDetailMenu", "AnimQualityMenu", "TextureQualityMenu",
						"ShadowsMenu", "OcclusionMenu", "GammaSlider" }) assertTrue(fdf.contains(control), control);
			}
			for (final String file : new String[] { "UI\\WorldEditStrings.txt", "UI\\TriggerStrings.txt", "UI\\War3Strings.txt", "UI\\FrameDef\\GlobalStrings.fdf" }) {
				if (!source.has(file)) continue;
				try (InputStream input = source.getResourceAsStream(file)) {
					final String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
					for (final String line : text.split("\n")) {
						if (line.toLowerCase(java.util.Locale.ROOT).contains("occlusion")) System.out.println(file + ": " + line);
					}
				}
			}
		}
		finally { source.close(); }
	}
}
