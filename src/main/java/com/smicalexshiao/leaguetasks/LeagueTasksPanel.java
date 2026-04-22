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
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

class LeagueTasksPanel extends PluginPanel
{
	private static final String ALL_REGIONS = "All regions";

	private final LeagueTasksPlugin plugin;
	private final LeagueTasksConfig config;
	private final List<LeagueTask> allTasks;

	private final IconTextField searchField;
	private final JComboBox<String> regionCombo;
	private final JCheckBox hideCompletedCheckbox;
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

		regionCombo = new JComboBox<>();
		regionCombo.addItem(ALL_REGIONS);
		for (LeagueTaskRegion r : LeagueTaskRegion.values())
		{
			regionCombo.addItem(r.getDisplayName());
		}
		regionCombo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		regionCombo.setFocusable(false);
		regionCombo.addActionListener(e -> rebuild());
		regionCombo.setAlignmentX(LEFT_ALIGNMENT);
		controls.add(regionCombo);
		controls.add(verticalStrut(4));

		hideCompletedCheckbox = new JCheckBox("Hide completed");
		hideCompletedCheckbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		hideCompletedCheckbox.setForeground(ColorScheme.TEXT_COLOR);
		hideCompletedCheckbox.setFocusPainted(false);
		hideCompletedCheckbox.setSelected(config.hideCompleted());
		hideCompletedCheckbox.addActionListener(e -> plugin.setHideCompleted(hideCompletedCheckbox.isSelected()));
		hideCompletedCheckbox.setAlignmentX(LEFT_ALIGNMENT);
		controls.add(hideCompletedCheckbox);
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
		// Keep checkboxes in sync with config (e.g. after config edit from settings panel).
		if (hideCompletedCheckbox.isSelected() != config.hideCompleted())
		{
			hideCompletedCheckbox.setSelected(config.hideCompleted());
		}
		if (hideUnavailableCheckbox.isSelected() != config.hideUnavailable())
		{
			hideUnavailableCheckbox.setSelected(config.hideUnavailable());
		}

		listPanel.removeAll();
		currentRows.clear();

		String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
		String selectedRegion = (String) regionCombo.getSelectedItem();
		boolean hideCompleted = config.hideCompleted();
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

			if (selectedRegion != null && !ALL_REGIONS.equals(selectedRegion)
				&& !selectedRegion.equals(task.getRegion().getDisplayName()))
			{
				continue;
			}
			if (hideCompleted && complete)
			{
				continue;
			}
			if (hideUnavailable && !plugin.meetsSkillRequirements(task))
			{
				continue;
			}
			if (!query.isEmpty() && !task.getName().toLowerCase().contains(query)
				&& !task.getDescription().toLowerCase().contains(query))
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

		setProgressText(completedTotal, shown);

		listPanel.revalidate();
		listPanel.repaint();
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
		// When hideCompleted is on, a completion flip can change the visible set, so rebuild.
		if (config.hideCompleted())
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
