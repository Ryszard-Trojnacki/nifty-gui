package de.lessvoid.nifty.render;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.lessvoid.nifty.spi.input.InputSystem;
import de.lessvoid.nifty.spi.render.MouseCursor;
import de.lessvoid.nifty.spi.render.RenderDevice;

public class NiftyMouseImplSetPositionTest {
  private NiftyMouseImpl niftyMouse;
  private InputSystem inputSystemMock;

  @Before
  public void before() {
    inputSystemMock = createMock(InputSystem.class);
    niftyMouse = new NiftyMouseImpl(null, inputSystemMock);
  }

  @After
  public void verifyMock() {
    verify(inputSystemMock);
  }

  @Test
  public void testSetPosition() {
    inputSystemMock.setMousePosition(100, 200);
    replay(inputSystemMock);

    niftyMouse.setMousePosition(100, 200);
  }
}
