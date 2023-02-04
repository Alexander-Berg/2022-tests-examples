package ru.auto.salesman.test

import java.io.{InputStream, Reader}
import java.net.URL
import java.sql._
import java.util
import java.util.Calendar

class DummyResultSet extends ResultSet {
  def next(): Boolean = ???
  def close(): Unit = ???
  def wasNull(): Boolean = ???
  def getString(columnIndex: Int): String = ???
  def getBoolean(columnIndex: Int): Boolean = ???
  def getByte(columnIndex: Int): Byte = ???
  def getShort(columnIndex: Int): Short = ???
  def getInt(columnIndex: Int): Int = ???
  def getLong(columnIndex: Int): Long = ???
  def getFloat(columnIndex: Int): Float = ???
  def getDouble(columnIndex: Int): Double = ???

  def getBigDecimal(columnIndex: Int, scale: Int): java.math.BigDecimal = ???
  def getBytes(columnIndex: Int): scala.Array[Byte] = ???
  def getDate(columnIndex: Int): Date = ???
  def getTime(columnIndex: Int): Time = ???
  def getTimestamp(columnIndex: Int): Timestamp = ???

  def getAsciiStream(columnIndex: Int): InputStream =
    ???
  def getUnicodeStream(columnIndex: Int): InputStream = ???

  def getBinaryStream(columnIndex: Int): InputStream =
    ???
  def getString(columnLabel: String): String = ???
  def getBoolean(columnLabel: String): Boolean = ???
  def getByte(columnLabel: String): Byte = ???
  def getShort(columnLabel: String): Short = ???
  def getInt(columnLabel: String): Int = ???
  def getLong(columnLabel: String): Long = ???
  def getFloat(columnLabel: String): Float = ???
  def getDouble(columnLabel: String): Double = ???

  def getBigDecimal(columnLabel: String, scale: Int): java.math.BigDecimal = ???

  def getBytes(columnLabel: String): scala.Array[Byte] =
    ???
  def getDate(columnLabel: String): Date = ???
  def getTime(columnLabel: String): Time = ???
  def getTimestamp(columnLabel: String): Timestamp = ???
  def getAsciiStream(columnLabel: String): InputStream = ???
  def getUnicodeStream(columnLabel: String): InputStream = ???
  def getBinaryStream(columnLabel: String): InputStream = ???
  def getWarnings: SQLWarning = ???
  def clearWarnings(): Unit = ???
  def getCursorName: String = ???
  def getMetaData: ResultSetMetaData = ???
  def getObject(columnIndex: Int): AnyRef = ???
  def getObject(columnLabel: String): AnyRef = ???
  def findColumn(columnLabel: String): Int = ???

  def getCharacterStream(columnIndex: Int): Reader =
    ???
  def getCharacterStream(columnLabel: String): Reader = ???

  def getBigDecimal(columnIndex: Int): java.math.BigDecimal =
    ???
  def getBigDecimal(columnLabel: String): java.math.BigDecimal = ???
  def isBeforeFirst: Boolean = ???
  def isAfterLast: Boolean = ???
  def isFirst: Boolean = ???
  def isLast: Boolean = ???
  def beforeFirst(): Unit = ???
  def afterLast(): Unit = ???
  def first(): Boolean = ???
  def last(): Boolean = ???
  def getRow: Int = ???
  def absolute(row: Int): Boolean = ???
  def relative(rows: Int): Boolean = ???
  def previous(): Boolean = ???
  def setFetchDirection(direction: Int): Unit = ???
  def getFetchDirection: Int = ???
  def setFetchSize(rows: Int): Unit = ???
  def getFetchSize: Int = ???
  def getType: Int = ???
  def getConcurrency: Int = ???
  def rowUpdated(): Boolean = ???
  def rowInserted(): Boolean = ???
  def rowDeleted(): Boolean = ???
  def updateNull(columnIndex: Int): Unit = ???
  def updateBoolean(columnIndex: Int, x: Boolean): Unit = ???
  def updateByte(columnIndex: Int, x: Byte): Unit = ???
  def updateShort(columnIndex: Int, x: Short): Unit = ???
  def updateInt(columnIndex: Int, x: Int): Unit = ???
  def updateLong(columnIndex: Int, x: Long): Unit = ???
  def updateFloat(columnIndex: Int, x: Float): Unit = ???
  def updateDouble(columnIndex: Int, x: Double): Unit = ???

