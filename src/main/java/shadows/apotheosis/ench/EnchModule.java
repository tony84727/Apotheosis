package shadows.apotheosis.ench;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.apotheosis.Apotheosis;
import shadows.apotheosis.Apotheosis.ApotheosisClientSetup;
import shadows.apotheosis.Apotheosis.ApotheosisSetup;
import shadows.apotheosis.ApotheosisObjects;
import shadows.apotheosis.ench.EnchantmentInfo.ExpressionPowerFunc;
import shadows.apotheosis.ench.altar.BlockPrismaticAltar;
import shadows.apotheosis.ench.altar.TilePrismaticAltar;
import shadows.apotheosis.ench.anvil.BlockAnvilExt;
import shadows.apotheosis.ench.anvil.EnchantmentSplitting;
import shadows.apotheosis.ench.anvil.ItemAnvilExt;
import shadows.apotheosis.ench.anvil.TileAnvil;
import shadows.apotheosis.ench.enchantments.EnchantmentBerserk;
import shadows.apotheosis.ench.enchantments.EnchantmentDepths;
import shadows.apotheosis.ench.enchantments.EnchantmentHellInfused;
import shadows.apotheosis.ench.enchantments.EnchantmentIcyThorns;
import shadows.apotheosis.ench.enchantments.EnchantmentKnowledge;
import shadows.apotheosis.ench.enchantments.EnchantmentLifeMend;
import shadows.apotheosis.ench.enchantments.EnchantmentMagicProt;
import shadows.apotheosis.ench.enchantments.EnchantmentMounted;
import shadows.apotheosis.ench.enchantments.EnchantmentNatureBless;
import shadows.apotheosis.ench.enchantments.EnchantmentRebounding;
import shadows.apotheosis.ench.enchantments.EnchantmentReflective;
import shadows.apotheosis.ench.enchantments.EnchantmentScavenger;
import shadows.apotheosis.ench.enchantments.EnchantmentShieldBash;
import shadows.apotheosis.ench.enchantments.EnchantmentStableFooting;
import shadows.apotheosis.ench.enchantments.EnchantmentTempting;
import shadows.apotheosis.ench.objects.BlockHellBookshelf;
import shadows.apotheosis.ench.objects.ItemHellBookshelf;
import shadows.apotheosis.ench.objects.ItemScrapTome;
import shadows.apotheosis.ench.objects.ItemShearsExt;
import shadows.apotheosis.ench.objects.ItemTypedBook;
import shadows.apotheosis.ench.table.EnchantingTableBlockExt;
import shadows.apotheosis.ench.table.EnchantingTableTileEntityExt;
import shadows.apotheosis.ench.table.EnchantmentContainerExt;
import shadows.placebo.config.Configuration;
import shadows.placebo.loot.LootSystem;
import shadows.placebo.recipe.NBTIngredient;
import shadows.placebo.util.PlaceboUtil;
import shadows.placebo.util.ReflectionHelper;

/**
 * Short document on enchanting methods:
 * Item Enchantibility is tied to the number of enchantments the item will recieve, and the "level" passed to getRandomEnchantments.
 * The possible enchantabilities for an item are equal to:
 * [table level + 1, table level + 1 + (E/4 + 1) + (E/4 + 1)].  E == item enchantability.  (E/4 + 1) is rolled as a random int.
 *
 * Enchantment min/max enchantability should really be called something else, they aren't fully based on enchantability.
 * Enchantment rarity affects weight in WeightedRandom list picking.
 * Max expected table level is 150, 40 before empowered shelves.
 *
 */
public class EnchModule {

