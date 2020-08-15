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
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.Helper;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CraftProcess extends BaritoneProcessHelper implements ICraftProcess {
    boolean active = false;
    Item item;
    int needed = -1, currentAmount = 0;
    List<IRecipe> posibilities;
    BlockPos craftingTablePosition = null;
    CraftingRecipe recipe = null;
    int DELAY = 0;
    private LinkedList<AbstractClickAction> clicks = null;

    public CraftProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void craft(Item item, int amount) {
        active = true;
        this.item = item;
        this.needed = amount;
        this.currentAmount = 0;
        posibilities = getPossibility();
        HELPER.logDirect(posibilities.toArray().length + "");
        // posibilities.forEach(recipe -> recipe.getIngredients().forEach(ingredient -> Arrays.stream(ingredient.getMatchingStacks()).forEach(a ->HELPER.logDirect(a.getDisplayName()))));
        craftingTablePosition = null;
        recipe = decideBest(posibilities);
        clicks = null;
        recipe.generateItems();
        HELPER.logDirect(recipe.toString());
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

    boolean removingResult = false;
    int amountRemoved = 0;

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {

        if (removingResult) {
            if (amountRemoved >= needed) {
                removingResult = false;
                amountRemoved = 0;
                active = false;
                return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);
            }
            removeResult();
            amountRemoved++;
        }


        if (clicks != null) {

            if (clicks.size() > 0) {
                HELPER.logDirect(clicks.size() + "   " + clicks.peekFirst().slotNumber);

                clicks.remove().click();
            } else {
                HELPER.logDirect(currentAmount + "   " + needed);
                removingResult = true;


            }
        }
        if (active) {

            if (baritone.getInputOverrideHandler().isInputForcedDown(Input.CLICK_RIGHT)) {
                baritone.getInputOverrideHandler().clearAllKeys();
            }

            if (craftingTablePosition == null) {
                List<BlockPos> positions = MineProcess.searchWorld(new CalculationContext(baritone), new BlockOptionalMetaLookup(Blocks.CRAFTING_TABLE), 1, new ArrayList<>(), new ArrayList<>(), Collections.emptyList());
                craftingTablePosition = positions.get(0);
                return new PathingCommand(new GoalGetToBlock(craftingTablePosition), PathingCommandType.SET_GOAL_AND_PATH);
            } else {
                // HELPER.logDirect(craftingTablePosition.distanceSq(mc.player.getPosition())+"");
                if (craftingTablePosition.distanceSq(mc.player.getPosition()) <= 6) {

                    if (!(Helper.mc.currentScreen instanceof GuiCrafting)) {

                        Optional<Rotation> rot = RotationUtils.reachable(ctx, craftingTablePosition);
                        if (rot.isPresent() && isSafeToCancel) {
                            baritone.getLookBehavior().updateTarget(rot.get(), true);
                        }

                        baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);


                    } else {
                        if (recipe != null) {
                            recipe.generateItems();
                            clicks = new LinkedList<>();
                            for(currentAmount = currentAmount; currentAmount<needed ;currentAmount++){
                                recipe.craft();
                            }
                            recipe=null;
                        }
                    }


                }

}
            return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);
        }else{
        return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);}
    }

    @Override
    public void onLostControl() {
active=false;
        if (clicks != null) {
            clicks.clear();
        }
    }

    @Override
    public String displayName0() {
        return null;
    }

    public void removeResult() {
        new LeftClickAction(0).click();
        HELPER.logDirect(findSlotFor(this.item)+"");
        new LeftClickAction(findSlotFor(this.item)).click();
    }

    public int findSlotFor(Item item) {
        NonNullList<ItemStack> mainInventory = mc.player.inventory.mainInventory;
        for (int i = 0; i < mainInventory.size(); i++) {


            if (
                    mainInventory.get(i).getItem() == item) {
                return convertPlayerInventorySlot(i)+10;
            }


        }

        for (int i = 0; i < mainInventory.size(); i++) {


            if (
                    mainInventory.get(i).getItem() == Items.AIR) {
                return convertPlayerInventorySlot(i)+10;
            }


        }
        return -1;
    }

    public int searchInventory(Item item) {
        NonNullList<ItemStack> mainInventory = mc.player.inventory.mainInventory;
        int inventorySlot = -1;
        for (int i = 0; i < mainInventory.size(); i++) {

            if (item != null) {
                if (item.equals(
                        mainInventory.get(i).getItem())) {
                    inventorySlot = i;
                }
            }
        }
        if (inventorySlot < 0) {
            return -1;
        }

        return 10 + convertPlayerInventorySlot(inventorySlot);
    }

    public CraftingRecipe decideBest(List<IRecipe> recipes) {

        Stream<CraftingRecipe> recipesC = recipes.stream().map(CraftingRecipe::new);
        Optional<CraftingRecipe> recipe2 = recipesC.findAny();
        return recipe2.orElse(null);
    }

    private int moveAll(Slot from, Slot to) {
        int oldCount = getSlotContentCount(to);

        addClick(new LeftClickAction(from.slotNumber));
        addClick(new LeftClickAction(to.slotNumber));

        return oldCount;
    }

    private int moveHalf(Slot from, Slot to) {
        int oldCount = getSlotContentCount(to);

        addClick(new RightClickAction(from.slotNumber));
        addClick(new LeftClickAction(to.slotNumber));

        return oldCount / 2;
    }

    private int moveStackPart(Slot from, Slot to, int count) {
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

    int getSlotContentCount(Slot slot) {
        if (slot == null) {
            return 0;
        }
        return slot.getHasStack() ? slot.getStack().getCount() : 0;
    }

    int convertPlayerInventorySlot(int inventorySlot) {
        // Offset: 10 blocks.
        if (inventorySlot < 9) {
            return inventorySlot + 9 * 3;
        } else {
            return inventorySlot - 9;
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
        ClickType getClickType() {
            return ClickType.PICKUP;
        }

        @Override
        int getClickKey() {
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
        ClickType getClickType() {
            return ClickType.PICKUP;
        }

        @Override
        int getClickKey() {
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
        int loop = 0;
        int x, y;
        List<ItemStack> preferableItems = new ArrayList<>();

        public CraftingRecipe(IRecipe recipe) {


            if (recipe instanceof ShapedRecipes) {
                ShapedRecipes shapedRecipes = (ShapedRecipes) recipe;

                int width = shapedRecipes.getWidth();
                int height = shapedRecipes.getHeight();


                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        Ingredient itemStack = shapedRecipes.getIngredients().get(x + y * width);

                        ingredients[x][y] = itemStack;
                    }
                }


            } else {

                throw new IllegalArgumentException("Cannot (yet) craft " + recipe);
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
                HELPER.logDirect(builder1.toString());
                builder.append(builder1);
            }


            return builder.toString();
        }

        ItemStack[] ingredientA = null;

        public void generateItems() {

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
                    ingredientA = new ItemStack[items2.toArray().length];
                    for (int i = 0; i < ingredientA.length; i++) {


                        ingredientA[i] = items2.get(i);
                    }
                    for (ItemStack itemStack : ingredientA) {
                        if (items[x][y] != null) {
                            break;
                        }
                        preferableItems.forEach(itemStack1 -> {
                            if (itemStack.getItem() == itemStack1.getItem()) {
                                items[x][y] = itemStack.getItem();
                            }
                        });

                    }
                    if (items[x][y] == null) {
                        if (ingredientA.length > 0)
                            items[x][y] = ingredientA[0].getItem();
                    }
                }

            }
        }

        public void craft() {

            final GuiContainer screen = (GuiContainer) mc.currentScreen;

            for (int y = 0; y < items.length; y++) {
                for (int x = 0; x < items[y].length; x++) {


                    int slotid = searchInventory(items[x][y]);
                    Slot from = null;
                    if (slotid != -1) {
                        from = screen.inventorySlots.getSlot(slotid);
                    }
                    Slot to = screen.inventorySlots.getSlot((x + y * 3) + 1);

                    int amount = 1;


                    if (from == null) {
                        HELPER.logDirect("from is null");
                        continue;

                    }

                    int limit = Math.min(to.getSlotStackLimit(), from.getStack().getMaxStackSize());
                    int missing = Math.min(amount, limit - getSlotContentCount(to));

                    HELPER.logDirect(x + "  :   " + y);
                    if (getSlotContentCount(from) <= missing && getSlotContentCount(from) > 0) {
                        missing -= moveAll(from, to);
                    } else if (getSlotContentCount(from) - getSlotContentCount(from) / 2 <= missing
                            && getSlotContentCount(from) > 0) {
                        missing -= moveHalf(from, to);
                    } else if (missing > 0 && getSlotContentCount(from) > 0) {
                        missing -= moveStackPart(from, to, missing);
                    }

                }

            }
           // clicks.stream().forEach(click -> HELPER.logDirect(click.toString()));
        }
    }
}
