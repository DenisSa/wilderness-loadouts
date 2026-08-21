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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

public class WildernessLoadoutsPanel extends PluginPanel
{
	private static final Color LOCKED_COLOR = new Color(90, 170, 230);
	private static final int ALTERNATIVE_LIMIT = 12;

	private final ItemManager itemManager;
	private final Listener listener;
	private final JComboBox<DefenceFocus> focusSelector = new JComboBox<>(DefenceFocus.values());
	private final JRadioButton protectedNone = new JRadioButton("High risk (0)");
	private final JRadioButton protectedThree = new JRadioButton("3", true);
	private final JRadioButton protectedFour = new JRadioButton("4");
	private final JTextField riskField = new JTextField("500k");
	private final JButton showInBankButton = new JButton("Show in Bank");
	private final JLabel statusLabel = new JLabel();
	private final JPanel resultPanel = new JPanel();
	private final Map<GearSlot, JCheckBox> slotCheckBoxes = new EnumMap<>(GearSlot.class);
	private final Map<GearSlot, LoadoutSlotSelection> slotSelections = new EnumMap<>(GearSlot.class);
	private final Timer recalculationTimer;

	private List<GearItem> latestOwnedGear = new ArrayList<>();
	private LoadoutResult latestResult;
	private boolean bankAvailable;
	private boolean updatingSlotControls;

