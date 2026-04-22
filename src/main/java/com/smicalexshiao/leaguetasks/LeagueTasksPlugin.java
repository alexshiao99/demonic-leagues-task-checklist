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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Demonic Leagues Task Checklist",
	description = "Browse and track your Demonic Pacts leagues task completion from the sidebar.",
	tags = {"leagues", "tasks", "demonic", "pacts", "checklist"}
)
public class LeagueTasksPlugin extends Plugin
{
	// Varp IDs holding the LEAGUE_TASK_COMPLETED_0..61 bitfields. Not sequentially
	// numbered in VarPlayerID — they're split across four ranges. A task with id=N
	// lives in COMPLETED_VARPS[N / 32], bit (N % 32).
	private static final int[] COMPLETED_VARPS = new int[62];
	private static final Set<Integer> COMPLETED_VARP_SET;
	private static final Pattern SKILL_REQ_PATTERN;

	static
	{
		for (int i = 0; i <= 15; i++)
		{
			COMPLETED_VARPS[i] = VarPlayerID.LEAGUE_TASK_COMPLETED_0 + i;
		}
		for (int i = 0; i <= 27; i++)
		{
			COMPLETED_VARPS[16 + i] = VarPlayerID.LEAGUE_TASK_COMPLETED_16 + i;
		}
		for (int i = 0; i <= 3; i++)
		{
			COMPLETED_VARPS[44 + i] = VarPlayerID.LEAGUE_TASK_COMPLETED_44 + i;
		}
		for (int i = 0; i <= 13; i++)
		{
			COMPLETED_VARPS[48 + i] = VarPlayerID.LEAGUE_TASK_COMPLETED_48 + i;
		}

		Set<Integer> varpSet = new HashSet<>(COMPLETED_VARPS.length * 2);
		for (int v : COMPLETED_VARPS)
		{
			varpSet.add(v);
		}
		COMPLETED_VARP_SET = Collections.unmodifiableSet(varpSet);

		StringBuilder alt = new StringBuilder();
		for (Skill s : Skill.values())
		{
			if (alt.length() > 0)
			{
				alt.append('|');
			}
			alt.append(Pattern.quote(s.getName()));
		}
		SKILL_REQ_PATTERN = Pattern.compile("(" + alt + ")\\s+(\\d+)");
	}

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private LeagueTasksConfig config;

	@Inject
	private Gson gson;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LeagueTaskOverlay overlay;

	private static final int REGION_ICON_SIZE = 24;

	private List<LeagueTask> tasks = Collections.emptyList();
	private final Map<LeagueTaskRegion, ImageIcon> regionIcons = new EnumMap<>(LeagueTaskRegion.class);
	private LeagueTasksPanel panel;
	private NavigationButton navButton;
	private LeagueTask selectedTask;

