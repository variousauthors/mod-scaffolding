package variousauthors.scaffold.block.bakers_squash;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BlockBakersSquash extends BlockTileEntity<TileEntityBakersSquash> implements CanRegisterItemBlock, ContainerFruit<TileEntityBakersSquash> {
    // baker's squash swell as they fill up with items
    // 0 - 20 | 21 - 41 | 42 - 63 | 64
    public static final PropertyInteger SWELL = PropertyInteger.create("swell", 0, 3);

    // the direction to the stem, default is north
    // this is used to determine rotation of the model
    public static final PropertyDirection STEM_DIR = PropertyDirection.create("stem_dir", Arrays.asList(EnumFacing.HORIZONTALS));

    protected static final AxisAlignedBB[][] STEM_AABB = new AxisAlignedBB[][]{
            // south
            new AxisAlignedBB[] {
                    new AxisAlignedBB(0.625D, 0.625D, 0.75D, 0.375D, 0.0D, 1.0D),
                    new AxisAlignedBB(0.75D, 0.75D, 0.5D, 0.25D, 0.0D, 1.0D),
                    new AxisAlignedBB(0.875D, 0.875D, 0.25D, 0.125D, 1.0D, 1.0D),
                    new AxisAlignedBB(1.0D, 1.0D, 0.0D, 0.0D, 0.0D, 1.0D)
            },
            // west
            new AxisAlignedBB[] {
                    new AxisAlignedBB(0.0D, 0.0D, 0.375D, 0.25D, 0.625D, 0.625D),
                    new AxisAlignedBB(0.0D, 0.0D, 0.25D, 0.5D, 0.75D, 0.75D),
                    new AxisAlignedBB(0.0D, 0.0D, 0.125D, 0.75D, 0.875D, 0.875D),
                    new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)
            },
            // north
            new AxisAlignedBB[] {
                    new AxisAlignedBB(0.625D, 0.625D, 0.25D, 0.375D, 0.0D, 0.0D),
                    new AxisAlignedBB(0.75D, 0.75D, 0.5D, 0.25D, 0.0D, 0.0D),
                    new AxisAlignedBB(0.875D, 0.875D, 0.75D, 0.125D, 0.0D, 0.0D),
                    new AxisAlignedBB(1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D)
            },
            // east
            new AxisAlignedBB[] {
                    new AxisAlignedBB(1D, 0.0D, 0.375D,  0.75D, 0.625D, 0.625D),
                    new AxisAlignedBB(1D, 0.0D, 0.25D, 0.5D, 0.75D, 0.75D),
                    new AxisAlignedBB(1D, 0.0D, 0.125D, 0.25D, 0.875D, 0.875D),
                    new AxisAlignedBB(1D, 0.0D, 0.0D, 0D, 1D, 1D)
            },
    };

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        int facing = state.getValue(STEM_DIR).getHorizontalIndex();
        AxisAlignedBB aabb = STEM_AABB[facing][((Integer)state.getValue(SWELL)).intValue()];

        return aabb;
    }

    public BlockBakersSquash(String name) {
        super(Material.ROCK, name);

        this.setDefaultState(this.blockState.getBaseState().withProperty(SWELL, Integer.valueOf(0)).withProperty(STEM_DIR, EnumFacing.NORTH));
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube(IBlockState state) {
        return state.getValue(SWELL).intValue() == 3;
    }

    @Override
    @Deprecated
    public boolean isFullCube(IBlockState state) {
        return state.getValue(SWELL).intValue() == 3;
    }

    public IBlockState getInitialState(BlockPos fruitPos, BlockPos stemPos) {
        BlockPos vec = stemPos.subtract(fruitPos);
        EnumFacing stemDir = EnumFacing.getFacingFromVector(vec.getX(), vec.getY(), vec.getZ());

        return this.getDefaultState().withProperty(STEM_DIR, stemDir);
    }

    /** @TODO need to deal with the AABB, see BlockStem for details */

    /**
     * sets the fruit swell to the given percent
     */
    public void swellFruit(World worldIn, BlockPos pos, IBlockState state, double percent) {
        double a = percent * 3;
        int b = Math.min(3, (int) a);

        ContainerFruit fruit = (ContainerFruit) state.getBlock();

        TileEntityBakersSquash te = (TileEntityBakersSquash) worldIn.getTileEntity(pos);

        worldIn.setBlockState(pos, state.withProperty(SWELL, b), 2);
    }

    /**
     * Whether this IGrowable can grow
     */
    public boolean canSwell(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return ((Integer) state.getValue(SWELL)).intValue() != 3;
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    public int quantityDropped(Random random) {
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
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(SWELL, Integer.valueOf(meta >> 2))
                .withProperty(STEM_DIR, EnumFacing.getHorizontal(meta & 0b0011));
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    public int getMetaFromState(IBlockState state) {
        return state.getValue(SWELL) << 2 | state.getValue(STEM_DIR).getHorizontalIndex();
    }

    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{SWELL, STEM_DIR});
    }
}
