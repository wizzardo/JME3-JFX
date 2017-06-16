package com.jme3x.jfx.injfx.processor;

import com.jme3.post.SceneProcessor;

/**
 * The interface for implementing frame transfer processor.
 *
 * @author JavaSaBr
 */
public interface FrameTransferSceneProcessor extends SceneProcessor {

    /**
     * Is main boolean.
     *
     * @return if this processor is main.
     */
    boolean isMain();

    /**
     * Is enabled boolean.
     *
     * @return true if this processor is enabled.
     */
    boolean isEnabled();

    /**
     * Sets enabled.
     *
     * @param enabled true if this processor is enabled.
     */
    void setEnabled(final boolean enabled);

    /**
     * Reshape a screen.
     */
    void reshape();
}
