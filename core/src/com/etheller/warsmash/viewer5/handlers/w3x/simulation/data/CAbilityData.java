package com.etheller.warsmash.viewer5.handlers.w3x.simulation.data;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityUltravision;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.paladin.CAbilityHolyLight;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityAreaDispel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityDeathExplosion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityHardenedSkin;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityMineTrigger;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPolymorph;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityLightningShield;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityDevour;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityBerserk;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityFlare;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityFragmentationShards;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityPurge;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityWitchDoctorWard;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityCreateCorpse;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityDiseaseCloud;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityNecromancerEnchantment;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilitySpikedBarricades;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.paladin.CAbilityDivineShield;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.paladin.CAbilityResurrect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.archmage.CAbilitySummonWaterElemental;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.archmage.CAbilityBlizzard;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.archmage.CAbilityBrilliance;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.archmage.CAbilityMassTeleport;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.mountainking.CAbilityThunderClap;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.mountainking.CAbilityAvatar;

import java.util.HashMap;
import java.util.Map;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.units.ObjectData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityGenericDoNothing;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityDropInstant;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat.CAbilitySearingArrows;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat.CAbilityOrbOfAnnihilation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat.CAbilityLightningAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat.CAbilityBlackArrow;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemChestOfGold;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemBundleOfLumber;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemGlyph;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemPlaceMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemRecall;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemWandOfManaStealing;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityRuneOfRebirth;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityDropCorpses;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityExhumeCorpses;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityGetCorpse;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityMeatWagonCargo;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityCloudOfFog;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityTreeOfLifeUpgradeEffects;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilitySleepAlways;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityDoom;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityParasite;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityHowlOfTerror;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityBansheePossession;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityAbsorbMana;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityFrostAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilitySpiderAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CAbilityFeedback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemRetrain;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemSoulTrap;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemSpeed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemTinyBuilding;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilitySoulPossession;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemTownPortal;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemExperienceGain;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemAntiMagicPotion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemFigurineSummon;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemOrb;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemHeal;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemRestore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemAreaArmor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemLifeRegeneration;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemLevelGain;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemManaBonus;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemManaRegain;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemPermanentLifeGain;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityEntangledMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.neutral.CAbilityWayGate;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.CAbilityShadowMeld;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.CAbilitySentinel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.eattree.CAbilityEatTree;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.moonwell.CAbilityMoonWell;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.root.CAbilityEntangleGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.bloodmage.phoenix.CAbilitySummonPhoenix;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.bloodmage.CAbilitySiphonMana;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.mountainking.CAbilityThunderBolt;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.beastmaster.CAbilitySummonGrizzly;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.beastmaster.CAbilitySummonHawk;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.beastmaster.CAbilitySummonQuilbeast;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityDarkConversion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityFingerOfDeath;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityRainOfChaos;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityDarkPortal;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.blight.CAbilityBlightPlacement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityBattleStations;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityChaosCargoLoad;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityCargoHoldDeath;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityChainDispel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityNightElfChannel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityAbolishMagic;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityDismount;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityManaFlare;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityPhaseShift;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityControlMagic;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityFlakCannons;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityBarrage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilitySpellSteal;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityBreathOfFire;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityCleavingAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityDrunkenHaze;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilitySoulPreservation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityStormEarthAndFire;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityTaunt;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityWarClub;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityCyclone;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityFaerieFire;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityAntiMagicShell;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityResearchIndicator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityTornado;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityTornadoBuildingDamage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityTornadoSpin;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityWander;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shaman.CAbilityBloodlust;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shaman.CAbilityFrenzy;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.darkranger.CAbilityCharm;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.seawitch.CAbilityForkedLightning;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.sappers.CAbilityKaboom;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.tinker.CAbilityClusterRockets;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.tinker.CAbilityFactory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.tinker.CAbilityPocketFactory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.demonhunter.CAbilityManaBurn;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.keeper.CAbilityEntanglingRoots;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.keeper.CAbilityForceOfNature;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.moonpriestess.CAbilitySummonOwlScout;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityEnsnare;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityPillage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityLiquidFire;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityPulverize;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityWeb;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityUnsummon;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.warden.CAbilityBlink;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.warden.CAbilityFanOfKnives;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.blademaster.CAbilityWhirlWind;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.farseer.CAbilityChainLightning;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.blademaster.CAbilityMirrorImage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CAbilityAttackPoison;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.deathknight.CAbilityAnimateDead;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.dreadlord.CAbilityInferno;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CAbilityPermanentImmolation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.farseer.CAbilityEarthquake;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.farseer.CAbilityFarSight;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shadowhunter.CAbilityHealingWave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.farseer.CAbilityFeralSpirit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.taurenchieftain.CAbilityWarStomp;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.deathknight.CAbilityDarkRitual;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.deathknight.CAbilityDeathCoil;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.deathknight.CAbilityDeathPact;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich.CAbilityDeathAndDecay;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich.CAbilityFrostArmor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich.CAbilityFrostNova;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.CAbilityType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.CAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionAcolyteHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionBlight;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionBlightedGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionCargoHold;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionCargoHoldBurrow;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionCargoHoldEntangledMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionDevourCargo;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionCarrionSwarmDummy;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionChannelTest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionColdArrows;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionDrop;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionGoldMineOverlayed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionHarvestLumber;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionHumanRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionImmolation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionInventory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionInvulnerable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionItemAttackBonus;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionItemDefenseBonus;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionItemHeal;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionItemLifeBonus;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionItemManaRegain;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionItemPermanentStatGain;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionItemStatBonus;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionLoad;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionNeutralBuilding;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionPhoenixFire;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionRally;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionReturnResources;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionRoot;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionShopPurchaseItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionShopSharing;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionStandDown;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.CAbilityTypeDefinitionWispHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.jass.CAbilityTypeJassDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderConfiguration;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderDupe;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderParserUtil;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.types.definitions.impl.CAbilityTypeDefinitionAbilityTemplateBuilder;

