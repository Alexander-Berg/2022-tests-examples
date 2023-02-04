package ru.yandex.qe.util.io;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.util.io.FileModes.PERM_U_R;
import static ru.yandex.qe.util.io.FileModes.PERM_U_W;
import static ru.yandex.qe.util.io.FileModes.PERM_U_X;
import static ru.yandex.qe.util.io.FileModes.PERM_X;

/**
 * @author entropia
 */
public final class FileModesTest {
  /**
   * Flags for "regular file" in CPIO archive format. When CPIO decompressor doesn't know the file mode, it
   * returns this. Which is equivalent to "only root can do things to me".
   */
  private static final int C_ISREG = 0100000;

  @Test
  public void zero_mode_is_not_safe_file() {
    int actualMode = FileModes.safeMode(0, false);

    assertTrue((actualMode & PERM_U_R) == PERM_U_R, "file can be READ by the user");
    assertTrue((actualMode & PERM_U_W) == PERM_U_W, "file can be WRITTEN by the user");
  }

  @Test
  public void zero_mode_is_not_safe_dir() {
    int actualMode = FileModes.safeMode(0, true);

    assertTrue((actualMode & PERM_U_R) == PERM_U_R, "directory can be READ to by the user");
    assertTrue((actualMode & PERM_U_W) == PERM_U_W, "directory can be WRITTEN to by the user");
    assertTrue((actualMode & PERM_U_X) == PERM_U_X, "directory can be LISTED by the user");
  }

  @Test
  public void cpio_regular_file_is_not_safe() {
    int actualMode = FileModes.safeMode(C_ISREG, false);

    assertTrue((actualMode & PERM_U_R) == PERM_U_R, "regular file can be READ by the user");
    assertTrue((actualMode & PERM_U_W) == PERM_U_W, "regular file can be WRITTEN by the user");
  }

  @Test
  public void already_safe_modes_are_unchanged_file() {
    for (int mode : new int[]{0644, 0755, 0777, 0700, 0400, 0300, 0200, 0100}) {
      assertThat(Integer.toOctalString(mode) + " is safe",
          FileModes.safeMode(mode, false), is(equalTo(mode)));
    }
  }

  @Test
  public void execution_bit_is_set_for_safe_modes_dir() {
    for (int mode : new int[]{0644, 0755, 0777, 0700, 0400, 0300, 0200, 0100}) {
      assertThat(Integer.toOctalString(mode) + " is turned into " + mode + " | 0111",
          FileModes.safeMode(mode, true), is(equalTo(mode | PERM_X)));
    }
  }

  @Test
  public void posix_file_permissions() {
    final Set<PosixFilePermission> actualPerms = FileModes.posixPermissionsFromMode(0754);
    assertThat(actualPerms, containsInAnyOrder(
        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_READ
    ));
  }
}
