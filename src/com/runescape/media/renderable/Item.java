package com.runescape.media.renderable;

import com.runescape.cache.def.ItemDefinition;
import com.runescape.cache.media.Model;

public class Item extends Renderable {

	public int itemId;
	public int itemCount;

	@Override
	public final Model getRotatedModel() {
		final ItemDefinition itemDef = ItemDefinition.getDefinition(itemId);
		return itemDef != null ? itemDef.getAmountModel(itemCount) : null;
	}
}
