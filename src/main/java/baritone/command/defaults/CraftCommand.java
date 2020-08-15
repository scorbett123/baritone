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
import baritone.api.command.datatypes.BlockById;
import baritone.api.command.datatypes.ItemById;
import baritone.api.command.exception.CommandException;
import baritone.cache.WorldScanner;
import net.minecraft.item.Item;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CraftCommand extends Command {


    /**
     * Creates a new Baritone control command.
     *
     * @param baritone it is just needed to say what baritone to use.
     */
    protected CraftCommand(IBaritone baritone) {
        super(baritone, "craft");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(2);
        args.requireMin(1);
        Item toCraft = null;
        int quantity = 1;
       HELPER.logDirect( args.getArgs().toArray().length+"");
        if(args.getArgs().toArray().length==1){

            toCraft = args.getDatatypeFor(ItemById.INSTANCE);
HELPER.logDirect("args has one");
        }else if (args.getArgs().toArray().length==2){
            HELPER.logDirect("args has two");
            toCraft = args.getDatatypeFor(ItemById.INSTANCE);
            quantity = args.getAs(Integer.class);
        }
        WorldScanner.INSTANCE.repack(ctx);
        logDirect("Crafting " + quantity+ toCraft);

        baritone.getCraftProcess().craft(toCraft,quantity);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return args.tabCompleteDatatype(BlockById.INSTANCE);
    }

    @Override
    public String getShortDesc() {
        return "Craft some items";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "This lets you craft items",

                "> craft piston - crafts a piston."

        );
    }
}
