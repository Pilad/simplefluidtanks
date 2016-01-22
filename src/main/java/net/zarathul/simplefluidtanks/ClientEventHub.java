package net.zarathul.simplefluidtanks;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RegistrySimple;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.Attributes;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.zarathul.simplefluidtanks.configuration.Config;
import net.zarathul.simplefluidtanks.registration.Registry;
import net.zarathul.simplefluidtanks.rendering.TankModelFactory;

/**
 * Hosts Forge and FML event handlers on the client side.
 */
public final class ClientEventHub
{
	@SubscribeEvent
	public void OnConfigChanged(OnConfigChangedEvent event)
	{
		if (SimpleFluidTanks.MOD_ID.equals(event.modID) && !event.isWorldRunning)
		{
			Config.sync();
		}
	}
	
	@SubscribeEvent
	public void onModelBakeEvent(ModelBakeEvent event)
	{
		// generate fluid models for all registered fluids for 16 levels each
		
		IRetexturableModel[] fluidModels = new IRetexturableModel[TankModelFactory.FLUID_LEVELS];
		
		try
		{
			// load the fluid models for the different levels from the .json files
			
			for (int x = 0; x < TankModelFactory.FLUID_LEVELS; x++)
			{
				fluidModels[x] = (IRetexturableModel)event.modelLoader.getModel(new ModelResourceLocation(SimpleFluidTanks.MOD_ID + ":block/fluid_" + String.valueOf(x)));
			}
		}
		catch (IOException e)
		{
			System.out.println("Failed loading fluid model.");
			
			return;
		}
		
        Function<ResourceLocation, TextureAtlasSprite> textureGetter = new Function<ResourceLocation, TextureAtlasSprite>()
        {
            @Override
			public TextureAtlasSprite apply(ResourceLocation location)
            {
                return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());
            }
        };
        
        IModel retexturedModel;
        
        // retexture and cache the loaded fluid models for each registered fluid
		
		for (Entry<Fluid, Integer> entry : FluidRegistry.getRegisteredFluidIDs().entrySet())
		{
			for (int x = 0; x < TankModelFactory.FLUID_LEVELS; x++)
			{
				retexturedModel = fluidModels[x].retexture(new ImmutableMap.Builder()
						.put("fluid", entry.getKey().getStill().toString())
						.build());

				TankModelFactory.FLUID_MODELS[x].put(
						entry.getValue(),
						retexturedModel.bake(retexturedModel.getDefaultState(), Attributes.DEFAULT_BAKED_FORMAT, textureGetter));
			}
		}
		
		// get ModelResourceLocations of all tank block variants from the registry except "inventory"
		
		RegistrySimple<ModelResourceLocation, IBakedModel> registry = (RegistrySimple)event.modelRegistry;
		HashSet<ModelResourceLocation> modelLocations = Sets.newHashSet();
		
		for (ModelResourceLocation modelLoc : registry.getKeys())
		{
			if (modelLoc.getResourceDomain().equals(SimpleFluidTanks.MOD_ID)
				&& modelLoc.getResourcePath().equals(Registry.TANK_BLOCK_NAME)
				&& !modelLoc.getVariant().equals("inventory"))
			{
				modelLocations.add(modelLoc);
			}
		}
		
		// replace the registered tank block variants with TankModelFactories
		
		IBakedModel registeredModel;
		TankModelFactory modelFactory;
		
		for (ModelResourceLocation loc : modelLocations)
		{
			registeredModel = event.modelRegistry.getObject(loc);
			modelFactory = new TankModelFactory(registeredModel);
			event.modelRegistry.putObject(loc, modelFactory);
		}
	}
}