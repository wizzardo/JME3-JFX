package com.jme3x.jfx.listener;

/**
 * Реализация слушателя отрисовки интерфейса.
 * 
 * @author Ronn
 */
public interface PaintListener {

	/**
	 * Обработка действий перед отрисовкой.
	 */
	public void prePaint();
	
	/**
	 * Обработка действий после отрисовки.
	 */
	public void postPaint();
}
