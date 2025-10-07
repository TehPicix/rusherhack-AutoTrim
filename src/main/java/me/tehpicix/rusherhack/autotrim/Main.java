package me.tehpicix.rusherhack.autotrim;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class Main extends Plugin {

	@Override
	public void onLoad() {
		final AutoTrim module = new AutoTrim();
		RusherHackAPI.getModuleManager().registerFeature(module);
	}

	@Override
	public void onUnload() {
	}
}