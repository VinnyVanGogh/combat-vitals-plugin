package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * A {@link Card} that folds from its title: the title row is a button, with a
 * chevron at its right edge pointing down while open and right while folded.
 * GIM Utilities' {@code GroupSettingsTab.collapsible} is the pattern: one
 * card per section, the body hidden rather than removed.
 */
class FoldCard extends Card
{
	final Header header;
	final VitalsSidePanel.Column body = new VitalsSidePanel.Column();
	private Consumer<Boolean> onToggle;

	FoldCard(String title, Icon icon, Color accent, boolean open)
	{
		super(PanelStyle.CARD, accent, PanelStyle.M, PanelStyle.M);
		header = new Header(title, icon);
		header.addActionListener(e ->
		{
			setOpen(!isOpen());
			if (onToggle != null)
			{
				onToggle.accept(isOpen());
			}
		});
		add(header);
		body.setBorder(new EmptyBorder(PanelStyle.S, 0, 0, 0));
		add(body);
		body.setVisible(open);
		header.showOpen(open);
	}

	/** Called with the new state after the user folds or opens the card; not for {@link #setOpen}. */
	void onToggle(Consumer<Boolean> listener)
	{
		onToggle = listener;
	}

	boolean isOpen()
	{
		return body.isVisible();
	}

	void setOpen(boolean open)
	{
		header.showOpen(open);
		if (body.isVisible() != open)
		{
			body.setVisible(open);
			revalidate();
			repaint();
		}
	}

	/** The title row: bold, left-aligned, as wide as the card, with the fold chevron drawn at the right. */
	static final class Header extends JButton
	{
		private static final int CHEVRON = 7;
		private boolean open;

		Header(String text, Icon icon)
		{
			super(text, icon);
			setFont(PanelStyle.title());
			setForeground(PanelStyle.TEXT);
			setFocusable(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setOpaque(false);
			setHorizontalAlignment(AbstractButton.LEFT);
			setIconTextGap(PanelStyle.S);
			setBorder(new EmptyBorder(0, 0, 0, CHEVRON + PanelStyle.S));
			setToolTipText("Fold or open " + text + ".");
		}

		boolean isOpen()
		{
			return open;
		}

		/** Points the chevron down (open) or right (folded); what it folds is the caller's. */
		void showOpen(boolean open)
		{
			this.open = open;
			repaint();
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			final Graphics2D g2 = PanelStyle.smooth(g);
			try
			{
				final int x = getWidth() - CHEVRON - 1;
				final int y = getHeight() / 2;
				final Polygon p = new Polygon();
				if (open)
				{
					p.addPoint(x, y - 2);
					p.addPoint(x + CHEVRON, y - 2);
					p.addPoint(x + CHEVRON / 2, y + 2);
				}
				else
				{
					p.addPoint(x + 1, y - 4);
					p.addPoint(x + 5, y);
					p.addPoint(x + 1, y + 4);
				}
				g2.setColor(PanelStyle.MUTED);
				g2.fill(p);
			}
			finally
			{
				g2.dispose();
			}
		}
	}
}
