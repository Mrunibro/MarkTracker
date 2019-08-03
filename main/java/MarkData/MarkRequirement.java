package MarkData;

/**
 * Data-type for a MarkRequirement
 *
 * A MarkRequirement is a requirement for completing a MarkQuest
 *
 * A MarkQuest can have 1 or more MarkRequirements
 *
 * A MarkRequirement has a:
 * markType: ("SnakePit", "TheNest")
 *
 * Convention for a mark type: Dungeon name capitalizing each letter, no spaces.
 *
 * amount: (int) , number of this mark type required
 */
public class MarkRequirement {

    private final String markType;
    private final int amount;

    MarkRequirement(String markType, int amount){
        this.markType = markType;
        this.amount = amount;
    }

    public String getMarkType() {
        return markType;
    }

    public int getAmount() {
        return amount;
    }
}
