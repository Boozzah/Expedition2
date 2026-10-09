/*
 * TextPanel
 *
 * A stand in screen for every part of the game that doesn't have its own screen yet.
 * It is the console in a window: the game's text scrolls in the middle and the
 * answers to each question are buttons along the bottom.
 * As Nadia's screens get built, each one takes its part of the game away from here.
 *
 * Everything in here runs on the Event Dispatch Thread. SwingView makes sure of that.
 */
package expedition.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

class TextPanel extends JPanel {

    // the dark navy from the top of Nadia's menu background
    private static final Color BACKGROUND = new Color(0x030F1F);
    // the light blue of the menu button text
    private static final Color TEXT = new Color(0xA6CEE6);

    private final Consumer<Object> onAnswer;
    private final JTextArea log = new JTextArea();
    private final JLabel promptLabel = new JLabel(" ");
    private final JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));

    // onAnswer gets the value of whichever button the player clicks
    TextPanel(Consumer<Object> onAnswer) {
        super(new BorderLayout());
        this.onAnswer = onAnswer;
        setBackground(BACKGROUND);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        // monospaced so the crew table lines up the same as it does in the console
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        log.setBackground(BACKGROUND);
        log.setForeground(TEXT);
        log.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(null);

        promptLabel.setFont(promptLabel.getFont().deriveFont(Font.BOLD, 16f));
        promptLabel.setForeground(Color.WHITE);
        buttonRow.setOpaque(false);
        // Always tall enough for one row of buttons, even when it's empty. If it grew when
        // the buttons arrived, it would push the last line of text off the bottom of the screen
        int rowHeight = new JButton(" ").getPreferredSize().height + 10;
        buttonRow.setPreferredSize(new Dimension(0, rowHeight));

        JPanel inputPanel = new JPanel(new BorderLayout(0, 6));
        inputPanel.setBackground(BACKGROUND.brighter());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        inputPanel.add(promptLabel, BorderLayout.NORTH);
        inputPanel.add(buttonRow, BorderLayout.CENTER);

        add(scroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    // adds a line to the bottom and scrolls down to it
    void append(String line) {
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    // wipes the text, used when the player goes back to the menu so the next
    // game starts on a clean screen
    void clear() {
        log.setText("");
        clearInput();
    }

    // one button per option, each one answers with the value at the same index
    void showButtons(String prompt, List<String> labels, List<?> values) {
        promptLabel.setText(prompt.trim().isEmpty() ? " " : prompt.trim());
        buttonRow.removeAll();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            Object value = values.get(i);
            JButton button = new JButton(label);
            button.addActionListener(e -> answer(label, value));
            buttonRow.add(button);
        }
        buttonRow.revalidate();
        buttonRow.repaint();
    }

    // for a number question with too many answers to fit as a row of buttons
    void showSpinner(String prompt, int min, int max, Object backValue) {
        promptLabel.setText(prompt.trim() + " (" + min + "-" + max + ")");
        buttonRow.removeAll();

        // the spinner can't go outside min..max, so it can't give a wrong answer
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(min, min, max, 1));
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            int value = (Integer) spinner.getValue();
            answer(String.valueOf(value), value);
        });
        JButton back = new JButton("Back");
        back.addActionListener(e -> answer("Back", backValue));

        buttonRow.add(spinner);
        buttonRow.add(ok);
        buttonRow.add(back);
        buttonRow.revalidate();
        buttonRow.repaint();
    }

    // Shows the answer in the text like the console would, then hands it over.
    // The buttons go straight away so a double click can't answer twice
    private void answer(String label, Object value) {
        append("> " + label);
        clearInput();
        onAnswer.accept(value);
    }

    private void clearInput() {
        promptLabel.setText(" ");
        buttonRow.removeAll();
        buttonRow.revalidate();
        buttonRow.repaint();
    }
}
