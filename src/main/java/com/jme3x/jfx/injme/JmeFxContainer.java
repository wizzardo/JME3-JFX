package com.jme3x.jfx.injme;

import static com.jme3x.jfx.injme.util.JmeWindowUtils.getHeight;
import static com.jme3x.jfx.injme.util.JmeWindowUtils.getWidth;
import static com.jme3x.jfx.util.JFXPlatform.runInFXThread;
import static com.ss.rlib.util.ObjectUtils.notNull;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.JmeContext;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import com.jme3x.jfx.injme.cursor.CursorDisplayProvider;
import com.jme3x.jfx.injme.cursor.proton.ProtonCursorProvider;
import com.jme3x.jfx.injme.input.JmeFXInputListener;
import com.jme3x.jfx.util.JFXPlatform;
import com.ss.rlib.concurrent.atomic.AtomicInteger;
import com.ss.rlib.concurrent.lock.AsyncReadSyncWriteLock;
import com.ss.rlib.concurrent.lock.LockFactory;
import com.ss.rlib.logging.Logger;
import com.ss.rlib.logging.LoggerLevel;
import com.ss.rlib.logging.LoggerManager;
import com.sun.glass.ui.Pixels;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.stage.EmbeddedWindow;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The container which interacts with jME and includes javaFX scene.
 *
 * @author abies / Artur Biesiadowski / JavaSaBr
 */
@SuppressWarnings("WeakerAccess")
public class JmeFxContainer {

    @NotNull
    private static final Logger LOGGER = LoggerManager.getLogger(JFXPlatform.class);

    private static final int MIN_RESIZE_INTERVAL = 300;

    /**
     * Build the JavaFX container for the application.
     *
     * @param application the application.
     * @param guiNode     the GUI node.
     * @return the javaFX container.
     */
    public static @NotNull JmeFxContainer install(@NotNull final Application application, @NotNull final Node guiNode) {
        return install(application, guiNode, new ProtonCursorProvider(application, application.getAssetManager(),
                application.getInputManager()));
    }

    /**
     * Build the JavaFX container for the application.
     *
     * @param application    the application.
     * @param guiNode        the GUI node.
     * @param cursorProvider the cursor provider.
     * @return the javaFX container.
     */
    public static @NotNull JmeFxContainer install(@NotNull final Application application, @NotNull final Node guiNode,
                                                  @NotNull final CursorDisplayProvider cursorProvider) {

        final JmeFxContainer container = new JmeFxContainer(application.getAssetManager(), application, cursorProvider);
        guiNode.attachChild(container.getJmeNode());

        final InputManager inputManager = application.getInputManager();
        inputManager.addRawInputListener(container.getInputListener());

        return container;
    }

    // TODO benchmark
    private static Void reorder_ARGB82ABGR8(@NotNull final ByteBuffer data) {

        final int limit = data.limit() - 3;

        byte v;

        for (int i = 0; i < limit; i += 4) {
            v = data.get(i + 1);
            data.put(i + 1, data.get(i + 3));
            data.put(i + 3, v);
        }

        return null;
    }

    // TODO benchmark
    private static Void reorder_BGRA82ABGR8(@NotNull final ByteBuffer data) {

        final int limit = data.limit() - 3;

        byte v0, v1, v2, v3;

        for (int i = 0; i < limit; i += 4) {
            v0 = data.get(i);
            v1 = data.get(i + 1);
            v2 = data.get(i + 2);
            v3 = data.get(i + 3);
            data.put(i, v3);
            data.put(i + 1, v0);
            data.put(i + 2, v1);
            data.put(i + 3, v2);
        }

        return null;
    }

    /**
     * The state to attach/detach javaFX UI.
     */
    private final @NotNull AppState fxAppState = new AbstractAppState() {

        @Override
        public void cleanup() {
            Platform.exit();
            super.cleanup();
        }
    };

    @NotNull
    protected volatile CompletableFuture<Format> nativeFormat = new CompletableFuture<>();

    /**
     * The count of frames which need to write to JME.
     */
    @NotNull
    private final AtomicInteger waitCount;

