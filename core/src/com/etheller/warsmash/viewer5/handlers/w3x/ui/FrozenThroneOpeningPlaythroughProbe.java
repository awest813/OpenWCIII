package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilitySellUnits;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityEntangledMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;

/** Plays through the authored Maiev pursuit and harbor objectives with ordinary player orders. */
public final class FrozenThroneOpeningPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.frozenThroneOpeningAudit");
	private static final boolean RESUME_HARBOR = Boolean.getBoolean("warsmash.frozenThroneOpeningResumeHarbor");
	private static final boolean ISLAND_OPENING = Boolean.getBoolean("warsmash.frozenThroneIslandOpening");
	private static final boolean ISLAND_VOYAGE = Boolean.getBoolean("warsmash.frozenThroneIslandVoyage");
	private static final boolean ISLAND_LANDFALL = Boolean.getBoolean("warsmash.frozenThroneIslandLandfall");
	private static final boolean ISLAND_PLAYTHROUGH = Boolean.getBoolean("warsmash.frozenThroneIslandPlaythrough");
	private static final boolean TOMB_OPENING = Boolean.getBoolean("warsmash.frozenThroneTombOpening");
	private static final boolean TOMB_FIRST_BARRIER = Boolean.getBoolean("warsmash.frozenThroneTombFirstBarrier");
	private static final boolean TOMB_SECOND_BARRIER = Boolean.getBoolean("warsmash.frozenThroneTombSecondBarrier");
	private static final boolean TOMB_THIRD_BARRIER = Boolean.getBoolean("warsmash.frozenThroneTombThirdBarrier");
	private static final boolean TOMB_EYE = Boolean.getBoolean("warsmash.frozenThroneTombEye");
	private static final boolean TOMB_ESCAPE = Boolean.getBoolean("warsmash.frozenThroneTombEscape");
	private static final boolean AFTER_TOMB = Boolean.getBoolean("warsmash.frozenThroneAfterTomb");
	private static final boolean AFTER_TOMB_RUNNER = Boolean.getBoolean("warsmash.frozenThroneAfterTombRunner");
	private static final boolean AFTER_TOMB_SEA_ESCAPE = Boolean.getBoolean("warsmash.frozenThroneAfterTombSeaEscape");
	private static final String SAVE_HARBOR = System.getProperty("warsmash.frozenThroneOpeningSaveHarbor");
	private static final War3ID FAN_OF_KNIVES = War3ID.fromString("AEfk");
	private static final War3ID BLINK = War3ID.fromString("AEbl");
	private static final War3ID SHADOW_STRIKE = War3ID.fromString("AEsh");
	private static final int VILLAGE_APPROACH = 10;
	private static final int NAGA_APPROACH = 21;
	private static final float[][] HARBOR_ROUTE = {
			{ 2000, -6352 }, { 1904, -6032 }, { 1616, -5712 }, { 1616, -4752 }, { 1328, -4560 },
			{ -176, -4496 }, { -592, -4848 }, { -1232, -5488 }, { -1872, -6128 }, { -2192, -6256 },
			{ -2544, -6288 }, { -3792, -6288 }, { -4432, -5712 }, { -4720, -5072 }, { -4144, -4560 },
			{ -3504, -3920 }, { -2544, -3856 }, { -2288, -3600 }, { -2192, -2960 }, { -2192, -2640 },
			{ -1584, -2000 }, { -976, -1360 }, { -784, -720 }, { -1424, -528 }, { -2704, -528 },
			{ -3248, 48 }, { -3536, 368 }, { -3664, 1008 }, { -3952, 1328 }, { -3952, 2608 },
			{ -3568, 3248 }, { -2928, 3888 }, { -2608, 4144 }, { -2288, 4144 }, { -1968, 4432 }, { -1648, 4752 }
	};
	private static final float[][] HARBOR_ASSAULT_ROUTE = {
			{ -432, 4720 }, { -176, 4912 }, { -176, 4592 }, { -80, 4272 }, { 48, 3952 }, { 368, 3632 }, { 624, 3312 }
	};
	private static final float[][] INNER_HARBOR_ROUTE = { { 624, 3312 }, { 624, 3600 }, { 528, 3920 } };
	private static final float[][] EAST_HARBOR_ROUTE = {
			{ 624, 3312 }, { 880, 3056 }, { 1200, 3056 }, { 1520, 3056 }, { 1840, 2960 }, { 2160, 2640 }
	};
	private static final float[][] NORTH_HARBOR_ROUTE = {
			{ 624, 3376 }, { 624, 3696 }, { 624, 4016 }, { 624, 4336 }, { 624, 4656 },
			{ 720, 4976 }, { 1040, 5104 }, { 1360, 5296 }, { 1616, 5456 }
	};
	private static final float[] MIDDLE_ISLAND = { 2272, 4736 };
	private static final float[] EAST_ISLAND = { 2800, 4848 };
	private static final War3ID ISLAND_TRANSPORT = War3ID.fromString("etrs");
	private static final War3ID NORTHERN_GATE = War3ID.fromString("B000");
	private static final War3ID MOON_WELL = War3ID.fromString("emow");
	private static final float[][] TOMB_GATE_CLEARANCE = { { 1776, 7856 }, { 1568, 7888 }, { 1392, 7984 }, { 1280, 7984 } };
	private static final float[][] TOMB_APPROACH = { { 1232, 7376 }, { 1232, 7696 }, { 1392, 7984 },
			{ 1712, 7984 }, { 2032, 7984 }, { 2352, 7984 }, { 2672, 7856 }, { 2960, 7600 },
			{ 3056, 7248 }, { 3376, 6928 } };
	private static final float[][] TOMB_INTRO_ROUTE = { { -3808, -192 }, { -3456, -384 }, { -3136, -512 },
			{ -2752, -512 } };
	private static final float[][] RESCUE_BOAT_ROUTE = {
			{ 4624, 688 }, { 4624, 112 }, { 4624, -464 }, { 4624, -1040 }, { 4624, -1616 },
			{ 4592, -1904 }, { 4304, -2160 }, { 3728, -2160 }, { 3152, -2192 }, { 2576, -2224 },
			{ 2000, -2224 }, { 1808, -2000 }, { 1744, -1712 }, { 1712, -1424 }, { 1552, -1136 },
			{ 1424, -848 }, { 1424, -560 }, { 1360, -272 }, { 1360, 16 }, { 1328, 304 }, { 1040, 592 },
			{ 976, 880 }, { 976, 1456 }, { 784, 1744 }, { 496, 2032 }, { 208, 2288 }, { -80, 2416 },
			{ -656, 2416 }, { -1232, 2416 }, { -1520, 2160 }, { -1712, 1872 }, { -1712, 1296 },
			{ -1712, 720 }, { -1616, 432 }, { -1328, 144 }, { -1040, -144 }, { -848, -432 },
			{ -784, -720 }, { -560, -848 }
	};
	private static final float[][] RESCUE_BOAT_BLINK_ROUTE = {
			{ 3888, 304 }, { 3120, 304 }, { 2352, 272 }, { 1584, -112 }, { 720, -80 },
			{ -176, -272 }, { -560, -848 }
	};
	private static final float[][] RUNNER_SHORE_ROUTE = {
			{ 4800, 800 }, { 4816, 592 }, { 4816, 304 }, { 4752, 16 }, { 4752, -272 },
			{ 4688, -560 }, { 4688, -1136 }, { 4688, -1712 }, { 4464, -2000 }, { 4176, -2160 },
			{ 3600, -2160 }, { 3312, -2160 }, { 3024, -2224 }, { 2448, -2224 }, { 1872, -2160 },
			{ 1744, -1872 }, { 1744, -1584 }, { 1616, -1296 }, { 1424, -1008 }, { 1424, -720 },
			{ 1360, -144 }, { 1360, 144 }, { 1200, 432 }, { 976, 720 }, { 976, 1296 }, { 880, 1584 },
			{ 592, 1872 }, { 304, 2160 }, { 16, 2320 }, { -272, 2384 }, { -848, 2384 }, { -1136, 2384 },
			{ -1424, 2224 }, { -1712, 1936 }, { -1712, 1360 }, { -1712, 784 }, { -1584, 592 },
			{ -1296, 432 }, { -1040, 432 }
	};
	private static final float[][] RUNNER_PICKUP_WATER_ROUTE = {
			{ -560, 144 }, { -848, 432 }
	};
	private static final int RUNNER_CAUSEWAY_STEP = 13;
	private static final int RUNNER_RIDGE_STEP = 30;
	private static final int RUNNER_LANDING_STEP = 34;
	private static final float[][] RUNNER_RIDGE_TURN = { { -832, 2528 }, { -1136, 2528 } };
	private static final float[][] RUNNER_PIER_TURN = { { -1712, 1344 }, { -1712, 1264 }, { -1712, 1008 },
			{ -1584, 880 }, { -1520, 688 }, { -1456, 496 }, { -1232, 464 }, { -976, 464 } };
	private static final float[][] PIER_DETOUR_OFFSETS = { { 0, -64 }, { -48, -64 }, { -64, 0 }, { 0, 64 } };
	private static final float[][] RUNNER_FERRY_BLINKS = { { -656, 2000 }, { -720, 1104 }, { -1104, 336 } };
	private static final float[][] RUNNER_SOUTH_BANK_ROUTE = { { -1232, 176 }, { -1488, -80 }, { -1744, -208 },
			{ -1872, -208 }, { -2704, -464 }, { -2832, -720 }, { -2832, -976 }, { -2768, -1232 }, { -2768, -1488 },
			{ -3024, -1744 }, { -3408, -2512 }, { -3600, -2576 }, { -3856, -2576 }, { -4112, -2384 } };
	private static final float[][] RUNNER_NORTH_BANK_ROUTE = { { -4240, -2576 }, { -4496, -2320 }, { -4752, -2064 },
			{ -4880, -1808 }, { -4688, -1552 }, { -4560, -1296 }, { -4560, -1040 }, { -4432, -912 }, { -3920, -208 },
			{ -3856, 112 }, { -3856, 368 }, { -4112, 624 }, { -4208, 960 }, { -4320, 1192 } };
	private static final float[][] RUNNER_SIEGE_LANDINGS = { { -4112, -2576 }, { -3984, -528 } };
	private static final float[][] RUNNER_SIEGE_ADVANCE = { { -4112, -2352 }, { -4112, 624 } };
	private static final float[][] SOUTH_GATE_FERRY_ROUTE = { { -848, 432 }, { -848, 144 }, { -720, -144 }, { -432, -432 },
			{ -336, -720 }, { -80, -1008 }, { -16, -1296 }, { -48, -1584 }, { -336, -1872 }, { -432, -2160 },
			{ -400, -2448 }, { -272, -2736 }, { -272, -3024 }, { -368, -3312 }, { -528, -3536 }, { -816, -3536 },
			{ -1104, -3536 }, { -1392, -3536 }, { -1680, -3600 }, { -1904, -3888 }, { -2192, -4176 }, { -2480, -4240 },
			{ -2768, -4240 }, { -3056, -4240 }, { -3344, -4240 }, { -3632, -4080 }, { -3792, -3792 }, { -4080, -3504 },
			{ -4112, -3216 }, { -4272, -2928 }, { -3984, -2640 } };
	private static final int NORTH_GATE_LANDING_STEP = 30;
	private static final float[][] RUNNER_ESCAPE_CHANNEL = {
			{ -848, 272 }, { -848, -16 }, { -592, -304 }, { -368, -592 }, { -176, -880 },
			{ -80, -1168 }, { -16, -1456 }, { -16, -1744 }, { -16, -2032 }, { -16, -2320 },
			{ -16, -2608 }, { -16, -2896 }, { -112, -3184 }, { -400, -3472 }, { -688, -3536 },
			{ -1264, -3536 }, { -1840, -3536 }, { -2128, -3664 }, { -2416, -3856 }, { -2704, -3856 },
			{ -2896, -3696 }, { -2960, -3408 }, { -3056, -3120 }, { -3344, -2832 }, { -3632, -2544 },
			{ -3792, -2256 }, { -3792, -2032 }, { -3792, -1680 }, { -3792, -1104 }, { -3792, -816 },
			{ -4048, -528 }, { -4272, -240 }, { -4368, 48 }, { -4368, 624 }, { -4368, 1200 },
			{ -4368, 1424 }, { -4368, 1776 }, { -4336, 2064 }, { -4304, 2640 }, { -4112, 2928 },
			{ -4112, 3504 }, { -4112, 4080 }, { -4144, 4368 }, { -4432, 4656 }, { -4688, 4944 }
	};
	private static final float[][] TOMB_FIRST_BARRIER_ROUTE = { { -2736, -560 }, { -2736, -848 }, { -2736, -1136 },
			{ -2736, -1424 }, { -2896, -1584 }, { -3184, -1584 }, { -3472, -1584 }, { -3728, -1744 },
			{ -3792, -1776 }, { -4016, -2064 }, { -4016, -2352 }, { -3728, -2416 }, { -3440, -2416 },
			{ -3152, -2416 }, { -2864, -2416 }, { -2576, -2416 }, { -2288, -2416 }, { -2000, -2416 },
			{ -1712, -2416 }, { -1424, -2416 }, { -1136, -2416 }, { -848, -2512 }, { -720, -2800 },
			{ -464, -3088 }, { -176, -3184 }, { 112, -3184 }, { 400, -3184 }, { 688, -3376 }, { 976, -3664 } };
	private static final int TOMB_DEMONIC_GATE_APPROACH = 7;
	private static final float[][] TOMB_SECOND_BARRIER_ROUTE = { { 976, -3664 }, { 1136, -3600 }, { 1424, -3312 },
			{ 1712, -3024 }, { 1792, -3008 }, { 1488, -2736 }, { 1296, -2448 }, { 1168, -2160 }, { 1104, -1872 },
			{ 1104, -1584 }, { 1104, -1296 }, { 1104, -1008 }, { 1104, -720 }, { 1104, -432 }, { 1104, -144 },
			{ 1104, 144 }, { 1104, 432 }, { 1104, 720 }, { 1040, 1008 }, { 1040, 1296 }, { 1040, 1584 },
			{ 1040, 1872 }, { 1040, 2160 }, { 1040, 2448 }, { 944, 2736 }, { 656, 2736 }, { 368, 2736 },
			{ 80, 2736 }, { -208, 2736 }, { -496, 2736 }, { -784, 2736 }, { -1072, 2736 }, { -1360, 2736 },
			{ -1648, 2736 }, { -1936, 2992 }, { -2224, 2992 }, { -2512, 2992 }, { -2800, 2992 },
			{ -3088, 2992 }, { -3376, 2992 }, { -3664, 2992 }, { -3952, 3216 }, { -4240, 3440 },
			{ -4528, 3504 }, { -4816, 3504 }, { -5104, 3504 }, { -5392, 3760 } };
	private static final int TOMB_SWITCH_APPROACH = 4;
	private static final float[][] TOMB_WEST_BRIDGE_ROUTE = { { -5296, 3600 }, { -5008, 3600 }, { -4720, 3600 },
			{ -4432, 3728 }, { -4144, 4016 }, { -3856, 4272 }, { -3568, 4400 }, { -3280, 4400 }, { -2992, 4400 },
			{ -2704, 4400 }, { -2416, 4400 }, { -2128, 4400 }, { -1840, 4400 }, { -1552, 4400 }, { -1264, 4400 },
			{ -976, 4400 }, { -688, 4400 }, { -400, 4560 } };
	private static final float[][] TOMB_THIRD_BARRIER_ROUTE = { { -400, 4560 }, { -288, 4480 }, { -64, 4480 },
			{ 192, 4480 }, { 416, 4512 }, { 544, 4640 }, { 784, 4656 }, { 1072, 4656 }, { 1360, 4656 },
			{ 1648, 4560 }, { 1936, 4560 }, { 2224, 4560 }, { 2512, 4304 }, { 2800, 4048 }, { 3088, 3984 },
			{ 3376, 3824 }, { 3664, 3792 }, { 3952, 3696 }, { 4240, 3408 }, { 4528, 3120 }, { 4816, 2832 },
			{ 5104, 2704 }, { 5392, 2640 }, { 5680, 2640 }, { 5968, 2640 }, { 6256, 2640 }, { 6544, 2640 },
			{ 6832, 2640 }, { 7120, 2640 }, { 7408, 2512 } };
	private static final int TOMB_BRIDGE_RALLY = 6;
	private static final float[][] TOMB_EYE_ROUTE = { { 7376, 2064 }, { 7120, 1776 }, { 6832, 1488 },
			{ 6544, 1200 }, { 6256, 976 }, { 5968, 976 }, { 5680, 976 }, { 5392, 944 }, { 5104, 656 },
			{ 4816, 432 }, { 4528, 176 }, { 4240, -80 }, { 3952, -240 }, { 3664, -240 }, { 3504, -496 },
			{ 3472, -784 }, { 3472, -1072 }, { 3472, -1360 }, { 3664, -1648 }, { 3760, -1936 },
			{ 4048, -2192 }, { 4240, -2192 }, { 4528, -2100 }, { 4816, -2000 }, { 5104, -2000 },
			{ 5392, -2000 }, { 5680, -2000 }, { 5968, -2000 }, { 6288, -2192 } };
	private static final float[][] TOMB_ESCAPE_EAST = { { 6080, -2160 }, { 5616, -2160 }, { 5040, -2160 },
			{ 4464, -2160 }, { 3888, -2064 }, { 3792, -1776 }, { 3536, -1488 }, { 3472, -912 },
			{ 3472, -624 }, { 3536, -336 }, { 4112, -336 }, { 4400, -240 }, { 4688, 48 },
			{ 4976, 112 }, { 5264, 304 }, { 5840, 560 }, { 6128, 784 }, { 6704, 1360 }, { 6960, 1648 },
			{ 6960, 2224 }, { 6672, 2352 }, { 6096, 2352 }, { 5520, 2352 }, { 4944, 2352 }, { 4656, 2480 },
			{ 4368, 2768 }, { 4080, 2864 }, { 3792, 3088 }, { 3504, 3184 }, { 3216, 3408 },
			{ 2928, 3696 }, { 2352, 4272 }, { 1776, 4272 }, { 1488, 4368 }, { 912, 4400 }, { 624, 4624 }, { 544, 4640 } };
	private static final float[][] TOMB_ESCAPE_WEST = { { -400, 4560 }, { -944, 4560 }, { -1520, 4560 },
			{ -2096, 4560 }, { -2672, 4560 }, { -3248, 4560 }, { -3536, 4560 }, { -3824, 4336 },
			{ -4016, 4048 }, { -4016, 3760 }, { -3952, 3536 }, { -3664, 3440 }, { -3376, 3280 },
			{ -3088, 3216 }, { -2512, 3216 }, { -2224, 3216 }, { -1936, 3120 }, { -1648, 2832 },
			{ -1360, 2768 }, { -784, 2768 }, { -208, 2768 }, { 368, 2768 }, { 944, 2736 }, { 944, 2448 },
			{ 1008, 2160 }, { 1008, 1584 }, { 1008, 1008 }, { 1008, 432 }, { 1008, -144 }, { 1008, -720 },
			{ 1072, -1008 }, { 1072, -1584 }, { 1072, -2160 }, { 1072, -2448 }, { 1264, -2736 },
			{ 1264, -3024 }, { 1072, -3312 }, { 496, -3312 }, { -80, -3312 }, { -368, -3184 },
			{ -464, -2896 }, { -752, -2640 }, { -1328, -2640 }, { -1904, -2640 }, { -2480, -2640 },
			{ -3056, -2640 }, { -3344, -2576 }, { -3632, -2576 }, { -3920, -2448 }, { -4016, -2160 },
			{ -3888, -2000 }, { -3312, -2000 }, { -3024, -1840 }, { -2768, -1552 }, { -2768, -976 },
			{ -2928, -720 }, { -3504, -720 }, { -4080, -720 }, { -4656, -720 }, { -4944, -656 },
			{ -5520, -656 }, { -5808, -528 } };
	private static final float[][] ISLAND_DOCK_ROUTE = {
			{ 2448, -4208 }, { 2128, -4208 }, { 1808, -4080 }, { 1488, -3760 }, { 1168, -3440 },
			{ 912, -3120 }, { 848, -2800 }, { 848, -2480 }, { 528, -2160 }, { 208, -1840 },
			{ -112, -1520 }, { -176, -1200 }, { -496, -880 }, { -720, -560 }
	};
	private static final float[][] ISLAND_CHANNEL_ROUTE = {
			{ -592, -496 }, { -592, -176 }, { -592, 144 }, { -592, 464 }, { -592, 784 },
			{ -592, 1104 }, { -592, 1424 }, { -272, 1744 }, { 48, 2064 }, { 176, 2384 },
			{ 304, 2704 }, { 304, 3024 }
	};
	private static final float[][] ISLAND_NORTH_ROUTE = {
			{ 304, 3248 }, { 304, 3568 }, { 432, 3888 }, { 752, 4208 }, { 944, 4528 }, { 1136, 4848 },
			{ 1136, 5168 }, { 1136, 5488 }, { 1200, 5808 }, { 1200, 6128 }, { 1200, 6448 },
			{ 1456, 6768 }, { 1456, 7088 }, { 1328, 7344 }
	};
	private static final float[][] FERRY_NORTH;
	private static final float[][] FERRY_SOUTH;
	static {
		final var route = new ArrayList<float[]>();
		route.add(new float[] { -720, -560 });
		route.addAll(Arrays.asList(ISLAND_CHANNEL_ROUTE));
		route.addAll(Arrays.asList(ISLAND_NORTH_ROUTE));
		FERRY_NORTH = route.toArray(float[][]::new);
		Collections.reverse(route);
		FERRY_SOUTH = route.toArray(float[][]::new);
	}
	private enum ResupplyStep { RETURNING, BOARDING, SAILING, LANDING }
	private static final Map<Integer, Integer> escortWaypoints = new HashMap<>();
	private static final Map<Integer, Integer> harborCombatWaypoints = new HashMap<>();
	private static int waypoint = -1;
	private static int orderTick;
	private static int reportTick;
	private static int progressTick;
	private static double closestApproach = Double.POSITIVE_INFINITY;
	private static boolean fanOrdered;
	private static boolean easternLandingReached;
	private static boolean retreating;
	private static int retreatWaypoint;
	private static boolean nagaEscortReady;
	private static float refugeX;
	private static float refugeY;
	private static float forwardRefugeX;
	private static float forwardRefugeY;
	private static float rearRefugeX;
	private static float rearRefugeY;
	private static boolean harborReached;
	private static int harborWaypoint;
	private static int harborRetreatWaypoint;
	private static CUnit harborTarget;
	private static CUnit offshoreDefender;
	private static boolean maievCrossed;
	private static boolean finished;
	private static Properties carriedMaiev;
	private static boolean islandIntroSkipped;
	private static int islandStartingGold = -1;
	private static boolean islandGoldDelivered;
	private static boolean islandTransportPurchased;
	private static boolean expeditionBoarded;
	private static boolean channelSailed;
	private static boolean northernPassengersUnloaded;
	private static List<CUnit> islandExpedition = List.of();
	private static final Map<Integer, Integer> tombSteps = new HashMap<>();
	private static final Map<Integer, Integer> northGateSteps = new HashMap<>();
	private static final int TOMB_RALLY = 5;
	private static int tombFront = TOMB_RALLY;
	private static int tombRefuge = 4;
	private static CUnit islandFerry;
	private static ResupplyStep resupply;
	private static List<CUnit> replacements = List.of();
	private static final Map<Integer, Integer> ferrySteps = new HashMap<>();
	private static boolean tombApproachOpened;
	private static boolean tombVictoryReported;
	private static CItem tombBlinkRune;
	private static boolean tombBlinkRuneUsed;
	private static CDestructable tombDemonicGate;
	private static List<CDestructable> tombFirstRocks = List.of();
	private static boolean tombFirstBarrierCleared;
	private static boolean tombFloorSwitchActivated;
	private static List<CDestructable> tombSecondRocks = List.of();
	private static boolean tombSecondBarrierCleared;
	private static boolean tombBarriersCleared;
	private static float[][] tombEyeApproach;
	private static boolean tombEyeRecovering;
	private static int tombEyeRefugeStep = -1;
	private static List<CDestructable> tombEyeGates;
	private static final Map<Integer, Integer> tombEscapeSteps = new HashMap<>();
	private static final Set<Integer> attemptedEscapeRunes = new HashSet<>();
	private static CItem tombEscapeRune;
	private static int tombEscapeRuneDeadline;
	private static boolean tombEyeReached;
	private static boolean tombEscapeRiverCrossed;
	private static boolean tombEscapeCompleted;
	private static Properties tombEscapeCarriedMaiev;
	private static final Set<Integer> attemptedEyeRunes = new HashSet<>();
	private static int eyeRuneDeadline;
	private static CItem eyeRune;
	private static boolean tombBridgeWasDown;
	private static boolean tombBridgeRaised;
	private static boolean tombBridgePartyCrossed;
	private static List<CDestructable> tombThirdRocks = List.of();
	private static int tombRetreatStep = -1;
	private static Set<Integer> originalIslandTransports = Set.of();
	private static boolean afterTombMaievVerified;
	private static boolean afterTombBoatsVerified;
	private static CUnit runnerTransport;
	private static boolean runnerCausewaySecured;
	private static boolean runnerRidgeSecured;
	private static boolean runnerBoarded;
	private static boolean runnerSouthGateCleared;
	private static final Map<Integer, Integer> southernFerryWaypoints = new HashMap<>();
	private static boolean runnerSeaGatesCleared;
	private static int runnerSeaBlinkStep;
	private static boolean runnerSeaScoutReady;
	private static int runnerGateScoutStep;
	private static int runnerGateScoutRefuge = -1;
	private static CUnit runnerEscortTransport;
	private static CUnit runnerReservedSiege;
	private static List<CUnit> runnerGateSiege = List.of();
	private static int runnerRidgeRefuge = -1;
	private static int runnerTurnStep;
	private static float[] runnerPierBypass;
	private static boolean runnerLandingCleared;
	private static boolean runnerEscortAboard;
	private static boolean runnerEscortLanded;
	private static int runnerEscortBlinkStep = RESCUE_BOAT_BLINK_ROUTE.length - 2;
	private static int boatRescueRefuge = -1;
	private static int boatRescueBlinkStep;
	private static final Map<Integer, Integer> boatRescueSteps = new HashMap<>();
	private static final Map<Integer, Integer> runnerRescueSteps = new HashMap<>();

	private FrozenThroneOpeningPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final var game = viewer.simulation;
		final GlobalScope globals = game.getGlobalScope();
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		final CUnit maiev = object(globals, "udg_Maiev");
		if (AFTER_TOMB) {
			check(map.endsWith("/nightelfx04.w3x"), "unexpected map after the tomb " + map);
			verifyBoatRescue(viewer, ui, globals, maiev);
			return;
		}
		if (TOMB_OPENING) {
			if (TOMB_ESCAPE && map.endsWith("/nightelfx04.w3x")) {
				if (maiev == null) return;
				check(tombEscapeCompleted && tombEscapeCarriedMaiev != null && tombEscapeCarriedMaiev.equals(snapshot(maiev)),
						"Maiev carryover after escaping the Tomb of Sargeras mismatch");
				System.out.println("[FrozenThroneOpeningProbe] entered NightElfX04 and verified Maiev carryover after escaping the tomb");
				finished = true;
				Gdx.app.exit();
				return;
			}
			check(map.endsWith("/nightelfx03.w3x"), "unexpected tomb map " + map);
			verifyTombOpening(viewer, ui, globals, maiev);
			return;
		}
		if (ISLAND_OPENING) {
			if (ISLAND_PLAYTHROUGH && map.endsWith("/nightelfx03.w3x")) {
				if (maiev == null) return;
				check(carriedMaiev != null && carriedMaiev.equals(snapshot(maiev)), "Maiev carryover into the Tomb of Sargeras mismatch");
				System.out.println("[FrozenThroneOpeningProbe] entered NightElfX03 and verified Maiev carryover");
				finished = true;
				Gdx.app.exit();
				return;
			}
			check(map.endsWith("/nightelfx02.w3x"), "unexpected island map " + map);
			verifyIslandOpening(viewer, ui, globals, maiev);
			return;
		}
		if (map.endsWith("/nightelfx02.w3x")) {
			if (maiev == null) return;
			check(carriedMaiev != null && carriedMaiev.equals(snapshot(maiev)), "Maiev carryover mismatch");
			System.out.println("[FrozenThroneOpeningProbe] entered NightElfX02 and verified Maiev carryover");
			finished = true;
			Gdx.app.exit();
			return;
		}
		check(map.endsWith("/nightelfx01.w3x"), "unexpected map " + map);
		final CQuest pursuit = object(globals, "udg_QuestMainIllidan");
		final CQuest ships = object(globals, "udg_QuestSaveShips");
		if (maiev == null || pursuit == null || ships == null) return;
		check(!maiev.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()), "native defeat");
		if (ships.isCompleted()) {
			check(harborReached && fanOrdered, "victory missed the pursuit or Maiev's opening spell");
			if (carriedMaiev == null) {
				carriedMaiev = snapshot(maiev);
				System.out.println("[FrozenThroneOpeningProbe] completed the original ship defense");
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final int tick = game.getGameTurnTick();
		check(tick < 60_000, "the original objectives stalled");
		if (!ui.isUserControlEnabled() || maiev.isPaused() || maiev.isHidden() || game.isGamePaused() || tick < orderTick) return;
		orderTick = tick + 12;
		if (waypoint < 0) {
			check(maiev.getAbilityLevel(FAN_OF_KNIVES) > 0, "the original opening did not teach Fan of Knives");
			if (RESUME_HARBOR) {
				check(pursuit.isCompleted() && ships.isDiscovered(), "the saved mission has not reached the harbor");
				waypoint = HARBOR_ROUTE.length - 1;
				harborWaypoint = HARBOR_ASSAULT_ROUTE.length;
				harborReached = fanOrdered = true;
				System.out.println("[FrozenThroneOpeningProbe] reached Illidan's harbor and opened the original ship defense in the restored mission");
			}
			else waypoint = VILLAGE_APPROACH;
			progressTick = tick;
			resetRefuges(maiev);
		}
		if (!easternLandingReached && maiev.getX() > 1500 && maiev.getY() < -6500) {
			easternLandingReached = true;
			waypoint = 0;
			closestApproach = Double.POSITIVE_INFINITY;
			progressTick = tick;
			resetRefuges(maiev);
		}
		final int player = maiev.getPlayerIndex();
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		if (maiev.getHeroData().getSkillPoints() > 0) {
			final War3ID skill = maiev.getAbilityLevel(BLINK) == 0 ? BLINK : FAN_OF_KNIVES;
			HumanCampaignPlaythroughProbe.immediate(viewer, orders, maiev, skill.getValue());
		}
		if (maiev.getLife() < maiev.getMaximumLife() * 0.60f
				&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "pghe")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "shea"))) return;
		final var army = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
				&& !unit.isHidden() && !unit.isPaused() && !unit.getCurrentAttacks().isEmpty()).toList();
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& unit.getPlayerIndex() != player && !game.getPlayer(player).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, player)).toList();
		final CUnit nearbyEnemy = enemies.stream().filter(unit -> maiev.distanceSquaredNoCollision(unit) < 560 * 560)
				.min(Comparator.comparingDouble(maiev::distanceSquaredNoCollision)).orElse(null);
		final float health = maiev.getLife() / maiev.getMaximumLife();
		if (!retreating && nearbyEnemy == null && health > 0.85f) {
			if (maiev.distanceSquaredNoCollision(forwardRefugeX, forwardRefugeY) > 430 * 430) {
				rearRefugeX = forwardRefugeX;
				rearRefugeY = forwardRefugeY;
				forwardRefugeX = maiev.getX();
				forwardRefugeY = maiev.getY();
			}
			refugeX = maiev.getX();
			refugeY = maiev.getY();
		}
		final boolean wasRetreating = retreating;
		final boolean fightingOffshore = offshoreDefender != null && !offshoreDefender.isDead();
		retreating = fightingOffshore ? health < (retreating ? 0.96f : 0.79f)
				|| maiev.getMana() < (retreating && maiev.getX() < 2200 ? 210 : 54)
				: health < (retreating ? 0.86f : waypoint >= NAGA_APPROACH ? 0.78f : 0.73f);
		if (wasRetreating && !retreating) {
			if (harborReached) {
				if (!fightingOffshore) harborWaypoint = Math.min(harborWaypoint, harborRetreatWaypoint + 1);
			}
			else if (waypoint >= NAGA_APPROACH) {
				waypoint = Math.min(waypoint, retreatWaypoint + 1);
				closestApproach = Double.POSITIVE_INFINITY;
				progressTick = tick;
			}
		}
		if (retreating && harborReached) {
			if (!wasRetreating) harborRetreatWaypoint = Math.max(0, harborWaypoint - 1);
			final float[] refuge = HARBOR_ASSAULT_ROUTE[harborRetreatWaypoint];
			if (nearbyEnemy != null && harborRetreatWaypoint > 0
					&& maiev.distanceSquaredNoCollision(refuge[0], refuge[1]) < 250 * 250) {
				harborRetreatWaypoint--;
			}
			refugeX = HARBOR_ASSAULT_ROUTE[harborRetreatWaypoint][0];
			refugeY = HARBOR_ASSAULT_ROUTE[harborRetreatWaypoint][1];
			if (harborRetreatWaypoint == 0 && health < 0.55f) {
				refugeX = HARBOR_ROUTE[HARBOR_ROUTE.length - 1][0];
				refugeY = HARBOR_ROUTE[HARBOR_ROUTE.length - 1][1];
				if (health < 0.40f && maiev.getX() < -1500) {
					refugeX = HARBOR_ROUTE[HARBOR_ROUTE.length - 3][0];
					refugeY = HARBOR_ROUTE[HARBOR_ROUTE.length - 3][1];
				}
			}
		}
		else if (retreating && waypoint >= NAGA_APPROACH) {
			if (!wasRetreating) retreatWaypoint = waypoint - 2;
			if (nearbyEnemy != null && retreatWaypoint > 14
					&& maiev.distanceSquaredNoCollision(HARBOR_ROUTE[retreatWaypoint][0], HARBOR_ROUTE[retreatWaypoint][1]) < 270 * 270) {
				retreatWaypoint--;
			}
			refugeX = HARBOR_ROUTE[retreatWaypoint][0];
			refugeY = HARBOR_ROUTE[retreatWaypoint][1];
		}
		else if (retreating && nearbyEnemy != null && maiev.distanceSquaredNoCollision(refugeX, refugeY) < 260 * 260
				&& maiev.distanceSquaredNoCollision(rearRefugeX, rearRefugeY) > 250 * 250) {
			refugeX = rearRefugeX;
			refugeY = rearRefugeY;
		}
		if (!retreating && nearbyEnemy == null) {
			final var healing = game.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
					&& item.getContainedInventory() == null && maiev.distanceSquaredNoCollision(item) < 460 * 460
					&& List.of("phea", "pghe", "shea").contains(item.getTypeId().toString()))
					.min(Comparator.comparingDouble(maiev::distanceSquaredNoCollision)).orElse(null);
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.smart, healing)) return;
		}
		if (enemies.stream().filter(unit -> maiev.distanceSquaredNoCollision(unit) < 370 * 370).count()
				>= (retreating ? 3 : 2) && health > 0.30f
				&& (maiev.getCurrentBehavior() == null || maiev.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.SPELL)
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, maiev, OrderIds.fanofknives)) fanOrdered = true;
		if (ships.isDiscovered() && pursuit.isCompleted() && !harborReached) {
			harborReached = true;
			System.out.println("[FrozenThroneOpeningProbe] reached Illidan's harbor and opened the original ship defense");
		}
		if (harborReached && !retreating && harborWaypoint < HARBOR_ASSAULT_ROUTE.length) {
			final float[] landing = HARBOR_ASSAULT_ROUTE[harborWaypoint];
			final long inPosition = army.stream().filter(unit -> unit != maiev
					&& unit.distanceSquaredNoCollision(landing[0], landing[1]) < 230 * 230).count();
			if (inPosition >= Math.min(army.size() - 1, 2)
					&& (army.size() > 1 || maiev.distanceSquaredNoCollision(landing[0], landing[1]) < 160 * 160)) {
				harborWaypoint++;
			}
		}
		final List<CUnit> defenders = object(globals, "udg_NagaVictoryGroup");
		final boolean inHarbor = harborReached && harborWaypoint >= HARBOR_ASSAULT_ROUTE.length;
		if (inHarbor && fanOrdered && SAVE_HARBOR != null) {
			try {
				game.getMissionCheckpoint().save(new File(SAVE_HARBOR));
			}
			catch (final IOException exception) {
				throw new IllegalStateException("Frozen Throne opening audit: unable to save the original harbor", exception);
			}
			System.out.println("[FrozenThroneOpeningProbe] saved the original harbor approach");
			finished = true;
			Gdx.app.exit();
			return;
		}
		if (inHarbor && defenders != null && (harborTarget == null || harborTarget.isDead() || harborTarget.isHidden())) {
			harborTarget = defenders.stream().filter(unit -> !unit.isDead() && !unit.isHidden())
					.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(624, 3312))).orElse(null);
			harborCombatWaypoints.clear();
		}
		final CUnit objectiveEnemy = inHarbor ? harborTarget : harborReached && defenders != null
				? defenders.stream().filter(unit -> !unit.isDead() && !unit.isHidden())
						.min(Comparator.comparingDouble(maiev::distanceSquaredNoCollision)).orElse(null) : null;
		if (inHarbor && offshoreDefender == null && objectiveEnemy != null && objectiveEnemy.getX() > 2550
				&& objectiveEnemy.getY() > 4200 && objectiveEnemy.getY() < 5200) offshoreDefender = objectiveEnemy;
		final boolean islandDefender = objectiveEnemy != null && objectiveEnemy == offshoreDefender;
		if (islandDefender && maiev.getX() > 2200) maievCrossed = true;
		final float[][] harborCombatRoute = objectiveEnemy != null && objectiveEnemy.getX() < 700
				&& objectiveEnemy.getY() > 4070 ? INNER_HARBOR_ROUTE
				: objectiveEnemy != null && objectiveEnemy.getX() > 2000 && objectiveEnemy.getY() < 3250 ? EAST_HARBOR_ROUTE
						: islandDefender ? NORTH_HARBOR_ROUTE : null;
		final float[] destination = HARBOR_ROUTE[waypoint];
		if (retreating) progressTick = tick;
		else if (!harborReached && waypoint < HARBOR_ROUTE.length - 1) {
			final double distance = maiev.distanceSquaredNoCollision(destination[0], destination[1]);
			if (distance <= 140 * 140) {
				waypoint++;
				closestApproach = Double.POSITIVE_INFINITY;
				progressTick = tick;
			}
			else if (distance < closestApproach - 32 * 32) {
				closestApproach = distance;
				progressTick = tick;
			}
			else check(tick - progressTick < 3000, "the pursuit stalled at waypoint " + waypoint);
		}
		final String gateName = easternLandingReached && waypoint <= 1 ? "gg_dest_LTe1_1140" : "gg_dest_ATg4_0111";
		final CDestructable gate = object(globals, gateName);
		final float[] shore = harborReached && harborWaypoint < HARBOR_ASSAULT_ROUTE.length
				? HARBOR_ASSAULT_ROUTE[harborWaypoint] : null;
		final float x = shore != null ? shore[0] : objectiveEnemy == null ? HARBOR_ROUTE[waypoint][0] : objectiveEnemy.getX();
		final float y = shore != null ? shore[1] : objectiveEnemy == null ? HARBOR_ROUTE[waypoint][1] : objectiveEnemy.getY();
		final long nearbyEscort = army.stream().filter(unit -> unit != maiev
				&& maiev.distanceSquaredNoCollision(unit) < 380 * 380).count();
		final boolean nagaApproach = !harborReached && waypoint >= NAGA_APPROACH && waypoint <= NAGA_APPROACH + 3;
		if (waypoint == NAGA_APPROACH && !nagaEscortReady) {
			final long nearbyAllies = army.stream().filter(unit -> unit != maiev
					&& maiev.distanceSquaredNoCollision(unit) < 500 * 500).count();
			nagaEscortReady = nearbyAllies >= Math.min(army.size() - 1, 5);
		}
		final boolean assemblingEscort = waypoint == NAGA_APPROACH && !nagaEscortReady && tick - progressTick < 1400;
		final int nagaRallyWaypoint = Math.max(0, waypoint - 1);
		final float[] nagaRally = HARBOR_ROUTE[nagaRallyWaypoint];
		final float marchX = x - maiev.getX();
		final float marchY = y - maiev.getY();
		final float marchDistance = Math.max(1f, (float) Math.hypot(marchX, marchY));
		final float[] harborEntrance = HARBOR_ROUTE[HARBOR_ROUTE.length - 1];
		for (int index = 0; index < army.size(); index++) {
			final CUnit unit = army.get(index);
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (islandDefender && unit == maiev) {
				fightIslandDefender(viewer, orders, maiev, objectiveEnemy, retreating);
				continue;
			}
			if (islandDefender && unit != maiev) {
				final int range = unit.getCurrentAttacks().stream().mapToInt(attack -> attack.getRange()).max().orElse(0);
				if (!unit.canReach(objectiveEnemy, range)
						|| !CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, objectiveEnemy)) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, NORTH_HARBOR_ROUTE,
							maievCrossed ? NORTH_HARBOR_ROUTE.length - 2 : NORTH_HARBOR_ROUTE.length - 4, 140,
							harborCombatWaypoints);
				}
				continue;
			}
			if (retreating && unit == maiev) {
				if (!harborReached && nearbyEnemy != null && maiev.distanceSquaredNoCollision(refugeX, refugeY) > 230 * 230
						&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, refugeX, refugeY)) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, refugeX, refugeY);
				continue;
			}
			if (assemblingEscort && unit == maiev && nearbyEnemy == null) {
				HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.holdposition);
				continue;
			}
			if (nagaApproach && !retreating && unit != maiev
					&& unit.distanceSquaredNoCollision(nagaRally[0], nagaRally[1]) > 675 * 675
					&& unit.distanceSquaredNoCollision(maiev) > 520 * 520) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, HARBOR_ROUTE, nagaRallyWaypoint,
						200, escortWaypoints);
				continue;
			}
			if (harborReached && unit != maiev && (unit.getY() < 2350 || unit.getX() < harborEntrance[0]
					&& unit.distanceSquaredNoCollision(harborEntrance[0], harborEntrance[1]) > 1000 * 1000)) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, HARBOR_ROUTE, HARBOR_ROUTE.length - 1,
						180, escortWaypoints);
				continue;
			}
			final int ranged = unit.getCurrentAttacks().stream().mapToInt(attack -> attack.getRange()).max().orElse(0);
			if (unit != maiev && ranged >= 300 && unit.getLife() < unit.getMaximumLife() * 0.55f) {
				final float safeX = retreating ? refugeX : rearRefugeX;
				final float safeY = retreating ? refugeY : rearRefugeY;
				if (game.isNight() && (unit.getCurrentBehavior() != null
						&& unit.getCurrentBehavior().getHighlightOrderId() == OrderIds.ambush
						|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.ambush))) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, safeX, safeY);
				continue;
			}
			if (gate != null && !gate.isDead() && unit.distanceSquaredNoCollision(gate) < 520 * 520
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, gate)) continue;
			final CUnit enemy = objectiveEnemy != null && unit.distanceSquaredNoCollision(objectiveEnemy) < 660 * 660
					? objectiveEnemy : enemies.stream().filter(candidate -> unit.distanceSquaredNoCollision(candidate) < 560 * 560)
							.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (enemy != null && (unit != maiev || nearbyEscort > 0 && maiev.distanceSquaredNoCollision(enemy) < 230 * 230)
					&& (ranged < 300 || unit.canReach(enemy, ranged))
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (inHarbor && !retreating && harborCombatRoute != null && objectiveEnemy != null
					&& !unit.canReach(objectiveEnemy, ranged)) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, harborCombatRoute,
						harborCombatRoute.length - 1, 140, harborCombatWaypoints);
				continue;
			}
			if (unit == maiev) {
				if (shore != null && army.size() > 1) {
					final float[] support = harborWaypoint == 0 ? HARBOR_ROUTE[HARBOR_ROUTE.length - 1]
							: HARBOR_ASSAULT_ROUTE[harborWaypoint - 1];
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, support[0], support[1]);
				}
				else if (waypoint >= NAGA_APPROACH && nearbyEnemy != null && nearbyEscort < Math.min(army.size() - 1, 2)) {
					HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.holdposition);
				}
				else CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, x, y);
			}
			else if (!harborReached && ranged >= 300 && !retreating && !assemblingEscort && nearbyEnemy != null) {
				final float behind = 190 + index / 3 * 56;
				final float flank = (index % 3 - 1) * 70;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit,
						maiev.getX() - (marchX * behind + marchY * flank) / marchDistance,
						maiev.getY() - (marchY * behind - marchX * flank) / marchDistance);
			}
			else if (shore != null && !retreating) {
				// Move through the narrow shore route without stopping for enemies across the water.
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, x, y);
			}
			else {
				final float rallyX = retreating || assemblingEscort ? refugeX : x;
				final float rallyY = retreating || assemblingEscort ? refugeY : y;
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, rallyX + (index % 3 - 1) * 70,
						rallyY + index / 3 * 74);
			}
		}
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
		if (tick >= reportTick) {
			reportTick = tick + 320;
			System.out.printf("[FrozenThroneOpeningProbe] tick=%d waypoint=%d Maiev=%.0f,%.0f life=%.0f allies=%d retreat=%s"
					+ " refuge=%.0f,%.0f enemy=%s%n", tick, waypoint, maiev.getX(), maiev.getY(), maiev.getLife(),
					army.size(), retreating, refugeX, refugeY, nearbyEnemy == null ? "none" : nearbyEnemy.getTypeId());
			if (harborReached && defenders != null) {
				System.out.printf("[FrozenThroneOpeningProbe] target=%s Blink=%d mana=%.0f order=%s%n",
						objectiveEnemy == null ? "none" : objectiveEnemy.getTypeId() + "@" + objectiveEnemy.getX()
								+ "," + objectiveEnemy.getY(), maiev.getAbilityLevel(BLINK), maiev.getMana(),
						maiev.getCurrentOrder());
				System.out.println("[FrozenThroneOpeningProbe] harbor approach=" + harborWaypoint + " defenders="
						+ defenders.stream().filter(unit -> !unit.isDead()).limit(6).map(unit -> unit.getTypeId()
								+ " at " + Math.round(unit.getX()) + "," + Math.round(unit.getY())
									+ " life " + Math.round(unit.getLife()) + (!game.containsUnit(unit) ? " removed"
											: unit.isHidden() ? " hidden" : "")).toList());
				System.out.println("[FrozenThroneOpeningProbe] shore escort=" + army.stream().filter(unit -> unit != maiev)
						.map(unit -> unit.getTypeId() + " at " + Math.round(unit.getX()) + "," + Math.round(unit.getY())
								+ " life " + Math.round(unit.getLife())).toList());
			}
		}
	}

	private static void verifyTombOpening(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		check(tick < (TOMB_EYE ? 120_000 : TOMB_THIRD_BARRIER ? 82_000
				: TOMB_SECOND_BARRIER ? 58_000 : TOMB_FIRST_BARRIER ? 28_000 : 8000),
				"the original Tomb of Sargeras opening stalled");
		if (tombEyeReached) {
			verifyTombEscape(viewer, ui, globals, maiev);
			return;
		}
		if (maiev == null || !ui.isUserControlEnabled() || maiev.isPaused() || game.isGamePaused() || tick < orderTick) return;
		check(!maiev.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()), "native defeat in the Tomb of Sargeras");
		orderTick = tick + 16;
		final CQuest illidan = object(globals, "udg_QuestIllidan");
		if (illidan == null || !illidan.isDiscovered()) return;
		if (carriedMaiev == null) {
			final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
			final var stored = cache == null ? null : cache.getStoredUnit("NightElfX03", "Maiev");
			carriedMaiev = snapshot(maiev);
			check(stored != null && CampaignPlaythroughProbe.heroProperties(stored).equals(carriedMaiev),
					"the Tomb of Sargeras did not restore earned Maiev after the introduction");
			System.out.println("[FrozenThroneOpeningProbe] watched the original tomb introduction and restored earned Maiev");
		}
		if (tombBarriersCleared) {
			verifyTombEye(viewer, ui, globals, maiev, illidan);
			return;
		}
		if (tombSecondBarrierCleared) {
			verifyTombThirdBarrier(viewer, ui, globals, maiev);
			return;
		}
		if (tombFirstBarrierCleared) {
			verifyTombSecondBarrier(viewer, ui, globals, maiev);
			return;
		}
		if (tombBlinkRuneUsed) {
			verifyTombFirstBarrier(viewer, ui, globals, maiev);
			return;
		}
		final Trigger blinkHint = object(globals, "gg_trg_Hint_Blink_Intro_Event");
		if (blinkHint != null && !blinkHint.isEnabled()) {
			verifyTombBlink(viewer, maiev);
			return;
		}
		final var player = game.getPlayer(maiev.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		final var escort = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId()
				&& !unit.isDead() && !unit.isHidden() && !unit.getCurrentAttacks().isEmpty()).toList();
		for (final CUnit unit : escort) {
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, TOMB_INTRO_ROUTE, TOMB_INTRO_ROUTE.length - 1,
					112, escortWaypoints);
		}
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
		if (tick >= reportTick) {
			reportTick = tick + 260;
			System.out.printf("[FrozenThroneOpeningProbe] Maiev in the tomb %.0f,%.0f life=%.0f escort=%d%n",
					maiev.getX(), maiev.getY(), maiev.getLife(), escort.size());
		}
	}

	private static void verifyTombBlink(final War3MapViewer viewer, final CUnit maiev) {
		final var game = viewer.simulation;
		if (tombBlinkRune == null) {
			tombBlinkRune = game.getItems().stream().filter(item -> "rman".equals(item.getTypeId().toString())
					&& item.distanceSquaredNoCollision(-2584, 355) < 24 * 24).findFirst().orElse(null);
			check(tombBlinkRune != null, "the original Blink hint's Rune of Mana is missing");
			System.out.println("[FrozenThroneOpeningProbe] reached the original Tomb of Sargeras Blink hint");
		}
		if (tombBlinkRune.isDead()) {
			check(maiev.getY() > 200 && maiev.distanceSquaredNoCollision(-2584, 355) < 180 * 180,
					"the rune was used without reaching the isolated ledge");
			System.out.println("[FrozenThroneOpeningProbe] used Blink to claim the original Rune of Mana");
			tombBlinkRuneUsed = true;
			escortWaypoints.clear();
			if (!TOMB_FIRST_BARRIER) {
				finished = true;
				Gdx.app.exit();
			}
			return;
		}
		final var orders = game.getDefaultPlayerUnitOrderExecutor(maiev.getPlayerIndex());
		final boolean orderAccepted;
		if (maiev.getY() > 200) {
			orderAccepted = CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.smart, tombBlinkRune);
		}
		else if (maiev.distanceSquaredNoCollision(-2608, 336) < 930 * 930) {
			orderAccepted = CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, -2608, 336);
		}
		else {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, -2736, -336);
			orderAccepted = maiev.getCurrentOrder() != null;
		}
		final int tick = game.getGameTurnTick();
		if (tick >= reportTick) {
			reportTick = tick + 240;
			System.out.printf("[FrozenThroneOpeningProbe] Blink ledge at tick=%d: Maiev %.0f,%.0f mana=%.0f level=%d order=%s accepted=%s rune=%.0f,%.0f%n",
					tick, maiev.getX(), maiev.getY(), maiev.getMana(), maiev.getAbilityLevel(BLINK),
					maiev.getCurrentOrder() == null ? "none" : maiev.getCurrentOrder().getOrderId(), orderAccepted,
					tombBlinkRune.getX(), tombBlinkRune.getY());
		}
	}

	private static void verifyTombFirstBarrier(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		final var game = viewer.simulation;
		final var player = game.getPlayer(maiev.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		if (maiev.getY() > 200 && tombDemonicGate == null) {
			CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, -2736, -336);
			return;
		}
		if (tombDemonicGate == null) {
			tombDemonicGate = game.getDestructables().stream().filter(gate -> "ATg3".equals(gate.getTypeId().toString())
					&& gate.distanceSquaredNoCollision(-3840, -1792) < 32 * 32).findFirst().orElse(null);
			check(tombDemonicGate != null && !tombDemonicGate.isDead(), "the original Demonic Gate is missing");
			tombFirstRocks = rocksInArea(viewer, object(globals, "gg_rct_R1_Rocks"));
			check(!tombFirstRocks.isEmpty(), "the first authored rock barrier is missing");
		}
		final CQuest barriers = object(globals, "udg_QuestBarriers");
		if (barriers != null && barriers.isDiscovered() && tombFirstRocks.stream().allMatch(CDestructable::isDead)
				&& rescuedTombGroup(globals, "udg_R1", maiev.getPlayerIndex())
				&& Boolean.TRUE.equals(globals.getGlobal("udg_R1Rescued").visit(BooleanJassValueVisitor.getInstance()))) {
			check(tombDemonicGate.isDead(), "the first rock barrier was reached without opening the original Demonic Gate");
			System.out.println("[FrozenThroneOpeningProbe] destroyed the first original rock barrier and freed the Watchers");
			tombFirstBarrierCleared = true;
			escortWaypoints.clear();
			if (!TOMB_SECOND_BARRIER) {
				finished = true;
				Gdx.app.exit();
			}
			return;
		}
		final int end = tombDemonicGate.isDead() ? TOMB_FIRST_BARRIER_ROUTE.length - 1 : TOMB_DEMONIC_GATE_APPROACH;
		final var obstacles = tombDemonicGate.isDead() ? tombFirstRocks : List.of(tombDemonicGate);
		advanceTombExpedition(viewer, ui, maiev, TOMB_FIRST_BARRIER_ROUTE, end, obstacles);
	}

	private static void verifyTombSecondBarrier(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		if (tombSecondRocks.isEmpty()) {
			tombSecondRocks = rocksInArea(viewer, object(globals, "gg_rct_R2_Rocks"));
			check(!tombSecondRocks.isEmpty(), "the second authored rock barrier is missing");
		}
		final CDestructable slimeDoor = object(globals, "udg_SlimeDoor");
		if (!tombFloorSwitchActivated && slimeDoor != null && slimeDoor.isDead()) {
			for (final String suffix : List.of("A", "B", "C", "D")) {
				final CDestructable plate = object(globals, "udg_FloorPlate01" + suffix);
				check(plate != null && plate.isDead(), "the original floor switch did not activate");
			}
			tombFloorSwitchActivated = true;
			System.out.println("[FrozenThroneOpeningProbe] the original floor switch opened the lower tomb passage");
		}
		if (tombFloorSwitchActivated && tombSecondRocks.stream().allMatch(CDestructable::isDead)
				&& rescuedTombGroup(globals, "udg_R2", maiev.getPlayerIndex())
				&& Boolean.TRUE.equals(globals.getGlobal("udg_R2Rescued").visit(BooleanJassValueVisitor.getInstance()))) {
			System.out.println("[FrozenThroneOpeningProbe] destroyed the second original rock barrier and freed the Dryads");
			tombSecondBarrierCleared = true;
			escortWaypoints.clear();
			if (!TOMB_THIRD_BARRIER) {
				finished = true;
				Gdx.app.exit();
			}
			return;
		}
		final int end = tombFloorSwitchActivated ? TOMB_SECOND_BARRIER_ROUTE.length - 1 : TOMB_SWITCH_APPROACH;
		advanceTombExpedition(viewer, ui, maiev, TOMB_SECOND_BARRIER_ROUTE, end, tombSecondRocks);
	}

	private static void verifyTombThirdBarrier(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		final var game = viewer.simulation;
		final CDestructable bridge = object(globals, "udg_Bridge");
		check(bridge != null, "the original tomb bridge is missing");
		tombBridgeWasDown |= bridge.isDead();
		if (!tombBridgeRaised) {
			if (tombBridgeWasDown && !bridge.isDead()) {
				check(maiev.getX() > 500, "the bridge rose before Maiev crossed the river");
				tombBridgeRaised = true;
				escortWaypoints.clear();
				tombThirdRocks = rocksInArea(viewer, object(globals, "gg_rct_R3_Rocks"));
				check(!tombThirdRocks.isEmpty(), "the third authored rock barrier is missing");
				System.out.println("[FrozenThroneOpeningProbe] Maiev blinked across the river and raised the original tomb bridge");
			}
			else {
				if (maiev.getX() > 150) {
					final var orders = game.getDefaultPlayerUnitOrderExecutor(maiev.getPlayerIndex());
					CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, 544, 4640);
				}
				else if (maiev.getX() > -494 && maiev.distanceSquaredNoCollision(-400, 4560) < 156 * 156) {
					final var orders = game.getDefaultPlayerUnitOrderExecutor(maiev.getPlayerIndex());
					CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, 544, 4640);
				}
				else {
					advanceTombExpedition(viewer, ui, maiev, TOMB_WEST_BRIDGE_ROUTE, TOMB_WEST_BRIDGE_ROUTE.length - 1, List.of());
				}
				return;
			}
		}
		tombBridgePartyCrossed |= game.getUnits().stream().anyMatch(unit -> unit != maiev
				&& unit.getPlayerIndex() == maiev.getPlayerIndex() && !unit.isDead() && !unit.getCurrentAttacks().isEmpty()
				&& unit.getX() > 400 && unit.getY() > 4200);
		final CQuest barriers = object(globals, "udg_QuestBarriers");
		if (tombBridgePartyCrossed && barriers != null && barriers.isCompleted()
				&& tombThirdRocks.stream().allMatch(CDestructable::isDead) && rescuedTombGroup(globals, "udg_R3", maiev.getPlayerIndex())
				&& Boolean.TRUE.equals(globals.getGlobal("udg_R3Rescued").visit(BooleanJassValueVisitor.getInstance()))) {
			System.out.println("[FrozenThroneOpeningProbe] crossed the original tomb bridge, rescued the third party and completed the barrier quest");
			if (TOMB_EYE) {
				tombBarriersCleared = true;
				return;
			}
			finished = true;
			Gdx.app.exit();
			return;
		}
		final int end = tombBridgePartyCrossed ? TOMB_THIRD_BARRIER_ROUTE.length - 1 : TOMB_BRIDGE_RALLY;
		advanceTombExpedition(viewer, ui, maiev, TOMB_THIRD_BARRIER_ROUTE, end, tombThirdRocks);
	}

	private static void verifyTombEye(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev, final CQuest illidan) {
		if (tombEyeGates == null) {
			final CDestructable eyeGate = object(globals, "gg_dest_ZTsg_0915");
			check(eyeGate != null, "the original Eye chamber gate is missing");
			tombEyeGates = List.of(tombGate(viewer, "ZTg1", 7168, 1984), tombGate(viewer, "ZTg3", 6144, 768),
					tombGate(viewer, "ZTsg", 4160, -2176), eyeGate);
			tombEyeApproach = Arrays.copyOf(TOMB_THIRD_BARRIER_ROUTE,
					TOMB_THIRD_BARRIER_ROUTE.length + TOMB_EYE_ROUTE.length);
			System.arraycopy(TOMB_EYE_ROUTE, 0, tombEyeApproach, TOMB_THIRD_BARRIER_ROUTE.length, TOMB_EYE_ROUTE.length);
			maiev.addDamageTakenListener((game, attacker, target, attack, ranged, type, damage, bonus, actual) -> {
				if (actual > 0 && target.getLife() < target.getMaximumLife() * 0.34f) {
					System.out.printf("[FrozenThroneOpeningProbe] Maiev hurt near the Eye %.0f,%.0f by %s life=%.0f mana=%.0f%n",
							target.getX(), target.getY(), attacker == null ? type : attacker.getTypeId(), target.getLife(), target.getMana());
				}
			});
		}
		final CQuest escape = object(globals, "udg_QuestEscape");
		final CTimer timer = object(globals, "udg_EscapeTimer");
		if (illidan.isCompleted() && escape != null && escape.isDiscovered() && timer != null && timer.isRunning()
				&& Boolean.TRUE.equals(globals.getGlobal("udg_Escaping").visit(BooleanJassValueVisitor.getInstance()))) {
			check(tombEyeGates.stream().allMatch(CDestructable::isDead), "the party bypassed an original Tomb gate");
			System.out.println("[FrozenThroneOpeningProbe] broke through the original Eye chamber, watched Illidan's cinematic and started the timed escape");
			if (TOMB_ESCAPE) {
				tombEyeReached = true;
				return;
			}
			finished = true;
			Gdx.app.exit();
			return;
		}
		if (!tombEyeGates.get(tombEyeGates.size() - 1).isDead()) {
			final var canLearn = new BooleanAbilityActivationReceiver();
			final var game = viewer.simulation;
			final War3ID skill = maiev.getAbilityLevel(BLINK) < 2 && maiev.getHeroData().getHeroLevel() >= 3
					|| maiev.getAbilityLevel(SHADOW_STRIKE) > 0 ? BLINK : SHADOW_STRIKE;
			maiev.getHeroData().checkCanUse(game, maiev, skill.getValue(), canLearn);
			if (canLearn.isOk()) {
				game.getDefaultPlayerUnitOrderExecutor(maiev.getPlayerIndex()).issueImmediateOrder(maiev.getHandleId(),
						maiev.getHeroData().getHandleId(), skill.getValue(), false);
				System.out.printf("[FrozenThroneOpeningProbe] learned %s level %d for the original Eye chamber%n",
						skill, maiev.getAbilityLevel(skill));
			}
			advanceTombExpedition(viewer, ui, maiev, tombEyeApproach, tombEyeApproach.length - 1, tombEyeGates);
		}
	}

	private static void verifyTombEscape(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		final var game = viewer.simulation;
		final CTimer timer = object(globals, "udg_EscapeTimer");
		final CQuest escape = object(globals, "udg_QuestEscape");
		check(!maiev.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()), "native defeat during the timed Tomb escape");
		if (escape.isCompleted()) {
			if (!tombEscapeCompleted) {
				check(tombEscapeRiverCrossed && !timer.isRunning() && timer.getRemaining(game) > 0,
						"the original escape did not finish before the countdown");
				tombEscapeCompleted = true;
				System.out.println("[FrozenThroneOpeningProbe] reached the original Tomb exit before the timer expired");
			}
			final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
			final var stored = cache == null ? null : cache.getStoredUnit("NightElfX04", "Maiev");
			if (stored != null) tombEscapeCarriedMaiev = CampaignPlaythroughProbe.heroProperties(stored);
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final int tick = game.getGameTurnTick();
		if (!ui.isUserControlEnabled() || maiev.isPaused() || game.isGamePaused() || tick < orderTick) return;
		orderTick = tick + 8;
		final var orders = game.getDefaultPlayerUnitOrderExecutor(maiev.getPlayerIndex());
		if (!tombEscapeRiverCrossed && maiev.distanceSquaredNoCollision(-400, 4560) < 120 * 120) {
			tombEscapeRiverCrossed = true;
			tombEscapeSteps.clear();
			System.out.println("[FrozenThroneOpeningProbe] Maiev blinked back across the river after the tomb bridge collapsed");
		}
		if (tick >= reportTick) {
			reportTick = tick + 200;
			System.out.printf("[FrozenThroneOpeningProbe] escape %.0f,%.0f life=%.0f mana=%.0f step=%d remaining=%.1f%n",
					maiev.getX(), maiev.getY(), maiev.getLife(), maiev.getMana(),
					tombEscapeSteps.getOrDefault(maiev.getHandleId(), 0), timer.getRemaining(game));
		}
		if (maiev.getCurrentBehavior() != null && maiev.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) return;
		if (maiev.getLife() < maiev.getMaximumLife() * 0.7f
				&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "pghe"))) return;
		if (!tombEscapeRiverCrossed && maiev.distanceSquaredNoCollision(-400, 4560) < 980 * 980
				&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, -400, 4560)) return;
		final float[][] route = tombEscapeRiverCrossed ? TOMB_ESCAPE_WEST : TOMB_ESCAPE_EAST;
		if (tombEscapeRune != null && (tombEscapeRune.isDead() || tick >= tombEscapeRuneDeadline)) {
			if (tombEscapeRune.isDead()) {
				System.out.printf("[FrozenThroneOpeningProbe] collected original escape rune %s%n", tombEscapeRune.getTypeId());
			}
			tombEscapeRune = null;
		}
		if (tombEscapeRune == null) {
			for (final String suffix : List.of("G", "B", "C", "D", "A", "E", "F")) {
				final CItem rune = object(globals, "udg_EscapeRune" + suffix);
				final int reach = "G".equals(suffix) ? 880 : 440;
				if (rune == null || rune.isDead() || rune.isHidden() || attemptedEscapeRunes.contains(rune.getHandleId())
						|| maiev.distanceSquaredNoCollision(rune) > reach * reach) continue;
				final String type = rune.getTypeId().toString();
				if ((type.startsWith("rhe") || "rres".equals(type)) && maiev.getMaximumLife() - maiev.getLife() < 130
						|| type.startsWith("rma") && maiev.getMaximumMana() - maiev.getMana() < 70) continue;
				attemptedEscapeRunes.add(rune.getHandleId());
				tombEscapeRune = rune;
				tombEscapeRuneDeadline = tick + 68;
				break;
			}
		}
		if (tombEscapeRune != null && "rres".equals(tombEscapeRune.getTypeId().toString())
				&& maiev.distanceSquaredNoCollision(tombEscapeRune) > 300 * 300
				&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink,
						tombEscapeRune.getX(), tombEscapeRune.getY())) return;
		if (CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.smart, tombEscapeRune)) return;
		final Integer step = tombEscapeSteps.get(maiev.getHandleId());
		if (step != null) {
			for (int next = Math.min(route.length - 1, step + 8); next > step; next--) {
				final float[] landing = route[next];
				final double distance = maiev.distanceSquaredNoCollision(landing[0], landing[1]);
				if (distance < 500 * 500 || distance > 920 * 920) continue;
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, landing[0], landing[1])) {
					tombEscapeSteps.put(maiev.getHandleId(), next);
					return;
				}
				break;
			}
		}
		CampaignPlaythroughProbe.followRoute(viewer, orders, maiev, route, route.length - 1, 114, tombEscapeSteps);
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
	}

	private static void verifyBoatRescue(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		if (maiev == null) return;
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		check(tick < (AFTER_TOMB_SEA_ESCAPE ? 84_000 : 45_000) && !maiev.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()),
				"NightElfX04 boat rescue stalled or ended in defeat");
		if (!afterTombMaievVerified) {
			final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
			final var earned = cache == null ? null : cache.getStoredUnit("NightElfX04", "Maiev");
			check(earned != null && CampaignPlaythroughProbe.heroProperties(earned).equals(snapshot(maiev)),
					"NightElfX04 did not restore the earned Maiev from the Tomb of Sargeras");
			afterTombMaievVerified = true;
			System.out.println("[FrozenThroneOpeningProbe] restored earned Maiev for the original NightElfX04 boat rescue");
		}
		final CQuest boats = object(globals, "udg_QuestBoats");
		if (AFTER_TOMB_SEA_ESCAPE && runnerBoarded) {
			final CQuest escape = object(globals, "udg_QuestEscape");
			if (escape != null && escape.isCompleted()) {
				check(runnerSeaGatesCleared, "the courier finished before the escape channel was cleared");
				System.out.println("[FrozenThroneOpeningProbe] sailed the courier into the original NightElfX04 escape region");
				finished = true;
				Gdx.app.exit();
				return;
			}
		}
		if (boats == null || !boats.isDiscovered() || !ui.isUserControlEnabled()
				|| (!AFTER_TOMB_SEA_ESCAPE || !runnerBoarded) && (maiev.isPaused() || maiev.isHidden())
				|| game.isGamePaused() || tick < orderTick) return;
		orderTick = tick + 14;
		final int player = maiev.getPlayerIndex();
		if (boats.isCompleted()) {
			if (!afterTombBoatsVerified) {
				runnerTransport = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
						&& ISLAND_TRANSPORT.equals(unit.getTypeId()) && unit.getCargoData() != null)
					.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(-528, -16))).orElse(null);
				check(runnerTransport != null, "the native rescue did not transfer a boat");
				afterTombBoatsVerified = true;
				if (AFTER_TOMB_SEA_ESCAPE) reserveRunnerSeaEscort(viewer, player);
				System.out.println("[FrozenThroneOpeningProbe] completed the original NightElfX04 boat rescue through normal orders");
				if (!AFTER_TOMB_RUNNER) {
					finished = true;
					Gdx.app.exit();
					return;
				}
			}
			if (runnerBoarded) guideRunnerEscape(viewer, ui, globals, maiev, player);
			else boardRunner(viewer, ui, globals, maiev, player);
			return;
		}
		final var owner = game.getPlayer(player);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final var escort = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
				&& !unit.isHidden() && !unit.isPaused() && !unit.isBuilding() && !unit.getCurrentAttacks().isEmpty()).toList();
		final var threats = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)
				&& maiev.canReach(unit, 560)).toList();
		final CUnit threat = threats.stream().min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		final int step = boatRescueSteps.getOrDefault(maiev.getHandleId(), 0);
		if (boatRescueRefuge < 0 && maiev.getLife() < maiev.getMaximumLife() * 0.65f) boatRescueRefuge = Math.max(0, step - 3);
		if (maiev.getLife() >= maiev.getMaximumLife() * 0.87f) boatRescueRefuge = -1;
		for (final CUnit unit : escort) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit == maiev && maiev.getAbilityLevel(BLINK) >= 2) {
				if (maiev.getLife() < maiev.getMaximumLife() * 0.72f
						&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "phea")
								|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "pghe"))) continue;
				if (boatRescueBlinkStep < RESCUE_BOAT_BLINK_ROUTE.length) {
					final float[] landing = RESCUE_BOAT_BLINK_ROUTE[boatRescueBlinkStep];
					if (maiev.distanceSquaredNoCollision(landing[0], landing[1]) < 84 * 84) boatRescueBlinkStep++;
					else if (!CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink,
							landing[0], landing[1]) && threat != null && maiev.canReach(threat, 200)) {
						CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.attack, threat);
					}
				}
				continue;
			}
			if (unit == maiev && boatRescueRefuge >= 0) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "pghe")) continue;
				final float[] refuge = RESCUE_BOAT_ROUTE[boatRescueRefuge];
				if (threat != null && maiev.distanceSquaredNoCollision(refuge[0], refuge[1]) < 170 * 170 && boatRescueRefuge > 0) {
					boatRescueRefuge--;
				}
				final float[] retreat = RESCUE_BOAT_ROUTE[boatRescueRefuge];
				if (threat != null && maiev.distanceSquaredNoCollision(retreat[0], retreat[1]) < 920 * 920
						&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, retreat[0], retreat[1])) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, retreat[0], retreat[1]);
				continue;
			}
			if (unit == maiev && threat != null && (threats.size() > 1 && maiev.canReach(threat, 300)
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, maiev, OrderIds.fanofknives)
					|| CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.shadowstrike, threat))) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, threat)) continue;
			final int end = boatRescueRefuge < 0 ? RESCUE_BOAT_ROUTE.length - 1 : Math.min(step + 1, RESCUE_BOAT_ROUTE.length - 1);
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, RESCUE_BOAT_ROUTE, end, 132, boatRescueSteps);
		}
		if (tick >= reportTick) {
			reportTick = tick + 400;
			System.out.printf("[FrozenThroneOpeningProbe] boat rescue %.0f,%.0f life=%.0f escort=%d path=%d blink=%d%n",
					maiev.getX(), maiev.getY(), maiev.getLife(), escort.size(), step, boatRescueBlinkStep);
		}
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
	}

	private static void boardRunner(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev, final int player) {
		final CUnit runner = object(globals, "udg_CurrentRunner");
		check(runner != null && !runner.isDead() && !runnerTransport.isDead(), "the original runner or rescued transport was lost");
		if (runner.getTransport() == runnerTransport) {
			check(runnerTransport.getCargoData().getCargoCount() > 0 && runnerTransport.getCargoData().getUnit(0) == runner,
					"the runner was missing from the ship's cargo");
			System.out.println("[FrozenThroneOpeningProbe] boarded the original NightElfX04 runner in a rescued transport");
			runnerBoarded = true;
			runnerRescueSteps.remove(runnerTransport.getHandleId());
			if (AFTER_TOMB_SEA_ESCAPE) {
				reportTick = viewer.simulation.getGameTurnTick();
				maiev.addDamageTakenListener((game, attacker, target, attack, ranged, type, damage, bonus, actual) -> {
					if (attacker != null && actual > 0 && (actual >= 70 || target.getLife() < 250)) {
						System.out.printf("[FrozenThroneOpeningProbe] %s hit Maiev for %.0f at %.0f,%.0f life=%.0f%n",
								attacker.getTypeId(), actual, target.getX(), target.getY(), target.getLife());
					}
				});
				runnerTransport.addDamageTakenListener((game, attacker, ship, attack, ranged, type, damage, bonus, actual) -> {
					if (attacker != null && actual > 0) {
						System.out.printf("[FrozenThroneOpeningProbe] escape transport hit by %s at %.0f,%.0f damage=%.0f life=%.0f%n",
								attacker.getTypeId(), attacker.getX(), attacker.getY(), actual, ship.getLife());
					}
				});
				return;
			}
			finished = true;
			Gdx.app.exit();
			return;
		}
		final var game = viewer.simulation;
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		if (!runnerCausewaySecured) {
			runnerCausewaySecured = clearRunnerPassage(viewer, ui, maiev, runner, orders, RUNNER_CAUSEWAY_STEP);
			if (runnerCausewaySecured) System.out.println("[FrozenThroneOpeningProbe] cleared the original causeway before moving the runner");
			return;
		}
		final var owner = game.getPlayer(player);
		final int step = runnerRescueSteps.getOrDefault(runner.getHandleId(), 0);
		if (step >= 23 && !runnerRidgeSecured) {
			runnerRidgeSecured = clearRunnerPassage(viewer, ui, maiev, runner, orders, RUNNER_RIDGE_STEP);
			if (runnerRidgeSecured) System.out.println("[FrozenThroneOpeningProbe] cleared the western ridge ahead of the runner");
			return;
		}
		if (AFTER_TOMB_SEA_ESCAPE && step >= RUNNER_RIDGE_STEP + 2 && !runnerLandingCleared) {
			runnerLandingCleared = clearRunnerPassage(viewer, ui, maiev, runner, orders, RUNNER_LANDING_STEP);
			if (runnerLandingCleared) System.out.println("[FrozenThroneOpeningProbe] cleared the landing guards before moving the courier");
			return;
		}
		final CUnit pursuer = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)
				&& runner.canReach(unit, 540)).min(Comparator.comparingDouble(runner::distanceSquaredNoCollision)).orElse(null);
		if (runner.canReach(runnerTransport, 370)) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, runnerTransport, OrderIds.load, runner);
			CampaignPlaythroughProbe.targetOrder(viewer, orders, runner, OrderIds.smart, runnerTransport);
		}
		else {
			if (pursuer != null && runner.getLife() < runner.getMaximumLife() * 0.65f) {
				final float[] refuge = RUNNER_SHORE_ROUTE[Math.max(0, step - 2)];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, runner, refuge[0], refuge[1]);
			}
			else {
				if (AFTER_TOMB_SEA_ESCAPE && step == RUNNER_RIDGE_STEP + 1) {
					followRunnerTurn(viewer, orders, runner, step, RUNNER_RIDGE_TURN, 65);
				}
				else if (AFTER_TOMB_SEA_ESCAPE && step == RUNNER_LANDING_STEP + 1) {
					followRunnerPier(viewer, orders, runner, step);
				}
				else CampaignPlaythroughProbe.followRoute(viewer, orders, runner, RUNNER_SHORE_ROUTE, RUNNER_SHORE_ROUTE.length - 1,
						125, runnerRescueSteps);
			}
			if (step >= RUNNER_SHORE_ROUTE.length - 9) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, runnerTransport, RUNNER_PICKUP_WATER_ROUTE,
						RUNNER_PICKUP_WATER_ROUTE.length - 1, 130, runnerRescueSteps);
			}
		}
		final var escort = game.getUnits().stream().filter(unit -> unit != runner && unit != runnerTransport && unit != runnerEscortTransport
				&& unit.getPlayerIndex() == player && !unit.isDead()
				&& (!AFTER_TOMB_SEA_ESCAPE || !runnerRidgeSecured || unit != maiev)
				&& unit != runnerReservedSiege
				&& !unit.isHidden() && !unit.isPaused() && !unit.isBuilding() && runner.canReach(unit, 1450)).toList();
		for (final CUnit unit : escort) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (runner.getLife() < runner.getMaximumLife() * 0.8f && runner.canReach(unit, 620)
					&& (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.rejuvination, runner)
							|| CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.heal, runner))) continue;
			if (unit.getCurrentAttacks().isEmpty()) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, pursuer)) continue;
			runnerRescueSteps.putIfAbsent(unit.getHandleId(), Math.max(0, step - 2));
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, RUNNER_SHORE_ROUTE,
					Math.min(step + 2, RUNNER_SHORE_ROUTE.length - 1), 140, runnerRescueSteps);
		}
		final int tick = game.getGameTurnTick();
		if (tick >= reportTick) {
			reportTick = tick + 300;
			System.out.printf("[FrozenThroneOpeningProbe] runner %.0f,%.0f life=%.0f step=%d escort=%d threat=%s transport %.0f,%.0f life=%.0f%n",
					runner.getX(), runner.getY(), runner.getLife(), step, escort.size(), pursuer == null ? "none" : pursuer.getTypeId(),
					runnerTransport.getX(), runnerTransport.getY(), runnerTransport.getLife());
			if (AFTER_TOMB_SEA_ESCAPE && step == RUNNER_LANDING_STEP + 1) {
				final float[] point = runnerPierBypass == null ? RUNNER_PIER_TURN[runnerTurnStep] : runnerPierBypass;
				reportPierPath(game, runner, point);
			}
		}
		ui.getCameraManager().setTarget(runner.getX(), runner.getY());
	}

	private static void followRunnerPier(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit runner,
			final int routeStep) {
		final var game = viewer.simulation;
		final float[] first = RUNNER_PIER_TURN[0];
		if (runnerTurnStep == 0 && runnerPierBypass == null && runner.canReach(first[0], first[1], 120)
				&& !pierSegmentClear(game, runner, runner.getX(), runner.getY(), first)) {
			final float[] second = RUNNER_PIER_TURN[1];
			for (final float[] offset : PIER_DETOUR_OFFSETS) {
				final float[] detour = { runner.getX() + offset[0], runner.getY() + offset[1] };
				if (pierSegmentClear(game, runner, runner.getX(), runner.getY(), detour)
						&& pierSegmentClear(game, runner, detour[0], detour[1], second)) {
					runnerPierBypass = detour;
					System.out.printf("[FrozenThroneOpeningProbe] routed the courier around the occupied pier at %.0f,%.0f%n", detour[0], detour[1]);
					break;
				}
			}
		}
		if (runnerPierBypass == null) {
			followRunnerTurn(viewer, orders, runner, routeStep, RUNNER_PIER_TURN, 7);
		}
		else if (runner.distanceSquaredNoCollision(runnerPierBypass[0], runnerPierBypass[1]) < 7 * 7) {
			runnerPierBypass = null;
			runnerTurnStep = 1;
		}
		else CampaignPlaythroughProbe.pointOrder(viewer, orders, runner, runnerPierBypass[0], runnerPierBypass[1]);
	}

	private static boolean pierSegmentClear(final CSimulation game, final CUnit runner, final float x, final float y,
			final float[] target) {
		final var movement = runner.getMovementType();
		final float collision = runner.getUnitType().getCollisionSize();
		final Rectangle bounds = new Rectangle(runner.getCollisionRectangle());
		final int samples = Math.max(1, (int) Math.ceil(Math.hypot(target[0] - x, target[1] - y) / 8));
		for (int sample = 1; sample <= samples; sample++) {
			final float fraction = (float) sample / samples;
			final float nextX = x + (target[0] - x) * fraction;
			final float nextY = y + (target[1] - y) * fraction;
			if (!game.getPathingGrid().isPathable(nextX, nextY, movement, collision)
					|| game.getWorldCollision().intersectsAnythingOtherThan(bounds.setCenter(nextX, nextY), runner, movement, false)) return false;
		}
		return true;
	}

	private static void reportPierPath(final CSimulation game, final CUnit runner, final float[] target) {
		final var movement = runner.getMovementType();
		final float collision = runner.getUnitType().getCollisionSize();
		final var pathing = game.getPathingGrid();
		final double angle = Math.atan2(target[1] - runner.getY(), target[0] - runner.getX());
		final float speed = runner.getMovementSpeed(game) * WarsmashConstants.SIMULATION_STEP_TIME;
		final float nextX = runner.getX() + speed * (float) Math.cos(angle);
		final float nextY = runner.getY() + speed * (float) Math.sin(angle);
		final Rectangle nextBounds = new Rectangle(runner.getCollisionRectangle()).setCenter(nextX, nextY);
		final boolean blocked = game.getWorldCollision().intersectsAnythingOtherThan(nextBounds, runner, movement, false);
		final var blockers = new ArrayList<String>();
		if (blocked) game.getWorldCollision().enumUnitsInRect(nextBounds, unit -> {
			if (unit != runner && !unit.isNoUnitCollision()) {
				blockers.add(unit.getTypeId() + " at " + Math.round(unit.getX()) + "," + Math.round(unit.getY()));
			}
			return false;
		});
		final var behavior = runner.getCurrentBehavior();
		System.out.printf("[FrozenThroneOpeningProbe] pier target=%.0f,%.0f walkable=%s next=%.1f,%.1f walkable=%s occupied=%s blockers=%s facing=%.1f behavior=%s%n",
				target[0], target[1], pathing.isPathable(target[0], target[1], movement, collision), nextX, nextY,
				pathing.isPathable(nextX, nextY, movement, collision), blocked, blockers, runner.getFacing(),
				behavior == null ? "idle" : behavior.getClass().getSimpleName());
	}

	private static void followRunnerTurn(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit runner,
			final int routeStep, final float[][] turn, final float arrivalRadius) {
		final float[] point = turn[runnerTurnStep];
		if (runner.distanceSquaredNoCollision(point[0], point[1]) >= arrivalRadius * arrivalRadius) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, runner, point[0], point[1]);
		}
		else if (++runnerTurnStep == turn.length) {
			runnerRescueSteps.put(runner.getHandleId(), routeStep + 1);
			runnerTurnStep = 0;
		}
	}

	private static void guideRunnerEscape(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev, final int player) {
		final CUnit runner = object(globals, "udg_CurrentRunner");
		check(runner != null && !runner.isDead() && !runnerTransport.isDead() && runner.getTransport() == runnerTransport,
				"the courier did not stay aboard his escape transport");
		final var game = viewer.simulation;
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		if (!runnerSeaGatesCleared) {
			final CDestructable southGate = tombGate(viewer, "ZTsx", -3968, -2048);
			final CDestructable northGate = tombGate(viewer, "ZTsx", -4352, 1408);
			if (southGate.isDead() && !runnerSouthGateCleared) {
				runnerSouthGateCleared = true;
				runnerEscortAboard = false;
				runnerEscortLanded = false;
				runnerGateScoutStep = 0;
				runnerGateScoutRefuge = -1;
				System.out.println("[FrozenThroneOpeningProbe] destroyed the southern sea gate");
			}
			if (southGate.isDead() && northGate.isDead()) {
				runnerSeaGatesCleared = true;
				System.out.println("[FrozenThroneOpeningProbe] cleared both original sea gates for the courier");
				return;
			}
			final CDestructable gate = runnerSouthGateCleared ? northGate : southGate;
			if (!prepareRunnerGateApproach(viewer, ui, orders, maiev)) return;
			final boolean canScout = maiev.getCurrentAttacks().stream()
					.anyMatch(attack -> gate.canBeTargetedBy(game, maiev, attack.getTargetsAllowed(), null));
			if (canScout) scoutRunnerSeaGate(viewer, ui, orders, maiev, gate);
			else ferrySiegeToGate(viewer, ui, orders, maiev, gate);
		}
		else {
			CampaignPlaythroughProbe.followRoute(viewer, orders, runnerTransport, RUNNER_ESCAPE_CHANNEL,
					RUNNER_ESCAPE_CHANNEL.length - 1, 110, runnerRescueSteps);
			ui.getCameraManager().setTarget(runnerTransport.getX(), runnerTransport.getY());
		}
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 400;
			System.out.printf("[FrozenThroneOpeningProbe] sea escape ship %.0f,%.0f life=%.0f Maiev %.0f,%.0f life=%.0f%n",
					runnerTransport.getX(), runnerTransport.getY(), runnerTransport.getLife(), maiev.getX(), maiev.getY(),
					maiev.getLife());
		}
	}

	private static boolean prepareRunnerGateApproach(final War3MapViewer viewer, final MeleeUI ui,
			final CPlayerUnitOrderExecutor orders, final CUnit maiev) {
		if (!runnerSeaScoutReady) {
			if (maiev.getLife() < maiev.getMaximumLife() * 0.92f || maiev.getMana() < maiev.getMaximumMana() * 0.78f) {
				if (!CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "phea")
						&& !CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "pghe")) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, -848, 2384);
				}
				ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
				return false;
			}
			runnerSeaScoutReady = true;
		}
		if (runnerSeaBlinkStep < RUNNER_FERRY_BLINKS.length) {
			final float[] point = RUNNER_FERRY_BLINKS[runnerSeaBlinkStep];
			if (maiev.canReach(point[0], point[1], 80)) runnerSeaBlinkStep++;
			else CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, point[0], point[1]);
			return false;
		}
		return true;
	}

	private static void scoutRunnerSeaGate(final War3MapViewer viewer, final MeleeUI ui,
			final CPlayerUnitOrderExecutor orders, final CUnit maiev, final CDestructable gate) {
		final var game = viewer.simulation;
		final int player = maiev.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final float[][] route = runnerSouthGateCleared ? RUNNER_NORTH_BANK_ROUTE : RUNNER_SOUTH_BANK_ROUTE;
		final int step = runnerGateScoutStep;
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
		if (maiev.getCurrentBehavior() != null && maiev.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) return;
		if (maiev.getLife() < maiev.getMaximumLife() * 0.66f && runnerGateScoutRefuge < 0) {
			if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "phea")
					|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "pghe")) return;
			runnerGateScoutRefuge = Math.max(0, step - 3);
		}
		if (runnerGateScoutRefuge >= 0) {
			final float[] refuge = route[runnerGateScoutRefuge];
			if (maiev.getLife() >= maiev.getMaximumLife() * 0.91f) {
				runnerGateScoutStep = runnerGateScoutRefuge;
				runnerGateScoutRefuge = -1;
			}
			else if (!maiev.canReach(refuge[0], refuge[1], 90)
					&& !CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, refuge[0], refuge[1])) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, refuge[0], refuge[1]);
			}
			return;
		}
		final int crossing = runnerSouthGateCleared ? 8 : step < 6 ? 4 : 10;
		if ((step == crossing || step == crossing - 1)
				&& maiev.distanceSquaredNoCollision(route[crossing][0], route[crossing][1]) < 965 * 965) {
			final float[] landing = route[crossing];
			if (maiev.distanceSquaredNoCollision(landing[0], landing[1]) <= 86 * 86) {
				runnerGateScoutStep = crossing + 1;
				System.out.printf("[FrozenThroneOpeningProbe] Maiev crossed to bank %d waypoint %d%n",
						runnerSouthGateCleared ? 2 : 1, crossing);
				return;
			}
			if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, landing[0], landing[1])) return;
		}
		final CUnit guard = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)
				&& maiev.canReach(unit, step == crossing - 1 ? 220 : 425))
				.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		if (guard != null && (maiev.canReach(guard, 285)
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, maiev, OrderIds.fanofknives)
				|| CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.shadowstrike, guard)
				|| maiev.canReach(guard, 235) && CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.attack, guard))) return;
		if (step >= route.length) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.attack, gate);
			return;
		}
		final float[] point = route[step];
		if (maiev.distanceSquaredNoCollision(point[0], point[1]) <= 86 * 86) {
			runnerGateScoutStep++;
			System.out.printf("[FrozenThroneOpeningProbe] Maiev scout reached bank %d waypoint %d%n",
					runnerSouthGateCleared ? 2 : 1, step);
		}
		else if (runnerSouthGateCleared ? step == 8 : step == 4 || step == 10) {
			CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, point[0], point[1]);
		}
		else CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, point[0], point[1]);
	}

	private static void ferrySiegeToGate(final War3MapViewer viewer, final MeleeUI ui, final CPlayerUnitOrderExecutor orders,
			final CUnit maiev, final CDestructable gate) {
		final var game = viewer.simulation;
		final int player = maiev.getPlayerIndex();
		final CUnit ferry = runnerEscortTransport;
		check((!ferry.isDead() || runnerEscortLanded) && runnerGateSiege.stream().anyMatch(unit -> !unit.isDead()),
				"lost the original harbor siege escort");
		final var party = new ArrayList<CUnit>();
		party.add(maiev);
		runnerGateSiege.stream().filter(unit -> !unit.isDead()).forEach(party::add);
		if (!runnerEscortAboard) {
			for (final CUnit unit : party) {
				if (unit.getTransport() == ferry) continue;
				CampaignPlaythroughProbe.targetOrder(viewer, orders, ferry, OrderIds.load, unit);
				CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, ferry);
				return;
			}
			runnerEscortAboard = true;
			System.out.println("[FrozenThroneOpeningProbe] boarded Maiev and the original Glaive Throwers in the escort transport");
		}
		final int gateIndex = runnerSouthGateCleared ? 1 : 0;
		final float[][] ferryRoute = runnerSouthGateCleared ? RUNNER_ESCAPE_CHANNEL : SOUTH_GATE_FERRY_ROUTE;
		final int shoreStep = runnerSouthGateCleared ? NORTH_GATE_LANDING_STEP : ferryRoute.length - 1;
		final Map<Integer, Integer> ferryWaypoints = runnerSouthGateCleared ? runnerRescueSteps : southernFerryWaypoints;
		final float[] landing = RUNNER_SIEGE_LANDINGS[gateIndex];
		if (!runnerEscortLanded) {
			if (ferry.getCargoData().isEmpty()) {
				runnerEscortLanded = true;
				System.out.println("[FrozenThroneOpeningProbe] landed the original siege escort for sea gate " + (gateIndex + 1));
			}
			else {
				final float[] shore = ferryRoute[shoreStep];
				if (ferry.canReach(shore[0], shore[1], 145)) {
					CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, ferry, OrderIds.unloadall, landing[0], landing[1]);
				}
				else CampaignPlaythroughProbe.followRoute(viewer, orders, ferry, ferryRoute,
						shoreStep, 105, ferryWaypoints);
				ui.getCameraManager().setTarget(ferry.getX(), ferry.getY());
				return;
			}
		}
		if (!ferry.isDead()) {
			final float[] refuge = ferryRoute[runnerSouthGateCleared ? shoreStep + 2 : shoreStep - 1];
			CampaignPlaythroughProbe.pointOrder(viewer, orders, ferry, refuge[0], refuge[1]);
		}
		final var owner = game.getPlayer(player);
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)).toList();
		for (final CUnit unit : party) {
			if (unit.isHidden() || unit.isPaused()
					|| unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final int range = unit.getCurrentAttacks().stream().mapToInt(attack -> attack.getRange()).max().orElse(0);
			if (unit != maiev && unit.canReach(gate, range)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, gate)) continue;
			final CUnit threat = enemies.stream().filter(enemy -> unit.canReach(enemy, unit == maiev ? 600 : 1100))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (unit == maiev && threat != null) {
				if (CampaignPlaythroughProbe.supportOrder(viewer, orders, maiev, threat)
						|| CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.shadowstrike, threat)
						|| maiev.canReach(threat, 310)
								&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, maiev, OrderIds.fanofknives)) continue;
			}
			if (unit == maiev && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, threat)) continue;
			if (!unit.canReach(gate, range) || !CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, gate)) {
				final float[] front = RUNNER_SIEGE_ADVANCE[gateIndex];
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, front[0], front[1]);
			}
		}
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
		if (game.getGameTurnTick() >= reportTick) {
			System.out.printf("[FrozenThroneOpeningProbe] sea gate %d life=%.0f siege=%d Maiev=%.0f,%.0f%n",
					gateIndex + 1, gate.getLife(), party.size() - 1, maiev.getX(), maiev.getY());
		}
	}

	private static void reserveRunnerSeaEscort(final War3MapViewer viewer, final int player) {
		final var units = viewer.simulation.getUnits();
		runnerEscortTransport = units.stream().filter(unit -> unit != runnerTransport && unit.getPlayerIndex() == player
				&& !unit.isDead() && ISLAND_TRANSPORT.equals(unit.getTypeId()) && unit.getCargoData() != null)
				.findFirst().orElse(null);
		runnerGateSiege = units.stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
				&& "ebal".equals(unit.getTypeId().toString()) && unit.canReach(-880, 144, 900)).toList();
		check(runnerEscortTransport != null && runnerGateSiege.size() == 2, "the original harbor rescue did not transfer its siege escort");
		runnerReservedSiege = runnerGateSiege.get(0);
		runnerEscortTransport.addDamageTakenListener((simulation, attacker, ship, attack, ranged, type, damage, bonus, actual) -> {
			if (attacker != null && actual > 0) {
				System.out.printf("[FrozenThroneOpeningProbe] siege transport hit by %s at %.0f,%.0f damage=%.0f life=%.0f%n",
						attacker.getTypeId(), attacker.getX(), attacker.getY(), actual, ship.getLife());
			}
		});
		System.out.println("[FrozenThroneOpeningProbe] identified both original Glaive Throwers and held one at the harbor");
	}

	private static boolean clearRunnerPassage(final War3MapViewer viewer, final MeleeUI ui, final CUnit maiev,
			final CUnit runner, final CPlayerUnitOrderExecutor orders, final int lastStep) {
		final var game = viewer.simulation;
		final int player = maiev.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final float[] destination = RUNNER_SHORE_ROUTE[lastStep];
		final var escort = game.getUnits().stream().filter(unit -> unit != runner && unit.getPlayerIndex() == player
				&& !unit.isDead() && !unit.isHidden() && !unit.isPaused() && !unit.isBuilding() && !unit.getCurrentAttacks().isEmpty()
				&& unit != runnerReservedSiege
				&& (unit == maiev || lastStep == RUNNER_CAUSEWAY_STEP && unit.getX() > 2900
						|| unit.canReach(destination[0], destination[1], 1400) || unit.canReach(maiev, 1200))).toList();
		final var hostiles = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)).toList();
		if (maiev.canReach(destination[0], destination[1], 440) && hostiles.stream().noneMatch(unit ->
				unit.distanceSquaredNoCollision(destination[0], destination[1]) < 820 * 820)) return true;
		final boolean protectingSiege = AFTER_TOMB_SEA_ESCAPE && lastStep >= RUNNER_RIDGE_STEP;
		final int maievStep = runnerRescueSteps.getOrDefault(maiev.getHandleId(), 23);
		if (protectingSiege && runnerRidgeRefuge < 0 && maiev.getLife() < maiev.getMaximumLife() * 0.66f) {
			runnerRidgeRefuge = Math.max(23, maievStep - 3);
		}
		if (runnerRidgeRefuge >= 0 && maiev.getLife() > maiev.getMaximumLife() * 0.82f) {
			runnerRescueSteps.put(maiev.getHandleId(), runnerRidgeRefuge);
			runnerRidgeRefuge = -1;
		}
		for (final CUnit unit : escort) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit == maiev) {
				if (maiev.getLife() < maiev.getMaximumLife() * 0.68f
						&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "phea")
								|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "pghe"))) continue;
				if (runnerEscortBlinkStep >= 0) {
					final float[] landing = RESCUE_BOAT_BLINK_ROUTE[runnerEscortBlinkStep];
					if (maiev.distanceSquaredNoCollision(landing[0], landing[1]) < 94 * 94) runnerEscortBlinkStep--;
					else CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, landing[0], landing[1]);
					continue;
				}
				if (runnerRidgeRefuge >= 0) {
					final float[] refuge = RUNNER_SHORE_ROUTE[runnerRidgeRefuge];
					if (!maiev.canReach(refuge[0], refuge[1], 98)
							&& !CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, refuge[0], refuge[1])) {
						CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, refuge[0], refuge[1]);
					}
					continue;
				}
			}
			final boolean siege = protectingSiege && runnerGateSiege.contains(unit);
			if (siege && unit.getLife() < unit.getMaximumLife() * 0.55f) {
				final float[] refuge = RUNNER_SHORE_ROUTE[33];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
				continue;
			}
			final CUnit enemy = hostiles.stream().filter(threat -> unit.canReach(threat, siege ? 980 : 560))
					.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			if (unit == maiev && enemy != null && (maiev.canReach(enemy, 310)
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, maiev, OrderIds.fanofknives)
					|| CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.shadowstrike, enemy))) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			final int end = protectingSiege && runnerRidgeRefuge >= 0 ? runnerRidgeRefuge + 1 : lastStep;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, RUNNER_SHORE_ROUTE, end, 120, runnerRescueSteps);
		}
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 350;
			System.out.printf("[FrozenThroneOpeningProbe] clearing runner route step=%d Maiev=%.0f,%.0f life=%.0f escorts=%d blink=%d%n",
					lastStep, maiev.getX(), maiev.getY(), maiev.getLife(), escort.size(), runnerEscortBlinkStep);
		}
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
		return false;
	}

	private static CDestructable tombGate(final War3MapViewer viewer, final String type, final int x, final int y) {
		return viewer.simulation.getDestructables().stream().filter(gate -> type.equals(gate.getTypeId().toString())
				&& gate.getX() == x && gate.getY() == y).findFirst()
				.orElseThrow(() -> new IllegalStateException("Missing original Tomb gate " + type + " at " + x + ", " + y));
	}

	private static boolean rescuedTombGroup(final GlobalScope globals, final String name, final int player) {
		final List<CUnit> group = object(globals, name);
		return group != null && group.stream().anyMatch(unit -> !unit.isDead() && unit.getPlayerIndex() == player);
	}

	private static List<CDestructable> rocksInArea(final War3MapViewer viewer, final Rectangle area) {
		return viewer.simulation.getDestructables().stream().filter(rock -> !rock.isDead()
				&& "DTrc".equals(rock.getTypeId().toString()) && area.contains(rock.getX(), rock.getY())).toList();
	}

	private static void advanceTombExpedition(final War3MapViewer viewer, final MeleeUI ui, final CUnit maiev,
			final float[][] route, final int end, final List<CDestructable> obstacles) {
		final var game = viewer.simulation;
		final var player = game.getPlayer(maiev.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		final var escort = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId()
				&& !unit.isDead() && !unit.isHidden() && !unit.isPaused() && !unit.getCurrentAttacks().isEmpty()).toList();
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player.getId())).toList();
		final boolean approachingEye = route == tombEyeApproach;
		final boolean inEyeHall = approachingEye && maiev.getX() >= 3400 && maiev.getY() < -1400;
		if (inEyeHall && maiev.getLife() < maiev.getMaximumLife() * 0.80f) tombEyeRecovering = true;
		if (maiev.getLife() >= maiev.getMaximumLife() * 0.92f) {
			tombEyeRecovering = false;
			tombEyeRefugeStep = -1;
		}
		final CUnit priority = enemies.stream().filter(enemy -> maiev.canReach(enemy, 620))
				.min(Comparator.comparing((CUnit target) -> approachingEye && !"nmyr".equals(target.getTypeId().toString()))
						.thenComparingDouble(CUnit::getLife)).orElse(null);
		for (final CUnit unit : escort) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit.getAbilityLevel(War3ID.fromString("Arej")) > 0) {
				final CUnit wounded = escort.stream().filter(ally -> ally.getMaximumLife() - ally.getLife() >= 190
						&& unit.canReach(ally, ally == maiev && tombEyeRecovering ? 800 : 400) && ally.getAbilities().stream()
								.noneMatch(ability -> "Brej".equals(ability.getAlias().toString())))
						.max(Comparator.comparing((CUnit ally) -> ally == maiev)
								.thenComparingDouble(ally -> ally.getMaximumLife() - ally.getLife())).orElse(null);
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.rejuvination, wounded)) continue;
			}
			final var near = enemies.stream().filter(target -> unit.canReach(target, 550)).toList();
			final CDestructable obstacle = obstacles.stream().filter(target -> !target.isDead() && unit.canReach(target, 360))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (unit != maiev && approachingEye && tombEyeRecovering) {
				final CUnit pursuer = priority != null && unit.canReach(priority, 860) ? priority
						: near.stream().min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
				if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, pursuer)) {
					final int position = escort.indexOf(unit);
					CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit,
							maiev.getX() + (position % 4 - 2) * 72, maiev.getY() + 96 + position / 4 * 72);
				}
				continue;
			}
			if (unit == maiev && approachingEye && tombEyeGates.get(0).isDead() && tombEyeGates.get(1).isDead()) {
				final int step = escortWaypoints.getOrDefault(unit.getHandleId(), 0);
				if (unit.getLife() < unit.getMaximumLife() * 0.82f && (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe"))) continue;
				if (tombEyeRecovering) {
					if (unit.isMovementDisabled() && priority != null && (CampaignPlaythroughProbe.targetOrder(viewer,
							orders, unit, OrderIds.shadowstrike, priority)
							|| CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, priority))) continue;
					if (tombEyeRefugeStep < 0) {
						tombEyeRefugeStep = unit.getX() > 4550 ? 21 : unit.getX() > 4000 ? 19 : 17;
					}
					final float[] refuge = TOMB_EYE_ROUTE[tombEyeRefugeStep];
					if (priority != null && tombEyeRefugeStep > 5
							&& unit.distanceSquaredNoCollision(refuge[0], refuge[1]) < 175 * 175) tombEyeRefugeStep--;
					final float refugeX = TOMB_EYE_ROUTE[tombEyeRefugeStep][0];
					final float refugeY = TOMB_EYE_ROUTE[tombEyeRefugeStep][1];
					if (priority != null && unit.distanceSquaredNoCollision(refugeX, refugeY) > 250 * 250
							&& unit.distanceSquaredNoCollision(refugeX, refugeY) < 930 * 930
							&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.blink, refugeX, refugeY)) continue;
					if (priority != null || unit.distanceSquaredNoCollision(refugeX, refugeY) > 110 * 110) {
						CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refugeX, refugeY);
					}
					continue;
				}
				if (inEyeHall && priority != null
						&& escort.stream().filter(ally -> ally != maiev && ally.canReach(maiev, 650)).limit(3).count() < 3) {
					continue;
				}
				if (eyeRune != null && (eyeRune.isDead() || game.getGameTurnTick() >= eyeRuneDeadline)) {
					if (eyeRune.isDead() && "rhe3".equals(eyeRune.getTypeId().toString())) {
						System.out.println("[FrozenThroneOpeningProbe] collected the original Greater Rune of Healing before the Eye chamber");
					}
					eyeRune = null;
				}
				final CItem rune = game.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
						&& item.getContainedInventory() == null && !attemptedEyeRunes.contains(item.getHandleId())
						&& ("rhe3".equals(item.getTypeId().toString()) && unit.getX() > 6100 && unit.getY() > 650
								&& unit.distanceSquaredNoCollision(item) < 970 * 970 && unit.getLife() < unit.getMaximumLife() * 0.98f
								|| unit.distanceSquaredNoCollision(item) < 440 * 440
										&& (item.getTypeId().toString().startsWith("rhe") && unit.getLife() < unit.getMaximumLife() * 0.84f
												|| "rma2".equals(item.getTypeId().toString()) && unit.getMaximumMana() - unit.getMana() >= 85)))
						.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
				if (eyeRune == null && rune != null) {
					eyeRune = rune;
					eyeRuneDeadline = game.getGameTurnTick() + ("rhe3".equals(rune.getTypeId().toString()) ? 210 : 64);
					attemptedEyeRunes.add(rune.getHandleId());
				}
				if (eyeRune != null && "rhe3".equals(eyeRune.getTypeId().toString())
						&& unit.distanceSquaredNoCollision(6960, 976) > 180 * 180) {
					if (unit.distanceSquaredNoCollision(6960, 976) < 930 * 930
							&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.blink, 6960, 976)) continue;
					if (unit.distanceSquaredNoCollision(6672, 1168) > 135 * 135 && unit.getX() < 6700) {
						CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, 6672, 1168);
					}
					else {
						CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, 6960, 976);
					}
					continue;
				}
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, eyeRune)) continue;
				if (priority != null && "nmyr".equals(priority.getTypeId().toString()) && unit.canReach(priority, 350)
						&& unit.getMana() >= 150 && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.shadowstrike, priority)) continue;
				if (unit.getX() > 4600 && unit.getX() < 6400 && unit.getY() < -1650 && tombEyeGates.get(2).isDead()) {
					if (unit.getLife() < unit.getMaximumLife() * 0.53f && priority != null) {
						if (!CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.blink, 4512, -2128)) {
							CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, 4512, -2128);
						}
						continue;
					}
					final var readiness = new BooleanAbilityActivationReceiver();
					final var blink = unit.getAbilities().stream().filter(ability -> BLINK.equals(ability.getCode()))
							.findFirst().orElse(null);
					if (blink != null) blink.checkCanUse(game, unit, OrderIds.blink, readiness);
					final float chamberX = 6288;
					final float chamberY = -2192;
					if (readiness.isOk() && unit.getX() >= 5450 && unit.getX() < 6130
							&& unit.distanceSquaredNoCollision(chamberX, chamberY) < 970 * 970
							&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.blink, chamberX, chamberY)) {
						escortWaypoints.put(unit.getHandleId(), end);
						continue;
					}
				}
				if (priority != null && obstacle == null && unit.getMana() >= 100 && unit.getX() < 4400
						&& step >= TOMB_THIRD_BARRIER_ROUTE.length + 17 && step < end - 4) {
					final int blinkStep = Math.min(end, step + 2);
					final float[] destination = route[blinkStep];
					final double distance = unit.distanceSquaredNoCollision(destination[0], destination[1]);
					if (distance > 400 * 400 && distance < 930 * 930
							&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.blink, destination[0], destination[1])) {
						escortWaypoints.put(unit.getHandleId(), blinkStep);
						continue;
					}
				}
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, obstacle)) continue;
				if (unit.getMana() >= 150 && near.stream().filter(target -> unit.canReach(target, 385)).limit(3).count() == 3
						&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.fanofknives)) continue;
				if (priority != null || step >= TOMB_THIRD_BARRIER_ROUTE.length + 5) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, 110, escortWaypoints);
					continue;
				}
			}
			if (unit == maiev && unit.getLife() < unit.getMaximumLife() * (tombRetreatStep >= 0 ? 0.88f : 0.73f)) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")) continue;
				if (tombRetreatStep < 0) tombRetreatStep = Math.max(0, escortWaypoints.getOrDefault(unit.getHandleId(), 0) - 2);
				if (priority != null && tombRetreatStep > 0 && unit.distanceSquaredNoCollision(
						route[tombRetreatStep][0], route[tombRetreatStep][1]) < 160 * 160) tombRetreatStep--;
				final float[] refuge = route[tombRetreatStep];
				if (priority != null && unit.distanceSquaredNoCollision(refuge[0], refuge[1]) < 937 * 937
						&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.blink, refuge[0], refuge[1])) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
				continue;
			}
			if (unit == maiev) {
				tombRetreatStep = -1;
				if (unit.getMana() >= 150 && near.stream().filter(target -> unit.canReach(target, 415)).limit(2).count() == 2
						&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.fanofknives)) continue;
				if (near.isEmpty()) {
					final CItem supply = game.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
							&& item.getContainedInventory() == null && List.of("phea", "pghe", "rhe1", "rhe2", "rhe3").contains(item.getTypeId().toString())
							&& unit.distanceSquaredNoCollision(item) < 185 * 185)
							.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
					if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, supply)) continue;
				}
			}
			final CUnit enemy = priority != null && unit.canReach(priority, 740) ? priority
					: near.stream().min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, obstacle)) continue;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, 94, escortWaypoints);
		}
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
		final int tick = game.getGameTurnTick();
		if (tick >= reportTick) {
			reportTick = tick + 360;
			System.out.printf("[FrozenThroneOpeningProbe] tomb expedition %.0f,%.0f life=%.0f mana=%.0f step=%d escort=%d obstacles=%d%n",
					maiev.getX(), maiev.getY(), maiev.getLife(), maiev.getMana(), escortWaypoints.getOrDefault(maiev.getHandleId(), 0),
					escort.size(), obstacles.stream().filter(obstacle -> !obstacle.isDead()).count());
		}
	}

	private static void verifyIslandOpening(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		check(tick < (ISLAND_PLAYTHROUGH ? 52_000 : ISLAND_LANDFALL ? 30_000 : ISLAND_VOYAGE ? 15_000 : 4000),
				"the original island expedition stalled");
		if (ISLAND_PLAYTHROUGH && tombApproachOpened) {
			final CQuest tomb = object(globals, "udg_QuestKillNaga");
			if (tomb != null && tomb.isCompleted()) {
				if (!tombVictoryReported) {
					tombVictoryReported = true;
					System.out.println("[FrozenThroneOpeningProbe] defeated the original Tomb Guardians");
				}
				final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
				final var stored = cache == null ? null : cache.getStoredUnit("NightElfX03", "Maiev");
				if (stored != null) carriedMaiev = CampaignPlaythroughProbe.heroProperties(stored);
				HumanCampaignPlaythroughProbe.continueVictory(ui);
				return;
			}
			if (maiev != null && ui.isUserControlEnabled() && tick >= orderTick) {
				orderTick = tick + 16;
				fightTombGuardians(viewer, ui, globals, maiev);
			}
			return;
		}
		if (ISLAND_VOYAGE && islandGoldDelivered) {
			if (maiev != null && ui.isUserControlEnabled() && tick >= orderTick) {
				orderTick = tick + 16;
				verifyIslandVoyage(viewer, ui, globals, maiev);
			}
			return;
		}
		final Trigger skip = object(globals, "gg_trg_Intro_Skipped");
		if (!islandIntroSkipped && skip != null && skip.isEnabled() && !ui.isUserControlEnabled()) {
			islandIntroSkipped = ui.keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
		}
		if (maiev == null || !ui.isUserControlEnabled() || maiev.isPaused() || tick < orderTick) return;
		check(!maiev.isDead(), "Maiev was lost on the starting island");
		orderTick = tick + 16;
		final int player = maiev.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final CUnit sourceMine = object(globals, "gg_unit_ngol_0015");
		final CUnit mine = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player
				&& !unit.isDead() && !unit.isConstructing()
				&& unit.getFirstAbilityOfType(CAbilityEntangledMine.class) != null).findFirst().orElse(null);
		if (mine == null || sourceMine == null) return;
		check(mine.distanceSquaredNoCollision(sourceMine) < 64 * 64, "the original mine overlay is misplaced");
		if (islandStartingGold < 0) {
			final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
			final var savedMaiev = cache == null ? null : cache.getStoredUnit("NightElfX02", "Maiev");
			check(savedMaiev != null && snapshot(maiev).equals(CampaignPlaythroughProbe.heroProperties(savedMaiev)),
					"the island opening did not restore Maiev from the earned Chapter One cache");
			islandStartingGold = owner.getGold();
		}
		for (final CUnit unit : game.getUnits()) {
			if (unit.getPlayerIndex() == player && !unit.isDead() && !unit.isHidden()
					&& War3ID.fromString("ewsp").equals(unit.getTypeId())) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, mine);
			}
		}
		if (owner.getGold() > islandStartingGold) {
			System.out.println("[FrozenThroneOpeningProbe] restored Maiev and mined gold from the original NightElfX02 settlement");
			islandGoldDelivered = true;
			if (ISLAND_VOYAGE) {
				originalIslandTransports = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player
						&& ISLAND_TRANSPORT.equals(unit.getTypeId())).map(CUnit::getHandleId).collect(Collectors.toSet());
			}
			if (!ISLAND_VOYAGE) {
				finished = true;
				Gdx.app.exit();
			}
		}
	}

	private static void verifyIslandVoyage(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		final var game = viewer.simulation;
		final int player = maiev.getPlayerIndex();
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final CUnit naisha = object(globals, "udg_Naisha");
		check(!maiev.isDead() && naisha != null && !naisha.isDead(), "lost Maiev or Naisha during the island expedition");
		if (islandExpedition.isEmpty()) {
			if (ISLAND_LANDFALL) {
				final int expeditionArchers = ISLAND_PLAYTHROUGH ? 8 : 6;
				final var archers = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
						&& "earc".equals(unit.getTypeId().toString())).limit(expeditionArchers).toList();
				if (archers.size() < expeditionArchers) {
					final var owner = game.getPlayer(player);
					if (ISLAND_PLAYTHROUGH && owner.getFoodCap() - owner.getFoodUsed() < 2) {
						prepareIslandMoonWell(viewer, player);
					}
					final CUnit ancient = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player
							&& !unit.isDead() && "eaom".equals(unit.getTypeId().toString()) && unit.getBuildQueue()[0] == null)
							.findFirst().orElse(null);
					if (ancient != null) HumanCampaignPlaythroughProbe.train(viewer, orders, ancient, "earc");
					if (game.getGameTurnTick() >= reportTick) {
						reportTick = game.getGameTurnTick() + 400;
						System.out.printf("[FrozenThroneOpeningProbe] gathering the escort, Archers=%d gold=%d wood=%d food=%d/%d%n",
								archers.size(), owner.getGold(), owner.getLumber(), owner.getFoodUsed(), owner.getFoodCap());
					}
					return;
				}
				final var expedition = new ArrayList<>(List.of(maiev, naisha));
				expedition.addAll(archers);
				islandExpedition = List.copyOf(expedition);
			}
			else islandExpedition = List.of(maiev, naisha);
		}
		final CUnit transport = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player
				&& !unit.isDead() && ISLAND_TRANSPORT.equals(unit.getTypeId())
				&& !originalIslandTransports.contains(unit.getHandleId())).findFirst().orElse(null);
		if (transport == null) {
			final CUnit shipyard = object(globals, "gg_unit_nshp_0013");
			final var sale = shipyard == null ? null : shipyard.getFirstAbilityOfType(CAbilitySellUnits.class);
			for (final CUnit unit : islandExpedition) {
				if (unit.isDead()) continue;
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, ISLAND_DOCK_ROUTE,
						ISLAND_DOCK_ROUTE.length - 1, 120, escortWaypoints);
			}
			ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
			if (!islandTransportPurchased && sale != null && shipyard.getNeutralBuildingData().getSelectedPlayerUnit(player) != null) {
				final int stockIndex = sale.getUnitsSold().indexOf(ISLAND_TRANSPORT);
				check(stockIndex >= 0, "the original shipyard is missing its transport");
				islandTransportPurchased = HumanCampaignPlaythroughProbe.immediate(viewer, orders, shipyard,
						player | ((stockIndex + 1) << 8));
			}
			return;
		}
		check(islandTransportPurchased, "the transport was not purchased from the original shipyard");
		islandFerry = transport;
		if (!expeditionBoarded) {
			for (final CUnit unit : islandExpedition) {
				if (!unit.isHidden() && !unit.isDead()) {
					CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, transport);
				}
			}
			if (!maiev.isHidden() || !naisha.isHidden()
					|| islandExpedition.stream().anyMatch(unit -> !unit.isDead() && !unit.isHidden())) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, transport, -720, -560);
				return;
			}
			expeditionBoarded = true;
			System.out.println("[FrozenThroneOpeningProbe] Maiev and Naisha boarded the purchased NightElfX02 transport");
		}
		if (channelSailed) {
			verifyIslandLandfall(viewer, ui, globals, maiev, naisha, transport, orders);
			return;
		}
		final int channelEnd = ISLAND_CHANNEL_ROUTE.length - 1;
		CampaignPlaythroughProbe.followRoute(viewer, orders, transport, ISLAND_CHANNEL_ROUTE,
				channelEnd, 145, harborCombatWaypoints);
		ui.getCameraManager().setTarget(transport.getX(), transport.getY());
		if (transport.distanceSquaredNoCollision(ISLAND_CHANNEL_ROUTE[channelEnd][0], ISLAND_CHANNEL_ROUTE[channelEnd][1])
				< 160 * 160) {
			check(transport.getCargoData().getCargoCount() >= 2, "the expedition was lost while sailing");
			System.out.println("[FrozenThroneOpeningProbe] sailed the original NightElfX02 channel with both passengers aboard");
			channelSailed = true;
			if (ISLAND_LANDFALL) harborCombatWaypoints.remove(transport.getHandleId());
			else {
				finished = true;
				Gdx.app.exit();
			}
		}
	}

	private static void prepareIslandMoonWell(final War3MapViewer viewer, final int player) {
		final var owned = viewer.simulation.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()).toList();
		if (owned.stream().anyMatch(unit -> MOON_WELL.equals(unit.getTypeId()) && unit.isConstructing())) return;
		if (owned.stream().anyMatch(unit -> unit.getCurrentOrder() != null && unit.getCurrentOrder().getOrderId() == MOON_WELL.getValue())) return;
		final CUnit builder = owned.stream().filter(unit -> !unit.isHidden() && "ewsp".equals(unit.getTypeId().toString()))
				.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(3008, -4352))).orElse(null);
		if (builder != null) NightElfOpeningPlaythroughProbe.buildNear(viewer, builder, MOON_WELL, 3008, -4352);
	}

	private static void verifyIslandLandfall(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals, final CUnit maiev,
			final CUnit naisha, final CUnit transport, final CPlayerUnitOrderExecutor orders) {
		final var game = viewer.simulation;
		if (transport.getCargoData().isEmpty() && !maiev.isHidden() && !naisha.isHidden()) {
			if (!northernPassengersUnloaded) {
				northernPassengersUnloaded = true;
				System.out.printf("[FrozenThroneOpeningProbe] landed Maiev at %.0f,%.0f and Naisha at %.0f,%.0f%n",
						maiev.getX(), maiev.getY(), naisha.getX(), naisha.getY());
			}
			final float shoreX = 1712;
			final float shoreY = 7728;
			for (final CUnit unit : islandExpedition) {
				if (unit == maiev || unit.isDead() || unit.isHidden()) continue;
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, TOMB_APPROACH, 1, 90, tombSteps);
			}
			if (maiev.getLife() < maiev.getMaximumLife() * 0.6f) {
				CampaignPlaythroughProbe.useInventoryItem(viewer, orders, maiev, "phea");
			}
			if (maiev.distanceSquaredNoCollision(shoreX, shoreY) >= 120 * 120) {
				CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink, shoreX, shoreY);
			}
			ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
			final CQuest tomb = object(globals, "udg_QuestKillNaga");
			if (maiev.getY() >= 7680 && tomb != null && tomb.isDiscovered()) {
				tombApproachOpened = true;
				System.out.println("[FrozenThroneOpeningProbe] disembarked the expedition and opened the original northern tomb approach");
				if (!ISLAND_PLAYTHROUGH) {
					finished = true;
					Gdx.app.exit();
				}
			}
			return;
		}
		final int end = ISLAND_NORTH_ROUTE.length - 1;
		final float[] shore = ISLAND_NORTH_ROUTE[end];
		if (transport.distanceSquaredNoCollision(shore[0], shore[1]) < 24 * 24) {
			CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, transport, OrderIds.unloadall, shore[0], shore[1]);
		}
		else CampaignPlaythroughProbe.followRoute(viewer, orders, transport, ISLAND_NORTH_ROUTE, end, 140, harborCombatWaypoints);
		ui.getCameraManager().setTarget(transport.getX(), transport.getY());
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 480;
			System.out.printf("[FrozenThroneOpeningProbe] northern channel transport %.0f,%.0f passengers=%d%n",
					transport.getX(), transport.getY(), transport.getCargoData().getCargoCount());
		}
	}

	private static void fightTombGuardians(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit maiev) {
		final var game = viewer.simulation;
		check(!maiev.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()), "lost Maiev at the Tomb Guardians");
		final var player = game.getPlayer(maiev.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		final List<CUnit> guards = object(globals, "udg_TombGuards");
		check(guards != null && guards.size() == 8, "the original Tomb Guardian group is missing members");
		final CDestructable gate = game.getDestructables().stream().filter(destructable -> NORTHERN_GATE.equals(destructable.getTypeId())
				&& destructable.distanceSquaredNoCollision(1120, 8032) < 32 * 32).findFirst().orElse(null);
		check(gate != null, "the northern Ruins Gate is missing");
		if (resupply != null || gate.isDead() && islandExpedition.stream().filter(unit -> unit != maiev && !unit.isDead()).count() <= 2) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, TOMB_APPROACH[tombRefuge][0], TOMB_APPROACH[tombRefuge][1]);
			if (resupply == null) {
				resupply = ResupplyStep.RETURNING;
				ferrySteps.clear();
			}
			ferryTombReinforcements(viewer, ui, maiev, orders);
			return;
		}
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, player.getId())).toList();
		final var priority = enemies.stream().filter(unit -> guards.contains(unit) && islandExpedition.stream()
				.anyMatch(ally -> ally != maiev && !ally.isDead() && ally.canReach(unit, 580)))
				.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		final float[] front = TOMB_APPROACH[tombFront];
		final long escortAtFront = islandExpedition.stream().filter(unit -> unit != maiev && !unit.isDead() && !unit.isHidden()
				&& unit.distanceSquaredNoCollision(front[0], front[1]) < 570 * 570).count();
		if (gate.isDead() && tombFront < TOMB_APPROACH.length - 1 && escortAtFront >= 3) tombFront++;
		final long guardsAlive = guards.stream().filter(unit -> !unit.isDead()).count();
		for (final CUnit unit : islandExpedition) {
			if (unit.isDead() || unit.isHidden() || unit.isPaused() || unit.getCurrentBehavior() != null
					&& unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final CUnit enemy = priority != null && unit.canReach(priority, 700) ? priority : enemies.stream()
					.filter(foe -> unit.canReach(foe, 570)).min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			final boolean wounded = unit.getLife() < unit.getMaximumLife() * 0.82f;
			if (unit == maiev && (wounded
					|| gate.isDead() && guardsAlive > 2 && enemy != null && unit.canReach(enemy, 430))) {
				if (wounded && (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe"))) continue;
				final float[] previousRefuge = TOMB_APPROACH[tombRefuge];
				if (enemy != null && tombRefuge > 2 && unit.distanceSquaredNoCollision(previousRefuge[0], previousRefuge[1]) < 215 * 215) {
					tombRefuge--;
				}
				final float[] refuge = TOMB_APPROACH[tombRefuge];
				if (enemy != null && CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.blink,
						refuge[0], refuge[1])) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
				continue;
			}
			if (unit == maiev && guardsAlive <= 2 && enemies.stream().filter(foe -> unit.canReach(foe, 390)).limit(2).count() == 2
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.fanofknives)) continue;
			if (!gate.isDead()) {
				if (unit.canReach(gate, unit == maiev ? 260 : 550)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, gate)) continue;
				if (unit == maiev) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, TOMB_GATE_CLEARANCE,
							TOMB_GATE_CLEARANCE.length - 1, 110, northGateSteps);
				}
				else {
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, TOMB_APPROACH, 1, 90, tombSteps);
				}
				continue;
			}
			if (unit == maiev && guardsAlive <= 2 && enemy != null && unit.canReach(enemy, 480)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.shadowstrike, enemy)) continue;
			final int escortStep = tombSteps.getOrDefault(unit.getHandleId(), 0);
			if (enemy != null && (unit == maiev ? guardsAlive <= 2 : escortStep >= 3)
					&& unit.canReach(enemy, unit == maiev ? 175 : 565)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			final int end = unit == maiev && guardsAlive > 2 ? tombRefuge : tombFront;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, TOMB_APPROACH, end, 135, tombSteps);
		}
		ui.getCameraManager().setTarget(maiev.getX(), maiev.getY());
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 400;
			System.out.printf("[FrozenThroneOpeningProbe] tomb guards=%d gate=%.0f invulnerable=%s Maiev=%.0f,%.0f life=%.0f escort=%d front=%d/%d%n",
					guardsAlive, gate.getLife(), gate.isInvulnerable(), maiev.getX(), maiev.getY(), maiev.getLife(),
					islandExpedition.stream().filter(unit -> unit != maiev && !unit.isDead()).count(), tombFront, escortAtFront);
		}
	}

	private static void ferryTombReinforcements(final War3MapViewer viewer, final MeleeUI ui, final CUnit maiev,
			final CPlayerUnitOrderExecutor orders) {
		final var game = viewer.simulation;
		check(islandFerry != null && !islandFerry.isDead(), "the purchased transport was lost while bringing reinforcements");
		final int player = maiev.getPlayerIndex();
		if (replacements.isEmpty()) {
			final var archers = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
					&& !islandExpedition.contains(unit) && "earc".equals(unit.getTypeId().toString())).limit(8).toList();
			if (archers.size() >= 8) replacements = archers;
			else {
				final CUnit ancient = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
						&& "eaom".equals(unit.getTypeId().toString()) && unit.getBuildQueue()[0] == null).findFirst().orElse(null);
				if (ancient != null) HumanCampaignPlaythroughProbe.train(viewer, orders, ancient, "earc");
			}
		}
		ui.getCameraManager().setTarget(islandFerry.getX(), islandFerry.getY());
		switch (resupply) {
		case RETURNING:
			CampaignPlaythroughProbe.followRoute(viewer, orders, islandFerry, FERRY_SOUTH, FERRY_SOUTH.length - 1, 105, ferrySteps);
			if (islandFerry.distanceSquaredNoCollision(-720, -560) < 145 * 145) resupply = ResupplyStep.BOARDING;
			break;
		case BOARDING:
			for (final CUnit unit : replacements) {
				if (unit.isHidden() || unit.isDead()) continue;
				if (unit.distanceSquaredNoCollision(-720, -560) < 520 * 520) {
					CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, islandFerry);
				}
				else CampaignPlaythroughProbe.followRoute(viewer, orders, unit, ISLAND_DOCK_ROUTE, ISLAND_DOCK_ROUTE.length - 1, 120, escortWaypoints);
			}
			if (!islandFerry.getCargoData().isEmpty() && replacements.stream().filter(unit -> !unit.isDead()).allMatch(CUnit::isHidden)) {
				ferrySteps.clear();
				resupply = ResupplyStep.SAILING;
			}
			break;
		case SAILING:
			CampaignPlaythroughProbe.followRoute(viewer, orders, islandFerry, FERRY_NORTH, FERRY_NORTH.length - 1, 105, ferrySteps);
			if (islandFerry.distanceSquaredNoCollision(1328, 7344) < 24 * 24) resupply = ResupplyStep.LANDING;
			break;
		case LANDING:
			CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, islandFerry, OrderIds.unloadall, 1328, 7344);
			if (islandFerry.getCargoData().isEmpty()) {
				final var party = new ArrayList<>(islandExpedition.stream().filter(unit -> !unit.isDead()).toList());
				party.addAll(replacements.stream().filter(unit -> !unit.isDead() && !unit.isHidden()).toList());
				check(party.size() > 3, "the original reinforcement landing produced too few archers");
				islandExpedition = List.copyOf(party);
				replacements = List.of();
				tombFront = TOMB_RALLY;
				tombSteps.clear();
				resupply = null;
				System.out.println("[FrozenThroneOpeningProbe] ferried a new Archer escort to the Tomb Guardians");
			}
			break;
		}
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 400;
			System.out.printf("[FrozenThroneOpeningProbe] reinforcements=%s transport %.0f,%.0f loaded=%d%n",
					resupply, islandFerry.getX(), islandFerry.getY(), islandFerry.getCargoData().getCargoCount());
		}
	}

	private static void fightIslandDefender(final War3MapViewer viewer,
			final CPlayerUnitOrderExecutor orders,
			final CUnit maiev, final CUnit defender, final boolean withdrawing) {
		final float[] mainland = NORTH_HARBOR_ROUTE[NORTH_HARBOR_ROUTE.length - 1];
		if (withdrawing) {
			if (maiev.getX() > 2630) {
				CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink,
						MIDDLE_ISLAND[0], MIDDLE_ISLAND[1]);
			}
			else if (maiev.getX() > 2200) {
				if (maiev.distanceSquaredNoCollision(mainland[0], mainland[1]) > 990 * 990) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, MIDDLE_ISLAND[0], MIDDLE_ISLAND[1]);
				}
				else CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink,
						mainland[0], mainland[1]);
			}
			else {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, maiev, mainland[0], mainland[1]);
			}
			return;
		}
		if (maiev.canReach(defender, 340) && (maiev.getMana() >= (maiev.getX() > 2630 ? 215 : 150)
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, maiev, OrderIds.fanofknives)
				|| CampaignPlaythroughProbe.targetOrder(viewer, orders, maiev, OrderIds.attack, defender))) return;
		if (maiev.getX() > 2200) {
			CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink,
					EAST_ISLAND[0], EAST_ISLAND[1]);
		}
		else if (maiev.distanceSquaredNoCollision(MIDDLE_ISLAND[0], MIDDLE_ISLAND[1]) < 992 * 992) {
			CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink,
					MIDDLE_ISLAND[0], MIDDLE_ISLAND[1]);
		}
		else {
			if (maiev.distanceSquaredNoCollision(mainland[0], mainland[1]) < 500 * 500
					&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, maiev, OrderIds.blink,
							mainland[0], mainland[1])) return;
			CampaignPlaythroughProbe.followRoute(viewer, orders, maiev, NORTH_HARBOR_ROUTE,
					NORTH_HARBOR_ROUTE.length - 1, 120, harborCombatWaypoints);
		}
	}

	private static void resetRefuges(final CUnit maiev) {
		refugeX = forwardRefugeX = rearRefugeX = maiev.getX();
		refugeY = forwardRefugeY = rearRefugeY = maiev.getY();
	}

	private static Properties snapshot(final CUnit maiev) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(maiev));
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Frozen Throne opening audit: " + message);
	}
}
