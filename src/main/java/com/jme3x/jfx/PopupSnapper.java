/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3x.jfx;

import com.sun.javafx.perf.PerformanceTracker;

import java.nio.IntBuffer;
import java.util.concurrent.Semaphore;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import rlib.util.array.Array;

/**
 * Redirect popups to the Bridge
 */
public class PopupSnapper {

    private final Window window;
    private final Scene scene;

    WritableImage img;

    double ignoreRepaintHeight;

    JmeFxContainer jmeFXcontainerReference;

    private final Semaphore repaintLock = new Semaphore(1);

    public PopupSnapper(final JmeFxContainer containReference, final Window window, final Scene scene) {
        this.window = window;
        this.scene = scene;
        this.jmeFXcontainerReference = containReference;
    }

    public void paint(final IntBuffer buf, final int pWidth, final int pHeight) {
        try {

            final WritableImage img = this.img;
            if (img == null) {
                // System.out.println("Skipping popup merge due to no image");
                return;
            }
            final boolean lock = repaintLock.tryAcquire();
            if (lock) {
                try {
                    final PixelReader pr = img.getPixelReader();

                    final int w = (int) img.getWidth();
                    final int h = (int) img.getHeight();

                    final byte[] pixels = new byte[w * h * 4];
                    pr.getPixels(0, 0, w, h, PixelFormat.getByteBgraPreInstance(), pixels, 0, w * 4);

                    final int xoff = (int) this.window.getX() - this.jmeFXcontainerReference.getWindowX();
                    final int yoff = (int) this.window.getY() - this.jmeFXcontainerReference.getWindowY();

                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < h; y++) {
                            final int offset = x + xoff + (y + yoff) * pWidth;
                            final int old = buf.get(offset);
                            final int boff = 4 * (x + y * w);
                            final int toMerge = pixels[boff] & 0xff | (pixels[boff + 1] & 0xff) << 8 | (pixels[boff + 2] & 0xff) << 16 | (pixels[boff + 3] & 0xff) << 24;

                            final int merge = PixelUtils.mergeBgraPre(old, toMerge);
                            buf.put(offset, merge);
                        }
                    }
                    // System.out.println("Done popup merge");
                } finally {
                    repaintLock.release();
                }
            } else {
                // System.out.println("Skipping popup merge due to contention");
            }

        } catch (final Exception exc) {
            exc.printStackTrace();
        }

    }

    public void repaint() {
        try {
            if (!Color.TRANSPARENT.equals(this.scene.getFill())) {
                this.scene.setFill(Color.TRANSPARENT);
            }
            if (this.img != null) {
                if (this.img.getWidth() != this.scene.getWidth() || this.img.getHeight() != this.scene.getHeight()) {
                    // System.out.println("Invalidating image due to size change");
                    this.img = null;
                }
            }
            final boolean lock = repaintLock.tryAcquire();
            if (lock) {
                try {
                    // System.out.println("Popup render");
                    this.img = this.scene.snapshot(this.img);
                } finally {
                    repaintLock.release();
                }
                this.jmeFXcontainerReference.paintComponent();
            } else {
                // System.out.println("Schedulling extra repaint due to contention");
                Platform.runLater(() -> repaint());
            }

        } catch (final Exception exc) {
            exc.printStackTrace();
        }
    }

    public void start() {

        PerformanceTracker.getSceneTracker(scene).setOnRenderedFrameTask(() -> {
            if (ignoreRepaintHeight == scene.getHeight()) {
                ignoreRepaintHeight = -1;
                return;
            }
            Platform.runLater(() -> {
                ignoreRepaintHeight = scene.getHeight();
                repaint();
            });
        });

        final Array<PopupSnapper> activeSnappers = this.jmeFXcontainerReference.activeSnappers;
        activeSnappers.writeLock();
        try {
            activeSnappers.add(this);
        } finally {
            activeSnappers.writeUnlock();
        }
    }

    public void stop() {

        final Array<PopupSnapper> activeSnappers = this.jmeFXcontainerReference.activeSnappers;
        activeSnappers.writeLock();
        try {
            activeSnappers.fastRemove(this);
        } finally {
            activeSnappers.writeUnlock();
        }
    }
}
