package GUI;

import MarkData.MarkQuest;
import javafx.util.Pair;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

/**
 * GUI for Mark Tracker program
 *
 * Consists of two windows:
 *
 * 'Currently Tracking' window, and 'Add Quest' window
 *
 * Mrunibro, 2 - 8 - 2019
 */
public class MarkTrackerGUI {

    private JPanel mainPanel;
    private JPanel questLabelPanel;
    private JLabel questsActiveLabel;
    private JButton addQuestButton;
    private JLabel activeQuestCount;
    private JScrollPane activeQuestsScrollPane;
    private JPanel questScrollPaneViewport;

    private static JFrame frame;
    private static JFrame questFrame;
    private static MarkQuestSelectorGUI questSelector;

    private ArrayList<Pair<MarkQuest,JPanel>> activeQuests;

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {

            //QUEST SELECT WINDOW (starts as invisible)
            questFrame = new JFrame("RotMG Mark Quest Tracker (Quest Selection)");
            questSelector = new MarkQuestSelectorGUI(questFrame);
            questFrame.setContentPane(questSelector.getMainPanel());
            questFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            questFrame.setResizable(false);
            questFrame.pack();

            //MAIN WINDOW
            frame = new JFrame("RotMG Mark Quest Tracker");
            frame.setContentPane(new MarkTrackerGUI().mainPanel);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false); //no resizing allowed, cursed UI design
            frame.pack();
            frame.setVisible(true);

        });
    }

    private MarkTrackerGUI(){
        activeQuests = new ArrayList<>();

        //GUI INITIALIZATION
        addQuestButton.addActionListener(e -> {
            questFrame.setVisible(true);
        });

        questSelector.setMarkTrackerReference(this); //set up reference back to this object for passing selected Quests
        //(cannot pass 'this' in static context of Main

        //declare UI Elements
        questScrollPaneViewport.setLayout(new BoxLayout(questScrollPaneViewport, BoxLayout.Y_AXIS)); //vertical alignment on quests

        activeQuestsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        activeQuestsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        activeQuestsScrollPane.getVerticalScrollBar().setUnitIncrement(15); //scroll speed
        activeQuestsScrollPane.setPreferredSize(new Dimension(500, 400));
        activeQuestsScrollPane.setViewportView(questScrollPaneViewport);
    }

    /**
     * Add a MarkQuest to the list of currently tracking quests
     * @param q the MarkQuest to add
     */
    void addQuest(MarkQuest q) {
        //generate JPanel
        JPanel toAdd = new JPanel();
        toAdd.setLayout(new GridLayout(0, 2));
        //add 2 panels to this, one holding text, the other the marks and add button (divides space 50/50)

        //pane 1
        JPanel textPane = questSelector.genTextQuestInfoPane(q);

        //pane 2
        JPanel markAndButtonPane = new JPanel();
        markAndButtonPane.setLayout(new FlowLayout()); //for tight padding between marks and addBtn

        JPanel questReqDisplay = questSelector.createMarkPanel(q.getCompletionReq(), q.getTotalMarkAmount(), true);

        JButton addQuestButton = new JButton("Remove");
        addQuestButton.setToolTipText("Removes this quest from the list!");
        addQuestButton.addActionListener(e -> {
            removeQuest(q);
            questSelector.refreshQuests();
        }); //add quest to tracking list if clicked

        markAndButtonPane.add(questReqDisplay);
        markAndButtonPane.add(addQuestButton);

        toAdd.add(textPane, BorderLayout.NORTH);
        toAdd.add(markAndButtonPane);

        //add to list
        activeQuests.add(new Pair<>(q, toAdd));
        activeQuestCount.setText(activeQuests.size()+"");

        //add to UI
        updateQuestLayout(toAdd);
        frame.pack();
        frame.repaint();

    }

    /**
     * Updates quest layout, by adding the new JPanel
     *
     * Also handles invisible padding used in securing correct text placement staying at the bottom of the
     * internal Components Array.
     *
     * @param toAdd the panel to be added
     */
    private void updateQuestLayout(JPanel toAdd) {
        for(Component c : questScrollPaneViewport.getComponents()){
            if (c.getClass().equals(JPanel.class) && c.getName() != null && c.getName().equals("padding")){
                questScrollPaneViewport.remove(c);
            }
        }
        questScrollPaneViewport.add(toAdd);

        //adding invisible padding to the bottom, so that GridLayout doesn't stretch text placement to the middle when only 1 or 2 quests are displayed
        for (int i = 0; i < 2; i++){
            JPanel jp = new JPanel();
            jp.setBorder(new EmptyBorder(0,0,100,0));
            jp.setName("padding"); //name it to identify for removal in the future
            questScrollPaneViewport.add(jp);
        }
    }

    /**
     * Removes quest from GUI and internal list
     * @param q the MarkQuest to remove
     */
    private void removeQuest(MarkQuest q) {
        for (Pair<MarkQuest, JPanel> p : activeQuests){
            if (p.getKey().equals(q)){
                questScrollPaneViewport.remove(p.getValue());
                activeQuests.remove(p);
                activeQuestCount.setText(activeQuests.size()+"");
                frame.pack();
                frame.repaint();
                break;
            }
        }
    }

    /**
     * Check MarkQuest completion status and change button text if required
     * @param container the container holding the MarkQuest status
     */
    void updateRemoveButton(Container container) {
        boolean allDone = true;
        for (Component c: container.getComponents()){
            if (((JLabel) c).getIcon() != null){ //this JLabel is NOT either an empty "grid filler" or marked as "X"
                allDone = false; //not all marks are marked as done (or are grid fillers)
            }
        }
        //get the button belonging to this questPanel
        JButton theButton = null;
        Container parent = container.getParent(); //panel holding marks && button
        for (Component c : parent.getComponents()){ //should always be element 2 (out of 2) but future-proof
            if (c.getClass().equals(JButton.class)){
                theButton = ((JButton) c);
            }
        }
        assert  theButton != null;

        theButton.setText(allDone ? "Complete" : "Remove");
    }

    /**
     * get list of all active MarkQuests
     * @return array of active MarkQuests
     */
    MarkQuest[] getActiveQuests() {
        ArrayList<MarkQuest> arr = new ArrayList<>();
        for (Pair<MarkQuest, JPanel> p : activeQuests){
            arr.add(p.getKey());
        }
        return arr.toArray(new MarkQuest[]{});
    }
}
