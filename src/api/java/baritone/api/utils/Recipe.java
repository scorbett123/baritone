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

package baritone.api.utils;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Recipe {
    private Item[][] itemGrid = new Item[3][3];
    private Ingredient[][] ingredientGrid = new Ingredient[3][3];
    private boolean canBeCraftedInInventory = false;
    private IRecipe recipe = null;

    public static List<IRecipe> getRecipesForItem(Item item) {
        return CraftingManager.REGISTRY.getKeys().stream()
                .map(CraftingManager.REGISTRY::getObject).filter(Objects::nonNull)
                .filter(recipe ->
                        recipe.getRecipeOutput().getItem() == item
                )
                .collect(Collectors.toList());
    }

    public void getFromIRecipe(IRecipe recipe) {
        this.recipe = recipe;
        if (recipe instanceof ShapedRecipes) {
            canBeCraftedInInventory = recipe.canFit(2, 2);

            if (recipe.canFit(2, 2)) {
                canBeCraftedInInventory = true;
                ingredientGrid = new Ingredient[2][2];
                itemGrid = new Item[2][2];
            }
            int width = ((ShapedRecipes) recipe).getWidth();
            int height = ((ShapedRecipes) recipe).getHeight();

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Ingredient itemStack = recipe.getIngredients().get(x + y * width);

                    ingredientGrid[x][y] = itemStack;
                    if (itemStack.getMatchingStacks().length > 0)
                        itemGrid[x][y] = itemStack.getMatchingStacks()[0].getItem();
                    else
                        itemGrid[x][y] = Items.AIR;
                }
            }


        }
    }

    public Item[][] getGrid() {
        return itemGrid;
    }

    public void setGrid(Item[][] grid) {
        this.itemGrid = grid;
    }

    public boolean canBeCraftedInInventory() {
        return canBeCraftedInInventory;
    }

    public IRecipe getRecipe() {
        return recipe;
    }

    public void setRecipe(IRecipe recipe) {
        getFromIRecipe(recipe);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        for (Item[] itemStackArr : itemGrid) {
            string.append("\n");
            for (Item j : itemStackArr) {
                if (j != null)
                    string.append(j.getDefaultInstance().getDisplayName()).append("  ");
            }
        }
        return string.toString();
    }
}
