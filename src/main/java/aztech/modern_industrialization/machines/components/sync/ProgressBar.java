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
package aztech.modern_industrialization.machines.components.sync;

import aztech.modern_industrialization.MIIdentifier;
import aztech.modern_industrialization.machines.SyncedComponent;
import aztech.modern_industrialization.machines.SyncedComponents;
import aztech.modern_industrialization.machines.gui.ClientComponentRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class ProgressBar {
    public static class Server implements SyncedComponent.Server<Float> {
        private final Parameters params;
        private final Supplier<Float> progressSupplier;

        public Server(Parameters params, Supplier<Float> progressSupplier) {
            this.params = params;
            this.progressSupplier = progressSupplier;
        }

        @Override
        public Float copyData() {
            return progressSupplier.get();
        }

        @Override
        public boolean needsSync(Float cachedData) {
            return !cachedData.equals(progressSupplier.get());
        }

        @Override
        public void writeInitialData(PacketByteBuf buf) {
            buf.writeInt(params.renderX);
            buf.writeInt(params.renderY);
            buf.writeString(params.progressBarType);
            buf.writeBoolean(params.isVertical);
            writeCurrentData(buf);
        }

        @Override
        public void writeCurrentData(PacketByteBuf buf) {
            buf.writeFloat(progressSupplier.get());
        }

        @Override
        public Identifier getId() {
            return SyncedComponents.PROGRESS_BAR;
        }
    }

    public static class Client implements SyncedComponent.Client {
        public final Parameters params;
        public float progress;

        public Client(PacketByteBuf buf) {
            this.params = new Parameters(buf.readInt(), buf.readInt(), buf.readString(), buf.readBoolean());
            read(buf);
        }

        @Override
        public void read(PacketByteBuf buf) {
            this.progress = buf.readFloat();
        }

        @Override
        public ClientComponentRenderer createRenderer() {
            return new Renderer();
        }

        public class Renderer implements ClientComponentRenderer {
            @Override
            public void renderBackground(DrawableHelper helper, MatrixStack matrices, int x, int y) {
                RenderHelper.renderProgress(helper, matrices, x, y, params, progress);
            }
        }
    }

    public static class RenderHelper {
        public static void renderProgress(DrawableHelper helper, MatrixStack matrices, int x, int y, Parameters params, float progress) {
            renderProgress(helper.getZOffset(), matrices, x, y, params, progress);
        }

        public static void renderProgress(int zoffset, MatrixStack matrices, int x, int y, Parameters params, float progress) {
            RenderSystem.setShaderTexture(0, params.getTextureId());
            // background
            DrawableHelper.drawTexture(matrices, x + params.renderX, y + params.renderY, zoffset, 0, 0, 20, 20, 20, 40);
            // foreground
            int foregroundPixels = (int) (progress * 20);
            if (foregroundPixels > 0) {
                if (!params.isVertical) {
                    DrawableHelper.drawTexture(matrices, x + params.renderX, y + params.renderY, zoffset, 0, 20, foregroundPixels, 20, 20, 40);
                } else {
                    DrawableHelper.drawTexture(matrices, x + params.renderX, y + params.renderY + 20 - foregroundPixels, zoffset, 0,
                            40 - foregroundPixels, 20, foregroundPixels, 20, 40);
                }
            }
        }
    }

    public static class Parameters {
        public final int renderX, renderY;
        /**
         * The real path will be
         * {@code modern_industrialization:textures/gui/progress_bar/<progressBarType>.png}.
         * Must have a size of 20 x 40.
         */
        public final String progressBarType;
        public final boolean isVertical;

        public Parameters(int renderX, int renderY, String progressBarType) {
            this(renderX, renderY, progressBarType, false);
        }

        public Parameters(int renderX, int renderY, String progressBarType, boolean isVertical) {
            this.renderX = renderX;
            this.renderY = renderY;
            this.progressBarType = progressBarType;
            this.isVertical = isVertical;
        }

        public Identifier getTextureId() {
            return new MIIdentifier("textures/gui/progress_bar/" + progressBarType + ".png");
        }
    }
}
