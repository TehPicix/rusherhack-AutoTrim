package me.tehpicix.rusherhack.autotrim;

import com.google.common.base.Predicate;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.core.setting.NumberSetting;

public class AutoTrim extends ToggleableModule {

	private int ticksPassed = 0;

	private final NumberSetting<Integer> delay = new NumberSetting<>("Delay", "Ticks between inventory clicks", 1, 0, 5);
	private final BooleanSetting customized = new BooleanSetting("Customize", "Apply a custom trim to the armor", false);
	private final BooleanSetting overwrite = new BooleanSetting("Overwrite", "Overwrite existing trim", true);
	private final EnumSetting<TrimMaterial> material = new EnumSetting<>("Material", "The material to use for the trim", TrimMaterial.DIAMOND);
	private final EnumSetting<TrimPattern> template = new EnumSetting<>("Template", "The trim to apply to the armor", TrimPattern.FLOW);
	private final BooleanSetting upgrade = new BooleanSetting("Upgrade", "Upgrade from diamond to Netherite first", true);

	public AutoTrim() {
		super("AutoTrim", ModuleCategory.MISC);
		registerSettings(delay);
		customized.addSubSettings(overwrite, material, template);
		registerSettings(customized);
		registerSettings(upgrade);
	}

	@Subscribe
	public void onTick(EventUpdate event) {

		// Ensure player and world are not null
		if (mc.player == null || mc.level == null) return;

		// Increment ticks and check against delay
		ticksPassed++;
		if (ticksPassed < delay.getValue()) return;
		ticksPassed = 0;

		// Check if the current screen is a SmithingScreen
		if (!(mc.screen instanceof SmithingScreen screen)) return;

		// Get the SmithingMenu from the screen
		SmithingMenu menu = screen.getMenu();
		List<ItemStack> inventory = mc.player.getInventory().items;

		// Auto take out the result if there is one
		if (menu.getSlot(3).hasItem()) {
			InventoryUtils.clickSlot(3, true);
			return;
		}

		// Upgrade diamond items to netherite first
		if (upgrade.getValue()) {

			// List diamond items in the inventory
			List<ItemStack> diamondItems =
			    inventory
			        .stream()
			        .filter(AutoTrim::isUpgradable)
			        .toList();

			// If there are no diamond items, return
			if (diamondItems.size() > 0 || (menu.getSlot(1).hasItem() && isUpgradable(menu.getSlot(1).getItem()))) {

				// Upgrade the first diamond item to netherite
				int templateSlot = findItem(menu, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
				int smithItemSlot = findItem(menu, stack -> diamondItems.contains(stack));
				int netheriteSlot = findItem(menu, Items.NETHERITE_INGOT);

				// Template slot
				if (menu.getSlot(0).hasItem()) {
					ItemStack slot0 = menu.getSlot(0).getItem();
					if (!slot0.getItem().equals(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
						InventoryUtils.clickSlot(0, true);
						return;
					}
				} else if (templateSlot != -1) {
					InventoryUtils.clickSlot(templateSlot, true);
					return;
				} else if (templateSlot == -1)
					return;

				// Material slot
				if (menu.getSlot(2).hasItem()) {
					ItemStack slot2 = menu.getSlot(2).getItem();
					if (!slot2.getItem().equals(Items.NETHERITE_INGOT)) {
						InventoryUtils.clickSlot(2, true);
						return;
					}
				} else if (netheriteSlot != -1) {
					InventoryUtils.clickSlot(netheriteSlot, true);
					return;
				}

				if (!menu.getSlot(1).hasItem()) {
					InventoryUtils.clickSlot(smithItemSlot, true);
					return;
				}
			}
		}

		if (customized.getValue()) {

			// List of all the armor pieces
			List<ItemStack> armorItems =
			    inventory
			        .stream()
			        .filter(stack -> {
				        if (stack.getItem().equals(Items.NETHERITE_BOOTS)) return true;
				        if (stack.getItem().equals(Items.NETHERITE_LEGGINGS)) return true;
				        if (stack.getItem().equals(Items.NETHERITE_CHESTPLATE)) return true;
				        if (stack.getItem().equals(Items.NETHERITE_HELMET)) return true;
				        return false;
			        })
			        .filter(stack -> {
				        ArmorTrim trim = stack.get(DataComponents.TRIM);
				        if (trim == null) return true;

				        // existing trim IDs on the stack
				        ResourceLocation materialId = trim.material().unwrapKey().map(k -> k.location()).orElse(null);
				        ResourceLocation patternId = trim.pattern().unwrapKey().map(k -> k.location()).orElse(null);

				        // desired IDs (adjust namespace if yours isn’t "minecraft")
				        ResourceLocation wantedMaterialId = ResourceLocation.fromNamespaceAndPath(
				            "minecraft", material.getValue().name().toLowerCase());
				        ResourceLocation wantedPatternId = ResourceLocation.fromNamespaceAndPath(
				            "minecraft", template.getValue().name().toLowerCase());

				        boolean sameMaterial = materialId != null && materialId.equals(wantedMaterialId);
				        boolean samePattern = patternId != null && patternId.equals(wantedPatternId);

				        // filter OUT stacks that already have the desired trim
				        return !(sameMaterial && samePattern);
			        })
			        // If overwrite is false, filter out items that already have a trim
			        .filter(stack -> overwrite.getValue() || stack.get(DataComponents.TRIM) == null)
			        .toList();

			// Get the corresponding Item for the selected TrimMaterial
			Item trimMaterial = switch (material.getValue()) {
			    case AMETHYST -> Items.AMETHYST_SHARD;
				case COPPER -> Items.COPPER_INGOT;
				case DIAMOND -> Items.DIAMOND;
				case EMERALD -> Items.EMERALD;
				case GOLD -> Items.GOLD_INGOT;
				case IRON -> Items.IRON_INGOT;
				case LAPIS -> Items.LAPIS_LAZULI;
				case NETHERITE -> Items.NETHERITE_INGOT;
				case QUARTZ -> Items.QUARTZ;
				case REDSTONE -> Items.REDSTONE;
				case RESIN -> Items.RESIN_CLUMP;
			};

			// List of all the trim materials in the inventory
			List<ItemStack> trimMaterials =
			    inventory
			        .stream()
			        .filter(stack -> stack.getItem().equals(trimMaterial))
			        .toList();

			// List templates in the inventory
			List<ItemStack> templates =
			    inventory
			        .stream()
			        .filter(stack -> stack.getItem() instanceof SmithingTemplateItem)
			        .filter(stack -> stack.getItem().getDescriptionId().toLowerCase().contains(template.getValue().name().toLowerCase()))
			        .toList();

			// Upgrade the first diamond item to netherite
			int templateSlot = findItem(menu, stack -> templates.contains(stack));
			int smithItemSlot = findItem(menu, stack -> armorItems.contains(stack));
			int materialSlot = findItem(menu, stack -> trimMaterials.contains(stack));

			// Template slot
			if (menu.getSlot(0).hasItem()) {
				ItemStack slot0 = menu.getSlot(0).getItem();
				if (!slot0.getItem().getDescriptionId().toLowerCase().contains(template.getValue().name().toLowerCase())) {
					InventoryUtils.clickSlot(0, true);
					return;
				}
			} else if (templateSlot != -1) {
				InventoryUtils.clickSlot(templateSlot, true);
				return;
			}

			// Material slot
			if (menu.getSlot(2).hasItem()) {
				ItemStack slot2 = menu.getSlot(2).getItem();
				if (!slot2.getItem().equals(trimMaterial)) {
					InventoryUtils.clickSlot(2, true);
					return;
				}
			} else if (materialSlot != -1) {
				InventoryUtils.clickSlot(materialSlot, true);
				return;
			}

			if (!menu.getSlot(1).hasItem() && smithItemSlot != -1) {
				InventoryUtils.clickSlot(smithItemSlot, true);
				return;
			}

		}
	}

	public static int findItem(SmithingMenu menu, Item item) {
		Predicate<ItemStack> predicate = stack -> stack.getItem().equals(item);
		return findItem(menu, predicate);
	}

	public static int findItem(SmithingMenu menu, Predicate<ItemStack> predicate) {
		int maxSlots = menu.slots.size();
		for (int i = 4; i < maxSlots; i++)
			if (predicate.test(menu.getSlot(i).getItem()))
				return i;
		return -1;
	}

	public static boolean isUpgradable(ItemStack stack) {
		if (stack.getItem().equals(Items.DIAMOND_BOOTS)) return true;
		if (stack.getItem().equals(Items.DIAMOND_LEGGINGS)) return true;
		if (stack.getItem().equals(Items.DIAMOND_CHESTPLATE)) return true;
		if (stack.getItem().equals(Items.DIAMOND_HELMET)) return true;
		if (stack.getItem().equals(Items.DIAMOND_AXE)) return true;
		if (stack.getItem().equals(Items.DIAMOND_HOE)) return true;
		if (stack.getItem().equals(Items.DIAMOND_PICKAXE)) return true;
		if (stack.getItem().equals(Items.DIAMOND_SHOVEL)) return true;
		if (stack.getItem().equals(Items.DIAMOND_SWORD)) return true;
		return false;
	}

}
