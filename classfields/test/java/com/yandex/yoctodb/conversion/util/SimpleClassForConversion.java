package com.yandex.yoctodb.conversion.util;

import com.yandex.yoctodb.conversion.YoctoConvertible;
import com.yandex.yoctodb.conversion.annotation.YoctoField;
import com.yandex.yoctodb.mutable.DocumentBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/** @author svyatoslav */
public class SimpleClassForConversion implements YoctoConvertible {
  @YoctoField(
      name = "long_field",
      indexOption = DocumentBuilder.IndexOption.FULL,
      indexType = DocumentBuilder.IndexType.FIXED_LENGTH)
  private long longField;

  @YoctoField(
      name = "int_field",
      indexOption = DocumentBuilder.IndexOption.FULL,
      indexType = DocumentBuilder.IndexType.FIXED_LENGTH)
  private long intField;

  @YoctoField(name = "string_field", indexType = DocumentBuilder.IndexType.VARIABLE_LENGTH)
  private String stringField;

  @YoctoField(name = "boolean_field")
  private boolean booleanField;

  @YoctoField(name = "ints_array_field", indexOption = DocumentBuilder.IndexOption.RANGE_FILTERABLE)
  private Integer[] intsArrayField;

  @YoctoField(
      name = "string_array_field",
      indexOption = DocumentBuilder.IndexOption.RANGE_FILTERABLE)
  private List<String> stringListField;

  @YoctoField(name = "ints_list_field", componentType = int.class)
  private List<Integer> intsList;

  @YoctoField(name = "map_field", componentType = Long.class)
  private Map<String, Long> map;

  @YoctoField(name = "enum_list_field", componentType = Enum.class)
  private List<Type> types;

  public SimpleClassForConversion() {}

  public SimpleClassForConversion(
      long longField,
      long intField,
      String stringField,
      boolean booleanField,
      Integer[] intsArrayField,
      List<String> stringListField,
      List<Integer> intsList,
      Map<String, Long> map,
      List<Type> types) {
    this.longField = longField;
    this.intField = intField;
    this.stringField = stringField;
    this.booleanField = booleanField;
    this.intsArrayField = intsArrayField;
    this.stringListField = stringListField;
    this.intsList = intsList;
    this.map = map;
    this.types = types;
  }

  @NotNull
  @Override
  public byte[] buildPayload() {
    return toString().getBytes();
  }

  public long getLongField() {
    return longField;
  }

  public void setLongField(long longField) {
    this.longField = longField;
  }

  public long getIntField() {
    return intField;
  }

  public void setIntField(long intField) {
    this.intField = intField;
  }

  public String getStringField() {
    return stringField;
  }

  public void setStringField(String stringField) {
    this.stringField = stringField;
  }

  public boolean isBooleanField() {
    return booleanField;
  }

  public void setBooleanField(boolean booleanField) {
    this.booleanField = booleanField;
  }

  public Integer[] getIntsArrayField() {
    return intsArrayField;
  }

  public void setIntsArrayField(Integer[] intsArrayField) {
    this.intsArrayField = intsArrayField;
  }

  public List<String> getStringListField() {
    return stringListField;
  }

  public void setStringListField(List<String> stringListField) {
    this.stringListField = stringListField;
  }

  public List<Integer> getIntsList() {
    return intsList;
  }

  public void setIntsList(List<Integer> intsList) {
    this.intsList = intsList;
  }

  public Map<String, Long> getMap() {
    return map;
  }

  public void setMap(Map<String, Long> map) {
    this.map = map;
  }

  public List<Type> getTypes() {
    return types;
  }

  public void setTypes(List<Type> types) {
    this.types = types;
  }

  @Override
  public String toString() {
    return "SimpleClassForConversion{"
        + "longField="
        + longField
        + ", intField="
        + intField
        + ", stringField='"
        + stringField
        + '\''
        + ", booleanField="
        + booleanField
        + ", intsArrayField="
        + Arrays.toString(intsArrayField)
        + ", stringListField="
        + stringListField
        + ", intsList="
        + intsList
        + ", map="
        + map
        + ", types="
        + types
        + '}';
  }

  public enum Type {
    TYPE1,
    TYPE2,
    TYPE3
  }
}
