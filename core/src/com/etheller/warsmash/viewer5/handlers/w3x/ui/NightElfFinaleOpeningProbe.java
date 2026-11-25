package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityEntangledMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

/** Verifies the original handoff to the defence of Mount Hyjal and starts its economy. */
final class NightElfFinaleOpeningProbe {
	static final boolean ENABLED = Boolean.getBoolean("warsmash.nightElfFinaleOpening");
	static final boolean VICTORY = Boolean.getBoolean("warsmash.nightElfFinaleVictory");
	private static final float[][] DEFENCE_SITES = { { 2112, 1984 }, { 2432, 2176 }, { 2752, 1984 },
			{ 2240, 2624 }, { 2816, 2496 }, { 3328, 2304 }, { 2816, 3136 }, { 3200, 3264 }, { 3584, 3136 },
			{ 4224, 2592 }, { 4608, 3072 }, { 4896, 3456 }, { 4736, 3888 }, { 4528, 4224 } };
	private static final float[][] WELL_SITES = { { 3072, 4192 }, { 3328, 3968 }, { 3200, 3776 },
			{ 3648, 4416 }, { 3968, 4384 }, { 4352, 3776 } };
	private static final String[] TYRANDE_SKILLS = { "AEsf", "AEar", "AHfa", "AEst" };
	private static final String[] FURION_SKILLS = { "AEtq", "AEer", "AEfn", "AEah" };
	private static int nextOrder;
	private static int nextReport;
	private static boolean economyVerified;
	private static boolean victoryRecorded;
	private static boolean restoredHeroes;
	private static int initialGold = -1;
	private static long startingArchers;
	private static boolean finished;

	private NightElfFinaleOpeningProbe() {
	}

