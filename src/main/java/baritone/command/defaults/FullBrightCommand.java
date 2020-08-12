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
import baritone.api.command.argument.ICommandArgument;
import baritone.api.command.exception.CommandException;
import baritone.api.utils.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

import java.util.List;
import java.util.stream.Stream;

public class FullBrightCommand extends Command {
    /**
     * Creates a new Baritone control command.
     *
     * @param baritone

     */
    protected FullBrightCommand(IBaritone baritone) {
        super(baritone,"fullBright" );
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(2);

        if(args.has(1)){
            ICommandArgument iarg = args.getArgs().get(1);
            approachGamma(Double.parseDouble(iarg.getValue()));
        }else{
            GameSettings options = Helper.mc.gameSettings;
            if(options.gammaSetting ==16){
                approachGamma(0.5);
            }else{
                approachGamma(16);
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
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
    private void approachGamma(double target)
    {
        GameSettings options = Helper.mc.gameSettings;
        boolean doFade =
                false;

        if(!doFade || Math.abs(options.gammaSetting - target) <= 0.5)
        {
            options.gammaSetting = (float)target;
            return;
        }

        if(options.gammaSetting < target)
            options.gammaSetting += 0.5;
        else
            options.gammaSetting -= 0.5;
    }
}
