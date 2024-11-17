package tech.williamist.bettermapdisplay.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.ItemFrameRenderer;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.map.SavedMapData;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin extends EntityRenderer {

	@Inject(
			// this is a terribly named method lol.
			// it has nothing to do with maps.
			method = "renderFrameForMap",
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private void mapNoRenderVanillaFrame(ItemFrameEntity itemFrame, CallbackInfo ci) {
		ItemStack is = itemFrame.getItemStackInItemFrame();
		if (is != null && is.getItem() == Item.FILLED_MAP) {
			ci.cancel();
		}
	}

	private static final float WORLD_PIXEL = 1f / 16f;

	@Inject(
			method = "renderItem",
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private void mapRenderOverride(ItemFrameEntity itemFrame, CallbackInfo ci) {
		ItemStack is = itemFrame.getItemStackInItemFrame();
		if (is == null || is.getItem() != Item.FILLED_MAP) {
			return;
		}

		ci.cancel();

		// setup transformation
		GL11.glPushMatrix();
		GL11.glRotatef(itemFrame.yaw, 0, 1, 0);
		GL11.glRotatef(180f + 90f * itemFrame.rotation(), 0, 0, 1);
		GL11.glTranslatef(-0.5f, -0.5f, 0.5f);

		// draw frame
		renderMapFrame();

		// more transformation
		GL11.glTranslatef(0, 0, -0.0001f - WORLD_PIXEL); // hopefully no z-fighting, yeah?
		GL11.glScalef(1f / 128f, 1f / 128f, 1f / 128f);

		GL11.glNormal3f(0, 0, -1);

		// draw the actual map
		SavedMapData mapData = Item.FILLED_MAP.getSavedMapData(is, itemFrame.world);
		if (mapData != null) {
			this.dispatcher.heldItemRenderer.mapRenderer.render(null, this.dispatcher.textureManager, mapData);
		}

		GL11.glPopMatrix();
	}

	private void renderMapFrame() {
		BufferBuilder bb = BufferBuilder.INSTANCE;

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.dispatcher.textureManager.load("/terrain.png"));

		final float TEX_PIXEL   = 1f  / 256f;
		final float TEX_PIXEL16 = 16f / 256f;

		final float BIRCH_PLANKS_U = 96f  / 256f;
		final float BIRCH_PLANKS_V = 208f / 256f;

		final float ITEM_FRAME_U = 144f / 256f;
		final float ITEM_FRAME_V = 176f / 256f;

		bb.start();

		// left border
		bb.normal(-1, 0, 0);
		bb.vertex(0, 0, 0, BIRCH_PLANKS_U, BIRCH_PLANKS_V);
		bb.vertex(0, 1, 0, BIRCH_PLANKS_U, BIRCH_PLANKS_V + TEX_PIXEL16);
		bb.vertex(0, 1, -WORLD_PIXEL, BIRCH_PLANKS_U + TEX_PIXEL, BIRCH_PLANKS_V + TEX_PIXEL16);
		bb.vertex(0, 0, -WORLD_PIXEL, BIRCH_PLANKS_U + TEX_PIXEL, BIRCH_PLANKS_V);

		// right border
		bb.normal(1, 0, 0);
		bb.vertex(1, 0, 0, BIRCH_PLANKS_U + TEX_PIXEL16, BIRCH_PLANKS_V);
		bb.vertex(1, 0, -WORLD_PIXEL, BIRCH_PLANKS_U + TEX_PIXEL16 - TEX_PIXEL, BIRCH_PLANKS_V);
		bb.vertex(1, 1, -WORLD_PIXEL, BIRCH_PLANKS_U + TEX_PIXEL16 - TEX_PIXEL, BIRCH_PLANKS_V + TEX_PIXEL16);
		bb.vertex(1, 1, 0, BIRCH_PLANKS_U + TEX_PIXEL16, BIRCH_PLANKS_V + TEX_PIXEL16);

		// top border
		bb.normal(0, 1, 0);
		bb.vertex(0, 0, 0, BIRCH_PLANKS_U, BIRCH_PLANKS_V);
		bb.vertex(0, 0, -WORLD_PIXEL, BIRCH_PLANKS_U, BIRCH_PLANKS_V + TEX_PIXEL);
		bb.vertex(1, 0, -WORLD_PIXEL, BIRCH_PLANKS_U + TEX_PIXEL16, BIRCH_PLANKS_V + TEX_PIXEL);
		bb.vertex(1, 0, 0, BIRCH_PLANKS_U + TEX_PIXEL16, BIRCH_PLANKS_V);

		// bottom border
		bb.normal(0, -1, 0);
		bb.vertex(0, 1, 0, BIRCH_PLANKS_U, BIRCH_PLANKS_V + TEX_PIXEL16 - TEX_PIXEL);
		bb.vertex(1, 1, 0, BIRCH_PLANKS_U + TEX_PIXEL16, BIRCH_PLANKS_V + TEX_PIXEL16 - TEX_PIXEL);
		bb.vertex(1, 1, -WORLD_PIXEL, BIRCH_PLANKS_U + TEX_PIXEL16, BIRCH_PLANKS_V + TEX_PIXEL16);
		bb.vertex(0, 1, -WORLD_PIXEL, BIRCH_PLANKS_U, BIRCH_PLANKS_V + TEX_PIXEL16);

		// frame
		bb.normal(0, 0, -1);
		bb.vertex(0, 0, -WORLD_PIXEL, ITEM_FRAME_U, ITEM_FRAME_V);
		bb.vertex(0, 1, -WORLD_PIXEL, ITEM_FRAME_U, ITEM_FRAME_V + TEX_PIXEL16);
		bb.vertex(1, 1, -WORLD_PIXEL, ITEM_FRAME_U + TEX_PIXEL16, ITEM_FRAME_V + TEX_PIXEL16);
		bb.vertex(1, 0, -WORLD_PIXEL, ITEM_FRAME_U + TEX_PIXEL16, ITEM_FRAME_V);

		bb.end();
	}

}
