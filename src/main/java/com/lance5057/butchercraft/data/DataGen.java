package com.lance5057.butchercraft.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.lance5057.butchercraft.Butchercraft;
import com.lance5057.butchercraft.data.builders.Advancements;
import com.lance5057.butchercraft.data.builders.BlockModels;
import com.lance5057.butchercraft.data.builders.BlockTagGenerator;
import com.lance5057.butchercraft.data.builders.ButchercraftEntityTypeTagsProvider;
import com.lance5057.butchercraft.data.builders.ButchercraftLootTableProvider;
import com.lance5057.butchercraft.data.builders.EnglishLoc;
import com.lance5057.butchercraft.data.builders.ItemModels;
import com.lance5057.butchercraft.data.builders.ItemTagGenerator;
import com.lance5057.butchercraft.data.builders.PoiTagGenerator;
import com.lance5057.butchercraft.data.builders.recipes.ButchercraftRecipeProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Butchercraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGen {

	@SubscribeEvent
	public static void gatherData(final GatherDataEvent event) {
		Butchercraft.logger.info("Data Generator Started!");

		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		CompletableFuture <HolderLookup.Provider> lookupProvider = event.getLookupProvider();
		ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

		final Advancements advancements = new Advancements();
		generator.addProvider(event.includeServer(), new ForgeAdvancementProvider(output, lookupProvider, existingFileHelper, List.of(advancements)));
		generator.addProvider(true, new ItemModels(output, existingFileHelper));
		generator.addProvider(true, new BlockModels(output, existingFileHelper));
		generator.addProvider(true, new EnglishLoc(output));

		BlockTagGenerator btg = new BlockTagGenerator(output, lookupProvider, existingFileHelper);
		generator.addProvider(true, btg);
		generator.addProvider(true, new ItemTagGenerator(output, lookupProvider, btg.contentsGetter(), existingFileHelper));

		generator.addProvider(true, new ButchercraftRecipeProvider(output));
		generator.addProvider(true, new ButchercraftLootTableProvider(output));
		generator.addProvider(true, new ButchercraftEntityTypeTagsProvider(output, lookupProvider, existingFileHelper));
		generator.addProvider(true, new PoiTagGenerator(output, lookupProvider, existingFileHelper));
	}
}
