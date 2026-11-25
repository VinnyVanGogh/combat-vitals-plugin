package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.ui.ColorScheme;

/**
 * The skilling badge's Clean style: label and value rows on the same card as
 * the combat card, no sentences. Pure, so the layout is tested without a client.
 * The caller dims the whole card while {@link SkillingReadout#isIdle()}.
 */
final class SkillingCardRows
{
	private static final Color PLAIN = SplashOverlay.PLAIN;
	private static final Color WARN = SplashOverlay.WARN;

	private SkillingCardRows()
	{
	}

	/** The card's rows, top to bottom: header, level, xp, inventory, session stats, missing gear. */
	static List<SplashOverlay.Row> rows(SkillingReadout r)
	{
		final List<SplashOverlay.Row> rows = new ArrayList<>();
		final String name = SkillingText.headerName(r.getSkill(), r.getTrainingMethod());
		rows.add(SplashOverlay.Row.header(name, r.isIdle() ? SkillingText.idleHeader(r.getSkill(), r.getTrainingMethod())
			: "Training", r.getColour()));
		if (r.isHeader())
		{
			rows.add(SplashOverlay.Row.pair("Level", level(r), r.isBoosted() ? SplashOverlay.SAFE : PLAIN));
		}
		if (r.getXpText() != null)
		{
			rows.add(SplashOverlay.Row.pair("XP", r.getXpText(), PLAIN));
		}
		if (r.getInventoryLabel() != null)
		{
			rows.add(SplashOverlay.Row.pair("Inventory", r.getInventoryLabel(), PLAIN));
		}
		for (SessionRow.Cell cell : r.getSessionCells())
		{
			rows.add(SplashOverlay.Row.text(cell.getText(), SessionRow.tint(r.getColour(), cell.getKind()), false));
		}
		if (r.getGearText() != null)
		{
			rows.add(SplashOverlay.Row.text(r.getGearText(), WARN, true));
		}
		return rows;
	}

	/** "73" or "78 (73)" while boosted. */
	static String level(SkillingReadout r)
	{
		return r.isBoosted() ? r.getBoosted() + " (" + r.getReal() + ")" : String.valueOf(r.getReal());
	}

	/** The card's accent: muted once stopped, the skill's colour while at it. */
	static Color accent(SkillingReadout r)
	{
		return r.isIdle() ? ColorScheme.MEDIUM_GRAY_COLOR : r.getColour();
	}
}
