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

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Wilderness Loadouts",
	description = "Build defensive Wilderness loadouts from owned gear under a risk budget",
	tags = {"wilderness", "pvp", "gear", "defence", "defense", "bank", "loadout"}
)
@PluginDependency(BankTagsPlugin.class)
@Slf4j
public class WildernessLoadoutsPlugin extends Plugin implements WildernessLoadoutsPanel.Listener
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OwnedGearService ownedGearService;

	@Inject
	private BankLayoutService bankLayoutService;

	private final LoadoutOptimizer optimizer = new LoadoutOptimizer();
	private final AtomicLong calculationGeneration = new AtomicLong();
	private ExecutorService optimizerExecutor;
	private WildernessLoadoutsPanel panel;
	private NavigationButton navigationButton;
	private LoadoutResult latestResult;

	@Override
	protected void startUp()
	{
		log.debug("Wilderness Loadouts started");
		ownedGearService.clearSessionSnapshot();
		optimizerExecutor = Executors.newSingleThreadExecutor(runnable ->
		{
			Thread thread = new Thread(runnable, "wilderness-loadouts-optimizer");
			thread.setDaemon(true);
			return thread;
		});

		panel = new WildernessLoadoutsPanel(itemManager, this);
		BufferedImage icon = itemManager.getImage(ItemID.DRAGON_SQ_SHIELD);
		navigationButton = NavigationButton.builder()
			.tooltip("Wilderness Loadouts")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		clientThread.invokeLater(() ->
		{
			ItemContainer bank = client.getItemContainer(InventoryID.BANK);
			if (bank != null)
			{
				ownedGearService.updateBankSnapshot(bank);
			}
			SwingUtilities.invokeLater(() -> panel.setBankAvailable(ownedGearService.hasBankSnapshot()));
		});
	}

	@Override
	protected void shutDown()
	{
		log.debug("Wilderness Loadouts stopped");
		calculationGeneration.incrementAndGet();
		if (optimizerExecutor != null)
		{
			optimizerExecutor.shutdownNow();
			optimizerExecutor = null;
		}

		clientThread.invokeLater(bankLayoutService::clearGeneratedLayout);
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		navigationButton = null;
		panel = null;
		latestResult = null;
		ownedGearService.clearSessionSnapshot();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK)
		{
			ownedGearService.updateBankSnapshot(event.getItemContainer());
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.setBankAvailable(true);
					panel.markResultStale();
				}
			});
		}
		else if (event.getContainerId() == InventoryID.WORN)
		{
			ownedGearService.markWornEquipmentChanged();
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.markResultStale();
				}
			});
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN && ownedGearService.hasBankSnapshot())
		{
			ownedGearService.clearSessionSnapshot();
			calculationGeneration.incrementAndGet();
			latestResult = null;
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.setBankAvailable(false);
				}
			});
		}
	}

	@Override
	public void onCalculate(LoadoutRequest request)
	{
		if (!ownedGearService.hasBankSnapshot())
		{
			panel.showStatus("Open your bank once to scan your gear.", true);
			return;
		}

		long generation = calculationGeneration.incrementAndGet();
		panel.setCalculating(true);
		clientThread.invokeLater(() ->
		{
			if (generation != calculationGeneration.get() || !ownedGearService.hasBankSnapshot())
			{
				return;
			}
			List<GearItem> ownedGear = ownedGearService.buildOwnedGear();
			long snapshotVersion = ownedGearService.getSnapshotVersion();
			try
			{
				optimizerExecutor.submit(() -> optimize(generation, snapshotVersion, request, ownedGear));
			}
			catch (RejectedExecutionException ignored)
			{
				log.debug("Optimizer request rejected during shutdown");
			}
		});
	}

	@Override
	public void onShowInBank()
	{
		if (latestResult == null)
		{
			panel.showStatus("Calculate a loadout before showing it in the bank.", true);
			return;
		}

		if (!bankLayoutService.isBankTagsEnabled())
		{
			int enable = JOptionPane.showConfirmDialog(
				panel,
				"Bank Tags is required for the virtual layout. Enable it now?",
				"Bank Tags required",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.INFORMATION_MESSAGE);
			if (enable != JOptionPane.YES_OPTION)
			{
				panel.showStatus("Enable Bank Tags in RuneLite settings to use virtual layouts.", true);
				return;
			}
			panel.showStatus("Enabling Bank Tags for the virtual layout...", false);
			try
			{
				bankLayoutService.enableBankTags();
			}
			catch (PluginInstantiationException exception)
			{
				log.error("Unable to enable Bank Tags", exception);
				panel.showStatus("Bank Tags could not be enabled. Enable it in RuneLite settings and try again.", true);
				return;
			}
		}

		LoadoutResult result = latestResult;
		clientThread.invokeLater(() ->
		{
			String error;
			try
			{
				error = bankLayoutService.showLoadout(result);
			}
			catch (RuntimeException exception)
			{
				log.error("Unable to show Wilderness Loadouts bank layout", exception);
				error = "The virtual bank layout could not be opened.";
			}
			String message = error;
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.showStatus(
						message == null ? "Virtual Wilderness Loadouts bank layout opened." : message,
						message != null);
				}
			});
		});
	}

	private void optimize(
		long generation,
		long snapshotVersion,
		LoadoutRequest request,
		List<GearItem> ownedGear)
	{
		try
		{
			LoadoutResult result = optimizer.optimize(request, ownedGear);
			SwingUtilities.invokeLater(() ->
			{
				if (panel == null || generation != calculationGeneration.get())
				{
					return;
				}
				if (snapshotVersion != ownedGearService.getSnapshotVersion())
				{
					panel.setCalculating(false);
					panel.showStatus("Gear changed during calculation. Calculate again to refresh.", true);
					return;
				}

				latestResult = result;
				panel.displayResult(request, result, ownedGear);
			});
		}
		catch (IllegalArgumentException exception)
		{
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null && generation == calculationGeneration.get())
				{
					panel.setCalculating(false);
					panel.showStatus(exception.getMessage(), true);
				}
			});
		}
		catch (RuntimeException exception)
		{
			log.error("Unable to optimize Wilderness loadout", exception);
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null && generation == calculationGeneration.get())
				{
					panel.setCalculating(false);
					panel.showStatus("The loadout could not be calculated.", true);
				}
			});
		}
	}
}
