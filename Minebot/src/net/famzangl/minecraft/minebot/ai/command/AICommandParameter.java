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
package net.famzangl.minecraft.minebot.ai.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.minecraft.block.Block;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AICommandParameter {
	/**
	 * Subclasses need to have an empty constructor.
	 * @author michael
	 *
	 */
	public static abstract class BlockFilter {
		public abstract boolean matches(Block b);
	}
	
	public static class AnyBlockFilter extends BlockFilter {
		@Override
		public boolean matches(Block b) {
			return true;
		}
	}
	
	ParameterType type();

	String description();

	String fixedName() default "";
	
	boolean optional() default false;

	Class<? extends BlockFilter> blockFilter() default AnyBlockFilter.class;

	String relativeToSettingsFile() default "";
}
