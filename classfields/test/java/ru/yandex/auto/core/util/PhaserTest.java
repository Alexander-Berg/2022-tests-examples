package ru.yandex.auto.core.util;

import static org.junit.Assert.*;

import java.util.concurrent.Phaser;
import org.junit.Test;

/**
 * Simple tests for {@link java.util.concurrent.Phaser}
 *
 * @author incubos
 */
public class PhaserTest {
  @Test
  public void testTieredWithoutWaiting() {
    final Phaser root = new Phaser(1);
    final Phaser child = new Phaser(root);

    assertEquals(0, child.register());
    assertEquals(0, child.arriveAndDeregister());

    assertEquals(0, root.arriveAndDeregister());
    assertTrue(root.awaitAdvance(0) < 0);

    assertTrue(child.register() < 0);
  }

  @Test
  public void testSimple() {
    final Phaser child = new Phaser(1);

    assertEquals(0, child.register());
    assertEquals(0, child.arriveAndDeregister());

    assertEquals(0, child.register());
    assertEquals(0, child.arriveAndDeregister());

    assertEquals(0, child.arriveAndDeregister());

    assertTrue(child.register() < 0);
  }
}
