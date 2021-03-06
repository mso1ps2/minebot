/*******************************************************************************
 * This file is part of Minebot.
 *
 * Minebot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Minebot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Minebot.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package net.famzangl.minecraft.minebot.ai.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map.Entry;

import net.famzangl.minecraft.minebot.Pos;
import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.BlockWhitelist;
import net.famzangl.minecraft.minebot.ai.scanner.BlockRangeScanner.BlockHandler;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public abstract class RangeBlockHandler<ReachData> implements BlockHandler {
	private static final BlockWhitelist THROUGH_REACHABLE = new BlockWhitelist(
			Blocks.air, Blocks.torch);
	private final Hashtable<BlockPos, ArrayList<ReachData>> reachable = new Hashtable<BlockPos, ArrayList<ReachData>>();

	@Override
	public void scanningDone(AIHelper helper) {
		updatePositionCache(helper);
	}

	protected abstract Collection<Entry<BlockPos, ReachData>> getTargetPositions();

	private void updatePositionCache(AIHelper helper) {
		reachable.clear();
		for (Entry<BlockPos, ReachData> c : getTargetPositions()) {
			addPositionToCache(helper, c.getKey(), c.getValue());
		}
	}

	protected void addPositionToCache(AIHelper helper, BlockPos pos, ReachData c) {
		for (EnumFacing d : new EnumFacing[] { EnumFacing.NORTH,
				EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
			addPositions(helper, pos, c, d);
		}
	}

	private void addPositions(AIHelper helper, BlockPos pos, ReachData c,
			EnumFacing d) {
		int dvertMax = 4;
		for (int dhor = 0; dhor < 4; dhor++) {
			int y = pos.getY() - dhor;
			for (int dvert = 1; dvert <= dvertMax; dvert++) {
				int x = pos.getX() + dvert * d.getFrontOffsetX();
				int z = pos.getZ() + dvert * d.getFrontOffsetZ();
				if (!THROUGH_REACHABLE.contains(helper.getBlockId(x, y, z))) {
					dvertMax = dvert;
				} else if (dvert > 1) {
					Pos allowed = new Pos(x, y, z);
					addReachable(allowed, c);
				}
			}
		}
	}

	private void addReachable(BlockPos allowed, ReachData c) {
		ArrayList<ReachData> list = reachable.get(allowed);
		if (list == null) {
			list = new ArrayList<ReachData>();
			reachable.put(allowed, list);
		}
		list.add(c);
	}

	public ArrayList<ReachData> getReachableForPos(BlockPos pos) {
		return reachable.get(pos);
	}

}
