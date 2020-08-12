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
import baritone.api.utils.Helper;
import baritone.cache.WorldData;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CraftProcess extends BaritoneProcessHelper implements ICraftProcess {
    boolean active = false;
    Item item;
    int needed = -1, currentAmount = 0;
    List<CraftingPossibility> posibilities;
    public CraftProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void craft(Item item, int amount) {
        active = true;
       this.item=item;
       this.needed=amount;
       this.currentAmount=0;
      posibilities = getPossibility();
    }

    public List<CraftingPossibility> getPossibility() {
        try {
            return CraftingManager.REGISTRY.getKeys().stream()
                    .map(id -> CraftingManager.REGISTRY.getObject(id))
                    .filter(recipe -> {
                        ItemStack out = recipe.getRecipeOutput();
                        return out != null && out.equals(item);
                    })
                    .map(CraftingPossibility::new)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            System.err.println("Cannot craft: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        MineProcess.searchWorld(baritone.ca);






        return null;
    }

    @Override
    public void onLostControl() {

    }

    @Override
    public String displayName0() {
        return null;
    }


    public static final class CraftingPossibility {
        public static final int SUBTYPE_IGNORED = 32767;

        private final Item[][][]slots = new Item[3][3][];

        public CraftingPossibility(IRecipe recipe) {

            if (recipe instanceof ShapedRecipes) {
                ShapedRecipes shapedRecipes = (ShapedRecipes) recipe;

                int width = shapedRecipes.getWidth();
                int height = shapedRecipes.getHeight();


                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        Ingredient itemStack = shapedRecipes.getIngredients().get(x + y * width);
                        Item[] items =  this.slots[x][y];
                        Arrays.stream(itemStack.getMatchingStacks()).forEach(item -> items[items.length] = item.getItem());
                        this.slots[x][y] = items;
                    }
                }


            } else {

                throw new IllegalArgumentException("Cannot (yet) craft " + recipe);
            }
        }
}}