	public WildernessLoadoutsPanel(ItemManager itemManager, Listener listener)
	{
		this.itemManager = itemManager;
		this.listener = listener;
		recalculationTimer = new Timer(250, event -> calculate());
		recalculationTimer.setRepeats(false);
		for (GearSlot slot : GearSlot.values())
		{
			slotSelections.put(slot, LoadoutSlotSelection.auto());
		}
		focusSelector.addActionListener(event -> requestRecalculate());
		protectedNone.addActionListener(event -> requestRecalculate());
		protectedThree.addActionListener(event -> requestRecalculate());
		protectedFour.addActionListener(event -> requestRecalculate());
		riskField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				requestRecalculate();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				requestRecalculate();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				requestRecalculate();
			}
		});
		showInBankButton.addActionListener(event -> listener.onShowInBank());

		JLabel title = new JLabel("Wilderness Loadouts");
		title.setForeground(Color.WHITE);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		add(title);

		add(sectionLabel("Defence focus"));
		add(focusSelector);
		add(sectionLabel("Protected/core items"));
		add(buildProtectedPanel());
		add(sectionLabel("Max filler risk"));
		add(riskField);
		add(sectionLabel("Equipment slots"));
		add(buildSlotControls());

		statusLabel.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
		statusLabel.setText("<html>Open your bank once to scan your gear.</html>");
		add(statusLabel);

		JLabel disclaimer = new JLabel(
			"<html><small>Protected/core items are assumed protected for loadout planning. "
				+ "High risk protects none. Risk uses RuneLite prices and deep-Wilderness repair fees "
				+ "for items physically locked with Trouver parchment.</small></html>");
		disclaimer.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(disclaimer);

		resultPanel.setLayout(new BorderLayout(0, 6));
		resultPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(resultPanel);
	}

	public void setBankAvailable(boolean available)
	{
		boolean becameAvailable = available && !bankAvailable;
		bankAvailable = available;
		if (available)
		{
			if (becameAvailable || latestResult == null)
			{
				showStatus("Bank gear scanned. Building a loadout...", false);
				requestRecalculate();
			}
		}
		else
		{
			recalculationTimer.stop();
			latestResult = null;
			latestOwnedGear = new ArrayList<>();
			setBankLayoutVisible(false);
			resultPanel.removeAll();
			resultPanel.revalidate();
			resultPanel.repaint();
			showStatus("Open your bank once to scan your gear.", true);
		}
	}

	public void setCalculating(boolean calculating)
	{
		if (calculating)
		{
			showStatus("Calculating the best valid loadout...", false);
		}
	}

	public void showStatus(String message, boolean warning)
	{
		statusLabel.setForeground(warning ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_INPROGRESS_COLOR);
		statusLabel.setText("<html>" + escapeHtml(message) + "</html>");
	}

	public void markResultStale()
	{
		if (bankAvailable)
		{
			showStatus("Owned gear changed. Refreshing the loadout...", false);
			requestRecalculate();
		}
	}

	public void setBankLayoutVisible(boolean visible)
	{
		showInBankButton.setText(visible ? "Hide in Bank" : "Show in Bank");
	}

	public void displayResult(LoadoutRequest request, LoadoutResult result, List<GearItem> ownedGear)
	{
		latestResult = result;
		latestOwnedGear = new ArrayList<>(ownedGear);
		showStatus("Loadout ready.", false);

		resultPanel.removeAll();
		JPanel summary = new JPanel(new GridLayout(0, 1, 0, 2));
		summary.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		summary.setBorder(new EmptyBorder(6, 6, 6, 6));
		summary.add(valueLabel(result.getFocus().toString(), formatScore(result.getObjectiveScore())));
		summary.add(detailLabel("Magic Defence", formatSigned(result.getTotalMagicDefence())));
		summary.add(detailLabel("Ranged Defence", formatSigned(result.getTotalRangedDefence())));
		summary.add(detailLabel("Avg Melee Defence", formatSigned(result.getAverageMeleeDefence())));
		summary.add(detailLabel(
			"Filler risk",
			formatGp(result.getFillerRisk()) + " / " + formatGp(result.getMaxFillerRisk())));
		summary.add(detailLabel("Remaining", formatGp(result.getRemainingRisk())));
		resultPanel.add(summary, BorderLayout.NORTH);

		JPanel equipmentGrid = new JPanel(new GridLayout(0, 2, 4, 4));
		equipmentGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (GearSlot slot : GearSlot.values())
		{
			equipmentGrid.add(buildResultButton(request, result, slot));
		}
		resultPanel.add(equipmentGrid, BorderLayout.CENTER);

		JPanel actions = new JPanel(new GridLayout(0, 1, 0, 4));
		actions.setBackground(ColorScheme.DARK_GRAY_COLOR);
		if (result.hasUnpricedItemsSelected())
		{
			JLabel warning = new JLabel("<html>Some items are unpriced; displayed risk may be incomplete.</html>");
			warning.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
			actions.add(warning);
		}
		actions.add(showInBankButton);
		resultPanel.add(actions, BorderLayout.SOUTH);

		resultPanel.revalidate();
		resultPanel.repaint();
	}

	private JPanel buildProtectedPanel()
	{
		ButtonGroup group = new ButtonGroup();
		group.add(protectedNone);
		group.add(protectedThree);
		group.add(protectedFour);
		JPanel panel = new JPanel(new GridLayout(1, 3));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.add(protectedNone);
		panel.add(protectedThree);
		panel.add(protectedFour);
		return panel;
	}

	private JPanel buildSlotControls()
	{
		JPanel panel = new JPanel(new GridLayout(0, 2, 2, 0));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (GearSlot slot : GearSlot.values())
		{
			JCheckBox checkBox = new JCheckBox(slot.getDisplayName(), true);
			checkBox.setToolTipText("Unchecked forces this slot empty");
			checkBox.addActionListener(event ->
			{
				if (updatingSlotControls)
				{
					return;
				}
				slotSelections.put(slot, checkBox.isSelected()
					? LoadoutSlotSelection.auto()
					: LoadoutSlotSelection.empty());
				requestRecalculate();
			});
			slotCheckBoxes.put(slot, checkBox);
			panel.add(checkBox);
		}
		return panel;
	}

	private JButton buildResultButton(LoadoutRequest request, LoadoutResult result, GearSlot slot)
	{
		GearItem item = result.getSelectedItem(slot);
		boolean protectedItem = result.isProtected(slot);
		boolean locked = request.getSlotSelection(slot).getState() == SlotState.LOCKED;
		String markers = (protectedItem ? "★ " : "") + (locked ? "[LOCK] " : "");
		String name = item == null || item.isEmpty() ? "Empty" : shorten(item.getName(), 18);

		JButton button = new JButton(
			"<html><center>" + escapeHtml(slot.getDisplayName()) + "<br>"
				+ escapeHtml(markers + name) + "</center></html>");
		button.setPreferredSize(new Dimension(96, 68));
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setBorder(BorderFactory.createLineBorder(
			protectedItem ? ColorScheme.BRAND_ORANGE : (locked ? LOCKED_COLOR : ColorScheme.MEDIUM_GRAY_COLOR),
			protectedItem || locked ? 2 : 1));
		button.setToolTipText(buildItemTooltip(item, result.getFocus()));
		button.addActionListener(event -> showAlternatives(slot));
		if (item != null && !item.isEmpty())
		{
			itemManager.getImage(item.getItemId()).addTo(button);
		}
		return button;
	}

	private void showAlternatives(GearSlot slot)
	{
		DefenceFocus focus = (DefenceFocus) focusSelector.getSelectedItem();
		List<GearItem> candidates = new ArrayList<>();
		for (GearItem item : latestOwnedGear)
		{
			if (item.getSlot() == slot)
			{
				candidates.add(item);
			}
		}
		candidates.sort(Comparator
			.comparingDouble((GearItem item) -> focus.score(item)).reversed()
			.thenComparingLong(GearItem::getRiskValue)
			.thenComparingInt(GearItem::getItemId));
		if (candidates.size() > ALTERNATIVE_LIMIT)
		{
			candidates = new ArrayList<>(candidates.subList(0, ALTERNATIVE_LIMIT));
		}

		DefaultListModel<GearItem> model = new DefaultListModel<>();
		for (GearItem candidate : candidates)
		{
			model.addElement(candidate);
		}
		JList<GearItem> list = new JList<>(model);
		list.setCellRenderer(new AlternativeRenderer(focus));
		list.setVisibleRowCount(Math.min(6, Math.max(1, model.size())));
		if (!model.isEmpty())
		{
			list.setSelectedIndex(0);
		}

		Object[] options = {"Lock selected", "Auto", "Empty", "Cancel"};
		int choice = JOptionPane.showOptionDialog(
			this,
			new JScrollPane(list),
			slot.getDisplayName() + " alternatives",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			options,
			options[0]);

		if (choice == 0)
		{
			GearItem selected = list.getSelectedValue();
			if (selected == null)
			{
				showStatus("No owned item is available to lock in that slot.", true);
				return;
			}
			setSlotSelection(slot, LoadoutSlotSelection.locked(selected.getItemId()));
		}
		else if (choice == 1)
		{
			setSlotSelection(slot, LoadoutSlotSelection.auto());
		}
		else if (choice == 2)
		{
			setSlotSelection(slot, LoadoutSlotSelection.empty());
		}
	}

	private void setSlotSelection(GearSlot slot, LoadoutSlotSelection selection)
	{
		slotSelections.put(slot, selection);
		updatingSlotControls = true;
		slotCheckBoxes.get(slot).setSelected(selection.getState() != SlotState.EMPTY);
		updatingSlotControls = false;
		requestRecalculate();
	}

	private void calculate()
	{
		if (!bankAvailable)
		{
			showStatus("Open your bank once to scan your gear.", true);
			return;
		}
		try
		{
			listener.onCalculate(buildRequest());
		}
		catch (IllegalArgumentException exception)
		{
			showStatus(exception.getMessage(), true);
		}
	}

	private void requestRecalculate()
	{
		if (bankAvailable)
		{
			recalculationTimer.restart();
		}
	}

	private LoadoutRequest buildRequest()
	{
		DefenceFocus focus = (DefenceFocus) focusSelector.getSelectedItem();
		int protectedLimit = protectedNone.isSelected() ? 0 : (protectedFour.isSelected() ? 4 : 3);
		long budget = RiskBudgetParser.parse(riskField.getText());
		return new LoadoutRequest(focus, protectedLimit, budget, slotSelections);
	}

	private static JLabel sectionLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JLabel valueLabel(String label, String value)
	{
		JLabel result = new JLabel(
			"<html><b>" + escapeHtml(label) + "</b><br><font size='+1'>" + escapeHtml(value) + "</font></html>");
		result.setForeground(Color.WHITE);
		return result;
	}

	private static JLabel detailLabel(String label, String value)
	{
		JLabel result = new JLabel(label + ": " + value);
		result.setForeground(ColorScheme.TEXT_COLOR);
		return result;
	}

	private static String buildItemTooltip(GearItem item, DefenceFocus focus)
	{
		if (item == null || item.isEmpty())
		{
			return "Click to choose Auto, Empty, or lock an owned alternative";
		}
		return item.getName()
			+ " | " + focus + ": " + formatScore(focus.score(item))
			+ " | Magic " + item.getMagicDefence()
			+ " | Ranged " + item.getRangedDefence()
			+ " | Melee " + formatScore(item.getMeleeDefence())
			+ " | " + formatPrice(item);
	}

	private static String formatScore(double value)
	{
		if (Math.abs(value - Math.rint(value)) < 0.0001)
		{
			return String.format(Locale.ENGLISH, "%.0f", value);
		}
		return String.format(Locale.ENGLISH, "%.1f", value);
	}

	private static String formatSigned(double value)
	{
		return (value >= 0 ? "+" : "") + formatScore(value);
	}

	private static String formatGp(long value)
	{
		return QuantityFormatter.quantityToStackSize(value);
	}

	private static String formatPrice(GearItem item)
	{
		if (!item.isPriceKnown())
		{
			return "unpriced";
		}
		return item.isTrouverRepairValue()
			? "repair " + formatGp(item.getRiskValue())
			: "~" + formatGp(item.getRiskValue());
	}

	private static String shorten(String value, int maxLength)
	{
		return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
	}

	private static String escapeHtml(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public interface Listener
	{
		void onCalculate(LoadoutRequest request);

		void onShowInBank();
	}

	private static final class AlternativeRenderer extends JPanel implements ListCellRenderer<GearItem>
	{
		private final DefenceFocus focus;
		private final JLabel name = new JLabel();
		private final JLabel details = new JLabel();

		private AlternativeRenderer(DefenceFocus focus)
		{
			super(new GridLayout(0, 1));
			this.focus = focus;
			setBorder(new EmptyBorder(4, 4, 4, 4));
			add(name);
			add(details);
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends GearItem> list,
			GearItem item,
			int index,
			boolean selected,
			boolean cellHasFocus)
		{
			name.setText(item.getName());
			name.setForeground(Color.WHITE);
			details.setText(
				"Score " + formatScore(focus.score(item))
					+ " | M " + item.getMagicDefence()
					+ " R " + item.getRangedDefence()
					+ " Melee " + formatScore(item.getMeleeDefence())
					+ " | " + formatPrice(item));
			details.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			setBackground(selected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			return this;
		}
	}
}
