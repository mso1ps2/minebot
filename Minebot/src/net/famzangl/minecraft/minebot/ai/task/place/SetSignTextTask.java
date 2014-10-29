package net.famzangl.minecraft.minebot.ai.task.place;

import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.strategy.TaskOperations;
import net.famzangl.minecraft.minebot.ai.task.AITask;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatAllowedCharacters;

/**
 * Use an {@link SetSignTextTask} afterwards!
 * 
 * @author michael
 *
 */
public class SetSignTextTask extends AITask {
	private boolean guiOpened;
	private int timer = 0;
	private final int x;
	private final int y;
	private final int z;

	private final String[] text = new String[4];

	public SetSignTextTask(int x, int y, int z, String[] text) {
		this.x = x;
		this.y = y;
		this.z = z;
		for (int i = 0; i < this.text.length; i++) {
			if (text.length > i && text[i] != null) {
				this.text[i] = makeTextSafe(text[i]);
			} else {
				this.text[i] = "";
			}
		}
	}

	private String makeTextSafe(String string) {
		StringBuffer res = new StringBuffer();
		for (int i = 0; i < string.length() && res.length() < 15; i++) {
			char c = string.charAt(i);
			if (ChatAllowedCharacters.isAllowedCharacter(c)) {
				res.append(c);
			} else if (c == '§') {
				res.append(' ');
			}
		}
		return res.toString();
	}

	@Override
	public boolean isFinished(AIHelper h) {
		return guiOpened && h.getMinecraft().currentScreen == null;
	}

	@Override
	public void runTick(AIHelper h, TaskOperations o) {
		if (!guiOpened) {
			// wait
			guiOpened = h.getMinecraft().currentScreen instanceof GuiEditSign;
		} else {
			if (timer == 0) {
				TileEntitySign sign = (TileEntitySign) h
						.getMinecraft().theWorld.getTileEntity(x, y, z);
				sign.signText = text;
			} else if (timer == 5) {
				//GuiEditSign edit = (GuiEditSign) h.getMinecraft().currentScreen;
				h.getMinecraft().displayGuiScreen(null);
			}
			timer++;
		}

	}
	
	@Override
	public int getGameTickTimeout() {
		return 40;
	}
}