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
package net.famzangl.minecraft.minebot.ai;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Map.Entry;

import net.famzangl.minecraft.minebot.Pos;
import net.famzangl.minecraft.minebot.ai.command.AIChatController;
import net.famzangl.minecraft.minebot.ai.command.IAIControllable;
import net.famzangl.minecraft.minebot.ai.net.MinebotNetHandler;
import net.famzangl.minecraft.minebot.ai.render.BuildMarkerRenderer;
import net.famzangl.minecraft.minebot.ai.render.PosMarkerRenderer;
import net.famzangl.minecraft.minebot.ai.strategy.AIStrategy;
import net.famzangl.minecraft.minebot.ai.strategy.AIStrategy.TickResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;

import org.lwjgl.input.Keyboard;

/**
 * The main class that handles the bot.
 * 
 * @author michael
 * 
 */
public class AIController extends AIHelper implements IAIControllable {
	private final static Hashtable<KeyBinding, AIStrategyFactory> uses = new Hashtable<KeyBinding, AIStrategyFactory>();

	protected static final KeyBinding stop = new KeyBinding("Stop",
			Keyboard.getKeyIndex("N"), "Command Mod");
	protected static final KeyBinding ungrab = new KeyBinding("Ungrab",
			Keyboard.getKeyIndex("U"), "Command Mod");

	static {
		// final KeyBinding mine = new KeyBinding("Farm ores",
		// Keyboard.getKeyIndex("K"), "Command Mod");
		// final KeyBinding lumberjack = new KeyBinding("Farm wood",
		// Keyboard.getKeyIndex("J"), "Command Mod");
		// final KeyBinding build_rail = new KeyBinding("Build Minecart tracks",
		// Keyboard.getKeyIndex("H"), "Command Mod");
		// final KeyBinding mobfarm = new KeyBinding("Farm mobs",
		// Keyboard.getKeyIndex("M"), "Command Mod");
		// final KeyBinding plant = new KeyBinding("Plant seeds",
		// Keyboard.getKeyIndex("P"), "Command Mod");
		// uses.put(mine, new MineStrategy());
		// uses.put(lumberjack, new LumberjackStrategy());
		// uses.put(build_rail, new LayRailStrategy());
		// uses.put(mobfarm, new EnchantStrategy());
		// uses.put(plant, new PlantStrategy());
		// ClientRegistry.registerKeyBinding(mine);
		// ClientRegistry.registerKeyBinding(lumberjack);
		// ClientRegistry.registerKeyBinding(build_rail);
		// ClientRegistry.registerKeyBinding(mobfarm);
		// ClientRegistry.registerKeyBinding(plant);
		ClientRegistry.registerKeyBinding(stop);
		ClientRegistry.registerKeyBinding(ungrab);
	}

	private boolean dead;
	private AIStrategy currentStrategy;

	private String strategyDescr = "";
	private final Object strategyDescrMutex = new Object();

	private AIStrategy requestedStrategy;

	private boolean nextPosIsPos2;

	private PosMarkerRenderer markerRenderer;

	private boolean skipNextTick;

	private MouseHelper oldMouseHelper;

	private BuildMarkerRenderer buildMarkerRenderer;

	public AIController() {
		AIChatController.getRegistry().setControlled(this);
	}

	@SubscribeEvent
	public void connect(ClientConnectedToServerEvent e) {
		MinebotNetHandler.inject(this, e.manager, e.handler);
	}
	
	/**
	 * Checks if the Bot is active and what it should do.
	 * 
	 * @param evt
	 */
	@SubscribeEvent
	public void onPlayerTick(ClientTickEvent evt) {
		if (evt.phase != ClientTickEvent.Phase.START
				|| getMinecraft().thePlayer == null) {
			return;
		}
		if (skipNextTick) {
			skipNextTick = false;
			return;
		}
		testUngrabMode();
		invalidateObjectMouseOver();
		resetAllInputs();
		invalidateChunkCache();

		if (ungrab.isPressed()) {
			doUngrab = true;
		}

		AIStrategy newStrat;
		if (dead || stop.isPressed() || stop.isKeyDown()) {
			deactivateCurrentStrategy();
			dead = false;
		} else if ((newStrat = findNewStrategy()) != null) {
			deactivateCurrentStrategy();
			currentStrategy = newStrat;
			System.out.println("Using new root strategy: " + newStrat);
			currentStrategy.setActive(true, this);
		}

		if (currentStrategy != null) {
			TickResult result = null;
			for (int i = 0; i < 100; i++) {
				result = currentStrategy.gameTick(this);
				if (result != TickResult.TICK_AGAIN) {
					break;
				}
			}
			if (result == TickResult.ABORT || result == TickResult.NO_MORE_WORK) {
				dead = true;
			}
			synchronized (strategyDescrMutex) {
				strategyDescr = currentStrategy.getDescription(this);
			}
		} else {
			synchronized (strategyDescrMutex) {
				strategyDescr = "";
			}
		}
	}

	private void deactivateCurrentStrategy() {
		if (currentStrategy != null) {
			currentStrategy.setActive(false, this);
		}
		currentStrategy = null;
	}

