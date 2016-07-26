package com.jme3x.jfx;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import com.jme3x.jfx.cursor.CursorDisplayProvider;
import com.jme3x.jfx.util.JFXEmbeddedUtils;
import com.jme3x.jfx.util.JFXWindowsUtils;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;

import javafx.application.Platform;
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

    public static final int PROP_MIN_RESIZE_INTERVAL = 300;

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
     * Контейнер сцены JavaFX.
     */
    protected volatile JmeJFXPanel hostContainer;

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
        final Point decorationSize = JFXWindowsUtils.getWindowDecorationSize();

        final AppStateManager stateManager = application.getStateManager();
        stateManager.attach(fxAppState);

        this.jmeContext = application.getContext();
        this.waitCount = new AtomicInteger();
        this.imageLock = LockFactory.newPrimitiveAtomicARSWLock();
        this.windowOffsetX = (int) decorationSize.getX();
        this.windowOffsetY = (int) decorationSize.getY();
        this.cursorDisplayProvider = cursorDisplayProvider;
        this.application = application;
        this.visibleCursor = true;

        this.hostContainer = new JmeJFXPanel(this);
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
     * @return текущая сцена UI.
     */
    public Scene getScene() {
        return scene;
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

        final JmeJFXPanel hostContainer = getHostContainer();
        hostContainer.handleEvent(new FocusEvent(hostContainer, FocusEvent.FOCUS_GAINED));

        setFocus(true);
    }

    /**
     * Инициализация или обновление размеров изображения.
     */
    public void handleResize() {

        final long time = System.currentTimeMillis();
        if(time - getLastResized() < PROP_MIN_RESIZE_INTERVAL) return;

        final JmeContext jmeContext = getJmeContext();

        final int displayWidth = JFXWindowsUtils.getWidth(jmeContext);
        final int displayHeight = JFXWindowsUtils.getHeight(jmeContext);

        final AsyncReadSyncWriteLock lock = getImageLock();
        lock.syncLock();
        try {

            final int pictureWidth = Math.max(displayWidth, 64);
            final int pictureHeight = Math.max(displayHeight, 64);

            final Picture picture = getPicture();

            if(isDebug()) {
                LOGGER.debug("handle resize from [" + getPictureWidth() + "x" + getPictureHeight() + "] to [" + pictureWidth + "x" + pictureHeight + "]");
            }

            picture.setWidth(pictureWidth);
            picture.setHeight(pictureHeight);

            if (fxData != null) BufferUtils.destroyDirectBuffer(fxData);
            if (tempData != null) BufferUtils.destroyDirectBuffer(tempData);
            if (jmeData != null) BufferUtils.destroyDirectBuffer(jmeData);
            if (jmeImage != null) jmeImage.dispose();

            fxData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
            tempData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
            jmeData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
            jmeImage = new Image(Format.BGRA8, pictureWidth, pictureHeight, jmeData, ColorSpace.sRGB);

            final Texture2D texture = getTexture();
            if (texture != null) texture.setImage(jmeImage);

            setPictureHeight(pictureHeight);
            setPictureWidth(pictureWidth);

            hostContainer.handleResize(pictureWidth, pictureHeight);

        } catch (final Exception e) {
            LOGGER.warning(e);
        } finally {
            lock.syncUnlock();
        }

        setLastResized(time);
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

        if(isDebug()) {
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

        final JmeJFXPanel hostContainer = getHostContainer();
        hostContainer.handleEvent(new FocusEvent(hostContainer, FocusEvent.FOCUS_LOST));

        setFocus(false);
    }

    /**
     * @return контейнер сцены JavaFX.
     */
    public JmeJFXPanel getHostContainer() {
        return hostContainer;
    }

    /**
     * Отрисока контейнера.
     */
    public void paintComponent() {

        long time = 0;

        if(isDebug()) {
            time = System.currentTimeMillis();
            LOGGER.debug("started paint FX scene...");
        }

        final JmeJFXPanel hostContainer = getHostContainer();
        if (hostContainer == null) return;

        final ByteBuffer tempData = getTempData();
        tempData.clear();

        final IntBuffer intBuffer = tempData.asIntBuffer();

        final int pictureWidth = getPictureWidth();
        final int pictureHeight = getPictureHeight();

        if (!JFXEmbeddedUtils.getPixels(hostContainer, intBuffer, pictureWidth, pictureHeight)) return;

        tempData.flip();
        tempData.limit(pictureWidth * pictureHeight * 4);

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {

            final ByteBuffer fxData = getFxData();
            fxData.clear();
            fxData.put(tempData);
            fxData.flip();

        } catch (final Exception exc) {
            exc.printStackTrace();
        } finally {
            imageLock.syncUnlock();
        }

        final AtomicInteger waitCount = getWaitCount();
        waitCount.incrementAndGet();

        if(isDebug()) {
            LOGGER.debug("finished paint FX scene(" + (System.currentTimeMillis() - time) + "ms.).");
        }
    }

    int retrieveKeyState() {

        int embedModifiers = 0;

        /*if (keyStateSet.get(KeyEvent.VK_SHIFT)) {
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
*/
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

    /**
     * @param scene текущая сцена UI.
     */
    public void setScene(final Scene scene) {
        this.scene = scene;
        this.hostContainer.setScene(scene);
        application.enqueue(() -> {
            picture.setCullHint(scene == null ? Spatial.CullHint.Always : Spatial.CullHint.Never);
            return null;
        });
    }

    /**
     * Запись резульата FX UI на текстуру в JME.
     */
    public Void writeToJME() {

        final AtomicInteger waitCount = getWaitCount();
        final int currentCount = waitCount.get();

        long time = 0;

        if(isDebug()) {
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

        if(isDebug()) {
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