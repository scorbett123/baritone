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
import baritone.api.utils.Recipe;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Stream;

public class CraftProcess extends BaritoneProcessHelper implements ICraftProcess {
    private final LinkedList<int[]> clicks = new LinkedList<>();
    boolean active = false;
    List<IRecipe> possibleRecipes = new ArrayList<>();
    int ticks = -1;
    BlockPos craftingTablePosition;
    Recipe recipeToCraft = new Recipe();

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
        if (recipeToCraft.canBeCraftedInInventory()) {
            if (clicks.size() > ticks) {
                int[] current = clicks.get(ticks);
                HELPER.logDirect(current[0] + "    " + current[1]);
                baritone.getInventoryBehavior().placeOneItem(current[0], current[1]);
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        } else {
            craftingTablePosition = MineProcess.searchWorld(new CalculationContext(baritone), new BlockOptionalMetaLookup(Blocks.CRAFTING_TABLE), 1, new ArrayList<>(), new ArrayList<>(), Collections.emptyList()).get(0);
            return new PathingCommand(new GoalGetToBlock(craftingTablePosition), PathingCommandType.SET_GOAL_AND_PATH);
        }
        baritone.getInventoryBehavior().takeResultItem();
        active = false;
        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);

    }

    @Override
    public void onLostControl() {
        active = false;
    }

    @Override
    public String displayName0() {
        return "crafting " + ticks;
    }

    @Override
    public void craft(List<IRecipe> recipes) {
        active = true;

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
        generateClicks(this.recipeToCraft);
        ticks = -1;

    }

    public void generateClicks(Recipe recipe) {
        if (recipe.canBeCraftedInInventory()) {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    if (recipe.getGrid()[i][j] != null)
                        clicks.add(new int[]{baritone.getInventoryBehavior().searchInventory(recipe.getGrid()[i][j]) + 9, i + j * 2 + 1});
                }
            }
        }
    }
}
