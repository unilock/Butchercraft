package com.lance5057.butchercraft.armor;

import com.lance5057.butchercraft.armor.models.GlovesModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class GlovesItem extends ArmorItem implements IClientItemExtensions {
	public GlovesItem(Holder<ArmorMaterial> pMaterial, Properties pProperties) {
		super(pMaterial, Type.CHESTPLATE, pProperties);
	}

	@Override
	public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
		EntityModelSet models = Minecraft.getInstance().getEntityModels();
		ModelPart root = models.bakeLayer(GlovesModel.LAYER_LOCATION);
		return new GlovesModel(root);
	}

}
