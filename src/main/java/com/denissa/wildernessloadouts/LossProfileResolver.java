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

import java.util.function.IntToLongFunction;
import net.runelite.api.gameval.ItemID;

final class LossProfileResolver
{
	private static final String ITEMS_KEPT_ON_DEATH_SOURCE =
		"OSRS Wiki: Items Kept on Death";

	private LossProfileResolver()
	{
	}

	static LossProfile resolve(int itemId, IntToLongFunction marketPriceLookup)
	{
		if (TrouverRiskValues.isLegacyLowTier(itemId))
		{
			return LossProfile.excluded(
				LossProfile.EligibilityPolicy.LEGACY_TROUVER,
				LossProfile.NonMonetaryBurden.NONE,
				"Jagex Trouver system rework");
		}

		long trouverRepairCost = TrouverRiskValues.getRepairCost(itemId);
		if (trouverRepairCost > 0)
		{
			return LossProfile.exact(
				trouverRepairCost,
				0,
				true,
				LossProfile.ReacquisitionMethod.TROUVER_REPAIR,
				LossProfile.NonMonetaryBurden.NONE,
				"Jagex Trouver system rework");
		}

		if (TrouverRiskValues.isUnlockedTrouverCapable(itemId))
		{
			return LossProfile.excluded(
				LossProfile.EligibilityPolicy.UNLOCKED_TROUVER,
				LossProfile.NonMonetaryBurden.REACQUISITION_TIME,
				"Jagex Trouver system rework");
		}

		LossProfile exactReplacement = ExactReplacementValues.get(itemId);
		if (exactReplacement != null)
		{
			return exactReplacement;
		}

		if (isRingOfWealthImbued(itemId))
		{
			long ringPrice = positivePrice(marketPriceLookup, ItemID.RING_OF_WEALTH);
			long scrollPrice = positivePrice(marketPriceLookup, ItemID.BH_IMBUE_RINGOFWEALTH);
			if (ringPrice <= 0 || scrollPrice <= 0)
			{
				return LossProfile.excluded(
					LossProfile.EligibilityPolicy.REPLACEMENT_UNKNOWN,
					LossProfile.NonMonetaryBurden.NONE,
					ITEMS_KEPT_ON_DEATH_SOURCE);
			}
			long replacementCost = 50_000L + ringPrice + scrollPrice;
			return LossProfile.estimated(
				replacementCost,
				50_000L,
				true,
				LossProfile.ReacquisitionMethod.SPECIAL_RULE,
				LossProfile.NonMonetaryBurden.NONE,
				ITEMS_KEPT_ON_DEATH_SOURCE);
		}

		if (isSalveAmulet(itemId))
		{
			return LossProfile.excluded(
				LossProfile.EligibilityPolicy.TIME_ONLY,
				LossProfile.NonMonetaryBurden.REACQUISITION_TIME,
				"OSRS Wiki: Salve amulet");
		}

		long marketPrice = positivePrice(marketPriceLookup, itemId);
		if (itemId == ItemID.DRAGONFIRE_WARD && marketPrice > 0)
		{
			return LossProfile.estimated(
				marketPrice,
				0,
				true,
				LossProfile.ReacquisitionMethod.MARKET,
				LossProfile.NonMonetaryBurden.CHARGES,
				ITEMS_KEPT_ON_DEATH_SOURCE);
		}

		return marketPrice > 0 ? LossProfile.market(marketPrice) : LossProfile.unknown();
	}

	private static long positivePrice(IntToLongFunction marketPriceLookup, int itemId)
	{
		return Math.max(0, marketPriceLookup.applyAsLong(itemId));
	}

	private static boolean isRingOfWealthImbued(int itemId)
	{
		return itemId == ItemID.RING_OF_WEALTH_I
			|| itemId == ItemID.RING_OF_WEALTH_I1
			|| itemId == ItemID.RING_OF_WEALTH_I2
			|| itemId == ItemID.RING_OF_WEALTH_I3
			|| itemId == ItemID.RING_OF_WEALTH_I4
			|| itemId == ItemID.RING_OF_WEALTH_I5;
	}

	private static boolean isSalveAmulet(int itemId)
	{
		return itemId == ItemID.CRYSTALSHARD_NECKLACE
			|| itemId == ItemID.LOTR_CRYSTALSHARD_NECKLACE_UPGRADE
			|| itemId == ItemID.NZONE_SALVE_AMULET
			|| itemId == ItemID.NZONE_SALVE_AMULET_E
			|| itemId == ItemID.SW_SALVE_AMULET
			|| itemId == ItemID.SW_SALVE_AMULET_E
			|| itemId == ItemID.PVPA_SALVE_AMULET
			|| itemId == ItemID.PVPA_SALVE_AMULET_E;
	}
}
