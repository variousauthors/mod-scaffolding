package variousauthors.scaffold;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Optional;

/* TODO got to generalize this, the default behaviour
*   should be multi-slot (OR, have MultiSlotContainerFruit out there) */
public interface ContainerFruit<TE extends TileEntity> {
    TE getTileEntity(IBlockAccess world, BlockPos pos);

    default Optional<IItemHandler> getItemHandler (World worldIn, BlockPos pos) {
        TE tile = getTileEntity(worldIn, pos);

        if (tile == null) return Optional.empty();

        IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);

        return Optional.ofNullable(itemHandler);
    }

    default void getContents(NonNullList<ItemStack> drops, World worldIn, BlockPos pos) {
        getItemHandler(worldIn, pos).ifPresent(itemHandler -> {
            ItemStack stack = itemHandler.getStackInSlot(0);

            if (!stack.isEmpty()) {
                drops.add((stack));
            }
        });
    }

    default NonNullList<ItemStack> insertContents(NonNullList<ItemStack> drops, World worldIn, BlockPos pos) {
        NonNullList<ItemStack> remainder = NonNullList.create();

        return getItemHandler(worldIn, pos)
                .map(itemHandler -> {
                    for (ItemStack drop : drops) {
                        ItemStack result = itemHandler.insertItem(0, drop, false);

                        if (!result.isEmpty()) {
                            remainder.add(result);
                        }
                    }

                    return remainder;
                })
                .orElse(remainder);
    }

    default void extractContents(NonNullList<ItemStack> drops, World worldIn, BlockPos pos, int amount) {
        getItemHandler(worldIn, pos).ifPresent(itemHandler -> {
            ItemStack stack = itemHandler.extractItem(0, amount, false);

            if (!stack.isEmpty()) {
                drops.add((stack));
            }

            ItemStack remainder = itemHandler.getStackInSlot(0);

            if (remainder.isEmpty()) {
                // remove the fruit
                worldIn.removeTileEntity(pos);
                worldIn.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        });
    }

    default boolean isFull (World worldIn, BlockPos pos) {
        return isFull(worldIn, pos, 0);
    }

    default boolean isFull (World worldIn, BlockPos pos, int index) {
        return getItemHandler(worldIn, pos)
                .map(itemHandler -> {
                    ItemStack stack = itemHandler.getStackInSlot(index);

                    return !(stack.getCount() < itemHandler.getSlotLimit(index));
                })
                .orElse(false);
    }

    default int getSlotLimit (World worldIn, BlockPos pos) {
        return getSlotLimit(worldIn, pos, 0);
    }

    default int getSlotLimit (World worldIn, BlockPos pos, int index) {
        return getItemHandler(worldIn, pos)
                .map(itemHandler -> itemHandler.getSlotLimit(index))
                .orElse(64);
    }

    default int getCount (World worldIn, BlockPos pos) {
        return getCount(worldIn, pos, 0);
    }

    default int getCount (World worldIn, BlockPos pos, int index) {
        return getItemHandler(worldIn, pos)
                .map(itemHandler -> {
                    ItemStack stack = itemHandler.getStackInSlot(index);

                    return stack.getCount();
                })
                .orElse(0);
    }
}