    /**
     * The lock to control transfer frames from javaFX to JME.
     */
    @NotNull
    private final AsyncReadSyncWriteLock imageLock;

    /**
     * The image node to present javaFX scene.
     */
    @NotNull
    private final Picture picture;

    /**
     * The texture to present javaFX scene.
     */
    @NotNull
    private final Texture2D texture;

    /**
     * The jMe context.
     */
    @NotNull
    private final JmeContext jmeContext;

    /**
     * The jME application.
     */
    @NotNull
    protected final Application application;

    /**
     * The current cursor provider.
     */
    @NotNull
    protected final CursorDisplayProvider cursorProvider;

    /**
     * The host interface.
     */
    @NotNull
    protected final HostInterface hostInterface;

    /**
     * The user input listener.
     */
    @NotNull
    protected volatile JmeFXInputListener inputListener;

    /**
     * The current embedded stage interface.
     */
    @Nullable
    protected volatile EmbeddedStageInterface stageInterface;

    /**
     * The current embedded scene interface.
     */
    @Nullable
    protected volatile EmbeddedSceneInterface sceneInterface;

    /**
     * The embedded window.
     */
    @Nullable
    protected volatile EmbeddedWindow embeddedWindow;

    /**
     * The current scene.
     */
    @Nullable
    protected volatile Scene scene;

    /**
     * The root UI node.
     */
    @Nullable
    protected volatile Group rootNode;

    /**
     * The image of jME presentation of javaFX frame.
     */
    @Nullable
    protected volatile Image jmeImage;

    /**
     * The data of javaFX frame on the jME side.
     */
    @Nullable
    protected volatile ByteBuffer jmeData;

    /**
     * The data buffer of javaFX frame on javaFX side.
     */
    @Nullable
    protected volatile ByteBuffer fxData;

    /**
     * The temp data to transfer frames between javaFX and jME.
     */
    @Nullable
    protected volatile ByteBuffer tempData;

    /**
     * The int presentation of the {@link #tempData}.
     */
    @Nullable
    protected volatile IntBuffer tempIntData;

    /**
     * The function to reorder pixels.
     */
    @Nullable
    protected volatile Function<ByteBuffer, Void> reorderData;

    /**
     * The time of last resized window.
     */
    protected volatile long lastResized;

    /**
     * The width of javaFX scene.
     */
    protected volatile int sceneWidth;

    /**
     * The height of javaFX scene.
     */
    protected volatile int sceneHeight;

    /**
     * The X position of this container.
     */
    protected volatile int positionX;

    /**
     * The Y position of this container.
     */
    protected volatile int positionY;

    /**
     * The flag of having focused.
     */
    protected volatile boolean focused;

    /**
     * The flag of supporting full screen.
     */
    protected volatile boolean fullScreenSupport;

    /**
     * The flag of visibility cursor.
     */
    protected volatile boolean visibleCursor;

    /**
     * The flag of enabling javaFX.
     */
    protected volatile boolean enabled;

    protected JmeFxContainer(@NotNull final AssetManager assetManager, @NotNull final Application application,
                             @NotNull final CursorDisplayProvider cursorProvider) {
        this.initFx();
        this.positionY = -1;
        this.positionX = -1;
        this.jmeContext = application.getContext();
        this.waitCount = new AtomicInteger();
        this.imageLock = LockFactory.newAtomicARSWLock();
        this.cursorProvider = cursorProvider;
        this.application = application;
        this.visibleCursor = true;
        this.inputListener = new JmeFXInputListener(this);

        final AppStateManager stateManager = application.getStateManager();
        stateManager.attach(fxAppState);

        this.hostInterface = new JmeFxHostInterface(this);
        this.picture = new JavaFxPicture(this);
        this.picture.move(0, 0, -1);
        this.picture.setPosition(0, 0);
        this.texture = new Texture2D(new Image());
        this.picture.setTexture(assetManager, texture, true);

        fitSceneToWindowSize();
    }

    /**
     * Requests the preferred size for UI.
     *
     * @param width  the preferred width.
     * @param height the preferred height.
     */
    public void requestPreferredSize(final int width, final int height) {
    }

