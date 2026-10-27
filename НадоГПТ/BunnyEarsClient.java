/*     */ package dev.bunnyears;
/*     */ import com.mojang.blaze3d.vertex.PoseStack;
/*     */ import java.util.Locale;
/*     */ import net.fabricmc.api.ClientModInitializer;
/*     */ import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
/*     */ import net.minecraft.client.model.HumanoidModel;
/*     */ import net.minecraft.client.model.geom.ModelPart;
/*     */ import net.minecraft.client.model.geom.PartPose;
/*     */ import net.minecraft.client.model.geom.builders.CubeListBuilder;
/*     */ import net.minecraft.client.model.geom.builders.LayerDefinition;
/*     */ import net.minecraft.client.model.geom.builders.MeshDefinition;
/*     */ import net.minecraft.client.model.geom.builders.PartDefinition;
/*     */ import net.minecraft.client.renderer.SubmitNodeCollector;
/*     */ import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
/*     */ import net.minecraft.client.renderer.rendertype.RenderType;
/*     */ import net.minecraft.client.renderer.rendertype.RenderTypes;
/*     */ import net.minecraft.core.component.DataComponents;
/*     */ import net.minecraft.network.chat.Component;
/*     */ import net.minecraft.resources.Identifier;
/*     */ import net.minecraft.world.entity.EquipmentSlot;
/*     */ import net.minecraft.world.item.ItemStack;
/*     */ import net.minecraft.world.item.Items;
/*     */ import net.minecraft.world.level.ItemLike;
/*     */ 
/*     */ public final class BunnyEarsClient implements ClientModInitializer {
/*  26 */   private static final Identifier EARS_TEXTURE = Identifier.fromNamespaceAndPath("bunnyears", "textures/bunny_ears.png");
/*  27 */   private static final Identifier NETHERITE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/equipment/humanoid/netherite.png");
/*  28 */   private static final RenderType EARS_RENDER_TYPE = RenderTypes.entityCutout(EARS_TEXTURE);
/*  29 */   private static final RenderType ARMOR_RENDER_TYPE = RenderTypes.armorCutoutNoCull(NETHERITE_TEXTURE);
/*     */   
/*     */   private static ModelPart ears;
/*     */   
/*     */   public void onInitializeClient() {
/*  34 */     ears = createEars();
/*  35 */     ArmorRenderer.register(BunnyEarsClient::renderHelmet, new ItemLike[] { (ItemLike)Items.NETHERITE_HELMET });
/*     */   }
/*     */   
/*     */   private static boolean isBunny(ItemStack stack) {
/*  39 */     Component customName = (Component)stack.get(DataComponents.CUSTOM_NAME);
/*  40 */     return (customName != null && customName.getString().trim().toLowerCase(Locale.ROOT).equals("bunny"));
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private static void renderHelmet(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, HumanoidRenderState state, EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> contextModel) {
/*  46 */     if (slot != EquipmentSlot.HEAD)
/*  47 */       return;  poseStack.pushPose();
/*  48 */     contextModel.head.translateAndRotate(poseStack);
/*     */     
/*  50 */     poseStack.translate(0.0D, -0.1D, 0.0D);
/*  51 */     if (isBunny(stack)) {
/*     */       
/*  53 */       animateEars(state);
/*  54 */       collector.submitModelPart(ears, poseStack, EARS_RENDER_TYPE, light, 0, null, false, false, -1, null, 0);
/*     */     } else {
/*     */       
/*  57 */       collector.submitModelPart(contextModel.head, poseStack, ARMOR_RENDER_TYPE, light, 0, null, false, false, -1, null, 0);
/*     */     } 
/*  59 */     poseStack.popPose();
/*     */   }
/*     */ 
/*     */   
/*     */   private static void animateEars(HumanoidRenderState state) {
/*  64 */     if (ears == null)
/*  65 */       return;  float walk = Math.min(1.0F, Math.max(0.0F, state.walkAnimationSpeed));
/*  66 */     float phase = state.walkAnimationPos * 0.65F;
/*  67 */     float sway = (float)Math.sin(phase) * walk * 0.18F;
/*     */     
/*  69 */     float idle = (float)Math.sin((state.ageInTicks * 0.16F)) * 0.025F;
/*  70 */     setSegmentRotation(10, 14, (float)Math.toRadians(-22.5D) + sway + idle);
/*  71 */     setSegmentRotation(15, 19, (float)Math.toRadians(-45.0D) + sway * 1.45F + idle * 1.4F);
/*  72 */     setSegmentRotation(20, 24, -sway + idle);
/*  73 */     setSegmentRotation(25, 29, -sway * 1.45F - idle * 1.4F);
/*  74 */     setSegmentRotation(30, 34, -sway * 1.7F - idle * 1.6F);
/*     */   }
/*     */   
/*     */   private static void setSegmentRotation(int first, int last, float angle) {
/*  78 */     for (int i = first; i <= last; i++) {
/*  79 */       ModelPart part = ears.getChild("bb_" + i);
/*  80 */       part.zRot = angle;
/*     */     } 
/*     */   }
/*     */   
/*     */   private static ModelPart createEars() {
/*  85 */     MeshDefinition mesh = new MeshDefinition();
/*  86 */     PartDefinition root = mesh.getRoot();
/*  87 */     root.addOrReplaceChild("bb_0", CubeListBuilder.create().texOffs(6, 7).addBox(-5.25F, -0.35F, -0.8F, 10.5F, 0.7F, 1.6F), PartPose.offsetAndRotation(0.0F, -4.9F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  88 */     root.addOrReplaceChild("bb_1", CubeListBuilder.create().texOffs(3, 5).addBox(-0.35F, -2.225F, -0.8F, 0.7F, 4.45F, 1.6F), PartPose.offsetAndRotation(-4.9F, -3.025F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  89 */     root.addOrReplaceChild("bb_2", CubeListBuilder.create().texOffs(0, 6).addBox(-0.35F, -2.225F, -0.8F, 0.7F, 4.45F, 1.6F), PartPose.offsetAndRotation(4.9F, -3.025F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  90 */     root.addOrReplaceChild("bb_3", CubeListBuilder.create().texOffs(1, 4).addBox(-1.5F, -0.55F, -1.2F, 3.0F, 1.1F, 2.4F), PartPose.offsetAndRotation(-2.4F, -5.75F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  91 */     root.addOrReplaceChild("bb_4", CubeListBuilder.create().texOffs(6, 2).addBox(-1.5F, -0.55F, -1.2F, 3.0F, 1.1F, 2.4F), PartPose.offsetAndRotation(2.4F, -5.75F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  92 */     root.addOrReplaceChild("bb_5", CubeListBuilder.create().texOffs(11, 2).addBox(-1.054F, -0.97F, -0.702F, 2.108F, 1.37F, 1.404F), PartPose.offsetAndRotation(-2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  93 */     root.addOrReplaceChild("bb_6", CubeListBuilder.create().texOffs(3, 1).addBox(-1.084F, -1.89F, -0.714F, 2.168F, 1.02F, 1.428F), PartPose.offsetAndRotation(-2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  94 */     root.addOrReplaceChild("bb_7", CubeListBuilder.create().texOffs(0, 8).addBox(-1.137F, -2.81F, -0.736F, 2.274F, 1.02F, 1.472F), PartPose.offsetAndRotation(-2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  95 */     root.addOrReplaceChild("bb_8", CubeListBuilder.create().texOffs(4, 8).addBox(-1.207F, -3.73F, -0.766F, 2.414F, 1.02F, 1.532F), PartPose.offsetAndRotation(-2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  96 */     root.addOrReplaceChild("bb_9", CubeListBuilder.create().texOffs(0, 4).addBox(-1.289F, -4.9F, -0.799F, 2.578F, 1.27F, 1.598F), PartPose.offsetAndRotation(-2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/*  97 */     root.addOrReplaceChild("bb_10", CubeListBuilder.create().texOffs(7, 1).addBox(-1.376F, -1.01F, -0.836F, 2.752F, 1.41F, 1.672F), PartPose.offsetAndRotation(-2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-22.5D)));
/*  98 */     root.addOrReplaceChild("bb_11", CubeListBuilder.create().texOffs(3, 3).addBox(-1.464F, -1.97F, -0.872F, 2.928F, 1.06F, 1.744F), PartPose.offsetAndRotation(-2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-22.5D)));
/*  99 */     root.addOrReplaceChild("bb_12", CubeListBuilder.create().texOffs(0, 1).addBox(-1.542F, -2.93F, -0.905F, 3.084F, 1.06F, 1.81F), PartPose.offsetAndRotation(-2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-22.5D)));
/* 100 */     root.addOrReplaceChild("bb_13", CubeListBuilder.create().texOffs(10, 4).addBox(-1.603F, -3.89F, -0.931F, 3.206F, 1.06F, 1.862F), PartPose.offsetAndRotation(-2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-22.5D)));
/* 101 */     root.addOrReplaceChild("bb_14", CubeListBuilder.create().texOffs(0, 4).addBox(-1.641F, -5.1F, -0.946F, 3.282F, 1.31F, 1.892F), PartPose.offsetAndRotation(-2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-22.5D)));
/* 102 */     root.addOrReplaceChild("bb_15", CubeListBuilder.create().texOffs(0, 5).addBox(-1.647F, -0.97F, -0.948F, 3.294F, 1.37F, 1.896F), PartPose.offsetAndRotation(-4.237F, -15.035F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-45.0D)));
/* 103 */     root.addOrReplaceChild("bb_16", CubeListBuilder.create().texOffs(0, 1).addBox(-1.594F, -1.89F, -0.918F, 3.189F, 1.02F, 1.836F), PartPose.offsetAndRotation(-4.237F, -15.035F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-45.0D)));
/* 104 */     root.addOrReplaceChild("bb_17", CubeListBuilder.create().texOffs(10, 3).addBox(-1.467F, -2.81F, -0.845F, 2.934F, 1.02F, 1.69F), PartPose.offsetAndRotation(-4.237F, -15.035F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-45.0D)));
/* 105 */     root.addOrReplaceChild("bb_18", CubeListBuilder.create().texOffs(12, 5).addBox(-1.242F, -3.73F, -0.715F, 2.484F, 1.02F, 1.43F), PartPose.offsetAndRotation(-4.237F, -15.035F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-45.0D)));
/* 106 */     root.addOrReplaceChild("bb_19", CubeListBuilder.create().texOffs(8, 10).addBox(-0.845F, -4.9F, -0.487F, 1.69F, 1.27F, 0.974F), PartPose.offsetAndRotation(-4.237F, -15.035F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(-45.0D)));
/* 107 */     root.addOrReplaceChild("bb_20", CubeListBuilder.create().texOffs(12, 0).addBox(-1.054F, -0.97F, -0.702F, 2.108F, 1.37F, 1.404F), PartPose.offsetAndRotation(2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 108 */     root.addOrReplaceChild("bb_21", CubeListBuilder.create().texOffs(0, 2).addBox(-1.084F, -1.89F, -0.714F, 2.168F, 1.02F, 1.428F), PartPose.offsetAndRotation(2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 109 */     root.addOrReplaceChild("bb_22", CubeListBuilder.create().texOffs(8, 10).addBox(-1.137F, -2.81F, -0.736F, 2.274F, 1.02F, 1.472F), PartPose.offsetAndRotation(2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 110 */     root.addOrReplaceChild("bb_23", CubeListBuilder.create().texOffs(1, 10).addBox(-1.207F, -3.73F, -0.766F, 2.414F, 1.02F, 1.532F), PartPose.offsetAndRotation(2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 111 */     root.addOrReplaceChild("bb_24", CubeListBuilder.create().texOffs(11, 12).addBox(-1.289F, -4.9F, -0.799F, 2.578F, 1.27F, 1.598F), PartPose.offsetAndRotation(2.4F, -6.0F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 112 */     root.addOrReplaceChild("bb_25", CubeListBuilder.create().texOffs(4, 12).addBox(-1.376F, -1.01F, -0.836F, 2.752F, 1.41F, 1.672F), PartPose.offsetAndRotation(2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 113 */     root.addOrReplaceChild("bb_26", CubeListBuilder.create().texOffs(10, 11).addBox(-1.464F, -1.97F, -0.872F, 2.928F, 1.06F, 1.744F), PartPose.offsetAndRotation(2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 114 */     root.addOrReplaceChild("bb_27", CubeListBuilder.create().texOffs(10, 1).addBox(-1.542F, -2.93F, -0.905F, 3.084F, 1.06F, 1.81F), PartPose.offsetAndRotation(2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 115 */     root.addOrReplaceChild("bb_28", CubeListBuilder.create().texOffs(7, 12).addBox(-1.603F, -3.89F, -0.931F, 3.206F, 1.06F, 1.862F), PartPose.offsetAndRotation(2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 116 */     root.addOrReplaceChild("bb_29", CubeListBuilder.create().texOffs(8, 7).addBox(-1.641F, -5.1F, -0.946F, 3.282F, 1.31F, 1.892F), PartPose.offsetAndRotation(2.4F, -10.6F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 117 */     root.addOrReplaceChild("bb_30", CubeListBuilder.create().texOffs(4, 5).addBox(-1.647F, -0.97F, -0.948F, 3.294F, 1.37F, 1.896F), PartPose.offsetAndRotation(2.4F, -15.4F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 118 */     root.addOrReplaceChild("bb_31", CubeListBuilder.create().texOffs(7, 6).addBox(-1.594F, -1.89F, -0.918F, 3.188F, 1.02F, 1.836F), PartPose.offsetAndRotation(2.4F, -15.4F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 119 */     root.addOrReplaceChild("bb_32", CubeListBuilder.create().texOffs(6, 5).addBox(-1.467F, -2.81F, -0.845F, 2.934F, 1.02F, 1.69F), PartPose.offsetAndRotation(2.4F, -15.4F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 120 */     root.addOrReplaceChild("bb_33", CubeListBuilder.create().texOffs(3, 11).addBox(-1.242F, -3.73F, -0.715F, 2.484F, 1.02F, 1.43F), PartPose.offsetAndRotation(2.4F, -15.4F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 121 */     root.addOrReplaceChild("bb_34", CubeListBuilder.create().texOffs(5, 6).addBox(-0.845F, -4.9F, -0.487F, 1.69F, 1.27F, 0.974F), PartPose.offsetAndRotation(2.4F, -15.4F, 0.0F, (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D), (float)Math.toRadians(0.0D)));
/* 122 */     return LayerDefinition.create(mesh, 16, 16).bakeRoot();
/*     */   }
/*     */ }


/* Location:              C:\Users\User\AppData\Roaming\PrismLauncher\instances\26.1.2\minecraft\mods\bunnyears-client-1.0.0.jar!\dev\bunnyears\BunnyEarsClient.class
 * Java compiler version: 25 (69.0)
 * JD-Core Version:       1.1.3
 */