package com.jme3x.jfx;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.JmeContext;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import com.jme3x.jfx.cursor.CursorDisplayProvider;
import com.jme3x.jfx.listener.PaintListener;
import com.jme3x.jfx.util.JFXUtils;
import com.sun.glass.ui.Pixels;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.stage.EmbeddedWindow;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import rlib.concurrent.atomic.AtomicInteger;
import rlib.concurrent.lock.AsyncReadSyncWriteLock;
import rlib.concurrent.lock.LockFactory;
import rlib.logging.Logger;
import rlib.logging.LoggerManager;

/**
 * Need to pass -Dprism.dirtyopts=false on startup
 *
 * @author abies / Artur Biesiadowski
 */
public class JmeFxContainer {

    private static final Logger LOGGER = LoggerManager.getLogger(JmeFxContainer.class);

    /**
     * Актитвировал ли дебаг.
     */
    private static boolean debug;

    /**
     * @param debug актитвировал ли дебаг.
     */
    public static void setDebug(boolean debug) {
        JmeFxContainer.debug = debug;
    }

    /**
     * @return актитвировал ли дебаг.
     */
    public static boolean isDebug() {
        return debug;
    }

    public static JmeFxContainer install(final Application app, final Node guiNode, final CursorDisplayProvider cursorDisplayProvider) {

        final JmeFxContainer container = new JmeFxContainer(app.getAssetManager(), app, cursorDisplayProvider);
        guiNode.attachChild(container.getJmeNode());

        final JmeFXInputListener inputListener = new JmeFXInputListener(container);

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
     * Игровая стадия FX UI.
     */
    private final AppState fxAppState = new AbstractAppState() {

        @Override
        public void cleanup() {
            Platform.exit();
            super.cleanup();
        }
    };

    protected volatile CompletableFuture<Format> nativeFormat = new CompletableFuture<>();

    /**
     * Кол-во незаписанных в JME кадров.
     */
    protected final AtomicInteger waitCount;

    /**
     * Блокировщик доступа к данным изображений.
     */
    protected final AsyncReadSyncWriteLock imageLock;

    /**
     * Изображение для отрисовки UI.
     */
    protected final Picture picture;

    /**
     * Текстура на которой отрисовано UI.
     */
    protected final Texture2D texture;

    /**
     * Набор слушателей отрисовки.
     */
    protected volatile PaintListener[] paintListeners;

    /**
     * Текущая стадия UI.
     */
    protected volatile EmbeddedStageInterface stagePeer;

    /**
     * Текущая сцена UI.
     */
    protected volatile EmbeddedSceneInterface scenePeer;

    /**
     * Встроенное окно JavaFX UI.
     */
    protected volatile EmbeddedWindow stage;
    protected volatile HostInterface hostContainer;

    /**
     * Слушатель ввода пользователя.
     */
    protected volatile JmeFXInputListener inputListener;

    /**
     * Текущая сцена UI.
     */
    protected volatile Scene scene;

    /**
     * Приложение JME.
     */
    protected volatile Application application;

    /**
     * Рутовый узел текущей сцены.
     */
    protected volatile Group rootNode;

    /**
     * Отрисованное изображение UI.
     */
    protected volatile Image jmeImage;

    /**
     * Данные кадра отрисованного в jME.
     */
    protected volatile ByteBuffer jmeData;

    /**
     * Данные кадра отрисованного в JavaFX.
     */
    protected volatile ByteBuffer fxData;

    /**
     * Временные данные кадра отрисованного в JavaFX.
     */
    protected volatile ByteBuffer tempData;

    /**
     * Провайдер по отображению нужных курсоров.
     */
    protected volatile CursorDisplayProvider cursorDisplayProvider;

    /**
     * Функция реординга данных.
     */
    protected volatile Function<ByteBuffer, Void> reorderData;

    /**
     * Время последнего изменения размера.
     */
    protected volatile long lastResized;

    /**
     * Ширина картики для отрисовки UI.
     */
    protected volatile int pictureWidth;

    /**
     * Высота картики для отрисовки UI.
     */
    protected volatile int pictureHeight;

    /**
     * Предыдущее положение экрана по X.
     */
    protected volatile int oldX = -1;

    /**
     * Предыдущее положение экрана по Y.
     */
    protected volatile int oldY = -1;

    /**
     * Indent the window position to account for window decoration by Ronn
     */
    private volatile int windowOffsetX;
    private volatile int windowOffsetY;

    /**
     * Есть ли сейчас фокус на FX UI.
     */
    protected volatile boolean focus;

    /**
     * Поддержка полноэкранного режима.
     */
    protected volatile boolean fullScreenSupport;

    /**
     * Отображается ли курсор.
     */
    protected volatile boolean visibleCursor;

    /**
     * Доступен ли сейчас JavaFX.
     */
    protected volatile boolean enabled;

    /**
     * Набор состояний клавиш.
     */
    private final BitSet keyStateSet = new BitSet(0xFF);

    /**
     * Контекст JME.
     */
    private final JmeContext jmeContext;

    protected JmeFxContainer(final AssetManager assetManager, final Application application, final CursorDisplayProvider cursorDisplayProvider) {
        this.initFx();

        this.jmeContext = application.getContext();

        final Point decorationSize = JFXUtils.getWindowDecorationSize();

        this.waitCount = new AtomicInteger();
        this.imageLock = LockFactory.newPrimitiveAtomicARSWLock();
        this.paintListeners = new PaintListener[0];
        this.windowOffsetX = (int) decorationSize.getX();
        this.windowOffsetY = (int) decorationSize.getY();
        this.cursorDisplayProvider = cursorDisplayProvider;
        this.application = application;
        this.visibleCursor = true;

        final AppStateManager stateManager = application.getStateManager();
        stateManager.attach(fxAppState);

        this.hostContainer = new JmeFXHostInterfaceImpl(this);
        this.picture = new JavaFXPicture(this);
        this.picture.move(0, 0, -1);
        this.picture.setPosition(0, 0);

        handleResize();

        this.texture = new Texture2D(jmeImage);
        this.picture.setTexture(assetManager, texture, true);
    }

    /**
     * @param lastResized время последнего изменения размера.
     */
    private void setLastResized(final long lastResized) {
        this.lastResized = lastResized;
    }

    /**
     * @return время последнего изменения размера.
     */
    private long getLastResized() {
        return lastResized;
    }

    /**
     * @return приложение JME.
     */
    public Application getApplication() {
        return application;
    }

    /**
     * @return контекст JME.
     */
    public JmeContext getJmeContext() {
        return jmeContext;
    }

    /**
     * Добавление нового слушателя.
     */
    public void addPaintListener(final PaintListener paintListener) {

        final List<PaintListener> temp = new ArrayList<>();
        Collections.addAll(temp, getPaintListeners());
        temp.add(paintListener);

        setPaintListeners(temp.toArray(new PaintListener[temp.size()]));
    }

    /**
     * Создание задачи по записи FX UI на JME.
     */
    protected void addWriteTask() {
        application.enqueue(this::writeToJME);
    }

    /**
     * @return провайдер по отображению нужных курсоров.
     */
    public CursorDisplayProvider getCursorDisplayProvider() {
        return cursorDisplayProvider;
    }

    /**
     * @return данные кадра отрисованного в JavaFX.
     */
    public ByteBuffer getFxData() {
        return fxData;
    }

    /**
     * @return блокировщик доступа к данным изображений.
     */
    private AsyncReadSyncWriteLock getImageLock() {
        return imageLock;
    }

    /**
     * @return слушатель ввода пользователя.
     */
    public JmeFXInputListener getInputListener() {
        return inputListener;
    }

    /**
     * @param inputListener слушатель ввода пользователя.
     */
    public void setInputListener(final JmeFXInputListener inputListener) {
        this.inputListener = inputListener;
    }

    /**
     * @return данные кадра отрисованного в jME.
     */
    public ByteBuffer getJmeData() {
        return jmeData;
    }

    /**
     * @return отрисованное изображение UI.
     */
    public Image getJmeImage() {
        return jmeImage;
    }

    /**
     * @return изображение для отрисовки UI.
     */
    public Picture getJmeNode() {
        return picture;
    }

    /**
     * @return набор состояний клавиш.
     */
    public BitSet getKeyStateSet() {
        return keyStateSet;
    }

    /**
     * @return предыдущее положение экрана по X.
     */
    public int getOldX() {
        return oldX;
    }

    /**
     * @param oldX предыдущее положение экрана по X.
     */
    public void setOldX(final int oldX) {
        this.oldX = oldX;
    }

    /**
     * @return предыдущее положение экрана по Y.
     */
    public int getOldY() {
        return oldY;
    }

    /**
     * @param oldY предыдущее положение экрана по Y.
     */
    public void setOldY(final int oldY) {
        this.oldY = oldY;
    }

    /**
     * @return набор слушателей отрисовки.
     */
    private PaintListener[] getPaintListeners() {
        return paintListeners;
    }

    /**
     * @param paintListeners набор слушателей отрисовки.
     */
    private void setPaintListeners(final PaintListener[] paintListeners) {
        this.paintListeners = paintListeners;
    }

    /**
     * @return изображение для отрисовки UI.
     */
    public Picture getPicture() {
        return picture;
    }

    /**
     * @return высота картики для отрисовки UI.
     */
    public int getPictureHeight() {
        return pictureHeight;
    }

    /**
     * @param pictureHeight высота картики для отрисовки UI.
     */
    public void setPictureHeight(final int pictureHeight) {
        this.pictureHeight = pictureHeight;
    }

    /**
     * @return ширина картики для отрисовки UI.
     */
    public int getPictureWidth() {
        return pictureWidth;
    }

    /**
     * @param pictureWidth ширина картики для отрисовки UI.
     */
    public void setPictureWidth(final int pictureWidth) {
        this.pictureWidth = pictureWidth;
    }

    /**
     * @return функция реординга данных.
     */
    public Function<ByteBuffer, Void> getReorderData() {
        return reorderData;
    }

    /**
     * @return рутовый узел текущей сцены.
     */
    public Group getRootNode() {
        return rootNode;
    }

    /**
     * @return текущая сцена UI.
     */
    public Scene getScene() {
        return scene;
    }

    /**
     * @return текущая сцена UI.
     */
    public EmbeddedSceneInterface getScenePeer() {
        return scenePeer;
    }

    /**
     * @param scenePeer текущая сцена UI.
     */
    public void setScenePeer(final EmbeddedSceneInterface scenePeer) {
        this.scenePeer = scenePeer;
    }

    /**
     * @return встроенное окно JavaFX UI.
     */
    public EmbeddedWindow getStage() {
        return stage;
    }

    /**
     * @return текущая стадия UI.
     */
    public EmbeddedStageInterface getStagePeer() {
        return stagePeer;
    }

    /**
     * @param stagePeer текущая стадия UI.
     */
    public void setStagePeer(final EmbeddedStageInterface stagePeer) {
        this.stagePeer = stagePeer;
    }

    /**
     * @return временные данные кадра отрисованного в JavaFX.
     */
    public ByteBuffer getTempData() {
        return tempData;
    }

    /**
     * @return текстура на которой отрисовано UI.
     */
    public Texture2D getTexture() {
        return texture;
    }

    /**
     * @return кол-во незаписанных в JME кадров.
     */
    public AtomicInteger getWaitCount() {
        return waitCount;
    }

    /**
     * Indent the window position to account for window decoration.
     */
    public int getWindowOffsetX() {
        return windowOffsetX;
    }

    /**
     * Indent the window position to account for window decoration.
     */
    public void setWindowOffsetX(final int windowOffsetX) {
        this.windowOffsetX = windowOffsetX;
    }

    /**
     * Indent the window position to account for window decoration.
     */
    public int getWindowOffsetY() {
        return windowOffsetY;
    }

    /**
     * Indent the window position to account for window decoration.
     */
    public void setWindowOffsetY(final int windowOffsetY) {
        this.windowOffsetY = windowOffsetY;
    }

    /**
     * @return предыдущее положение экрана по X.
     */
    public int getWindowX() {
        return oldX;
    }

    /**
     * @return предыдущее положение экрана по Y.
     */
    public int getWindowY() {
        return oldY;
    }

    /**
     * Получение фокуса сценой FX UI.
     */
    public void grabFocus() {

        final EmbeddedStageInterface stagePeer = getStagePeer();

        if (isFocus() || stagePeer == null) {
            return;
        }

        stagePeer.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);
        setFocus(true);

        if (isDebug()) {
            LOGGER.debug("got focus.");
        }
    }

