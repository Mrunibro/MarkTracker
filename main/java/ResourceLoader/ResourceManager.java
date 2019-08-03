package ResourceLoader;

import MarkData.MarkQuest;
import MarkData.MarkRequirement;
import MarkData.QuestLibrary;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class handles reading files from the resources folder, and returning them as meaningful types.
 *
 * This class can handle both JSON and spritesheets.
 */
public class ResourceManager {

    private static ResourceManager singleton = new ResourceManager();

    private final BufferedImage markSpriteSheet; //the spritesheet, held in memory to reduce load times
    private final HashMap<String, Integer> questMapping; //Mapping of quest types (Epic, Scout) to their 'tier'
    private final HashMap<String, Integer> spriteMapping; //Mapping of dungeon names to their mark on the spriteSheet. Also uses Keyset for list of all dungeons!

    private boolean loadedQuests = false; //does not permit use of loadAllQuests more than once, since QuestLibrary (singleton) will be holding it.

    private ResourceManager() {
        markSpriteSheet = loadSpritesheet("MarkRenders.png");

        JSONArray questTiers = readResourceAsJSONArray("MarkQuestTypes.json");
        JSONArray spritePositions = readResourceAsJSONArray("MarkQuestDungeons.json");

        questMapping = mapJSONArray(questTiers, "type", "tier");
        spriteMapping = mapJSONArray(spritePositions, "dungeon", "sheetPos");
    }

    public static ResourceManager getInstance(){
        return singleton;
    }

    /**
     * Returns the sprite of the mark associated with the Dungeon.
     *
     * This method assumes user input has been filtered to a valid name.
     *
     * @param dungeon the dungeon the mark sprite is associated with.
     * @return the sprite of the Mark as BufferedImage
     */
    public BufferedImage getSprite(String dungeon){
        assertValidDungeon(dungeon);
        return getSprite(spriteMapping.get(dungeon));
    }

    /**
     * Nothing to see here
     * @return nothing
     */
    public BufferedImage getSecretSprite(){
        return getSprite(99);
    }

    /**
     * Loads ArrayList of all Tinkerer Quests, then converts to Array.
     *
     * Reads from JSON holding all quest data (MarkQuests.json)
     *
     * Also checks validity of Dungeons & Quest types.
     *
     * Does not permit use after initialisation, despite being public.
     * (Want QuestLibrary class to hold result instead of ResourceManager)
     * Use QuestLibrary.getInstance().getAllQuests(); instead. This method reads from file.
     *
     * @return all Quests currently available at the Tinkerer
     */
    public MarkQuest[] loadAllQuests() {
        if (loadedQuests){
            throw new IllegalArgumentException("Attempted to load quests from file after initialisation! use QuestLibrary.getAllQuests instead.");
        }
        loadedQuests = true;
        JSONArray questData = readResourceAsJSONArray("MarkQuests.json");
        ArrayList<MarkQuest> tempQuestList = new ArrayList<>();

        String type;
        String name;
        MarkRequirement[] reqs;
        for (Object aQuestData : questData) {
            JSONObject j = (JSONObject) aQuestData; //j is a MarkQuest in JSON format

            name = (String) j.get("name");
            type = (String) j.get("type");
            assertValidType(type);

            JSONArray markReqs = (JSONArray) j.get("req");
            reqs = new MarkRequirement[markReqs.size()];

            for (int k = 0; k < markReqs.size(); k++) {
                JSONObject requirement = (JSONObject) markReqs.get(k);
                String dungeon = (String) requirement.get("dungeon");
                assertValidDungeon(dungeon);
                int amount = Integer.parseInt((String) requirement.get("amount")); //simpleJSON library hates ints, so I store numbers as String instead. Fight me.

                reqs[k] = QuestLibrary.createMarkRequirement(dungeon, amount);
            }
            //We now have all the data needed to make a MarkQuest
            tempQuestList.add(QuestLibrary.createMarkQuest(type, name, reqs));
        }

        return tempQuestList.toArray(new MarkQuest[]{});
    }

    /**
     * Get the tier associated with a quest type.
     * @param type the type of the quest (Scout, Epic)
     * @return the tier of this quest.
     */
    public int getTypeTier(String type){
        assertValidType(type);
        return questMapping.get(type);
    }

    /**
     * Check inserted type against list of valid ones.
     * @param a quest Type to test validity of
     * @throws IllegalArgumentException if parameter is not in list
     */
    public void assertValidType(String a){
        boolean isValid = false;
        for (String b : questMapping.keySet()){
            if (a.equals(b)) isValid = true;
        }
        if (!isValid) throw new IllegalArgumentException("Type " + a + " is not a supported Quest type!");
    }

