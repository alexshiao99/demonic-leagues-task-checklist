/*
 * Copyright (c) 2026, alexshiao99 <smicalexshiao@hotmail.com>
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.smicalexshiao.leaguetasks;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

class LeagueTasksPanel extends PluginPanel
{
	private final LeagueTasksPlugin plugin;
	private final LeagueTasksConfig config;
	private final List<LeagueTask> allTasks;

	private final IconTextField searchField;
	private final Map<LeagueTaskTier, JCheckBox> tierCheckboxes = new EnumMap<>(LeagueTaskTier.class);
	private final Map<LeagueTaskRegion, JCheckBox> regionCheckboxes = new EnumMap<>(LeagueTaskRegion.class);
	private final JComboBox<CompletedFilter> completedCombo;
	private final JComboBox<PactFilter> pactCombo;
	private final JCheckBox hideUnavailableCheckbox;
	private final JLabel progressLabel = new JLabel();
	private final JPanel listPanel = new JPanel();

	private final List<LeagueTaskRow> currentRows = new ArrayList<>();

	LeagueTasksPanel(LeagueTasksPlugin plugin, LeagueTasksConfig config, List<LeagueTask> tasks)
	{
		super(false);
		this.plugin = plugin;
		this.config = config;
		this.allTasks = tasks;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(6, 6, 6, 6));

		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Demonic Pacts Tasks");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		controls.add(title);
		controls.add(verticalStrut(6));

		searchField = new IconTextField();
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setPreferredSize(new Dimension(PANEL_WIDTH - 12, 28));
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.addClearListener(this::rebuild);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				rebuild();
			}
		});
		searchField.setAlignmentX(LEFT_ALIGNMENT);
		controls.add(searchField);
		controls.add(verticalStrut(6));

		for (LeagueTaskTier t : LeagueTaskTier.values())
		{
			tierCheckboxes.put(t, buildFilterCheckbox(t.getDisplayName()));
		}
		controls.add(buildLabeledRow("Tiers", buildMultiSelectDropdown(tierCheckboxes)));
		controls.add(verticalStrut(4));

		for (LeagueTaskRegion r : LeagueTaskRegion.values())
		{
			regionCheckboxes.put(r, buildFilterCheckbox(r.getDisplayName()));
		}
		controls.add(buildLabeledRow("Regions", buildMultiSelectDropdown(regionCheckboxes)));
		controls.add(verticalStrut(4));

		completedCombo = new JComboBox<>(CompletedFilter.values());
		completedCombo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		completedCombo.setFocusable(false);
		completedCombo.setSelectedItem(config.completedFilter());
		completedCombo.addActionListener(e -> plugin.setCompletedFilter((CompletedFilter) completedCombo.getSelectedItem()));
		controls.add(buildLabeledRow("Completed", completedCombo));
		controls.add(verticalStrut(4));

		pactCombo = new JComboBox<>(PactFilter.values());
		pactCombo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		pactCombo.setFocusable(false);
		pactCombo.addActionListener(e -> rebuild());
		controls.add(buildLabeledRow("Pacts", pactCombo));
		controls.add(verticalStrut(4));

		hideUnavailableCheckbox = new JCheckBox("Hide unmet skill requirement");
		hideUnavailableCheckbox.setToolTipText("Hide tasks whose skill requirements you don't yet meet.");
		hideUnavailableCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		hideUnavailableCheckbox.setForeground(ColorScheme.TEXT_COLOR);
		hideUnavailableCheckbox.setFocusPainted(false);
		hideUnavailableCheckbox.setSelected(config.hideUnavailable());
		hideUnavailableCheckbox.addActionListener(e -> plugin.setHideUnavailable(hideUnavailableCheckbox.isSelected()));
		hideUnavailableCheckbox.setAlignmentX(LEFT_ALIGNMENT);
		controls.add(hideUnavailableCheckbox);
		controls.add(verticalStrut(4));

		progressLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		progressLabel.setFont(FontManager.getRunescapeSmallFont());
		progressLabel.setAlignmentX(LEFT_ALIGNMENT);
		controls.add(progressLabel);

		add(controls, BorderLayout.NORTH);

		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listPanel.setBorder(new EmptyBorder(6, 0, 0, 0));

		JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(listPanel, BorderLayout.NORTH);

		JScrollPane listScroll = new JScrollPane(listWrapper);
		listScroll.setBorder(null);
		listScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listScroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listScroll.getVerticalScrollBar().setUnitIncrement(16);
		add(listScroll, BorderLayout.CENTER);
	}

	private static <E extends Enum<E>> EnumSet<E> collectSelected(Map<E, JCheckBox> checkboxes, Class<E> type)
	{
		EnumSet<E> selected = EnumSet.noneOf(type);
		for (Map.Entry<E, JCheckBox> e : checkboxes.entrySet())
		{
			if (e.getValue().isSelected())
			{
				selected.add(e.getKey());
			}
		}
		return selected;
	}

	private JCheckBox buildFilterCheckbox(String text)
	{
		JCheckBox cb = new JCheckBox(text);
		cb.setBackground(ColorScheme.DARK_GRAY_COLOR);
		cb.setForeground(ColorScheme.TEXT_COLOR);
		cb.setFocusPainted(false);
		cb.setFont(FontManager.getRunescapeSmallFont());
		cb.addActionListener(e -> rebuild());
		return cb;
	}

	private JButton buildMultiSelectDropdown(Map<?, JCheckBox> checkboxes)
	{
		JButton button = new JButton();
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(ColorScheme.TEXT_COLOR);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setFocusPainted(false);
		button.setMargin(new Insets(2, 6, 2, 6));

		JPopupMenu popup = new JPopupMenu();
		popup.setBackground(ColorScheme.DARK_GRAY_COLOR);
		popup.setBorder(new EmptyBorder(4, 4, 4, 4));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel toggles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		toggles.setBackground(ColorScheme.DARK_GRAY_COLOR);
		toggles.add(buildSelectionToggle("All", true, checkboxes));
		content.add(toggles);

		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 0));
		grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (JCheckBox cb : checkboxes.values())
		{
			grid.add(cb);
		}
		content.add(grid);
		popup.add(content);

		button.addActionListener(e -> popup.show(button, 0, button.getHeight()));

		Runnable updateText = () ->
		{
			int selectedCount = 0;
			StringBuilder names = new StringBuilder();
			for (JCheckBox cb : checkboxes.values())
			{
				if (cb.isSelected())
				{
					if (names.length() > 0)
					{
						names.append(", ");
					}
					names.append(cb.getText());
					selectedCount++;
				}
			}
			String text = (selectedCount == 0 || selectedCount == checkboxes.size())
				? "All"
				: names.toString();
			button.putClientProperty("fullText", text);
			applyEllipsis(button);
		};
		// ItemListener fires for both user clicks and programmatic setSelected (used by All/None toggles).
		for (JCheckBox cb : checkboxes.values())
		{
			cb.addItemListener(e -> updateText.run());
		}
		// Re-truncate when the button's width changes (e.g. when the panel first lays out).
		button.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				applyEllipsis(button);
			}
		});
		updateText.run();
		return button;
	}

	private static void applyEllipsis(JButton button)
	{
		String full = (String) button.getClientProperty("fullText");
		if (full == null)
		{
			return;
		}
		Insets ins = button.getInsets();
		int avail = button.getWidth() - ins.left - ins.right;
		if (avail <= 0)
		{
			button.setText(full);
			return;
		}
		FontMetrics fm = button.getFontMetrics(button.getFont());
		if (fm.stringWidth(full) <= avail)
		{
			button.setText(full);
			return;
		}
		String ellipsis = "...";
		int eW = fm.stringWidth(ellipsis);
		int len = full.length();
		while (len > 0 && fm.stringWidth(full.substring(0, len)) + eW > avail)
		{
			len--;
		}
		button.setText(full.substring(0, len) + ellipsis);
	}

	private static JPanel buildLabeledRow(String labelText, java.awt.Component field)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setAlignmentX(LEFT_ALIGNMENT);
		JLabel label = new JLabel(labelText);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(FontManager.getRunescapeSmallFont());
		row.add(label, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private JButton buildSelectionToggle(String text, boolean selectAll, Map<?, JCheckBox> checkboxes)
	{
		JButton b = new JButton(text);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		b.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		b.setMargin(new Insets(0, 0, 0, 0));
		b.setFocusPainted(false);
		b.addActionListener(e ->
		{
			for (JCheckBox cb : checkboxes.values())
			{
				cb.setSelected(selectAll);
			}
			rebuild();
		});
		return b;
	}

	private static JPanel verticalStrut(int height)
	{
		JPanel p = new JPanel();
		p.setPreferredSize(new Dimension(0, height));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}

	void rebuild()
	{
		// Keep config-bound widgets in sync (e.g. after edits from the RuneLite settings panel).
		if (completedCombo.getSelectedItem() != config.completedFilter())
		{
			completedCombo.setSelectedItem(config.completedFilter());
		}
		if (hideUnavailableCheckbox.isSelected() != config.hideUnavailable())
		{
			hideUnavailableCheckbox.setSelected(config.hideUnavailable());
		}

		listPanel.removeAll();
		currentRows.clear();

		String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
		EnumSet<LeagueTaskTier> selectedTiers = collectSelected(tierCheckboxes, LeagueTaskTier.class);
		EnumSet<LeagueTaskRegion> selectedRegions = collectSelected(regionCheckboxes, LeagueTaskRegion.class);
		CompletedFilter completedFilter = config.completedFilter();
		PactFilter pactFilter = (PactFilter) pactCombo.getSelectedItem();
		boolean hideUnavailable = config.hideUnavailable();

		int completedTotal = 0;
		int shown = 0;
		for (LeagueTask task : allTasks)
		{
			boolean complete = plugin.isComplete(task);
			if (complete)
			{
				completedTotal++;
			}

			if (!selectedTiers.isEmpty() && !selectedTiers.contains(task.getTier()))
			{
				continue;
			}
			if (!selectedRegions.isEmpty() && !selectedRegions.contains(task.getRegion()))
			{
				continue;
			}
			if (completedFilter == CompletedFilter.COMPLETED && !complete)
			{
				continue;
			}
			if (completedFilter == CompletedFilter.INCOMPLETE && complete)
			{
				continue;
			}
			if (pactFilter == PactFilter.PACT_ONLY && !task.isPactTask())
			{
				continue;
			}
			if (pactFilter == PactFilter.NON_PACT_ONLY && task.isPactTask())
			{
				continue;
			}
			if (hideUnavailable && !plugin.meetsSkillRequirements(task))
			{
				continue;
			}
			if (!query.isEmpty() && !task.getName().toLowerCase().contains(query))
			{
				continue;
			}

			Icon regionIcon = plugin.getRegionIcon(task.getRegion());
			LeagueTaskRow row = new LeagueTaskRow(task, complete, regionIcon, () -> plugin.toggleSelectedTask(task));
			row.setSelected(task.equals(plugin.getSelectedTask()));
			listPanel.add(row);
			currentRows.add(row);
			shown++;
		}

		if (shown == 0)
		{
			listPanel.add(buildEmptyState());
		}

		setProgressText(completedTotal, shown);

		listPanel.revalidate();
		listPanel.repaint();
	}

	private static JPanel buildEmptyState()
	{
		// Stretch to the listPanel's full width so the centered label actually centers
		// against the sidebar, not the label's own intrinsic width.
		JPanel empty = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		empty.setBackground(ColorScheme.DARK_GRAY_COLOR);
		empty.setBorder(new EmptyBorder(24, 8, 24, 8));
		empty.setAlignmentX(LEFT_ALIGNMENT);
		JLabel label = new JLabel("No tasks match your filters.", SwingConstants.CENTER);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(FontManager.getRunescapeSmallFont());
		empty.add(label, BorderLayout.CENTER);
		return empty;
	}

	private void setProgressText(int completed, int shown)
	{
		progressLabel.setText(String.format("%d / %d tasks completed · %d shown",
			completed, allTasks.size(), shown));
	}

	void updateSelection()
	{
		LeagueTask sel = plugin.getSelectedTask();
		for (LeagueTaskRow row : currentRows)
		{
			row.setSelected(row.getTask().equals(sel));
		}
	}

	void refreshCompletion()
	{
		// A completion flip can move a task in or out of the visible set when the completed filter is active.
		if (config.completedFilter() != CompletedFilter.ALL)
		{
			rebuild();
			return;
		}
		int completedTotal = 0;
		for (LeagueTask task : allTasks)
		{
			if (plugin.isComplete(task))
			{
				completedTotal++;
			}
		}
		for (LeagueTaskRow row : currentRows)
		{
			row.setCompleted(plugin.isComplete(row.getTask()));
		}
		setProgressText(completedTotal, currentRows.size());
		listPanel.repaint();
	}
}
