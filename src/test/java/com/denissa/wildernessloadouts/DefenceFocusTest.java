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

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DefenceFocusTest
{
	@Test
	public void scoresEachDefenceFocus()
	{
		GearItem item = new GearItem(1, "Test", GearSlot.HEAD, 3, 6, 9, 12, 15, 100, false, true);

		assertEquals(12.0, DefenceFocus.MAGIC.score(item), 0.0001);
		assertEquals(15.0, DefenceFocus.RANGED.score(item), 0.0001);
		assertEquals(6.0, DefenceFocus.MELEE.score(item), 0.0001);
		assertEquals(11.0, DefenceFocus.OVERALL.score(item), 0.0001);
	}

	@Test
	public void supportsNegativeBonuses()
	{
		GearItem item = new GearItem(1, "Negative", GearSlot.HEAD, -3, -6, -9, -12, -15, 100, false, true);

		assertEquals(-6.0, DefenceFocus.MELEE.score(item), 0.0001);
		assertEquals(-11.0, DefenceFocus.OVERALL.score(item), 0.0001);
	}

	@Test
	public void emptySlotScoresZero()
	{
		GearItem empty = GearItem.empty(GearSlot.HEAD);

		for (DefenceFocus focus : DefenceFocus.values())
		{
			assertEquals(0.0, focus.score(empty), 0.0001);
		}
	}
}
