package GUI;

import MarkData.MarkQuest;
import MarkData.MarkRequirement;
import MarkData.QuestLibrary;
import ResourceLoader.ResourceManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;

public class MarkQuestSelectorGUI {
    private JPanel mainPanel;
    private JTextField questSearchBox;
    private JButton doneButton; //Button is not set to visible in form (unused, UX reason)
    private JScrollPane availableQuestsPane;
    private JPanel scrollPanelViewport;
    private JCheckBox exclude;
    private JCheckBox showActiveCheckBox;
    private JLabel searchInfoPane;
    private JCheckBox excludeScoutCheckBox;

    private JFrame thisFrame;
    private QuestLibrary questLibrary;
    private ResourceManager resourceManager;
    private MarkQuest[] eligibleQuests;
    private SearchManager searchManager;

    private boolean includeActiveQuests = false;
    private boolean excludeScoutQuests = true;

    private MarkTrackerGUI markTracker = null;

    Container getMainPanel() {
        return mainPanel;

    }

    MarkQuestSelectorGUI(JFrame thisFrame){
        //global variable declarations
        this.thisFrame = thisFrame;
        questLibrary = QuestLibrary.getInstance();
        resourceManager = ResourceManager.getInstance();
        searchManager = new SearchManager();
        eligibleQuests = questLibrary.sortQuestByTier(questLibrary.getAllQuests(), false); //descending sort: epic quests > standard quests > (...)

        //UI element declarations
        scrollPanelViewport.setLayout(new BoxLayout(scrollPanelViewport, BoxLayout.Y_AXIS)); //vertical alignment on quests

        availableQuestsPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        availableQuestsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        availableQuestsPane.getVerticalScrollBar().setUnitIncrement(15); //scroll speed
        availableQuestsPane.setPreferredSize(new Dimension(500, 400));
        availableQuestsPane.setViewportView(scrollPanelViewport);


        //actionlistener declarations
        // For each key press, execute a search on the available quests for that term.
        questSearchBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                new Thread(() -> searchQuests(questSearchBox.getText())).start();
            }
        });
        doneButton.addActionListener(e -> { //invisible button currently (just close it 4hed)
            thisFrame.setVisible(false); //hide frame until use later
        });
        exclude.addActionListener(e -> {
            //update available quests if exclusive-search is triggered
            searchQuests(questSearchBox.getText());
        });
        showActiveCheckBox.addActionListener(e -> {
            includeActiveQuests = showActiveCheckBox.isSelected();
            refreshQuests();
        });
        excludeScoutCheckBox.addActionListener(e -> {
            excludeScoutQuests = excludeScoutCheckBox.isSelected();
            refreshQuests();
        });

        //finalize by drawing current quests.
        refreshQuests();
    }

    /**
     * Search for quests that match for the following text, using SearchManager
     * @param text the text to search for matches with
     */
    private void searchQuests(String text) {

        if (! text.trim().isEmpty()){
            eligibleQuests = searchManager.getEligibleQuests(text, !exclude.isSelected());
        } else {
            if (exclude.isSelected()){
                //exclusion search mode on empty string, return empty list
                eligibleQuests = new MarkQuest[]{};
            } else {
                //inclusive search mode on empty string, return all options
                eligibleQuests = questLibrary.sortQuestByTier(questLibrary.getAllQuests(), false);
            }
        }
        refreshQuests();

        if(text.toLowerCase().equals("mrunibro")){

            loadSecret();
        }
    }

    /**
     * Generates textual information of a MarkQuest
     *
     * This code is in a separate method for re-use purposes inn MarkTracker, when displaying a currently tracking quest
     *
     * The mark and button panes were excluded due to them fulfilling a different purpose in that class.
     *
     * @param q the MarkQuest to generate text for
     * @return a JPanel containing a display of the generated text.
     */
    JPanel genTextQuestInfoPane(MarkQuest q) {
        JPanel textPane = new JPanel();
        textPane.setLayout(new GridLayout(0, 2)); //for generous padding between type and name
        textPane.setBorder(new EmptyBorder(0,0,50,0)); //more UI-layout shoe-horning. Make text go just a bit more upward to counter GridLayout stretching.
        JLabel questType = new JLabel(q.getType());
        questType.setBorder(new EmptyBorder(0,30,0,0));
        JLabel questName = new JLabel(q.getName());

        textPane.add(questType);
        textPane.add(questName);

        return textPane;
    }

    /**
     * Generate a JPanel holding up to 8 marks as pictures. Interactability optional
     *
     * @param reqs The Mark Requirements that form this Quest
     * @param totalAmount the sum of all individual MarkRequirement amounts
     * @param interactiveMarks whether or not to make marks interactive.
     * @return JPanel holding marks as icons
     */
    JPanel createMarkPanel(MarkRequirement[] reqs, int totalAmount, boolean interactiveMarks) {
        JPanel toReturn = new JPanel();
        toReturn.setLayout(new GridLayout(2, 4));

        int requiredPadding = 8 - totalAmount; //adding padding to always fill gridlayout; looks better
        for (MarkRequirement req : reqs){
            //images
            ImageIcon icon = new ImageIcon(resourceManager.getSprite(req.getMarkType()));
            for (int i = 0; i < req.getAmount(); i++){
                JLabel l = new JLabel();
                l.setIcon(icon);
                l.setToolTipText(req.getMarkType());

                if (interactiveMarks){ //this is the MarkTrackerGUI : label is clickable for marking as completed.
                    addMarkToggle(l, icon);
                }

                toReturn.add(l);
            }
        }
        //padding on right side
        for (int i = 0; i < requiredPadding; i++){
            JLabel l = new JLabel();
            l.setBorder(new EmptyBorder(20, 20, 20, 20)); //invis labels of 40x40 to keep gridLayout same size when manipulating marks
            toReturn.add(l);
        }

        return toReturn;
    }

    /**
     * Remove all quests on the 'eligible quests' search results pane.
     *
     * Called before displaying an updated eligible quests list.
     */
    private void removeQuests(){
        for (Component c : scrollPanelViewport.getComponents()){
            scrollPanelViewport.remove(c);
        }
    }

    /**
     * Adds a mark toggle, the Mark JLabel can be marked as complete by clicking it.
     * This can be undone by clicking it again.
     *
     * Moved to external method for readability
     *
     * When 'undone' it restores to the supplied icon.
     *
     * @param l the JLabel to add an actionlistener to
     * @param icon the ImageIcon to restore the JLabel
     */
    private void addMarkToggle(JLabel l, ImageIcon icon){
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (l.getText().equals("X")){
                    //restore icon
                    l.setBorder(new EmptyBorder(0,0,0,0)); //reset border
                    l.setText("");
                    l.setIcon(icon);
                } else {
                    l.setIcon(null);
                    l.setText("X");
                    l.setBorder(new EmptyBorder(0, 10, 0, 10)); //lessen shrink of GridLayout if all marks are marked as 'X' (& invis marks of 40x40  were not present, e.g. 8 mark quest)
                    Font f = l.getFont();
                    l.setFont(new Font(f.getName(), Font.BOLD, 26));
                }
                markTracker.updateRemoveButton(l.getParent()); //change 'remove' button to 'complete' if all marks are set to X, and vice-versa
            }
        });
    }

    /**
     * Draws all elligible Quests using scrollPanelViewport variable.
     * These are drawn into the availableQuestsPane
     */
    private void drawQuests(MarkQuest[] eligibleQuests){
        for (MarkQuest q : eligibleQuests){
            JPanel questPane = new JPanel(); //(vertically aligned) panel containing the quest info
            questPane.setLayout(new GridLayout());
            //add 2 panels to this, one holding text, the other the marks and add button (divides space 50/50)

            //pane 1
            JPanel textPane = genTextQuestInfoPane(q);

            //pane 2
            JPanel markAndButtonPane = new JPanel();
            markAndButtonPane.setLayout(new FlowLayout()); //for tight padding between marks and addBtn

            JPanel questReqDisplay = createMarkPanel(q.getCompletionReq(), q.getTotalMarkAmount(), false);
            JButton addQuestButton = new JButton("Add");
            addQuestButton.setToolTipText("Add this quest to the list of currently tracking quests!");
            addQuestButton.addActionListener(e -> {
                addQuest(q);
                refreshQuests();
            }); //add quest to tracking list if clicked

            markAndButtonPane.add(questReqDisplay);
            markAndButtonPane.add(addQuestButton);

            questPane.add(textPane);
            questPane.add(markAndButtonPane);

            scrollPanelViewport.add(questPane);
        }

        //adding invisible padding to the bottom, so that GridLayout doesn't stretch text placement to the middle when only 1 or 2 quests are displayed
        for (int i = 0; i < 2; i++){
            JPanel jp = new JPanel();
            jp.setBorder(new EmptyBorder(0,0,100,0));
            scrollPanelViewport.add(jp);
        }
    }

    /**
     * refresh selectable quests in frame
     */
    void refreshQuests() {
        removeQuests();

        MarkQuest[] toShow = eligibleQuests;

        if (! includeActiveQuests) toShow = excludeActiveQuests(toShow);
        if (excludeScoutQuests) toShow = excludeScoutQuests(toShow);

        drawQuests(toShow);

        thisFrame.pack();
        thisFrame.repaint();
    }

    /**
     * creates sub-array from quests eligible for display by removing currently tracking quests
     * @param eligibleQuests list of eligible quests to remove actives from.
     * @return the difference of the set of eligible quests and set of tracking quests
     */
    private MarkQuest[] excludeActiveQuests(MarkQuest[] eligibleQuests) {
        if (markTracker == null){ //true on initialization
            return eligibleQuests;
        }
        MarkQuest[] active = markTracker.getActiveQuests();
        //from eligible remove active
        ArrayList<MarkQuest> eligible = new ArrayList<>(Arrays.asList(eligibleQuests));
        eligible.removeAll(Arrays.asList(active));

        return eligible.toArray(new MarkQuest[]{});

    }

    /**
     * creates sub-array from quests eligible for display by removing Scout type quests
     * @param eligibleQuests array of quests eligible to be displayed
     * @return sub-array excluding scout quests
     */
    private MarkQuest[] excludeScoutQuests(MarkQuest[] eligibleQuests){
        ArrayList<MarkQuest> toReturn = new ArrayList<>(Arrays.asList(eligibleQuests));
        for (MarkQuest m : eligibleQuests){
            if (m.getType().equals("Scout")){
                toReturn.remove(m);
            }
        }
        return toReturn.toArray(new MarkQuest[]{});
    }

    /**
     * Load secret dialogue box
     */
    private void loadSecret(){
        String message = "You expected an easter egg, but it was me, BEEO!\n\nHopefully you'll find some use out of this tool, it took about 24h to make :^)";
        String title = "Easter Egg";
        JOptionPane.showMessageDialog(thisFrame, message, title, JOptionPane.INFORMATION_MESSAGE, new ImageIcon(resourceManager.getSecretSprite()));
    }

    /**
     * Add a quest to the 'currently tracking' pane in MarkTrackerGUI
     * @param q the quest to add.
     */
    private void addQuest(MarkQuest q) {
        markTracker.addQuest(q);
    }

    /**
     * Set reference back to MarkTrackerGUI class. Value is effectively final.
     *
     * Is not final due to static context of class creation not allowing 'this'  to be passed as parameter in MarkTrackerGUI
     * @param markTrackerGUI the reference back to the MarkTrackerGUI class.
     */
    void setMarkTrackerReference(MarkTrackerGUI markTrackerGUI) {
        if (markTracker != null) {
            throw new UnsupportedOperationException("Changing reference to markTracker after initialization not allowed!");
        }
        markTracker = markTrackerGUI;
    }
}
