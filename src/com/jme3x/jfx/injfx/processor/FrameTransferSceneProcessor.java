package com.jme3x.jfx.injfx.processor;

import com.jme3.post.SceneProcessor;

/**
 * The interface for implementing frame transfer processor.
 *
 * @author JavaSaBr
 */
public interface FrameTransferSceneProcessor extends SceneProcessor {

    /**
     * @return if this processor is main.
     */
    boolean isMain();

    /**
     * @return true if this processor is enabled.
     */
    boolean isEnabled();

    /**
     * @param enabled true if this processor is enabled.
     */
    void setEnabled(final boolean enabled);
}
