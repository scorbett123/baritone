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
import baritone.api.process.ICraftProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Recipe;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class CraftProcess extends BaritoneProcessHelper implements ICraftProcess {
    private final LinkedList<int[]> clicks = new LinkedList<>();
    boolean active = false;
    List<IRecipe> possibleRecipes = new ArrayList<>();
    int ticks = -1;
    BlockPos craftingTablePosition;
    Recipe recipeToCraft = new Recipe();
    boolean exit = false;
    int amount;

    public CraftProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        ticks++;
        mc.playerController.func_194338_a(mc.player.inventoryContainer.windowId, recipeToCraft.getRecipe(), false, mc.player);
        baritone.getInventoryBehavior().takeResultItem(mc.player.inventoryContainer);
        active = false;
        exit = false;
        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);

    }

    @Override
    public void onLostControl() {
        active = false;
        exit = false;
        clicks.clear();
    }

    @Override
    public String displayName0() {
        return "crafting " + ticks;
    }

    @Override
    public void craft(List<IRecipe> recipes, int amount) {
        craftingTablePosition = null;
        active = true;
        this.amount = amount;
        clicks.clear();
        IRecipe recipeToCraft = recipes.stream()
                .filter((recipe) -> {
                            Stream<Ingredient> ingredients = recipe.getIngredients().stream()
                                    .filter(ingredient -> Arrays.stream(ingredient.getMatchingStacks())
                                            .anyMatch(itemStack -> baritone.getInventoryBehavior().doesInventoryContain(itemStack.getItem()))
                                    );

                            return ingredients.count() == recipe.getIngredients().stream().filter(ingredient -> ingredient.getMatchingStacks().length != 0).count();
                        }
                ).findFirst().orElse(null);

        if (recipeToCraft == null) {
            HELPER.logDirect("You don't have the required items");
            return;
        }
        this.recipeToCraft.getFromIRecipe(recipeToCraft);
        generateClicks(this.recipeToCraft, amount);
        ticks = -1;

    }

    public void generateClicks(Recipe recipe, int amount) {
        NonNullList<ItemStack> inventory = mc.player.inventory.mainInventory;
        for (int k = 0; k < amount; k++) {
            if (recipe.canBeCraftedInInventory()) {
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        if (recipe.getGrid()[i][j] != null) {
                            int inInventory = baritone.getInventoryBehavior().searchInventory(recipe.getGrid()[i][j], inventory);
                            if (inInventory == -1) {
                                HELPER.logDirect("Don't have enough materials");
                                onLostControl();
                                return;
                            }
                            inventory = changeAmountAtSlot(inInventory, -1, inventory);
                            clicks.add(new int[]{convertToPlayerInventory(inInventory) + 9, i + j * 2 + 1});
                        }
                    }
                }
            } else {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        if (recipe.getGrid()[i][j] != null) {
                            int inInventory = baritone.getInventoryBehavior().searchInventory(recipe.getGrid()[i][j], inventory);
                            if (inInventory == -1) {
                                HELPER.logDirect("Don't have enough materials");
                                onLostControl();
                                return;
                            }
                            inventory = changeAmountAtSlot(inInventory, -1, inventory);
                            clicks.add(new int[]{convertToPlayerInventory(inInventory) + 10, i + j * 3 + 1});
                        }
                    }
                }
            }
        }
    }

    public int convertToPlayerInventory(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 9 * 3 : inventorySlot - 9;
    }

    public NonNullList<ItemStack> changeAmountAtSlot(int locationInInv, int diff, NonNullList<ItemStack> inventory) {
        ItemStack stack = inventory.get(locationInInv);
        stack.setCount(stack.getCount() + diff);
        if (stack.getCount() == 0) {
            stack = Items.AIR.getDefaultInstance();
        }
        inventory.set(locationInInv, stack);
        return inventory;
    }
}