  def updateBigDecimal(columnIndex: Int, x: java.math.BigDecimal): Unit = ???
  def updateString(columnIndex: Int, x: String): Unit = ???
  def updateBytes(columnIndex: Int, x: scala.Array[Byte]): Unit = ???

  def updateDate(columnIndex: Int, x: Date): Unit =
    ???

  def updateTime(columnIndex: Int, x: Time): Unit =
    ???
  def updateTimestamp(columnIndex: Int, x: Timestamp): Unit = ???

  def updateAsciiStream(columnIndex: Int, x: InputStream, length: Int): Unit =
    ???

  def updateBinaryStream(columnIndex: Int, x: InputStream, length: Int): Unit =
    ???

  def updateCharacterStream(columnIndex: Int, x: Reader, length: Int): Unit =
    ???

  def updateObject(columnIndex: Int, x: Any, scaleOrLength: Int): Unit = ???
  def updateObject(columnIndex: Int, x: Any): Unit = ???
  def updateNull(columnLabel: String): Unit = ???
  def updateBoolean(columnLabel: String, x: Boolean): Unit = ???
  def updateByte(columnLabel: String, x: Byte): Unit = ???
  def updateShort(columnLabel: String, x: Short): Unit = ???

  def updateInt(columnLabel: String, x: Int): Unit =
    ???
  def updateLong(columnLabel: String, x: Long): Unit = ???
  def updateFloat(columnLabel: String, x: Float): Unit = ???
  def updateDouble(columnLabel: String, x: Double): Unit = ???

  def updateBigDecimal(columnLabel: String, x: java.math.BigDecimal): Unit = ???
  def updateString(columnLabel: String, x: String): Unit = ???

  def updateBytes(columnLabel: String, x: scala.Array[Byte]): Unit =
    ???
  def updateDate(columnLabel: String, x: Date): Unit = ???
  def updateTime(columnLabel: String, x: Time): Unit = ???
  def updateTimestamp(columnLabel: String, x: Timestamp): Unit = ???

  def updateAsciiStream(
      columnLabel: String,
      x: InputStream,
      length: Int
  ): Unit = ???

  def updateBinaryStream(
      columnLabel: String,
      x: InputStream,
      length: Int
  ): Unit = ???

  def updateCharacterStream(
      columnLabel: String,
      reader: Reader,
      length: Int
  ): Unit = ???

  def updateObject(columnLabel: String, x: Any, scaleOrLength: Int): Unit = ???
  def updateObject(columnLabel: String, x: Any): Unit = ???
  def insertRow(): Unit = ???
  def updateRow(): Unit = ???
  def deleteRow(): Unit = ???
  def refreshRow(): Unit = ???
  def cancelRowUpdates(): Unit = ???
  def moveToInsertRow(): Unit = ???
  def moveToCurrentRow(): Unit = ???
  def getStatement: Statement = ???

  def getObject(columnIndex: Int, map: util.Map[String, Class[_]]): AnyRef = ???
  def getRef(columnIndex: Int): Ref = ???
  def getBlob(columnIndex: Int): Blob = ???
  def getClob(columnIndex: Int): Clob = ???
  def getArray(columnIndex: Int): Array = ???

  def getObject(columnLabel: String, map: util.Map[String, Class[_]]): AnyRef =
    ???
  def getRef(columnLabel: String): Ref = ???
  def getBlob(columnLabel: String): Blob = ???
  def getClob(columnLabel: String): Clob = ???
  def getArray(columnLabel: String): Array = ???
  def getDate(columnIndex: Int, cal: Calendar): Date = ???
  def getDate(columnLabel: String, cal: Calendar): Date = ???
  def getTime(columnIndex: Int, cal: Calendar): Time = ???
  def getTime(columnLabel: String, cal: Calendar): Time = ???
  def getTimestamp(columnIndex: Int, cal: Calendar): Timestamp = ???
  def getTimestamp(columnLabel: String, cal: Calendar): Timestamp = ???
  def getURL(columnIndex: Int): URL = ???
  def getURL(columnLabel: String): URL = ???
  def updateRef(columnIndex: Int, x: Ref): Unit = ???
  def updateRef(columnLabel: String, x: Ref): Unit = ???

  def updateBlob(columnIndex: Int, x: Blob): Unit =
    ???
  def updateBlob(columnLabel: String, x: Blob): Unit = ???

  def updateClob(columnIndex: Int, x: Clob): Unit =
    ???
  def updateClob(columnLabel: String, x: Clob): Unit = ???

