/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.machines.multiblocks;

import aztech.modern_industrialization.machines.blockentities.multiblocks.LargeTankMultiblockBlockEntity;
import aztech.modern_industrialization.util.RenderHelper;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public class MultiblockTankBER extends MultiblockMachineBER {
    public MultiblockTankBER(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(MultiblockMachineBlockEntity be, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        super.render(be, tickDelta, ms, vcp, light, overlay);
        LargeTankMultiblockBlockEntity tankBlockEntity = (LargeTankMultiblockBlockEntity) be;
        FluidVariant fluid = tankBlockEntity.getFluid();
        if (tankBlockEntity.shapeValid.shapeValid && !fluid.isBlank() && tankBlockEntity.getFullnessFraction() > 0) {

            VertexConsumer vc = vcp.getBuffer(RenderLayer.getTranslucent());

            Sprite sprite = FluidVariantRendering.getSprite(fluid);
            int color = FluidVariantRendering.getColor(fluid);

            float r = ((color >> 16) & 255) / 256f;
            float g = ((color >> 8) & 255) / 256f;
            float b = (color & 255) / 256f;

            int[] cornerPosition = tankBlockEntity.getCornerPosition();

            int maxX = cornerPosition[0];
            int totalMaxY = cornerPosition[1];
            int maxZ = cornerPosition[2];

            int minX = cornerPosition[3];
            int totalMinY = cornerPosition[4];
            int minZ = cornerPosition[5];

            float fullness = (float) tankBlockEntity.getFullnessFraction();
            // Top and bottom positions of the fluid inside the tank
            float topHeight = fullness;
            float bottomHeight = 0;
            // Render gas from top to bottom
            if (FluidVariantRendering.fillsFromTop(fluid)) {
                topHeight = 1;
                bottomHeight = 1 - fullness;
            }

            int minY = (int) Math.floor(bottomHeight * (totalMaxY + 1 - totalMinY) + totalMinY);
            int maxY = (int) Math.floor(topHeight * (totalMaxY + 1 - totalMinY) + totalMinY);

            int[] mins = new int[] { minX, minY, minZ };
            int[] maxs = new int[] { maxX, maxY, maxZ };
            Vec3i[] dirs = new Vec3i[] { Direction.EAST.getVector(), Direction.UP.getVector(), Direction.SOUTH.getVector() };

            Renderer renderer = RendererAccess.INSTANCE.getRenderer();

            for (Direction direction : Direction.values()) {
                QuadEmitter emitter = renderer.meshBuilder().getEmitter();
                ms.push();

                int u_index = direction.getAxis() == Direction.Axis.X ? 2 : 0;
                int v_index = direction.getAxis() == Direction.Axis.Y ? 2 : 1;

                Vec3i offset_u = dirs[u_index];
                Vec3i offset_v = dirs[v_index];

                int dirAxis = direction.getAxis() == Direction.Axis.X ? 0 : direction.getAxis() == Direction.Axis.Z ? 2 : 1;
                int dirWays = direction.getId() % 2;
                int offset_w = dirWays == 0 ? mins[dirAxis] : maxs[dirAxis];

                Vec3i origin = offset_u.multiply(mins[u_index]).add(offset_v.multiply(mins[v_index])).add(dirs[dirAxis].multiply(offset_w));

                float originX = origin.getX();
                float originY = origin.getY();
                float originZ = origin.getZ();

                if (direction == Direction.UP) {
                    originY = (topHeight * (totalMaxY + 1 - totalMinY)) - 1;
                } else if (direction == Direction.DOWN) {
                    originY = (bottomHeight * (totalMaxY + 1 - totalMinY));
                }

                ms.translate(originX, originY, originZ);

                int max_u = maxs[u_index] - mins[u_index];
                int max_v = maxs[v_index] - mins[v_index];

                for (int u = 0; u <= max_u; u++) {
                    ms.push();
                    for (int v = 0; v <= max_v; v++) {

                        float bottom = 0;
                        float top = 1;

                        if (!direction.getAxis().isVertical()) {
                            if (v == 0) {
                                bottom = (bottomHeight * (totalMaxY + 1 - totalMinY)) % 1.0f;
                            }
                            if (v == max_v) {
                                top = (topHeight * (totalMaxY + 1 - totalMinY)) % 1.0f;
                            }
                        }

                        emitter.square(direction, 0, bottom, 1, top, 0);
                        emitter.spriteBake(0, sprite, MutableQuadView.BAKE_LOCK_UV);
                        emitter.spriteColor(0, -1, -1, -1, -1);
                        vc.quad(ms.peek(), emitter.toBakedQuad(0, sprite, false), r, g, b, RenderHelper.FULL_LIGHT, OverlayTexture.DEFAULT_UV);

                        ms.translate(offset_v.getX(), offset_v.getY(), offset_v.getZ());
                    }
                    ms.pop();
                    ms.translate(offset_u.getX(), offset_u.getY(), offset_u.getZ());
                }

                ms.pop();
            }

        }

    }
}
