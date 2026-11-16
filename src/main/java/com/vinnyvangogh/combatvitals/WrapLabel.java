package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Text that wraps at the width it is given, in the panel's small font.
 *
 * <p>A fixed CSS {@code <body style='width:170px'>} clipped guide text at the
 * panel's right edge (Board, 2026-11-14): Swing's HTML does not map CSS
 * pixels one to one onto the panel's. This asks the label's HTML view for its height at the
 * parent's real width instead, and before the first layout at a width no
 * wider than the narrowest place the panel puts text, so a first guess is
 * never wider than the panel.
 */
final class WrapLabel extends JLabel
{
	/** Inside PluginPanel's 10 px borders and a card's padding and accent bar, with room to spare. */
	static final int FIRST_WIDTH = PluginPanel.PANEL_WIDTH - 2 * 10 - 2 * PanelStyle.M - Card.ACCENT_WIDTH - 12;

	private String plain;

	WrapLabel(String text, Color colour)
	{
		setFont(FontManager.getRunescapeSmallFont());
		setForeground(colour);
		setBorder(new EmptyBorder(1, 0, 1, 0));
		setPlain(text);
	}

	/** Sets the text; false when it is already showing, so nothing is redrawn. */
	boolean setPlain(String text)
	{
		if (text.equals(plain))
		{
			return false;
		}
		plain = text;
		setText("<html>" + VitalsSidePanel.escape(text) + "</html>");
		return true;
	}

	String plain()
	{
		return plain;
	}

	/** The width the parent lays its children out in, less this label's own border. */
	int wrapWidth()
	{
		final Insets own = getInsets();
		final Container parent = getParent();
		int width = FIRST_WIDTH;
		if (parent != null && parent.getWidth() > 0)
		{
			final Insets in = parent.getInsets();
			width = parent.getWidth() - in.left - in.right;
		}
		return Math.max(1, width - own.left - own.right);
	}

	@Override
	public Dimension getPreferredSize()
	{
		final View view = (View) getClientProperty(BasicHTML.propertyKey);
		if (view == null || isPreferredSizeSet())
		{
			return super.getPreferredSize();
		}
		final Insets own = getInsets();
		final int width = wrapWidth();
		final int natural = (int) Math.ceil(view.getPreferredSpan(View.X_AXIS));
		view.setSize(width, 0);
		final int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
		return new Dimension(Math.min(width, natural) + own.left + own.right, height + own.top + own.bottom);
	}

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	@Override
	public Dimension getMaximumSize()
	{
		return getPreferredSize();
	}
}