	@SubscribeEvent
	public void drawHUD(RenderTickEvent event) {
		if (event.phase != Phase.END) {
			return;
		}
		if (doUngrab) {
			System.out.println("Un-grabbing mouse");
			// TODO: Reset this on grab
			getMinecraft().gameSettings.pauseOnLostFocus = false;
			// getMinecraft().mouseHelper.ungrabMouseCursor();
			if (getMinecraft().inGameHasFocus) {
				startUngrabMode();
			}
			doUngrab = false;
		}

		try {
			// Dynamic 1.7.2 / 1.7.10 fix.
			ScaledResolution res;
			final Constructor<?> method = ScaledResolution.class
					.getConstructors()[0];
			final Object arg1 = method.getParameterTypes()[0] == Minecraft.class ? getMinecraft()
					: getMinecraft().gameSettings;
			res = (ScaledResolution) method.newInstance(arg1,
					getMinecraft().displayWidth, getMinecraft().displayHeight);

			String[] str;
			synchronized (strategyDescrMutex) {
				str = (strategyDescr == null ? "?" : strategyDescr).split("\n");
			}
			int y = 10;
			for (String s : str) {
				getMinecraft().fontRendererObj.drawStringWithShadow(
						s,
						res.getScaledWidth()
								- getMinecraft().fontRendererObj
										.getStringWidth(s) - 10, y, 16777215);
				y += 15;
			}
		} catch (final InstantiationException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private synchronized void startUngrabMode() {
		getMinecraft().mouseHelper.ungrabMouseCursor();
		getMinecraft().inGameHasFocus = true;
		oldMouseHelper = getMinecraft().mouseHelper;
		getMinecraft().mouseHelper = new MouseHelper() {
			@Override
			public void mouseXYChange() {
			}

			@Override
			public void grabMouseCursor() {
			}

			@Override
			public void ungrabMouseCursor() {
			}
		};
	}

	private synchronized void testUngrabMode() {
		if (oldMouseHelper != null) {
			if (userTookOver()) {
				System.out.println("Preparing to re-grab the mouse.");
				// Tell minecraft what really happened.
				getMinecraft().mouseHelper = oldMouseHelper;
				getMinecraft().inGameHasFocus = false;
				getMinecraft().setIngameFocus();
				oldMouseHelper = null;
			}
		}
	}

	// FIXME: Find alternative
	// @SubscribeEvent
	// public void resetOnGameEnd(GuiOpenEvent unload) {
	// if (unload.gui instanceof GuiMainMenu) {
	// System.out.println("Unloading world.");
	// dead = true;
	// buildManager.reset();
	// }
	// }

	/**
	 * Draws the position markers.
	 * 
	 * @param event
	 */
	@SubscribeEvent
	public void drawMarkers(RenderTickEvent event) {
		if (event.phase != Phase.END) {
			return;
		}
		final Entity view = getMinecraft().getRenderViewEntity();
		if (!(view instanceof EntityPlayerSP)) {
			return;
		}
		EntityPlayerSP player = (EntityPlayerSP) view;
		if (player.getHeldItem() != null
				&& player.getHeldItem().getItem() == Items.wooden_axe) {
			if (markerRenderer == null) {
				markerRenderer = new PosMarkerRenderer(1, 0, 0);
			}
			markerRenderer.render(event, this, pos1, pos2);
		} else if (player.getHeldItem() != null
				&& player.getHeldItem().getItem() == Items.stick) {
			if (buildMarkerRenderer == null) {
				buildMarkerRenderer = new BuildMarkerRenderer();
			}
			buildMarkerRenderer.render(event, this);
		}
		AIStrategy strat = currentStrategy;
		if (strat != null) {
			strat.drawMarkers(event, this);
		}
	}

	public void positionMarkEvent(int x, int y, int z, int side) {
		final Pos pos = new Pos(x, y, z);
		setPosition(pos, nextPosIsPos2);
		nextPosIsPos2 ^= true;
	}

	private AIStrategy findNewStrategy() {
		if (requestedStrategy != null) {
			final AIStrategy r = requestedStrategy;
			requestedStrategy = null;
			return r;
		}

		for (final Entry<KeyBinding, AIStrategyFactory> e : uses.entrySet()) {
			if (e.getKey().isPressed()) {
				final AIStrategy strat = e.getValue().produceStrategy(this);
				if (strat != null) {
					return strat;
				}
			}
		}
		return null;
	}

	@Override
	public AIHelper getAiHelper() {
		return this;
	}

	@Override
	public void requestUseStrategy(AIStrategy strategy) {
		System.out.println("Request to use " + strategy);
		requestedStrategy = strategy;
	}

//	@SubscribeEvent
//	@SideOnly(Side.CLIENT)
	// public void playerInteract(PlayerInteractEvent event) {
	// final ItemStack stack = event.entityPlayer.inventory.getCurrentItem();
	// if (stack != null && stack.getItem() == Items.wooden_axe) {
	// if (event.action == Action.RIGHT_CLICK_BLOCK) {
	// if (event.entityPlayer.worldObj.isRemote) {
	// positionMarkEvent(event.x, event.y, event.z, 0);
	// }
	// }
	// }
	// }
	public void initialize() {
		FMLCommonHandler.instance().bus().register(this);

		// registerAxe();
	}

}
