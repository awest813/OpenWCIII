package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.ArrayJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;

/** Follows the original caravan route while the map script controls Cairne and the wagons. */
public final class OrcMarchPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.orcMarchAudit");
	private static final War3ID CARAVAN_KODO = War3ID.fromString("oosc");
	private static final War3ID RAIDER = War3ID.fromString("orai");
	private static final War3ID HEALING_SCROLL = War3ID.fromString("shea");
	private static final Oasis[] OASES = { new Oasis(26, 30, 32, 3584, -4608),
			new Oasis(66, 70, 72, 3776, 4672) };
	private static int orderTick;
	private static int reportTick;
	private static int oasisRestStarted;
	private static int lastReached;
	private static int lastAdvanceTick;
	private static boolean interludeVerified;
	private static boolean quakeRollObserved;
	private static boolean merchantVerified;
	private static int nextOasis;
	private static boolean retreating;
	private static boolean returningToOasis;
	private static Properties carriedThrall;

	private OrcMarchPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		if (map.endsWith("/orc02interlude.w3m")) {
			if (!interludeVerified) {
				interludeVerified = true;
				System.out.println("[OrcMarchProbe] entered the original Orc02 story interlude");
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final CUnit thrall = object(globals, "udg_Thrall");
		if (map.endsWith("/orc03.w3m")) {
			if (thrall == null) return;
			check(quakeRollObserved, "the original earthquake camera did not roll");
			check(interludeVerified && carriedThrall != null && carriedThrall.equals(snapshot(thrall)),
					"Thrall carryover mismatch");
			System.out.println("[OrcMarchProbe] entered Orc03; Thrall carryover verified");
			Gdx.app.exit();
			return;
		}
		check(map.endsWith("/orc02.w3m"), "unexpected map " + map);
		check(!"Defeat".equals(ui.getScoreDialogTitle()), "native defeat");
		if (!quakeRollObserved && Math.abs(ui.getCameraManager().getRoll()) > Math.toRadians(2)) {
			quakeRollObserved = true;
			System.out.println("[OrcMarchProbe] rendered the original earthquake camera roll");
		}
		final CUnit cairne = object(globals, "udg_Cairne");
		final CQuest caravan = object(globals, "udg_QuestCaravan");
		final CQuest oases = object(globals, "udg_QuestOases");
		if (thrall == null || cairne == null || caravan == null || oases == null || !oases.isDiscovered()) return;
		check(!thrall.isDead() && !cairne.isDead(), "a required hero died (Thrall " + Math.round(thrall.getLife())
				+ ", Cairne " + Math.round(cairne.getLife()) + ")");
		final var wagons = game.getUnits().stream().filter(unit -> CARAVAN_KODO.equals(unit.getTypeId()) && !unit.isDead()).toList();
		check(wagons.size() >= 2 || ui.getVisibleScriptDialog() == null, "native defeat after losing three wagons");
		if (caravan.isCompleted() && oases.isCompleted()) {
			if (carriedThrall == null) {
				check(completed(globals, "udg_QuestOasisReq01") && completed(globals, "udg_QuestOasisReq02")
						&& wagons.size() >= 2, "the caravan missed an oasis or lost too many wagons");
				carriedThrall = snapshot(thrall);
				System.out.printf("[OrcMarchProbe] completed the original caravan escort with %d surviving wagons%n", wagons.size());
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final int tick = game.getGameTurnTick();
		if (!ui.isUserControlEnabled() || thrall.isHidden() || thrall.isPaused() || game.isGamePaused()
				|| tick < orderTick) return;
		orderTick = tick + 16;
		check(tick < 48_000, "the caravan stalled");
		final int reached = number(globals, "udg_BreadcrumbReached");
		if (reached > lastReached) {
			lastReached = reached;
			lastAdvanceTick = tick;
		}
		final int current = number(globals, "udg_BreadcrumbCurrent");
		final int allowed = number(globals, "udg_BreadcrumbAllowed");
		final int total = number(globals, "udg_BreadcrumbTotal");
		final var path = globals.getGlobal("udg_zBreadcrumbArray").visit(ArrayJassValueVisitor.getInstance());
		final int nextBreadcrumb = Math.min(total, Math.max(reached + 4, Math.min(reached + 6, allowed + 4)));
		final AbilityPointTarget rally = path.get(nextBreadcrumb).visit(ObjectJassValueVisitor.getInstance());
		final AbilityPointTarget vanguardRally = path.get(Math.min(total, Math.max(reached + 3, allowed + 3)))
				.visit(ObjectJassValueVisitor.getInstance());
		final float caravanX = Math.round(cairne.getX() / 128) * 128;
		final float caravanY = Math.round(cairne.getY() / 128) * 128;
		final var rearWagon = wagons.stream()
				.max(Comparator.comparingDouble(wagon -> wagon.distanceSquaredNoCollision(rally.getX(), rally.getY())))
				.orElse(cairne);
		final float routeX = rally.getX() - caravanX;
		final float routeY = rally.getY() - caravanY;
		final float routeLength = Math.max(1, (float) Math.hypot(routeX, routeY));
		final float forwardX = routeX / routeLength;
		final float forwardY = routeY / routeLength;
		final var owner = game.getPlayer(thrall.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(owner.getId());
		OrcOpeningPlaythroughProbe.learnSpells(viewer, orders, thrall);
		final var assailants = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && !unit.getCurrentAttacks().isEmpty() && unit.getPlayerIndex() != owner.getId()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, owner.getId())).toList();
		final CUnit wagonAttacker = assailants.stream().filter(unit -> wagons.stream()
				.anyMatch(wagon -> wagon.distanceSquaredNoCollision(unit) < 620 * 620))
				.min(Comparator.comparingDouble(unit -> wagons.stream().mapToDouble(unit::distanceSquaredNoCollision)
						.min().orElse(Double.POSITIVE_INFINITY))).orElse(null);
		final boolean protectCairne = cairne.getLife() < cairne.getMaximumLife() * 0.88f
				|| reached >= 20 && reached <= 36 && cairne.distanceSquaredNoCollision(rearWagon) > 1150 * 1150;
		final CUnit cairneAttacker = protectCairne
				? assailants.stream().filter(unit -> cairne.distanceSquaredNoCollision(unit) < 650 * 650)
						.min(Comparator.comparingDouble(cairne::distanceSquaredNoCollision)).orElse(null)
				: null;
		final var escort = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == owner.getId() && !unit.isDead()
				&& !unit.isHidden() && !unit.isPaused() && !unit.getCurrentAttacks().isEmpty()).toList();
		final Oasis oasis = getRestingOasis(reached, tick, escort);
		if (!merchantVerified && escort.stream().anyMatch(unit -> RAIDER.equals(unit.getTypeId()))) {
			merchantVerified = true;
			System.out.println("[OrcMarchProbe] received the original merchant's Raider reinforcements");
		}
		final boolean merchantApproach = reached >= 46 && reached < 49 && allowed < 52;
		final boolean needsScout = reached >= 30 && allowed <= reached + 1 && tick - lastAdvanceTick >= 960;
		final boolean clearFirstBend = reached >= 8 && reached <= 12 && current > reached
				&& tick - lastAdvanceTick >= 320 && wagonAttacker == null;
		retreating = thrall.getLife() < thrall.getMaximumLife() * (retreating ? 0.76f : 0.60f);
		if (retreating && (reached >= 33 || thrall.getLife() < thrall.getMaximumLife() * 0.45f)
				&& CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "phea")) return;
		if (retreating && reached >= 40 && CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "shea")) return;
		returningToOasis = reached >= 33 && reached < 66
				&& thrall.getLife() < thrall.getMaximumLife() * (returningToOasis ? 0.72f : 0.27f);
		boolean snareOrdered = escort.stream().anyMatch(unit -> unit.getCurrentOrder() != null
				&& unit.getCurrentOrder().getOrderId() == OrderIds.ensnare);
		final boolean lastCanyon = reached >= 70 && reached < 89 && oasis == null;
		final float thrallMissingLife = thrall.getMaximumLife() - thrall.getLife();
		for (int position = 0; position < escort.size(); position++) {
			final CUnit unit = escort.get(position);
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) {
				continue;
			}
			if (unit == thrall && returningToOasis) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, OASES[0].x, OASES[0].y);
				continue;
			}
			if (unit == thrall && reached <= 32 && needsScout && oasis == null
					&& cairne.getLife() >= cairne.getMaximumLife() * 0.75f
					&& thrall.getLife() >= thrall.getMaximumLife() * 0.65f) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, rally.getX(), rally.getY());
				continue;
			}
			if (clearFirstBend && unit != thrall) {
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, -2384 + (position % 3) * 96,
						-7568 + (position / 3) * 96);
				continue;
			}
			if (cairneAttacker != null) {
				if (unit == thrall && unit.getLife() > unit.getMaximumLife() * 0.45f
						&& unit.canReach(cairneAttacker, 650)
						&& OrcOpeningPlaythroughProbe.castSpells(viewer, orders, unit, cairneAttacker)) continue;
				if ((unit != thrall || !retreating)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, cairneAttacker)) continue;
			}
			if (protectCairne && (unit == thrall ? !retreating : position % 2 == 1)
					&& unit.distanceSquaredNoCollision(cairne) > 300 * 300) {
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit,
						caravanX - forwardY * (position % 3 - 1) * 96,
						caravanY + forwardX * (position % 3 - 1) * 96);
				continue;
			}
			final CUnit enemy = assailants.stream().filter(target -> unit.distanceSquaredNoCollision(target) < 720 * 720)
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (lastCanyon && unit == thrall && enemy != null && thrallMissingLife > 240) {
				if (thrall.getLife() > 180 && thrall.distanceSquaredNoCollision(enemy) < 600 * 600
						&& OrcOpeningPlaythroughProbe.castSpells(viewer, orders, thrall, enemy)) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, thrall, caravanX - forwardX * 400,
						caravanY - forwardY * 400);
				continue;
			}
			if (unit == thrall && merchantApproach && thrall.getLife() > thrall.getMaximumLife() * 0.32f) {
				if (thrall.getX() < -2752) CampaignPlaythroughProbe.pointOrder(viewer, orders, thrall, -2672, -832);
				else if (thrall.getX() < -1780) CampaignPlaythroughProbe.pointOrder(viewer, orders, thrall, -1648, -1008);
				else CampaignPlaythroughProbe.pointOrder(viewer, orders, thrall, -896, -544);
				continue;
			}
			if (unit == thrall && oasis == null && needsScout && thrall.getLife() >= thrall.getMaximumLife() * 0.30f
					&& (!lastCanyon || enemy == null)) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, thrall, rally.getX(), rally.getY());
				continue;
			}
			if (unit == thrall && enemy != null && retreating && oasis == null
					&& cairne.getLife() >= cairne.getMaximumLife() * 0.45f) {
				if (thrall.getLife() > thrall.getMaximumLife() * 0.40f
						&& thrall.distanceSquaredNoCollision(enemy) < 500 * 500
						&& OrcOpeningPlaythroughProbe.castSpells(viewer, orders, thrall, enemy)) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, thrall,
						caravanX - forwardX * 224, caravanY - forwardY * 224);
				continue;
			}
			final CUnit focus = unit != thrall && wagonAttacker != null && unit.canReach(wagonAttacker, 1100)
					? wagonAttacker : enemy;
			if (focus != null && unit == thrall && OrcOpeningPlaythroughProbe.castSpells(viewer, orders, thrall, focus)) continue;
			if (!snareOrdered && focus != null && RAIDER.equals(unit.getTypeId()) && unit.canReach(focus, 500)
					&& !focus.isUnitType(CUnitTypeJass.SNARED)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.ensnare, focus)) {
				snareOrdered = true;
				continue;
			}
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, focus)) continue;
			if (oasis != null) {
				final float x = unit == thrall ? oasis.x : oasis.x - 80 + position % 3 * 80;
				final float y = unit == thrall ? oasis.y : oasis.y + 128 + position / 3 * 84;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, x, y);
				continue;
			}
			if (unit == thrall) {
				if (!CampaignPlaythroughProbe.hasItem(thrall, "shea")) {
					final var scroll = game.getItems().stream().filter(item -> HEALING_SCROLL.equals(item.getTypeId())
							&& !item.isDead() && !item.isHidden() && item.getContainedInventory() == null
							&& thrall.distanceSquaredNoCollision(item) < 480 * 480).findFirst().orElse(null);
					if (CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.smart, scroll)) continue;
				}
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, rally.getX(), rally.getY());
			}
			else {
				final boolean rearGuard = position % 3 == 2;
				if (lastCanyon && !rearGuard) {
					CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit,
							vanguardRally.getX() - forwardY * (position % 4 - 1) * 90,
							vanguardRally.getY() + forwardX * (position % 4 - 1) * 90);
					continue;
				}
				final float anchorX = rearGuard ? Math.round(rearWagon.getX() / 128) * 128 : caravanX;
				final float anchorY = rearGuard ? Math.round(rearWagon.getY() / 128) * 128 : caravanY;
				final float ahead = rearGuard ? -112 : 160;
				final float spread = ((position % 2 == 0 ? -1 : 1) * (132 + (position / 3) * 80));
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit,
						anchorX + forwardX * ahead - forwardY * spread,
						anchorY + forwardY * ahead + forwardX * spread);
			}
		}
		ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
		if (tick >= reportTick) {
			reportTick = tick + 360;
			System.out.printf("[OrcMarchProbe] tick=%d caravan=%d/%d current=%d allowed=%d wagons=%s escort=%d Thrall=%.0f,%.0f life=%.0f Cairne=%.0f,%.0f life=%.0f%n",
					tick, reached, total, current, allowed, wagons.stream()
							.map(unit -> Math.round(unit.getX()) + "," + Math.round(unit.getY()) + ":" + Math.round(unit.getLife())).toList(),
					escort.size() - 1, thrall.getX(), thrall.getY(), thrall.getLife(),
					cairne.getX(), cairne.getY(), cairne.getLife());
		}
	}

	private static Oasis getRestingOasis(final int marker, final int tick, final List<CUnit> escort) {
		while (nextOasis < OASES.length) {
			final Oasis oasis = OASES[nextOasis];
			if (marker < oasis.firstMarker) return null;
			if (oasisRestStarted == 0) oasisRestStarted = tick;
			if (marker <= oasis.lastMarker && tick - oasisRestStarted < 1800
					&& (marker < oasis.arrivalMarker || escort.stream().anyMatch(unit -> unit.getLife() < unit.getMaximumLife() * 0.88f))) {
				return oasis;
			}
			nextOasis++;
			oasisRestStarted = 0;
		}
		return null;
	}

	private record Oasis(int firstMarker, int arrivalMarker, int lastMarker, float x, float y) {
	}

	private static Properties snapshot(final CUnit hero) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero));
	}

	private static boolean completed(final GlobalScope globals, final String name) {
		final CQuestItem objective = object(globals, name);
		return objective != null && objective.isCompleted();
	}

	private static int number(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(IntegerJassValueVisitor.getInstance());
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Orc march audit: " + message);
	}
}
