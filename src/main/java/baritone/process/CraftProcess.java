/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.process.ICraftProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.schematic.FillSchematic;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.Helper;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextFormatting;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CraftProcess extends BaritoneProcessHelper implements ICraftProcess {
    boolean shouldMine = false;
    boolean shouldBuild = false;
    private boolean active = false;
    private Item item;
    private int needed = -1,
            currentAmount = 0;
    private BlockPos craftingTablePosition = null;
    private CraftingRecipe recipe = null;
    private boolean useInventory = false;
    private NonNullList<ItemStack> inventory = null;
    private boolean removingResult = false;
    private int amountRemoved = 0;
    private LinkedList<AbstractClickAction> clicks = null;
    private boolean usingMyOwnCraftingTable = false;

    public CraftProcess(Baritone baritone) {
        super(baritone);
    }

    public List<IRecipe> getPossibility() {
        try {
            return CraftingManager.REGISTRY.getKeys().stream()
                    .map(CraftingManager.REGISTRY::getObject).filter(Objects::nonNull)
                    .filter(recipe -> {
                        ItemStack out = recipe.getRecipeOutput();


                        return out.getItem().equals(item);
                    })
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            System.err.println("Cannot craft: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void craft(Item item, int amount) {
        useInventory = false;
        if (item == null) {
            return;
        }
        active = true;
        this.item = item;
        this.needed = amount;
        this.currentAmount = 0;
        inventory = mc.player.inventory.mainInventory;
        List<IRecipe> possibilities = getPossibility();
        usingMyOwnCraftingTable = false;
        craftingTablePosition = null;
        recipe = decideBest(possibilities);
        clicks = null;
        recipe.generateItems();
        shouldMine = false;
        shouldBuild = false;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (baritone.getBuilderProcess().isActive() && shouldBuild) {
            return new PathingCommand(null, PathingCommandType.DEFER);
        }
        baritone.getInputOverrideHandler().clearAllKeys();
        if (shouldMine) {

            IBlockState state = baritone.bsi.get0(craftingTablePosition.subtract(new Vec3i(1, 0, 0)));
            if (state.getBlock() == Blocks.AIR) {
                active = false;
                HELPER.logDirect("finished");
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            BlockPos position = craftingTablePosition.subtract(new Vec3i(1, 0, 0));
            if (!MovementHelper.avoidBreaking(baritone.bsi, position.getX(), position.getY(), position.getZ(), state)) {
                Optional<Rotation> rot = RotationUtils.reachable(ctx, position);
                if (rot.isPresent() && isSafeToCancel) {
                    baritone.getLookBehavior().updateTarget(rot.get(), true);
                    MovementHelper.switchToBestToolFor(ctx, ctx.world().getBlockState(position));

                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }
            }
        }
        if (removingResult) {
            if (amountRemoved >= needed) {
                mc.player.closeScreen();

                if (usingMyOwnCraftingTable) {
                    usingMyOwnCraftingTable = false;
                    useInventory = true;
                    shouldMine = true;
                    //if you have more than one stack of crafting tables you don't need to re-mine the one placed down.
                    //  baritone.getMineProcess().mine(getSlotContentCount(mc.player.inventoryContainer.getSlot(findSlotFor(Item.getItemById(58)))) + 1, Blocks.CRAFTING_TABLE);
                    removingResult = false;
                    amountRemoved = 0;

                    return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);
                } else {

                    active = false;
                    removingResult = false;
                    amountRemoved = 0;
                }
                return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);

            }
            removeResult();
            amountRemoved++;
        }

        if ((mc.currentScreen instanceof GuiCrafting || mc.currentScreen instanceof GuiInventory) && clicks != null) {
            if (clicks.size() > 0) {
                clicks.remove().click();
            } else {
                removingResult = true;
            }
        }
        if (active) {

            if (baritone.getInputOverrideHandler().isInputForcedDown(Input.CLICK_RIGHT)) {
                baritone.getInputOverrideHandler().clearAllKeys();
            }

            if (recipe != null && recipe.use22) {

                useInventory = true;
                mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiInventory(mc.player)));
                craftAll();
                return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);

            }
            if (useInventory) {
                return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);

            }
            if (craftingTablePosition == null) {
                List<BlockPos> positions = MineProcess.searchWorld(new CalculationContext(baritone), new BlockOptionalMetaLookup(Blocks.CRAFTING_TABLE), 1, new ArrayList<>(), new ArrayList<>(), Collections.emptyList());
                if (positions.size() == 0) {
                    //force place a crafting table
                    craftingTablePosition = new BlockPos(10000, 10000, 1000);
                } else {
                    craftingTablePosition = positions.get(0);
                }

                if (!(craftingTablePosition.distanceSq(mc.player.getPosition()) <= Baritone.settings().craftingPlaceMinDistance.value * Baritone.settings().craftingPlaceMinDistance.value) && Baritone.settings().allowCraftingPlace.value) {
                    int id = searchInventory(Item.getItemById(58));
                    if (useInventory)
                        id -= 9;
                    else
                        id -= 10;

                    if (id < 0) {
                        if (craftingTablePosition.getY() == 10000) {
                            HELPER.logDirect("no crafting table in inventory or in render distance", TextFormatting.DARK_RED);
                            active = false;
                            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                        }
                        return new PathingCommand(new GoalGetToBlock(craftingTablePosition), PathingCommandType.SET_GOAL_AND_PATH);
                    }
                    mc.player.inventory.currentItem = id;
                    usingMyOwnCraftingTable = true;
                    List<BlockPos> positionsPlace = MineProcess.searchWorld(new CalculationContext(baritone), new BlockOptionalMetaLookup(Blocks.AIR), 70, new ArrayList<>(), new ArrayList<>(), Collections.emptyList());
                    craftingTablePosition = positionsPlace.get(0);
                    FillSchematic fillSchematic = new FillSchematic(1, 1, 1, Blocks.CRAFTING_TABLE.getBlockState().getBaseState());
                    // make it place a crafting table.
                    baritone.getBuilderProcess().build("hi", fillSchematic, new Vec3i(craftingTablePosition.getX() - 1, craftingTablePosition.getY(), craftingTablePosition.getZ()));
                    shouldBuild = true;

                }
                return new PathingCommand(new GoalGetToBlock(craftingTablePosition), PathingCommandType.SET_GOAL_AND_PATH);
            } else {

                if (craftingTablePosition.distanceSq(mc.player.getPosition()) <= 6) {

                    if (!(Helper.mc.currentScreen instanceof GuiCrafting)) {

                        Optional<Rotation> rot = RotationUtils.reachable(ctx, craftingTablePosition.subtract(new Vec3i(1, 0, 0)));
                        if (rot.isPresent() && isSafeToCancel) {
                            baritone.getLookBehavior().updateTarget(rot.get(), true);
                        }

                        baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);

                    } else {
                        if (recipe != null) {
                            craftAll();
                        }
                    }


                }

            }
        }

        return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);
    }

    @Override
    public void onLostControl() {
        if (clicks != null)
            clicks.clear();
        active = false;
        craftingTablePosition = null;
    }

    public void craftAll() {
        if (recipe != null) {

            recipe.generateItems();
            clicks = new LinkedList<>();

            for (; currentAmount < needed; currentAmount++) {
                recipe.craft();
            }
            recipe = null;
        }
    }


    @Override
    public String displayName0() {
        return "crafting " + item.getDefaultInstance().getDisplayName();
    }

    public void removeResult() {
        new LeftClickAction(0).click();

        new LeftClickAction(findSlotFor(this.item)).click();
    }

    public int findSlotFor(Item item) {
        NonNullList<ItemStack> mainInventory = inventory;
        for (int i = 0; i < mainInventory.size(); i++) {


            if (
                    mainInventory.get(i).getItem() == item) {
                if (mainInventory.get(i).getMaxStackSize() == mainInventory.get(i).getCount()) {
                    continue;
                }
                return useInventory ? convertToPlayerInventorySlot(i) + 9 : 10 + convertToPlayerInventorySlot(i);
            }


        }

        for (int i = 0; i < mainInventory.size(); i++) {


            if (
                    mainInventory.get(i).getItem() == Items.AIR) {

                return useInventory ? convertToPlayerInventorySlot(i) + 9 : 10 + convertToPlayerInventorySlot(i);
            }


        }
        return -1;
    }

    public int searchInventory(Item item) {
        NonNullList<ItemStack> mainInventory = inventory;
        int inventorySlot = -1;
        for (int i = 0; i < mainInventory.size(); i++) {

            if (item != null && item.equals(
                    mainInventory.get(i).getItem())) {
                inventorySlot = i;
            }
        }
        if (inventorySlot < 0) {
            return -1;
        }

        return useInventory ? convertToPlayerInventorySlot(inventorySlot) + 9 : 10 + convertToPlayerInventorySlot(inventorySlot);
    }

    public CraftingRecipe decideBest(List<IRecipe> recipes) {

        Stream<CraftingRecipe> recipesC = recipes.stream().map(CraftingRecipe::new);
        Optional<CraftingRecipe> recipe2 = recipesC.findAny();
        return recipe2.orElse(null);
    }

    private int moveItemAmount(Slot from, Slot to, int count) {
        int oldCount = getSlotContentCount(to);

        addClick(new LeftClickAction(from.slotNumber));
        for (int i = 0; i < count; i++) {
            addClick(new RightClickAction(to.slotNumber));
        }
        addClick(new LeftClickAction(from.slotNumber));
        return getSlotContentCount(to) - oldCount;
    }

    private void addClick(AbstractClickAction action) {
        clicks.add(action);
    }

    private int getSlotContentCount(Slot slot) {
        if (slot == null) {
            return 0;
        }
        return slot.getHasStack() ? slot.getStack().getCount() : 0;
    }

    private int convertToPlayerInventorySlot(int inventorySlot) {
        // Offset: 10 blocks.
        if (inventorySlot < 9) {
            return inventorySlot + 9 * 3; //
        } else {
            return inventorySlot - 9;
        }
    }

    private int convertFromPlayerInventorySlot(int inventorySlot) {
        // Offset: 10 blocks.
        if (inventorySlot > 27) {
            return inventorySlot - 9 * 3;
        } else {
            return inventorySlot + 9;
        }
    }


    private static abstract class AbstractClickAction {
        protected final int slotNumber;

        public AbstractClickAction(int slotNumber) {
            super();
            this.slotNumber = slotNumber;
        }

        protected void click() {
            int clickKey = getClickKey();
            ClickType clickType = getClickType();

            final GuiContainer screen = (GuiContainer) Helper.mc.currentScreen;
            assert screen != null;
            Helper.mc.playerController.windowClick(
                    screen.inventorySlots.windowId, slotNumber, clickKey, clickType,
                    Helper.mc.player);
        }

        abstract ClickType getClickType();

        abstract int getClickKey();
    }

    private static class RightClickAction extends AbstractClickAction {
        public RightClickAction(int slotNumber) {
            super(slotNumber);
        }

        @Override
        public ClickType getClickType() {
            return ClickType.PICKUP;
        }

        @Override
        public int getClickKey() {
            return 1;
        }

        @Override
        public String toString() {
            return "RightClickAction [slotNumber=" + slotNumber + "]";
        }
    }

    private static class LeftClickAction extends AbstractClickAction {
        public LeftClickAction(int slotNumber) {
            super(slotNumber);
        }

        @Override
        public ClickType getClickType() {
            return ClickType.PICKUP;
        }

        @Override
        public int getClickKey() {
            return 0;
        }

        @Override
        public String toString() {
            return "LeftClickAction [slotNumber=" + slotNumber + "]";
        }
    }

    public class CraftingRecipe {
        public Ingredient[][] ingredients = new Ingredient[3][3];
        public Item[][] items = new Item[3][3];
        private boolean use22 = false;
        private int x,
                y;


        public CraftingRecipe(IRecipe recipe) {


            if (recipe instanceof ShapedRecipes) {
                ShapedRecipes shapedRecipes = (ShapedRecipes) recipe;
                if (shapedRecipes.canFit(2, 2)) {
                    use22 = true;
                    useInventory = true;
                    ingredients = new Ingredient[2][2];
                    items = new Item[2][2];
                }
                int width = shapedRecipes.getWidth();
                int height = shapedRecipes.getHeight();
                shapedRecipes.canFit(2, 2);

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        Ingredient itemStack = shapedRecipes.getIngredients().get(x + y * width);

                        ingredients[x][y] = itemStack;
                    }
                }


            } else {
// this sometimes gets thrown, I don't really know why. They all should be shaped recipes.
                HELPER.logDirect("Cannot (yet) craft " + recipe);
                onLostControl();
            }

        }

        @Override
        public String toString() {
            generateItems();
            StringBuilder builder = new StringBuilder();
            for (int y = 0; y < items.length; y++) {
                StringBuilder builder1 = new StringBuilder();
                for (int x = 0; x < items[y].length; x++) {
                    if (items[x][y] != null) {
                        builder1.append(items[x][y].getItemStackDisplayName(items[x][y].getDefaultInstance())).append(" item ");
                    } else {
                        builder1.append("  null  item  ");

                    }
                }

                builder.append(builder1);
            }


            return builder.toString();
        }

        public void generateItems() {
            List<Item> preferableItems = new ArrayList<>();
            for (x = 0; x < items.length; x++) {
                for (y = 0; y < items[x].length; y++) {

                    Ingredient ingredient = ingredients[x][y];


                    if (ingredient == null) {
                        continue;
                    }
                    Stream<ItemStack> ingredients =
                            Arrays.stream(ingredient.getMatchingStacks()).filter(
                                    itemStack -> searchInventory(itemStack.getItem()) != -1);
                    List<ItemStack> items2 = ingredients.collect(Collectors.toList());
                    ItemStack[] ingredientA = new ItemStack[items2.toArray().length];
                    for (int i = 0; i < ingredientA.length; i++) {
                        ingredientA[i] = items2.get(i);
                    }
                    for (ItemStack itemStack : ingredientA) {
                        if (items[x][y] != null) {
                            break;
                        }
                        //prefer to reuse previously used items.
                        //make this take into account the amount of each item
                        preferableItems.forEach(itemStack1 -> {
                            if (itemStack.getItem() == itemStack1) {
                                items[x][y] = itemStack.getItem();
                            }
                        });

                    }
                    preferableItems.add(items[x][y]);
                    //If we don't have an answer yet just assume the first one.
                    //TODO make it take into account the rarity of items.
                    if (items[x][y] == null && ingredientA.length > 0) {
                        items[x][y] = ingredientA[0].getItem();
                        preferableItems.add(ingredientA[0].getItem());
                    }
                }
            }
        }

        public void craft() {
            GuiContainer screen = (GuiContainer) mc.currentScreen;
            for (int y = 0; y < items.length; y++) {
                for (int x = 0; x < items[y].length; x++) {
                    assert screen != null;
                    if (items[x][y] == null)
                        continue;
                    int slotId = searchInventory(items[x][y]);
                    if (slotId == -1) {
                        active = false;
                        mc.player.closeScreen();
                        HELPER.logDirect("cannot find all of the items    " + items[x][y].getDefaultInstance().getDisplayName());
                        clicks = new LinkedList<>();
                        return;
                    }

                    Slot from = screen.inventorySlots.
                            getSlot(slotId);

                    Slot to = screen.inventorySlots.getSlot((x + y * items.length) + 1);
//This stuff here means that it can be speeded up in the future.
                    //you have to make it so that it remembers what has already been clicked while doing the loop.
                    //so that more than 1 can be placed in the crafting table at a time.
                    int amount = 1;
                    int limit = Math.min(to.getSlotStackLimit(), from.getStack().getMaxStackSize());
                    int toMove = Math.min(amount, limit - getSlotContentCount(to));
                    int amountMoved = 0;

                    amountMoved += moveItemAmount(from, to, toMove);

                    int inventorySlot;
                    if (useInventory) {
                        inventorySlot = convertFromPlayerInventorySlot(from.slotNumber - 9);
                    } else {
                        inventorySlot = convertFromPlayerInventorySlot(from.slotNumber - 10);
                    }
//update our inventory.
                    ItemStack itemStack = inventory.get(inventorySlot);
                    itemStack.setCount(itemStack.getCount() - 1);
                    if (itemStack.getCount() <= 0) {
                        itemStack = Items.AIR.getDefaultInstance();
                    }
                    inventory.set(inventorySlot, itemStack);
                }

            }

        }
    }

}
