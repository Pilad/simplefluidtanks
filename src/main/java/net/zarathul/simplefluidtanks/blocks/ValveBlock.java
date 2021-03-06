package net.zarathul.simplefluidtanks.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.zarathul.simplefluidtanks.SimpleFluidTanks;
import net.zarathul.simplefluidtanks.common.Utils;
import net.zarathul.simplefluidtanks.configuration.Config;
import net.zarathul.simplefluidtanks.tileentities.ValveBlockEntity;

import java.util.Random;

/**
 * Represents a valve in the mods multiblock structure.
 */
public class ValveBlock extends WrenchableBlock
{
	public static final PropertyInteger DOWN = PropertyInteger.create("down", 0, 2);
	public static final PropertyInteger UP = PropertyInteger.create("up", 0, 2);
	public static final PropertyInteger NORTH = PropertyInteger.create("north", 0, 2);
	public static final PropertyInteger SOUTH = PropertyInteger.create("south", 0, 2);
	public static final PropertyInteger WEST = PropertyInteger.create("west", 0, 2);
	public static final PropertyInteger EAST = PropertyInteger.create("east", 0, 2);
	
	private static final int GRATE_TEXTURE_ID = 0;
	private static final int IO_TEXTURE_ID = 1;
	private static final int TANK_TEXTURE_ID = 2;

