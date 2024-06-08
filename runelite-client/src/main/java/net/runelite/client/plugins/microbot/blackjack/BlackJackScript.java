package net.runelite.client.plugins.microbot.blackjack;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.blackjack.enums.Area;
import net.runelite.client.plugins.microbot.blackjack.enums.State;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.blackjack.enums.State.*;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntilTrue;
import static net.runelite.client.plugins.microbot.util.math.Random.random;
import static net.runelite.client.plugins.microbot.util.walker.Rs2Walker.getTile;


public class BlackJackScript extends Script {
    public static double version = 2.2;
    public static State state = BANKING;
    BlackJackConfig config;
    static boolean firstHit=false;
    boolean firstPlayerOpenCurtain = false;
    boolean initScript = false;
    boolean npcIsTrapped = false;
    boolean knockout = false;
    static boolean isPlayerNearby = false;
    static boolean npcsCanSeeEachother = false;
    NPC npc;
    public static List<NPC> npcsInArea = new ArrayList();
    static int playerHit=0;
    int lureFailed=0;
    int previousHP;
    static int hitsplatXP;
    int koXpDrop;
    int xpDrop;
    int bjCycle = 0;
    int emptyJug = 1935;
    int notedWine = 1994;
    int unnotedWine= 1993;
    int pollniveachTeleport = 11743;
    static long hitsplatStart;
    long xpdropstartTime;
    long startTime;
    long endTime;
    long previousAction;
    WorldPoint shopsLocation = new WorldPoint(3359, 2988, 0);
    private boolean hasRequiredItems() {
        return Rs2Inventory.hasItem("Coins")
                && Rs2Equipment.isWearing("blackjack")
                && (Rs2Equipment.isWearing(config.teleportItemToBank()) || Rs2Inventory.hasItem(config.teleportItemToBank()))
                && Rs2Inventory.hasItem(notedWine);
    }
    private boolean withdrawRequiredItems() {
        Rs2Bank.depositAll();
        sleep(600, 1000);
        Rs2Bank.withdrawX("Coins", 1000);
        sleepUntil(() -> Rs2Inventory.hasItem("Coins"));
        sleep(80, 120);
        Rs2Bank.withdrawAll(notedWine);//noted wines
        sleepUntil(() -> Rs2Inventory.hasItem(notedWine));
        sleep(80, 120);
        if(!Rs2Equipment.isWearing(config.teleportItemToBank())) {
            Rs2Bank.withdrawX(config.teleportItemToBank(), 1);
            sleepUntil(() -> Rs2Inventory.hasItem(config.teleportItemToBank()));
        }
        sleep(80, 120);
        Rs2Bank.withdrawX(pollniveachTeleport, 1);//Pollnivneach teleport(make with redirect scroll
        sleepUntil(() -> Rs2Inventory.hasItem(pollniveachTeleport));
        sleep(800, 1200);
        return true;
    }

