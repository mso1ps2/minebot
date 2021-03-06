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
package net.famzangl.minecraft.minebot.ai.commands;

import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.BlockWhitelist;
import net.famzangl.minecraft.minebot.ai.command.AICommand;
import net.famzangl.minecraft.minebot.ai.command.AICommandInvocation;
import net.famzangl.minecraft.minebot.ai.command.AICommandParameter;
import net.famzangl.minecraft.minebot.ai.command.AICommandParameter.BlockFilter;
import net.famzangl.minecraft.minebot.ai.command.ParameterType;
import net.famzangl.minecraft.minebot.ai.command.SafeStrategyRule;
import net.famzangl.minecraft.minebot.ai.path.MineBySettingsPathFinder;
import net.famzangl.minecraft.minebot.ai.path.MineSinglePathFinder;
import net.famzangl.minecraft.minebot.ai.path.OrebfuscatedMinePathFinder;
import net.famzangl.minecraft.minebot.ai.strategy.AIStrategy;
import net.famzangl.minecraft.minebot.ai.strategy.PathFinderStrategy;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

@AICommand(helpText = "Mines for ores.\n"
		+ "Uses the minebot.properties file to find ores."
		+ "If blockName is given, only the block that is given is searched for.", name = "minebot")
public class CommandMine {

	public static BlockWhitelist MINEABLE = new BlockWhitelist(Blocks.air,
			Blocks.lava, Blocks.flowing_lava, Blocks.water,
			Blocks.flowing_water, Blocks.waterlily, Blocks.bedrock).invert();

	@AICommandInvocation(safeRule = SafeStrategyRule.DEFEND_MINING)
	public static AIStrategy run(
			AIHelper helper,
			@AICommandParameter(type = ParameterType.FIXED, fixedName = "mine", description = "") String nameArg) {
		return new PathFinderStrategy(new MineBySettingsPathFinder(
				helper.getLookDirection(), helper.getPlayerPosition().getY()),
				"Mining ores");
	}

	public static final class MineBlockFilter extends BlockFilter {
		@Override
		public boolean matches(Block b) {
			return MINEABLE.contains(b);
		}
	}

	@AICommandInvocation(safeRule = SafeStrategyRule.DEFEND_MINING)
	public static AIStrategy run(
			AIHelper helper,
			@AICommandParameter(type = ParameterType.FIXED, fixedName = "mine", description = "") String nameArg,
			@AICommandParameter(type = ParameterType.BLOCK_NAME, description = "The block to mine.", blockFilter = MineBlockFilter.class) Block blockName) {
		return new PathFinderStrategy(new MineSinglePathFinder(blockName,
				helper.getLookDirection(), helper.getPlayerPosition().getY()),
				"Mining " + blockName.getLocalizedName());
	}

	@AICommandInvocation(safeRule = SafeStrategyRule.DEFEND_MINING)
	public static AIStrategy run(
			AIHelper helper,
			@AICommandParameter(type = ParameterType.FIXED, fixedName = "mine", description = "") String nameArg,
			@AICommandParameter(type = ParameterType.FIXED, fixedName = "orebfuscated", description = "") String orebfuscated) {
		return new PathFinderStrategy(new OrebfuscatedMinePathFinder(
				helper.getLookDirection(), helper.getPlayerPosition().getY()),
				"Mining ores");
	}

}
