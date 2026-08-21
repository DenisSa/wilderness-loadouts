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
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WildernessLoadoutsPanelTest
{
	@Test
	public void ranksEligibleAlternativesByFocusScore()
	{
		List<GearItem> owned = new ArrayList<>();
		owned.add(ring(1, 5));
		owned.add(ring(2, 20));
		owned.add(excludedRing(3, 40));

		List<GearItem> alternatives = WildernessLoadoutsPanel.buildAlternatives(
			owned, GearSlot.RING, DefenceFocus.MAGIC, -1);

		assertEquals(3, alternatives.size());
		assertEquals(2, alternatives.get(0).getItemId());
		assertEquals(1, alternatives.get(1).getItemId());
		assertEquals(3, alternatives.get(2).getItemId());
	}

	@Test
	public void keepsTheLockedItemVisibleBeyondTheDisplayCap()
	{
		List<GearItem> owned = new ArrayList<>();
		for (int i = 1; i <= 15; i++)
		{
			owned.add(ring(i, 100 - i));
		}

		List<GearItem> alternatives = WildernessLoadoutsPanel.buildAlternatives(
			owned, GearSlot.RING, DefenceFocus.MAGIC, 15);

		assertEquals(13, alternatives.size());
		assertEquals(15, alternatives.get(alternatives.size() - 1).getItemId());
		assertTrue(containsItem(alternatives, 15));
	}

	@Test
	public void doesNotDuplicateALockedItemAlreadyWithinTheCap()
	{
		List<GearItem> owned = new ArrayList<>();
		for (int i = 1; i <= 15; i++)
		{
			owned.add(ring(i, 100 - i));
		}

		List<GearItem> alternatives = WildernessLoadoutsPanel.buildAlternatives(
			owned, GearSlot.RING, DefenceFocus.MAGIC, 1);

		assertEquals(12, alternatives.size());
		assertEquals(1, alternatives.get(0).getItemId());
	}

	@Test
	public void ignoresOtherSlots()
	{
		List<GearItem> owned = new ArrayList<>();
		owned.add(ring(1, 5));
		owned.add(new GearItem(2, "Helm", GearSlot.HEAD, 0, 0, 0, 9, 0, 100L, false, true));

		List<GearItem> alternatives = WildernessLoadoutsPanel.buildAlternatives(
			owned, GearSlot.RING, DefenceFocus.MAGIC, -1);

		assertEquals(1, alternatives.size());
		assertEquals(1, alternatives.get(0).getItemId());
	}

	private static boolean containsItem(List<GearItem> items, int itemId)
	{
		for (GearItem item : items)
		{
			if (item.getItemId() == itemId)
			{
				return true;
			}
		}
		return false;
	}

	private static GearItem ring(int id, int magicDefence)
	{
		return new GearItem(id, "Ring " + id, GearSlot.RING, 0, 0, 0, magicDefence, 0, 1_000L, false, true);
	}

	private static GearItem excludedRing(int id, int magicDefence)
	{
		return new GearItem(
			id,
			"Ring " + id,
			GearSlot.RING,
			0,
			0,
			0,
			magicDefence,
			0,
			LossProfile.unknown(),
			false);
	}
}
