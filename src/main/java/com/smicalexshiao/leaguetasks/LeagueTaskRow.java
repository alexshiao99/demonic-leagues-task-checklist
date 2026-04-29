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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class LeagueTaskRow extends JPanel
{
	private static final Color COMPLETED_BG = new Color(0x2A3A2A);
	private static final Color DEFAULT_BG = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color HOVER_BG = new Color(0x3B3B3B);
	private static final Color SELECTED_BG = new Color(0x3A322A);

	private static final Border DEFAULT_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
		new EmptyBorder(10, 8, 10, 8));
	private static final Border SELECTED_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 2, 1, 0, ColorScheme.BRAND_ORANGE),
		new EmptyBorder(10, 6, 10, 8));

	@Getter
	private final LeagueTask task;

	private final JLabel nameLabel;
	private final JLabel metaLabel;
	private final JLabel iconLabel;

	private boolean completed;
	private boolean selected;
	private boolean hovered;

	LeagueTaskRow(LeagueTask task, boolean completed, boolean pinned, Icon regionIcon, Runnable onClick, Runnable onPinToggle)
	{
		this.task = task;

		setLayout(new BorderLayout(8, 0));
		setBorder(DEFAULT_BORDER);
		setAlignmentX(LEFT_ALIGNMENT);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					onClick.run();
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				hovered = true;
				applyBackground();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hovered = false;
				applyBackground();
			}

			// isPopupTrigger fires on press for X11/Win and on release for Mac;
			// listening to both is the standard cross-platform idiom.
			private void maybeShowPopup(MouseEvent e)
			{
				if (!e.isPopupTrigger())
				{
					return;
				}
				JPopupMenu menu = new JPopupMenu();
				JMenuItem pinItem = new JMenuItem(pinned ? "Unpin" : "Pin");
				pinItem.addActionListener(ev -> onPinToggle.run());
				menu.add(pinItem);
				menu.show(LeagueTaskRow.this, e.getX(), e.getY());
			}
		});

		iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(24, 24));
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		iconLabel.setVerticalAlignment(SwingConstants.CENTER);
		if (regionIcon != null)
		{
			iconLabel.setIcon(regionIcon);
		}

		JPanel textPanel = new JPanel(new GridBagLayout());
		textPanel.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(0, 0, 3, 0);

		nameLabel = new JLabel();
		nameLabel.setFont(FontManager.getRunescapeFont());
		textPanel.add(nameLabel, c);

		metaLabel = new JLabel();
		metaLabel.setFont(FontManager.getRunescapeSmallFont());
		metaLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		c.gridy = 1;
		c.insets = new Insets(0, 0, 0, 0);
		textPanel.add(metaLabel, c);

		add(iconLabel, BorderLayout.WEST);
		add(textPanel, BorderLayout.CENTER);

		// The location icon doubles as a pin button: hovering swaps it to a star,
		// clicking it toggles the pin. Click events delivered to the iconLabel don't
		// bubble to the row's selection handler, so this doesn't change task selection.
		final Icon defaultIcon = regionIcon;
		final Icon hoverIcon = new StarIcon(20, pinned, pinned ? new Color(0xFFD080) : ColorScheme.LIGHT_GRAY_COLOR);
		iconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		iconLabel.setToolTipText(pinned ? "Unpin task" : "Pin task");
		iconLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					onPinToggle.run();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				iconLabel.setIcon(hoverIcon);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (defaultIcon != null)
				{
					iconLabel.setIcon(defaultIcon);
				}
				else
				{
					iconLabel.setIcon(null);
				}
			}
		});

		StringBuilder tooltip = new StringBuilder("<html><body style='width:250px'>");
		tooltip.append("<b>").append(escape(task.getName())).append("</b>");
		if (task.isPactTask())
		{
			tooltip.append(" <span style='color:#FFD080'>[Demonic Pact]</span>");
		}
		tooltip.append("<br>").append(escape(task.getDescription()));
		if (!task.getSkillRequirements().isEmpty())
		{
			tooltip.append("<br><br><b>Skills:</b> ").append(escape(task.getSkillRequirements()));
		}
		if (!task.getOtherRequirements().isEmpty())
		{
			tooltip.append("<br><b>Other:</b> ").append(escape(task.getOtherRequirements()));
		}
		tooltip.append("</body></html>");
		setToolTipText(tooltip.toString());

		setCompleted(completed);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	// Pin/star icon drawn via Java2D — no font glyph dependency, always renders.
	private static final class StarIcon implements Icon
	{
		private final int size;
		private final boolean filled;
		private final Color color;

		StarIcon(int size, boolean filled, Color color)
		{
			this.size = size;
			this.filled = filled;
			this.color = color;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			try
			{
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int outerR = size / 2 - 1;
				int innerR = outerR / 2;
				int cx = x + size / 2;
				int cy = y + size / 2;
				int[] xs = new int[10];
				int[] ys = new int[10];
				for (int i = 0; i < 10; i++)
				{
					double angle = -Math.PI / 2 + i * Math.PI / 5;
					int r = (i % 2 == 0) ? outerR : innerR;
					xs[i] = cx + (int) Math.round(r * Math.cos(angle));
					ys[i] = cy + (int) Math.round(r * Math.sin(angle));
				}
				g2.setColor(color);
				if (filled)
				{
					g2.fillPolygon(xs, ys, 10);
				}
				else
				{
					g2.setStroke(new BasicStroke(1.5f));
					g2.drawPolygon(xs, ys, 10);
				}
			}
			finally
			{
				g2.dispose();
			}
		}

		@Override
		public int getIconWidth()
		{
			return size;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}
	}

	void setCompleted(boolean completed)
	{
		this.completed = completed;

		Color nameColor = completed ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.TEXT_COLOR;
		String name = truncate(task.getName(), 28);
		if (completed)
		{
			nameLabel.setText("<html><span style='color:#37F046'>✔</span> " + escape(name) + "</html>");
		}
		else
		{
			nameLabel.setText(name);
		}
		nameLabel.setForeground(nameColor);

		String meta = task.getTier().getDisplayName() + " · " + task.getRegion().getDisplayName();
		if (task.isPactTask())
		{
			meta = "★ " + meta;
		}
		metaLabel.setText(meta);
		metaLabel.setForeground(task.getTier().getColor());

		applyBackground();
	}

	void setSelected(boolean selected)
	{
		if (this.selected == selected)
		{
			return;
		}
		this.selected = selected;
		setBorder(selected ? SELECTED_BORDER : DEFAULT_BORDER);
		applyBackground();
	}

	private void applyBackground()
	{
		Color bg;
		if (selected)
		{
			bg = SELECTED_BG;
		}
		else if (hovered)
		{
			bg = HOVER_BG;
		}
		else if (completed)
		{
			bg = COMPLETED_BG;
		}
		else
		{
			bg = DEFAULT_BG;
		}
		setBackground(bg);
	}

	private static String truncate(String s, int max)
	{
		if (s.length() <= max)
		{
			return s;
		}
		return s.substring(0, max - 1) + "…";
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
