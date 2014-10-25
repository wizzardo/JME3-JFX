/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3x.jfx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3x.jfx.cursor.ICursorDisplayProvider;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;

/**
 * Fakes a top level window
 */
public class JmeFXHostInterfaceImpl implements HostInterface {
	
	private static final Logger logger = LoggerFactory.getLogger(JmeFXHostInterfaceImpl.class);

	/** контейнер JavaFX UI */
	private final JmeFxContainer jmeFxContainer;

	public JmeFXHostInterfaceImpl(final JmeFxContainer jmeFxContainer) {
		this.jmeFxContainer = jmeFxContainer;
	}
	
	/**
	 * @return контейнер JavaFX UI.
	 */
	private JmeFxContainer getJmeFxContainer() {
		return jmeFxContainer;
	}

	@Override
	public boolean grabFocus() {
		return true;
	}

	@Override
	public void repaint() {
		final JmeFxContainer jmeFxContainer = getJmeFxContainer();
		jmeFxContainer.paintComponent();
	}

	@Override
	public boolean requestFocus() {
		return true;
	}

	@Override
	public void setCursor(final CursorFrame cursorFrame) {
		
		final JmeFxContainer jmeFxContainer = getJmeFxContainer();
		final ICursorDisplayProvider cursorDisplayProvider = jmeFxContainer.getCursorDisplayProvider();
		
		if(cursorDisplayProvider != null) {
			cursorDisplayProvider.showCursor(cursorFrame);
		}
	}

	@Override
	public void setEmbeddedScene(final EmbeddedSceneInterface embeddedScene) {

		final JmeFxContainer jmeFxContainer = getJmeFxContainer();
		jmeFxContainer.setScenePeer(embeddedScene);

		if(embeddedScene == null) {
			return;
		}
		
		final int width = jmeFxContainer.getPictureWidth();
		final int heigh = jmeFxContainer.getPictureHeight();
		
		if(width > 0 && heigh > 0) {
			embeddedScene.setSize(jmeFxContainer.getPictureWidth(), jmeFxContainer.getPictureHeight());
		}
		
		embeddedScene.setDragStartListener(new JmeFxDNDHandler(jmeFxContainer));
	}

	@Override
	public void setEmbeddedStage(final EmbeddedStageInterface embeddedStage) {
		
		final JmeFxContainer jmeFxContainer = getJmeFxContainer();
		jmeFxContainer.setStagePeer(embeddedStage);
		
		if(embeddedStage == null) {
			return;
		}
		
		final int width = jmeFxContainer.getPictureWidth();
		final int heigh = jmeFxContainer.getPictureHeight();
		
		if(width > 0 && heigh > 0) {
			embeddedStage.setSize(jmeFxContainer.getPictureWidth(), jmeFxContainer.getPictureHeight());
		}

		embeddedStage.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		final JmeFxContainer jmeFxContainer = getJmeFxContainer();
		jmeFxContainer.setFxEnabled(enabled);
	}

	@Override
	public void setPreferredSize(final int width, final int height) {
	}

	@Override
	public boolean traverseFocusOut(final boolean forward) {
		logger.debug("Called traverseFocusOut({})", forward);
		return true;
	}

	@Override
	public void ungrabFocus() {
	}
}
