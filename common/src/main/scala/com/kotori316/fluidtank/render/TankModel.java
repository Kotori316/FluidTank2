package com.kotori316.fluidtank.render;

import com.kotori316.fluidtank.FluidTankCommon;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

public final class TankModel extends Model<Unit> {
    public static final ModelLayerLocation LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "tank"), "main");

    public TankModel(ModelPart root) {
        super(root, RenderType::entityCutout);
    }

    public static LayerDefinition createDefinition() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild("container",
            CubeListBuilder.create().texOffs(0, 0).addBox(2, 0, 2, 12, 16, 12),
            PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 64, 32);
    }
}
