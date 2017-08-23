package com.github.technus.tectech;

import com.github.technus.tectech.auxiliary.Reference;
import com.github.technus.tectech.auxiliary.TecTechConfig;
import com.github.technus.tectech.elementalMatter.definitions.dAtomDefinition;
import com.github.technus.tectech.loader.AtomOverrider;
import com.github.technus.tectech.loader.MainLoader;
import com.github.technus.tectech.proxy.CommonProxy;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import eu.usrv.yamcore.auxiliary.IngameErrorLog;
import eu.usrv.yamcore.auxiliary.LogHelper;
import gregtech.api.enums.Materials;
import gregtech.api.util.GT_Recipe;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.HashMap;

import static com.github.technus.tectech.auxiliary.TecTechConfig.DEBUG_MODE;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION, dependencies = "required-after:Forge@[10.13.4.1614,);"
        + "required-after:YAMCore@[0.5.70,);" + "required-after:gregtech;" + "after:CoFHCore;" + "after:Thaumcraft;" + "after:dreamcraft;" + "after:miscutils;")
public class TecTech {
    @SidedProxy(clientSide = Reference.CLIENTSIDE, serverSide = Reference.SERVERSIDE)
    public static CommonProxy proxy;

    @Instance(Reference.MODID)
    public static TecTech instance;

    public static final LogHelper Logger = new LogHelper(Reference.MODID);
    private static IngameErrorLog Module_AdminErrorLogs = null;
    public static MainLoader GTCustomLoader = null;
    public static TecTechConfig ModConfig;
    public static XSTR Rnd = null;
    public static CreativeTabs mainTab = null;
    private static boolean oneTimeFix = false;

    public static boolean hasCOFH = false, hasThaumcraft = false;

    public static final byte tectechTexturePage1=8;

    public static void AddLoginError(String pMessage) {
        if (Module_AdminErrorLogs != null)
            Module_AdminErrorLogs.AddErrorLogOnAdminJoin(pMessage);
    }

    @EventHandler
    public void PreLoad(FMLPreInitializationEvent PreEvent) {
        Logger.setDebugOutput(true);
        Rnd = new XSTR();

        ModConfig = new TecTechConfig(PreEvent.getModConfigurationDirectory(), Reference.COLLECTIONNAME,
                Reference.MODID);

        if (!ModConfig.LoadConfig())
            Logger.error(Reference.MODID + " could not load its config file. Things are going to be weird!");

        if (ModConfig.ModAdminErrorLogs_Enabled) {
            Logger.debug("Module_AdminErrorLogs is enabled");
            Module_AdminErrorLogs = new IngameErrorLog();
        }

        proxy.addTexturePage(tectechTexturePage1);

        GTCustomLoader = new MainLoader();

        dAtomDefinition.overrides.add(new AtomOverrider());
        TecTech.Logger.info("Added Atom Overrider");
    }

    @EventHandler
    public void Load(FMLInitializationEvent event) {
        hasCOFH = Loader.isModLoaded(Reference.COFHCORE);
        hasThaumcraft = Loader.isModLoaded(Reference.THAUMCRAFT);

        GTCustomLoader.load();

        proxy.registerRenderInfo();
    }

    @EventHandler
    public void PostLoad(FMLPostInitializationEvent PostEvent) {
        GTCustomLoader.postLoad();
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent pEvent) {
    }

    @EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent ev) {
        if (!oneTimeFix) {
            oneTimeFix = true;
            if (ModConfig.NERF_FUSION) FixBrokenFusionRecipes();
            fixBlocks();
        }
    }

    private void FixBrokenFusionRecipes() {
        HashMap<Fluid, Fluid> binds = new HashMap<>();
        for (Materials m : Materials.values()) {
            FluidStack p = m.getPlasma(1);
            if (p != null) {
                if (DEBUG_MODE)
                    TecTech.Logger.info("Found Plasma of " + m.mName);
                if (m.mElement != null &&
                        (m.mElement.mProtons >= Materials.Iron.mElement.mProtons ||
                                -m.mElement.mProtons >= Materials.Iron.mElement.mProtons ||
                                m.mElement.mNeutrons >= Materials.Iron.mElement.mNeutrons ||
                                -m.mElement.mNeutrons >= Materials.Iron.mElement.mNeutrons)) {
                    if (DEBUG_MODE)
                        TecTech.Logger.info("Attempting to bind " + m.mName);
                    if (m.getMolten(1) != null) binds.put(p.getFluid(), m.getMolten(1).getFluid());
                    else if (m.getGas(1) != null) binds.put(p.getFluid(), m.getGas(1).getFluid());
                    else if (m.getFluid(1) != null) binds.put(p.getFluid(), m.getFluid(1).getFluid());
                    else binds.put(p.getFluid(), Materials.Iron.getMolten(1).getFluid());
                }
            }
        }
        for (GT_Recipe r : GT_Recipe.GT_Recipe_Map.sFusionRecipes.mRecipeList) {
            Fluid f = binds.get(r.mFluidOutputs[0].getFluid());
            if (f != null) {
                if (DEBUG_MODE)
                    TecTech.Logger.info("Nerfing Recipe " + r.mFluidOutputs[0].getUnlocalizedName());
                r.mFluidOutputs[0] = new FluidStack(f, r.mFluidInputs[0].amount);
            }
        }
    }

    private void fixBlocks(){
        String modId;
        for(Block block : GameData.getBlockRegistry().typeSafeIterable()) {
            modId = GameRegistry.findUniqueIdentifierFor(block).modId;
            if (//Full Whitelisted Mods
                            modId.equals("minecraft") ||
                            modId.equals("IC2") ||
                            modId.equals("gregtech") ||
                            modId.equals("dreamcraft") ||
                            modId.equals("miscutils") ||
                            modId.equals("GT++DarkWorld") ||
                            modId.equals("TwilightForest") ||
                            modId.equals("GalacticraftCore") ||
                            modId.equals("GalacticraftMars") ||
                            modId.equals("GalaxySpace") ||
                            //modId.equals("EnderIO") || // nerf
                            //modId.equals("Thaumcraft") || // too op, nerf
                            modId.equals("extracells") ||
                            modId.equals("ExtraUtilities") ||
                            modId.equals("Avaritia") ||
                            modId.equals("avaritiaddons") ||
                            modId.equals("EnderStorage") ||
                            modId.equals("enhancedportals") ||
                            modId.equals("DraconicEvolution") ||
                            modId.equals("IC2NuclearControl") ||
                            modId.equals("IronChest") ||
                            modId.equals("opensecurity") ||
                            modId.equals("openmodularturrets") ||
                            modId.equals("Railcraft") ||
                            modId.equals("RIO") ||
                            modId.equals("SGCraft") ||
                            modId.equals("appliedenergistics2") ||
                            modId.equals("thaumicenergistics") ||
                            modId.equals("witchery") ||
                            modId.equals("lootgames") ||
                            modId.equals(Reference.MODID) ||
                            modId.equals("utilityworlds")) {
                continue;
            } else if (modId.equals("TConstruct")) {
                block.slipperiness = 1;//cos we know it is slippery, right Greg?
            } else if (modId.equals("OpenBlocks")) {
                if (GameRegistry.findUniqueIdentifierFor(block).name.equals("grave"))
                    continue;
            }
            block.setResistance(6);
        }
    }
}
