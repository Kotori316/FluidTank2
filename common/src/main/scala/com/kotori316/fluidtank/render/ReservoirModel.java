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

public final class ReservoirModel extends Model {
    public static final ModelLayerLocation LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "reservoir"), "main");

    public ReservoirModel(ModelPart root) {
        super(root, RenderType::entityCutout);
    }

    public static LayerDefinition createDefinition() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild("container",
            CubeListBuilder.create().texOffs(0, 0).addBox(2, 0, 0, 12, 16, 1),
            PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 32, 32);
    }
}
