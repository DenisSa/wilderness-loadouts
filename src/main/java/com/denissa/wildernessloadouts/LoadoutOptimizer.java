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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LoadoutOptimizer
{
	public LoadoutResult optimize(LoadoutRequest request, Collection<GearItem> ownedGear)
	{
		EnumMap<GearSlot, List<GearItem>> ownedBySlot = groupOwnedGear(ownedGear);
		GearItem lockedWeapon = findLockedItem(request, ownedBySlot, GearSlot.WEAPON);
		GearItem lockedShield = findLockedItem(request, ownedBySlot, GearSlot.SHIELD);
		if (lockedWeapon != null && lockedWeapon.isTwoHanded() && lockedShield != null)
		{
			throw new IllegalArgumentException("A locked two-handed weapon cannot be used with a locked shield");
		}

		EnumMap<GearSlot, List<GearItem>> candidatesBySlot = new EnumMap<>(GearSlot.class);
		for (GearSlot slot : GearSlot.values())
		{
			candidatesBySlot.put(slot, buildCandidates(
				request,
				ownedBySlot.get(slot),
				slot,
				lockedWeapon != null && lockedWeapon.isTwoHanded(),
				lockedShield != null));
		}

		List<State> states = Collections.singletonList(State.initial());
		for (GearSlot slot : GearSlot.values())
		{
			List<State> next = new ArrayList<>();
			for (State state : states)
			{
				for (GearItem candidate : candidatesBySlot.get(slot))
				{
					if (slot == GearSlot.SHIELD && state.twoHandedWeaponSelected && !candidate.isEmpty())
					{
						continue;
					}

					long risk = candidate.getLossProfile().getCostIfUnprotected();
					if (risk <= request.getMaxFillerRisk() - state.fillerCost)
					{
						next.add(state.select(slot, candidate, false, request.getFocus()));
					}

					if (!candidate.isEmpty()
						&& candidate.getLossProfile().canBeProtected()
						&& state.protectedUsed < request.getProtectedLimit())
					{
						next.add(state.select(slot, candidate, true, request.getFocus()));
					}
				}
			}
			states = pruneStates(next);
		}

		if (states.isEmpty())
		{
			throw new IllegalArgumentException("No valid loadout satisfies the selected locks and risk budget");
		}

		State best = states.get(0);
		for (int i = 1; i < states.size(); i++)
		{
			State candidate = states.get(i);
			if (compareFinal(candidate, best) < 0)
			{
				best = candidate;
			}
		}

		return new LoadoutResult(
			request,
			best.objectiveScore,
			best.fillerCost,
			best.otherRisk,
			best.selectedItems,
			best.protectedSlots);
	}

	List<GearItem> preprocessCandidates(DefenceFocus focus, List<GearItem> candidates)
	{
		List<GearItem> ordered = new ArrayList<>(candidates);
		ordered.sort(Comparator.comparingInt(GearItem::getItemId));

		List<GearItem> frontier = new ArrayList<>();
		for (GearItem candidate : ordered)
		{
			boolean dominated = false;
			for (GearItem other : ordered)
			{
				if (candidate == other)
				{
					continue;
				}
				if (candidateDominates(other, candidate, focus))
				{
					dominated = true;
					break;
				}
			}
			if (!dominated)
			{
				frontier.add(candidate);
			}
		}

		frontier.sort(Comparator
			.comparingDouble((GearItem item) -> focus.score(item)).reversed()
			.thenComparingLong(item -> item.getLossProfile().getCostIfUnprotected())
			.thenComparingInt(GearItem::getItemId));
		return frontier;
	}

	private static EnumMap<GearSlot, List<GearItem>> groupOwnedGear(Collection<GearItem> ownedGear)
	{
		EnumMap<GearSlot, List<GearItem>> bySlot = new EnumMap<>(GearSlot.class);
		for (GearSlot slot : GearSlot.values())
		{
			bySlot.put(slot, new ArrayList<>());
		}

		for (GearItem item : ownedGear)
		{
			if (!item.isEmpty())
			{
				bySlot.get(item.getSlot()).add(item);
			}
		}
		for (List<GearItem> items : bySlot.values())
		{
			items.sort(Comparator.comparingInt(GearItem::getItemId));
		}
		return bySlot;
	}

	private GearItem findLockedItem(
		LoadoutRequest request,
		Map<GearSlot, List<GearItem>> ownedBySlot,
		GearSlot slot)
	{
		LoadoutSlotSelection selection = request.getSlotSelection(slot);
		if (selection.getState() != SlotState.LOCKED)
		{
			return null;
		}

		for (GearItem item : ownedBySlot.get(slot))
		{
			if (item.getItemId() == selection.getLockedItemId())
			{
				return item;
			}
		}
		throw new IllegalArgumentException("The locked " + slot.getDisplayName().toLowerCase() + " item is no longer owned");
	}

	private List<GearItem> buildCandidates(
		LoadoutRequest request,
		List<GearItem> owned,
		GearSlot slot,
		boolean lockedTwoHandedWeapon,
		boolean shieldLocked)
	{
		LoadoutSlotSelection selection = request.getSlotSelection(slot);
		if (slot == GearSlot.SHIELD && lockedTwoHandedWeapon)
		{
			return Collections.singletonList(GearItem.empty(slot));
		}
		if (selection.getState() == SlotState.EMPTY)
		{
			return Collections.singletonList(GearItem.empty(slot));
		}
		if (selection.getState() == SlotState.LOCKED)
		{
			GearItem lockedItem = findLockedItem(request, Collections.singletonMap(slot, owned), slot);
			if (!lockedItem.getLossProfile().isAutoEligible())
			{
				throw new IllegalArgumentException(
					lockedItem.getName() + " cannot be used: "
						+ lockedItem.getLossProfile().getEligibilityPolicy().getExclusionReason());
			}
			return Collections.singletonList(lockedItem);
		}

		List<GearItem> candidates = new ArrayList<>();
		for (GearItem item : owned)
		{
			if (!item.getLossProfile().isAutoEligible())
			{
				continue;
			}
			if (slot == GearSlot.WEAPON && shieldLocked && item.isTwoHanded())
			{
				continue;
			}
			candidates.add(item);
		}
		candidates.add(GearItem.empty(slot));
		return preprocessCandidates(request.getFocus(), candidates);
	}

	private static boolean candidateDominates(GearItem first, GearItem second, DefenceFocus focus)
	{
		if (first.getSlot() != second.getSlot() || !hasCompatibleSuperset(first, second))
		{
			return false;
		}

		double firstScore = focus.score(first);
		double secondScore = focus.score(second);
		LossProfile firstLoss = first.getLossProfile();
		LossProfile secondLoss = second.getLossProfile();
		if (Double.compare(firstScore, secondScore) < 0
			|| firstLoss.getCostIfUnprotected() > secondLoss.getCostIfUnprotected())
		{
			return false;
		}
		if (secondLoss.canBeProtected()
			&& (!firstLoss.canBeProtected()
				|| firstLoss.getCostIfProtected() > secondLoss.getCostIfProtected()))
		{
			return false;
		}

		return Double.compare(firstScore, secondScore) > 0
			|| firstLoss.getCostIfUnprotected() < secondLoss.getCostIfUnprotected()
			|| (secondLoss.canBeProtected()
				&& firstLoss.getCostIfProtected() < secondLoss.getCostIfProtected())
			|| (firstLoss.canBeProtected() && !secondLoss.canBeProtected())
			|| (first.isTwoHanded() != second.isTwoHanded() && !first.isTwoHanded())
			|| first.getItemId() < second.getItemId();
	}

	private static boolean hasCompatibleSuperset(GearItem first, GearItem second)
	{
		if (first.getSlot() == GearSlot.SHIELD)
		{
			// An empty shield is what keeps a two-handed weapon legal, so a real
			// shield is never a superset of it.
			return first.isEmpty() || !second.isEmpty();
		}
		if (first.getSlot() != GearSlot.WEAPON)
		{
			return true;
		}
		return !first.isTwoHanded() || second.isTwoHanded();
	}

	private static List<State> pruneStates(List<State> states)
	{
		Map<StateKey, List<State>> groups = new HashMap<>();
		for (State state : states)
		{
			StateKey key = new StateKey(state.protectedUsed, state.twoHandedWeaponSelected);
			List<State> frontier = groups.computeIfAbsent(key, ignored -> new ArrayList<>());
			boolean dominated = false;
			for (State existing : frontier)
			{
				if (dominatesOrPreferred(existing, state))
				{
					dominated = true;
					break;
				}
			}
			if (dominated)
			{
				continue;
			}

			Iterator<State> iterator = frontier.iterator();
			while (iterator.hasNext())
			{
				if (dominatesOrPreferred(state, iterator.next()))
				{
					iterator.remove();
				}
			}
			frontier.add(state);
		}

		List<State> result = new ArrayList<>();
		for (List<State> frontier : groups.values())
		{
			result.addAll(frontier);
		}
		return result;
	}

	private static boolean dominatesOrPreferred(State first, State second)
	{
		if (first.fillerCost > second.fillerCost
			|| first.otherRisk > second.otherRisk
			|| Double.compare(first.objectiveScore, second.objectiveScore) < 0)
		{
			return false;
		}

		if (first.fillerCost < second.fillerCost
			|| first.otherRisk < second.otherRisk
			|| Double.compare(first.objectiveScore, second.objectiveScore) > 0)
		{
			return true;
		}
		return compareSelection(first, second) <= 0;
	}

	private static int compareFinal(State first, State second)
	{
		int score = Double.compare(second.objectiveScore, first.objectiveScore);
		if (score != 0)
		{
			return score;
		}
		int risk = Long.compare(first.totalRisk(), second.totalRisk());
		if (risk != 0)
		{
			return risk;
		}
		int fillerRisk = Long.compare(first.fillerCost, second.fillerCost);
		if (fillerRisk != 0)
		{
			return fillerRisk;
		}
		int protectedItems = Integer.compare(second.protectedUsed, first.protectedUsed);
		if (protectedItems != 0)
		{
			return protectedItems;
		}
		return compareSelection(first, second);
	}

	private static int compareSelection(State first, State second)
	{
		for (GearSlot slot : GearSlot.values())
		{
			GearItem firstItem = first.selectedItems.get(slot);
			GearItem secondItem = second.selectedItems.get(slot);
			if (firstItem == null || secondItem == null)
			{
				continue;
			}
			int item = Integer.compare(firstItem.getItemId(), secondItem.getItemId());
			if (item != 0)
			{
				return item;
			}
			boolean firstProtected = first.protectedSlots.contains(slot);
			boolean secondProtected = second.protectedSlots.contains(slot);
			if (firstProtected != secondProtected)
			{
				return firstProtected ? -1 : 1;
			}
		}
		return 0;
	}

	private static final class State
	{
		private final int protectedUsed;
		private final long fillerCost;
		private final long otherRisk;
		private final double objectiveScore;
		private final EnumMap<GearSlot, GearItem> selectedItems;
		private final EnumSet<GearSlot> protectedSlots;
		private final boolean twoHandedWeaponSelected;

		private State(
			int protectedUsed,
			long fillerCost,
			long otherRisk,
			double objectiveScore,
			EnumMap<GearSlot, GearItem> selectedItems,
			EnumSet<GearSlot> protectedSlots,
			boolean twoHandedWeaponSelected)
		{
			this.protectedUsed = protectedUsed;
			this.fillerCost = fillerCost;
			this.otherRisk = otherRisk;
			this.objectiveScore = objectiveScore;
			this.selectedItems = selectedItems;
			this.protectedSlots = protectedSlots;
			this.twoHandedWeaponSelected = twoHandedWeaponSelected;
		}

		private static State initial()
		{
			return new State(
				0,
				0,
				0,
				0,
				new EnumMap<>(GearSlot.class),
				EnumSet.noneOf(GearSlot.class),
				false);
		}

		private State select(GearSlot slot, GearItem item, boolean protect, DefenceFocus focus)
		{
			EnumMap<GearSlot, GearItem> items = new EnumMap<>(selectedItems);
			items.put(slot, item);
			EnumSet<GearSlot> protectedCopy = protectedSlots.isEmpty()
				? EnumSet.noneOf(GearSlot.class)
				: EnumSet.copyOf(protectedSlots);
			if (protect)
			{
				protectedCopy.add(slot);
			}

			return new State(
				protectedUsed + (protect ? 1 : 0),
				fillerCost + (protect ? 0 : item.getLossProfile().getCostIfUnprotected()),
				otherRisk + (protect ? item.getLossProfile().getCostIfProtected() : 0),
				objectiveScore + focus.score(item),
				items,
				protectedCopy,
				twoHandedWeaponSelected || (slot == GearSlot.WEAPON && item.isTwoHanded()));
		}

		private long totalRisk()
		{
			return fillerCost > Long.MAX_VALUE - otherRisk
				? Long.MAX_VALUE
				: fillerCost + otherRisk;
		}
	}

	private static final class StateKey
	{
		private final int protectedUsed;
		private final boolean twoHandedWeaponSelected;

		private StateKey(int protectedUsed, boolean twoHandedWeaponSelected)
		{
			this.protectedUsed = protectedUsed;
			this.twoHandedWeaponSelected = twoHandedWeaponSelected;
		}

		@Override
		public boolean equals(Object object)
		{
			if (this == object)
			{
				return true;
			}
			if (!(object instanceof StateKey))
			{
				return false;
			}
			StateKey other = (StateKey) object;
			return protectedUsed == other.protectedUsed
				&& twoHandedWeaponSelected == other.twoHandedWeaponSelected;
		}

		@Override
		public int hashCode()
		{
			return 31 * protectedUsed + (twoHandedWeaponSelected ? 1 : 0);
		}
	}
}