	public ValveBlock()
	{
		super(TankMaterial.tankMaterial);

		setRegistryName(SimpleFluidTanks.VALVE_BLOCK_NAME);
		setUnlocalizedName(SimpleFluidTanks.VALVE_BLOCK_NAME);
		setCreativeTab(SimpleFluidTanks.creativeTab);
		setHardness(Config.valveBlockHardness);
		setResistance(Config.valveBlockResistance);
		setSoundType(SoundType.METAL);
		setHarvestLevel("pickaxe", 2);
		
		this.setDefaultState(this.blockState.getBaseState()
				.withProperty(UP, GRATE_TEXTURE_ID)
				.withProperty(NORTH, IO_TEXTURE_ID));
	}
    
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return new ValveBlockEntity();
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, DOWN, UP, NORTH, SOUTH, WEST, EAST);
	}
	
	@Override
	public int getMetaFromState(IBlockState state)
	{
		return 0;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		ValveBlockEntity valveEntity = Utils.getTileEntityAt(world, ValveBlockEntity.class, pos);
		
		if (valveEntity != null)
		{
			if (valveEntity.hasTanks())
			{
				state = state.withProperty(DOWN, (valveEntity.isFacingTank(EnumFacing.DOWN)) ? GRATE_TEXTURE_ID : IO_TEXTURE_ID)
						.withProperty(UP, (valveEntity.isFacingTank(EnumFacing.UP)) ? GRATE_TEXTURE_ID : IO_TEXTURE_ID)
						.withProperty(NORTH, (valveEntity.isFacingTank(EnumFacing.NORTH)) ? GRATE_TEXTURE_ID : IO_TEXTURE_ID)
						.withProperty(SOUTH, (valveEntity.isFacingTank(EnumFacing.SOUTH)) ? GRATE_TEXTURE_ID : IO_TEXTURE_ID)
						.withProperty(WEST, (valveEntity.isFacingTank(EnumFacing.WEST)) ? GRATE_TEXTURE_ID : IO_TEXTURE_ID)
						.withProperty(EAST, (valveEntity.isFacingTank(EnumFacing.EAST)) ? GRATE_TEXTURE_ID : IO_TEXTURE_ID);
			}
			else
			{
				EnumFacing facing = valveEntity.getFacing();
				
				state = state.withProperty(DOWN, TANK_TEXTURE_ID)
						.withProperty(UP, GRATE_TEXTURE_ID)
						.withProperty(NORTH, (facing == EnumFacing.NORTH) ? IO_TEXTURE_ID : TANK_TEXTURE_ID)
						.withProperty(SOUTH, (facing == EnumFacing.SOUTH) ? IO_TEXTURE_ID : TANK_TEXTURE_ID)
						.withProperty(WEST, (facing == EnumFacing.WEST) ? IO_TEXTURE_ID : TANK_TEXTURE_ID)
						.withProperty(EAST, (facing == EnumFacing.EAST) ? IO_TEXTURE_ID : TANK_TEXTURE_ID);
			}
		}
		
		return state;
	}

	@Override
	public boolean requiresUpdates()
	{
		return false;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack items)
	{
		super.onBlockPlacedBy(world, pos, state, placer, items);
		
		if (!world.isRemote)
		{
			EnumFacing facing = placer.getHorizontalFacing().getOpposite();
			
			ValveBlockEntity valveEntity = Utils.getTileEntityAt(world, ValveBlockEntity.class, pos);
			
			if (valveEntity != null)
			{
				valveEntity.setFacing(facing);
				world.markChunkDirty(pos, valveEntity);
				world.notifyBlockUpdate(pos, state, state, 3);
			}
		}
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state)
	{
		if (!world.isRemote)
		{
			ValveBlockEntity valveEntity = Utils.getTileEntityAt(world, ValveBlockEntity.class, pos);

			if (valveEntity != null)
			{
				valveEntity.formMultiblock();
			}
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
			EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (!world.isRemote)
		{
			ValveBlockEntity valveEntity = Utils.getTileEntityAt(world, ValveBlockEntity.class, pos);

			if (valveEntity != null)
			{
				IFluidHandler handler = valveEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);

				if (handler != null)
				{
					// FIXME: Horrible workaround until Forge fixes sounds not playing client-side.
					FluidStack tankFluidBefore = (valveEntity.getFluidAmount() > 0) ? valveEntity.getFluid().copy() : null;

					if (FluidUtil.interactWithFluidHandler(player, hand, handler))
					{
						// Pick a sound depending on what happens with the held container item.
						SoundEvent soundevent = (tankFluidBefore == null || tankFluidBefore.amount < valveEntity.getFluidAmount())
							? valveEntity.getFluid().getFluid().getEmptySound()
							: tankFluidBefore.getFluid().getFillSound();

						((EntityPlayerMP)player).connection.sendPacket(new SPacketSoundEffect(
							soundevent,
							player.getSoundCategory(),
							player.posX, player.posY, player.posZ,
							1.0f, 1.0f));
					}
				}
			}
		}

		if (FluidUtil.getFluidHandler(player.getHeldItem(hand)) != null) return true;
		
		return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
	}

	@Override
	public int quantityDropped(IBlockState state, int fortune, Random random)
	{
		return 1;
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state)
	{
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos)
	{
		ValveBlockEntity valveEntity = Utils.getTileEntityAt(world, ValveBlockEntity.class, pos);

		if (valveEntity != null)
		{
			float fluidAmount = valveEntity.getFluidAmount();
			float capacity = valveEntity.getCapacity();
			int signalStrength = Utils.getComparatorLevel(fluidAmount, capacity);

			return signalStrength;
		}

		return 0;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		if (!world.isRemote)
		{
			// disband the multiblock if the valve is mined/destroyed
			ValveBlockEntity valveEntity = Utils.getTileEntityAt(world, ValveBlockEntity.class, pos);

			if (valveEntity != null)
			{
				valveEntity.disbandMultiblock();
			}
		}
	}

	@Override
	protected void handleToolWrenchClick(World world, BlockPos pos, EntityPlayer player, ItemStack equippedItemStack)
	{
		// On sneak use: disband the multiblock | On use: rebuild the multiblock

		ValveBlockEntity valveEntity = Utils.getTileEntityAt(world, ValveBlockEntity.class, pos);

		if (player.isSneaking())
		{
			if (valveEntity != null)
			{
				valveEntity.disbandMultiblock();
			}

			world.setBlockToAir(pos);
			dropBlockAsItem(world, pos, this.getDefaultState(), 0);
		}
		else if (valveEntity != null)
		{
			// rebuild the tank
			valveEntity.formMultiblock();
		}
	}
}
