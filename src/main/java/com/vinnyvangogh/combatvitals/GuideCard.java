package com.vinnyvangogh.combatvitals;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * A tab's guide as a boxed card (Board, 2026-11-14: "make the guides more
 * obvious ... expanded and bigger, maybe in a box"): the orange accent bar, a
 * bold title after an info icon, what the tab does and how to use it as two
 * short paragraphs, then the walkthrough as a numbered list. Every line wraps
 * at the card's width ({@link WrapLabel}).
 *
 * <p>Open until the user folds it; {@link VitalsSidePanel} remembers which
 * guides were folded and whether guides are hidden altogether.
 */
final class GuideCard extends FoldCard
{
	/** Which guide this is, as remembered: "PRESETS" or a tab's name. */
	final String id;

	GuideCard(String id, String title, PanelGuide.Page page)
	{
		super(title, new InfoIcon(), PanelStyle.ACCENT, true);
		this.id = id;
		setName("guide-" + id);
		body.add(paragraph(page.what, PanelStyle.TEXT));
		body.add(paragraph(page.how, PanelStyle.SOFT));
		for (int i = 0; i < page.walkthrough.size(); i++)
		{
			final WrapLabel step = new WrapLabel(page.walkthrough.get(i), PanelStyle.TEXT);
			step.setBorder(new StepBorder(i + 1));
			body.add(step);
		}
	}

	private static WrapLabel paragraph(String text, java.awt.Color colour)
	{
		final WrapLabel p = new WrapLabel(text, colour);
		p.setBorder(new EmptyBorder(0, 0, PanelStyle.M, 0));
		return p;
	}

	/** A round orange "i" before the guide's title. */
	static final class InfoIcon implements Icon
	{
		private static final int SIZE = 12;

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			final Graphics2D g2 = PanelStyle.smooth(g);
			try
			{
				g2.setColor(PanelStyle.ACCENT);
				g2.fillOval(x, y, SIZE, SIZE);
				g2.setColor(PanelStyle.CARD);
				g2.fillRect(x + SIZE / 2 - 1, y + 5, 2, 5);
				g2.fillRect(x + SIZE / 2 - 1, y + 2, 2, 2);
			}
			finally
			{
				g2.dispose();
			}
		}

		@Override
		public int getIconWidth()
		{
			return SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return SIZE;
		}
	}

	/**
	 * A walkthrough step's number, drawn in a small round badge in the label's
	 * left margin, so the step's text wraps beside it rather than under it.
	 */
	static final class StepBorder implements Border
	{
		private static final int BADGE = 12;
		private final String number;

		StepBorder(int number)
		{
			this.number = String.valueOf(number);
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
		{
			final Graphics2D g2 = PanelStyle.smooth(g);
			try
			{
				g2.setColor(PanelStyle.INSET);
				g2.fillOval(x, y + 1, BADGE, BADGE);
				g2.setFont(PanelStyle.body());
				g2.setColor(PanelStyle.ACCENT);
				final FontMetrics m = g2.getFontMetrics();
				g2.drawString(number, x + (BADGE - m.stringWidth(number)) / 2,
					y + 1 + (BADGE - m.getHeight()) / 2 + m.getAscent());
			}
			finally
			{
				g2.dispose();
			}
		}

		@Override
		public Insets getBorderInsets(Component c)
		{
			return new Insets(1, BADGE + PanelStyle.S, PanelStyle.S, 0);
		}

		@Override
		public boolean isBorderOpaque()
		{
			return false;
		}
	}
}