public class CAbilityData {

	private final ObjectData abilityData;
	private Map<War3ID, CAbilityType<?>> aliasToAbilityType = new HashMap<>();
	private final Map<War3ID, CAbilityTypeDefinition> codeToAbilityTypeDefinition = new HashMap<>();

	public CAbilityData(final ObjectData abilityData) {
		this.abilityData = abilityData;
		this.aliasToAbilityType = new HashMap<>();
		registerCodes();
	}

	private void registerCodes() {
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANto"),
				new CAbilityTypeDefinitionSpellBase(CAbilityTornado::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Atdg"),
				new CAbilityTypeDefinitionSpellBase(CAbilityTornadoBuildingDamage::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Atsp"),
				new CAbilityTypeDefinitionSpellBase(CAbilityTornadoSpin::new));
		this.codeToAbilityTypeDefinition.put(CAbilityWander.TORNADO_CODE,
				new CAbilityTypeDefinitionSpellBase((handle, alias) -> new CAbilityWander(handle, alias, CAbilityWander.TORNADO_CODE)));
		this.codeToAbilityTypeDefinition.put(CAbilityWander.CRITTER_CODE,
				new CAbilityTypeDefinitionSpellBase((handle, alias) -> new CAbilityWander(handle, alias, CAbilityWander.CRITTER_CODE)));
		// Native implementations are fallbacks. Ability Builder/JASS registrations
		// below may override them, but campaigns must work without optional scripts.
		final var attackPoison = new CAbilityTypeDefinitionSpellBase(CAbilityAttackPoison::new);
		for (final String code : new String[] { "Aspo", "Apoi", "Apo2", "Aven" }) {
			this.codeToAbilityTypeDefinition.put(War3ID.fromString(code), attackPoison);
		}
		// Night Elf research-enabled sight:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Ault"),
				new CAbilityTypeDefinitionSpellBase(CAbilityUltravision::new));
		final War3ID moonGlaive = War3ID.fromString("Amgl");
		final War3ID reinforcedDefenses = War3ID.fromString("Arbr");
		final War3ID corrosiveBreath = War3ID.fromString("Acor");
		final War3ID stormHammers = War3ID.fromString("Asth");
		final War3ID flyingMachineBombs = War3ID.fromString("Agyb");
		this.codeToAbilityTypeDefinition.put(moonGlaive, new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityResearchIndicator(handleId, alias, moonGlaive, War3ID.fromString("Remg"))));
		this.codeToAbilityTypeDefinition.put(reinforcedDefenses, new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityResearchIndicator(handleId, alias, reinforcedDefenses, War3ID.fromString("Rorb"))));
		this.codeToAbilityTypeDefinition.put(corrosiveBreath, new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityResearchIndicator(handleId, alias, corrosiveBreath, War3ID.fromString("Recb"))));
		this.codeToAbilityTypeDefinition.put(stormHammers, new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityResearchIndicator(handleId, alias, stormHammers, War3ID.fromString("Rhhb"))));
		this.codeToAbilityTypeDefinition.put(flyingMachineBombs, new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityResearchIndicator(handleId, alias, flyingMachineBombs, War3ID.fromString("Rhgb"))));
		this.codeToAbilityTypeDefinition.put(CAbilityFlakCannons.CODE, new CAbilityTypeDefinitionSpellBase(CAbilityFlakCannons::new));
		this.codeToAbilityTypeDefinition.put(CAbilityBarrage.CODE, new CAbilityTypeDefinitionSpellBase(CAbilityBarrage::new));
		this.codeToAbilityTypeDefinition.put(CAbilityOrbOfAnnihilation.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityOrbOfAnnihilation::new));
		this.codeToAbilityTypeDefinition.put(CAbilityLightningAttack.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityLightningAttack::new));
		this.codeToAbilityTypeDefinition.put(CAbilityPulverize.CODE, new CAbilityTypeDefinitionSpellBase(CAbilityPulverize::new));
		this.codeToAbilityTypeDefinition.put(CAbilityLiquidFire.CODE, new CAbilityTypeDefinitionSpellBase(CAbilityLiquidFire::new));
		this.codeToAbilityTypeDefinition.put(CAbilityCleavingAttack.CODE, new CAbilityTypeDefinitionSpellBase(CAbilityCleavingAttack::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Acyc"),
				new CAbilityTypeDefinitionSpellBase(CAbilityCyclone::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Afae"),
				new CAbilityTypeDefinitionSpellBase(CAbilityFaerieFire::new));
		final War3ID shadowMeld = War3ID.fromString("Ashm");
		this.codeToAbilityTypeDefinition.put(shadowMeld, new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityShadowMeld(handleId, shadowMeld, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aesn"), new CAbilityTypeDefinitionSpellBase(CAbilitySentinel::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Adis"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityAreaDispel(handleId, alias, OrderIds.dispel)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Adtn"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityAreaDispel(handleId, alias, OrderIds.detonate)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIdi"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityAreaDispel(handleId, alias, OrderIds.itemdispelaoe)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Advm"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityAreaDispel(handleId, alias, OrderIds.devourmagic)));
		this.codeToAbilityTypeDefinition.put(CAbilityItemPlaceMine.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemPlaceMine::new));
		this.codeToAbilityTypeDefinition.put(CAbilityItemTinyBuilding.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemTinyBuilding::new));
		this.codeToAbilityTypeDefinition.put(CAbilityItemRetrain.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemRetrain::new));
		this.codeToAbilityTypeDefinition.put(CAbilityItemRecall.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemRecall::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUds"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemRecall(handleId, alias, OrderIds.darksummoning)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aste"),
				new CAbilityTypeDefinitionSpellBase(CAbilityItemWandOfManaStealing::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIrb"),
				new CAbilityTypeDefinitionSpellBase(CAbilityRuneOfRebirth::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aclf"),
				new CAbilityTypeDefinitionSpellBase(CAbilityCloudOfFog::new));
		this.codeToAbilityTypeDefinition.put(CAbilityTreeOfLifeUpgradeEffects.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityTreeOfLifeUpgradeEffects::new));
		this.codeToAbilityTypeDefinition.put(CAbilitySleepAlways.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilitySleepAlways::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANdo"),
				new CAbilityTypeDefinitionSpellBase(CAbilityDoom::new));
		this.codeToAbilityTypeDefinition.put(CAbilityParasite.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityParasite::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANht"),
				new CAbilityTypeDefinitionSpellBase(CAbilityHowlOfTerror::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Apos"),
				new CAbilityTypeDefinitionSpellBase(CAbilityBansheePossession::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aabs"),
				new CAbilityTypeDefinitionSpellBase(CAbilityAbsorbMana::new));
		final var frostAttack = new CAbilityTypeDefinitionSpellBase(CAbilityFrostAttack::new);
		this.codeToAbilityTypeDefinition.put(CAbilityFrostAttack.ATTACK, frostAttack);
		this.codeToAbilityTypeDefinition.put(CAbilityFrostAttack.BREATH, frostAttack);
		this.codeToAbilityTypeDefinition.put(CAbilityFrostAttack.FREEZING_BREATH, frostAttack);
		this.codeToAbilityTypeDefinition.put(CAbilitySpiderAttack.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilitySpiderAttack::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Afbk"), new CAbilityTypeDefinitionSpellBase(CAbilityFeedback::new));
		this.codeToAbilityTypeDefinition.put(CAbilityItemSoulTrap.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemSoulTrap::new));
		this.codeToAbilityTypeDefinition.put(CAbilitySoulPossession.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilitySoulPossession::new));
		final var itemSpeed = new CAbilityTypeDefinitionSpellBase(CAbilityItemSpeed::new);
		this.codeToAbilityTypeDefinition.put(CAbilityItemSpeed.POTION, itemSpeed);
		this.codeToAbilityTypeDefinition.put(CAbilityItemSpeed.AREA_SPEED, itemSpeed);
		this.codeToAbilityTypeDefinition.put(CAbilityItemTownPortal.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemTownPortal::new));
		this.codeToAbilityTypeDefinition.put(CAbilityMineTrigger.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityMineTrigger::new));
		this.codeToAbilityTypeDefinition.put(CAbilityDeathExplosion.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityDeathExplosion::new));
		this.codeToAbilityTypeDefinition.put(CAbilityHardenedSkin.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityHardenedSkin::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Agyd"),
				new CAbilityTypeDefinitionSpellBase(CAbilityCreateCorpse::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aapl"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityDiseaseCloud(handleId, alias, false)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Apts"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityDiseaseCloud(handleId, alias, true)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Acri"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityNecromancerEnchantment(handleId, alias, true)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Auhf"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityNecromancerEnchantment(handleId, alias, false)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aspi"),
				new CAbilityTypeDefinitionSpellBase(CAbilitySpikedBarricades::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aprg"),
				new CAbilityTypeDefinitionSpellBase(CAbilityPurge::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Alsh"),
				new CAbilityTypeDefinitionSpellBase(CAbilityLightningShield::new));
		this.codeToAbilityTypeDefinition.put(CAbilityPolymorph.HEX,
				new CAbilityTypeDefinitionSpellBase(CAbilityPolymorph::new));
		this.codeToAbilityTypeDefinition.put(CAbilityPolymorph.POLYMORPH,
				new CAbilityTypeDefinitionSpellBase(CAbilityPolymorph::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aeye"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) ->
						new CAbilityWitchDoctorWard(handleId, alias, CAbilityWitchDoctorWard.Kind.SENTRY)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Ahwd"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) ->
						new CAbilityWitchDoctorWard(handleId, alias, CAbilityWitchDoctorWard.Kind.HEALING)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Asta"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) ->
						new CAbilityWitchDoctorWard(handleId, alias, CAbilityWitchDoctorWard.Kind.STASIS)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOwd"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) ->
						new CAbilityWitchDoctorWard(handleId, alias, CAbilityWitchDoctorWard.Kind.SERPENT)));
		// ----Human----
		// Paladin:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHhb"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityHolyLight(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHds"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityDivineShield(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHre"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityResurrect(handleId, alias)));
		// Archmage
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHwe"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilitySummonWaterElemental(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHbz"),
				new CAbilityTypeDefinitionSpellBase(CAbilityBlizzard::new));
		this.codeToAbilityTypeDefinition.put(CAbilityBlizzard.RAIN_OF_FIRE,
				new CAbilityTypeDefinitionSpellBase(CAbilityBlizzard::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHdr"),
				new CAbilityTypeDefinitionSpellBase(CAbilitySiphonMana::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHab"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityBrilliance(handleId, alias, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHmt"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityMassTeleport(handleId, alias)));
		// Mountain King:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHtb"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityThunderBolt(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHtc"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityThunderClap(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ACtc"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityThunderClap(handleId, alias, OrderIds.creepthunderclap)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANfb"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityThunderBolt(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHav"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityAvatar(handleId, alias)));

		// Blood Mage:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Apxf"), new CAbilityTypeDefinitionPhoenixFire());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHpx"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilitySummonPhoenix(handleId, alias)));

		// ----Orc----
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aens"),
				new CAbilityTypeDefinitionSpellBase(CAbilityEnsnare::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aweb"),
				new CAbilityTypeDefinitionSpellBase(CAbilityWeb::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Auns"),
				new CAbilityTypeDefinitionSpellBase(CAbilityUnsummon::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANfd"),
				new CAbilityTypeDefinitionSpellBase(CAbilityFingerOfDeath::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANrc"),
				new CAbilityTypeDefinitionSpellBase(CAbilityRainOfChaos::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANdp"),
				new CAbilityTypeDefinitionSpellBase(CAbilityDarkPortal::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEsf"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityNightElfChannel(handleId, alias, false)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEtq"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityNightElfChannel(handleId, alias, true)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Asal"),
				new CAbilityTypeDefinitionSpellBase(CAbilityPillage::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Ablo"),
				new CAbilityTypeDefinitionSpellBase(CAbilityBloodlust::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Afzy"),
				new CAbilityTypeDefinitionSpellBase(CAbilityFrenzy::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOsf"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityFeralSpirit(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOcl"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityChainLightning(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOfs"),
				new CAbilityTypeDefinitionSpellBase(CAbilityFarSight::new));
		this.codeToAbilityTypeDefinition.put(CAbilityFarSight.ITEM_AREA_DETECTION,
				new CAbilityTypeDefinitionSpellBase(CAbilityFarSight::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOeq"),
				new CAbilityTypeDefinitionSpellBase(CAbilityEarthquake::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOmi"),
				new CAbilityTypeDefinitionSpellBase(CAbilityMirrorImage::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOhw"),
				new CAbilityTypeDefinitionSpellBase(CAbilityHealingWave::new));

		// Tauren Chieftain
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOws"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityWarStomp(handleId, alias)));

		// Burrow:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Abtl"),
				new CAbilityTypeDefinitionSpellBase(CAbilityBattleStations::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Abun"), new CAbilityTypeDefinitionCargoHoldBurrow());
		this.codeToAbilityTypeDefinition.put(CAbilityChaosCargoLoad.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityChaosCargoLoad::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Astd"), new CAbilityTypeDefinitionStandDown());

		// ----Night Elf----
		// Keeper
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEfn"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityForceOfNature(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEer"),
				new CAbilityTypeDefinitionSpellBase(CAbilityEntanglingRoots::new));

		// Demon Hunter:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEim"), new CAbilityTypeDefinitionImmolation());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEmb"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityManaBurn(handleId, alias)));

		// Moon Priestess
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEst"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilitySummonOwlScout(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHfa"),
				new CAbilityTypeDefinitionSpellBase(CAbilitySearingArrows::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANba"),
				new CAbilityTypeDefinitionSpellBase(CAbilityBlackArrow::new));

		// Warden
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEbl"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityBlink(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEfk"),
				new CAbilityTypeDefinitionSpellBase(CAbilityFanOfKnives::new));

		// ----Undead----
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aams"),
				new CAbilityTypeDefinitionSpellBase(CAbilityAntiMagicShell::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUfa"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityFrostArmor(handleId, alias, false)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUfu"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityFrostArmor(handleId, alias, true)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUfn"),
				new CAbilityTypeDefinitionSpellBase(CAbilityFrostNova::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUdd"),
				new CAbilityTypeDefinitionSpellBase(CAbilityDeathAndDecay::new));
		// Death Knight
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUan"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityAnimateDead(handleId, alias, OrderIds.animatedead)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIan"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityAnimateDead(handleId, alias, OrderIds.itemanimatedead)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ACad"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityAnimateDead(handleId, alias, OrderIds.creepanimatedead)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUdc"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityDeathCoil(handleId, alias)));
		final var inferno = new CAbilityTypeDefinitionSpellBase(CAbilityInferno::new);
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUin"), inferno);
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANin"), inferno);
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANpi"),
				new CAbilityTypeDefinitionSpellBase(CAbilityPermanentImmolation::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUdp"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityDeathPact(handleId, alias)));

		// Light
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUdr"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityDarkRitual(handleId, alias)));

		// Entangled Mine:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aenc"),
				new CAbilityTypeDefinitionCargoHoldEntangledMine());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aent"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityEntangleGoldMine(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aegm"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityEntangledMine(handleId, alias, alias)));

		// Ancients:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aeat"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityEatTree(handleId, alias)));

		// Moon Well
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Ambt"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityMoonWell(handleId, alias)));
		// ----Neutral----
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANfl"),
				new CAbilityTypeDefinitionSpellBase(CAbilityForkedLightning::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANdc"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityDarkConversion(handleId, alias)));
		// Dark Ranger:
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANch"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityCharm(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIco"), // Item Command
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityCharm(handleId, alias)));
		// Tinker
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANcs"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityClusterRockets(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANfy"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityFactory(handleId, alias, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANsy"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityPocketFactory(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Asds"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityKaboom(handleId, alias)));

		// Beastmaster
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANsg"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilitySummonGrizzly(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANsq"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilitySummonQuilbeast(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANsw"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilitySummonHawk(handleId, alias)));

		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AHca"), new CAbilityTypeDefinitionColdArrows());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Agld"), new CAbilityTypeDefinitionGoldMine());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Agl2"), new CAbilityTypeDefinitionGoldMineOverlayed());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Abgm"), new CAbilityTypeDefinitionBlightedGoldMine());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Abli"), new CAbilityTypeDefinitionBlight());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Ablp"),
				new CAbilityTypeDefinitionSpellBase(CAbilityBlightPlacement::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIdc"),
				new CAbilityTypeDefinitionSpellBase(CAbilityChainDispel::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aaha"), new CAbilityTypeDefinitionAcolyteHarvest());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Artn"), new CAbilityTypeDefinitionReturnResources());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Ahar"), new CAbilityTypeDefinitionHarvest());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Awha"), new CAbilityTypeDefinitionWispHarvest());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Ahrl"), new CAbilityTypeDefinitionHarvestLumber());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANcl"), new CAbilityTypeDefinitionChannelTest());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AUcs"), new CAbilityTypeDefinitionCarrionSwarmDummy());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AInv"), new CAbilityTypeDefinitionInventory());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Arep"), new CAbilityTypeDefinitionHumanRepair());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aren"), new CAbilityTypeDefinitionRepair());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Arst"), new CAbilityTypeDefinitionRepair());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Avul"), new CAbilityTypeDefinitionInvulnerable());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Apit"), new CAbilityTypeDefinitionShopPurchaseItem());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aneu"), new CAbilityTypeDefinitionNeutralBuilding());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aall"), new CAbilityTypeDefinitionShopSharing());
		// this.codeToAbilityTypeDefinition.put(War3ID.fromString("Acoi"), new
		// CAbilityTypeDefinitionCoupleInstant());
		this.codeToAbilityTypeDefinition.put(CAbilityItemHeal.CODE, new CAbilityTypeDefinitionItemHeal());
		this.codeToAbilityTypeDefinition.put(CAbilityItemLifeRegeneration.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemLifeRegeneration::new));
		this.codeToAbilityTypeDefinition.put(CAbilityItemAreaArmor.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemAreaArmor::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIha"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemRestore(handleId, alias, OrderIds.itemhealaoe)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AImr"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemRestore(handleId, alias, OrderIds.itemmanarestoreaoe)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIra"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemRestore(handleId, alias, OrderIds.itemrestoreaoe)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIre"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemRestore(handleId, alias, OrderIds.itemrestore)));
		this.codeToAbilityTypeDefinition.put(CAbilityItemManaRegain.CODE, new CAbilityTypeDefinitionItemManaRegain());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIat"), new CAbilityTypeDefinitionItemAttackBonus());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIab"), new CAbilityTypeDefinitionItemStatBonus());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aamk"), new CAbilityTypeDefinitionItemStatBonus());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIim"),
				new CAbilityTypeDefinitionItemPermanentStatGain());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIsm"),
				new CAbilityTypeDefinitionItemPermanentStatGain());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIam"),
				new CAbilityTypeDefinitionItemPermanentStatGain());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIxm"),
				new CAbilityTypeDefinitionItemPermanentStatGain());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIde"), new CAbilityTypeDefinitionItemDefenseBonus());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIml"), new CAbilityTypeDefinitionItemLifeBonus());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AImm"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityItemManaBonus(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIfs"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemFigurineSummon(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIff"), new CAbilityTypeDefinitionSpellBase(CAbilityItemFigurineSummon::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIfr"), new CAbilityTypeDefinitionSpellBase(CAbilityItemFigurineSummon::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIfu"), new CAbilityTypeDefinitionSpellBase(CAbilityItemFigurineSummon::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIfh"), new CAbilityTypeDefinitionSpellBase(CAbilityItemFigurineSummon::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIsh"), new CAbilityTypeDefinitionSpellBase(CAbilityFeralSpirit::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ANwm"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityFeralSpirit(handleId, alias, OrderIds.wateryminion, AbilityFields.DATA_A)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AEsv"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityFeralSpirit(handleId, alias, OrderIds.spiritofvengeance, AbilityFields.DATA_A)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIwb"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityWeb(handleId, alias, OrderIds.itemweb)));
		final var orb = new CAbilityTypeDefinitionSpellBase(CAbilityItemOrb::new);
		this.codeToAbilityTypeDefinition.put(CAbilityItemOrb.FIRE, orb);
		this.codeToAbilityTypeDefinition.put(CAbilityItemOrb.FROST, orb);
		this.codeToAbilityTypeDefinition.put(CAbilityItemOrb.VENOM, orb);
		this.codeToAbilityTypeDefinition.put(CAbilityItemOrb.SPELL, orb);
		this.codeToAbilityTypeDefinition.put(CAbilityItemOrb.LEGACY_LIGHTNING, orb);
		this.codeToAbilityTypeDefinition.put(CAbilityItemOrb.LEGACY_PURGE, orb);
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIrs"), new CAbilityTypeDefinitionSpellBase(CAbilityResurrect::new));
		this.codeToAbilityTypeDefinition.put(CAbilityItemAntiMagicPotion.CODE, new CAbilityTypeDefinitionSpellBase(CAbilityItemAntiMagicPotion::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AImi"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemPermanentLifeGain(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIem"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemExperienceGain(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIgo"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityItemChestOfGold(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(CAbilityItemBundleOfLumber.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityItemBundleOfLumber::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIgl"),
				new CAbilityTypeDefinitionSpellBase(CAbilityItemGlyph::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AIlm"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityItemLevelGain(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Acar"), new CAbilityTypeDefinitionCargoHold());
		this.codeToAbilityTypeDefinition.put(CAbilityCargoHoldDeath.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityCargoHoldDeath::new));
		this.codeToAbilityTypeDefinition.put(CAbilityMeatWagonCargo.CODE, new CAbilityTypeDefinitionCargoHold());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Amel"), new CAbilityTypeDefinitionSpellBase(CAbilityGetCorpse::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Amed"), new CAbilityTypeDefinitionSpellBase(CAbilityDropCorpses::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aexh"), new CAbilityTypeDefinitionSpellBase(CAbilityExhumeCorpses::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Afla"), new CAbilityTypeDefinitionSpellBase(CAbilityFlare::new));
		this.codeToAbilityTypeDefinition.put(CAbilityFragmentationShards.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityFragmentationShards::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Atau"), new CAbilityTypeDefinitionSpellBase(CAbilityTaunt::new));
		this.codeToAbilityTypeDefinition.put(CAbilityAbolishMagic.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityAbolishMagic::new));
		this.codeToAbilityTypeDefinition.put(CAbilityDismount.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityDismount::new));
		this.codeToAbilityTypeDefinition.put(CAbilityManaFlare.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityManaFlare::new));
		this.codeToAbilityTypeDefinition.put(CAbilityPhaseShift.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityPhaseShift::new));
		this.codeToAbilityTypeDefinition.put(CAbilityControlMagic.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityControlMagic::new));
		this.codeToAbilityTypeDefinition.put(CAbilitySpellSteal.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilitySpellSteal::new));
		this.codeToAbilityTypeDefinition.put(CAbilityBreathOfFire.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityBreathOfFire::new));
		this.codeToAbilityTypeDefinition.put(CAbilityDrunkenHaze.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityDrunkenHaze::new));
		this.codeToAbilityTypeDefinition.put(CAbilitySoulPreservation.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilitySoulPreservation::new));
		this.codeToAbilityTypeDefinition.put(CAbilityStormEarthAndFire.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityStormEarthAndFire::new));
		this.codeToAbilityTypeDefinition.put(CAbilityWarClub.CODE,
				new CAbilityTypeDefinitionSpellBase(CAbilityWarClub::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Advc"), new CAbilityTypeDefinitionDevourCargo());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Adev"), new CAbilityTypeDefinitionSpellBase(CAbilityDevour::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Absk"), new CAbilityTypeDefinitionSpellBase(CAbilityBerserk::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ACdv"), new CAbilityTypeDefinitionSpellBase(CAbilityDevour::new));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aloa"), new CAbilityTypeDefinitionLoad());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Adro"), new CAbilityTypeDefinitionDrop());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Adri"), new CAbilityTypeDefinitionSpellBase(
				(handleId, alias) -> new CAbilityDropInstant(handleId, alias, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Aroo"), new CAbilityTypeDefinitionRoot());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("ARal"), new CAbilityTypeDefinitionRally());
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("Awrp"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityWayGate(handleId, alias)));
		this.codeToAbilityTypeDefinition.put(War3ID.fromString("AOww"),
				new CAbilityTypeDefinitionSpellBase((handleId, alias) -> new CAbilityWhirlWind(handleId, alias)));

		System.err.println("========================================================================");
		System.err.println("Starting to load ability builder");
		System.err.println("========================================================================");

		if (true) {
			AbilityBuilderParserUtil.loadAbilityBuilderFiles(behavior -> {
				if (behavior.getType().equals(AbilityBuilderType.TEMPLATE)) {
					for (final AbilityBuilderDupe dupe : behavior.getIds()) {
						this.codeToAbilityTypeDefinition.put(War3ID.fromString(dupe.getId()),
								new CAbilityTypeDefinitionAbilityTemplateBuilder(behavior));
					}
				}
				else {
					for (final AbilityBuilderDupe dupe : behavior.getIds()) {
						final AbilityBuilderConfiguration config = new AbilityBuilderConfiguration(behavior, dupe);
						this.codeToAbilityTypeDefinition.put(War3ID.fromString(config.getId()),
								config.createDefinition());
					}
				}
			});
		}

		System.err.println("========================================================================");
		System.err.println("registered abilities");
		System.err.println("========================================================================");

	}

	public void registerJassType(final War3ID war3id, final CAbilityTypeJassDefinition whichAbilityType) {
		this.codeToAbilityTypeDefinition.put(war3id, whichAbilityType);
		this.aliasToAbilityType.entrySet().removeIf(entry -> entry.getValue().getCode().equals(war3id));
	}

	public void registerAbilityBuilderType(final War3ID war3id, final AbilityBuilderConfiguration configuration) {
		this.codeToAbilityTypeDefinition.put(war3id, configuration.createDefinition());
		this.aliasToAbilityType.entrySet().removeIf(entry -> entry.getValue().getCode().equals(war3id));
	}

	public CAbilityType<?> getAbilityType(final War3ID alias) {
		CAbilityType<?> abilityType = this.aliasToAbilityType.get(alias);
		if (abilityType == null) {
			final GameObject gameObject = this.abilityData.get(alias);
			if (gameObject == null) {
				return null;
			}
			final War3ID code = War3ID.fromString(gameObject.readSLKTag("code"));
			final CAbilityTypeDefinition abilityTypeDefinition = this.codeToAbilityTypeDefinition.get(code);
			if (abilityTypeDefinition != null) {
				abilityType = abilityTypeDefinition.createAbilityType(alias, gameObject);
				this.aliasToAbilityType.put(alias, abilityType);
			}
		}
		return abilityType;
	}

	public GameObject getDefinition(final War3ID alias) {
		return this.abilityData.get(alias);
	}

	public int getHeroRequiredLevel(final CSimulation game, final War3ID alias, final int currentLevelOfAbility) {
		// TODO maybe use CAbilityType for this to avoid hashtable lookups and just do
		// fast symbol table resolution.
		// (i.e. like all other fields of CAbilityType). For now I didn't bother because
		// I wanted to just have this working.
		final GameObject mutableGameObject = this.abilityData.get(alias);
		int levelSkip = mutableGameObject.getFieldAsInteger(AbilityFields.REQUIRED_LEVEL_SKIP, 0);
		if (levelSkip == 0) {
			levelSkip = game.getGameplayConstants().getHeroAbilityLevelSkip();
		}
		final int baseRequiredLevel = mutableGameObject.getFieldAsInteger(AbilityFields.REQUIRED_LEVEL, 0);
		return baseRequiredLevel + (currentLevelOfAbility * levelSkip);
	}

	public CAbility createAbility(final War3ID abilityId, final int handleId) {
		final CAbilityType<?> abilityType = getAbilityType(abilityId);
		if (abilityType != null) {
			return abilityType.createAbility(handleId);
		}
		return new CAbilityGenericDoNothing(abilityId, abilityId, handleId);
	}
}
