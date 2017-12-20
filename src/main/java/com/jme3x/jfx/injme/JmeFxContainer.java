package com.jme3x.jfx.injme;

import static com.jme3x.jfx.util.JFXPlatform.runInFXThread;
import static java.util.Objects.requireNonNull;
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
import com.jme3x.jfx.injme.input.JmeFXInputListener;
import com.jme3x.jfx.injme.util.JmeContextUtils;
import com.ss.rlib.concurrent.atomic.AtomicInteger;
import com.ss.rlib.concurrent.lock.AsyncReadSyncWriteLock;
import com.ss.rlib.concurrent.lock.LockFactory;
import com.ss.rlib.logging.Logger;
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
 * @author abies / Artur Biesiadowski / JavaSaBr
 */
@SuppressWarnings("WeakerAccess")
public class JmeFxContainer {

    @NotNull
    private static final Logger LOGGER = LoggerManager.getLogger(JmeFxContainer.class);

    private static final int MIN_RESIZE_INTERVAL = 300;

    /**
     * The flag to show debug information.
     */
    private static boolean debugEnabled;

    /**
     * @param debugEnabled true if need to activate debug.
     */
    public static void setDebugEnabled(final boolean debugEnabled) {
        JmeFxContainer.debugEnabled = debugEnabled;
    }

    /**
     * @return true id debug is enabled.
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Build the JavaFX container for the application.
     *
     * @param app                   the application.
     * @param guiNode               the GUI node.
     * @param cursorDisplayProvider the cursor provider.
     * @return the javaFX container.
     */

    public static @NotNull JmeFxContainer install(@NotNull final Application app, @NotNull final Node guiNode,
                                                  @NotNull final CursorDisplayProvider cursorDisplayProvider) {

        final JmeFxContainer container = new JmeFxContainer(app.getAssetManager(), app, cursorDisplayProvider);
        final JmeFXInputListener inputListener = new JmeFXInputListener(container);

        guiNode.attachChild(container.getJmeNode());
        container.setInputListener(inputListener);

        final InputManager inputManager = app.getInputManager();
        inputManager.addRawInputListener(inputListener);

        return container;
    }