    public String[] getTypes(){
        return questMapping.keySet().toArray(new String[]{});
    }

    /**
     * Check inserted dungeon against list of valid ones.
     * @param a quest Dungeon to test validity of
     * @throws IllegalArgumentException if parameter is not in list
     */
    public void assertValidDungeon(String a) {
        boolean isValid = false;
        for (String b : spriteMapping.keySet()){
            if (a.equals(b)) isValid = true;
        }
        if (!isValid) throw new IllegalArgumentException("Dungeon " + a + " is not a supported Dungeon!");
    }

    public String[] getDungeons(){
        return spriteMapping.keySet().toArray(new String[]{});
    }

    /**
     * Get a specific sprite by way of sub-image.
     * Sprites are indexed from 0 until ?,
     *
     * There are 5 sprites of 40x40 (RotMG Renders size of 8x8) per row
     *
     * @param spriteNum the index of the sprite
     * @return the subimage that is the sprite
     */
    private BufferedImage getSprite(int spriteNum){
        final int spriteWidth = 40; //a sprite is 40px wide
        final int spriteHeight = 40; //a sprite is 40px high
        final int sheetWidth = 5; //the sheet supports 5 sprites per row
        //(height is always unlimited, " just add more space " )

        int spriteX = spriteWidth * (spriteNum % sheetWidth);
        int spriteY = spriteHeight * (spriteNum / sheetWidth);

        BufferedImage sheet = getMarkSheet();
        return sheet.getSubimage(spriteX, spriteY, spriteWidth, spriteHeight);
    }

    /**
     * Get the mark sheet from memory rather than loading the file
     * @return BufferedImage of MarkRenders.png
     */
    private BufferedImage getMarkSheet() {
        return markSpriteSheet;
    }

    /**
     * find a resource and return it as a JSONArray
     *
     * Uses inputStream & ClassLoader because File system cannot access files within a JAR archive, Apparently.
     *
     * @param resourceName fileName of the resource that has to be loaded
     * @return JSONArray of the resource
     */
    private JSONArray readResourceAsJSONArray(String resourceName){
        System.out.println("loading File" + resourceName);

        final String resourceFolder = "/ResourceLoader/";
        InputStream is = this.getClass().getResourceAsStream(resourceFolder + resourceName);

        JSONArray arr = null;
        try {
            String result = readInputStream(is);
            arr = (JSONArray) new JSONParser().parse(result);
        } catch (IOException | ParseException e){
            System.out.println("Failed to load resource " + resourceFolder + resourceName);
            e.printStackTrace();
        }
        assert arr != null;
        return arr;
    }

    /**
     * Load a resource, read it as a " spritesheet"
     *
     * different from "getters" of spritesheets: getters return an image held in memory,
     * So that loading times for many lonesome sprites need not obtain the entire sheet every load.
     *
     * @param resourceName the name of the resource
     * @return a BufferedImage of the spritesheet
     */
    private BufferedImage loadSpritesheet(String resourceName){
        System.out.println("loading " + resourceName);
        URL url = ResourceManager.class.getResource(resourceName);
        BufferedImage sheet = null;
        try {
            sheet = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not read spritesheet" + resourceName + "at: \n" + url);
        }
        assert sheet != null;
        return sheet;
    }

    /**
     * Helper method of readResourceAsJSONArray
     *
     * Reads an InputStream and returns the String it has found.
     * The inputStream is required to reliably get resources from inside a JAR,
     * since the File class cannot access it.
     *
     * @param is the InputStream to read
     * @return String of file that was read, UTF-8
     * @throws IOException if reading failure
     */
    private String readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;

        while ((length = is.read(buffer)) != -1){
            result.write(buffer, 0, length);
        }
        is.close();
        return result.toString("UTF-8");
    }

    /**
     * Fulfills a rather specific purpose of mapping a JSONArray of JSONObjects
     * To a String; Int type mapping for holding in memory.
     *
     * Not robust
     *
     * @param arr the JSONArray holding JSONObjects
     * @param key the key to 'get' from the JSONObject to use as HashMap key
     * @param value the key to 'get' from the JSONObject to use as HashMap value
     * @return mapping of key to value from JSONArray's JSONObjects.
     */
    private HashMap<String, Integer> mapJSONArray(JSONArray arr, String key, String value){
        HashMap<String, Integer> toReturn = new HashMap<>();

        for (Object o: arr){
            JSONObject j = (JSONObject) o;
            String theKey = (String) j.get(key);
            Integer theValue = Integer.parseInt((String) j.get(value));

            toReturn.put(theKey, theValue);
        }
        return toReturn;
    }
}
