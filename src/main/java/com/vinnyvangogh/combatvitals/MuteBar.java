package com.vinnyvangogh.combatvitals;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * The panel's mute control under the title: one button that mutes every alert
 * (or turns them back on), three that mute for 15, 30 or 60 minutes, and a
 * "Muted until hh:mm" line while it is on. It only shows and sets the
 * {@link AlertMute}; the plugin owns the state, so a rebuilt panel draws the
 * same. A one-second timer, running only while the panel is open in the
 * sidebar, redraws it when a timed mute runs out. Swing thread.
 */
final class MuteBar extends VitalsSidePanel.Column
{
	static final String MUTE = "Mute alerts";
	static final String UNMUTE = "Unmute alerts";

	private final AlertMute mute;
	private final JButton toggle = new JButton(MUTE);
	private final JButton[] timed = new JButton[AlertMute.MINUTES.length];
	private final JLabel status = new JLabel();
	private final Timer ticker = new Timer(1000, e -> refresh());

	MuteBar(AlertMute mute)
	{
		this.mute = mute;
		setBorder(new EmptyBorder(0, 0, PanelStyle.M, 0));

		toggle.setName("mute-toggle");
		toggle.setFont(PanelStyle.body());
		toggle.setFocusable(false);
		toggle.setToolTipText("Silences every alert of this plugin, sound and on-screen, until you turn it back on. "
			+ "Not kept across a restart.");
		toggle.addActionListener(e ->
		{
			if (mute.muted())
			{
				mute.unmute();
			}
			else
			{
				mute.muteUntilUnmuted();
			}
			refresh();
		});
		final JPanel toggleRow = new JPanel(new GridLayout(1, 1));
		toggleRow.setOpaque(false);
		toggleRow.add(toggle);
		add(toggleRow);

		final JPanel row = new JPanel(new GridLayout(1, timed.length, PanelStyle.S, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(PanelStyle.S, 0, 0, 0));
		for (int i = 0; i < timed.length; i++)
		{
			final int minutes = AlertMute.MINUTES[i];
			final JButton b = new JButton(minutes + " min");
			b.setName("mute-" + minutes);
			b.setFont(PanelStyle.body());
			b.setFocusable(false);
			b.setToolTipText("Mute every alert for " + minutes + " minutes; they turn back on by themselves.");
			b.addActionListener(e ->
			{
				mute.muteFor(minutes);
				refresh();
			});
			timed[i] = b;
			row.add(b);
		}
		add(row);

		status.setName("mute-status");
		status.setFont(PanelStyle.body());
		status.setForeground(PanelStyle.ACCENT);
		status.setBorder(new EmptyBorder(PanelStyle.S, 0, 0, 0));
		add(status);
		refresh();
	}

	/** Draws the mute's state: the button's wording and the "Muted until" line. */
	void refresh()
	{
		final String line = mute.status();
		toggle.setText(line == null ? MUTE : UNMUTE);
		status.setText(line == null ? " " : line);
		status.setVisible(line != null);
		revalidate();
		repaint();
	}

	/** The panel is open in the sidebar: draw now and keep watching for a timed mute to end. */
	void start()
	{
		refresh();
		ticker.start();
	}

	void stop()
	{
		ticker.stop();
	}
}
