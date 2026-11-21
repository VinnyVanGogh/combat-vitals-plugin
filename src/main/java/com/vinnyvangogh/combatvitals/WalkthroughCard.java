package com.vinnyvangogh.combatvitals;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The tour as one card at the top of the sidebar: step n of 7, a title, a
 * short text, and Back, Next and Skip. It points the panel at each step's tab
 * through {@code onStep} and says it is finished through {@code onClose},
 * whether by Skip or by Done.
 */
final class WalkthroughCard extends Card
{
	private final Consumer<Walkthrough.Step> onStep;
	private final Runnable onClose;
	private final JLabel count = new JLabel();
	private final JLabel title = new JLabel();
	private final WrapLabel body = new WrapLabel(" ", PanelStyle.TEXT);
	private final JButton back = VitalsSidePanel.linkButton("Back", "The previous step.");
	private final JButton next = VitalsSidePanel.linkButton("Next", "The next step.");
	private final JButton skip = VitalsSidePanel.linkButton("Skip", "Close the tour. Show guides plays it again.");
	private int index;

	WalkthroughCard(Consumer<Walkthrough.Step> onStep, Runnable onClose)
	{
		super(PanelStyle.CARD, PanelStyle.ACCENT, PanelStyle.M, PanelStyle.M);
		this.onStep = onStep;
		this.onClose = onClose;
		setName("walkthrough");
		count.setFont(PanelStyle.body());
		count.setForeground(PanelStyle.MUTED);
		title.setFont(PanelStyle.title());
		title.setForeground(PanelStyle.TEXT);
		body.setBorder(new EmptyBorder(PanelStyle.S, 0, PanelStyle.S, 0));
		back.setName("tour-back");
		next.setName("tour-next");
		skip.setName("tour-skip");
		next.setForeground(PanelStyle.ACCENT);
		back.addActionListener(e -> show(index - 1));
		next.addActionListener(e ->
		{
			if (index >= Walkthrough.STEPS.size() - 1)
			{
				close();
			}
			else
			{
				show(index + 1);
			}
		});
		skip.addActionListener(e -> close());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, PanelStyle.L, 0));
		buttons.setOpaque(false);
		buttons.add(back);
		buttons.add(next);
		final JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.add(buttons, BorderLayout.WEST);
		row.add(skip, BorderLayout.EAST);

		add(count);
		add(title);
		add(body);
		add(row);
		setVisible(false);
	}

	/** Opens the tour at a step. */
	void start(int step)
	{
		setVisible(true);
		show(step);
	}

	boolean isOpen()
	{
		return isVisible();
	}

	int step()
	{
		return index;
	}

	private void show(int step)
	{
		index = Math.max(0, Math.min(Walkthrough.STEPS.size() - 1, step));
		final Walkthrough.Step s = Walkthrough.STEPS.get(index);
		count.setText("Step " + (index + 1) + " of " + Walkthrough.STEPS.size());
		title.setText(s.title);
		body.setPlain(s.text);
		back.setVisible(index > 0);
		next.setText(index == Walkthrough.STEPS.size() - 1 ? "Done" : "Next");
		if (s.tab != null)
		{
			onStep.accept(s);
		}
		revalidate();
		repaint();
	}

	private void close()
	{
		setVisible(false);
		onClose.run();
	}
}
