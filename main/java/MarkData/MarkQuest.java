package MarkData;

/**
 * Data-type for holding a mark quest
 * A mark quest has:
 *
 * Type: (Scout, Standard, Epic)
 * Name: ("The Pirate King", "To the Mountains!")
 * completionReq : Array of MarkRequirements. When all Requirements are fulfilled, the Quest is complete.
 */
public class MarkQuest {

    private final String questType;
    private final String name;
    private final MarkRequirement[] completionReq;

    MarkQuest(String type, String name, MarkRequirement[] completionReq){
        this.questType = type;
        this.name = name;
        this.completionReq = completionReq;
    }

    public String getType() {
        return questType;
    }

    public String getName() {
        return name;
    }

    public MarkRequirement[] getCompletionReq() {
        return completionReq;
    }

    public int getTotalMarkAmount() {
        int total = 0;
        for (MarkRequirement req : completionReq){
            total += req.getAmount();
        }
        return total;
    }
}
