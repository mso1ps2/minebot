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

import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.minecraft.util.BlockPos;

public class BlockRangeScanner {
	private static final int HORIZONTAL_SCAN = 100;
	private static final int VERTICAL_SCAN = 20;
	private final BlockPos center;
	
	public interface BlockHandler {
		int[] getIds();

		void scanningDone(AIHelper helper);

		void scanBlock(AIHelper helper, int id, int x, int y, int z);
		
	}
	
	private final BlockHandler[] handlersCache = new BlockHandler[4096];
	
	private final ArrayList<BlockHandler> handlers = new ArrayList<BlockHandler>();
	
	private boolean scaningFinished;

	public BlockRangeScanner(BlockPos center) {
		this.center = center;
	}
	
	public void addHandler(BlockHandler h) {
		handlers.add(h);
		for (int i : h.getIds()) {
			handlersCache[i] = h;
		}
	}

	public void scanArea(AIHelper helper) {
		for (int y = center.getY() - VERTICAL_SCAN; y <= center.getY() + VERTICAL_SCAN; y++) {
			for (int z = center.getZ() - HORIZONTAL_SCAN; z <= center.getZ()
					+ HORIZONTAL_SCAN; z++) {
				for (int x = center.getX() - HORIZONTAL_SCAN; x <= center.getX()
						+ HORIZONTAL_SCAN; x++) {
					int id = helper.getBlockId(x, y, z);					
					BlockHandler handler = handlersCache[id];
					if (handler != null) {
						handler.scanBlock(helper, id, x, y, z);
					}
				}
			}
		}
		for (BlockHandler handler : handlers) {
			handler.scanningDone(helper);
		}
		
		scaningFinished = true;
	}


	public void startAsync(final AIHelper helper) {
		new Thread("Block range finder") {
			@Override
			public void run() {
				try {
				scanArea(helper);
				} catch (Throwable t) {
					t.printStackTrace();
					scaningFinished = true;
				}
			};
		}.start();
	}

	public boolean isScaningFinished() {
		return scaningFinished;
	}
}