    /**
     * Requests focus.
     *
     * @return true if it was successful.
     */
    public boolean requestFocus() {
        return true;
    }

    /**
     * Sets the last timestamp of context resizing.
     *
     * @param time the last timestamp of context resizing.
     */
    private void setLastResized(final long time) {
        this.lastResized = time;
    }

    /**
     * Gets the last timestamp of context resizing.
     *
     * @return the last timestamp of context resizing.
     */
    private long getLastResized() {
        return lastResized;
    }

    /**
     * Gets the jME application.
     *
     * @return the jME application.
     */
    public @NotNull Application getApplication() {
        return application;
    }

    /**
     * Gets the jMe context.
     *
     * @return the jMe context.
     */
    public @NotNull JmeContext getJmeContext() {
        return jmeContext;
    }

    /**
     * @return the current cursor provider.
     */
    public @NotNull CursorDisplayProvider getCursorProvider() {
        return cursorProvider;
    }

    /**
     * Gets the lock to control transferring frames.
     *
     * @return the lock.
     */
    private @NotNull AsyncReadSyncWriteLock getImageLock() {
        return imageLock;
    }

    /**
     * Gets the user input listener.
     *
     * @return the user input listener.
     */
    public @NotNull JmeFXInputListener getInputListener() {
        return inputListener;
    }

    /**
     * Gets the image of jME presentation of javaFX frame.
     *
     * @return the image of jME presentation of javaFX frame.
     */
    private @Nullable Image getJmeImage() {
        return jmeImage;
    }

    /**
     * Gets the image node to present javaFX scene.
     *
     * @return the image node to present javaFX scene.
     */
    private @NotNull Picture getJmeNode() {
        return picture;
    }

    /**
     * Gets the X position.
     *
     * @return the X position.
     */
    public int getPositionX() {
        return positionX;
    }

    /**
     * Sets the X position.
     *
     * @param positionX the X position.
     */
    private void setPositionX(final int positionX) {
        this.positionX = positionX;
    }

    /**
     * Gets the Y position.
     *
     * @return the Y position.
     */
    public int getPositionY() {
        return positionY;
    }

    /**
     * Sets the Y position.
     *
     * @param positionY the Y position.
     */
    private void setPositionY(final int positionY) {
        this.positionY = positionY;
    }

    /**
     * Gets the image node to present javaFX scene.
     *
     * @return the image node to present javaFX scene.
     */
    private @NotNull Picture getPicture() {
        return picture;
    }

    /**
     * Gets the scene height.
     *
     * @return the scene height.
     */
    public int getSceneHeight() {
        return sceneHeight;
    }

    /**
     * Sets the scene height.
     *
     * @param sceneHeight the scene height.
     */
    private void setSceneHeight(final int sceneHeight) {
        this.sceneHeight = sceneHeight;
    }

    /**
     * Gets the scene width.
     *
     * @return the scene width.
     */
    public int getSceneWidth() {
        return sceneWidth;
    }

    /**
     * Sets the scene width.
     *
     * @param sceneWidth the scene width.
     */
    private void setSceneWidth(final int sceneWidth) {
        this.sceneWidth = sceneWidth;
    }

    /**
     * Gets the target pixel factor.
     *
     * @return the target pixel factor.
     */
    public float getPixelScaleFactor() {
        return 1.0F;
    }

    /**
     * Gets the function to reorder pixels.
     *
     * @return the function to reorder pixels.
     */
    private @Nullable Function<ByteBuffer, Void> getReorderData() {
        return reorderData;
    }

    /**
     * Gets the root UI node.
     *
     * @return the root UI node.
     */
    public @Nullable Group getRootNode() {
        return rootNode;
    }

    /**
     * Gets the current scene.
     *
     * @return the current scene.
     */
    public @Nullable Scene getScene() {
        return scene;
    }

    /**
     * Gets the current scene interface.
     *
     * @return the current scene interface.
     */
    public @Nullable EmbeddedSceneInterface getSceneInterface() {
        return sceneInterface;
    }

