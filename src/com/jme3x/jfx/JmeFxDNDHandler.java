package com.jme3x.jfx;

import com.jme3x.jfx.util.JFXDNDUtils;
import com.jme3x.jfx.util.JFXEmbeddedUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.TransferMode;
import rlib.logging.Logger;
import rlib.logging.LoggerManager;

import static rlib.util.array.ArrayFactory.toArray;

/**
 * Реализация слушателя DND для обработки его в случае встраивания в jME.
 *
 * @author Ronn
 */
public class JmeFxDNDHandler implements InvocationHandler {

    private static final Logger LOGGER = LoggerManager.getLogger(InvocationHandler.class);

    public static final Class<?> LISTENER_TYPE;

    static {
        try {
            LISTENER_TYPE = Class.forName("com.sun.javafx.embed.HostDragStartListener");
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Контейнер.
     */
    private final JmeFxContainer jmeFxContainer;

    /**
     * Реализация слушателя DnD.
     */
    private final Object handler;

    private Object dropTarget;
    private Object dragSource;

    private TransferMode overTarget;

    /**
     * Изображение для отображения перемещения элемента.
     */
    private ImageView dragImage;

    public JmeFxDNDHandler(final JmeFxContainer jmeFxContainer) {
        this.jmeFxContainer = jmeFxContainer;
        this.handler = Proxy.newProxyInstance(LISTENER_TYPE.getClassLoader(), toArray(LISTENER_TYPE), this);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        dragStarted(args[0], (TransferMode) args[1]);
        return null;
    }

    /**
     * @return реализация слушателя DnD.
     */
    public Object getHandler() {
        return handler;
    }

    /**
     * this is kinda ridiculous, but well at least it seems to work
     */
    private void createDragImageProxy(final Object jmeJfxDragImage, final Object offset) {
        if (!(jmeJfxDragImage instanceof ByteBuffer)) return;

        try {

            final ByteBuffer casted = (ByteBuffer) jmeJfxDragImage;
            casted.position(0);

            final int width = casted.getInt();
            final int height = casted.getInt();

            final byte[] imgdata = new byte[casted.remaining()];
            casted.get(imgdata);

            final WritableImage img = new WritableImage(width, height);
            final PixelWriter writer = img.getPixelWriter();
            writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), imgdata, 0, width * 4);

            final ImageView dragImage = new ImageView(img);
            dragImage.setStyle("dragimage:true;");
            dragImage.setMouseTransparent(true);
            dragImage.setVisible(true);

            if (offset instanceof ByteBuffer) {

                ((ByteBuffer) offset).position(0);

                final int x = ((ByteBuffer) offset).getInt();
                final int y = ((ByteBuffer) offset).getInt();

                if (LOGGER.isEnabledDebug()) LOGGER.debug("Img offset " + x + ", " + y);
            }

        } catch (final Exception e) {
            LOGGER.warning(e.getMessage(), e);
        }
    }

    /**
     * Инициализациястарта перемещения какого-то элемента.
     */
    private void dragStarted(final Object dragSource, final TransferMode dragAction) {

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final JmeFxInputListener inputListener = jmeFxContainer.getInputListener();
        final JmeFxPanel hostContainer = jmeFxContainer.getHostContainer();
        final Scene scene = jmeFxContainer.getScene();
        final Group rootNode = (Group) scene.getRoot();
        final ObservableList<Node> children = rootNode.getChildren();

        final ImageView dragImage = getDragImage();

        if (dragImage != null) {
            children.remove(dragImage);
            setDragImage(null);
        }

        try {

            final Object dragImg = JFXDNDUtils.getData(dragSource, "application/x-java-drag-image");
            final Object offset = JFXDNDUtils.getData(dragSource, "application/x-java-drag-image-offset");

            if (dragImg != null) createDragImageProxy(dragImg, offset);
            inputListener.setMouseDNDListener(this);

            final Object dropTarget = JFXEmbeddedUtils.createDropTarget(hostContainer);

            setDragSource(dragSource);
            setDropTarget(dropTarget);

            // pseudo enter, we only support inner events, so it stays always entered
            JFXDNDUtils.handleDragEnter(dropTarget, 0, 0, TransferMode.COPY, dragSource);

        } catch (final Exception e) {
            LOGGER.warning(e.getMessage(), e);
        }
    }

    private void setDragSource(final Object dragSource) {
        this.dragSource = dragSource;
    }

    private Object getDragSource() {
        return dragSource;
    }

    private void setDropTarget(final Object dropTarget) {
        this.dropTarget = dropTarget;
    }

    private Object getDropTarget() {
        return dropTarget;
    }

    private void setOverTarget(final TransferMode overTarget) {
        this.overTarget = overTarget;
    }

    private TransferMode getOverTarget() {
        return overTarget;
    }

    /**
     * @param dragImage изображение для отображения перемещения элемента.
     */
    private void setDragImage(final ImageView dragImage) {
        this.dragImage = dragImage;
    }

    /**
     * @return изображение для отображения перемещения элемента.
     */
    private ImageView getDragImage() {
        return dragImage;
    }

    private JmeFxContainer getJmeFxContainer() {
        return jmeFxContainer;
    }

    /**
     * Обновление положения перемещаемого элемента в JavaFX.
     *
     * @param x            координата перемещаемого элемента.
     * @param y            координата перемещаемого элемента.
     * @param mousePressed зажата ли кнопка мыши.
     */
    public void mouseUpdate(final int x, final int y, final boolean mousePressed) {

        final Object dropTarget = getDropTarget();
        final Object dragSource = getDragSource();

        if (dragSource == null || dropTarget == null) return;

        final JmeFxContainer container = getJmeFxContainer();
        final JmeFxInputListener inputListener = container.getInputListener();
        final Scene scene = container.getScene();
        final Group rootNode = (Group) scene.getRoot();
        final ObservableList<Node> children = rootNode.getChildren();

        final ImageView dragImage = getDragImage();

        try {

            if (mousePressed) {

                if (dragImage != null) {
                    dragImage.relocate(x, y);
                    if (!children.contains(dragImage)) children.add(dragImage);
                }

                setOverTarget(JFXDNDUtils.handleDragOver(dropTarget, x, y, TransferMode.COPY));

            } else {

                if (dragImage != null) dragImage.setVisible(false);

                final TransferMode overTarget = getOverTarget();

                if (overTarget != null) {

                    // causes exceptions when done without a target
                    setOverTarget(JFXDNDUtils.handleDragOver(dropTarget, x, y, TransferMode.COPY));

                    final TransferMode acceptedMode = JFXDNDUtils.handleDragDrop(dropTarget, x, y, TransferMode.COPY);

                    // Necessary to reset final the internal states, and allow final another drag drop
                    JFXDNDUtils.dragDropEnd(dragSource, acceptedMode);

                } else {
                    // // seems to be necessary if no dragdrop attempt is being made
                    JFXDNDUtils.handleDragLeave(dropTarget);
                    JFXDNDUtils.dragDropEnd(dragSource, null);
                }

                inputListener.setMouseDNDListener(null);

                setDragSource(null);
                setOverTarget(null);
            }

        } catch (final Exception e) {
            LOGGER.warning(e.getMessage(), e);
        }
    }
}
