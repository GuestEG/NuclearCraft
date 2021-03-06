package nc.recipe.processor;

import static nc.util.FissionHelper.FISSION_FLUID;

import nc.recipe.ProcessorRecipeHandler;
import nc.util.FluidStackHelper;

public class ElectrolyzerRecipes extends ProcessorRecipeHandler {
	
	public ElectrolyzerRecipes() {
		super("electrolyzer", 0, 1, 0, 4);
	}

	@Override
	public void addRecipes() {
		addRecipe(fluidStack("water", FluidStackHelper.BUCKET_VOLUME), fluidStack("hydrogen", FluidStackHelper.BUCKET_VOLUME*2), fluidStack("oxygen", FluidStackHelper.BUCKET_VOLUME), emptyFluidStack(), emptyFluidStack(), 1D, 1D);
		addRecipe(fluidStack("heavy_water", FluidStackHelper.BUCKET_VOLUME), fluidStack("deuterium", FluidStackHelper.BUCKET_VOLUME*2), fluidStack("oxygen", FluidStackHelper.BUCKET_VOLUME), emptyFluidStack(), emptyFluidStack(), 1D, 1D);
		addRecipe(fluidStack("hydrofluoric_acid", FluidStackHelper.BUCKET_VOLUME), fluidStack("hydrogen", FluidStackHelper.BUCKET_VOLUME), fluidStack("fluorine", FluidStackHelper.BUCKET_VOLUME), emptyFluidStack(), emptyFluidStack(), 1D, 0.5D);
		
		addRecipe(fluidStack("naoh", FluidStackHelper.GEM_VOLUME), fluidStack("sodium", FluidStackHelper.INGOT_VOLUME), fluidStack("water", FluidStackHelper.BUCKET_VOLUME), fluidStack("oxygen", FluidStackHelper.BUCKET_VOLUME/2), emptyFluidStack(), 1.5D, 1.5D);
		addRecipe(fluidStack("koh", FluidStackHelper.GEM_VOLUME), fluidStack("potassium", FluidStackHelper.INGOT_VOLUME), fluidStack("water", FluidStackHelper.BUCKET_VOLUME), fluidStack("oxygen", FluidStackHelper.BUCKET_VOLUME/2), emptyFluidStack(), 1.5D, 1.5D);
		
		addRecipe(fluidStack("alumina", FluidStackHelper.INGOT_VOLUME), fluidStack("aluminum", 2*FluidStackHelper.INGOT_VOLUME), fluidStack("oxygen", 3*FluidStackHelper.BUCKET_VOLUME), emptyFluidStack(), emptyFluidStack(), 2D, 1D);
		
		// Fission Materials
		addFissionFluorideRecipes();
		addFissionZARecipes();
	}
	
	public void addFissionZARecipes() {
		for (int i = 0; i < FISSION_FLUID.length; i++) {
			addRecipe(fluidStack(FISSION_FLUID[i] + "_za", FluidStackHelper.INGOT_VOLUME), fluidStack(FISSION_FLUID[i], FluidStackHelper.INGOT_VOLUME), fluidStack("zirconium", FluidStackHelper.INGOT_VOLUME), emptyFluidStack(), emptyFluidStack(), 0.5D, 1D);
		}
	}
	
	public void addFissionFluorideRecipes() {
		for (int i = 0; i < FISSION_FLUID.length; i++) {
			addRecipe(fluidStack(FISSION_FLUID[i] + "_fluoride", FluidStackHelper.INGOT_VOLUME), fluidStack(FISSION_FLUID[i], FluidStackHelper.INGOT_VOLUME), fluidStack("fluorine", FluidStackHelper.BUCKET_VOLUME), emptyFluidStack(), emptyFluidStack(), 0.5D, 1D);
			addRecipe(fluidStack("depleted_" + FISSION_FLUID[i] + "_fluoride", FluidStackHelper.INGOT_VOLUME), fluidStack("depleted_" + FISSION_FLUID[i], FluidStackHelper.INGOT_VOLUME), fluidStack("fluorine", FluidStackHelper.BUCKET_VOLUME), emptyFluidStack(), emptyFluidStack(), 0.5D, 1D);
		}
	}
}
