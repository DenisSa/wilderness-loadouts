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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class LoadoutRequest
{
	private final DefenceFocus focus;
	private final int protectedLimit;
	private final long maxFillerRisk;
	private final Map<GearSlot, LoadoutSlotSelection> slotSelections;

	public LoadoutRequest(
		DefenceFocus focus,
		int protectedLimit,
		long maxFillerRisk,
		Map<GearSlot, LoadoutSlotSelection> slotSelections)
	{
		this.focus = Objects.requireNonNull(focus);
		if (protectedLimit != 0 && protectedLimit != 1 && protectedLimit != 3 && protectedLimit != 4)
		{
			throw new IllegalArgumentException("Protected/core items must be 0, 1, 3, or 4");
		}
		if (maxFillerRisk < 0)
		{
			throw new IllegalArgumentException("Max filler risk cannot be negative");
		}
		this.protectedLimit = protectedLimit;
		this.maxFillerRisk = maxFillerRisk;

		EnumMap<GearSlot, LoadoutSlotSelection> selections = new EnumMap<>(GearSlot.class);
		for (GearSlot slot : GearSlot.values())
		{
			LoadoutSlotSelection selection = slotSelections == null ? null : slotSelections.get(slot);
			selections.put(slot, selection == null ? LoadoutSlotSelection.auto() : selection);
		}
		this.slotSelections = Collections.unmodifiableMap(selections);
	}

	public DefenceFocus getFocus()
	{
		return focus;
	}

	public int getProtectedLimit()
	{
		return protectedLimit;
	}

	public long getMaxFillerRisk()
	{
		return maxFillerRisk;
	}

	public Map<GearSlot, LoadoutSlotSelection> getSlotSelections()
	{
		return slotSelections;
	}

	public LoadoutSlotSelection getSlotSelection(GearSlot slot)
	{
		return slotSelections.get(slot);
	}
}
