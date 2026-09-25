package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.AbilityBuilderGsonBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class AbilityBuilderParserUtil {
	public static void loadAbilityBuilderFiles(final AbilityBuilderFileListener listener) {
		final Gson gson = AbilityBuilderGsonBuilder.create();
		try {
			final File abilityBehaviorsDir = findAbilityBehaviorsDirectory();
			final File[] abilityBehaviorFiles = abilityBehaviorsDir.listFiles();
			if (abilityBehaviorFiles != null) {
				Arrays.sort(abilityBehaviorFiles, Comparator.comparing(File::getName));
				for (final File abilityBehaviorFile : abilityBehaviorFiles) {
					loadAbilityBuilderFile(gson, abilityBehaviorFile, listener);
				}
			}
			else {
				throw new IllegalStateException("Ability Builder directory not found: " + abilityBehaviorsDir.getAbsolutePath());
			}
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private static File findAbilityBehaviorsDirectory() {
		// Launchers run in assets, while Gradle tests/audits run in core or repo root.
		for (final String path : new String[] { "abilityBehaviors", "assets/abilityBehaviors", "core/assets/abilityBehaviors" }) {
			final File directory = new File(path);
			if (directory.isDirectory()) {
				return directory;
			}
		}
		return new File("abilityBehaviors");
	}

	public static void loadAbilityBuilderFile(final Gson gson, final File abilityBehaviorFile,
			final AbilityBuilderFileListener listener) throws FileNotFoundException {
		if (abilityBehaviorFile.isDirectory()) {
			final File[] abilityBehaviorFiles = abilityBehaviorFile.listFiles();
			if (abilityBehaviorFiles != null) {
				Arrays.sort(abilityBehaviorFiles, Comparator.comparing(File::getName));
				for (final File subAbilityBehaviorFile : abilityBehaviorFiles) {
					loadAbilityBuilderFile(gson, subAbilityBehaviorFile, listener);
				}
			}
			return;
		}
		if (!abilityBehaviorFile.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(abilityBehaviorFile.toPath(), StandardCharsets.UTF_8)) {
			final AbilityBuilderFile behaviors = gson.fromJson(reader,
					AbilityBuilderFile.class);
			for (final AbilityBuilderParser behavior : behaviors.getAbilityList()) {
				listener.callback(behavior);
			}
		}
		catch (final JsonParseException e) {
			System.err.println("Failed to load Ability Builder config file: " + abilityBehaviorFile.getName());
			e.printStackTrace();
		}
		catch (final IllegalArgumentException e) {
			System.err.println("Failed to load Ability Builder config file: " + abilityBehaviorFile.getName());
			e.printStackTrace();
		}
		catch (final IOException e) {
			throw new IllegalStateException("Could not read Ability Builder config: " + abilityBehaviorFile, e);
		}
	}

	public static interface AbilityBuilderFileListener {
		void callback(AbilityBuilderParser behavior);
	}
}