    public boolean run(BlackJackConfig config) {
        this.config = config;
        initScript = true;
        state = BANKING;
        Microbot.enableAutoRunOn = false;
        useStaminaPotsIfNeeded = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {

            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()){sleep(1000); return;}
                startTime = System.currentTimeMillis();
                long restartTime = startTime-endTime;
                //System.out.println("Script took "+restartTime+"ms to restart.");
                //final Rs2Item amulet = getEquippedItem(EquipmentInventorySlot.AMULET);
                //final Rs2Item blackjack = getEquippedItem(EquipmentInventorySlot.WEAPON);
                //List<Rs2Item> foods = Microbot.getClientThread().runOnClientThread(Rs2Inventory::getInventoryFood);
                if (initScript) {
                    previousHP = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
                    if(hasRequiredItems()) {
                        if (Rs2Player.getWorldLocation().distanceTo(config.THUGS().location) < 30) {
                            state = WALK_TO_THUGS;
                        } else {
                            if(Rs2Inventory.hasItem(pollniveachTeleport)){
                                Rs2Inventory.interact(pollniveachTeleport, "break");
                                Rs2Player.waitForAnimation();
                                sleep(300,900);
                            }
                        }
                    } else {
                        state = BANKING;
                    }
                    initScript = false;
                }
                handlePlayerHit();
                if(state==BLACKJACK){
                    if(!checkCurtain(config.THUGS().door)) {
                        if (!isPlayerNearby) {
                            sleep(120, 240);
                            Rs2GameObject.interact(config.THUGS().door, "Close");
                            sleepUntil(() -> checkCurtain(config.THUGS().door), 5000);
                            bjCycle = 0;
                            sleep(120, 240);
                            if (state == BLACKJACK) {
                                state = WALK_TO_THUGS;
                            }
                        } else {
                            int r = random(1,4);
                            if(r==4 && bjCycle==0 && firstPlayerOpenCurtain){
                                sleep(400,600);
                                Rs2GameObject.interact(config.THUGS().door, "Close");
                                sleepUntil(() -> checkCurtain(config.THUGS().door), 3000);
                                bjCycle = 0;
                                sleep(400, 600);
                                if (state == BLACKJACK) {
                                    state = WALK_TO_THUGS;
                                }
                            }
                            if (npcsCanSeeEachother) {
                                int e=0;
                                while (e < 3){
                                    npcsInArea = Microbot.getClient().getNpcs().stream().filter(x ->
                                                 x.getWorldLocation().getX() >= 3340).filter(x -> x.getWorldLocation().getY() <= 2956).filter(x ->
                                                 x.getWorldLocation().getX() <= 3344).filter(x -> x.getWorldLocation().getY() >= 2953)
                                                 .collect(Collectors.toList());
                                    if ((npcsInArea.size() == 1) || (checkCurtain(config.THUGS().door) && npcsInArea.size() == 1)) {
                                        npcsCanSeeEachother=false;
                                        break;
                                    }
                                    sleep(3000);
                                    e++;
                                    //npcsCanSeeEachother=false;
                                }
                                if (e==3){
                                    npcsCanSeeEachother=false;
                                    state = BANKING;
                                }
                            }
                            if(bjCycle==0 && !firstPlayerOpenCurtain) {
                                firstPlayerOpenCurtain = true;
                            }
                        }
                    } else {
                        if(firstPlayerOpenCurtain){
                            firstPlayerOpenCurtain=false;
                        }
                    }
                }
                if (Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) <= config.healAt() || !Rs2Inventory.hasItem(unnotedWine)) {
                    if (!Rs2Inventory.hasItem(unnotedWine)) {
                        if (!Rs2Inventory.hasItem(notedWine)) {
                            state = BANKING;
                        } else {
                            state = UN_NOTING;
                        }
                    } else {
                        sleep(120,240);
                        Rs2Inventory.interact(unnotedWine, "drink");
                        sleep(120,240);
                    }
                }
                switch (state) {
                    case BANKING:
                        //System.out.println("state == BANKING");
                        if(inArea(Rs2Player.getWorldLocation(), Area.ThugHut) && isPlayerNearby){
                            sleep(120,240);
                            Rs2Equipment.interact(config.teleportItemToBank(), config.teleportActionToBank());
                            Rs2Player.waitForAnimation();
                            sleep(1000, 3000);
                        }
                        // need to teleport to bank
                            if(!hasRequiredItems()){
                                // add config item teleport here
                                boolean foundBank = Rs2Bank.openBank();
                                if (!foundBank) {
                                    sleep(120,240);
                                    Rs2Equipment.interact(config.teleportItemToBank(), config.teleportActionToBank());
                                    Rs2Player.waitForAnimation();
                                    sleep(80, 120);
                                    if(!Rs2Bank.walkToBank()) {
                                        Rs2Bank.walkToBank();
                                        return;
                                    }
                                    return;
                                }
                                sleepUntil(() -> Rs2Bank.isOpen());
                                sleep(80, 120);
                                if (Rs2Bank.isOpen()) {
                                    boolean result = withdrawRequiredItems();
                                    if (!result) return;
                                    Rs2Bank.closeBank();
                                    sleepUntil(() -> !Rs2Bank.isOpen());
                                    sleep(80, 120);
                                    if(config.wearTeleportItem() && !Rs2Equipment.isWearing(config.teleportItemToBank())){
                                        if(Rs2Inventory.hasItem(config.teleportItemToBank())){
                                            Rs2Inventory.wear(config.teleportItemToBank());
                                        }
                                    }
                                }
                                // teleport to pollniveach
                                if(Rs2Inventory.hasItem(pollniveachTeleport)){
                                    Rs2Inventory.interact(pollniveachTeleport, "break");
                                    Rs2Player.waitForAnimation();
                                    sleep(300,900);
                                    return;
                                }
                            }

                        state = UN_NOTING;
                        break;
                    case UN_NOTING:
                        //System.out.println("state == UN_NOTING");
                            if(!inArea(Rs2Player.getWorldLocation(), Area.ShopsArea)){
                              if(inArea(Rs2Player.getWorldLocation(), Area.ThugHut)){
                                sleep(120,240);
                                if(checkCurtain(config.THUGS().door)){
                                    sleep(120,240);
                                    Rs2GameObject.interact(config.THUGS().door, "Open");
                                    sleepUntil(() -> !checkCurtain(config.THUGS().door), 5000);
                                    sleep(80,160);
                                    Rs2Walker.walkTo(new WorldPoint(3346,2955,0), 0);
                                    sleepUntil(() -> Rs2Player.getWorldLocation().getX()==3346,2000);
                                    sleep(80,160);
                                    Rs2GameObject.interact(config.THUGS().door, "Close");
                                    sleepUntil(() -> checkCurtain(config.THUGS().door), 5000);
                                    sleep(120,240);
                                    Rs2Player.toggleRunEnergy(true);
                                    sleep(60,120);
                                } else {
                                    Rs2Walker.walkTo(new WorldPoint(3346,2955,0), 0);
                                    sleepUntil(() -> Rs2Player.getWorldLocation().getX()>3345);
                                    sleep(80,160);
                                    Rs2GameObject.interact(config.THUGS().door, "Close");
                                    sleepUntil(() -> checkCurtain(config.THUGS().door), 5000);
                                    sleep(120,240);
                                }
                            }
                            Rs2Walker.walkTo(shopsLocation, 1);
                            sleep(300,400);
                            return;
                        }
                        if(!Rs2Inventory.hasItem(unnotedWine)){
                            if(Rs2Inventory.hasItem(emptyJug)) {
                                sleep(120,240);
                                Rs2Npc.interact(3537, "trade");
                                sleepUntil(() -> Rs2Shop.isOpen(), 5000);
                                sleep(200, 260);
                                if(Rs2Shop.isOpen()){
                                    if (Rs2Inventory.hasItem(Rs2Inventory.get(emptyJug).name)) {
                                        Rs2Inventory.sellItem(Rs2Inventory.get(emptyJug).name, "50");
                                        sleepUntil(() -> !Rs2Inventory.hasItem(Rs2Inventory.get(emptyJug).name));
                                        sleep(200, 260);
                                        Rs2Shop.closeShop();
                                        sleepUntil(() -> !Rs2Shop.isOpen(), 5000);
                                        sleep(200, 260);
                                    }
                                }
                            }
                            if(Rs2Inventory.hasItem(notedWine)){
                                if (!Rs2Inventory.isItemSelected()) {
                                    Rs2Inventory.use(notedWine);
                                    sleep(80, 160);
                                } else {
                                    sleep(120,240);
                                    Rs2Npc.interact(1615, "Use");
                                    sleepUntil(() -> Microbot.getClient().getWidget(14352385) != null,2000);
                                    sleep(120,240);
                                    if (Microbot.getClient().getWidget(14352385) != null) {
                                        //Rs2Keyboard.keyPress(KeyEvent.VK_3);
                                        Rs2Keyboard.keyPress('3');
                                        sleepUntil(() -> Rs2Inventory.hasItem(unnotedWine));
                                        sleep(40, 450);
                                    }
                                }
                            }
                        }
                        if(!Rs2Inventory.hasItem(notedWine)){
                            state = BANKING;
                            return;
                        }
                        if(!Rs2Inventory.hasItem(unnotedWine)){
                            return;
                        }
                        state = WALK_TO_THUGS;
                        break;
                    case WALK_TO_THUGS:
                        //System.out.println("state == WALK_TO_THUGS");
                        if (inArea(Rs2Player.getWorldLocation(), Area.ThugHut)) {
                            //npcsInArea = Microbot.getClient().getNpcs().stream().filter(x -> x.getWorldLocation().distanceTo(getInitialPlayerLocation()) < 2).collect(Collectors.toList()); ORIGINAL
                            npcsInArea = Microbot.getClient().getNpcs().stream().filter(x ->
                            Objects.requireNonNull(x.getName()).contains(config.THUGS().displayName)).filter(x ->
                            x.getWorldLocation().getX() >= 3340).filter(x -> x.getWorldLocation().getY() <= 2956).filter(x ->
                            x.getWorldLocation().getX() <= 3344).filter(x -> x.getWorldLocation().getY() >= 2953)
                            .collect(Collectors.toList());
                            if(npcsInArea.isEmpty()){
                                sleep(120,240);
                                if(checkCurtain(config.THUGS().door)) {
                                    Rs2GameObject.interact(config.THUGS().door, "Open");
                                    sleepUntil(() -> !checkCurtain(config.THUGS().door), 5000);
                                    sleep(120, 240);
                                }
                                npcIsTrapped=false;
                                state = TRAP_NPC;
                                //TODO make the player walk to the NPC / check that the player can get to the NPC before trying to lure it.
                                npc = Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getNpcs().stream()
                                        .filter(x -> x != null && x.getName() != null && !x.isDead())
                                        .filter(x -> Objects.requireNonNull(x.getName()).contains(config.THUGS().displayName))
                                        .sorted(Comparator.comparingInt(value -> value.getLocalLocation()
                                        .distanceTo(new LocalPoint(3337,2950,0))))).findFirst().get();
                                //npc = Rs2Npc.getNpc(config.THUGS().displayName); OLD VERSION
                                return;
                            } else {
                                if(npcsInArea.size()>1){
                                    state = LURE_AWAY;
                                    return;
                                }
                                sleep(120,240);
                                  if(checkCurtain(config.THUGS().door)){//true == door is closed
                                    sleep(120,240);
                                    if (!npcIsTrapped) {
                                        npcIsTrapped = true;
                                    }
                                      state = BLACKJACK;
                                } else {
                                    npcIsTrapped = true;
                                    state = BLACKJACK;
                                }
                                  npc = npcsInArea.stream().findFirst().get();
                                //npc = Rs2Npc.getNpcs().findFirst().get();
                            }
                        } else {
                            Rs2Walker.walkTo(config.THUGS().location, 0);
                            sleep(120,180);
                            return;
                        }
                        break;
                    case LURE_AWAY:
                        //System.out.println("state == LURE_AWAY");
                        npc = Rs2Npc.getNpcs().findFirst().get();
                        sleep(120,240);
                        if(lure_NPC(npc)){
                            sleep(60, 180);
                            state = RUN_AWAY;
                        }