	public static final Map<Enchantment, EnchantmentInfo> ENCHANTMENT_INFO = new HashMap<>();
	public static final Logger LOGGER = LogManager.getLogger("Apotheosis : Enchantment");
	public static final List<ItemTypedBook> TYPED_BOOKS = new LinkedList<>();
	public static final DamageSource CORRUPTED = new DamageSource("apoth_corrupted").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final EquipmentSlotType[] ARMOR = { EquipmentSlotType.HEAD, EquipmentSlotType.CHEST, EquipmentSlotType.LEGS, EquipmentSlotType.FEET };
	public static final EnchantmentType HOE = EnchantmentType.create("HOE", i -> i instanceof HoeItem);
	public static final EnchantmentType SHIELD = EnchantmentType.create("SHIELD", i -> i instanceof ShieldItem);
	public static final EnchantmentType ANVIL = EnchantmentType.create("ANVIL", i -> i instanceof BlockItem && ((BlockItem) i).getBlock() instanceof AnvilBlock);
	static Configuration enchInfoConfig;

	@SubscribeEvent
	public void init(ApotheosisSetup e) {
		Configuration config = new Configuration(new File(Apotheosis.configDir, "enchantment_module.cfg"));
		if (config.hasChanged()) config.save();

		config = new Configuration(new File(Apotheosis.configDir, "enchantments.cfg"));
		for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
			int max = config.getInt("Max Level", ench.getRegistryName().toString(), getDefaultMax(ench), 1, 127, "The max level of this enchantment.");
			int min = config.getInt("Min Level", ench.getRegistryName().toString(), ench.getMinLevel(), 1, 127, "The min level of this enchantment.");
			if (min > max) min = max;
			EnchantmentInfo info = new EnchantmentInfo(ench, max, min);
			String maxF = config.getString("Max Power Function", ench.getRegistryName().toString(), "", "A function to determine the max enchanting power.  The variable \"x\" is level.  See: https://github.com/uklimaschewski/EvalEx#usage-examples");
			if (!maxF.isEmpty()) info.setMaxPower(new ExpressionPowerFunc(maxF));
			String minF = config.getString("Min Power Function", ench.getRegistryName().toString(), "", "A function to determine the min enchanting power.");
			if (!minF.isEmpty()) info.setMinPower(new ExpressionPowerFunc(minF));
			ENCHANTMENT_INFO.put(ench, info);
		}
		if (config.hasChanged()) config.save();
		enchInfoConfig = config;

