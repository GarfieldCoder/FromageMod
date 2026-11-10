package fromage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Blockbench project: blockbench/cheese_golem.bbmodel */
final class CheeseGolemModel extends EntityModel<CheeseGolem> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath("fromage", "cheese_golem"), "main");
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    CheeseGolemModel(ModelPart root) {
        body = root.getChild("body");
        head = body.getChild("head");
        rightArm = body.getChild("rightArm");
        leftArm = body.getChild("leftArm");
        rightLeg = body.getChild("rightLeg");
        leftLeg = body.getChild("leftLeg");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(62, 0).addBox(-8.0F, -16.0F, -6.0F, 16.0F, 20.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(0, 120).addBox(-4.0F, -15.0F, -8.0F, 8.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 36).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 13.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(23, 120).addBox(-3.0F, -13.0F, -8.0F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -15.0F, 0.0F));

        PartDefinition rightHorn = head.addOrReplaceChild("rightHorn", CubeListBuilder.create().texOffs(90, 62).addBox(-3.0F, -13.0F, -3.0F, 7.0F, 14.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -11.0F, 0.0F, 0.2443F, -0.1047F, 0.384F));

        PartDefinition rightHornTip = rightHorn.addOrReplaceChild("rightHornTip", CubeListBuilder.create().texOffs(27, 84).addBox(-3.0F, -9.0F, -2.0F, 7.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -11.0F, -1.0F, 0.1047F, -0.0349F, 0.2793F));

        PartDefinition rightArm = body.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, -4.0F, 7.0F, 27.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(0, 62).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 9.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(81, 84).addBox(2.0F, 3.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(74, 105).addBox(-3.0F, 3.0F, -8.0F, 6.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(11.0F, -13.0F, 0.0F));

        PartDefinition rightLeg = body.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(49, 36).addBox(-3.0F, -1.0F, -4.0F, 6.0F, 16.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(0, 105).addBox(-4.0F, 11.0F, -5.0F, 8.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 3.0F, 0.0F));

        PartDefinition leftHorn = head.addOrReplaceChild("leftHorn", CubeListBuilder.create().texOffs(0, 84).addBox(-4.0F, -13.0F, -3.0F, 7.0F, 14.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -11.0F, 0.0F, 0.2443F, 0.1047F, -0.384F));

        PartDefinition leftHornTip = leftHorn.addOrReplaceChild("leftHornTip", CubeListBuilder.create().texOffs(54, 84).addBox(-4.0F, -9.0F, -2.0F, 7.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -11.0F, -1.0F, 0.1047F, 0.0349F, -0.2793F));

        PartDefinition leftArm = body.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(31, 0).addBox(-4.0F, 0.0F, -4.0F, 7.0F, 27.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(45, 62).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 9.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(96, 84).addBox(-5.0F, 3.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(93, 105).addBox(-3.0F, 3.0F, -8.0F, 6.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-11.0F, -13.0F, 0.0F));

        PartDefinition leftLeg = body.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(78, 36).addBox(-3.0F, -1.0F, -4.0F, 6.0F, 16.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(37, 105).addBox(-4.0F, 11.0F, -5.0F, 8.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 3.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 144);
    }

    @Override
    public void setupAnim(CheeseGolem entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        body.xRot = 0;
        body.yRot = 0;
        body.zRot = Mth.sin(ageInTicks * 0.08F) * 0.03F;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.55F;
        leftArm.xRot = swing * 0.55F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                               int color) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
