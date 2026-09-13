package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderFile;
import com.google.gson.Gson;

class AbilityBuilderGsonBuilderTest {

	/**
	 * Gson builds an adapter for every field type it can reach, including the
	 * runtime simulation objects held by ability actions and callbacks. On Java 9+
	 * that walk fails as soon as it reaches a JDK type the module system does not
	 * open, which used to make every abilityBehaviors config file fail to parse.
	 * Runtime-only fields must stay transient so the walk stops at the config data.
	 */
	@Test
	void buildsAdapterForAbilityConfigFiles() {
		final Gson gson = AbilityBuilderGsonBuilder.create();
		assertNotNull(gson.getAdapter(AbilityBuilderFile.class));
	}
}
