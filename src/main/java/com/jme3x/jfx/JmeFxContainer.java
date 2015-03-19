package com.jme3x.jfx;

import static rlib.util.ReflectionUtils.getStaticFieldValue;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import org.lwjgl.opengl.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rlib.concurrent.atomic.AtomicInteger;
import rlib.concurrent.lock.AsynReadSynWriteLock;
import rlib.concurrent.lock.LockFactory;
import rlib.util.ReflectionUtils;
import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;
import rlib.util.dictionary.DictionaryFactory;
import rlib.util.dictionary.ObjectDictionary;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import com.jme3x.jfx.cursor.ICursorDisplayProvider;
import com.jme3x.jfx.listener.PaintListener;
import com.jme3x.jfx.util.JFXUtils;
import com.sun.glass.ui.Accessible;
import com.sun.glass.ui.Pixels;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.cursor.CursorType;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.scene.SceneHelper.SceneAccessor;
import com.sun.javafx.stage.EmbeddedWindow;

/**
 * Need to pass -Dprism.dirtyopts=false on startup
 *
 * @author abies / Artur Biesiadowski
 */
public class JmeFxContainer {

	private static final Logger logger = LoggerFactory.getLogger(JmeFxContainer.class);

	public static final String FIELD_SCENE_ACCESSOR = "sceneAccessor";

	public static JmeFxContainer install(final Application app, final Node guiNode, final boolean fullScreenSupport, final ICursorDisplayProvider cursorDisplayProvider) {

		final JmeFxContainer container = new JmeFxContainer(app.getAssetManager(), app, fullScreenSupport, cursorDisplayProvider);
		guiNode.attachChild(container.getJmeNode());

		final JmeFXInputListener inputListener = new JmeFXInputListener(container);

		container.setInputListener(inputListener);

		final InputManager inputManager = app.getInputManager();
		inputManager.addRawInputListener(inputListener);

		if(fullScreenSupport) {
			container.installSceneAccessorHack();
		}

		return container;
	}