		Ingredient pot = new NBTIngredient(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), Potions.REGENERATION));
		Apotheosis.HELPER.addShaped(ApotheosisObjects.HELLSHELF, 3, 3, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS, Items.BLAZE_ROD, "forge:bookshelves", pot, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS);
		Apotheosis.HELPER.addShaped(ApotheosisObjects.PRISMATIC_WEB, 3, 3, null, Items.PRISMARINE_SHARD, null, Items.PRISMARINE_SHARD, Blocks.COBWEB, Items.PRISMARINE_SHARD, null, Items.PRISMARINE_SHARD, null);
		ItemStack book = new ItemStack(Items.BOOK);
		ItemStack stick = new ItemStack(Items.STICK);
		ItemStack blaze = new ItemStack(Items.BLAZE_ROD);
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.ARMOR_HEAD_BOOK, 5), 3, 2, book, book, book, book, blaze, book);
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.ARMOR_CHEST_BOOK, 8), 3, 3, book, blaze, book, book, book, book, book, book, book);
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.ARMOR_LEGS_BOOK, 7), 3, 3, book, null, book, book, blaze, book, book, book, book);
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.ARMOR_FEET_BOOK, 4), 3, 2, book, null, book, book, blaze, book);
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.WEAPON_BOOK, 2), 1, 3, book, book, new ItemStack(Items.BLAZE_POWDER));
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.DIGGER_BOOK, 3), 3, 3, book, book, book, null, blaze, null, null, stick, null);
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.FISHING_ROD_BOOK, 2), 3, 3, null, null, blaze, null, stick, book, stick, null, book);
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.BOW_BOOK, 3), 3, 3, null, stick, book, blaze, null, book, null, stick, book);
		Apotheosis.HELPER.addShapeless(new ItemStack(ApotheosisObjects.NULL_BOOK, 6), book, book, book, book, book, book, blaze);
		ItemStack msBrick = new ItemStack(Blocks.MOSSY_STONE_BRICKS);
		Apotheosis.HELPER.addShaped(ApotheosisObjects.PRISMATIC_ALTAR, 3, 3, msBrick, null, msBrick, msBrick, Items.HEART_OF_THE_SEA, msBrick, msBrick, Blocks.ENCHANTING_TABLE, msBrick);
		Apotheosis.HELPER.addShaped(new ItemStack(Items.EXPERIENCE_BOTTLE, 16), 3, 3, Items.ENDER_EYE, Items.GOLD_NUGGET, Items.ENDER_EYE, Items.BLAZE_POWDER, Items.DRAGON_BREATH, Items.BLAZE_POWDER, Items.GLOWSTONE_DUST, Items.GLOWSTONE_DUST, Items.GLOWSTONE_DUST);
		Apotheosis.HELPER.addShaped(new ItemStack(Items.EXPERIENCE_BOTTLE, 1), 3, 3, Items.ENDER_EYE, Blocks.GOLD_BLOCK, Items.ENDER_EYE, Items.BLAZE_ROD, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), Potions.WATER), Items.BLAZE_ROD, Blocks.GLOWSTONE, Blocks.GLOWSTONE, Blocks.GLOWSTONE);
		Apotheosis.HELPER.addShaped(new ItemStack(ApotheosisObjects.SCRAP_TOME, 8), 3, 3, book, book, book, book, Blocks.ANVIL, book, book, book, book);
		LootSystem.defaultBlockTable(ApotheosisObjects.PRISMATIC_ALTAR);
		MinecraftForge.EVENT_BUS.register(this);
		for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
			EnchantmentInfo info = ENCHANTMENT_INFO.get(ench);
			for (int i = 1; i <= info.getMaxLevel(); i++)
				if (info.getMinPower(i) > info.getMaxPower(i)) LOGGER.error("Enchantment {} has min/max power {}/{} at level {}, making this level unobtainable.", ench.getRegistryName(), info.getMinPower(i), info.getMaxPower(i), i);
		}
	}

	@SubscribeEvent
	public void client(ApotheosisClientSetup e) {
		MinecraftForge.EVENT_BUS.register(new EnchModuleClient());
		EnchModuleClient.init();
	}

	@SubscribeEvent
	public void tiles(Register<TileEntityType<?>> e) {
		e.getRegistry().register(new TileEntityType<>(TileAnvil::new, ImmutableSet.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL), null).setRegistryName("anvil"));
		e.getRegistry().register(new TileEntityType<>(TilePrismaticAltar::new, ImmutableSet.of(ApotheosisObjects.PRISMATIC_ALTAR), null).setRegistryName("prismatic_altar"));
		e.getRegistry().register(new TileEntityType<>(EnchantingTableTileEntityExt::new, ImmutableSet.of(Blocks.ENCHANTING_TABLE), null).setRegistryName("minecraft:enchanting_table"));
	}

	@SubscribeEvent
	public void containers(Register<ContainerType<?>> e) {
		e.getRegistry().register(new ContainerType<>(EnchantmentContainerExt::new).setRegistryName("enchanting"));
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		//Formatter::off
		e.getRegistry().registerAll(
				new BlockHellBookshelf().setRegistryName("hellshelf"),
				new BlockPrismaticAltar().setRegistryName(Apotheosis.MODID, "prismatic_altar"),
				new BlockAnvilExt().setRegistryName("minecraft", "anvil"),
				new BlockAnvilExt().setRegistryName("minecraft", "chipped_anvil"),
				new BlockAnvilExt().setRegistryName("minecraft", "damaged_anvil"));
		//Formatter::on
		PlaceboUtil.registerOverrideBlock(new EnchantingTableBlockExt().setRegistryName("minecraft:enchanting_table"), Apotheosis.MODID);
	}

	@SubscribeEvent
	public void items(Register<Item> e) {
		Item oldShears = Items.SHEARS;
		Item shears;
		//Formatter::off
		e.getRegistry().registerAll(
				shears = new ItemShearsExt(),
				new ItemHellBookshelf(ApotheosisObjects.HELLSHELF).setRegistryName(ApotheosisObjects.HELLSHELF.getRegistryName()),
				new Item(new Item.Properties().group(ItemGroup.MISC)).setRegistryName(Apotheosis.MODID, "prismatic_web"),
				new ItemAnvilExt(Blocks.ANVIL),
				new ItemAnvilExt(Blocks.CHIPPED_ANVIL),
				new ItemAnvilExt(Blocks.DAMAGED_ANVIL),
				new ItemTypedBook(Items.AIR, null),
				new ItemTypedBook(Items.DIAMOND_HELMET, EnchantmentType.ARMOR_HEAD),
				new ItemTypedBook(Items.DIAMOND_CHESTPLATE, EnchantmentType.ARMOR_CHEST),
				new ItemTypedBook(Items.DIAMOND_LEGGINGS, EnchantmentType.ARMOR_LEGS),
				new ItemTypedBook(Items.DIAMOND_BOOTS, EnchantmentType.ARMOR_FEET),
				new ItemTypedBook(Items.DIAMOND_SWORD, EnchantmentType.WEAPON),
				new ItemTypedBook(Items.DIAMOND_PICKAXE, EnchantmentType.DIGGER),
				new ItemTypedBook(Items.FISHING_ROD, EnchantmentType.FISHING_ROD),
				new ItemTypedBook(Items.BOW, EnchantmentType.BOW),
				new BlockItem(ApotheosisObjects.PRISMATIC_ALTAR, new Item.Properties().group(ItemGroup.BUILDING_BLOCKS)).setRegistryName("prismatic_altar"),
				new ItemScrapTome()
				);
		//Formatter::on
		DispenserBlock.registerDispenseBehavior(shears, DispenserBlock.DISPENSE_BEHAVIOR_REGISTRY.get(oldShears));
	}

	@SubscribeEvent
	public void enchants(Register<Enchantment> e) {
		//Formatter::off
		e.getRegistry().registerAll(
				new EnchantmentHellInfused().setRegistryName(Apotheosis.MODID, "hell_infusion"),
				new EnchantmentMounted().setRegistryName(Apotheosis.MODID, "mounted_strike"),
				new EnchantmentDepths().setRegistryName(Apotheosis.MODID, "depth_miner"),
				new EnchantmentStableFooting().setRegistryName(Apotheosis.MODID, "stable_footing"),
				new EnchantmentScavenger().setRegistryName(Apotheosis.MODID, "scavenger"),
				new EnchantmentLifeMend().setRegistryName(Apotheosis.MODID, "life_mending"),
				new EnchantmentIcyThorns().setRegistryName(Apotheosis.MODID, "icy_thorns"),
				new EnchantmentTempting().setRegistryName(Apotheosis.MODID, "tempting"),
				new EnchantmentShieldBash().setRegistryName(Apotheosis.MODID, "shield_bash"),
				new EnchantmentReflective().setRegistryName(Apotheosis.MODID, "reflective"),
				new EnchantmentBerserk().setRegistryName(Apotheosis.MODID, "berserk"),
				new EnchantmentKnowledge().setRegistryName(Apotheosis.MODID, "knowledge"),
				new EnchantmentSplitting().setRegistryName(Apotheosis.MODID, "splitting"),
				new EnchantmentNatureBless().setRegistryName(Apotheosis.MODID, "natures_blessing"),
				new EnchantmentRebounding().setRegistryName(Apotheosis.MODID, "rebounding"),
				new EnchantmentMagicProt().setRegistryName(Apotheosis.MODID, "magic_protection")
				);
		//Formatter::on
	}

	@SubscribeEvent
	public void sounds(Register<SoundEvent> e) {
		e.getRegistry().register(new SoundEvent(new ResourceLocation(Apotheosis.MODID, "altar")).setRegistryName(Apotheosis.MODID, "altar_sound"));
	}

	@SubscribeEvent
	public void anvilEvent(AnvilUpdateEvent e) {
		if (!EnchantmentHelper.getEnchantments(e.getLeft()).isEmpty()) {
			if (e.getRight().getItem() == Items.COBWEB) {
				ItemStack stack = e.getLeft().copy();
				EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(stack).entrySet().stream().filter(ent -> ent.getKey().isCurse()).collect(Collectors.toMap(ent -> ent.getKey(), ent -> ent.getValue())), stack);
				e.setCost(1);
				e.setMaterialCost(1);
				e.setOutput(stack);
			} else if (e.getRight().getItem() == ApotheosisObjects.PRISMATIC_WEB) {
				ItemStack stack = e.getLeft().copy();
				EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(stack).entrySet().stream().filter(ent -> !ent.getKey().isCurse()).collect(Collectors.toMap(ent -> ent.getKey(), ent -> ent.getValue())), stack);
				e.setCost(30);
				e.setMaterialCost(1);
				e.setOutput(stack);
				return;
			}
		}
		if ((e.getLeft().getItem() == Items.CHIPPED_ANVIL || e.getLeft().getItem() == Items.DAMAGED_ANVIL) && e.getRight().getItem().isIn(Tags.Items.STORAGE_BLOCKS_IRON)) {
			if (e.getLeft().getCount() != 1) return;
			int dmg = e.getLeft().getItem() == Items.DAMAGED_ANVIL ? 2 : 1;
			ItemStack out = new ItemStack(dmg == 1 ? Items.ANVIL : Items.CHIPPED_ANVIL);
			EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(e.getLeft()), out);
			out.setCount(1);
			e.setOutput(out);
			e.setCost(5 + EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, e.getLeft()) + EnchantmentHelper.getEnchantmentLevel(ApotheosisObjects.SPLITTING, e.getLeft()) * 2);
			e.setMaterialCost(1);
			return;
		}
		if (ItemTypedBook.updateAnvil(e)) return;
		if (ItemScrapTome.updateAnvil(e)) return;
	}

	Method dropLoot;

	/**
	 * Event handler for the Scavenger and Knowledge of the Ages enchantments.
	 */
	@SubscribeEvent(priority = EventPriority.LOW)
	public void drops(LivingDropsEvent e) throws Exception {
		Entity attacker = e.getSource().getTrueSource();
		if (attacker instanceof PlayerEntity) {
			PlayerEntity p = (PlayerEntity) attacker;
			if (p.world.isRemote) return;
			int scavenger = EnchantmentHelper.getEnchantmentLevel(ApotheosisObjects.SCAVENGER, p.getHeldItemMainhand());
			if (scavenger > 0 && p.world.rand.nextInt(100) < scavenger * 2.5F) {
				if (dropLoot == null) {
					dropLoot = ReflectionHelper.findMethod(LivingEntity.class, "dropLoot", "func_213354_a", DamageSource.class, boolean.class);
				}
				dropLoot.invoke(e.getEntityLiving(), e.getSource(), true);
			}
			int knowledge = EnchantmentHelper.getEnchantmentLevel(ApotheosisObjects.KNOWLEDGE, p.getHeldItemMainhand());
			if (knowledge > 0 && !(e.getEntity() instanceof PlayerEntity)) {
				int items = 0;
				for (ItemEntity i : e.getDrops())
					items += i.getItem().getCount();
				if (items > 0) e.getDrops().clear();
				items *= knowledge * 25;
				Entity ded = e.getEntityLiving();
				while (items > 0) {
					int i = ExperienceOrbEntity.getXPSplit(items);
					items -= i;
					p.world.addEntity(new ExperienceOrbEntity(p.world, ded.getX(), ded.getY(), ded.getZ(), i));
				}
			}
		}
	}

	final EquipmentSlotType[] slots = EquipmentSlotType.values();

	/**
	 * Event handler for the Life Mending enchantment
	 */
	@SubscribeEvent
	public void lifeMend(LivingUpdateEvent e) {
		if (e.getEntity().world.isRemote || e.getEntity().ticksExisted % 20 != 0) return;
		for (EquipmentSlotType slot : slots) {
			ItemStack stack = e.getEntityLiving().getItemStackFromSlot(slot);
			if (!stack.isEmpty() && stack.isDamaged()) {
				int level = EnchantmentHelper.getEnchantmentLevel(ApotheosisObjects.LIFE_MENDING, stack);
				if (level > 0) {
					int i = Math.min(level, stack.getDamage());
					e.getEntityLiving().attackEntityFrom(CORRUPTED, i * 0.7F);
					stack.setDamage(stack.getDamage() - i);
					return;
				}
			}
		}
	}

	/**
	 * Event handler for the Stable Footing and Miner's Fervor enchants.
	 */
	@SubscribeEvent
	public void breakSpeed(PlayerEvent.BreakSpeed e) {
		PlayerEntity p = e.getPlayer();
		if (!p.onGround && EnchantmentHelper.getMaxEnchantmentLevel(ApotheosisObjects.STABLE_FOOTING, p) > 0) {
			if (e.getOriginalSpeed() < e.getNewSpeed() * 5) e.setNewSpeed(e.getNewSpeed() * 5F);
		}
		ItemStack stack = p.getHeldItemMainhand();
		if (stack.isEmpty()) return;
		int depth = EnchantmentHelper.getEnchantmentLevel(ApotheosisObjects.DEPTH_MINER, stack);
		if (depth > 0) {
			if (!stack.getToolTypes().stream().anyMatch(e.getState()::isToolEffective)) return;
			float effectiveness = (p.world.getSeaLevel() - (float) p.getY()) / p.world.getSeaLevel();
			if (effectiveness < 0) effectiveness /= 3;
			float speedChange = 1 + depth * 5 * effectiveness;
			e.setNewSpeed(Math.max(0.05F, e.getNewSpeed() + speedChange));
		}
	}

	/**
	 * Event handler for the Nature's Blessing enchantment.
	 */
	@SubscribeEvent
	public void rightClick(PlayerInteractEvent.RightClickBlock e) {
		ItemStack s = e.getItemStack();
		int nbLevel = EnchantmentHelper.getEnchantmentLevel(ApotheosisObjects.NATURES_BLESSING, s);
		if (!e.getEntity().isSneaking() && nbLevel > 0 && BoneMealItem.applyBonemeal(s.copy(), e.getWorld(), e.getPos(), e.getPlayer())) {
			s.damageItem(6 - nbLevel, e.getPlayer(), ent -> ent.sendBreakAnimation(e.getHand()));
			e.setCanceled(true);
			e.setCancellationResult(ActionResultType.SUCCESS);
		}
	}

	/**
	 * Event handler for Anvil Unbreaking.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void applyUnbreaking(AnvilRepairEvent e) {
		if (e.getPlayer().openContainer instanceof RepairContainer) {
			RepairContainer r = (RepairContainer) e.getPlayer().openContainer;
			TileEntity te = r.field_216980_g.apply((w, p) -> w.getTileEntity(p)).orElse(null);
			if (te instanceof TileAnvil) e.setBreakChance(e.getBreakChance() / (((TileAnvil) te).getUnbreaking() + 1));
		}
	}

	/**
	 * Handles the Berserker's Fury and Occult Aversion enchantments.
	 */
	@SubscribeEvent
	public void livingHurt(LivingHurtEvent e) {
		LivingEntity user = e.getEntityLiving();
		if (e.getSource().getTrueSource() instanceof Entity && user.getActivePotionEffect(Effects.RESISTANCE) == null) {
			int level = EnchantmentHelper.getMaxEnchantmentLevel(ApotheosisObjects.BERSERK, user);
			if (level > 0) {
				user.attackEntityFrom(EnchModule.CORRUPTED, level * level);
				user.addPotionEffect(new EffectInstance(Effects.RESISTANCE, 200 * level, level - 1));
				user.addPotionEffect(new EffectInstance(Effects.STRENGTH, 200 * level, level - 1));
				user.addPotionEffect(new EffectInstance(Effects.SPEED, 200 * level, level - 1));
			}
		}
		if (e.getSource().isMagicDamage() && e.getSource().getTrueSource() instanceof LivingEntity) {
			LivingEntity src = (LivingEntity) e.getSource().getTrueSource();
			int lvl = EnchantmentHelper.getMaxEnchantmentLevel(ApotheosisObjects.MAGIC_PROTECTION, src);
			if (lvl > 0) {
				//TODO: FIXME should only be reducing damage by the value of OA, this will use all active protection enchantments.
				e.setAmount(CombatRules.getDamageAfterMagicAbsorb(e.getAmount(), EnchantmentHelper.getEnchantmentModifierDamage(src.getArmorInventoryList(), e.getSource())));
			}
		}
	}

	public static EnchantmentInfo getEnchInfo(Enchantment ench) {
		EnchantmentInfo info = ENCHANTMENT_INFO.get(ench);

		if (!Apotheosis.enableEnch) {
			return ENCHANTMENT_INFO.computeIfAbsent(ench, e -> new EnchantmentInfo(e, e.getMaxLevel(), e.getMinLevel()));
		}

		if (enchInfoConfig == null) {
			LOGGER.error("A mod has attempted to access enchantment information before Apotheosis init, this should not happen.");
			Thread.dumpStack();
			return new EnchantmentInfo(ench, ench.getMaxLevel(), ench.getMinLevel());
		}
		if (info == null) {
			int max = enchInfoConfig.getInt("Max Level", ench.getRegistryName().toString(), getDefaultMax(ench), 1, 127, "The max level of this enchantment.");
			int min = enchInfoConfig.getInt("Min Level", ench.getRegistryName().toString(), ench.getMinLevel(), 1, 127, "The min level of this enchantment.");
			if (min > max) min = max;
			info = new EnchantmentInfo(ench, max, min);
			String maxF = enchInfoConfig.getString("Max Power Function", ench.getRegistryName().toString(), "", "A function to determine the max enchanting power.  The variable \"x\" is level.  See: https://github.com/uklimaschewski/EvalEx#usage-examples");
			if (!maxF.isEmpty()) info.setMaxPower(new ExpressionPowerFunc(maxF));
			String minF = enchInfoConfig.getString("Min Power Function", ench.getRegistryName().toString(), "", "A function to determine the min enchanting power.");
			if (!minF.isEmpty()) info.setMinPower(new ExpressionPowerFunc(minF));
			ENCHANTMENT_INFO.put(ench, info);
			if (enchInfoConfig.hasChanged()) enchInfoConfig.save();
			LOGGER.error("Had to late load enchantment info for {}, this is a bug in the mod {} as they are registering late!", ench.getRegistryName(), ench.getRegistryName().getNamespace());
		}
		return info;
	}

	/**
	 * Tries to find a max level for this enchantment.  This is used to scale up default levels to the Apoth cap.
	 */
	public static int getDefaultMax(Enchantment ench) {
		int level = ench.getMaxLevel();
		if (level == 1) return 1;
		int maxPower = ench.getMaxEnchantability(level);
		if (maxPower >= 85) return level;
		int lastMaxPower = maxPower; //Need this to check that we don't get locked up on single-level enchantments.
		while (maxPower < 85) {
			maxPower = ench.getMaxEnchantability(++level);
			if (lastMaxPower == maxPower) {
				level--;
				break;
			}
			lastMaxPower = maxPower;
		}
		return level;
	}

}