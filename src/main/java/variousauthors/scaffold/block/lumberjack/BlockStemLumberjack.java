package variousauthors.scaffold.block.lumberjack;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.SaplingGrowTreeEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import variousauthors.scaffold.ContainerFruit;
import variousauthors.scaffold.block.BlockStemCucurbit;
import variousauthors.scaffold.block.ModBlocks;

import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

public class BlockStemLumberjack extends BlockStemCucurbit {
    public BlockStemLumberjack(Block crop, String name) {
        super(crop, name);

        /* lumberjack doesn't have its own ticks, it steals them from saplings */
        this.setTickRandomly(false);
    }

    @SubscribeEvent
    public static void onSaplingGrowTree(SaplingGrowTreeEvent event) {
        World worldIn = event.getWorld();

        if (worldIn.isRemote) {
            return;
        }

        // find the stem block near the sapling
        BlockPos eventPos = event.getPos();
        BlockPos from = eventPos.west().north();
        BlockPos to = eventPos.east().south();
        Iterator<BlockPos> neighbourhood = BlockPos.getAllInBox(from, to).iterator();

        BlockPos stemPos = null;

        while (neighbourhood.hasNext() && stemPos == null) {
            BlockPos current = neighbourhood.next();
            Block block = worldIn.getBlockState(current).getBlock();

            if (block == ModBlocks.stemLumberjack) {
                // stop that sapling!
                event.setResult(Event.Result.DENY);
                worldIn.scheduleUpdate(current, block, 0);
            }
        }
    }

    /*called whenever a sapling growth tick is eaten */
    @Override
    protected void growFruit(World worldIn, BlockPos stemPos, IBlockState state, Random rand) {
        if (cropIsAlreadyGrown(worldIn, stemPos)) {
            // if crop is already grown, do the crop grown version
            tryToFeedCrop(worldIn, stemPos);
        } else {
            /* TODO should this really be mutating the parameter? Vanilla does this. */
            BlockPos targetPos = stemPos.offset(EnumFacing.Plane.HORIZONTAL.random(rand));

            if (!canGrowCropAtPos(worldIn, targetPos)) return;

            tryToGrowCrop(worldIn, stemPos, targetPos);
        }
    }

    private void tryToFeedCrop(World worldIn, BlockPos stemPos) {
        findCropMatchingStem(worldIn, stemPos).ifPresent(cropPos -> {
            Block crop = worldIn.getBlockState(cropPos).getBlock();

            if (!(crop instanceof ContainerFruit)) return;

            ContainerFruit fruit = (ContainerFruit) crop;

            if (fruit.isFull(worldIn, cropPos)) return;

            findFuelBlockInWorld(worldIn, stemPos, cropPos).ifPresent(fuelPos -> {
                IBlockState fuelState = worldIn.getBlockState(fuelPos);
                NonNullList<ItemStack> drops = extractDropsFromFuel(worldIn, fuelPos, fuelState, 0, FUEL_EXTRACTION_RATE);

                /* maybe we should throw here, since we've already checked in a previous method that
                 * the drops were not empty... maybe just during dev? */
                if (drops.isEmpty()) return;

                /* we are not doing anything with the remainder right now
                 * but maybe later we can... */
                insert(worldIn, cropPos, drops);
            });
        });
    }

    /* this will need to be a JSON config file
    * because there is no clean way to get the log type from the sapling type
    * and this implementation is not extensible */
    private NonNullList<ItemStack> extractDropsFromFuel(World worldIn, BlockPos fuelPos, IBlockState fuelState, int fortune, int amount) {
        Block fuelBlock = worldIn.getBlockState(fuelPos).getBlock();
        NonNullList<ItemStack> drops = NonNullList.create();

        if (fuelBlock instanceof BlockSapling) {
            IBlockState saplingState = worldIn.getBlockState(fuelPos);
            IBlockState logState = getLogStateFromSapling(saplingState);

            drops.add(getItemStackFromLogState(logState, amount));
        }

        return drops;
    }

