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

import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OwnedGearServiceTest
{
	@Test
	public void acceptsOnlyPhysicalContainerItems()
	{
		assertTrue(OwnedGearService.isPhysicalItem(new Item(1, 1), false, false));
		assertFalse(OwnedGearService.isPhysicalItem(new Item(1, 1), true, false));
		assertFalse(OwnedGearService.isPhysicalItem(new Item(1, 1), false, true));
		assertFalse(OwnedGearService.isPhysicalItem(new Item(1, 0), false, false));
		assertFalse(OwnedGearService.isPhysicalItem(new Item(-1, 1), false, false));
	}

	@Test
	public void excludesLegacyLowTierTrouverVariantsRegardlessOfMarketPrice()
	{
		LossProfile profile = LossProfileResolver.resolve(
			ItemID.MA2_ZAMORAK_CAPE_TROUVER,
			ignored -> 999_999);

		assertFalse(profile.isAutoEligible());
		assertEquals(LossProfile.EligibilityPolicy.LEGACY_TROUVER, profile.getEligibilityPolicy());
	}

	@Test
	public void usesConservativeDeepWildernessFeeForHighTierTrouverItems()
	{
		LossProfile locked = LossProfileResolver.resolve(
			ItemID.GAME_PEST_MELEE_HELM_TROUVER,
			ignored -> 0);
		LossProfile unlocked = LossProfileResolver.resolve(
			ItemID.GAME_PEST_MELEE_HELM,
			ignored -> 0);

		assertTrue(locked.isAutoEligible());
		assertEquals(500_000L, locked.getCostIfUnprotected());
		assertEquals(0L, locked.getCostIfProtected());
		assertEquals(LossProfile.ReacquisitionMethod.TROUVER_REPAIR, locked.getReacquisitionMethod());
		assertFalse(unlocked.isAutoEligible());
		assertEquals(LossProfile.EligibilityPolicy.UNLOCKED_TROUVER, unlocked.getEligibilityPolicy());
	}

	@Test
	public void doesNotAdviseAParchmentForLegacyLowTierItems()
	{
		LossProfile unlocked = LossProfileResolver.resolve(ItemID.TZHAAR_CAPE_FIRE, ignored -> 0);
		LossProfile locked = LossProfileResolver.resolve(ItemID.TZHAAR_CAPE_FIRE_TROUVER, ignored -> 0);

		assertEquals(
			LossProfile.EligibilityPolicy.LEGACY_TROUVER_UNLOCKED,
			unlocked.getEligibilityPolicy());
		assertEquals(
			"a Trouver parchment will not make this usable",
			unlocked.getEligibilityPolicy().getExclusionReason());
		assertEquals(LossProfile.EligibilityPolicy.LEGACY_TROUVER, locked.getEligibilityPolicy());
	}

	@Test
	public void stillAdvisesAParchmentForCurrentHighTierItems()
	{
		LossProfile unlocked = LossProfileResolver.resolve(ItemID.INFERNAL_CAPE, ignored -> 0);
		LossProfile locked = LossProfileResolver.resolve(ItemID.INFERNAL_CAPE_TROUVER, ignored -> 0);

		assertEquals(LossProfile.EligibilityPolicy.UNLOCKED_TROUVER, unlocked.getEligibilityPolicy());
		assertTrue(locked.isAutoEligible());
		assertEquals(500_000L, locked.getCostIfUnprotected());
	}

	@Test
	public void suppliesExactQuestShopAndNpcReplacementValues()
	{
		LossProfile barrowsGloves = LossProfileResolver.resolve(
			ItemID.HUNDRED_GAUNTLETS_LEVEL_10,
			ignored -> 0);
		LossProfile mythicalCape = LossProfileResolver.resolve(ItemID.MYTHICAL_CAPE, ignored -> 0);
		LossProfile ringOfShadows = LossProfileResolver.resolve(ItemID.RING_OF_SHADOWS, ignored -> 0);

		assertEquals(130_000L, barrowsGloves.getCostIfUnprotected());
		assertEquals(10_000L, mythicalCape.getCostIfUnprotected());
		assertEquals(75_000L, ringOfShadows.getCostIfUnprotected());
		assertTrue(ringOfShadows.hasNonMonetaryBurden());
	}

	@Test
	public void modelsLunarEquipmentAsAlwaysLostAtPerduPrice()
	{
		LossProfile lunarRing = LossProfileResolver.resolve(ItemID.LUNAR_RING, ignored -> 0);

		assertEquals(8_000L, lunarRing.getCostIfUnprotected());
		assertEquals(8_000L, lunarRing.getCostIfProtected());
		assertFalse(lunarRing.canBeProtected());
		assertEquals(LossProfile.ReacquisitionMethod.PERDU, lunarRing.getReacquisitionMethod());
	}

	@Test
	public void modelsImbuedRingOfWealthResidualLossWhenProtected()
	{
		LossProfile profile = LossProfileResolver.resolve(ItemID.RING_OF_WEALTH_I, itemId ->
		{
			if (itemId == ItemID.RING_OF_WEALTH)
			{
				return 12_000;
			}
			if (itemId == ItemID.BH_IMBUE_RINGOFWEALTH)
			{
				return 4_500;
			}
			return 0;
		});

		assertEquals(66_500L, profile.getCostIfUnprotected());
		assertEquals(50_000L, profile.getCostIfProtected());
		assertTrue(profile.canBeProtected());
	}

	@Test
	public void excludesImbuedRingOfWealthWhenAComponentPriceIsUnavailable()
	{
		LossProfile profile = LossProfileResolver.resolve(ItemID.RING_OF_WEALTH_I, ignored -> 0);

		assertFalse(profile.isAutoEligible());
		assertEquals(LossProfile.EligibilityPolicy.REPLACEMENT_UNKNOWN, profile.getEligibilityPolicy());
	}

	@Test
	public void recordsChargedWardBurdenAlongsideMarketReplacement()
	{
		LossProfile profile = LossProfileResolver.resolve(ItemID.DRAGONFIRE_WARD, ignored -> 2_000_000);

		assertTrue(profile.isAutoEligible());
		assertEquals(2_000_000L, profile.getCostIfUnprotected());
		assertEquals(LossProfile.NonMonetaryBurden.CHARGES, profile.getNonMonetaryBurden());
		assertEquals(LossProfile.Confidence.MARKET_ESTIMATE, profile.getConfidence());
	}

	@Test
	public void leavesTimeOnlyAndUnknownItemsVisibleButIneligible()
	{
		LossProfile salve = LossProfileResolver.resolve(
			ItemID.LOTR_CRYSTALSHARD_NECKLACE_UPGRADE,
			ignored -> 0);
		LossProfile unknown = LossProfileResolver.resolve(123_456, ignored -> 0);

		assertFalse(salve.isAutoEligible());
		assertEquals(LossProfile.EligibilityPolicy.TIME_ONLY, salve.getEligibilityPolicy());
		assertFalse(unknown.isAutoEligible());
		assertEquals(LossProfile.EligibilityPolicy.REPLACEMENT_UNKNOWN, unknown.getEligibilityPolicy());
	}

	@Test
	public void fallsBackToRuneLiteMarketPrice()
	{
		LossProfile profile = LossProfileResolver.resolve(123_456, ignored -> 42_000);

		assertNotNull(profile);
		assertTrue(profile.isAutoEligible());
		assertEquals(42_000L, profile.getCostIfUnprotected());
		assertEquals(LossProfile.Confidence.MARKET_ESTIMATE, profile.getConfidence());
	}
}
