package shadows.apotheosis.ench.table;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.EnchantmentScreen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.entity.model.BookModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.EnchantmentContainer;
import net.minecraft.util.EnchantmentNameParts;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import shadows.apotheosis.Apotheosis;

public class EnchantmentScreenExt extends EnchantmentScreen {

	private static final ResourceLocation ENCHANTMENT_TABLE_GUI_TEXTURE = new ResourceLocation(Apotheosis.MODID, "textures/gui/enchanting_table.png");
	private static final ResourceLocation ENCHANTMENT_TABLE_BOOK_TEXTURE = new ResourceLocation("textures/entity/enchanting_table_book.png");
	private static final BookModel MODEL_BOOK = new BookModel();
	protected final EnchantmentContainerExt container;

	public EnchantmentScreenExt(EnchantmentContainer container, PlayerInventory inv, ITextComponent title) {
		super(container, inv, title);
		this.container = (EnchantmentContainerExt) container;
		this.ySize = 197;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int p_146979_1_, int p_146979_2_) {
		this.font.drawString(this.title.getFormattedText(), 12.0F, 5.0F, 4210752);
		this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 7.0F, this.ySize - 96 + 4F, 4210752);
		this.font.drawString(I18n.format("gui.apotheosis.enchant.power"), 19, 74, 0x737300);
		this.font.drawString(I18n.format("gui.apotheosis.enchant.rarity"), 19, 84, 0x005A73);
		this.font.drawString(I18n.format("gui.apotheosis.enchant.count"), 19, 94, 0x891F00);
	}

	@Override
	public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;

		for (int k = 0; k < 3; ++k) {
			double d0 = p_mouseClicked_1_ - (i + 60);
			double d1 = p_mouseClicked_3_ - (j + 14 + 19 * k);
			if (d0 >= 0.0D && d1 >= 0.0D && d0 < 108.0D && d1 < 19.0D && this.container.enchantItem(this.minecraft.player, k)) {
				this.minecraft.playerController.sendEnchantPacket(this.container.windowId, k);
				return true;
			}
		}

		return super.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
	}

	/**
	 * Draws the background layer of this container (behind the items).
	 */
	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		RenderHelper.disableGuiDepthLighting();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bindTexture(ENCHANTMENT_TABLE_GUI_TEXTURE);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.blit(i, j, 0, 0, this.xSize, this.ySize);
		RenderSystem.matrixMode(5889);
		RenderSystem.pushMatrix();
		RenderSystem.loadIdentity();
		int k = (int) this.minecraft.getWindow().getGuiScaleFactor();
		RenderSystem.viewport((this.width - 320) / 2 * k, (this.height - 240) / 2 * k, 320 * k, 240 * k);
		RenderSystem.translatef(-0.34F, 0.23F, 0.0F);
		RenderSystem.multMatrix(Matrix4f.perspective(90.0D, 1.3333334F, 9.0F, 80.0F));
		RenderSystem.matrixMode(5888);
		MatrixStack matrixstack = new MatrixStack();
		matrixstack.push();
		MatrixStack.Entry matrixstack$entry = matrixstack.peek();

		matrixstack$entry.getModel().loadIdentity();
		matrixstack$entry.getNormal().loadIdentity();
		matrixstack.translate(0.0D, 5, 1984.0D);
		matrixstack.scale(5.0F, 5.0F, 5.0F);
		matrixstack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(180.0F));
		matrixstack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(20.0F));
		float f1 = MathHelper.lerp(p_146976_1_, this.oOpen, this.open);

		matrixstack.translate((1.0F - f1) * 0.2F, (1.0F - f1) * 0.1F, (1.0F - f1) * 0.25F);
		float f2 = -(1.0F - f1) * 90.0F - 90.0F;
		matrixstack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(f2));
		matrixstack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(180.0F));

		float f3 = MathHelper.lerp(p_146976_1_, this.oFlip, this.flip) + 0.25F;
		float f4 = MathHelper.lerp(p_146976_1_, this.oFlip, this.flip) + 0.75F;
		f3 = (f3 - MathHelper.fastFloor(f3)) * 1.6F - 0.3F;
		f4 = (f4 - MathHelper.fastFloor(f4)) * 1.6F - 0.3F;
		if (f3 < 0.0F) {
			f3 = 0.0F;
		}

		if (f4 < 0.0F) {
			f4 = 0.0F;
		}

		if (f3 > 1.0F) {
			f3 = 1.0F;
		}

		if (f4 > 1.0F) {
			f4 = 1.0F;
		}

		RenderSystem.enableRescaleNormal();
		MODEL_BOOK.setPageAngles(0.0F, f3, f4, f1);
		IRenderTypeBuffer.Impl irendertypebuffer$impl = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuffer());
		IVertexBuilder ivertexbuilder = irendertypebuffer$impl.getBuffer(MODEL_BOOK.getLayer(ENCHANTMENT_TABLE_BOOK_TEXTURE));
		MODEL_BOOK.render(matrixstack, ivertexbuilder, 15728880, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
		irendertypebuffer$impl.draw();
		matrixstack.pop();

		j = (this.height - this.ySize) / 2;

		RenderSystem.matrixMode(5889);
		RenderSystem.viewport(0, 0, this.minecraft.getWindow().getFramebufferWidth(), this.minecraft.getWindow().getFramebufferHeight());
		RenderSystem.popMatrix();
		RenderSystem.matrixMode(5888);
		RenderHelper.enableGuiDepthLighting();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		EnchantmentNameParts.getInstance().reseedRandomGenerator(this.container.func_217005_f());
		int l = this.container.getLapisAmount();

		for (int i1 = 0; i1 < 3; ++i1) {
			int j1 = i + 60;
			int k1 = j1 + 20;
			this.setBlitOffset(0);
			this.minecraft.getTextureManager().bindTexture(ENCHANTMENT_TABLE_GUI_TEXTURE);

			int l1 = this.container.enchantLevels[i1];
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			if (l1 == 0) {
				this.blit(j1, j + 14 + 19 * i1, 148, 218, 108, 19);
			} else {
				String s = "" + l1;
				int i2 = 86 - this.font.getStringWidth(s);
				String s1 = EnchantmentNameParts.getInstance().generateNewRandomName(this.font, i2);
				FontRenderer fontrenderer = this.minecraft.getFontResourceManager().getFontRenderer(Minecraft.standardGalacticFontRenderer);
				int j2 = 6839882;
				if ((l < i1 + 1 || this.minecraft.player.experienceLevel < l1) && !this.minecraft.player.abilities.isCreativeMode || this.container.enchantClue[i1] == -1) { // Forge: render buttons as disabled when enchantable but enchantability not met on lower levels
					this.blit(j1, j + 14 + 19 * i1, 148, 218, 108, 19);
					this.blit(j1 + 1, j + 15 + 19 * i1, 16 * i1, 238, 16, 16);
					fontrenderer.drawSplitString(s1, k1, j + 16 + 19 * i1, i2, (j2 & 16711422) >> 1);
					j2 = 4226832;
				} else {
					int k2 = p_146976_2_ - (i + 60);
					int l2 = p_146976_3_ - (j + 14 + 19 * i1);
					if (k2 >= 0 && l2 >= 0 && k2 < 108 && l2 < 19) {
						this.blit(j1, j + 14 + 19 * i1, 148, 237, 108, 19);
						j2 = 16777088;
					} else {
						this.blit(j1, j + 14 + 19 * i1, 148, 199, 108, 19);
					}

					this.blit(j1 + 1, j + 15 + 19 * i1, 16 * i1, 223, 16, 16);
					fontrenderer.drawSplitString(s1, k1, j + 16 + 19 * i1, i2, j2);
					j2 = 8453920;
				}
				fontrenderer = this.minecraft.fontRenderer;
				fontrenderer.drawStringWithShadow(s, k1 + 86 - fontrenderer.getStringWidth(s), j + 16 + 19 * i1 + 7, j2);
			}
		}

		this.minecraft.getTextureManager().bindTexture(ENCHANTMENT_TABLE_GUI_TEXTURE);
		//if (this.container.power.get() > 0) {
		//	this.blit(i + 59, j + 75, 0, 197, (int) (this.container.power.get() / 35.2 * 110), 5);
		//}

	}

	@Override
	public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
		p_render_3_ = this.minecraft.getRenderPartialTicks();
		this.renderBackground();
		super.render(p_render_1_, p_render_2_, p_render_3_);
		this.renderHoveredToolTip(p_render_1_, p_render_2_);
		boolean flag = this.minecraft.player.abilities.isCreativeMode;
		int i = this.container.getLapisAmount();

		for (int j = 0; j < 3; ++j) {
			int k = this.container.enchantLevels[j];
			Enchantment enchantment = Enchantment.getEnchantmentByID(this.container.enchantClue[j]);
			int l = this.container.worldClue[j];
			int i1 = j + 1;
			if (this.isPointInRegion(60, 14 + 19 * j, 108, 17, p_render_1_, p_render_2_) && k > 0) {
				List<String> list = Lists.newArrayList();
				list.add("" + TextFormatting.WHITE + TextFormatting.ITALIC + I18n.format("container.enchant.clue", enchantment == null ? "" : enchantment.getDisplayName(l).getFormattedText()));
				if (enchantment == null) {
					java.util.Collections.addAll(list, "", TextFormatting.RED + I18n.format("forge.container.enchant.limitedEnchantability"));
				} else if (!flag) {
					list.add("");
					if (this.minecraft.player.experienceLevel < k) {
						list.add(TextFormatting.RED + I18n.format("container.enchant.level.requirement", this.container.enchantLevels[j]));
					} else {
						String s;
						if (i1 == 1) {
							s = I18n.format("container.enchant.lapis.one");
						} else {
							s = I18n.format("container.enchant.lapis.many", i1);
						}

						TextFormatting textformatting = i >= i1 ? TextFormatting.GRAY : TextFormatting.RED;
						list.add(textformatting + "" + s);
						if (i1 == 1) {
							s = I18n.format("container.enchant.level.one");
						} else {
							s = I18n.format("container.enchant.level.many", i1);
						}

						list.add(TextFormatting.GRAY + "" + s);
					}
				}

				this.renderTooltip(list, p_render_1_, p_render_2_);
				break;
			}
		}

	}

}