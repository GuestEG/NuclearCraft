package nc.multiblock.heatExchanger;

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import nc.Global;
import nc.config.NCConfig;
import nc.multiblock.ILogicMultiblock;
import nc.multiblock.ITileMultiblockPart;
import nc.multiblock.Multiblock;
import nc.multiblock.TileBeefBase.SyncReason;
import nc.multiblock.container.ContainerHeatExchangerController;
import nc.multiblock.cuboidal.CuboidalMultiblock;
import nc.multiblock.heatExchanger.tile.IHeatExchangerController;
import nc.multiblock.heatExchanger.tile.IHeatExchangerPart;
import nc.multiblock.heatExchanger.tile.TileCondenserTube;
import nc.multiblock.heatExchanger.tile.TileHeatExchangerController;
import nc.multiblock.heatExchanger.tile.TileHeatExchangerTube;
import nc.multiblock.heatExchanger.tile.TileHeatExchangerVent;
import nc.multiblock.network.HeatExchangerUpdatePacket;
import nc.util.NCMath;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class HeatExchanger extends CuboidalMultiblock<HeatExchangerUpdatePacket> implements ILogicMultiblock<HeatExchangerLogic, IHeatExchangerPart> {
	
	public static final ObjectSet<Class<? extends IHeatExchangerPart>> PART_CLASSES = new ObjectOpenHashSet<>();
	public static final Object2ObjectMap<String, Constructor<? extends HeatExchangerLogic>> LOGIC_MAP = new Object2ObjectOpenHashMap<>();
	
	protected final ObjectSet<IHeatExchangerController> controllers = new ObjectOpenHashSet<>();
	protected final ObjectSet<TileHeatExchangerVent> vents = new ObjectOpenHashSet<>();
	protected final ObjectSet<TileHeatExchangerTube> tubes = new ObjectOpenHashSet<>();
	protected final ObjectSet<TileCondenserTube> condenserTubes = new ObjectOpenHashSet<>();
	
	protected @Nonnull HeatExchangerLogic logic = new HeatExchangerLogic(this);
	
	protected final PartSuperMap<IHeatExchangerPart> partSuperMap = new PartSuperMap<>();
	
	protected IHeatExchangerController controller;
	
	protected int updateCount = 0;
	
	public boolean isHeatExchangerOn, computerActivated;
	public double fractionOfTubesActive, efficiency, maxEfficiency;
	
	public HeatExchanger(World world) {
		super(world);
	}
	
	@Override
	public @Nonnull HeatExchangerLogic getLogic() {
		return logic;
	}
	
	@Override
	public void setLogic(String logicID) {
		if (logicID.equals(logic.getID())) return;
		logic = getNewLogic(LOGIC_MAP.get(logicID));
	}
	
	// Multiblock Part Getters
	
	@Override
	public PartSuperMap<IHeatExchangerPart> getPartSuperMap() {
		return partSuperMap;
	}
	
	public ObjectSet<IHeatExchangerController> getControllers() {
		return controllers;
	}
	
	public ObjectSet<TileHeatExchangerVent> getVents() {
		return vents;
	}
	
	public ObjectSet<TileHeatExchangerTube> getTubes() {
		return tubes;
	}
	
	public ObjectSet<TileCondenserTube> getCondenserTubes() {
		return condenserTubes;
	}
	
	// Multiblock Size Limits
	
	@Override
	protected int getMinimumInteriorLength() {
		return NCConfig.heat_exchanger_min_size;
	}
	
	@Override
	protected int getMaximumInteriorLength() {
		return NCConfig.heat_exchanger_max_size;
	}
	
	@Override
	protected int getMinimumNumberOfBlocksForAssembledMachine() {
		return NCMath.hollowCuboid(Math.max(4, getMinimumInteriorLength() + 2), getMinimumInteriorLength() + 2, getMinimumInteriorLength() + 2);
	}
	
	// Multiblock Methods
	
	@Override
	public void onAttachedPartWithMultiblockData(ITileMultiblockPart part, NBTTagCompound data) {
		syncDataFrom(data, SyncReason.FullSync);
	}
	
	@Override
	protected void onBlockAdded(ITileMultiblockPart newPart) {
		if (newPart instanceof IHeatExchangerController) controllers.add((IHeatExchangerController) newPart);
		if (newPart instanceof TileHeatExchangerVent) vents.add((TileHeatExchangerVent) newPart);
		if (newPart instanceof TileHeatExchangerTube) tubes.add((TileHeatExchangerTube) newPart);
		if (newPart instanceof TileCondenserTube) condenserTubes.add((TileCondenserTube) newPart);
	}
	
	@Override
	protected void onBlockRemoved(ITileMultiblockPart oldPart) {
		if (oldPart instanceof IHeatExchangerController) controllers.remove(oldPart);
		if (oldPart instanceof TileHeatExchangerVent) vents.remove(oldPart);
		if (oldPart instanceof TileHeatExchangerTube) tubes.remove(oldPart);
		if (oldPart instanceof TileCondenserTube) condenserTubes.remove(oldPart);
	}
	
	@Override
	protected void onMachineAssembled() {
		onHeatExchangerFormed();
	}
	
	@Override
	protected void onMachineRestored() {
		onHeatExchangerFormed();
	}
	
	protected void onHeatExchangerFormed() {
		for (IHeatExchangerController contr : controllers) controller = contr;
		setIsHeatExchangerOn();
		
		if (!WORLD.isRemote) {
			for (TileHeatExchangerTube tube : tubes) tube.updateFlowDir();
			for (TileCondenserTube condenserTube : condenserTubes) condenserTube.updateAdjacentTemperatures();
			
			updateHeatExchangerStats();
		}
	}
	
	@Override
	protected void onMachinePaused() {
		
	}
	
	@Override
	protected void onMachineDisassembled() {
		isHeatExchangerOn = false;
		if (controller != null) controller.updateBlockState(false);
		fractionOfTubesActive = efficiency = maxEfficiency = 0D;
	}
	
	@Override
	protected boolean isMachineWhole(Multiblock multiblock) {
		
		// Only one controller
		
		if (controllers.size() == 0) {
			multiblock.setLastError(Global.MOD_ID + ".multiblock_validation.no_controller", null);
			return false;
		}
		if (controllers.size() > 1) {
			multiblock.setLastError(Global.MOD_ID + ".multiblock_validation.too_many_controllers", null);
			return false;
		}
		
		return super.isMachineWhole(multiblock);
	}
	
	@Override
	protected void onAssimilate(Multiblock assimilated) {
		
	}
	
	@Override
	protected void onAssimilated(Multiblock assimilator) {
		
	}
	
	// Server
	
	@Override
	protected boolean updateServer() {
		//setIsHeatExchangerOn();
		if (shouldUpdate()) updateHeatExchangerStats();
		if (shouldUpdate()) sendUpdateToListeningPlayers();
		incrementUpdateCount();
		return true;
	}
	
	public void setIsHeatExchangerOn() {
		boolean oldIsHeatExchangerOn = isHeatExchangerOn;
		isHeatExchangerOn = (isRedstonePowered() || computerActivated) && isAssembled();
		if (isHeatExchangerOn != oldIsHeatExchangerOn) {
			if (controller != null) controller.updateBlockState(isHeatExchangerOn);
			sendUpdateToAllPlayers();
		}
	}
	
	protected boolean isRedstonePowered() {
		if (controller != null && controller.checkIsRedstonePowered(WORLD, controller.getTilePos())) return true;
		return false;
	}
	
	protected void updateHeatExchangerStats() {
		int numberOfTubes = tubes.size() + condenserTubes.size();
		if (numberOfTubes < 1) {
			fractionOfTubesActive = efficiency = maxEfficiency = 0D;
			return;
		}
		int activeCount = 0, efficiencyCount = 0, maxEfficiencyCount = 0;
		
		for (TileHeatExchangerTube tube : tubes) {
			int[] eff = tube.checkPosition();
			if (eff[0] > 0) activeCount++;
			efficiencyCount += eff[0];
			maxEfficiencyCount += eff[1];
		}
		
		for (TileCondenserTube condenserTube : condenserTubes) {
			int eff = condenserTube.checkPosition();
			if (eff > 0) activeCount++;
			efficiencyCount += eff;
			maxEfficiencyCount += eff;
		}
		
		fractionOfTubesActive = (double)activeCount/numberOfTubes;
		efficiency = activeCount == 0 ? 0D : (double)efficiencyCount/activeCount;
		maxEfficiency = (double)maxEfficiencyCount/numberOfTubes;
	}
	
	protected void incrementUpdateCount() {
		updateCount++; updateCount %= updateTime();
	}
	
	protected static int updateTime() {
		return NCConfig.machine_update_rate;
	}
	
	protected boolean shouldUpdate() {
		return updateCount == 0;
	}
	
	// Client
	
	@Override
	protected void updateClient() {
		
	}
	
	// NBT
	
	@Override
	protected void syncDataTo(NBTTagCompound data, SyncReason syncReason) {
		data.setBoolean("isHeatExchangerOn", isHeatExchangerOn);
		data.setBoolean("computerActivated", computerActivated);
		data.setDouble("fractionOfTubesActive", fractionOfTubesActive);
		data.setDouble("efficiency", efficiency);
		data.setDouble("maxEfficiency", maxEfficiency);
		
		writeLogicNBT(data, syncReason);
	}
	
	@Override
	protected void syncDataFrom(NBTTagCompound data, SyncReason syncReason) {
		isHeatExchangerOn = data.getBoolean("isHeatExchangerOn");
		computerActivated = data.getBoolean("computerActivated");
		fractionOfTubesActive = data.getDouble("fractionOfTubesActive");
		efficiency = data.getDouble("efficiency");
		maxEfficiency = data.getDouble("maxEfficiency");
		
		readLogicNBT(data, syncReason);
	}
	
	// Packets
	
	@Override
	protected HeatExchangerUpdatePacket getUpdatePacket() {
		return new HeatExchangerUpdatePacket(controller.getTilePos(), isHeatExchangerOn, fractionOfTubesActive, efficiency, maxEfficiency);
	}
	
	@Override
	public void onPacket(HeatExchangerUpdatePacket message) {
		isHeatExchangerOn = message.isHeatExchangerOn;
		fractionOfTubesActive = message.fractionOfTubesActive;
		efficiency = message.efficiency;
		maxEfficiency = message.maxEfficiency;
	}
	
	public Container getContainer(EntityPlayer player) {
		return new ContainerHeatExchangerController(player, (TileHeatExchangerController)controller);
	}
	
	@Override
	public void clearAllMaterial() {
		ILogicMultiblock.super.clearAllMaterial();
	}
	
	// Multiblock Validators
	
	@Override
	protected boolean isBlockGoodForInterior(World world, int x, int y, int z, Multiblock multiblock) {
		return true;
	}
}
