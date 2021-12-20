package variousauthors.scaffold.block;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import variousauthors.scaffold.block.counter.BlockCounter;
import variousauthors.scaffold.block.lavamelon.BlockLavamelon;
import variousauthors.scaffold.block.lavamelon.BlockStemLavamelon;
import variousauthors.scaffold.block.pedestal.BlockPedestal;

public class ModBlocks {
    public static BlockOre oreCopper = new BlockOre("ore_copper", "oreCopper");
    public static BlockCropCorn cropCorn = new BlockCropCorn();
    public static BlockPedestal pedestal = new BlockPedestal();

    public static BlockCounter counter = new BlockCounter();

    public static BlockLavamelon lavamelon = new BlockLavamelon();
    public static BlockStemLavamelon stemLavamelon = new BlockStemLavamelon(lavamelon, "stem_lavamelon");

    public static void register(IForgeRegistry<Block> registry) {
        registry.registerAll(
                oreCopper,
                cropCorn,
                pedestal,
                counter,
                lavamelon,
                stemLavamelon
        );

        GameRegistry.registerTileEntity(pedestal.getTileEntityClass(), pedestal.getRegistryName().toString());
    }

    public static void registerItemBlocks(IForgeRegistry<Item> registry) {
        registry.registerAll(
                oreCopper.createItemBlock(),
                pedestal.createItemBlock(),
                counter.createItemBlock(),
                lavamelon.createItemBlock()
        );
    }

    public static void registerModels() {
        oreCopper.registerItemModel();
        pedestal.registerItemModel();
        counter.registerItemModel();
        lavamelon.registerItemModel();
    }

}