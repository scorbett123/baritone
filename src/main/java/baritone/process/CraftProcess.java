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
import baritone.utils.BaritoneProcessHelper;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.recipebook.RecipeList;
import net.minecraft.client.util.RecipeBookClient;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CraftProcess extends BaritoneProcessHelper implements ICraftProcess {
    private final LinkedList<int[]> clicks = new LinkedList<>();
    boolean active = false;
    List<IRecipe> possibleRecipes = new ArrayList<>();
    int ticks = -1;
    BlockPos craftingTablePosition;
    IRecipe recipeToCraft = null;
    boolean exit = false;
    int amount;
    RecipeList recipes = new RecipeList();

    public CraftProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    int ticksToRest = 0;
    GuiInventory gui;

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        ticks++;
        if (recipeToCraft.canFit(2, 2)) {
            //inventory crafting
            if (ticks < amount) {
                mc.playerController.func_194338_a(mc.player.inventoryContainer.windowId, recipeToCraft, false, mc.player);
            } else {
                baritone.getInventoryBehavior().takeResultItem(mc.player.inventoryContainer);
                active = false;
                onLostControl();
            }
        }
        exit = false;
        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);

    }

    @Override
    public void onLostControl() {
        active = false;
        exit = false;
        ticks = -1;
        clicks.clear();
    }

    @Override
    public String displayName0() {
        return "crafting " + ticks;
    }

    @Override
    public void craft(Item item, int amount) {
        RecipeItemHelper itemHelper = new RecipeItemHelper();
        mc.player.inventory.fillStackedContents(itemHelper, false);
        List<RecipeList> list = RecipeBookClient.ALL_RECIPES;
        list.forEach((p_193944_1_) ->
        {
            p_193944_1_.canCraft(itemHelper, 2, 2, mc.player.getRecipeBook());
        });
        List<RecipeList> list1 = Lists.newArrayList(list);
        list1.removeIf((p_193952_0_) ->
        {
            return !p_193952_0_.isNotEmpty();
        });
        list1.removeIf((p_193953_0_) ->
                !p_193953_0_.containsValidRecipes());
        list1.removeIf((p_193958_0_) ->
        {
            return !p_193958_0_.containsCraftableRecipes();
        });
        list1.removeIf(
                (recipeList -> {
                    if (recipeList.hasSingleResultItem()) {
                        return !(recipeList.getRecipes(true).get(0).getRecipeOutput().getItem() == item);

                    } else {
                        for (IRecipe recipe : recipeList.getRecipes()) {
                            if (recipe.getRecipeOutput().getItem() == item) {
                                recipeToCraft = recipe;
                                return true;
                            }
                        }
                    }
                    return true;
                }));

        active = true;

        if (list1.size() > 0) {
            recipeToCraft = list1.get(0).getRecipes(true).get(0);
        }
    }
}