  def updateArray(columnIndex: Int, x: Array): Unit =
    ???
  def updateArray(columnLabel: String, x: Array): Unit = ???
  def getRowId(columnIndex: Int): RowId = ???
  def getRowId(columnLabel: String): RowId = ???

  def updateRowId(columnIndex: Int, x: RowId): Unit =
    ???
  def updateRowId(columnLabel: String, x: RowId): Unit = ???
  def getHoldability: Int = ???
  def isClosed: Boolean = ???
  def updateNString(columnIndex: Int, nString: String): Unit = ???
  def updateNString(columnLabel: String, nString: String): Unit = ???
  def updateNClob(columnIndex: Int, nClob: NClob): Unit = ???
  def updateNClob(columnLabel: String, nClob: NClob): Unit = ???
  def getNClob(columnIndex: Int): NClob = ???
  def getNClob(columnLabel: String): NClob = ???
  def getSQLXML(columnIndex: Int): SQLXML = ???
  def getSQLXML(columnLabel: String): SQLXML = ???
  def updateSQLXML(columnIndex: Int, xmlObject: SQLXML): Unit = ???
  def updateSQLXML(columnLabel: String, xmlObject: SQLXML): Unit = ???
  def getNString(columnIndex: Int): String = ???
  def getNString(columnLabel: String): String = ???

  def getNCharacterStream(columnIndex: Int): Reader =
    ???
  def getNCharacterStream(columnLabel: String): Reader = ???

  def updateNCharacterStream(columnIndex: Int, x: Reader, length: Long): Unit =
    ???

  def updateNCharacterStream(
      columnLabel: String,
      reader: Reader,
      length: Long
  ): Unit = ???

  def updateAsciiStream(columnIndex: Int, x: InputStream, length: Long): Unit =
    ???

  def updateBinaryStream(columnIndex: Int, x: InputStream, length: Long): Unit =
    ???

  def updateCharacterStream(columnIndex: Int, x: Reader, length: Long): Unit =
    ???

  def updateAsciiStream(
      columnLabel: String,
      x: InputStream,
      length: Long
  ): Unit = ???

  def updateBinaryStream(
      columnLabel: String,
      x: InputStream,
      length: Long
  ): Unit = ???

  def updateCharacterStream(
      columnLabel: String,
      reader: Reader,
      length: Long
  ): Unit = ???

  def updateBlob(
      columnIndex: Int,
      inputStream: InputStream,
      length: Long
  ): Unit = ???

  def updateBlob(
      columnLabel: String,
      inputStream: InputStream,
      length: Long
  ): Unit = ???

  def updateClob(columnIndex: Int, reader: Reader, length: Long): Unit = ???

  def updateClob(columnLabel: String, reader: Reader, length: Long): Unit = ???

  def updateNClob(columnIndex: Int, reader: Reader, length: Long): Unit = ???

  def updateNClob(columnLabel: String, reader: Reader, length: Long): Unit = ???
  def updateNCharacterStream(columnIndex: Int, x: Reader): Unit = ???

  def updateNCharacterStream(columnLabel: String, reader: Reader): Unit = ???
  def updateAsciiStream(columnIndex: Int, x: InputStream): Unit = ???
  def updateBinaryStream(columnIndex: Int, x: InputStream): Unit = ???
  def updateCharacterStream(columnIndex: Int, x: Reader): Unit = ???

  def updateAsciiStream(columnLabel: String, x: InputStream): Unit =
    ???

  def updateBinaryStream(columnLabel: String, x: InputStream): Unit =
    ???

  def updateCharacterStream(columnLabel: String, reader: Reader): Unit = ???

  def updateBlob(columnIndex: Int, inputStream: InputStream): Unit =
    ???

  def updateBlob(columnLabel: String, inputStream: InputStream): Unit =
    ???
  def updateClob(columnIndex: Int, reader: Reader): Unit = ???
  def updateClob(columnLabel: String, reader: Reader): Unit = ???
  def updateNClob(columnIndex: Int, reader: Reader): Unit = ???
  def updateNClob(columnLabel: String, reader: Reader): Unit = ???
  def getObject[T](columnIndex: Int, `type`: Class[T]): T = ???
  def getObject[T](columnLabel: String, `type`: Class[T]): T = ???
  def unwrap[T](iface: Class[T]): T = ???
  def isWrapperFor(iface: Class[_]): Boolean = ???
}