    /**
     * Sets the current scene interface.
     *
     * @param sceneInterface the current scene interface.
     */
    public void setSceneInterface(@Nullable final EmbeddedSceneInterface sceneInterface) {
        this.sceneInterface = sceneInterface;
    }

    /**
     * Gets the embedded window.
     *
     * @return the embedded window.
     */
    public @Nullable EmbeddedWindow getEmbeddedWindow() {
        return embeddedWindow;
    }

    /**
     * Sets the embedded window.
     *
     * @param embeddedWindow the embedded window.
     */
    public void setEmbeddedWindow(@Nullable final EmbeddedWindow embeddedWindow) {
        this.embeddedWindow = embeddedWindow;
    }

    /**
     * Gets the current stage interface.
     *
     * @return the current stage interface.
     */
    public @Nullable EmbeddedStageInterface getStageInterface() {
        return stageInterface;
    }

    /**
     * Sets the current stage interface.
     *
     * @param stageInterface the current stage interface.
     */
    public void setStageInterface(@Nullable final EmbeddedStageInterface stageInterface) {
        this.stageInterface = stageInterface;
    }

    /**
     * Gets the data buffer of javaFX frame on javaFX side.
     *
     * @return the data buffer.
     */
    private @Nullable ByteBuffer getFxData() {
        return fxData;
    }

    /**
     * Gets the data of javaFX frame on the jME side.
     *
     * @return the data of javaFX frame on the jME side.
     */
    private @Nullable ByteBuffer getJmeData() {
        return jmeData;
    }

    /**
     * Gets the temp data to transfer frames between javaFX and jME.
     *
     * @return the temp data to transfer frames between javaFX and jME.
     */
    private @Nullable ByteBuffer getTempData() {
        return tempData;
    }

    /**
     * Gets the int presentation of the tempData.
     *
     * @return the int presentation of the tempData.
     */
    private @Nullable IntBuffer getTempIntData() {
        return tempIntData;
    }

    /**
     * Gets the texture to present javaFX scene.
     *
     * @return the texture to present javaFX scene.
     */
    private @NotNull Texture2D getTexture() {
        return texture;
    }

    /**
     * Gets the the count of waited frames.
     *
     * @return the count of waited frames.
     */
    private @NotNull AtomicInteger getWaitCount() {
        return waitCount;
    }

    /**
     * Get focused.
     */
    public void grabFocus() {

        final EmbeddedStageInterface stageInterface = getStageInterface();
        if (isFocused() || stageInterface == null) return;

        stageInterface.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);
        setFocused(true);