                        break;
                    case TRAP_NPC:
                        //System.out.println("state == TRAP_NPC");
                        if(!npcIsTrapped){
                            if(lure_NPC(npc)) {
                                if(lureFailed>0){
                                    lureFailed=0;
                                }
                                sleep(60, 180);
                                state = WALK_TO_THUGS;
                                return;
                            } else {
                                lureFailed++;
                                if(lureFailed==5){
                                    lureFailed=0;
                                    state = BANKING;
                                }
                            }
                        } else {
                            state = BLACKJACK;
                            return;
                        }
                        //break;
                    case RUN_AWAY:
                        //System.out.println("state == RUN_AWAY");
                        Rs2Player.toggleRunEnergy(true);
                        if(checkCurtain(config.THUGS().door)){
                            sleep(160,190);
                            Rs2GameObject.interact(config.THUGS().door, "Open");
                            sleepUntil(() ->!checkCurtain(config.THUGS().door));
                            sleep(80,160);
                        }
                        Rs2Walker.walkTo(new WorldPoint(3346,2955,0), 0);
                        sleepUntil(() -> Rs2Player.getWorldLocation().getX()>3345 && Rs2Player.getWorldLocation().getY()==2955);
                        sleep(60,120);
                        Rs2GameObject.interact(config.THUGS().door, "Close");
                        sleepUntil(() -> checkCurtain(config.THUGS().door), 5000);
                        sleep(120,240);
                        Rs2Walker.walkTo(new WorldPoint(3352,2960,0), 0);
                        sleepUntil(() -> Rs2Player.getWorldLocation().getX()>3347 && Rs2Player.getWorldLocation().getY()>2956, 2000);
                        sleep(200,300);
                        Rs2GameObject.interact(6242,true);
                        sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().getPlane()==1,8000);
                        sleep(120,240);
                        Rs2GameObject.interact(6243,true);
                        sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().getPlane()==0,8000);
                        sleep(120,240);
                        state = WALK_TO_THUGS;
                        return;

                    case BLACKJACK:
                        //System.out.println("state == BLACKJACK");
                        if(!npcIsTrapped){
                            state = TRAP_NPC;
                            return;
                        }
                        if (bjCycle == 0){
                            previousHP = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
                            xpdropstartTime = System.currentTimeMillis();
                            koXpDrop = Microbot.getClient().getSkillExperience(Skill.THIEVING);
                            Rs2Npc.interact(npc, "Knock-Out");
                            previousAction=System.currentTimeMillis();
                            knockout = true;
                            endTime = System.currentTimeMillis();
                            ++bjCycle;
                            return;
                        }
                        if (bjCycle <= 2 && bjCycle>0){
                            if(knockout){
                                if(koXpDrop==Microbot.getClient().getSkillExperience(Skill.THIEVING)){
                                    if(playerHit>0) {
                                        bjCycle=0;
                                        sleep(220,280);
                                        return;
                                    }
                                    return;
                                }
                                sleepUntil(() -> npc.getAnimation()==838, 600);
                            }
                            xpDrop = Microbot.getClient().getSkillExperience(Skill.THIEVING);
                            xpdropstartTime = System.currentTimeMillis();//+random(50,65)
                            //followingAction=previousAction+1140+random(50,65);//TODO 60ms is good.70ms starts to miss.50ms decent.
                            //TODO change this to calculate the time needed to sleep?
                            if((previousAction+1190)>System.currentTimeMillis()) {
                                sleep((int) ((previousAction + 1140 + random(50, 65)) - System.currentTimeMillis()));
                            }
                            if(npc.getAnimation()==838) {
                                Rs2Npc.interact(npc, "Pickpocket");
                                knockout=false;
                                sleepUntil(() -> xpDrop < Microbot.getClient().getSkillExperience(Skill.THIEVING), 1000);
                            } else {
                                //System.out.println("reset bjCycle");
                                sleep(90,140);
                                bjCycle=0;
                                return;
                            }
                            previousAction=System.currentTimeMillis();
                            endTime = System.currentTimeMillis();
                            ++bjCycle;
                            return;
                        }
                        sleepUntil(() -> npc.getAnimation()!=838,600);
                        sleep(120,180);
                        bjCycle=0;
                        break;

                }
                endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                //System.out.println("Total time for loop " + totalTime);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
        return true;
    }

    public static boolean checkCurtain(WorldPoint a) {
        Tile currentTile = getTile(a);
        WallObject wallObject;
        if (currentTile != null) {
            wallObject = currentTile.getWallObject();
        } else {
            wallObject = null;
        }

        if (wallObject != null) {
            ObjectComposition objectComposition = Rs2GameObject.getObjectComposition(wallObject.getId());
            if (objectComposition == null) {
                return false;
            }
            for (String action : objectComposition.getActions()) {
                return action != null && (action.equals("Open"));
            }
        }
        return false;
    }
    public boolean lure_NPC(NPC npc){
        Rs2Player.toggleRunEnergy(false);
        sleep(200,260);
        Rs2Npc.interact(npc, "Lure");
        boolean lureStarted = sleepUntilTrue(() -> Rs2Widget.hasWidget("Psst. Come here, I want to show you something."), 100, 5000);
        if(lureStarted){
            sleep(120,160);
            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            sleepUntilTrue(() -> !Rs2Widget.hasWidget("Psst. Come here, I want to show you something."), 100, 3000);
            sleep(120,160);
        } else {
            //TODO add something here to target a different NPC
            return false;
        }
        boolean lureResult = Rs2Widget.hasWidget("What is it?");
        if(lureResult){
            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            sleepUntilTrue(() -> !Rs2Widget.hasWidget("What is it?"), 100, 3000);
            sleep(120,160);
            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            Rs2Walker.walkTo(new WorldPoint(3346,2955,0), 0);
            sleepUntil(() -> Rs2Player.getWorldLocation().getX()==3346 && Rs2Player.getWorldLocation().getY()==2955);
            sleep(120,160);
            waitForNPC(npc);
            if(npc.getWorldLocation().getY()<Rs2Player.getWorldLocation().getY()){
                Rs2Walker.walkTo(new WorldPoint(3346,2959,0), 0);
                sleepUntil(() -> Rs2Player.getWorldLocation().getY()>=2958, 3000);
                waitForNPC(npc);
                Rs2Walker.walkTo(new WorldPoint(3346,2955,0), 0);
                sleepUntil(() -> Rs2Player.getWorldLocation().getX()==3346 && Rs2Player.getWorldLocation().getY()==2955);
                waitForNPC(npc);
                Rs2Walker.walkTo(new WorldPoint(3343,2954,0), 0);
                sleepUntil(() -> Rs2Player.getWorldLocation().getX()==3343 && Rs2Player.getWorldLocation().getY()==2954, 3000);
                Rs2Player.toggleRunEnergy(true);
            } else {
                Rs2Walker.walkTo(new WorldPoint(3343,2954,0), 0);
                sleepUntil(() -> Rs2Player.getWorldLocation().getX()==3343 && Rs2Player.getWorldLocation().getY()==2954, 3000);
                waitForNPC(npc);
                Rs2Player.toggleRunEnergy(true);
            }
            npcIsTrapped=true;
            return true;
        } else {
            sleep(300,600);
            return false;
        }
    }
    public void waitForNPC(NPC npc){
        long movingStart = System.currentTimeMillis();
        while(npc.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())!=1){
            WorldPoint isMoving = npc.getWorldLocation();
            sleep(700);
            if(npc.getWorldLocation()==isMoving){
                break;
            }
            if((System.currentTimeMillis()-movingStart)>=5000){
                break;
            }
        }
    }
    public void handlePlayerHit(){
        if(playerHit>=1){
            int j = 0;
            int i = random(2, 3);
            int c = 80;
            if(playerHit==1 && firstHit) {
                //TODO play around with this sleep to see what's the highest tolerance before you're too late to react.
                sleep(60,120);
                while (j < i) {
                    Rs2Npc.interact(npc, "Pickpocket");
                    sleep(c, (int) (c * 1.3));
                    c = (int) (c * 1.4);
                    ++j;
                }
                firstHit=false;
                bjCycle = 0;
            }
                if(Microbot.getClient().getLocalPlayer().hasSpotAnim(245)){
                return;
                }
                if (playerHit == 1) {
                    playerHit = 0;
                } else {
                    playerHit = 0;
                    state = RUN_AWAY;
                    knockout = false;
                    bjCycle = 0;
                }
                previousHP = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        }
    }
    public static boolean inArea(WorldPoint entity, Area area){
        return (entity.getX() >= area.ax && entity.getY() <= area.ay) && (entity.getX() <= area.bx && entity.getY() >= area.by);
    }
    @Override
    public void shutdown() {
        super.shutdown();
    }
}
