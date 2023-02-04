/*
 * Copyright (c) 2006, Stephen Kelvin Friedrich,  All rights reserved.
 *
 * This a BSD license. If you use or enhance the code, I'd be pleased if you sent a mail to s.friedrich@eekboom.com
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
         following disclaimer in the documentation and/or other materials provided with the distribution.
 *     * Neither the name of the "Stephen Kelvin Friedrich" nor the names of its contributors may be used to endorse
 *       or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ru.yandex.qe.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @see <a href="http://www.eekboom.com/java/compareNatural/src_test/com/eekboom/utils/TestStrings.java">Original code</a>
 */
public final class StringOrderingTest {
  @Test
  public void compare_natural() {
    final Collator c = Collator.getInstance(Locale.US);
    assertEquals(c(c, "", ""), 0);
    assertEquals(c(c, "1-02", "1-2"), 1);
    assertEquals(c(c, "1-2", "1-02"), -1);
    assertEquals(c(c, "catch 22", "catch 022"), -1);
    assertEquals(c(c, "a", "a"), 0);
    assertEquals(c(c, "2a", "2a2"), -1);
    assertEquals(c(c, "b", "a"), 1);
    assertEquals(c(c, "a", "b"), -1);
    assertEquals(c(c, "002", "11"), -1);
    assertEquals(c(c, "2", "11"), -1);
    assertEquals(c(c, "22", "11"), 1);
    assertEquals(c(c, "222", "99"), 1);
    assertEquals(c(c, "a 2", "a 11"), -1);
    assertEquals(c(c, "c23", "c111"), -1);
    assertEquals(c(c, "a2", "aa2"), -1);
    assertEquals(c(c, "a 22", "a 2"), 1);
    assertEquals(c(c, "a", "A"), -1);
    assertEquals(c(c, "a 2 h", "a 2 h 2"), -1);
    assertEquals(c(c, "abcd 234 huj", "abcd 234 huj 2"), -1);
    assertEquals(c(c, "abcd 234 huj", "abcd 234 huj"), 0);
    assertEquals(c(c, "abcd 234 huj 33", "abcd 234 huj 9"), 1);
  }

  @Test
  public void compare_natural_with_whitespace() {
    String[] strings = { "p4", "p  3" };
    List<String> sortedStrings = Arrays.asList(strings);
    List<String> testStrings = new ArrayList<>(sortedStrings);
    for(int i = 0; i < 10; ++i) {
      Collections.shuffle(testStrings);
      Collections.sort(testStrings, StringOrdering.getNaturalComparator(Collator.getInstance(Locale.US)));
      assertEquals(sortedStrings, testStrings);
    }
  }

  @Test
  public void compare_natural_ignore_case() {
    Collator c = Collator.getInstance(Locale.US);

    assertEquals(ci(c, "a", "a"), 0);
    assertEquals(ci(c, "b", "a"), 1);
    assertEquals(ci(c, "A", "a"), 0);
    assertEquals(ci(c, "A12", "a12"), 0);
    assertEquals(ci(c, "A12 11", "a12 9"), 1);
    assertEquals(ci(c, "catch 22", "cAtCh 022"), -1);
    assertEquals(ci(c, "pic 5", "pic 4 else"), 1);
    assertEquals(ci(c, "p 5 s", "p 5"), 1);
    assertEquals(ci(c, "p 5", "p 5 s"), -1);
  }

  @Test
  public void compare_natural_with_collator() {
    Collator c = Collator.getInstance(Locale.GERMANY);
    c.setStrength(Collator.SECONDARY);

    assertEquals(c(c, "a", "a"), 0);
    assertEquals(c(c, "a", "A"), 0);
    assertEquals(c(c, "ä", "a"), 1);
    assertEquals(c(c, "B", "a"), 1);
  }

  private int c(@Nonnull Collator c, @Nonnull String a, @Nonnull String b) {
    int result = StringOrdering.compareNatural(c, a, b);
    result = result < 0 ? -1 : result > 0 ? 1 : 0;
    return result;
  }

  private int ci(@Nonnull Collator c, @Nonnull String a, @Nonnull String b) {
    c.setStrength(Collator.PRIMARY);

    int result = StringOrdering.compareNatural(c, a, b);
    result = result < 0 ? -1 : result > 0 ? 1 : 0;
    return result;
  }

  @Test
  public void list_sort() {
    String[] strings = new String[]{"1-2", "1-02", "1-20", "10-20", "fred", "jane",
        "pic01", "pic2", "pic02", "pic02a", "pic3", "pic4", "pic 4 else", "pic 5", "pic 5", "pic05",
        "pic 5 something", "pic 6", "pic   7", "pic100", "pic100a", "pic120", "pic121",
        "pic02000", "tom", "x2-g8", "x2-y7", "x2-y08", "x8-y8"};

    List<String> expectedSorted = new ArrayList<>(Arrays.asList(strings));
    List<String> actualSorted = new ArrayList<>(Arrays.asList(strings));

    final Collator c = Collator.getInstance(Locale.US);
    c.setStrength(Collator.PRIMARY);

    for(int i = 0; i < 1000; ++i) {
      Collections.shuffle(actualSorted);
      Collections.sort(actualSorted, StringOrdering.getNaturalComparator(c));

      assertEquals(actualSorted, expectedSorted);
    }
  }
}
