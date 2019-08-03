package MarkData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import ResourceLoader.*;
import javafx.util.Pair;

/**
 * "Library" Holding all Available quests from the Tinkerer
 *
 * Data taken from https://www.realmeye.com/wiki/the-tinkerer
 *
 * The QuestLibrary also provides functions for searching for a specific quest.
 */
public class QuestLibrary {

    private final MarkQuest[] allQuests; //array of all quests available
    private final ResourceManager resourceManager;
    private static QuestLibrary singleton = new QuestLibrary();

    private QuestLibrary(){
        resourceManager = ResourceManager.getInstance();
        allQuests = resourceManager.loadAllQuests();
    }

    public static QuestLibrary getInstance(){
        return singleton;
    }

    /**
     * Filtering method for quests, filters by name.
     *
     * This filtering method differs slightly from Dungeon and Type filters,
     * by searching for each word and sorting for the highest amount of hits,
     * rather than a 1:1 match.
     *
     * This method assumes the nameFilter has already been cleaned up (remove [?!'.], lowercase, split in words, etc.
     *
     * @param nameFilter array of all words in this filter.
     * @param includeFilter to returnn all types within the filter, or not within filter.
     * @return all quests (not) in the filter.
     */
    public MarkQuest[] filterQuestOfName(String[] nameFilter, boolean includeFilter){
        ArrayList<MarkQuest> tempQuestList = new ArrayList<>();
        ArrayList<Pair<MarkQuest, Integer>> filterMatches = new ArrayList<>();

        for (MarkQuest q : allQuests) {
            String questName = q.getName()
                    .replaceAll("['!?. ]","") //remove '?', '!', '.' ' ' and apostrophe from quest name.
                    .toLowerCase();

            int hits = 0;
            for (String searchWord : nameFilter){ //for each search word,
                if (questName.contains(searchWord)) hits++; //see if the name has it, and add to total
            }
            if (includeFilter){
                if (hits > 0) filterMatches.add(new Pair<>(q, hits)); //for sorting, later
            } else { //exclude filter
                if (hits == 0) tempQuestList.add(q);
            }
        }

        if (includeFilter) { //want to sort pairs by hits
            Comparator<Pair<MarkQuest, Integer>> byHits =
                    Comparator.comparing(Pair::getValue); //first time I get to use '::' in the wild, OwO
            filterMatches.sort(byHits);

            for (int i = filterMatches.size() - 1; i >= 0; i--){ //descending order
                Pair<MarkQuest, Integer> p = filterMatches.get(i);
                tempQuestList.add(p.getKey());
            }
        }

        return tempQuestList.toArray(new MarkQuest[]{});
    }

    /**
     * Filtering method for quests, filters by type
     *
     * @param includeFilter to return all types within filter, or not within filter
     * @param typeFilter array of all types in this filter
     * @return all quests (not) in the filter
     */
    public MarkQuest[] filterQuestOfType(String[] typeFilter, boolean includeFilter){
        for (String a : typeFilter){
           resourceManager.assertValidType(a);
        }

        ArrayList<MarkQuest> tempQuestList = new ArrayList<>();
        for (MarkQuest q : allQuests){
            String type = q.getType();

            boolean inFilter = false;

            for (String str : typeFilter){
                if (type.equals(str)){
                    inFilter = true;
                }
            }

            if (includeFilter) {
                if (inFilter) tempQuestList.add(q); //add only if on filter, inclusion mode
            } else {
                if (! inFilter) tempQuestList.add(q); //add only if not on filter, exclusion mode
            }
        }

        return tempQuestList.toArray(new MarkQuest[]{});
    }