    /**
     * Инициализация или обновление размеров изображения.
     */
    public void handleResize() {

        final long time = System.currentTimeMillis();

        if (time - getLastResized() < 1000) {
            return;
        }

        final JmeContext jmeContext = getJmeContext();

        final int displayWidth = JFXUtils.getWidth(jmeContext);
        final int displayHeight = JFXUtils.getHeight(jmeContext);

        final AsyncReadSyncWriteLock lock = getImageLock();
        lock.syncLock();
        try {

            final int pictureWidth = Math.max(displayWidth, 64);
            final int pictureHeight = Math.max(displayHeight, 64);

            final Picture picture = getPicture();

            if (isDebug()) {
                LOGGER.debug("handle resize from [" + getPictureWidth() + "x" + getPictureHeight() + "] to [" + pictureWidth + "x" + pictureHeight + "]");
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
            jmeData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
            jmeImage = new Image(nativeFormat.get(), pictureWidth, pictureHeight, jmeData, ColorSpace.sRGB);

            final Texture2D texture = getTexture();

            if (texture != null) {
                texture.setImage(jmeImage);
            }

            setPictureHeight(pictureHeight);
            setPictureWidth(pictureWidth);

            final EmbeddedStageInterface stagePeer = getStagePeer();
            final EmbeddedSceneInterface scenePeer = getScenePeer();

            if (stagePeer != null) {
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

    private void initFx() {
        PlatformImpl.startup(() -> {
            // TODO 3.1: use Format.ARGB8 and Format.BGRA8 and remove used
            // of exchangeData, fx2jme_ARGB82ABGR8,...
            switch (Pixels.getNativeFormat()) {
                case Pixels.Format.BYTE_ARGB:
                    try {
                        JmeFxContainer.this.nativeFormat.complete(Format.valueOf("ARGB8"));
                        reorderData = null;
                    } catch (final Exception exc1) {
                        JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainer::reorder_ARGB82ABGR8;
                    }
                    break;
                case Pixels.Format.BYTE_BGRA_PRE:
                    try {
                        JmeFxContainer.this.nativeFormat.complete(Format.valueOf("BGRA8"));
                        reorderData = null;
                    } catch (final Exception exc2) {
                        JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainer::reorder_BGRA82ABGR8;
                    }
                    break;
                default:
                    try {
                        JmeFxContainer.this.nativeFormat.complete(Format.valueOf("ARGB8"));
                        reorderData = null;
                    } catch (final Exception exc3) {
                        JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainer::reorder_ARGB82ABGR8;
                    }
                    break;
            }
        });
    }

    /**
     * Есть ли по этим координатом элемент JavaFX на сцене.
     */
    public boolean isCovered(final int x, final int y) {

        final int pictureWidth = getPictureWidth();

        if (x < 0 || x >= pictureWidth) {
            return false;
        } else if (y < 0 || y >= getPictureHeight()) {
            return false;
        }

        final Image jmeImage = getJmeImage();

        final ByteBuffer data = jmeImage.getData(0);
        data.limit(data.capacity());

        final int alpha = data.get(3 + 4 * (y * pictureWidth + x));

        data.limit(0);

        if (isDebug()) {
            LOGGER.debug("is covered " + x + ", " + y + " = " + (alpha != 0));
        }

        return alpha != 0;
    }

    /**
     * @return есть ли сейчас фокус на FX UI.
     */
    public boolean isFocus() {
        return focus;
    }

    /**
     * @param focus есть ли сейчас фокус на FX UI.
     */
    public void setFocus(final boolean focus) {
        this.focus = focus;
    }

    /**
     * @return поддержка полноэкранного режима.
     */
    public boolean isFullScreenSupport() {
        return fullScreenSupport;
    }

    /**
     * @return нужна ли отрисовка.
     */
    public boolean isNeedWriteToJME() {
        return waitCount.get() > 0;
    }

    /**
     * @return отображается ли курсор.
     */
    public boolean isVisibleCursor() {
        return visibleCursor;
    }

    /**
     * @param visibleCursor отображается ли курсор.
     */
    public void setVisibleCursor(final boolean visibleCursor) {
        this.visibleCursor = visibleCursor;
    }

    /**
     * Уберание фокуса из сцены.
     */
    public void loseFocus() {

        final EmbeddedStageInterface stagePeer = getStagePeer();

        if (!isFocus() || stagePeer == null) {
            return;
        }

        stagePeer.setFocused(false, AbstractEvents.FOCUSEVENT_DEACTIVATED);

        setFocus(false);

        if (isDebug()) {
            LOGGER.debug("lost focus.");
        }
    }

    /**
     * Отрисока контейнера.
     */
    public void paintComponent() {

        long time = 0;

        if (isDebug()) {
            time = System.currentTimeMillis();
            LOGGER.debug("started paint FX scene...");
        }

        final EmbeddedSceneInterface scenePeer = getScenePeer();

        if (scenePeer == null) {
            return;
        }

        final PaintListener[] paintListeners = getPaintListeners();

        if (paintListeners.length > 0) {
            for (final PaintListener paintListener : paintListeners) {
                paintListener.prePaint();
            }
        }

        final ByteBuffer tempData = getTempData();
        tempData.clear();

        final IntBuffer intBuffer = tempData.asIntBuffer();

        final int pictureWidth = getPictureWidth();
        final int pictureHeight = getPictureHeight();

        if (!scenePeer.getPixels(intBuffer, pictureWidth, pictureHeight)) {
            return;
        }

        tempData.flip();
        tempData.limit(pictureWidth * pictureHeight * 4);

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {

            final ByteBuffer fxData = getFxData();
            fxData.clear();
            fxData.put(tempData);
            fxData.flip();

            final Function<ByteBuffer, Void> reorderData = getReorderData();

            if (reorderData != null) {
                reorderData.apply(fxData);
                fxData.position(0);
            }

        } catch (final Exception exc) {
            exc.printStackTrace();
        } finally {
            imageLock.syncUnlock();
        }

        if (paintListeners.length > 0) {
            for (final PaintListener paintListener : paintListeners) {
                paintListener.postPaint();
            }
        }

        final AtomicInteger waitCount = getWaitCount();
        waitCount.incrementAndGet();

        if (isDebug()) {
            LOGGER.debug("finished paint FX scene(" + (System.currentTimeMillis() - time) + "ms.).");
        }
    }

    /**
     * Удаление слушателя отрисовки.
     */
    public void removePaintListener(final PaintListener paintListener) {

        final List<PaintListener> temp = new ArrayList<>();
        Collections.addAll(temp, getPaintListeners());
        temp.remove(paintListener);

        setPaintListeners(temp.toArray(new PaintListener[temp.size()]));
    }

    int retrieveKeyState() {

        int embedModifiers = 0;

        if (keyStateSet.get(KeyEvent.VK_SHIFT)) {
            embedModifiers |= AbstractEvents.MODIFIER_SHIFT;
        }

        if (keyStateSet.get(KeyEvent.VK_CONTROL)) {
            embedModifiers |= AbstractEvents.MODIFIER_CONTROL;
        }

        if (keyStateSet.get(KeyEvent.VK_ALT)) {
            embedModifiers |= AbstractEvents.MODIFIER_ALT;
        }

        if (keyStateSet.get(KeyEvent.VK_META)) {
            embedModifiers |= AbstractEvents.MODIFIER_META;
        }

        return embedModifiers;
    }

    /**
     * call via gui manager!
     */
    public void setEverListeningRawInputListener(final RawInputListener rawInputListenerAdapter) {
        this.inputListener.setEverListeningRawInputListener(rawInputListenerAdapter);
    }

    void setFxEnabled(final boolean enabled) {
    }

    public void setScene(final Scene newScene, final Group highLevelGroup) {
        this.rootNode = highLevelGroup;
        FxPlatformExecutor.runOnFxApplication(() -> JmeFxContainer.this.setSceneImpl(newScene));
    }

    /*
     * Called on JavaFX application thread.
     */
    private void setSceneImpl(final Scene newScene) {
        if (this.stage != null && newScene == null) {
            this.stage.hide();
            this.stage = null;
        }

        this.application.enqueue(() -> {
            JmeFxContainer.this.picture.setCullHint(newScene == null ? CullHint.Always : CullHint.Never);
            return null;
        });

        this.scene = newScene;
        if (this.stage == null && newScene != null) {
            this.stage = new EmbeddedWindow(this.hostContainer);
        }
        if (this.stage != null) {
            this.stage.setScene(newScene);
            if (!this.stage.isShowing()) {
                this.stage.show();
            }
        }
    }

    /**
     * Запись резульата FX UI на текстуру в JME.
     */
    public Void writeToJME() {

        final AtomicInteger waitCount = getWaitCount();
        final int currentCount = waitCount.get();

        long time = 0;

        if (isDebug()) {
            time = System.currentTimeMillis();
            LOGGER.debug("started writing FX data to JME...");
        }

        final ByteBuffer jmeData = getJmeData();
        jmeData.clear();

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {
            jmeData.put(getFxData());
        } finally {
            imageLock.syncUnlock();
        }

        jmeData.flip();

        final Image jmeImage = getJmeImage();
        jmeImage.setUpdateNeeded();

        waitCount.subAndGet(currentCount);

        if (isDebug()) {
            LOGGER.debug("finished writing FX data to JME(" + (System.currentTimeMillis() - time) + "ms.).");
        }

        return null;
    }

    /**
     * @param enabled доступен ли сейчас JavaFX.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return доступен ли сейчас JavaFX.
     */
    public boolean isEnabled() {
        return enabled;
    }
}