        LOGGER.debug(this, "got focused.");
    }

    /**
     * Fit scene to window size.
     */
    public void fitSceneToWindowSize() {

        final long time = System.currentTimeMillis();
        if (time - getLastResized() < MIN_RESIZE_INTERVAL) return;

        final JmeContext jmeContext = getJmeContext();

        final int winWidth = getWidth(jmeContext);
        final int winHeight = getHeight(jmeContext);

        final AsyncReadSyncWriteLock lock = getImageLock();
        lock.syncLock();
        try {

            final int textureWidth = Math.max(winWidth, 64);
            final int textureHeight = Math.max(winHeight, 64);

            final Picture picture = getPicture();

            if (LOGGER.isEnabled(LoggerLevel.DEBUG)) {
                LOGGER.debug(this, "Fit the scene to window size from [" + getSceneWidth() + "x" + getSceneHeight() + "] to " +
                                "[" + textureWidth + "x" + textureHeight + "]");
            }

            picture.setWidth(textureWidth);
            picture.setHeight(textureHeight);

            final ByteBuffer fxData = getFxData();
            if (fxData != null) {
                BufferUtils.destroyDirectBuffer(fxData);
            }

            final ByteBuffer tempData = getTempData();
            if (tempData != null) {
                BufferUtils.destroyDirectBuffer(tempData);
            }

            final ByteBuffer jmeData = getJmeData();
            if (jmeData != null) {
                BufferUtils.destroyDirectBuffer(jmeData);
            }

            final Image jmeImage = getJmeImage();
            if (jmeImage != null) {
                jmeImage.dispose();
            }

            this.fxData = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            this.tempData = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            this.tempIntData = getTempData().asIntBuffer();
            this.jmeData = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            this.jmeImage = new Image(nativeFormat.get(), textureWidth, textureHeight, getJmeData(), ColorSpace.sRGB);

            final Texture2D texture = getTexture();
            texture.setImage(getJmeImage());

            setSceneHeight(textureHeight);
            setSceneWidth(textureWidth);

            final EmbeddedStageInterface stageInterface = getStageInterface();
            final EmbeddedSceneInterface sceneInterface = getSceneInterface();

            if (stageInterface != null && sceneInterface != null) {
                JFXPlatform.runInFXThread(() -> {
                    stageInterface.setSize(textureWidth, textureHeight);
                    sceneInterface.setSize(textureWidth, textureHeight);
                    hostInterface.repaint();
                });
            }

        } catch (final Exception e) {
            LOGGER.warning(e);
        } finally {
            lock.syncUnlock();
        }

        setLastResized(time);
    }

    /**
     * Moves the container to the new position.
     *
     * @param positionX the new X position.
     * @param positionY the new Y position.
     */
    public void move(final int positionX, final int positionY) {
        setPositionX(positionX);
        setPositionY(positionY);

        final EmbeddedStageInterface stageInterface = getStageInterface();
        if (stageInterface == null) {
            return;
        }

        runInFXThread(() -> stageInterface.setLocation(getPositionX(), getPositionY()));
    }

    private void initFx() {
        PlatformImpl.startup(() -> {
            switch (Pixels.getNativeFormat()) {
                case Pixels.Format.BYTE_ARGB:
                    try {
                        nativeFormat.complete(Format.ARGB8);
                        reorderData = null;
                    } catch (final Exception exc1) {
                        nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainer::reorder_ARGB82ABGR8;
                    }
                    break;
                case Pixels.Format.BYTE_BGRA_PRE:
                    try {
                        nativeFormat.complete(Format.BGRA8);
                        reorderData = null;
                    } catch (final Exception exc2) {
                        nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainer::reorder_BGRA82ABGR8;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Not supported javaFX pixel format " + Pixels.getNativeFormat());
            }
        });
    }

    public boolean isCovered(final int x, final int y) {

        final Image jmeImage = getJmeImage();
        final int sceneWidth = getSceneWidth();

        if (jmeImage == null || x < 0 || x >= sceneWidth) {
            return false;
        } else if (y < 0 || y >= getSceneHeight()) {
            return false;
        }

        final ByteBuffer data = jmeImage.getData(0);
        data.limit(data.capacity());

        final int alpha = data.get(3 + 4 * (y * sceneWidth + x));

        data.limit(0);

        if (LOGGER.isEnabled(LoggerLevel.DEBUG)) {
            LOGGER.debug(this, "is covered " + x + ", " + y + " = " + (alpha != 0));
        }

        return alpha != 0;
    }

    /**
     * @return true if the windows has focused.
     */
    public boolean isFocused() {
        return focused;
    }

    /**
     * @param focused true if the windows has focused.
     */
    private void setFocused(final boolean focused) {
        this.focused = focused;
    }

    /**
     * @return true if this container is supported fullscreen.
     */
    public boolean isFullScreenSupport() {
        return fullScreenSupport;
    }

    /**
     * @return true if need to write javaFx frame.
     */
    public boolean isNeedWriteToJME() {
        return waitCount.get() > 0;
    }

    /**
     * @return true if the cursor is visible.
     */
    public boolean isVisibleCursor() {
        return visibleCursor;
    }

    /**
     * @param visibleCursor true if the cursor is visible.
     */
    public void setVisibleCursor(final boolean visibleCursor) {
        this.visibleCursor = visibleCursor;
    }

    /**
     * Lose focused.
     */
    public void loseFocus() {

        final EmbeddedStageInterface stagePeer = getStageInterface();
        if (!isFocused() || stagePeer == null) return;

        stagePeer.setFocused(false, AbstractEvents.FOCUSEVENT_DEACTIVATED);

        setFocused(false);

        LOGGER.debug(this, "lost focused.");
    }

    /**
     * Draw new frame of JavaFX to byte buffer.
     */
    void requestRedraw() {

        long time = 0;

        if (LOGGER.isEnabled(LoggerLevel.DEBUG)) {
            time = System.currentTimeMillis();
            LOGGER.debug(this, "Started paint FX scene...");
        }

        final EmbeddedSceneInterface sceneInterface = getSceneInterface();
        if (sceneInterface == null) return;

        final ByteBuffer tempData = notNull(getTempData());
        tempData.clear();

        final int sceneWidth = getSceneWidth();
        final int sceneHeight = getSceneHeight();

        if (!sceneInterface.getPixels(notNull(getTempIntData()), sceneWidth, sceneHeight)) {
            return;
        }

        tempData.flip();
        tempData.limit(sceneWidth * sceneHeight * 4);

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {

            final ByteBuffer fxData = notNull(getFxData());
            fxData.clear();
            fxData.put(tempData);
            fxData.flip();

            final Function<ByteBuffer, Void> reorderData = getReorderData();

            if (reorderData != null) {
                reorderData.apply(fxData);
                fxData.position(0);
            }

        } catch (final Exception exc) {
            LOGGER.warning(exc.getMessage(), exc);
        } finally {
            imageLock.syncUnlock();
        }

        final AtomicInteger waitCount = getWaitCount();
        waitCount.incrementAndGet();

        if (LOGGER.isEnabled(LoggerLevel.DEBUG)) {
            LOGGER.debug(this, "finished paint FX scene(" + (System.currentTimeMillis() - time) + "ms.).");
        }
    }

    /**
     * Set a new scene to this container.
     *
     * @param newScene the new scene or null.
     * @param rootNode the new root of the scene.
     */
    public void setScene(@Nullable final Scene newScene, @NotNull final Group rootNode) {
        this.rootNode = rootNode;
        runInFXThread(() -> setSceneImpl(newScene));
    }

    /**
     * Set a new scene to javaFX container.
     *
     * @param newScene the new scene.
     */
    private void setSceneImpl(@Nullable final Scene newScene) {

        if (embeddedWindow != null && newScene == null) {
            embeddedWindow.hide();
            embeddedWindow = null;
        }

        final Application application = notNull(getApplication());
        application.enqueue(() -> {
            picture.setCullHint(newScene == null ? CullHint.Always : CullHint.Never);
            return null;
        });

        this.scene = newScene;

        if (embeddedWindow == null && newScene != null) {
            embeddedWindow = new EmbeddedWindow(hostInterface);
        }

        if (embeddedWindow == null) {
            return;
        }

        embeddedWindow.setScene(newScene);

        if (!embeddedWindow.isShowing()) {
            embeddedWindow.show();
        }
    }

    /**
     * Request showing the cursor frame.
     *
     * @param cursorFrame the cursor frame.
     */
    public void requestShowingCursor(@NotNull final CursorFrame cursorFrame) {
        cursorProvider.show(cursorFrame);
    }

    /**
     * Write javaFX frame to jME texture.
     */
    public Void writeToJME() {

        final AtomicInteger waitCount = getWaitCount();
        final int currentCount = waitCount.get();

        long time = 0;

        if (LOGGER.isEnabled(LoggerLevel.DEBUG)) {
            time = System.currentTimeMillis();
            LOGGER.debug(this, "Started writing FX data to JME...");
        }

        final ByteBuffer jmeData = notNull(getJmeData());
        jmeData.clear();

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {
            jmeData.put(notNull(getFxData()));
        } finally {
            imageLock.syncUnlock();
        }

        jmeData.flip();

        final Image jmeImage = notNull(getJmeImage());
        jmeImage.setUpdateNeeded();

        waitCount.subAndGet(currentCount);

        if (LOGGER.isEnabled(LoggerLevel.DEBUG)) {
            LOGGER.debug(this, "Finished writing FX data to JME(" + (System.currentTimeMillis() - time) + "ms.).");
        }

        return null;
    }

    /**
     * @param enabled the flag of enabling javaFX.
     */
    public void requestEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return true if the javaFx is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}