	static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals) {
		if (finished) return;
		final var game = viewer.simulation;
		check(game.getGameTurnTick() < (VICTORY ? 83_000 : 20_000) && !"Defeat".equals(ui.getScoreDialogTitle())
				&& !Boolean.TRUE.equals(globals.getGlobal("udg_ArchimondeHasWon").visit(BooleanJassValueVisitor.getInstance())),
				"Mount Hyjal ended in defeat or stalled");
		if (VICTORY && "Victory".equals(ui.getScoreDialogTitle())) {
			check(economyVerified, "the original economy was bypassed");
			victoryRecorded = true;
			System.out.println("[NightElfFinaleProbe] survived the native Mount Hyjal countdown and reached the original victory");
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final CUnit tyrande = get(globals, "udg_Tyrande");
		final CUnit furion = get(globals, "udg_Furion");
		final CQuest defence = get(globals, "udg_QuestArchimonde");
		final CTimer timer = get(globals, "udg_GameTimer");
		if (!ui.isUserControlEnabled() || tyrande == null || furion == null || defence == null
				|| !defence.isDiscovered() || timer == null || !timer.isRunning()) return;
		final int player = tyrande.getPlayerIndex();
		final var owned = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()).toList();
		final long archers = owned.stream().filter(unit -> "earc".equals(unit.getTypeId().toString())).count();
		if (!restoredHeroes) {
			final CGameCache cache = get(globals, "bj_lastCreatedGameCache");
			verifyHero(cache, "Tyrande", tyrande);
			verifyHero(cache, "Furion", furion);
			check(Math.abs(timer.getTimeoutTime() - 2700) < 0.01f, "the original 45-minute defence clock changed");
			restoredHeroes = true;
			startingArchers = archers;
			System.out.println("[NightElfFinaleProbe] restored both earned heroes and started the original Mount Hyjal defence");
		}

		final CUnit mine = owned.stream().filter(unit -> unit.getFirstAbilityOfType(CAbilityEntangledMine.class) != null)
				.findFirst().orElse(null);
		if (mine == null) return;
		if (VICTORY && economyVerified) {
			if (game.getGameTurnTick() >= nextOrder) {
				nextOrder = game.getGameTurnTick() + 22;
				defendMountHyjal(viewer, ui, timer, owned, tyrande, furion, mine);
			}
			return;
		}
		final int gold = mine.getFirstAbilityOfType(CAbilityEntangledMine.class).getGold();
		if (initialGold < 0) initialGold = gold;
		if (initialGold > gold && archers > startingArchers) {
			System.out.println("[NightElfFinaleProbe] mined gold and trained a new Archer during the original defence");
			economyVerified = true;
			if (!VICTORY) {
				finished = true;
				Gdx.app.exit();
			}
			return;
		}
		if (game.getGameTurnTick() < nextOrder) return;
		nextOrder = game.getGameTurnTick() + 16;
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final var wisps = owned.stream().filter(unit -> !unit.isHidden() && "ewsp".equals(unit.getTypeId().toString()))
				.sorted(java.util.Comparator.comparingDouble(mine::distanceSquaredNoCollision)).limit(5).toList();
		for (final CUnit wisp : wisps) CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.smart, mine);
		for (final CUnit unit : owned) {
			if ("eaom".equals(unit.getTypeId().toString()) && unit.getBuildQueue()[0] == null) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, unit, "earc");
			}
		}
		ui.getCameraManager().setTarget(mine.getX(), mine.getY());
	}

	private static void defendMountHyjal(final War3MapViewer viewer, final MeleeUI ui, final CTimer timer,
			final List<CUnit> owned, final CUnit tyrande, final CUnit furion, final CUnit mine) {
		final var game = viewer.simulation;
		final int player = tyrande.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final var wisps = owned.stream().filter(unit -> !unit.isHidden() && type(unit, "ewsp")).toList();
		final var timber = new ArrayList<>(wisps.stream()
				.sorted(Comparator.comparingDouble(mine::distanceSquaredNoCollision)).toList());
		final var cargo = mine.getCargoData();
		final int needed = cargo == null ? 0 : cargo.getCargoCapacity() - cargo.getCargoCount();
		for (int index = 0; index < Math.min(needed, timber.size()); index++) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, timber.get(index), OrderIds.smart, mine);
		}
		timber.subList(0, Math.min(needed, timber.size())).clear();

		final var army = owned.stream().filter(unit -> !unit.isHidden() && !unit.isPaused() && !unit.isBuilding()
				&& !unit.getCurrentAttacks().isEmpty()).toList();
		final long archers = army.stream().filter(unit -> type(unit, "earc")).count();
		final long dryads = army.stream().filter(unit -> type(unit, "edry")).count();
		final long druids = army.stream().filter(unit -> type(unit, "edoc")).count();
		final long huntresses = army.stream().filter(unit -> type(unit, "esen")).count();
		final boolean newWells = owner.getFoodCap() - owner.getFoodUsed() < 5 && owner.getFoodCap() < 90;
		for (final CUnit building : owned) {
			if (!building.isBuilding() || building.getBuildQueue()[0] != null || building.isConstructingOrUpgrading()) continue;
			if (type(building, "eaom")) {
				if (archers < 10) HumanCampaignPlaythroughProbe.train(viewer, orders, building, "earc");
				else if (huntresses < 8) HumanCampaignPlaythroughProbe.train(viewer, orders, building, "esen");
			}
			else if (type(building, "eaoe") && dryads < 11) HumanCampaignPlaythroughProbe.train(viewer, orders, building, "edry");
			else if (type(building, "eaow") && druids < 4) HumanCampaignPlaythroughProbe.train(viewer, orders, building, "edoc");
		}

		final long protectors = owned.stream().filter(unit -> type(unit, "etrp")).count();
		if (!timber.isEmpty()) {
			final CUnit builder = timber.remove(timber.size() - 1);
			final String building = newWells ? "emow" : "etrp";
			if ((newWells || protectors < 15) && owned.stream().noneMatch(unit -> type(unit, building) && unit.isConstructing())) {
				for (final float[] site : newWells ? WELL_SITES : DEFENCE_SITES) {
					if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, builder,
							War3ID.fromString(building).getValue(), site[0], site[1])) break;
				}
			}
			for (final CUnit wisp : timber) {
				if (wisp.getCurrentOrder() != null && wisp.getCurrentOrder().getOrderId() == OrderIds.wispharvest) continue;
				game.getDestructables().stream().filter(tree -> !tree.isDead() && wisp.canReach(tree, 1100))
						.sorted(Comparator.comparingDouble(wisp::distanceSquaredNoCollision))
						.anyMatch(tree -> CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.wispharvest, tree));
			}
		}
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)
				&& unit.getX() > 1000 && unit.getY() > 650).toList();
		for (final CUnit unit : army) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit == tyrande || unit == furion) {
				trainHeroSkill(viewer, orders, unit, unit == tyrande ? TYRANDE_SKILLS : FURION_SKILLS);
				if (unit.getLife() < unit.getMaximumLife() * 0.66f
						&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
								|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")
								|| healAtMoonWell(viewer, orders, owned, unit))) continue;
				if (unit == tyrande && enemies.stream().filter(enemy -> unit.canReach(enemy, 700)).limit(4).count() == 4
						&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.starfall)) continue;
				if (unit == furion && army.stream().filter(ally -> unit.canReach(ally, 650)
						&& ally.getLife() < ally.getMaximumLife() * 0.62f).limit(4).count() == 4
						&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.tranquility)) continue;
			}
			final CUnit threat = enemies.stream().filter(enemy -> unit.canReach(enemy, 625))
					.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			if (unit == furion && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.entanglingroots, threat)) continue;
			if (type(unit, "edoc") && threat != null && HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.roar)) continue;
			if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, threat)) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, 2950, 2720);
			}
		}
		if (game.getGameTurnTick() >= nextReport) {
			nextReport = game.getGameTurnTick() + 1000;
			final CUnit archimonde = get(game.getGlobalScope(), "udg_Archimonde");
			System.out.printf("[NightElfFinaleProbe] time=%.0f gold=%d lumber=%d troops=%d protectors=%d Tyrande=%.0f Furion=%.0f Archimonde=%s%n",
					timer.getRemaining(game), owner.getGold(), owner.getLumber(), army.size(), protectors, tyrande.getLife(), furion.getLife(),
					archimonde == null ? "unavailable" : Math.round(archimonde.getX()) + "," + Math.round(archimonde.getY()));
		}
		ui.getCameraManager().setTarget(2950, 2720);
	}

	private static void trainHeroSkill(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit hero, final String[] skills) {
		if (hero.getHeroData().getSkillPoints() <= 0) return;
		for (final String skill : skills) {
			if (HumanCampaignPlaythroughProbe.immediate(viewer, orders, hero, War3ID.fromString(skill).getValue())) return;
		}
	}

	private static boolean healAtMoonWell(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final List<CUnit> owned, final CUnit hero) {
		final var well = owned.stream().filter(unit -> type(unit, "emow") && unit.getMana() > 40 && !unit.isConstructing())
				.min(Comparator.comparingDouble(hero::distanceSquaredNoCollision)).orElse(null);
		if (well == null) return false;
		if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, well, OrderIds.replenish, hero)) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, hero, well.getX() - 130, well.getY());
		}
		return true;
	}

	private static boolean type(final CUnit unit, final String id) {
		return id.equals(unit.getTypeId().toString());
	}

	static void verifyCredits() {
		if (finished) return;
		check(victoryRecorded, "the credits loaded without the original Mount Hyjal victory");
		System.out.println("[NightElfFinaleProbe] reached the original Night Elf campaign credits");
		finished = true;
		Gdx.app.exit();
	}

	private static void verifyHero(final CGameCache cache, final String name, final CUnit hero) {
		final var stored = cache == null ? null : cache.getStoredUnit("NightElf07", name);
		final var current = CGameSave.snapshotUnit(hero);
		check(stored != null && current.xp == stored.xp + 700, name + " did not receive the authored 700 experience after restoration");
		final var expected = CampaignPlaythroughProbe.heroProperties(stored);
		final var loaded = CampaignPlaythroughProbe.heroProperties(current);
		for (final String property : expected.stringPropertyNames()) {
			if (property.equals("type") || property.equals("name") || property.startsWith("item.") || property.startsWith("ability.")) {
				check(expected.getProperty(property).equals(loaded.getProperty(property)), name + " changed " + property);
			}
		}
	}

	private static <T> T get(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Night Elf finale opening audit: " + message);
	}
}
