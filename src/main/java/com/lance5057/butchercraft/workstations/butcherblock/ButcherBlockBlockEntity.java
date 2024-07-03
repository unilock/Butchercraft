package com.lance5057.butchercraft.workstations.butcherblock;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.lance5057.butchercraft.ButchercraftBlockEntities;
import com.lance5057.butchercraft.ButchercraftMobEffects;
import com.lance5057.butchercraft.ButchercraftRecipes;
import com.lance5057.butchercraft.armor.ApronItem;
import com.lance5057.butchercraft.armor.BootsItem;
import com.lance5057.butchercraft.armor.GlovesItem;
import com.lance5057.butchercraft.armor.MaskItem;
import com.lance5057.butchercraft.workstations.bases.recipes.AnimatedRecipeItemUse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ButcherBlockBlockEntity extends BlockEntity {
	public int progress;
	public int maxProgress;
	private Ingredient curTool;
	public int toolCount;
	public int stage = 0;

	public ButcherBlockBlockEntity(BlockPos pPos, BlockState pState) {
		super(ButchercraftBlockEntities.BUTCHER_BLOCK.get(), pPos, pState);
	}

	public Optional<ItemStackHandler> getHandler() {
		if (this.getLevel() != null) {
			return Optional.ofNullable((ItemStackHandler) this.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, this.getBlockPos(), this.getBlockState(), this, Direction.NORTH));
		}
		return Optional.empty();
	}

	public void setRecipe(Optional<ButcherBlockRecipe> r) {

		if (r.isPresent()) {
			this.setupStage(r.get(), 0);
		} else
			this.zeroProgress();
	}

	public void zeroProgress() {
		this.progress = 0;
		this.maxProgress = 0;
		this.curTool = null;
		this.toolCount = 0;
		this.stage = 0;
	}

	public Optional<AnimatedRecipeItemUse> getCurrentTool() {
		return matchRecipe().map(ButcherBlockRecipe -> ButcherBlockRecipe.value().tools().get(stage));
	}

	protected void setupStage(ButcherBlockRecipe r, int i) {

		this.progress = 0;
		this.maxProgress = r.tools().get(i).uses();
		this.curTool = r.tools().get(i).tool();
		this.toolCount = r.tools().get(i).count();

		this.stage = i;
	}

	boolean isFinalStage(ButcherBlockRecipe r) {
		int i = r.tools().size();
		if (i - 1 > stage) {
			return false;
		}
		return true;
	}

	// Attempt to find a recipe that matches the tool and the item in its inventory
	private Optional<RecipeHolder<ButcherBlockRecipe>> matchRecipe() {
		if (this.level != null) {
			return level.getRecipeManager().getRecipeFor(ButchercraftRecipes.BUTCHER_BLOCK.get(),
					new ButcherBlockContainer(getInsertedItem()), level);
		}
		return Optional.empty();

	}

	public ItemStackHandler createHandler() {
		return new ItemStackHandler(1) {
			@Override
			protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
				return 1;
			}

			@Override
			protected void onContentsChanged(int slot) {
				updateInventory();
				zeroProgress();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				// Item is only valid if it can find a recipe that uses the inserted item
				boolean recipeWithInputExists = false;
				if (level != null) {
					recipeWithInputExists = level.getRecipeManager().getRecipes().stream()
							.filter(recipe -> recipe.value() instanceof ButcherBlockRecipe)
							.map(recipe -> (ButcherBlockRecipe) recipe.value())
							.anyMatch(ButcherBlockRecipe -> ButcherBlockRecipe.carcass().test(stack));
				}
				return recipeWithInputExists && super.isItemValid(slot, stack);
			}
		};
	}

	public ItemStack getInsertedItem() {
		return getHandler().map(inventory -> inventory.getStackInSlot(0)).orElse(ItemStack.EMPTY);
	}

	public void extractInsertItem(Player playerEntity, InteractionHand hand) {
		getHandler().ifPresent(inventory -> {
			ItemStack held = playerEntity.getItemInHand(hand);
			if (!held.isEmpty()) {
				insertItem(inventory, held);
			} else {
				extractItem(playerEntity, inventory);
			}
		});
		updateInventory();
	}

	public void extractItem(Player playerEntity, IItemHandler inventory) {
		if (!inventory.getStackInSlot(0).isEmpty()) {
			ItemStack itemStack = inventory.extractItem(0, inventory.getStackInSlot(0).getCount(), false);
			playerEntity.addItem(itemStack);
		}
		updateInventory();
	}

	public void insertItem(IItemHandler inventory, ItemStack heldItem) {
		if (inventory.isItemValid(0, heldItem))
			if (!ItemStack.isSameItemSameTags(inventory.insertItem(0, heldItem, true), heldItem)) {
				final int leftover = inventory.insertItem(0, heldItem.copy(), false).getCount();
				heldItem.setCount(leftover);
			}
		updateInventory();
	}

	public boolean isEmpty() {
		return getHandler().map(inventory -> inventory.getStackInSlot(0).isEmpty()).orElse(false);
	}

	// External extract handler
	public void extractItem(Player playerEntity) {
		getHandler().ifPresent(inventory -> this.extractItem(playerEntity, inventory));
	}

	// External insert handler
	public void insertItem(ItemStack heldItem) {
		getHandler().ifPresent(inventory -> this.insertItem(inventory, heldItem));
	}

	public void updateInventory() {
		requestModelDataUpdate();
		this.setChanged();
		if (this.getLevel() != null) {
			getLevel().setBlock(getBlockPos(),
					getBlockState().setValue(ButcherBlockBlock.CARCASS_HOOKED, !getInsertedItem().isEmpty()),
					Block.UPDATE_ALL);
			this.getLevel().sendBlockUpdated(this.worldPosition, this.getBlockState(),
					this.getBlockState().setValue(ButcherBlockBlock.CARCASS_HOOKED, !getInsertedItem().isEmpty()),
					Block.UPDATE_ALL);
		}
	}

	public InteractionResult butcher(Player p, ItemStack butcheringTool) {
		Optional<RecipeHolder<ButcherBlockRecipe>> recipeOptional = matchRecipe();
		if (recipeOptional.isPresent()) {
			ButcherBlockRecipe recipe = recipeOptional.get().value();
			if (this.curTool == null) {
				setupStage(recipe, stage);
			}
			if (this.curTool.test(butcheringTool)) {
				if (butcheringTool.getCount() >= this.toolCount) {
					progress++;
					if (this.progress >= this.maxProgress) {

						if (butcheringTool.isDamageableItem())
							butcheringTool.hurtAndBreak(1, p, (x) -> {
								x.broadcastBreakEvent(EquipmentSlot.MAINHAND);
							});
						else
							butcheringTool.setCount(butcheringTool.getCount() - this.toolCount);

						if (isFinalStage(recipe)) {

//
							dropLoot(recipe.tools().get(stage), p);
							this.finishRecipe();
						} else {
							dropLoot(recipe.tools().get(stage), p);
							setupStage(recipe, stage + 1);
						}

						level.playSound(p, worldPosition, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1, 1);
					} else {
						if (butcheringTool.isDamageableItem())
							butcheringTool.hurtAndBreak(1, p, (x) -> {
								x.broadcastBreakEvent(EquipmentSlot.MAINHAND);
							});
						else
							butcheringTool.setCount(butcheringTool.getCount() - this.toolCount);

						for (int i = 0; i < 1 + level.random.nextInt(4); i++)
							level.addParticle(ParticleTypes.FALLING_DRIPSTONE_LAVA,
									worldPosition.getX() + 0.25f + level.random.nextDouble() / 2,
									worldPosition.getY() - 0.5f - level.random.nextDouble(),
									worldPosition.getZ() + 0.25f + level.random.nextDouble() / 2, 0, 0, 0);

						level.playSound(p, worldPosition, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 1, 1);

					}
				}

				ItemStack boots = p.getInventory().getArmor(0);
				if (boots.getItem() instanceof BootsItem)
					boots.hurtAndBreak(1, p, (x) -> {
						x.broadcastBreakEvent(EquipmentSlot.FEET);
					});
				else
					p.addEffect(new MobEffectInstance(ButchercraftMobEffects.BLOODTRAIL.get(), 3600, 0, false, false,
							true));

				ItemStack apron = p.getInventory().getArmor(1);
				if (apron.getItem() instanceof ApronItem)
					apron.hurtAndBreak(1, p, (x) -> {
						x.broadcastBreakEvent(EquipmentSlot.LEGS);
					});
				else
					p.addEffect(
							new MobEffectInstance(ButchercraftMobEffects.BLOODY.get(), 3600, 0, false, false, true));

				ItemStack gloves = p.getInventory().getArmor(2);
				if (gloves.getItem() instanceof GlovesItem)
					gloves.hurtAndBreak(1, p, (x) -> {
						x.broadcastBreakEvent(EquipmentSlot.CHEST);
					});
				else
					p.addEffect(new MobEffectInstance(ButchercraftMobEffects.DIRTY.get(), 3600, 0, false, false, true));

				ItemStack mask = p.getInventory().getArmor(3);
				if (mask.getItem() instanceof MaskItem)
					mask.hurtAndBreak(1, p, (x) -> {
						x.broadcastBreakEvent(EquipmentSlot.HEAD);
					});
				else
					p.addEffect(
							new MobEffectInstance(ButchercraftMobEffects.STINKY.get(), 3600, 0, false, false, true));

				this.updateInventory();
				return InteractionResult.SUCCESS;
			}

		}

		return InteractionResult.PASS;
	}

	private void dropLoot(AnimatedRecipeItemUse recipeToolsIn, Player player) {
		if (level != null && !level.isClientSide()) {
			final LootParams pParams = new LootParams.Builder((ServerLevel) level)
					.withParameter(LootContextParams.TOOL, player.getMainHandItem())
					.withParameter(LootContextParams.THIS_ENTITY, player)
					.withLuck(player.getLuck()
							+ player.getMainHandItem().getEnchantmentLevel(Enchantments.BLOCK_FORTUNE))
					.create(LootContextParamSets.EMPTY);

			player.getServer().getLootData().getLootTable(recipeToolsIn.lootTable()).getRandomItems(pParams)
					.forEach(itemStack -> {
						level.addFreshEntity(new ItemEntity(level, getBlockPos().getX() + 0.5f,
								getBlockPos().getY() + 1.5f, getBlockPos().getZ() + 0.5f, itemStack, 0, 0, 0));
					});

		}
	}

	public void finishRecipe() {
		getHandler().ifPresent((h -> h.setStackInSlot(0, ItemStack.EMPTY)));
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = super.getUpdateTag();

		writeNBT(nbt);

		return nbt;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		readNBT(tag);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		CompoundTag tag = new CompoundTag();

		writeNBT(tag);

		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		CompoundTag tag = pkt.getTag();
		// InteractionHandle your Data
		readNBT(tag);
	}

	void readNBT(CompoundTag nbt) {

		this.stage = nbt.getInt("stage");
	}

	CompoundTag writeNBT(CompoundTag tag) {

		tag.putInt("stage", stage);

		return tag;
	}

	@Override
	public void load(@Nonnull CompoundTag nbt) {
		super.load(nbt);
		readNBT(nbt);
	}

	@Override
	public void saveAdditional(@Nonnull CompoundTag nbt) {
		super.saveAdditional(nbt);
		writeNBT(nbt);
	}
}
