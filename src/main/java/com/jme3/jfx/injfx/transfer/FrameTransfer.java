package com.jme3.jfx.injfx.transfer;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;

import org.jetbrains.annotations.NotNull;

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
     * @param main     the main
     */
    default void initFor(@NotNull final Renderer renderer, final boolean main) {
    }

    /**
     * Gets width.
     *
     * @return the width.
     */
    int getWidth();

    /**
     * Gets height.
     *
     * @return the height.
     */
    int getHeight();

    /**
     * Copy the content from render to the frameByteBuffer and write this content to image view.
     *
     * @param renderManager the render manager
     */
    void copyFrameBufferToImage(@NotNull final RenderManager renderManager);

    /**
     * Dispose this transfer.
     */
    void dispose();
}
