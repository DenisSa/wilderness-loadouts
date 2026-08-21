/*
 * Copyright (c) 2026, DenisSa
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.denissa.wildernessloadouts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoadoutOptimizerTest
{
	private final LoadoutOptimizer optimizer = new LoadoutOptimizer();

	@Test
	public void prunesDominatedAndDuplicateCandidates()
	{
		List<GearItem> candidates = Arrays.asList(
			item(1, GearSlot.HEAD, 10, 100),
			item(2, GearSlot.HEAD, 9, 200),
			item(3, GearSlot.HEAD, 20, 500),
			item(4, GearSlot.HEAD, 10, 100));

		List<Integer> ids = optimizer.preprocessCandidates(DefenceFocus.MAGIC, candidates)
			.stream()
			.map(GearItem::getItemId)
			.collect(Collectors.toList());

		assertEquals(Arrays.asList(3, 1), ids);
	}

	@Test
	public void keepsCompatibilityRelevantWeaponCandidate()
	{
		GearItem twoHanded = item(1, GearSlot.WEAPON, 10, 100, true, true);
		GearItem oneHanded = item(2, GearSlot.WEAPON, 9, 50, false, true);

		List<GearItem> frontier = optimizer.preprocessCandidates(
			DefenceFocus.MAGIC,
			Arrays.asList(twoHanded, oneHanded));

		assertEquals(2, frontier.size());
	}

	@Test
	public void keepsCandidateThatIsCheaperOnlyWhenProtected()
	{
		GearItem cheaperUnprotected = item(
			1,
			GearSlot.HEAD,
			10,
			LossProfile.exact(
				100,
				100,
				true,
				LossProfile.ReacquisitionMethod.SPECIAL_RULE,
				LossProfile.NonMonetaryBurden.NONE,
				"Test rule"));
		GearItem cheaperProtected = item(
			2,
			GearSlot.HEAD,
			10,
			LossProfile.exact(
				110,
				0,
				true,
				LossProfile.ReacquisitionMethod.SPECIAL_RULE,
				LossProfile.NonMonetaryBurden.NONE,
				"Test rule"));

		List<GearItem> frontier = optimizer.preprocessCandidates(
			DefenceFocus.MAGIC,
			Arrays.asList(cheaperUnprotected, cheaperProtected));
		LoadoutResult result = optimize(frontier, 3, 0);

		assertEquals(2, frontier.size());
		assertEquals(2, result.getSelectedItem(GearSlot.HEAD).getItemId());
		assertTrue(result.isProtected(GearSlot.HEAD));
	}

	@Test
	public void choosesHigherScoreWhenBudgetAllowsAndCheaperWhenItDoesNot()
	{
		List<GearItem> gear = withCore(
			item(10, GearSlot.HEAD, 20, 100),
			item(11, GearSlot.HEAD, 10, 0));

		assertEquals(10, optimize(gear, 3, 100).getSelectedItem(GearSlot.HEAD).getItemId());
		assertEquals(11, optimize(gear, 3, 50).getSelectedItem(GearSlot.HEAD).getItemId());
	}

	@Test
	public void appliesBudgetAcrossAllFillerItems()
	{
		List<GearItem> gear = withCore(
			item(10, GearSlot.HEAD, 10, 60),
			item(11, GearSlot.BODY, 10, 60));

		LoadoutResult result = optimize(gear, 3, 100);

		assertEquals(10.0, result.getObjectiveScore() - 3_000.0, 0.0001);
		assertTrue(result.getFillerRisk() <= 100);
	}

	@Test
	public void protectedLimitChangesResult()
	{
		List<GearItem> gear = Arrays.asList(
			item(1, GearSlot.HEAD, 40, 10),
			item(2, GearSlot.CAPE, 30, 10),
			item(3, GearSlot.NECK, 20, 10),
			item(4, GearSlot.AMMO, 10, 10));

		LoadoutResult three = optimize(gear, 3, 0);
		LoadoutResult four = optimize(gear, 4, 0);
		LoadoutResult one = optimize(gear, 1, 0);

		assertEquals(40.0, one.getObjectiveScore(), 0.0001);
		assertEquals(1, one.getProtectedSlots().size());
		assertEquals(90.0, three.getObjectiveScore(), 0.0001);
		assertTrue(three.getSelectedItem(GearSlot.AMMO).isEmpty());
		assertEquals(100.0, four.getObjectiveScore(), 0.0001);
		assertEquals(4, four.getProtectedSlots().size());
	}

	@Test
	public void protectedResidualLossIsReportedOutsideFillerBudget()
	{
		GearItem item = item(
			1,
			GearSlot.HEAD,
			10,
			LossProfile.exact(
				100,
				50,
				true,
				LossProfile.ReacquisitionMethod.SPECIAL_RULE,
				LossProfile.NonMonetaryBurden.NONE,
				"Test rule"));

		GearItem alwaysLost = item(
			2,
			GearSlot.BODY,
			5,
			LossProfile.exact(
				20,
				20,
				false,
				LossProfile.ReacquisitionMethod.PERDU,
				LossProfile.NonMonetaryBurden.NONE,
				"Test rule"));
		LoadoutResult result = optimize(Arrays.asList(item, alwaysLost), 1, 20);

		assertEquals(1, result.getSelectedItem(GearSlot.HEAD).getItemId());
		assertEquals(2, result.getSelectedItem(GearSlot.BODY).getItemId());
		assertTrue(result.isProtected(GearSlot.HEAD));
		assertEquals(20L, result.getFillerRisk());
		assertEquals(50L, result.getOtherRisk());
		assertEquals(70L, result.getTotalRisk());
	}

	@Test
	public void alwaysLostItemsCannotConsumeAProtectedSlot()
	{
		GearItem lunarLike = item(
			1,
			GearSlot.RING,
			10,
			LossProfile.exact(
				8_000,
				8_000,
				false,
				LossProfile.ReacquisitionMethod.PERDU,
				LossProfile.NonMonetaryBurden.NONE,
				"Test Perdu value"));

		LoadoutResult result = optimize(Collections.singletonList(lunarLike), 3, 8_000);

		assertEquals(1, result.getSelectedItem(GearSlot.RING).getItemId());
		assertFalse(result.isProtected(GearSlot.RING));
		assertEquals(8_000L, result.getFillerRisk());
	}

	@Test
	public void highRiskModeProtectsNothingAndPricesEveryItem()
	{
		List<GearItem> gear = Arrays.asList(
			item(1, GearSlot.HEAD, 20, 60),
			item(2, GearSlot.CAPE, 10, 60));

		LoadoutResult result = optimize(gear, 0, 100);

		assertEquals(20.0, result.getObjectiveScore(), 0.0001);
		assertEquals(60, result.getFillerRisk());
		assertTrue(result.getProtectedSlots().isEmpty());
		assertEquals(1, result.getSelectedItem(GearSlot.HEAD).getItemId());
		assertTrue(result.getSelectedItem(GearSlot.CAPE).isEmpty());
	}

	@Test
	public void findsModeledUntradeableLoadoutOptimum()
	{
		GearItem mythicalCape = item(1, GearSlot.CAPE, 8, exact(10_000));
		GearItem runeGloves = item(2, GearSlot.GLOVES, 4, exact(6_500));
		GearItem barrowsGloves = item(3, GearSlot.GLOVES, 6, exact(130_000));
		GearItem ringOfShadows = item(4, GearSlot.RING, 5, exact(75_000));
		GearItem lunarRing = item(
			5,
			GearSlot.RING,
			2,
			LossProfile.exact(
				8_000,
				8_000,
				false,
				LossProfile.ReacquisitionMethod.PERDU,
				LossProfile.NonMonetaryBurden.NONE,
				"Test Perdu value"));

		LoadoutResult result = optimize(
			Arrays.asList(mythicalCape, runeGloves, barrowsGloves, ringOfShadows, lunarRing),
			0,
			200_000);

		assertEquals(1, result.getSelectedItem(GearSlot.CAPE).getItemId());
		assertEquals(2, result.getSelectedItem(GearSlot.GLOVES).getItemId());
		assertEquals(4, result.getSelectedItem(GearSlot.RING).getItemId());
		assertEquals(17.0, result.getObjectiveScore(), 0.0001);
		assertEquals(91_500L, result.getFillerRisk());
	}

	@Test
	public void findsGlobalMultiSlotOptimum()
	{
		List<GearItem> gear = withCore(
			item(10, GearSlot.HEAD, 10, 100),
			item(11, GearSlot.HEAD, 9, 0),
			item(12, GearSlot.BODY, 10, 100));

		LoadoutResult result = optimize(gear, 3, 100);

		assertEquals(11, result.getSelectedItem(GearSlot.HEAD).getItemId());
		assertEquals(12, result.getSelectedItem(GearSlot.BODY).getItemId());
		assertEquals(3_019.0, result.getObjectiveScore(), 0.0001);
	}

	@Test
	public void resolvesTiesDeterministically()
	{
		List<GearItem> gear = withCore(
			item(20, GearSlot.HEAD, 10, 0),
			item(10, GearSlot.HEAD, 10, 0));

		for (int i = 0; i < 5; i++)
		{
			Collections.shuffle(gear);
			assertEquals(10, optimize(gear, 3, 0).getSelectedItem(GearSlot.HEAD).getItemId());
		}
	}

	@Test
	public void emptyAndLockedSlotStatesAreRespected()
	{
		GearItem best = item(1, GearSlot.HEAD, 100, 0);
		GearItem locked = item(2, GearSlot.HEAD, 5, 0);
		Map<GearSlot, LoadoutSlotSelection> emptySelection = selections();
		emptySelection.put(GearSlot.HEAD, LoadoutSlotSelection.empty());

		LoadoutResult empty = optimizer.optimize(
			request(3, 0, emptySelection),
			Arrays.asList(best, locked));
		assertTrue(empty.getSelectedItem(GearSlot.HEAD).isEmpty());

		Map<GearSlot, LoadoutSlotSelection> lockedSelection = selections();
		lockedSelection.put(GearSlot.HEAD, LoadoutSlotSelection.locked(2));
		LoadoutResult result = optimizer.optimize(
			request(3, 0, lockedSelection),
			Arrays.asList(best, locked, item(3, GearSlot.BODY, 10, 0)));
		assertEquals(2, result.getSelectedItem(GearSlot.HEAD).getItemId());
		assertEquals(3, result.getSelectedItem(GearSlot.BODY).getItemId());
	}

	@Test
	public void neverCombinesTwoHandedWeaponAndShield()
	{
		GearItem twoHanded = item(1, GearSlot.WEAPON, 100, 0, true, true);
		GearItem oneHanded = item(2, GearSlot.WEAPON, 60, 0, false, true);
		GearItem shield = item(3, GearSlot.SHIELD, 60, 0);

		LoadoutResult result = optimize(Arrays.asList(twoHanded, oneHanded, shield), 3, 0);

		GearItem weapon = result.getSelectedItem(GearSlot.WEAPON);
		assertFalse(weapon.isTwoHanded() && !result.getSelectedItem(GearSlot.SHIELD).isEmpty());
		assertEquals(2, weapon.getItemId());
		assertEquals(3, result.getSelectedItem(GearSlot.SHIELD).getItemId());
	}

	@Test
	public void keepsAnEmptyShieldOnTheCandidateFrontier()
	{
		List<GearItem> frontier = optimizer.preprocessCandidates(
			DefenceFocus.MAGIC,
			Arrays.asList(item(1, GearSlot.SHIELD, 5, 0), GearItem.empty(GearSlot.SHIELD)));

		assertEquals(2, frontier.size());
	}

	@Test
	public void prefersATwoHandedWeaponOverACheaperShieldedPair()
	{
		GearItem twoHanded = item(1, GearSlot.WEAPON, 100, 0, true, true);
		GearItem oneHanded = item(2, GearSlot.WEAPON, 10, 0, false, true);
		GearItem shield = item(3, GearSlot.SHIELD, 5, 0);

		LoadoutResult result = optimize(Arrays.asList(twoHanded, oneHanded, shield), 0, 0);

		assertEquals(1, result.getSelectedItem(GearSlot.WEAPON).getItemId());
		assertTrue(result.getSelectedItem(GearSlot.SHIELD).isEmpty());
		assertEquals(100.0, result.getObjectiveScore(), 0.0001);
	}

	@Test
	public void lockedTwoHandedWeaponForcesEmptyShield()
	{
		GearItem weapon = item(1, GearSlot.WEAPON, 100, 0, true, true);
		GearItem shield = item(2, GearSlot.SHIELD, 100, 0);
		Map<GearSlot, LoadoutSlotSelection> selections = selections();
		selections.put(GearSlot.WEAPON, LoadoutSlotSelection.locked(1));

		LoadoutResult result = optimizer.optimize(request(3, 0, selections), Arrays.asList(weapon, shield));

		assertEquals(1, result.getSelectedItem(GearSlot.WEAPON).getItemId());
		assertTrue(result.getSelectedItem(GearSlot.SHIELD).isEmpty());
	}

	@Test
	public void lockedShieldRemovesTwoHandedCandidates()
	{
		GearItem twoHanded = item(1, GearSlot.WEAPON, 100, 0, true, true);
		GearItem oneHanded = item(2, GearSlot.WEAPON, 10, 0, false, true);
		GearItem shield = item(3, GearSlot.SHIELD, 1, 0);
		Map<GearSlot, LoadoutSlotSelection> selections = selections();
		selections.put(GearSlot.SHIELD, LoadoutSlotSelection.locked(3));

		LoadoutResult result = optimizer.optimize(
			request(3, 0, selections),
			Arrays.asList(twoHanded, oneHanded, shield));

		assertEquals(2, result.getSelectedItem(GearSlot.WEAPON).getItemId());
		assertEquals(3, result.getSelectedItem(GearSlot.SHIELD).getItemId());
	}

	@Test
	public void excludesUnknownReplacementValuesFromAuto()
	{
		GearItem unpriced = item(1, GearSlot.HEAD, 10, 0, false, false);

		LoadoutResult result = optimize(Collections.singletonList(unpriced), 3, 0);

		assertTrue(result.getSelectedItem(GearSlot.HEAD).isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void refusesToLockItemExcludedByLossPolicy()
	{
		GearItem unpriced = item(1, GearSlot.HEAD, 10, 0, false, false);
		Map<GearSlot, LoadoutSlotSelection> selections = selections();
		selections.put(GearSlot.HEAD, LoadoutSlotSelection.locked(1));

		optimizer.optimize(request(3, 0, selections), Collections.singletonList(unpriced));
	}

	private LoadoutResult optimize(List<GearItem> gear, int protectedCount, long budget)
	{
		return optimizer.optimize(request(protectedCount, budget, selections()), gear);
	}

	private static LoadoutRequest request(
		int protectedCount,
		long budget,
		Map<GearSlot, LoadoutSlotSelection> selections)
	{
		return new LoadoutRequest(DefenceFocus.MAGIC, protectedCount, budget, selections);
	}

	private static Map<GearSlot, LoadoutSlotSelection> selections()
	{
		EnumMap<GearSlot, LoadoutSlotSelection> selections = new EnumMap<>(GearSlot.class);
		for (GearSlot slot : GearSlot.values())
		{
			selections.put(slot, LoadoutSlotSelection.auto());
		}
		return selections;
	}

	private static List<GearItem> withCore(GearItem... extra)
	{
		List<GearItem> gear = new ArrayList<>();
		gear.add(item(101, GearSlot.CAPE, 1_000, 1_000_000));
		gear.add(item(102, GearSlot.NECK, 1_000, 1_000_000));
		gear.add(item(103, GearSlot.AMMO, 1_000, 1_000_000));
		gear.addAll(Arrays.asList(extra));
		return gear;
	}

	private static GearItem item(int id, GearSlot slot, int magicDefence, long risk)
	{
		return item(id, slot, magicDefence, risk, false, true);
	}

	private static GearItem item(
		int id,
		GearSlot slot,
		int magicDefence,
		long risk,
		boolean twoHanded,
		boolean priceKnown)
	{
		return new GearItem(
			id,
			"Item " + id,
			slot,
			magicDefence,
			magicDefence,
			magicDefence,
			magicDefence,
			magicDefence,
			risk,
			twoHanded,
			priceKnown);
	}

	private static GearItem item(
		int id,
		GearSlot slot,
		int magicDefence,
		LossProfile lossProfile)
	{
		return new GearItem(
			id,
			"Item " + id,
			slot,
			magicDefence,
			magicDefence,
			magicDefence,
			magicDefence,
			magicDefence,
			lossProfile,
			false);
	}

	private static LossProfile exact(long cost)
	{
		return LossProfile.exact(
			cost,
			0,
			true,
			LossProfile.ReacquisitionMethod.QUEST_SHOP,
			LossProfile.NonMonetaryBurden.NONE,
			"Test value");
	}
}
