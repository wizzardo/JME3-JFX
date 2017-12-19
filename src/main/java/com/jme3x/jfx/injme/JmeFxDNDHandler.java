package com.jme3x.jfx.injme;

import com.jme3x.jfx.injme.input.JmeFXInputListener;
import com.ss.rlib.logging.Logger;
import com.ss.rlib.logging.LoggerManager;
import com.sun.javafx.embed.EmbeddedSceneDSInterface;
import com.sun.javafx.embed.EmbeddedSceneDTInterface;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.HostDragStartListener;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.TransferMode;

import java.nio.ByteBuffer;

/**
 * A very hacky implementation of a DND system, similar to SwingDND but for jme context. <br> Allows
 * for inner application drag and drop support. <br> Cross GuiManager support is untested.
 *
 * @author empire
 */
public class JmeFxDNDHandler implements HostDragStartListener {

    private static final Logger LOGGER = LoggerManager.getLogger(JmeFxDNDHandler.class);

    private JmeFxContainer jmeFxContainer;
    private EmbeddedSceneDTInterface dropTarget;

    // mouse event stuff
    private EmbeddedSceneDSInterface dragSource;
    private TransferMode overTarget;
    private ImageView dragImage;

    public JmeFxDNDHandler(final JmeFxContainer jmeFxContainer) {
        this.jmeFxContainer = jmeFxContainer;
    }

    /**
     * this is kinda ridiculous, but well at least it seems to work
     */
    private void createDragImageProxy(final Object jmeJfxDragImage, final Object offset) {

        if (!(jmeJfxDragImage instanceof ByteBuffer)) {
            return;
        }

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

            dragImage = new ImageView(img);
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

    @Override
    public void dragStarted(final EmbeddedSceneDSInterface dragSource, final TransferMode dragAction) {

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final JmeFXInputListener inputListener = jmeFxContainer.getInputListener();
        final EmbeddedSceneInterface scenePeer = jmeFxContainer.getScenePeer();
        final Group rootNode = jmeFxContainer.getRootNode();
        final ObservableList<Node> children = rootNode.getChildren();

        if (dragImage != null) {
            children.remove(dragImage);
            dragImage = null;
        }

        try {

            final Object dragImg = dragSource.getData("application/x-java-drag-image");
            final Object offset = dragSource.getData("application/x-java-drag-image-offset");

            if (dragImg != null) {
                createDragImageProxy(dragImg, offset);
            }

            inputListener.setMouseDNDListener(this);

            assert dragAction == TransferMode.COPY : "Only Copy is supported currently";

            if (LOGGER.isEnabledDebug()) {
                LOGGER.debug("Drag started of " + dragSource + " in mode " + dragAction);
            }

            final Clipboard clip = Clipboard.getSystemClipboard();

            if (LOGGER.isEnabledDebug()) {
                LOGGER.debug("clip : " + clip);
            }

            assert this.dragSource == null;
            assert this.dropTarget == null;

            this.dragSource = dragSource;
            this.dropTarget = scenePeer.createDropTarget();
            // pseudo enter, we only support inner events, so it stays always entered
            this.dropTarget.handleDragEnter(0, 0, 0, 0, TransferMode.COPY, dragSource);

        } catch (final Exception e) {
            LOGGER.warning(e.getMessage(), e);
        }
    }

    public ImageView getDragImage() {
        return dragImage;
    }

    public JmeFxContainer getJmeFxContainer() {
        return jmeFxContainer;
    }

    public void mouseUpdate(final int x, final int y, final boolean mousePressed) {

        if (this.dragSource == null || this.dropTarget == null) {
            return;
        }

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final JmeFXInputListener inputListener = jmeFxContainer.getInputListener();
        final Group rootNode = jmeFxContainer.getRootNode();
        final ObservableList<Node> children = rootNode.getChildren();

        final ImageView dragImage = getDragImage();

        try {

            if (mousePressed) {

                if (dragImage != null) {
                    dragImage.relocate(x, y);

                    // only add once it has a valid position
                    if (!children.contains(dragImage)) {
                        children.add(dragImage);
                    }
                }

                this.overTarget = this.dropTarget.handleDragOver(x, y, x, y, TransferMode.COPY);

            } else {

                if (dragImage != null) {
                    dragImage.setVisible(false);
                }

                if (LOGGER.isEnabledDebug()) {
                    LOGGER.debug("Drag released!");
                }

                if (this.overTarget != null) {

                    // // causes exceptions when done without a target
                    this.overTarget = this.dropTarget.handleDragOver(x, y, x, y, TransferMode.COPY);
                    final TransferMode acceptedMode = this.dropTarget.handleDragDrop(x, y, x, y, TransferMode.COPY);
                    // // Necessary to reset final the internal states, and allow final another drag drop
                    this.dragSource.dragDropEnd(acceptedMode);

                } else {

                    if (LOGGER.isEnabledDebug()) {
                        LOGGER.debug("invalid drag target");
                    }

                    // // seems to be necessary if no dragdrop attempt is being made
                    this.dropTarget.handleDragLeave();
                    this.dragSource.dragDropEnd(null);
                }

                inputListener.setMouseDNDListener(null);

                this.dragSource = null;
                this.dropTarget = null;
            }

        } catch (final Exception e) {
            LOGGER.warning(e.getMessage(), e);
        }
    }
}