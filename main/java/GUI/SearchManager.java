package GUI;

import MarkData.MarkQuest;
import MarkData.QuestLibrary;
import ResourceLoader.ResourceManager;
import javafx.util.Pair;

import java.util.*;

/**
 * Helper class of MarkQuestSelectorGUI
 *
 * uses QuestLibrary and ResourceManager to turn user search string into suggested quests,
 * Sorted by relevancy and Tier.
 */
class SearchManager {

    private ResourceManager resourceManager;
    private QuestLibrary questLibrary;

    SearchManager(){
        resourceManager = ResourceManager.getInstance();
        questLibrary = QuestLibrary.getInstance();
    }

    MarkQuest[] getEligibleQuests(String text, boolean include){
        String[] words = sanitizeInput(text);

        String[] typeFilter = getTypeFilter(words);
        String[] dungeonFilter = getDungeonFilter(words);

        MarkQuest[] typeHits = questLibrary.filterQuestOfType(typeFilter, include);
        typeHits = questLibrary.sortQuestByTier(typeHits, false);

        MarkQuest[] dungeonHits = questLibrary.filterQuestOfDungeon(dungeonFilter, include);
        dungeonHits = questLibrary.sortQuestByTier(dungeonHits, false);

        MarkQuest[] nameHits = questLibrary.filterQuestOfName(words, include);

        //if it found nothing, show no/all quests
        boolean noResults = 0 == (nameHits.length + dungeonFilter.length + typeHits.length);

        if (noResults) { //return sorted by tier list of all
            if (include) {
                return new MarkQuest[]{};
            } else return questLibrary.sortQuestByTier(questLibrary.getAllQuests(), false); //nothing matching to exclude --> include everything
        }

        if (include) {

            //the union of these arrays has all eligible quests. But which are the most relevant?
            MarkQuest[][] groupedHitsUnion;
            groupedHitsUnion = hitsUnionSorted(nameHits, dungeonHits, typeHits);

            return sortGroupsByType(groupedHitsUnion);
        } else { //the intersection of these lists is only relevant

            MarkQuest[] intersection = hitsIntersection(nameHits, dungeonHits, typeHits);
            return questLibrary.sortQuestByTier(intersection, false); //no need for hit-relevancy sorting, since any hits mean exclusion from list
        }

    }

    private MarkQuest[] hitsIntersection(MarkQuest[] nameHits, MarkQuest[] dungeonHits, MarkQuest[] typeHits) {
        ArrayList<MarkQuest> intersection = new ArrayList<>();
        for (MarkQuest m : nameHits){
            if (Arrays.asList(dungeonHits).contains(m) && Arrays.asList(typeHits).contains(m)){ //element is present in all 3, a.k.a. it does not mach the search term in any way.
                intersection.add(m);
            }
        }

        return intersection.toArray(new MarkQuest[]{});
    }

    private MarkQuest[] sortGroupsByType(MarkQuest[][] groupedHitsUnion){
        ArrayList<MarkQuest> arr = new ArrayList<>();

        for (MarkQuest[] m : groupedHitsUnion){
            arr.addAll(Arrays.asList(questLibrary.sortQuestByTier(m, false)));
        }

        return arr.toArray(new MarkQuest[]{});
    }

    private MarkQuest[][] hitsUnionSorted(MarkQuest[] nameHits, MarkQuest[] dungeonHits, MarkQuest[] typeHits) {
        ArrayList<Pair<MarkQuest,Integer>> toReturn = new ArrayList<>();

        MarkQuest[][] hitLists = new MarkQuest[][]{nameHits, dungeonHits, typeHits};

        for (MarkQuest[] hitList : hitLists) {
            for (MarkQuest i : hitList) {
                boolean inList = false;
                Pair<MarkQuest, Integer> toRemove = null;
                for (Pair<MarkQuest, Integer> p : toReturn) {
                    if (p.getKey().equals(i)) {
                        inList = true;
                        toRemove = p;
                    }
                }
                if (!inList) {
                    toReturn.add(new Pair<>(i, 1));
                } else {
                    toReturn.remove(toRemove);
                    toRemove = new Pair<>(toRemove.getKey(), toRemove.getValue() + 1);
                    toReturn.add(toRemove);
                }
            }
        }

        //now each markquest is paired with its hitcount
        //sort on hits, ascending
        Comparator<Pair<MarkQuest, Integer>> byHits =
                Comparator.comparing(Pair::getValue);
        toReturn.sort(byHits);
        //now have sorted (ascending) list of search result hits, no duplicates.
        //for each subgroup of hit counts, we want to sort by quest tier (descending).
        //(A different method should handle this to keep it atomic, but we don't want hitAmount information to be lost)

        Collections.reverse(toReturn); //make descending
        return createGroupedUnionArray(toReturn);
    }

    private MarkQuest[][] createGroupedUnionArray(ArrayList<Pair<MarkQuest, Integer>> sortedArrayList){
        ArrayList<ArrayList<MarkQuest>> groupSplitUnion = new ArrayList<>();

        int current = sortedArrayList.get(0).getValue();
        ArrayList<MarkQuest> toAdd = new ArrayList<>();
        for (Pair<MarkQuest, Integer> p : sortedArrayList){
            if (p.getValue() < current) { //found a new (always lower since sorted) value, create new group
                current = p.getValue();
                groupSplitUnion.add(toAdd);
                toAdd = new ArrayList<>();
            }

            toAdd.add(p.getKey()); //add to current group
        }
        groupSplitUnion.add(toAdd); //don't forget to add the last group
        //convert to 2D array
        ArrayList<MarkQuest[]> intermediate = new ArrayList<>();
        for (ArrayList<MarkQuest> arr : groupSplitUnion){
            intermediate.add(arr.toArray(new MarkQuest[]{}));
        }

        return intermediate.toArray(new MarkQuest[][]{});

    }

    private String[] getDungeonFilter(String[] words) {
        ArrayList<String> dungeonFilter = new ArrayList<>();

        for (String d : resourceManager.getDungeons()) {
            for (String w : words) {
                if (d.toLowerCase().replaceAll("[?!'.]","").contains(w)) {
                    dungeonFilter.add(d); //do not add word; case sensitivity
                }
            }
        }



        return dungeonFilter.toArray(new String[]{});
    }

    private String[] getTypeFilter(String[] words) {
        ArrayList<String> typeFilter = new ArrayList<>();

        for (String t: resourceManager.getTypes()){
            for (String w : words){
                if (t.toLowerCase().replaceAll("[?!'.]","").contains(w)){
                    typeFilter.add(t); //do not add word; case sensitivity
                }
            }
        }

        return typeFilter.toArray(new String[]{});
    }

    private String[] sanitizeInput(String text) {
        text = text.trim();
        text = text.toLowerCase();
        text = text.replaceAll("[?!'.]","");
        String[] txt = text.split(" ");
        ArrayList<String> toReturn = new ArrayList<>();
        //remove empty search words (user did double spaces)
        for (String s : txt){
            if (! s.isEmpty()){

                toReturn.add(s);
            }
        }
        return toReturn.toArray(new String[]{});
    }
}
