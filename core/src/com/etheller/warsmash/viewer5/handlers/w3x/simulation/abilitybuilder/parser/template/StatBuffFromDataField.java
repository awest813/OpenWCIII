package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.template;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.types.impl.CAbilityTypeAbilityBuilderLevelData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;

public class StatBuffFromDataField {
	private StatBuffType type;
	private DataFieldLetter percentageBooleanField;
	private DataFieldLetter flatBooleanField;
	private DataFieldLetter dataField;
	private Boolean percentageOverride;
	private DataFieldLetter targetMeleeField;
	private DataFieldLetter targetRangeField;
	private boolean targetMeleeDefault;
	private boolean targetRangeDefault;
	
	private transient NonStackingStatBuff buff;
	private transient NonStackingStatBuff secondAtkBuff;
	
	public StatBuffFromDataField(StatBuffFromDataField statBuff) {
		this.type = statBuff.getType();
		this.percentageBooleanField = statBuff.getPercentageBooleanField();
		this.flatBooleanField = statBuff.getFlatBooleanField();
		this.dataField = statBuff.getDataField();
		this.percentageOverride = statBuff.isPercentageOverride();
		this.targetMeleeField = statBuff.getTargetMeleeField();
		this.targetRangeField = statBuff.getTargetRangeField();
		this.targetMeleeDefault = statBuff.targetMeleeDefault;
		this.targetRangeDefault = statBuff.targetRangeDefault;
	}
	public StatBuffType getType() {
		return type;
	}
	public void setType(StatBuffType type) {
		this.type = type;
	}
	public DataFieldLetter getPercentageBooleanField() {
		return percentageBooleanField;
	}
	public void setPercentageBooleanField(DataFieldLetter percentageBooleanField) {
		this.percentageBooleanField = percentageBooleanField;
	}
	public DataFieldLetter getFlatBooleanField() {
		return flatBooleanField;
	}
	public void setFlatBooleanField(DataFieldLetter flatBooleanField) {
		this.flatBooleanField = flatBooleanField;
	}
	public DataFieldLetter getDataField() {
		return dataField;
	}
	public void setDataField(DataFieldLetter dataField) {
		this.dataField = dataField;
	}
	public Boolean isPercentageOverride() {
		return percentageOverride;
	}
	public void setPercentageOverride(Boolean percentageOverride) {
		this.percentageOverride = percentageOverride;
	}
	public DataFieldLetter getTargetMeleeField() {
		return targetMeleeField;
	}
	public void setTargetMeleeField(DataFieldLetter targetMeleeField) {
		this.targetMeleeField = targetMeleeField;
	}
	public DataFieldLetter getTargetRangeField() {
		return targetRangeField;
	}
	public void setTargetRangeField(DataFieldLetter targetRangeField) {
		this.targetRangeField = targetRangeField;
	}
	public NonStackingStatBuff getBuff() {
		return buff;
	}
	public void setBuff(NonStackingStatBuff buff) {
		this.buff = buff;
	}
	public NonStackingStatBuff getSecondAtkBuff() {
		return secondAtkBuff;
	}
	public void setSecondAtkBuff(NonStackingStatBuff secondAtkBuff) {
		this.secondAtkBuff = secondAtkBuff;
	}
	
	public NonStackingStatBuffType convertToNonStackingType(CAbilityTypeAbilityBuilderLevelData abilityData) {
		boolean percentage = false;
		if (this.isPercentageOverride() != null) {
			percentage = this.isPercentageOverride();
		} else {
			if (this.getPercentageBooleanField() != null) {
				try {
					percentage = Integer.parseInt(abilityData.getData()
							.get(this.getPercentageBooleanField().getIndex())) == 1;
				} catch (NumberFormatException exc) {
					percentage = false;
				}
			} else if (this.getFlatBooleanField() != null) {
				try {
					percentage = !(Integer.parseInt(
							abilityData.getData().get(this.getFlatBooleanField().getIndex())) == 1);
				} catch (NumberFormatException exc) {
					percentage = true;
				}
			}
		}
		if (this.getType() == StatBuffType.ATK) {
			boolean targetMelee = this.targetMeleeDefault;
			boolean targetRange = this.targetRangeDefault;
			if (this.getTargetMeleeField() != null) {
				targetMelee = readTargetFlag(abilityData, this.getTargetMeleeField(), targetMelee);
			}
			if (this.getTargetRangeField() != null) {
				targetRange = readTargetFlag(abilityData, this.getTargetRangeField(), targetRange);
			}

			return this.getType().toAtkNonStackingStatBuffType(percentage, targetMelee, targetRange);
		} else {
			return this.getType().toNonStackingStatBuffType(percentage);
		}
	}

	private static boolean readTargetFlag(CAbilityTypeAbilityBuilderLevelData data, DataFieldLetter field,
			boolean legacyDefault) {
		final String value = data.getData().get(field.getIndex()).trim();
		// Original campaign data predates these configurable aura target flags.
		return value.isEmpty() || "-".equals(value) ? legacyDefault : Integer.parseInt(value) == 1;
	}
}
