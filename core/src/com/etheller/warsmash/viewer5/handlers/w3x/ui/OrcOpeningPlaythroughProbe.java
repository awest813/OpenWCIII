package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;

/** Replays Landfall using the original rescue, Cairne escort, and centaur wave triggers. */
public final class OrcOpeningPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.orcOpeningAudit");
	private static final War3ID CHAIN_LIGHTNING = War3ID.fromString("AOcl");
	private static final War3ID FERAL_SPIRIT = War3ID.fromString("AOsf");
	private static final int FIRST_SURVIVORS = 8;
	private static final int SECOND_SHIP_APPROACH = 26;
	private static final float[][] SHORE = {
			{-8304, -5808}, {-8304, -4496}, {-7424, -3504}, {-6096, -3408}, {-5056, -3536},
			{-4240, -4304}, {-3264, -5440}, {-2864, -6512}, {-2528, -6952}, {-1504, -6152},
			{-784, -5264}, {-416, -5024}, {-128, -5488}, {464, -4400}, {992, -2784},
			{400, -2704}, {-880, -3248}, {-1968, -2352}, {-2736, -1008}, {-2736, 1328},
			{-2352, 1712}, {-1040, 1712}, {496, 176}, {1392, 176}, {2800, -1232},
			{3376, -1456}, {2576, -2256}, {1744, -2512}, {3840, -3432}, {4736, -4224}, {5568, -4616}
	};
	private static int waypoint;
	private static int orderTick;
	private static int reportTick;
	private static int defenseCompleteTick;
	private static boolean durationVerified;
	private static boolean shipsVerified;
	private static boolean defenseVerified;
	private static boolean endingSkipRequested;
	private static boolean shipPotionCollected;
	private static Properties carryover;

	private OrcOpeningPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) {
			return;
		}
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final CUnit thrall = object(globals, "udg_Thrall");
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		if (map.endsWith("/orc02.w3m")) {
			if (thrall == null) return;
			check(carryover != null && carryover.equals(snapshot(thrall)), "Thrall carryover mismatch");
			System.out.println("[OrcOpeningProbe] entered Orc02; Thrall carryover verified");
			Gdx.app.exit();
			return;
		}
		check(map.endsWith("/orc01.w3m"), "unexpected map " + map);
		check(!"Defeat".equals(ui.getScoreDialogTitle()), "native defeat");
		if ("Victory".equals(ui.getScoreDialogTitle())) {
			check(shipsVerified && defenseVerified && carryover != null, "victory without completed objectives");
			ui.keyDown(Input.Keys.C);
			return;
		}
		final CQuest ships = object(globals, "udg_QuestShips");
		final CQuest defense = object(globals, "udg_QuestCairne");
		final CUnit cairne = object(globals, "udg_CairneBloodhoof");
		if (thrall == null || ships == null || defense == null || !ships.isDiscovered()) return;
		if (!durationVerified) {
			final int authoredDuration = globals.getNativeById(globals.getNativeId("GetSoundDuration"))
					.call(List.of(globals.getGlobal("gg_snd_O01Cairne23")), globals, TriggerExecutionScope.EMPTY)
					.visit(IntegerJassValueVisitor.getInstance());
			check(authoredDuration == 12356, "the opening did not retain its authored dialogue timing");
			durationVerified = true;
			System.out.println("[OrcOpeningProbe] verified the authored 12356 ms dialogue duration");
		}
		check(!thrall.isDead(), "Thrall died");
		if (ships.isCompleted() && !shipsVerified) {
			shipsVerified = true;
			System.out.println("[OrcOpeningProbe] native ship rescue completed and Cairne encountered");
		}
		if (defense.isCompleted() && !defenseVerified) {
			check(cairne != null && !cairne.isDead(), "Cairne died");
			defenseVerified = true;
			defenseCompleteTick = game.getGameTurnTick();
			System.out.println("[OrcOpeningProbe] native centaur defense completed");
		}
		if (defenseVerified) {
			carryover = snapshot(thrall);
			if (ui.getVisibleScriptDialog() != null) {
				System.out.println("[OrcOpeningProbe] confirmed the native victory prompt");
				HumanCampaignPlaythroughProbe.continueVictory(ui);
				return;
			}
			final Trigger skip = object(globals, "gg_trg_Victory_Cinematic_Cancel");
			if (!endingSkipRequested && skip != null && skip.isEnabled()
					&& game.getGameTurnTick() - defenseCompleteTick >= 2_000) {
				endingSkipRequested = true;
				System.out.println("[OrcOpeningProbe] requested the original cinematic skip with Escape");
				ui.keyDown(Input.Keys.ESCAPE);
			}
			check(game.getGameTurnTick() - defenseCompleteTick < 3500,
					"the ending did not reach the victory screen");
			return;
		}
		final int tick = game.getGameTurnTick();
		if (thrall.isHidden() || thrall.isPaused() || !ui.isUserControlEnabled() || game.isGamePaused()
				|| tick < orderTick) return;
		orderTick = tick + 12;
		check(tick < 22000, "the shore or centaur defense stalled");
		final var owner = game.getPlayer(thrall.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(owner.getId());
		if (!shipPotionCollected && !shipsVerified && waypoint >= SECOND_SHIP_APPROACH) {
			final CDestructable crate = object(globals, "gg_dest_LTcr_2152");
			if (CampaignPlaythroughProbe.hasItem(thrall, "phea")) {
				shipPotionCollected = true;
				System.out.println("[OrcOpeningProbe] collected the original second-ship healing potion");
			}
			else if (crate != null && thrall.canReach(crate, 590)) {
				if (!crate.isDead() && CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.attack, crate)) {
					return;
				}
				final var potion = game.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
						&& item.getContainedInventory() == null && "phea".equals(item.getTypeId().toString())
						&& item.distanceSquaredNoCollision(3904, -4096) < 200 * 200).findFirst().orElse(null);
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.smart, potion)) return;
			}
		}
		learnSpells(viewer, orders, thrall);
		final int arrivalRadius = waypoint == 11 || waypoint == 12 || waypoint == 14 || waypoint == 28 ? 48
				: waypoint == FIRST_SURVIVORS ? 200 : 144;
		if (!shipsVerified && waypoint < SHORE.length - 1
				&& thrall.distanceSquaredNoCollision(SHORE[waypoint][0], SHORE[waypoint][1]) < arrivalRadius * arrivalRadius) {
			waypoint++;
		}
		final float x = defense.isDiscovered() && cairne != null ? cairne.getX() : SHORE[waypoint][0];
		final float y = defense.isDiscovered() && cairne != null ? Math.min(cairne.getY() - 160, 1872) : SHORE[waypoint][1];
		final var opponents = game.getUnits().stream()
				.filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
						&& !unit.getCurrentAttacks().isEmpty() && unit.getPlayerIndex() != owner.getId()
						&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
						&& unit.isVisible(game, owner.getId())).toList();
		final var threat = opponents.stream()
				.filter(unit -> thrall.distanceSquaredNoCollision(unit) < 580 * 580)
				.min(Comparator.comparingDouble(thrall::distanceSquaredNoCollision)).orElse(null);
		int escort = 0;
		for (final CUnit unit : game.getUnits()) {
			if (unit.getPlayerIndex() != owner.getId() || unit.isDead() || unit.isHidden() || unit.isPaused()
					|| unit.getCurrentAttacks().isEmpty()) continue;
			if (unit.getCurrentBehavior() != null
					&& unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final CUnit enemy = opponents.stream().filter(target -> unit.distanceSquaredNoCollision(target) < 760 * 760)
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (unit == thrall && enemy != null && castSpells(viewer, orders, unit, enemy)) continue;
			if (unit != thrall && threat != null && thrall.getLife() < thrall.getMaximumLife() * 0.75f
					&& unit.canReach(threat, 1050)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, threat)) continue;
			if (enemy != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (unit == thrall) CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, x, y);
			else {
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, x + (escort % 3 - 1) * 92,
						y + 100 + (escort / 3) * 84);
				escort++;
			}
		}
		ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
		if (tick >= reportTick) {
			reportTick = tick + 320;
			System.out.printf("[OrcOpeningProbe] tick=%d waypoint=%d Thrall=%.0f,%.0f life=%.0f escort=%d opponents=%d%n",
					tick, waypoint, thrall.getX(), thrall.getY(), thrall.getLife(), escort, opponents.size());
		}
	}

	static void learnSpells(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit thrall) {
		final War3ID skill = thrall.getAbilityLevel(CHAIN_LIGHTNING) <= thrall.getAbilityLevel(FERAL_SPIRIT)
				? CHAIN_LIGHTNING : FERAL_SPIRIT;
		final var canLearn = new BooleanAbilityActivationReceiver();
		thrall.getHeroData().checkCanUse(viewer.simulation, thrall, skill.getValue(), canLearn);
		if (canLearn.isOk()) {
			orders.issueImmediateOrder(thrall.getHandleId(), thrall.getHeroData().getHandleId(), skill.getValue(), false);
		}
	}

	static boolean castSpells(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit thrall,
			final CUnit enemy) {
		return CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.chainlightning, enemy)
				|| thrall.order(viewer.simulation, OrderIds.spiritwolf, null);
	}

	private static Properties snapshot(final CUnit hero) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero));
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Orc opening audit: " + message);
	}
}