	// TODO benchmark
	private static Void reorder_ARGB82ABGR8(final ByteBuffer data) {

		final int limit = data.limit() - 3;

		byte v;

		for(int i = 0; i < limit; i += 4) {
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

		for(int i = 0; i < limit; i += 4) {
			v0 = data.get(i + 0);
			v1 = data.get(i + 1);
			v2 = data.get(i + 2);
			v3 = data.get(i + 3);
			data.put(i + 0, v3);
			data.put(i + 1, v0);
			data.put(i + 2, v1);
			data.put(i + 3, v2);
		}

		return null;
	}

	/** игровая стадия FX UI */
	private final AppState fxAppState = new AbstractAppState() {

		@Override
		public void cleanup() {
			Platform.exit();
			super.cleanup();
		};
	};

	protected final Array<PopupSnapper> activeSnappers;

	/** кол-во незаписанных в JME кадров */
	protected final AtomicInteger waitCount;
	/** блокировщик доступа к данным изображений */
	protected final AsynReadSynWriteLock imageLock;
	/** изображение для отрисовки UI */
	protected final Picture picture;
	/** текстура на которой отрисовано UI */
	protected final Texture2D texture;

	/** набор слушателей отрисовки */
	protected volatile PaintListener[] paintListeners;

	/** текущая стадия UI */
	protected volatile EmbeddedStageInterface stagePeer;
	/** текущая сцена UI */
	protected volatile EmbeddedSceneInterface scenePeer;

	/** встроенное окно JavaFX UI */
	protected volatile EmbeddedWindow stage;
	protected volatile HostInterface hostContainer;
	/** слушатель ввода пользователя */
	protected volatile JmeFXInputListener inputListener;

	/** текущая сцена UI */
	protected volatile Scene scene;
	protected volatile Application app;

	/** рутовый узел текущей сцены */
	protected volatile Group rootNode;
	/** отрисованное изображение UI */
	protected volatile Image jmeImage;

	/** данные кадра отрисованного в jME */
	protected volatile ByteBuffer jmeData;
	/** данные кадра отрисованного в JavaFX */
	protected volatile ByteBuffer fxData;
	/** временные данные кадра отрисованного в JavaFX */
	protected volatile ByteBuffer tempData;

	protected volatile CompletableFuture<Format> nativeFormat = new CompletableFuture<Format>();
	/** провайдер по отображению нужных курсоров */
	protected volatile ICursorDisplayProvider cursorDisplayProvider;

	/** функция реординга данных */
	protected volatile Function<ByteBuffer, Void> reorderData;

	/** ширина картики для отрисовки UI */
	protected volatile int pictureWidth;
	/** высота картики для отрисовки UI */
	protected volatile int pictureHeight;

	/** предыдущее положение экрана по X */
	protected volatile int oldX = -1;
	/** предыдущее положение экрана по Y */
	protected volatile int oldY = -1;

	/** Indent the window position to account for window decoration by Ronn */
	private volatile int windowOffsetX;
	private volatile int windowOffsetY;

	protected volatile boolean fxDataReady;
	/** есть ли сейчас фокус на FX UI */
	protected volatile boolean focus;
	/** поддержка полноэкранного режима */
	protected volatile boolean fullScreenSuppport;

	/** отображается ли курсор */
	protected volatile boolean visibleCursor;

	public CursorType lastcursor;

	/** набор состояний клавиш */
	private final BitSet keyStateSet = new BitSet(0xFF);

	/** словарь с набором созданных всплывающих окон */
	private final ObjectDictionary<Window, PopupSnapper> snappers;

	protected JmeFxContainer(final AssetManager assetManager, final Application app, final boolean fullScreenSupport, final ICursorDisplayProvider cursorDisplayProvider) {
		this.initFx();

		final Point decorationSize = JFXUtils.getWindowDecorationSize();

		this.snappers = DictionaryFactory.newObjectDictionary();
		this.waitCount = new AtomicInteger();
		this.activeSnappers = ArrayFactory.newConcurrentAtomicArray(PopupSnapper.class);
		this.imageLock = LockFactory.newPrimitiveAtomicARSWLock();
		this.paintListeners = new PaintListener[0];
		this.windowOffsetX = (int) decorationSize.getX();
		this.windowOffsetY = (int) decorationSize.getY();
		this.cursorDisplayProvider = cursorDisplayProvider;
		this.app = app;
		this.fullScreenSuppport = fullScreenSupport;
		this.visibleCursor = true;

		final AppStateManager stateManager = app.getStateManager();
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
	 * @param visibleCursor отображается ли курсор.
	 */
	public void setVisibleCursor(final boolean visibleCursor) {
		this.visibleCursor = visibleCursor;
	}

	/**
	 * @return отображается ли курсор.
	 */
	public boolean isVisibleCursor() {
		return visibleCursor;
	}

	/**
	 * @return словарь с набором созданных всплывающих окон.
	 */
	public ObjectDictionary<Window, PopupSnapper> getSnappers() {
		return snappers;
	}

	/**
	 * @return нужна ли отрисовка.
	 */
	public boolean isNeedWriteToJME() {
		return waitCount.get() > 0;
	}

	/**
	 * @return кол-во незаписанных в JME кадров.
	 */
	public AtomicInteger getWaitCount() {
		return waitCount;
	}

	/**
	 * Добавление нового слушателя.
	 */
	public void addPaintListener(final PaintListener paintListener) {

		final List<PaintListener> temp = new ArrayList<>();

		for(final PaintListener listener : getPaintListeners()) {
			temp.add(listener);
		}

		temp.add(paintListener);

		setPaintListeners(temp.toArray(new PaintListener[temp.size()]));
	}

	/**
	 * Создание задачи по записи FX UI на JME.
	 */
	protected void addWriteTask() {
		app.enqueue(() -> {
			writeToJME();
			return null;
		});
	}

	/**
	 * @return провайдер по отображению нужных курсоров.
	 */
	public ICursorDisplayProvider getCursorDisplayProvider() {
		return cursorDisplayProvider;
	}

	/**
	 * @return блокировщик доступа к данным изображений.
	 */
	private AsynReadSynWriteLock getImageLock() {
		return imageLock;
	}

	/**
	 * @return слушатель ввода пользователя.
	 */
	public JmeFXInputListener getInputListener() {
		return inputListener;
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
	 * @return предыдущее положение экрана по Y.
	 */
	public int getOldY() {
		return oldY;
	}

	/**
	 * @return набор слушателей отрисовки.
	 */
	private PaintListener[] getPaintListeners() {
		return paintListeners;
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
	 * @return ширина картики для отрисовки UI.
	 */
	public int getPictureWidth() {
		return pictureWidth;
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
	 * @return текстура на которой отрисовано UI.
	 */
	public Texture2D getTexture() {
		return texture;
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
	public int getWindowOffsetY() {
		return windowOffsetY;
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

		if(!isFocus() && stagePeer != null) {
			stagePeer.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);
			setFocus(true);
		}
	}

	/**
	 * Инициализация или обновление размеров изображения.
	 */
	public void handleResize() {

		final AsynReadSynWriteLock lock = getImageLock();
		lock.synLock();
		try {

			final int pictureWidth = Math.max(Display.getWidth(), 64);
			final int pictureHeight = Math.max(Display.getHeight(), 64);

			final Picture picture = getPicture();
			picture.setWidth(pictureWidth);
			picture.setHeight(pictureHeight);

			if(fxData != null) {
				BufferUtils.destroyDirectBuffer(fxData);
			}

			if(tempData != null) {
				BufferUtils.destroyDirectBuffer(tempData);
			}

			if(jmeData != null) {
				BufferUtils.destroyDirectBuffer(jmeData);
			}

			if(jmeImage != null) {
				jmeImage.dispose();
			}

			fxData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
			tempData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
			jmeData = BufferUtils.createByteBuffer(pictureWidth * pictureHeight * 4);
			jmeImage = new Image(nativeFormat.get(), pictureWidth, pictureHeight, jmeData, ColorSpace.sRGB);

			final Texture2D texture = getTexture();

			if(texture != null) {
				texture.setImage(jmeImage);
			}

			setPictureHeight(pictureHeight);
			setPictureWidth(pictureWidth);

			final EmbeddedStageInterface stagePeer = getStagePeer();
			final EmbeddedSceneInterface scenePeer = getScenePeer();

			if(stagePeer != null) {
				Platform.runLater(() -> {
					stagePeer.setSize(pictureWidth, pictureHeight);
					scenePeer.setSize(pictureWidth, pictureHeight);
					hostContainer.repaint();
				});
			}

		} catch(final Exception exc) {
			exc.printStackTrace();
		} finally {
			lock.synUnlock();
		}
	}

	private void initFx() {
		PlatformImpl.startup(() -> {
			// TODO 3.1: use Format.ARGB8 and Format.BGRA8 and remove used
			// of exchangeData, fx2jme_ARGB82ABGR8,...
				switch(Pixels.getNativeFormat()) {
					case Pixels.Format.BYTE_ARGB:
						try {
							JmeFxContainer.this.nativeFormat.complete(Format.valueOf("ARGB8"));
							reorderData = null;
						} catch(final Exception exc1) {
							JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
							reorderData = JmeFxContainer::reorder_ARGB82ABGR8;
						}
						break;
					case Pixels.Format.BYTE_BGRA_PRE:
						try {
							JmeFxContainer.this.nativeFormat.complete(Format.valueOf("BGRA8"));
							reorderData = null;
						} catch(final Exception exc2) {
							JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
							reorderData = JmeFxContainer::reorder_BGRA82ABGR8;
						}
						break;
					default:
						try {
							JmeFxContainer.this.nativeFormat.complete(Format.valueOf("ARGB8"));
							reorderData = null;
						} catch(final Exception exc3) {
							JmeFxContainer.this.nativeFormat.complete(Format.ABGR8);
							reorderData = JmeFxContainer::reorder_ARGB82ABGR8;
						}
						break;
				}
			});

	}

	protected void installSceneAccessorHack() {

		try {

			final SceneAccessor originalSceneAccessor = getStaticFieldValue(SceneHelper.class, FIELD_SCENE_ACCESSOR);
			final SceneAccessor newSceneAccessor = new SceneAccessor() {

				@Override
				public Scene createPopupScene(final Parent root) {

					final Scene scene = originalSceneAccessor.createPopupScene(root);
					scene.windowProperty().addListener((observable, oldValue, window) -> window.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {

						if(!Display.isFullscreen()) {
							return;
						}

						final PopupSnapper popupSnapper = new PopupSnapper(JmeFxContainer.this, window, scene);
						final ObjectDictionary<Window, PopupSnapper> snappers = getSnappers();
						snappers.put(window, popupSnapper);
						popupSnapper.start();
					}));

					scene.windowProperty().addListener((observable, oldValue, window) -> window.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {

						if(!Display.isFullscreen()) {
							return;
						}

						final ObjectDictionary<Window, PopupSnapper> snappers = getSnappers();
						final PopupSnapper popupSnapper = snappers.remove(window);
						if(popupSnapper == null) {
							logger.warn("Cannot find snapper for window " + window);
						} else {
							popupSnapper.stop();
						}
					}));

					return scene;
				}

				@Override
				public Camera getEffectiveCamera(final Scene scene) {
					return originalSceneAccessor.getEffectiveCamera(scene);
				}

				@Override
				public void parentEffectiveOrientationInvalidated(final Scene scene) {
					originalSceneAccessor.parentEffectiveOrientationInvalidated(scene);
				}

				@Override
				public void setPaused(final boolean paused) {
					originalSceneAccessor.setPaused(paused);
				}

				@Override
				public void setTransientFocusContainer(final Scene scene, final javafx.scene.Node node) {
				}

				@Override
				public Accessible getAccessible(final Scene scene) {
					return originalSceneAccessor.getAccessible(scene);
				}
			};

			ReflectionUtils.setStaticFieldValue(SceneHelper.class, FIELD_SCENE_ACCESSOR, newSceneAccessor);

		} catch(final Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Есть ли по этим координатом элемент JavaFX на сцене.
	 */
	public boolean isCovered(final int x, final int y) {

		final int pictureWidth = getPictureWidth();

		if(x < 0 || x >= pictureWidth) {
			return false;
		}

		if(y < 0 || y >= getPictureHeight()) {
			return false;
		}

		final Image jmeImage = getJmeImage();

		final ByteBuffer data = jmeImage.getData(0);
		data.limit(data.capacity());

		final int alpha = data.get(3 + 4 * (y * pictureWidth + x));

		data.limit(0);

		return alpha != 0;
	}

	/**
	 * @return есть ли сейчас фокус на FX UI.
	 */
	public boolean isFocus() {
		return focus;
	}

	/**
	 * @return поддержка полноэкранного режима.
	 */
	public boolean isFullScreenSuppport() {
		return fullScreenSuppport;
	}

	/**
	 * Уберание фокуса из сцены.
	 */
	public void loseFocus() {

		final EmbeddedStageInterface stagePeer = getStagePeer();

		if(isFocus() && stagePeer != null) {
			stagePeer.setFocused(false, AbstractEvents.FOCUSEVENT_DEACTIVATED);
			setFocus(false);
		}
	}

	/**
	 * @return
	 */
	public Array<PopupSnapper> getActiveSnappers() {
		return activeSnappers;
	}

	/**
	 * @return данные кадра отрисованного в JavaFX.
	 */
	public ByteBuffer getFxData() {
		return fxData;
	}

	/**
	 * @return временные данные кадра отрисованного в JavaFX.
	 */
	public ByteBuffer getTempData() {
		return tempData;
	}

	/**
	 * Отрисока контейнера.
	 */
	public void paintComponent() {

		final EmbeddedSceneInterface scenePeer = getScenePeer();

		if(scenePeer == null) {
			return;
		}

		final PaintListener[] paintListeners = getPaintListeners();

		if(paintListeners.length > 0) {
			for(final PaintListener paintListener : paintListeners) {
				paintListener.prePaint();
			}
		}

		final long time = System.currentTimeMillis();

		final ByteBuffer tempData = getTempData();
		tempData.clear();

		final IntBuffer intBuffer = tempData.asIntBuffer();

		final int pictureWidth = getPictureWidth();
		final int pictureHeight = getPictureHeight();

		if(!scenePeer.getPixels(intBuffer, pictureWidth, pictureHeight)) {
			return;
		}

		paintPopupSnapper(intBuffer, pictureWidth, pictureHeight);

		tempData.flip();
		tempData.limit(pictureWidth * pictureHeight * 4);

		final AsynReadSynWriteLock imageLock = getImageLock();
		imageLock.synLock();
		try {

			final ByteBuffer fxData = getFxData();
			fxData.clear();
			fxData.put(tempData);
			fxData.flip();

			final Function<ByteBuffer, Void> reorderData = getReorderData();

			if(reorderData != null) {
				reorderData.apply(fxData);
				fxData.position(0);
			}

		} catch(final Exception exc) {
			exc.printStackTrace();
		} finally {
			imageLock.synUnlock();
		}

		final long diff = System.currentTimeMillis() - time;

		if(diff > 3) {
			// System.out.println("slow write FX frame(" + diff + ")");
		}

		if(paintListeners.length > 0) {
			for(final PaintListener paintListener : paintListeners) {
				paintListener.postPaint();
			}
		}

		final AtomicInteger waitCount = getWaitCount();
		waitCount.incrementAndGet();
	}

	public void paintPopupSnapper(final IntBuffer intBuffer, final int pictureWidth, final int pictureHeight) {

		final Array<PopupSnapper> activeSnappers = getActiveSnappers();

		if(!isFullScreenSuppport() || activeSnappers.isEmpty()) {
			return;
		}

		activeSnappers.readLock();
		try {

			for(final PopupSnapper popupSnapper : activeSnappers.array()) {

				if(popupSnapper == null) {
					break;
				}

				popupSnapper.paint(intBuffer, pictureWidth, pictureHeight);
			}

		} finally {
			activeSnappers.readUnlock();
		}
	}

	/**
	 * Удаление слушателя отрисовки.
	 */
	public void removePaintListener(final PaintListener paintListener) {

		final List<PaintListener> temp = new ArrayList<>();

		for(final PaintListener listener : getPaintListeners()) {
			temp.add(listener);
		}

		temp.remove(paintListener);

		setPaintListeners(temp.toArray(new PaintListener[temp.size()]));
	}

	int retrieveKeyState() {

		int embedModifiers = 0;

		if(keyStateSet.get(KeyEvent.VK_SHIFT)) {
			embedModifiers |= AbstractEvents.MODIFIER_SHIFT;
		}

		if(keyStateSet.get(KeyEvent.VK_CONTROL)) {
			embedModifiers |= AbstractEvents.MODIFIER_CONTROL;
		}

		if(keyStateSet.get(KeyEvent.VK_ALT)) {
			embedModifiers |= AbstractEvents.MODIFIER_ALT;
		}

		if(keyStateSet.get(KeyEvent.VK_META)) {
			embedModifiers |= AbstractEvents.MODIFIER_META;
		}

		return embedModifiers;
	}

	/**
	 * call via gui manager!
	 *
	 * @param rawInputListenerAdapter
	 */
	public void setEverListeningRawInputListener(final RawInputListener rawInputListenerAdapter) {
		this.inputListener.setEverListeningRawInputListener(rawInputListenerAdapter);
	}

	/**
	 * @param focus есть ли сейчас фокус на FX UI.
	 */
	public void setFocus(final boolean focus) {
		this.focus = focus;
	}

	void setFxEnabled(final boolean enabled) {
	}

	/**
	 * @param inputListener слушатель ввода пользователя.
	 */
	public void setInputListener(final JmeFXInputListener inputListener) {
		this.inputListener = inputListener;
	}

	/**
	 * @param oldX предыдущее положение экрана по X.
	 */
	public void setOldX(final int oldX) {
		this.oldX = oldX;
	}

	/**
	 * @param oldY предыдущее положение экрана по Y.
	 */
	public void setOldY(final int oldY) {
		this.oldY = oldY;
	}

	/**
	 * @param paintListeners набор слушателей отрисовки.
	 */
	private void setPaintListeners(final PaintListener[] paintListeners) {
		this.paintListeners = paintListeners;
	}

	/**
	 * @param pictureHeight высота картики для отрисовки UI.
	 */
	public void setPictureHeight(final int pictureHeight) {
		this.pictureHeight = pictureHeight;
	}

	/**
	 * @param pictureWidth ширина картики для отрисовки UI.
	 */
	public void setPictureWidth(final int pictureWidth) {
		this.pictureWidth = pictureWidth;
	}

	public void setScene(final Scene newScene, final Group highLevelGroup) {
		this.rootNode = highLevelGroup;
		FxPlatformExecutor.runOnFxApplication(() -> JmeFxContainer.this.setSceneImpl(newScene));
	}

	/*
	 * Called on JavaFX app thread.
	 */
	private void setSceneImpl(final Scene newScene) {
		if(this.stage != null && newScene == null) {
			this.stage.hide();
			this.stage = null;
		}

		this.app.enqueue(() -> {
			JmeFxContainer.this.picture.setCullHint(newScene == null ? CullHint.Always : CullHint.Never);
			return null;
		});

		this.scene = newScene;
		if(this.stage == null && newScene != null) {
			this.stage = new EmbeddedWindow(this.hostContainer);
		}
		if(this.stage != null) {
			this.stage.setScene(newScene);
			if(!this.stage.isShowing()) {
				this.stage.show();
			}
		}
	}

	/**
	 * @param scenePeer текущая сцена UI.
	 */
	public void setScenePeer(final EmbeddedSceneInterface scenePeer) {
		this.scenePeer = scenePeer;
	}

	/**
	 * @param stagePeer текущая стадия UI.
	 */
	public void setStagePeer(final EmbeddedStageInterface stagePeer) {
		this.stagePeer = stagePeer;
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
	public void setWindowOffsetY(final int windowOffsetY) {
		this.windowOffsetY = windowOffsetY;
	}

	/**
	 * Запись резульата FX UI на текстуру в JME.
	 */
	public void writeToJME() {

		final AtomicInteger waitCount = getWaitCount();
		final int currentCount = waitCount.get();

		final long time = System.currentTimeMillis();

		final ByteBuffer jmeData = getJmeData();
		jmeData.clear();

		final AsynReadSynWriteLock imageLock = getImageLock();
		imageLock.synLock();
		try {
			jmeData.put(getFxData());
		} finally {
			imageLock.synUnlock();
		}

		jmeData.flip();

		final long diff = System.currentTimeMillis() - time;

		if(diff > 3) {
			// System.out.println("slow write JME FX frame(" + diff + ")");
		}

		final Image jmeImage = getJmeImage();
		jmeImage.setUpdateNeeded();

		waitCount.subAndGet(currentCount);
	}
}