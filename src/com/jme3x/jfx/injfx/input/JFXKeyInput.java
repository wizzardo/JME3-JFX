package com.jme3x.jfx.injfx.input;

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;

/**
 * Created by ronn on 03.12.16.
 */
public class JFXKeyInput implements KeyInput {

    @Override
    public void initialize() {

    }

    @Override
    public void update() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public void setInputListener(RawInputListener listener) {

    }

    @Override
    public long getInputTimeNanos() {
        return System.nanoTime();
    }
}