    /**
     * Filtering method for quests, filters by dungeon.
     *
     * This method assumes human words have been converted into a dungeon the program understands before calling (e.g. abyss --> AbyssOfDemons)
     *
     * @param dungeonFilter array of all dungeons in this filter
     * @param includeFilter to return all types within the filter, or not within filter
     * @return all quests (not) inn the filter
     */
    public MarkQuest[] filterQuestOfDungeon(String[] dungeonFilter, boolean includeFilter){
        for (String a: dungeonFilter){
            resourceManager.assertValidDungeon(a);
        }

        ArrayList<MarkQuest> tempQuestList = new ArrayList<>();
        ArrayList<Pair<MarkQuest, Integer>> filterMatches = new ArrayList<>();
        for (MarkQuest q : allQuests) {
            //get all dungeons a quest is a member of
            MarkRequirement[] questDungeons = q.getCompletionReq();
            String[] dungeons = new String[questDungeons.length];

            for (int i = 0; i < questDungeons.length; i++){
                dungeons[i] = questDungeons[i].getMarkType();
            }

            boolean filterHit = false;
            //check if any of those dungeons is within the filter
            for (String dungeon : dungeons) {
                for (String str : dungeonFilter) {
                    if (dungeon.equals(str)) {
                        filterHit = true;
                        break;
                    }
                } 
            }
            //we now know how many times this MQ is in the filter
            if (includeFilter){
                if (filterHit) {
                    tempQuestList.add(q);
                }
            } else { //excludeFilter
                if (! filterHit) tempQuestList.add(q); //add if no member of this quest in dungeonFilter
            }
        }

        return tempQuestList.toArray(new MarkQuest[]{});
    }

    /**
     * Sorts a given Array of MarkQuests by tier, ascending or descending
     *
     * Using insertionSort for a sorting algorithm.
     *
     * @param quests lists of quests to sort by tier
     * @param ascending sort ascending Y/N
     * @return sorted MarkQuest[] by tier
     */
    public MarkQuest[] sortQuestByTier(MarkQuest[] quests, boolean ascending){
        MarkQuest[] toReturn = new MarkQuest[quests.length];
        ArrayList<MarkQuest> questsList = new ArrayList<>(Arrays.asList(quests));

        for (int i = 0; i < toReturn.length; i++){
            int highestTier = Integer.MIN_VALUE;
            MarkQuest nextUP = null;
            for (MarkQuest q : questsList){

                String type = q.getType();
                int tier = resourceManager.getTypeTier(type);
                if (tier > highestTier){
                    highestTier = tier;
                    nextUP = q;
                }
            }
            assert nextUP != null;
            questsList.remove(nextUP);
            toReturn[i] = nextUP;
        }
        //array is now sorted in descending order
        //because im lazy and fuck performance for an array of ~30, if ascending just return the reversed array.
        if (! ascending){
            return toReturn;
        } else {
            MarkQuest[] reversed = new MarkQuest[toReturn.length];
            for (int i = toReturn.length - 1; i >= 0; i--){
                reversed[toReturn.length - 1 - i] = toReturn[i];
                reversed[toReturn.length - 1 - i] = toReturn[i];
            }
            return reversed;
        }
    }

    /**
     * @return array holding all MarkQuests in the library
     */
    public MarkQuest[] getAllQuests() {
        return allQuests;
    }

    /**
     * Used to keep MarkQuest Class package-private
     *
     * @param type the type of the MarkQuest ("Scout", "Epic", etc.)
     * @param name the name of the MQ ("Scout the Abyss", "King Who?")
     * @param req the requirements of completing the MQ, as MarkRequirement[]
     * @return a new MarkQuest Object.
     */
    public static MarkQuest createMarkQuest(String type, String name, MarkRequirement[] req){
        return new MarkQuest(type, name, req);
    }

    /**
     * Used to keep the MarkRequirement Class package-private
     *
     * @param dungeon the name of the dungeonn this mark is from
     * @param amount the amount of times this mark must be collected.
     * @return a new MarkRequirement Object.
     */
    public static MarkRequirement createMarkRequirement(String dungeon, int amount){
        return new MarkRequirement(dungeon, amount);
    }
}
