package ru.yandex.auto.core.unification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** @author Anton Irinev (airinev@yandex-team.ru) */
public class TrimmerTest {

  @Test
  public void removeNbsps() {
    String result = Trimmer.trim("abc&#33;def&nbsp;ghi");
    assertEquals("abc def ghi", result);
  }

  @Test
  public void removeTags() {
    String result = Trimmer.trim("abc<a> def<b/> <c>ghi<c/>");
    assertEquals("abc def ghi", result);
  }

  @Test
  public void removeQuotations() {
    String result = Trimmer.trim("abc &quot;def&quot;");
    assertEquals("abc def ", result);
  }

  @Test
  public void removeDoubleSpace() {
    String result = Trimmer.trim("abc  def");
    assertEquals("abc def", result);
  }

  @Test
  public void removePunctuationsFromBeginningAndFromTheEnd() {
    String result = Trimmer.trim(";'\"-+=:;*?!abc;'\"-+=:;*?!");
    assertEquals("abc", result);
  }
}
