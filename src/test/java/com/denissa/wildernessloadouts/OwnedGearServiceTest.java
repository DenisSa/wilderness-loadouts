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
	public void excludesLegacyLowTierTrouverVariants()
	{
		assertEquals(0L, OwnedGearService.resolveRiskValue(ItemID.MA2_ZAMORAK_CAPE_TROUVER, 0));
		assertEquals(0L, OwnedGearService.resolveRiskValue(ItemID.MA2_ZAMORAK_CAPE_TROUVER, 999_999));
		assertEquals(0L, OwnedGearService.resolveRiskValue(ItemID.MA2_ZAMORAK_CAPE, 0));
		assertEquals(12_345L, OwnedGearService.resolveRiskValue(ItemID.MA2_ZAMORAK_CAPE, 12_345));
		assertEquals(0L, OwnedGearService.resolveRiskValue(ItemID.TZHAAR_CAPE_FIRE_TROUVER, 999_999));
	}

	@Test
	public void usesConservativeDeepWildernessFeeForHighTierTrouverItems()
	{
		assertEquals(500_000L, OwnedGearService.resolveRiskValue(ItemID.GAME_PEST_MELEE_HELM_TROUVER, 0));
		assertEquals(0L, OwnedGearService.resolveRiskValue(ItemID.GAME_PEST_MELEE_HELM, 0));
	}
}
