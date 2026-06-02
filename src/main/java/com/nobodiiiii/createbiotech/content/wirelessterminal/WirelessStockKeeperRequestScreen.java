package com.nobodiiiii.createbiotech.content.wirelessterminal;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WirelessStockKeeperRequestScreen extends StockKeeperRequestScreen {

	public WirelessStockKeeperRequestScreen(WirelessStockKeeperRequestMenu container, Inventory inv, Component title) {
		super(container, inv, title);
	}
}