	@Provides
	LeagueTasksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LeagueTasksConfig.class);
	}

	@Override
	protected void startUp()
	{
		tasks = loadTasks();
		log.debug("Loaded {} league tasks", tasks.size());

		loadRegionIcons();

		panel = new LeagueTasksPanel(this, config, tasks);
		SwingUtilities.invokeLater(panel::rebuild);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "checklist.png");
		navButton = NavigationButton.builder()
			.tooltip("League Tasks")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);
	}

	private void loadRegionIcons()
	{
		for (LeagueTaskRegion region : LeagueTaskRegion.values())
		{
			BufferedImage img = ImageUtil.loadImageResource(getClass(), region.getIconResource());
			if (img == null)
			{
				log.warn("Missing region icon resource {}", region.getIconResource());
				continue;
			}
			int targetHeight = REGION_ICON_SIZE;
			int targetWidth = Math.max(1, img.getWidth() * targetHeight / img.getHeight());
			Image scaled = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
			ImageIcon icon = new ImageIcon(scaled);
			regionIcons.put(region, icon);
		}
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		regionIcons.clear();
		panel = null;
		navButton = null;
		selectedTask = null;
	}

	ImageIcon getRegionIcon(LeagueTaskRegion region)
	{
		return regionIcons.get(region);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (!COMPLETED_VARP_SET.contains(event.getVarpId()))
		{
			return;
		}
		if (panel != null)
		{
			SwingUtilities.invokeLater(panel::refreshCompletion);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!LeagueTasksConfig.GROUP.equals(event.getGroup()) || panel == null)
		{
			return;
		}
		SwingUtilities.invokeLater(panel::rebuild);
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (panel != null && config.hideUnavailable())
		{
			SwingUtilities.invokeLater(panel::rebuild);
		}
	}

	LeagueTask getSelectedTask()
	{
		return selectedTask;
	}

	void toggleSelectedTask(LeagueTask task)
	{
		if (task != null && task.equals(selectedTask))
		{
			selectedTask = null;
		}
		else
		{
			selectedTask = task;
		}
		if (panel != null)
		{
			SwingUtilities.invokeLater(panel::updateSelection);
		}
	}

	void clearSelectedTask()
	{
		if (selectedTask == null)
		{
			return;
		}
		selectedTask = null;
		if (panel != null)
		{
			SwingUtilities.invokeLater(panel::updateSelection);
		}
	}

	int getRealSkillLevel(Skill skill)
	{
		return client.getRealSkillLevel(skill);
	}

	void setHideCompleted(boolean hide)
	{
		configManager.setConfiguration(LeagueTasksConfig.GROUP, "hideCompleted", hide);
	}

	void setHideUnavailable(boolean hide)
	{
		configManager.setConfiguration(LeagueTasksConfig.GROUP, "hideUnavailable", hide);
	}

	/**
	 * Whether the given task is marked complete in the player's varps.
	 * Must be called on the client thread.
	 */
	boolean isComplete(LeagueTask task)
	{
		int index = task.getId() / 32;
		int bit = task.getId() % 32;
		if (index < 0 || index >= COMPLETED_VARPS.length)
		{
			return false;
		}
		int value = client.getVarpValue(COMPLETED_VARPS[index]);
		return ((value >>> bit) & 1) == 1;
	}

	/**
	 * Whether the player's current real levels meet every parsed skill requirement.
	 * Non-skill requirements (quests, items, area unlocks) are not modeled — a task
	 * with no parseable skill requirements is always considered achievable here.
	 */
	boolean meetsSkillRequirements(LeagueTask task)
	{
		Map<Skill, Integer> reqs = task.getParsedSkillRequirements();
		if (reqs.isEmpty())
		{
			return true;
		}
		for (Map.Entry<Skill, Integer> e : reqs.entrySet())
		{
			if (client.getRealSkillLevel(e.getKey()) < e.getValue())
			{
				return false;
			}
		}
		return true;
	}

	private List<LeagueTask> loadTasks()
	{
		try (InputStream in = getClass().getResourceAsStream("tasks.json"))
		{
			if (in == null)
			{
				log.error("League tasks.json resource not found");
				return Collections.emptyList();
			}
			Type listType = new TypeToken<List<RawTask>>() {}.getType();
			List<RawTask> raw = gson.fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8), listType);
			List<LeagueTask> out = new ArrayList<>(raw.size());
			for (RawTask r : raw)
			{
				LeagueTaskTier tier;
				try
				{
					tier = LeagueTaskTier.valueOf(r.tier);
				}
				catch (IllegalArgumentException ex)
				{
					log.warn("Skipping task {} with unknown tier {}", r.id, r.tier);
					continue;
				}
				LeagueTaskRegion region = LeagueTaskRegion.fromJson(r.region);
				if (region == null)
				{
					log.warn("Skipping task {} with unknown region {}", r.id, r.region);
					continue;
				}
				String skillReqs = r.skillRequirements == null ? "" : r.skillRequirements;
				out.add(new LeagueTask(
					r.id,
					r.name,
					r.description,
					tier,
					region,
					skillReqs,
					r.otherRequirements == null ? "" : r.otherRequirements,
					r.pactTask,
					parseSkillRequirements(skillReqs)));
			}
			return Collections.unmodifiableList(out);
		}
		catch (IOException ex)
		{
			log.error("Failed to load league tasks", ex);
			return Collections.emptyList();
		}
	}

	private static Map<Skill, Integer> parseSkillRequirements(String raw)
	{
		if (raw == null || raw.isEmpty())
		{
			return Collections.emptyMap();
		}
		Map<Skill, Integer> out = new EnumMap<>(Skill.class);
		Matcher m = SKILL_REQ_PATTERN.matcher(raw);
		while (m.find())
		{
			Skill skill = skillByName(m.group(1));
			if (skill == null)
			{
				continue;
			}
			int level = Integer.parseInt(m.group(2));
			// A skill can appear twice in one string — keep the highest level.
			Integer existing = out.get(skill);
			if (existing == null || level > existing)
			{
				out.put(skill, level);
			}
		}
		return out.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(out);
	}

	private static Skill skillByName(String name)
	{
		for (Skill s : Skill.values())
		{
			if (s.getName().equals(name))
			{
				return s;
			}
		}
		return null;
	}

	private static final class RawTask
	{
		int id;
		String name;
		String description;
		String tier;
		String region;
		String skillRequirements;
		String otherRequirements;
		boolean pactTask;
	}
}
