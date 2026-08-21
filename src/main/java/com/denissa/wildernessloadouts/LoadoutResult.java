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
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class LoadoutResult
{
	private final DefenceFocus focus;
	private final double objectiveScore;
	private final long fillerRisk;
	private final long maxFillerRisk;
	private final Map<GearSlot, GearItem> selectedItems;
	private final Set<GearSlot> protectedSlots;
	private final int totalStabDefence;
	private final int totalSlashDefence;
	private final int totalCrushDefence;
	private final int totalMagicDefence;
	private final int totalRangedDefence;
	private final boolean unpricedItemsSelected;

	LoadoutResult(
		LoadoutRequest request,
		double objectiveScore,
		long fillerRisk,
		Map<GearSlot, GearItem> selectedItems,
		Set<GearSlot> protectedSlots)
	{
		this.focus = request.getFocus();
		this.objectiveScore = objectiveScore;
		this.fillerRisk = fillerRisk;
		this.maxFillerRisk = request.getMaxFillerRisk();
		this.selectedItems = Collections.unmodifiableMap(new EnumMap<>(selectedItems));
		this.protectedSlots = Collections.unmodifiableSet(
			protectedSlots.isEmpty() ? EnumSet.noneOf(GearSlot.class) : EnumSet.copyOf(protectedSlots));

		int stab = 0;
		int slash = 0;
		int crush = 0;
		int magic = 0;
		int ranged = 0;
		boolean unpriced = false;
		for (GearItem item : selectedItems.values())
		{
			if (item.isEmpty())
			{
				continue;
			}
			stab += item.getStabDefence();
			slash += item.getSlashDefence();
			crush += item.getCrushDefence();
			magic += item.getMagicDefence();
			ranged += item.getRangedDefence();
			unpriced |= !item.isPriceKnown();
		}
		this.totalStabDefence = stab;
		this.totalSlashDefence = slash;
		this.totalCrushDefence = crush;
		this.totalMagicDefence = magic;
		this.totalRangedDefence = ranged;
		this.unpricedItemsSelected = unpriced;
	}

	public DefenceFocus getFocus()
	{
		return focus;
	}

	public double getObjectiveScore()
	{
		return objectiveScore;
	}

	public long getFillerRisk()
	{
		return fillerRisk;
	}

	public long getMaxFillerRisk()
	{
		return maxFillerRisk;
	}

	public long getRemainingRisk()
	{
		return maxFillerRisk - fillerRisk;
	}

	public Map<GearSlot, GearItem> getSelectedItems()
	{
		return selectedItems;
	}

	public GearItem getSelectedItem(GearSlot slot)
	{
		return selectedItems.get(slot);
	}

	public Set<GearSlot> getProtectedSlots()
	{
		return protectedSlots;
	}

	public boolean isProtected(GearSlot slot)
	{
		return protectedSlots.contains(slot);
	}

	public int getTotalStabDefence()
	{
		return totalStabDefence;
	}

	public int getTotalSlashDefence()
	{
		return totalSlashDefence;
	}

	public int getTotalCrushDefence()
	{
		return totalCrushDefence;
	}

	public int getTotalMagicDefence()
	{
		return totalMagicDefence;
	}

	public int getTotalRangedDefence()
	{
		return totalRangedDefence;
	}

	public double getAverageMeleeDefence()
	{
		return (totalStabDefence + totalSlashDefence + totalCrushDefence) / 3.0;
	}

	public boolean hasUnpricedItemsSelected()
	{
		return unpricedItemsSelected;
	}
}
