package com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest;

/**
 * Defeat-condition companion to {@link CQuest}. State-only for scripting;
 * shown in the quest dialog when present.
 */
public class CDefeatCondition {
	private String description = "";

	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String description) {
		this.description = description != null ? description : "";
	}
}
