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

import java.util.Objects;

public final class GearItem
{
	private final int itemId;
	private final String name;
	private final GearSlot slot;
	private final int stabDefence;
	private final int slashDefence;
	private final int crushDefence;
	private final int magicDefence;
	private final int rangedDefence;
	private final long riskValue;
	private final boolean twoHanded;
	private final boolean priceKnown;

	public GearItem(
		int itemId,
		String name,
		GearSlot slot,
		int stabDefence,
		int slashDefence,
		int crushDefence,
		int magicDefence,
		int rangedDefence,
		long riskValue,
		boolean twoHanded,
		boolean priceKnown)
	{
		this.itemId = itemId;
		this.name = Objects.requireNonNull(name);
		this.slot = Objects.requireNonNull(slot);
		this.stabDefence = stabDefence;
		this.slashDefence = slashDefence;
		this.crushDefence = crushDefence;
		this.magicDefence = magicDefence;
		this.rangedDefence = rangedDefence;
		this.riskValue = Math.max(0, riskValue);
		this.twoHanded = twoHanded;
		this.priceKnown = priceKnown;
	}

	public static GearItem empty(GearSlot slot)
	{
		return new GearItem(-1, "Empty", slot, 0, 0, 0, 0, 0, 0, false, true);
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getName()
	{
		return name;
	}

	public GearSlot getSlot()
	{
		return slot;
	}

	public int getStabDefence()
	{
		return stabDefence;
	}

	public int getSlashDefence()
	{
		return slashDefence;
	}

	public int getCrushDefence()
	{
		return crushDefence;
	}

	public int getMagicDefence()
	{
		return magicDefence;
	}

	public int getRangedDefence()
	{
		return rangedDefence;
	}

	public long getRiskValue()
	{
		return riskValue;
	}

	public boolean isTwoHanded()
	{
		return twoHanded;
	}

	public boolean isPriceKnown()
	{
		return priceKnown;
	}

	public boolean isEmpty()
	{
		return itemId < 0;
	}

	public double getMeleeDefence()
	{
		return (stabDefence + slashDefence + crushDefence) / 3.0;
	}
}