    // TODO benchmark
    private static Void reorder_ARGB82ABGR8(final ByteBuffer data) {

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
    private static Void reorder_BGRA82ABGR8(final ByteBuffer data) {

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
    protected final AtomicInteger waitCount;

    /**
     * The lock to control transfer frames from javaFX to JME.
     */
    @NotNull
    protected final AsyncReadSyncWriteLock imageLock;

    /**
     * The image to show javaFX UI.
     */
    @NotNull
    protected final Picture picture;

    /**
     * The texture to show javaFX UI.
     */
    @NotNull
    protected final Texture2D texture;

    /**
     * The JME render context.
     */
    @NotNull
    private final JmeContext jmeContext;

    /**
     * The current embedded stage interface.
     */
    @Nullable
    protected volatile EmbeddedStageInterface stagePeer;

    /**
     * The current embedded scene interface.
     */
    @Nullable
    protected volatile EmbeddedSceneInterface scenePeer;

    /**
     * The embedded window.
     */
    @Nullable
    protected volatile EmbeddedWindow embeddedWindow;

    /**
     * The host interface.
     */
    @Nullable
    protected volatile HostInterface hostContainer;

    /**
     * The user input listener.
     */
    @Nullable
    protected volatile JmeFXInputListener inputListener;

    /**
     * The current scene.
     */
    @Nullable
    protected volatile Scene scene;

    /**
     * The jME application.
     */
    @Nullable
    protected volatile Application application;

    /**
     * The root UI node.
     */
    @Nullable
    protected volatile Group rootNode;

    /**
     * The image to contains javaFX UI.
     */
    @Nullable
    protected volatile Image jmeImage;

    /**
     * The data of javaFX frame on the jME side.
     */
    @Nullable
    protected volatile ByteBuffer jmeData;

    /**
     * The data of javaFX frame on the javaFX side.
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
     * The current cursor provider.
     */
    @Nullable
    protected volatile CursorDisplayProvider cursorDisplayProvider;

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
     * The picture width.
     */
    protected volatile int sceneWidth;

    /**
     * The picture height.
     */
    protected volatile int sceneHeight;

    /**
     * The old X position.
     */
    protected volatile int positionX;

    /**
     * The old Y position.
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
                             @NotNull final CursorDisplayProvider cursorDisplayProvider) {
        this.initFx();

        this.positionY = -1;
        this.positionX = -1;
        this.jmeContext = application.getContext();
        this.waitCount = new AtomicInteger();
        this.imageLock = LockFactory.newAtomicARSWLock();
        this.cursorDisplayProvider = cursorDisplayProvider;
        this.application = application;
        this.visibleCursor = true;

        final AppStateManager stateManager = application.getStateManager();
        stateManager.attach(fxAppState);

        this.hostContainer = new JmeFxHostInterface(this);
        this.picture = new JavaFxPicture(this);
        this.picture.move(0, 0, -1);
        this.picture.setPosition(0, 0);
        this.texture = new Texture2D(new Image());
        this.picture.setTexture(assetManager, texture, true);

        handleResize();
    }

    public void requestPrefferedSize(final int width, final int height) {


    }

    public boolean requestFocus() {
        return true;
    }

    /**
     * @param lastResized the time of last resized window.
     */
    private void setLastResized(final long lastResized) {
        this.lastResized = lastResized;
    }

    /**
     * @return the time of last resized window.
     */
    private long getLastResized() {
        return lastResized;
    }

    /**
     * @return the jME application.
     */
    @Nullable
    public Application getApplication() {
        return application;
    }

    /**
     * @return The jme render context.
     */
    @NotNull
    JmeContext getJmeContext() {
        return jmeContext;
    }

    /**
     * @return the current cursor provider.
     */
    @Nullable
    CursorDisplayProvider getCursorDisplayProvider() {
        return cursorDisplayProvider;
    }

    /**
     * @return the data of javaFX frame on the javaFX side.
     */
    @Nullable
    private ByteBuffer getFxData() {
        return fxData;
    }

    /**
     * @return the lock to control transfer frames from javaFX to JME.
     */
    @NotNull
    private AsyncReadSyncWriteLock getImageLock() {
        return imageLock;
    }

    /**
     * @return the user input listener.
     */
    @Nullable
    JmeFXInputListener getInputListener() {
        return inputListener;
    }

    /**
     * @param inputListener the user input listener.
     */
    private void setInputListener(@Nullable final JmeFXInputListener inputListener) {
        this.inputListener = inputListener;
    }

    /**
     * @return the data of javaFX frame on the jME side.
     */
    @Nullable
    private ByteBuffer getJmeData() {
        return jmeData;
    }

    /**
     * @return the image to contains javaFX UI.
     */
    @Nullable
    private Image getJmeImage() {
        return jmeImage;
    }

    /**
     * @return the image to show javaFX UI.
     */
    @NotNull
    private Picture getJmeNode() {
        return picture;
    }

    /**
     * @return the old X position.
     */
    public int getPositionX() {
        return positionX;
    }

    /**
     * @param positionX the old X position.
     */
    void setPositionX(final int positionX) {
        this.positionX = positionX;
    }

    /**
     * @return the old Y position.
     */
    public int getPositionY() {
        return positionY;
    }

    /**
     * @param positionY the old Y position.
     */
    void setPositionY(final int positionY) {
        this.positionY = positionY;
    }

    /**
     * @return the image to show javaFX UI.
     */
    @NotNull
    private Picture getPicture() {
        return picture;
    }

    /**
     * @return the picture height.
     */
    int getSceneHeight() {
        return sceneHeight;
    }

    /**
     * @param sceneHeight the picture height.
     */
    private void setSceneHeight(final int sceneHeight) {
        this.sceneHeight = sceneHeight;
    }

    /**
     * @return the picture width.
     */
    int getSceneWidth() {
        return sceneWidth;
    }

    public float getPixelScaleFactor() {
        return 1.0F;
    }

    /**
     * @param sceneWidth the picture width.
     */
    private void setSceneWidth(final int sceneWidth) {
        this.sceneWidth = sceneWidth;
    }

    /**
     * @return the function to reorder pixels.
     */
    @Nullable
    private Function<ByteBuffer, Void> getReorderData() {
        return reorderData;
    }

    /**
     * @return the root UI node.
     */
    @Nullable
    Group getRootNode() {
        return rootNode;
    }

    /**
     * @return the current scene.
     */
    @Nullable
    public Scene getScene() {
        return scene;
    }

    /**
     * @return the current embedded scene interface.
     */
    @Nullable
    public EmbeddedSceneInterface getSceneInterface() {
        return scenePeer;
    }

    /**
     * @param scenePeer the current embedded scene interface.
     */
    void setSceneInterface(@Nullable final EmbeddedSceneInterface scenePeer) {
        this.scenePeer = scenePeer;
    }

    /**
     * @return the embedded window.
     */
    @Nullable
    public EmbeddedWindow getEmbeddedWindow() {
        return embeddedWindow;
    }

    /**
     * @param embeddedWindow the embedded window.
     */
    public void setEmbeddedWindow(@Nullable final EmbeddedWindow embeddedWindow) {
        this.embeddedWindow = embeddedWindow;
    }

    /**
     * @return the current embedded stage interface.
     */
    @Nullable
    EmbeddedStageInterface getStageInterface() {
        return stagePeer;
    }

    /**
     * @param stagePeer the current embedded stage interface.
     */
    void setStageInterface(@Nullable final EmbeddedStageInterface stagePeer) {
        this.stagePeer = stagePeer;
    }

    /**
     * @return the temp data to transfer frames between javaFX and jME.
     */
    @Nullable
    private ByteBuffer getTempData() {
        return tempData;
    }

    /**
     * @return the int presentation of the tempData.
     */
    @Nullable
    private IntBuffer getTempIntData() {
        return tempIntData;
    }

    /**
     * @return the texture to show javaFX UI.
     */
    @NotNull
    private Texture2D getTexture() {
        return texture;
    }

    /**
     * @return the count of frames which need to write to JME.
     */
    @NotNull
    private AtomicInteger getWaitCount() {
        return waitCount;
    }

    /**
     * @return the old X position.
     */
    public int getWindowX() {
        return positionX;
    }

    /**
     * @return the old Y position.
     */
    public int getWindowY() {
        return positionY;
    }

    /**
     * Get focused.
     */
    public void grabFocus() {

        final EmbeddedStageInterface stagePeer = getStageInterface();
        if (isFocused() || stagePeer == null) return;

        stagePeer.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);

        setFocused(true);

        if (isDebugEnabled()) {
            LOGGER.debug("got focused.");
        }
    }

    /**
     * Handle resize.
     */
    void handleResize() {

        final long time = System.currentTimeMillis();
        if (time - getLastResized() < MIN_RESIZE_INTERVAL) return;

        final JmeContext jmeContext = getJmeContext();

        final int displayWidth = JmeContextUtils.getWidth(jmeContext);
        final int displayHeight = JmeContextUtils.getHeight(jmeContext);

        final AsyncReadSyncWriteLock lock = getImageLock();
        lock.syncLock();
        try {

            final int pictureWidth = Math.max(displayWidth, 64);
            final int pictureHeight = Math.max(displayHeight, 64);

            final Picture picture = getPicture();

            if (isDebugEnabled()) {
                LOGGER.debug("handle resize from [" + getSceneWidth() + "x" + getSceneHeight() + "] to " +
                        "[" + pictureWidth + "x" + pictureHeight + "]");
            }

            picture.setWidth(pictureWidth);
            picture.setHeight(pictureHeight);

            if (fxData != null) {
                BufferUtils.destroyDirectBuffer(fxData);
            }

            if (tempData != null) {
                BufferUtils.destroyDirectBuffer(tempData);
            }

            if (jmeData != null) {
                BufferUtils.destroyDirectBuffer(jmeData);
            }

            if (jmeImage != null) {
                jmeImage.dispose();
            }

            fxData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
            tempData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
            tempIntData = tempData.asIntBuffer();
            jmeData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
            jmeImage = new Image(nativeFormat.get(), pictureWidth, pictureHeight, jmeData, ColorSpace.sRGB);

            final Texture2D texture = getTexture();
            texture.setImage(jmeImage);

            setSceneHeight(pictureHeight);
            setSceneWidth(pictureWidth);

            final EmbeddedStageInterface stagePeer = getStageInterface();
            final EmbeddedSceneInterface scenePeer = getSceneInterface();

            if (stagePeer != null && scenePeer != null) {
                Platform.runLater(() -> {
                    stagePeer.setSize(pictureWidth, pictureHeight);
                    scenePeer.setSize(pictureWidth, pictureHeight);
                    hostContainer.repaint();
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
            // TODO 3.1: use Format.ARGB8 and Format.BGRA8 and remove used
            // of exchangeData, fx2jme_ARGB82ABGR8,...
            switch (Pixels.getNativeFormat()) {
                case Pixels.Format.BYTE_ARGB:
                    try {
                        nativeFormat.complete(Format.valueOf("ARGB8"));
                        reorderData = null;
                    } catch (final Exception exc1) {
                        JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainer::reorder_ARGB82ABGR8;
                    }
                    break;
                case Pixels.Format.BYTE_BGRA_PRE:
                    try {
                        nativeFormat.complete(Format.valueOf("BGRA8"));
                        reorderData = null;
                    } catch (final Exception exc2) {
                        JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainer::reorder_BGRA82ABGR8;
                    }
                    break;
                default:
                    try {
                        nativeFormat.complete(Format.valueOf("ARGB8"));
                        reorderData = null;
                    } catch (final Exception exc3) {
                        JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainer::reorder_ARGB82ABGR8;
                    }
                    break;
            }
        });
    }

    public boolean isCovered(final int x, final int y) {

        final Image jmeImage = getJmeImage();
        final int pictureWidth = getSceneWidth();

        if (jmeImage == null || x < 0 || x >= pictureWidth) {
            return false;
        } else if (y < 0 || y >= getSceneHeight()) {
            return false;
        }

        final ByteBuffer data = jmeImage.getData(0);
        data.limit(data.capacity());

        final int alpha = data.get(3 + 4 * (y * pictureWidth + x));

        data.limit(0);

        if (isDebugEnabled()) {
            LOGGER.debug("is covered " + x + ", " + y + " = " + (alpha != 0));
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

        if (isDebugEnabled()) {
            LOGGER.debug("lost focused.");
        }
    }

    /**
     * Draw new frame of JavaFX to byte buffer.
     */
    void requestRedraw() {

        long time = 0;

        if (isDebugEnabled()) {
            time = System.currentTimeMillis();
            LOGGER.debug("started paint FX scene...");
        }

        final EmbeddedSceneInterface scenePeer = getSceneInterface();
        if (scenePeer == null) return;

        final ByteBuffer tempData = requireNonNull(getTempData());
        tempData.clear();

        final int pictureWidth = getSceneWidth();
        final int pictureHeight = getSceneHeight();

        if (!scenePeer.getPixels(requireNonNull(getTempIntData()), pictureWidth, pictureHeight)) {
            return;
        }

        tempData.flip();
        tempData.limit(pictureWidth * pictureHeight * 4);

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {

            final ByteBuffer fxData = requireNonNull(getFxData());
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

        if (isDebugEnabled()) {
            LOGGER.debug("finished paint FX scene(" + (System.currentTimeMillis() - time) + "ms.).");
        }
    }

    /**
     * Set a new scene to this container.
     *
     * @param newScene the new scene or null.
     * @param rootNode the new root of the scene or root.
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

        final Application application = requireNonNull(getApplication());
        application.enqueue(() -> {
            picture.setCullHint(newScene == null ? CullHint.Always : CullHint.Never);
            return null;
        });

        this.scene = newScene;

        if (embeddedWindow == null && newScene != null) {
            embeddedWindow = new EmbeddedWindow(hostContainer);
        }

        if (embeddedWindow == null) return;

        embeddedWindow.setScene(newScene);

        if (!embeddedWindow.isShowing()) {
            embeddedWindow.show();
        }
    }

    public void requestShowingCursor(final @NotNull CursorFrame cursorFrame) {

    }

    /**
     * Write javaFX frame to jME texture.
     */
    public Void writeToJME() {

        final AtomicInteger waitCount = getWaitCount();
        final int currentCount = waitCount.get();

        long time = 0;

        if (isDebugEnabled()) {
            time = System.currentTimeMillis();
            LOGGER.debug("started writing FX data to JME...");
        }

        final ByteBuffer jmeData = requireNonNull(getJmeData());
        jmeData.clear();

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {
            jmeData.put(requireNonNull(getFxData()));
        } finally {
            imageLock.syncUnlock();
        }

        jmeData.flip();

        final Image jmeImage = requireNonNull(getJmeImage());
        jmeImage.setUpdateNeeded();

        waitCount.subAndGet(currentCount);

        if (isDebugEnabled()) {
            LOGGER.debug("finished writing FX data to JME(" + (System.currentTimeMillis() - time) + "ms.).");
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