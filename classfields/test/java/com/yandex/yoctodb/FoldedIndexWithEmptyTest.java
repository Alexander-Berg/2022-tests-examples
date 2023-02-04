/*
 * (C) YANDEX LLC, 2014-2019
 *
 * The Source Code called "YoctoDB" available at
 * https://github.com/yandex/yoctodb is subject to the terms of the
 * Mozilla Public License, v. 2.0 (hereinafter referred to as the "License").
 *
 * A copy of the License is also available at http://mozilla.org/MPL/2.0/.
 */

package com.yandex.yoctodb;

import static com.yandex.yoctodb.mutable.DocumentBuilder.IndexOption.RANGE_FILTERABLE;
import static com.yandex.yoctodb.mutable.DocumentBuilder.IndexOption.STORED;
import static org.junit.Assert.assertEquals;

import com.yandex.yoctodb.immutable.Database;
import com.yandex.yoctodb.mutable.DatabaseBuilder;
import com.yandex.yoctodb.util.UnsignedByteArray;
import com.yandex.yoctodb.util.UnsignedByteArrays;
import com.yandex.yoctodb.util.buf.Buffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Test;

/**
 * {@link com.yandex.yoctodb.util.immutable.ByteArrayIndexedList} with fixed size elements
 *
 * @author irenkamalova
 */
public class FoldedIndexWithEmptyTest {

  @Test
  public void buildDatabase() throws IOException {
    final DatabaseBuilder dbBuilder = DatabaseFormat.getCurrent().newDatabaseBuilder();

    dbBuilder.merge(
        DatabaseFormat.getCurrent().newDocumentBuilder().withField("1", "1", RANGE_FILTERABLE));

    // Document 1, docId = 0
    dbBuilder.merge(
        DatabaseFormat.getCurrent().newDocumentBuilder().withField("state", "NEW", STORED));

    // for docId = 1 there is no value for field state:
    dbBuilder.merge(
        DatabaseFormat.getCurrent().newDocumentBuilder().withField("region", "1", STORED));

    // for docId = 2
    dbBuilder.merge(
        DatabaseFormat.getCurrent().newDocumentBuilder().withField("state", "NEW", STORED));

    dbBuilder.merge(
        DatabaseFormat.getCurrent().newDocumentBuilder().withField("region", "1", STORED));

    dbBuilder.merge(
        DatabaseFormat.getCurrent().newDocumentBuilder().withField("state", "USED", STORED));

    dbBuilder.merge(
        DatabaseFormat.getCurrent().newDocumentBuilder().withField("1", "2", RANGE_FILTERABLE));

    dbBuilder.merge(
        DatabaseFormat.getCurrent().newDocumentBuilder().withField("region", "2", STORED));

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    dbBuilder.buildWritable().writeTo(os);

    final Database db =
        DatabaseFormat.getCurrent().getDatabaseReader().from(Buffer.from(os.toByteArray()));

    assertEquals("NEW", getValueFromBuffer(db.getFieldValue(1, "state")));
    assertEquals("1", getValueFromBuffer(db.getFieldValue(2, "region")));
    assertEquals("NEW", getValueFromBuffer(db.getFieldValue(3, "state")));
    assertEquals("1", getValueFromBuffer(db.getFieldValue(4, "region")));
    assertEquals("USED", getValueFromBuffer(db.getFieldValue(5, "state")));
    assertEquals("2", getValueFromBuffer(db.getFieldValue(7, "region")));
  }

  private String getValueFromBuffer(Buffer buffer) {
    UnsignedByteArray byteArray = UnsignedByteArrays.from(buffer);
    return UnsignedByteArrays.toString(byteArray);
  }
}
