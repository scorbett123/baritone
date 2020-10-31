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

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.ItemById;
import baritone.api.command.exception.CommandException;
import baritone.api.utils.Recipe;
import baritone.cache.WorldScanner;
import net.minecraft.item.Item;

import java.util.List;
import java.util.stream.Stream;

public class CraftCommand extends Command {

    public CraftCommand(IBaritone baritone) {
        super(baritone, "craft");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(2);
        args.requireMin(1);

        Item toCraft;
        int quantity;
        toCraft = args.getDatatypeFor(ItemById.INSTANCE);
        quantity = args.getArgs().size() == 0 ? 1 : args.getAs(Integer.class);

        WorldScanner.INSTANCE.repack(ctx);
        if (toCraft == null) {
            HELPER.logDirect("cannot find requested item");
            return;
        }

        HELPER.logDirect(String.format("crafting %s %s%s", quantity, toCraft.getDefaultInstance().getDisplayName(), quantity == 1 ? "" : "s"));
        // logDirect("the id is " + Item.getIdFromItem(toCraft));
        baritone.getCraftProcess().craft(Recipe.getRecipesForItem(toCraft), quantity);
    }


    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return null;
    }

    @Override
    public String getShortDesc() {
        return null;
    }

    @Override
    public List<String> getLongDesc() {
        return null;
    }
}
