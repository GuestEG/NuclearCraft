package nc.tile.passive;

import nc.block.tile.passive.BlockAcceleratorElectromagnet;
import nc.block.tile.passive.BlockCobblestoneGenerator;
import nc.block.tile.passive.BlockElectromagnetSupercooler;
import nc.block.tile.passive.BlockFusionElectromagnet;
import nc.block.tile.passive.BlockHeliumCollector;
import nc.block.tile.passive.BlockWaterSource;
import nc.config.NCConfig;
import nc.init.NCFluids;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;

public class Passives {
	
	public static abstract class TileElectromagnet extends TilePassive {
		
		public TileElectromagnet(String name, int energyChange) {
			super(name + "_electromagnet", -energyChange, 5);
		}
	}
	
	public static class TileFusionElectromagnet extends TileElectromagnet {
		
		public TileFusionElectromagnet() {
			super("fusion", NCConfig.fusion_electromagnet_power);
		}
		
		public void setBlockState() {
			BlockFusionElectromagnet.setState(isRunning, world, pos);
		}
	}
	
	public static class TileAcceleratorElectromagnet extends TileElectromagnet {
		
		public TileAcceleratorElectromagnet() {
			super("accelerator", NCConfig.accelerator_electromagnet_power);
		}
		
		public void setBlockState() {
			BlockAcceleratorElectromagnet.setState(isRunning, world, pos);
		}
	}
	
	public static class TileElectromagnetSupercooler extends TilePassive {
		
		public TileElectromagnetSupercooler() {
			super("electromagnet_supercooler", -NCConfig.accelerator_electromagnet_power, NCFluids.liquidhelium, -NCConfig.accelerator_supercooler_coolant, 5);
		}
		
		public void setBlockState() {
			BlockElectromagnetSupercooler.setState(isRunning, world, pos);
		}
	}
	
	public static class TileHeliumCollector extends TilePassive {
		
		public TileHeliumCollector() {
			super("helium_collector", NCFluids.helium, NCConfig.processor_passive_rate[0], 5);
		}
		
		public void setBlockState() {
			BlockHeliumCollector.setState(world, pos);
		}
	}
	
	public static class TileCobblestoneGenerator extends TilePassive {
		
		public TileCobblestoneGenerator() {
			super("cobblestone_generator", new ItemStack(Blocks.COBBLESTONE), NCConfig.processor_passive_rate[1], 5);
		}
		
		public void setBlockState() {
			BlockCobblestoneGenerator.setState(world, pos);
		}
	}
	
	public static class TileWaterSource extends TilePassive {
		
		public TileWaterSource() {
			super("water_source", FluidRegistry.WATER, NCConfig.processor_passive_rate[2], 5);
		}
		
		public void setBlockState() {
			BlockWaterSource.setState(world, pos);
		}
	}
}
