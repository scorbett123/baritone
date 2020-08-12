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

package baritone.api.process;

import baritone.api.schematic.ISchematic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.util.List;

/**
 * @author Brady
 * @since 1/15/2019
 */
public interface ICraftProcess extends IBaritoneProcess {



    /**
     * Requests to craft
     *
     * @param item     the item to craft
     * @param amount   the amount to craft
     */
    void craft(Item item, int amount);
    /**
     * requests to craft
     *
     * @param item     the item to craft
     */
    default void craft(Item item) {
      craft(item,1);
    }





}
