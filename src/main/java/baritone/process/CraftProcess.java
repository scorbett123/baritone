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
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CraftProcess extends BaritoneProcessHelper implements ICraftProcess {
    boolean active = false;
    Item item;
    int needed = -1, currentAmount = 0;
    List<IRecipe> posibilities;
    BlockPos craftingTablePosition = null;
    CraftingRecipe recipe = null;
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
      HELPER.logDirect(posibilities.toArray().length+"");
      posibilities.forEach(recipe ->recipe.getIngredients().stream().forEach(ingredient -> Arrays.stream(ingredient.getMatchingStacks()).forEach(a ->HELPER.logDirect(a.getDisplayName()))));
      craftingTablePosition=null;
recipe=decideBest(posibilities);
HELPER.logDirect(recipe.toString());
    }

    public List<IRecipe> getPossibility() {
        try {
            return CraftingManager.REGISTRY.getKeys().stream()
                    .map(id -> CraftingManager.REGISTRY.getObject(id))
                    .filter(recipe -> {
                        ItemStack out = recipe.getRecipeOutput();


                        return out != null && out.getItem().equals(item);
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
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if(active) {
            if (craftingTablePosition == null) {
                List<BlockPos> positions = MineProcess.searchWorld(new CalculationContext(baritone), new BlockOptionalMetaLookup(Blocks.CRAFTING_TABLE), 1, new ArrayList<>(), new ArrayList<>(), Collections.emptyList());
                craftingTablePosition = positions.get(0);
                return new PathingCommand(new GoalGetToBlock(craftingTablePosition),PathingCommandType.SET_GOAL_AND_PATH);
            }
if(craftingTablePosition.distanceSq(mc.player.getPosition())==2){





}






            return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);
        }else{
        return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);}
    }

    @Override
    public void onLostControl() {
active=false;
    }

    @Override
    public String displayName0() {
        return null;
    }


public class CraftingRecipe{
    @Override
    public String toString() {
generateItems();
        StringBuilder builder = new StringBuilder();
        for (int x = 0; x <3 ; x++) {
            for (int y = 0; y < 3; y++) {
                if(items[x][y]!=null){
                builder.append(items[x][y].getItemStackDisplayName(items[x][y].getDefaultInstance())).append(" item ");}
                else{
                    builder.append("  null  item  ");
                }
            }
        }


        return builder.toString();
    }

    public Ingredient[][] ingredients= new Ingredient[3][3];
        public Item[][] items = new Item[3][3];
        public CraftingRecipe(IRecipe recipe){


            HELPER.logDirect("finding possibility");
            if (recipe instanceof ShapedRecipes) {
                ShapedRecipes shapedRecipes = (ShapedRecipes) recipe;
                HELPER.logDirect("recipe is shaped");
                int width = shapedRecipes.getWidth();
                int height = shapedRecipes.getHeight();


                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        Ingredient itemStack = shapedRecipes.getIngredients().get(x + y * width);

                        ingredients[x][y]= itemStack;
                    }
                }


            } else {

                throw new IllegalArgumentException("Cannot (yet) craft " + recipe);
            }

        }

int loop = 0;
        int x,y;
        List<ItemStack>preferableItems = new ArrayList<>();
    ItemStack[] ingredientA = null;
    public void generateItems(){
            for ( x = 0; x < items.length; x++) {
                for ( y = 0; y < items[x].length; y++) {

                    Ingredient ingredient= ingredients[x][y];
HELPER.logDirect("generating items");
if(ingredient==null){
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
                        if(items[x][y]!=null){
                            break;
                        }
                      preferableItems.forEach(itemStack1 -> {
                          if(itemStack.getItem()==itemStack1.getItem()){
                              items[x][y]=itemStack.getItem();
                          }
                      });

                    }
                    if(items[x][y]==null){
                        if(ingredientA.length>0)
items[x][y] = ingredientA[0].getItem();
                    }
                }

            }
        }


}
    public CraftingRecipe decideBest(List<IRecipe> recipes){

        Stream<CraftingRecipe> recipesC = recipes.stream().map(CraftingRecipe::new);
       Optional<CraftingRecipe> recipe2 = recipesC.findAny();
        return recipe2.orElse(null);
    }


    public int searchInventory(Item item){
        NonNullList<ItemStack> inventory = mc.player.inventory.mainInventory;
        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger ints = new AtomicInteger(-1);
        AtomicInteger counter = new AtomicInteger(0);

        inventory.forEach(itemStack -> {
            if(ints.get()==-1){
            if(itemStack.getItem()==item){
                found.set(true);
                ints.set(counter.get());
                counter.set(counter.get()+1);}
            }
        });
        return ints.get();
    }
}
