package variousauthors.scaffold.block.bakers_squash;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import scala.Int;
import variousauthors.scaffold.CanRegisterItemBlock;
import variousauthors.scaffold.ContainerFruit;
import variousauthors.scaffold.block.BlockTileEntity;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockBakersSquash extends BlockTileEntity<TileEntityBakersSquash> implements CanRegisterItemBlock, ContainerFruit<TileEntityBakersSquash> {
    // baker's squash swell as they fill up with items
    // 0 - 15 | 16 - 31 | 32 - 47 | 48 - 63 | 64
    public static final PropertyInteger SWELL = PropertyInteger.create("swell", 0, 4);

    public BlockBakersSquash(String name)
    {
        super(Material.ROCK, name);

        this.setDefaultState(this.blockState.getBaseState().withProperty(SWELL, Integer.valueOf(0)));
    }

    /** @TODO need to deal with the AABB, see BlockStem for details */

    /** sets the fruit swell to the given percent */
    public void swellFruit(World worldIn, BlockPos pos, IBlockState state, double percent)
    {
        double a = percent * 4;
        int b = Math.min(4, (int) a);

        ContainerFruit fruit = (ContainerFruit) state.getBlock();

        System.out.println("swellFruit-before");
        System.out.println(fruit.getCount(worldIn, pos));

        TileEntityBakersSquash te = (TileEntityBakersSquash) worldIn.getTileEntity(pos);

        worldIn.setBlockState(pos, state.withProperty(SWELL, b), 2);

        System.out.println("swellFruit-after");
        System.out.println(fruit.getCount(worldIn, pos));
    }

    /**
     * Whether this IGrowable can grow
     */
    public boolean canSwell(World worldIn, BlockPos pos, IBlockState state, boolean isClient)
    {
        return ((Integer)state.getValue(SWELL)).intValue() != 4;
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    public int quantityDropped(Random random)
    {
        return 0;
    }

    @Override
    public Class<TileEntityBakersSquash> getTileEntityClass() {
        return TileEntityBakersSquash.class;
    }

    @Nullable
    @Override
    public TileEntityBakersSquash createTileEntity(World world, IBlockState state) {
        return new TileEntityBakersSquash();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntityBakersSquash tile = getTileEntity(world, pos);

        if (null == tile) return;

        IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);

        if (null == itemHandler) return;

        ItemStack stack = itemHandler.getStackInSlot(0);

        if (!stack.isEmpty()) {
            EntityItem item = new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            world.spawnEntity(item);
        }
        super.breakBlock(world, pos, state);
    }

    /**
     * Convert the given metadata into a BlockState for this Block
     */
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(SWELL, Integer.valueOf(meta));
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    public int getMetaFromState(IBlockState state)
    {
        return ((Integer)state.getValue(SWELL)).intValue();
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {SWELL});
    }
}
