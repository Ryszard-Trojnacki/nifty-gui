package de.lessvoid.nifty.screen;

import de.lessvoid.nifty.Nifty;

/**
 * ScreenController Interface all screen controllers should support.
 * @author void
 */
public interface ScreenController {

  /**
   * Bind this ScreenController to a screen. This happens
   * when the Screen got the onStartScreen() method.
   * @param nifty nifty
   * @param screen screen
   */
  void bind(Nifty nifty, Screen screen);

  /**
   * called when all start effects are ended and the screen
   * is ready for interactive manipulation.
   */
  void onStartScreen();

  /**
   * called when the onEndScreen effects ended and this screen is done.
   */
  void onEndScreen();
}
