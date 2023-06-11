package variousauthors.scaffold.block.bakers_squash;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import variousauthors.scaffold.ContainerFruit;
import variousauthors.scaffold.block.BlockStemCucurbit;

import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

/** The Baker's Squash stem search for harvestable blocks that have a furnace recipe
 * and it fills up with their products. It will not break just any block! */
public class BlockStemBakersSquash extends BlockStemCucurbit
{
    public BlockStemBakersSquash(Block crop, String name)
    {
        super(crop, name);
    }

    @Override
    protected void growFruit(World worldIn, BlockPos stemPos, IBlockState state, Random rand) {
        System.out.println("BlockStemBakersSquash->growFruit");
        if (cropIsAlreadyGrown(worldIn, stemPos)) {
            System.out.println("crop is grown");
            // if crop is already grown, do the crop grown version
            tryToFeedCrop(worldIn, stemPos);
        } else {
            System.out.println("plant new crop");
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

            System.out.println("BlockStemBakersSquash->tryToFeedCrop");
            System.out.println(fruit.getCount(worldIn, cropPos));
            if (fruit.isFull(worldIn, cropPos)) return;

            findFuelBlockInWorld(worldIn, stemPos, cropPos).ifPresent(fuelPos -> {
                System.out.println("BlockStemBakersSquash->tryToFeedCrop->fuel");

                IBlockState fuelState = worldIn.getBlockState(fuelPos);
                System.out.println(fuelState);
                NonNullList<ItemStack> drops = extractDropsFromFuel(worldIn, fuelPos, fuelState, 0, FUEL_EXTRACTION_RATE);
                System.out.println(drops);
                /* maybe we should throw here, since we've already checked in a previous method that
                 * the drops were not empty... maybe just during dev? */
                if (drops.isEmpty()) return;

                /* we are not doing anything with the remainder right now
                 * but maybe later we can... */
                cookAndInsert(worldIn, cropPos, drops);
            });
        });
    }

    /** this breaks the block, gets the drops, or asks the fruit to extract its contents */
    private NonNullList<ItemStack> extractDropsFromFuel(World worldIn, BlockPos fuelPos, IBlockState fuelState, int fortune, int amount) {
        System.out.println("extractDropsFromFuel");
        Block fuelBlock = worldIn.getBlockState(fuelPos).getBlock();
        NonNullList<ItemStack> drops = NonNullList.create();
        System.out.println(drops);

        if (fuelBlock instanceof ContainerFruit) {
            ((ContainerFruit<?>) fuelBlock).extractContents(drops, worldIn, fuelPos, amount);
        } else if (fuelBlock instanceof BlockCrops) {
            System.out.println("extractDropsFromFuel->crop");

            // for now we will just use potato
            fuelBlock.getDrops(drops, worldIn, fuelPos, fuelState, fortune);
            System.out.println(drops);

            boolean hasGrabbedSeed = false;
            // remove a seed item to replant
            for (int i = 0; i < drops.size(); i++) {
                ItemStack drop = drops.get(i);

                if (!hasGrabbedSeed && drop.isItemEqual(fuelBlock.getItem(worldIn, fuelPos, fuelState))) {
                    drop.shrink(1);
                    hasGrabbedSeed = true;
                }
            }
            System.out.println(drops);
            // reset the crop to its initial state
            worldIn.setBlockState(fuelPos, fuelBlock.getDefaultState());
        }

        return drops;
    }

    /* returns the drops from an eligible block in the world */
    private NonNullList<ItemStack> getDropsFromFuel(World worldIn, BlockPos fuelPos, IBlockState fuelState, int fortune) {
        Block fuelBlock = worldIn.getBlockState(fuelPos).getBlock();
        NonNullList<ItemStack> drops = NonNullList.create();

        if (fuelBlock instanceof ContainerFruit) {
            ((ContainerFruit) fuelBlock).getContents(drops, worldIn, fuelPos);
        } else if (isHarvestable(fuelBlock)){
            fuelBlock.getDrops(drops, worldIn, fuelPos, fuelState, fortune);

            boolean hasGrabbedSeed = false;
            // remove a seed item to replant
            for (int i = 0; i < drops.size(); i++) {
                ItemStack drop = drops.get(i);

                if (!hasGrabbedSeed && drop.isItemEqual(fuelBlock.getItem(worldIn, fuelPos, fuelState))) {
                    drop.shrink(1);
                    hasGrabbedSeed = true;
                }
            }
        }

        return drops;
    }

    final private int FUEL_EXTRACTION_RATE = 8;

    @Override
    protected void tryToGrowCrop(World worldIn, BlockPos stemPos, BlockPos targetPos) {
        findFuelBlockInWorld(worldIn, stemPos, targetPos).ifPresent(fuelPos -> {
            IBlockState fuelState = worldIn.getBlockState(fuelPos);
            NonNullList<ItemStack> drops = extractDropsFromFuel(worldIn, fuelPos, fuelState, 0, FUEL_EXTRACTION_RATE);

            /* maybe we should throw here, since we've already checked in a previous method that
            * the drops were not empty... maybe just during dev? */
            if (drops.isEmpty()) return;

            BlockBakersSquash crop = (BlockBakersSquash) this.crop;

            if (!worldIn.setBlockState(targetPos, crop.getInitialState(targetPos, stemPos))) return;

            /* we are not doing anything with the remainder right now
            * but maybe later we can... */
            cookAndInsert(worldIn, targetPos, drops);
        });
    }

    /* this is here for now, the logic is that the stem does the cooking, and then
    * feeds the cooked drops into the fruit, which is just a container */
    /* cook and insert will do bounds checking on the fruit, and fail to insert if the fruit is full */
    private void cookAndInsert(World worldIn, BlockPos pos, NonNullList<ItemStack> drops) {
        System.out.println("BlockStemBakersSquash->cookAndInsert");
        System.out.println(drops);

        NonNullList<ItemStack> cooked = NonNullList.create();

        for (ItemStack drop : drops) {
            ItemStack output = FurnaceRecipes.instance().getSmeltingResult(drop).copy();

            if (!output.isEmpty()) {
                output.setCount(drop.getCount());
                cooked.add(output);
            }
        }

        if (cooked.isEmpty()) return;

        Block block = worldIn.getBlockState(pos).getBlock();
        System.out.println("fruit block state");
        System.out.println(block.getBlockState());

        if (isContainerFruit(block)) {
            ContainerFruit fruit = (ContainerFruit) block;

            if (!fruit.isFull(worldIn, pos)) {
                fruit.insertContents(cooked, worldIn, pos);

                // we know this to be true
                BlockBakersSquash squash = (BlockBakersSquash) block;
                double percent = (double) fruit.getCount(worldIn, pos) / (double) fruit.getSlotLimit(worldIn, pos);
                System.out.println("before swell");
                System.out.println(fruit.getCount(worldIn, pos));
                System.out.println(fruit.getSlotLimit(worldIn, pos));
                System.out.println(percent);

                if (squash.canSwell(worldIn, pos, worldIn.getBlockState(pos), worldIn.isRemote)) {
                    squash.swellFruit(worldIn, pos, worldIn.getBlockState(pos), percent);
                }

                System.out.println("after swell");
                System.out.println(fruit.getCount(worldIn, pos));
                System.out.println(fruit.getSlotLimit(worldIn, pos));
                System.out.println(percent);
            }
        }
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

            /* returns only the drops that are fuel */
            NonNullList<ItemStack> drops = getDropsFromFuel(worldIn, current, blockState, 0);

            for (ItemStack drop : drops) {
                if (!FurnaceRecipes.instance().getSmeltingResult(drop).isEmpty()) {
                    fuelPos = current;
                }
            }
        }

        return Optional.ofNullable(fuelPos);
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