    private ItemStack getItemStackFromLogState (IBlockState logState, int amount) {
        Block logBlock = logState.getBlock();
        return new ItemStack(logBlock, amount, logBlock.getMetaFromState(logState));
    }

    private IBlockState getLogStateFromSapling(IBlockState saplingState) {
        switch (saplingState.getValue(BlockSapling.TYPE)) {
            case OAK:
                return (Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.OAK));
            case SPRUCE:
                return (Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE));
            case BIRCH:
                return (Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.BIRCH));
            case JUNGLE:
                return (Blocks.LOG2.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE));
            case ACACIA:
                return (Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.ACACIA));
            case DARK_OAK:
            default:
                return (Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.DARK_OAK));
        }
    }

    /* returns the drops from an eligible block in the world */
    private NonNullList<ItemStack> getDropsFromFuel(World worldIn, BlockPos fuelPos, IBlockState fuelState, int fortune) {
        Block fuelBlock = worldIn.getBlockState(fuelPos).getBlock();
        NonNullList<ItemStack> drops = NonNullList.create();

        if (fuelBlock instanceof BlockSapling) {
            IBlockState saplingState = worldIn.getBlockState(fuelPos);

            switch ((BlockPlanks.EnumType) saplingState.getValue(BlockSapling.TYPE)) {
                case SPRUCE:
                case ACACIA:
                case DARK_OAK:
                case OAK:
                case BIRCH:
                case JUNGLE:
                default:
                    IBlockState logState = Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.ACACIA);
                    drops.add(new ItemStack(logState.getBlock()));
                    break;
            }
        }

        return drops;
    }

    final private int FUEL_EXTRACTION_RATE = 1;

    @Override
    protected void tryToGrowCrop(World worldIn, BlockPos stemPos, BlockPos targetPos) {
        findFuelBlockInWorld(worldIn, stemPos, targetPos).ifPresent(fuelPos -> {
            IBlockState fuelState = worldIn.getBlockState(fuelPos);
            NonNullList<ItemStack> drops = extractDropsFromFuel(worldIn, fuelPos, fuelState, 0, FUEL_EXTRACTION_RATE);

            /* maybe we should throw here, since we've already checked in a previous method that
             * the drops were not empty... maybe just during dev? */
            if (drops.isEmpty()) return;

            if (!worldIn.setBlockState(targetPos, this.crop.getDefaultState())) return;

            insert(worldIn, targetPos, drops);
        });
    }

    @Override
    protected Optional<BlockPos> findFuelBlockInWorld(World worldIn, BlockPos stemPos, BlockPos fruitPos) {
        BlockPos from = stemPos.west().north();
        BlockPos to = stemPos.east().south();

        Iterator<BlockPos> neighbourhood = BlockPos.getAllInBox(from, to).iterator();
        BlockPos fuelPos = null;

        while (neighbourhood.hasNext() && fuelPos == null) {
            BlockPos current = neighbourhood.next();

            // skip the middle and the fruit
            if (current.equals(stemPos) || current.equals(fruitPos)) {
                continue;
            }

            IBlockState blockState = worldIn.getBlockState(current);

            if (blockState.getBlock() instanceof BlockSapling) {
                fuelPos = current;
            }
        }

        return Optional.ofNullable(fuelPos);
    }


    private void insert(World worldIn, BlockPos pos, NonNullList<ItemStack> drops) {
        Block block = worldIn.getBlockState(pos).getBlock();

        if (isContainerFruit(block)) {
            ContainerFruit fruit = (ContainerFruit) block;

            if (!fruit.isFull(worldIn, pos)) {
                fruit.insertContents(drops, worldIn, pos);
            }
        }
    }

    protected boolean isHarvestable(Block fuelBlock) {
        return fuelBlock instanceof BlockCrops;
    }

    protected boolean isContainerFruit(Block block) {
        return block instanceof ContainerFruit || block instanceof BlockChest;
    }

    protected boolean isContainerFruit(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).getBlock() instanceof ContainerFruit
                || worldIn.getBlockState(pos).getBlock() instanceof BlockChest;
    }
}
