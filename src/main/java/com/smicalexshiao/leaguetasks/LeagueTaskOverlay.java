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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class LeagueTaskOverlay extends OverlayPanel
{
	private static final Color COMPLETE_COLOR = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color INCOMPLETE_COLOR = new Color(230, 150, 30);
	private static final Color REQ_MET = new Color(120, 210, 120);
	private static final Color REQ_MISSING = new Color(230, 90, 90);
	private static final Color PACT_COLOR = new Color(0xFFD080);
	private static final int PANEL_WIDTH = 220;

	private final LeagueTasksPlugin plugin;

	@Inject
	private LeagueTaskOverlay(LeagueTasksPlugin plugin)
	{
		super(plugin);
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_LOW);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "League task overlay");
		addMenuEntry(RUNELITE_OVERLAY, "Clear", "League task overlay", e -> plugin.clearSelectedTask());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		LeagueTask task = plugin.getSelectedTask();
		if (task == null)
		{
			return null;
		}

		panelComponent.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Current Task")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(task.getName())
			.leftColor(task.getTier().getColor())
			.build());

		boolean complete = plugin.isComplete(task);
		panelComponent.getChildren().add(LineComponent.builder()
			.left(task.getTier().getDisplayName() + " · " + task.getRegion().getDisplayName())
			.right(complete ? "Complete" : "Incomplete")
			.rightColor(complete ? COMPLETE_COLOR : INCOMPLETE_COLOR)
			.build());

		if (task.isPactTask())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Demonic Pact task")
				.leftColor(PACT_COLOR)
				.build());
		}

		panelComponent.getChildren().add(LineComponent.builder().build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(task.getDescription())
			.build());

		Map<Skill, Integer> reqs = task.getParsedSkillRequirements();
		if (!reqs.isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder().build());
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Skill requirements")
				.build());
			for (Map.Entry<Skill, Integer> e : reqs.entrySet())
			{
				int have = plugin.getRealSkillLevel(e.getKey());
				boolean met = have >= e.getValue();
				panelComponent.getChildren().add(LineComponent.builder()
					.left(e.getKey().getName())
					.right(have + " / " + e.getValue())
					.leftColor(met ? REQ_MET : REQ_MISSING)
					.rightColor(met ? REQ_MET : REQ_MISSING)
					.build());
			}
		}

		if (!task.getOtherRequirements().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder().build());
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Other requirements")
				.build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left(task.getOtherRequirements())
				.build());
		}

		return super.render(graphics);
	}
}
