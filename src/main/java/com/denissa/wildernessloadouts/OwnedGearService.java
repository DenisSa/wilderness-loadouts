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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

@Singleton
public class OwnedGearService
{
	private final Client client;
	private final ItemManager itemManager;
	private final Set<Integer> bankItemIds = new LinkedHashSet<>();
	private volatile boolean bankSnapshotSeen;
	private volatile long snapshotVersion;

	@Inject
	OwnedGearService(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
	}

	public void clearSessionSnapshot()
	{
		bankItemIds.clear();
		bankSnapshotSeen = false;
		snapshotVersion++;
	}

	public void updateBankSnapshot(ItemContainer bank)
	{
		bankItemIds.clear();
		addContainerItems(bankItemIds, bank);
		bankSnapshotSeen = true;
		snapshotVersion++;
	}

	public boolean hasBankSnapshot()
	{
		return bankSnapshotSeen;
	}

	public long getSnapshotVersion()
	{
		return snapshotVersion;
	}

	public void markCarriedGearChanged()
	{
		snapshotVersion++;
	}

	public List<GearItem> buildOwnedGear()
	{
		if (!bankSnapshotSeen)
		{
			return new ArrayList<>();
		}

		Set<Integer> itemIds = new LinkedHashSet<>(bankItemIds);
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			addContainerItems(itemIds, inventory);
		}
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn != null)
		{
			addContainerItems(itemIds, worn);
		}

		List<GearItem> gear = new ArrayList<>();
		for (int itemId : itemIds)
		{
			ItemStats itemStats = itemManager.getItemStats(itemId);
			if (itemStats == null || !itemStats.isEquipable() || itemStats.getEquipment() == null)
			{
				continue;
			}

			ItemEquipmentStats equipment = itemStats.getEquipment();
			GearSlot slot = fromEquipmentSlot(equipment.getSlot());
			if (slot == null)
			{
				continue;
			}

			ItemComposition composition = itemManager.getItemComposition(itemId);
			int price = itemManager.getItemPrice(itemId);
			long riskValue = resolveRiskValue(itemId, price);
			if (riskValue <= 0)
			{
				continue;
			}
			boolean trouverRepairValue = TrouverRiskValues.getRepairCost(itemId) > 0;
			gear.add(new GearItem(
				itemId,
				composition.getName(),
				slot,
				equipment.getDstab(),
				equipment.getDslash(),
				equipment.getDcrush(),
				equipment.getDmagic(),
				equipment.getDrange(),
				riskValue,
				equipment.isTwoHanded(),
				true,
				trouverRepairValue));
		}
		return gear;
	}

	static long resolveRiskValue(int itemId, int marketPrice)
	{
		if (TrouverRiskValues.isLegacyLowTier(itemId))
		{
			return 0L;
		}
		long repairCost = TrouverRiskValues.getRepairCost(itemId);
		return repairCost > 0 ? repairCost : Math.max(0, marketPrice);
	}

	private void addContainerItems(Collection<Integer> itemIds, ItemContainer container)
	{
		for (Item item : container.getItems())
		{
			if (item.getId() < 0)
			{
				continue;
			}

			ItemComposition composition = itemManager.getItemComposition(item.getId());
			if (isPhysicalItem(
				item,
				composition.getPlaceholderTemplateId() != -1,
				composition.getNote() != -1))
			{
				itemIds.add(itemManager.canonicalize(item.getId()));
			}
		}
	}

	static boolean isPhysicalItem(Item item, boolean placeholder, boolean noted)
	{
		return item.getId() >= 0 && item.getQuantity() > 0 && !placeholder && !noted;
	}

	private static GearSlot fromEquipmentSlot(int slot)
	{
		if (slot == EquipmentInventorySlot.HEAD.getSlotIdx())
		{
			return GearSlot.HEAD;
		}
		if (slot == EquipmentInventorySlot.CAPE.getSlotIdx())
		{
			return GearSlot.CAPE;
		}
		if (slot == EquipmentInventorySlot.AMULET.getSlotIdx())
		{
			return GearSlot.NECK;
		}
		if (slot == EquipmentInventorySlot.AMMO.getSlotIdx())
		{
			return GearSlot.AMMO;
		}
		if (slot == EquipmentInventorySlot.WEAPON.getSlotIdx())
		{
			return GearSlot.WEAPON;
		}
		if (slot == EquipmentInventorySlot.BODY.getSlotIdx())
		{
			return GearSlot.BODY;
		}
		if (slot == EquipmentInventorySlot.SHIELD.getSlotIdx())
		{
			return GearSlot.SHIELD;
		}
		if (slot == EquipmentInventorySlot.LEGS.getSlotIdx())
		{
			return GearSlot.LEGS;
		}
		if (slot == EquipmentInventorySlot.GLOVES.getSlotIdx())
		{
			return GearSlot.GLOVES;
		}
		if (slot == EquipmentInventorySlot.BOOTS.getSlotIdx())
		{
			return GearSlot.BOOTS;
		}
		if (slot == EquipmentInventorySlot.RING.getSlotIdx())
		{
			return GearSlot.RING;
		}
		return null;
	}
}
