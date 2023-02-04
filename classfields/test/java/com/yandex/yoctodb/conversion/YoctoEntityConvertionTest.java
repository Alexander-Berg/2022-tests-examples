package com.yandex.yoctodb.conversion;

import static com.yandex.yoctodb.query.QueryBuilder.*;

import com.yandex.yoctodb.DatabaseFormat;
import com.yandex.yoctodb.conversion.util.SimpleClassForConversion;
import com.yandex.yoctodb.immutable.Database;
import com.yandex.yoctodb.mutable.DatabaseBuilder;
import com.yandex.yoctodb.mutable.DocumentBuilder;
import com.yandex.yoctodb.query.Query;
import com.yandex.yoctodb.util.UnsignedByteArrays;
import com.yandex.yoctodb.util.buf.Buffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

/** @author svyatoslav */
public class YoctoEntityConvertionTest {

  private static final int ELEMENTS_COUNT = 100;

  @Test
  public void test() throws IOException {
    YoctoEntityConverter<SimpleClassForConversion> entityConverter =
        new YoctoEntityConverter<SimpleClassForConversion>(SimpleClassForConversion.class);
    List<SimpleClassForConversion> simpleClassForConversions =
        new ArrayList<SimpleClassForConversion>();
    for (int i = 0; i < ELEMENTS_COUNT; i++) {
      Integer[] ints = new Integer[] {1, 2, 3, 4, 5};
      List<String> stringList = new ArrayList<String>();
      stringList.add("str1");
      stringList.add("str2");
      stringList.add("str3");
      List<Integer> intsList = Arrays.asList(9, 10, 11);
      Map<String, Long> map =
          new HashMap<String, Long>() {
            {
              put("lol", 1000L);
              put("lool", 2000L);
            }
          };
      List<SimpleClassForConversion.Type> types =
          Arrays.asList(SimpleClassForConversion.Type.TYPE1, SimpleClassForConversion.Type.TYPE2);
      SimpleClassForConversion simpleClassForConversion =
          new SimpleClassForConversion(
              i, i, "some string " + (i * i), i % 2 == 0, ints, stringList, intsList, map, types);
      simpleClassForConversions.add(simpleClassForConversion);
    }
    final DatabaseBuilder dbBuilder = DatabaseFormat.getCurrent().newDatabaseBuilder();

    for (SimpleClassForConversion simpleClassForConversion : simpleClassForConversions) {
      DocumentBuilder documentBuilder = entityConverter.toDocumentBuilder(simpleClassForConversion);
      dbBuilder.merge(documentBuilder);
    }

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    dbBuilder.buildWritable().writeTo(os);
    final Database db =
        DatabaseFormat.getCurrent().getDatabaseReader().from(Buffer.from(os.toByteArray()));
    final Query booleanFieldTestQuery =
        select().where(eq("boolean_field", UnsignedByteArrays.from(true)));
    final Query intFieldTestQuery = select().where(gte("int_field", UnsignedByteArrays.from(0)));
    final Query intsArrayFieldTestQuery =
        select().where(eq("ints_array_field", UnsignedByteArrays.from(3)));
    final Query stringListFieldTestQuery =
        select().where(eq("string_array_field", UnsignedByteArrays.from("str2")));
    final Query intsListFieldTestQuery =
        select().where(eq("ints_list_field", UnsignedByteArrays.from(11)));
    final Query enumListFieldTestQuery =
        select()
            .where(
                in(
                    "enum_list_field",
                    UnsignedByteArrays.from(SimpleClassForConversion.Type.TYPE2.name())));
    Assert.assertEquals(ELEMENTS_COUNT / 2, db.count(booleanFieldTestQuery));
    Assert.assertEquals(ELEMENTS_COUNT, db.count(intFieldTestQuery));
    Assert.assertEquals(ELEMENTS_COUNT, db.count(intsArrayFieldTestQuery));
    Assert.assertEquals(ELEMENTS_COUNT, db.count(stringListFieldTestQuery));
    Assert.assertEquals(ELEMENTS_COUNT, db.count(intsListFieldTestQuery));
    Assert.assertEquals(ELEMENTS_COUNT, db.count(enumListFieldTestQuery));
  }
}
