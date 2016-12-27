package com.jme3x.jfx.injfx.transfer;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.sun.istack.internal.NotNull;

/**
 * The class for transferring content from a jME frame buffer to somewhere.
 *
 * @author JavaSaBr
 */
public interface FrameTransfer {

    /**
     * Init this transfer for the render.
     *
     * @param renderer the render.
     */
    default void initFor(@NotNull final Renderer renderer, final boolean main) {
    }

    /**
     * @return the width.
     */
    int getWidth();

    /**
     * @return the height.
     */
    int getHeight();

    /**
     * Copy the content from render to the frameByteBuffer and write this content to image view.
     */
    void copyFrameBufferToImage(@NotNull final RenderManager renderManager);

    /**
     * Dispose this transfer.
     */
    void dispose();
}
