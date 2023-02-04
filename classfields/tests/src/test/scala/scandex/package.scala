import scandex.bitset.mutable.LongArrayBitSet
import scandex.core.bitset.BitSet
import scandex.db.index.{DocumentId, ValueIdx}
import zio.test.*
import zio.test.Assertion.*
import zio.test.Gen.*
import zio.Random
import zio.test.Sized
import zio.test.Gen.{byte, char, double, float, int, short}

package object scandex {

  val ByteSequence: Gen[Sized, Vector[Byte]] = vectorOfN(100)(byte)
    .map(_.distinct.sorted)

  val ShortSequence: Gen[Sized, Vector[Short]] = vectorOfN(100)(short)
    .map(_.distinct.sorted)

  def ShortSequenceUnordered(
    min: Short = -500,
    max: Short = 500,
    n: Int = 5,
  ): Gen[Sized, Vector[Short]] = vectorOfN(n)(short(min, max))

  val DoubleSequence: Gen[Sized, Vector[Double]] = vectorOfN(300)(double)
    .map(_.sorted(Ordering.Double.TotalOrdering).distinct)

  def DoubleSequenceUnordered(n: Int = 5): Gen[Sized, Vector[Double]] =
    vectorOfN(n)(double)

  val FloatSequence: Gen[Sized, Vector[Float]] = vectorOfN(300)(float)
    .map(_.sorted(Ordering.Float.TotalOrdering).distinct)

  val IntSequence: Gen[Sized, Vector[Int]] = vectorOfN(300)(int)
    .map(_.sorted.distinct)

  val CharacterSequence: Gen[Sized, Vector[Char]] = vectorOfN(100)(char)
    .map(_.sorted.distinct)

  def LongSequence(
    min: Long = 1L,
    max: Long = 99L,
    n: Int = 5,
  ): Gen[Sized, Vector[Long]] =
    vectorOfN(n)(long(min, max)).map(_.distinct.sorted(Ordering.Long))

  def LongSequenceUnordered(
    min: Long = 1L,
    max: Long = 9999L,
    n: Int = 5,
  ): Gen[Sized, Vector[Long]] = vectorOfN(n)(long(min, max))

  def LongOrderedSequenceN(
    min: Long = 1L,
    max: Long = 9999L,
    n: Int = 5,
  ): Gen[Random, Vector[Long]] =
    setOfN(n)(long(min, max)).map(_.toVector.sorted(Ordering.Long))

  def StringOrderedSequenceN(
    min: Int = 1,
    max: Int = 20,
    n: Int = 5,
  ): Gen[Random, Vector[String]] = {
    setOfN(n)(stringBounded(min, max)(char('a', 'z'))).map(_.toVector.sorted)
  }

  def SeqOfSet(
    documentCount: Int,
    maxValueIdx: Long,
    withNull: Boolean = true,
  ): Gen[Random, List[Set[Long]]] = {
    val initValue =
      if (withNull)
        0
      else
        1
    listOfN(documentCount)(int(initValue, maxValueIdx.toInt)).map { deltas =>
      val shuffleValueIndices = scala
        .util
        .Random
        .shuffle((0L to maxValueIdx).toList)

      deltas.map(size =>
        if (size == 0)
          Set(-1L)
        else
          shuffleValueIndices.take(size).toSet,
      )
    }
  }

  /** @return
    *   Сколько битов нужно, чтобы закодировать valuesCount значений
    */
  def bits(valuesCount: Int): Int =
    Math.ceil(Math.log(valuesCount.toDouble) / Math.log(2)).toInt

  /** Построить обратный сегмент по прямому сегменту для SINGLE индекса
    * @return
    */
  def buildSinglePostingsByForward(
    forwardSeq: Seq[Long],
    valuesCount: Int,
  ): Seq[Long] = {
    val matrixPart = (0 until bits(valuesCount)).map(i =>
      Integer.parseInt(
        forwardSeq
          .map(value =>
            if (value < 0)
              0
            else
              (value >> i) & 1,
          )
          .mkString,
        2,
      ),
    )
    val nullPart = Integer.parseInt(
      forwardSeq
        .map(v =>
          if (v < 0)
            0
          else
            1,
        )
        .mkString,
      2,
    )
    (matrixPart :+ nullPart).map(_.toLong)
  }

  /** @return
    *   Построить обратный сегмент по прямому сегменту для MULTI индекса
    */
  def buildMultiPostingsByForward(
    forwardSeq: Seq[Seq[Long]],
    valuesCount: Int,
  ): Seq[Long] = {
    (0 until valuesCount)
      .map(i =>
        Integer.parseInt(
          forwardSeq
            // потому что в postingList номер документа = разряду
            .reverse
            .map(current => {
              if (current.contains(i.toLong))
                1
              else
                0
            })
            .mkString,
          2,
        ),
      )
      .map(_.toLong)
  }

  /** Для single postings. По posting листу строит строку с notNull сегментами и
    * forward для оценки при дебаге
    * @param wordMatrix
    *   предварительная матрица значений документов (обратный сегмент)
    * @param seed
    *   случайное число для обезначивания некоторых документов
    * @return
    *   пара списков:
    *   - человекочитаемый прямой сегмент (с ValueIdx)
    *   - непустые документы для построения postingList
    */
  def generateNotNullRowForSingleSegment(
    wordMatrix: Seq[Seq[Long]],
    seed: Long,
  )(docCount: Long): (List[Long], List[Long]) = {
    val dirtyValues = computeRealValues(wordMatrix)(docCount)
    val nonNull =
      wordMatrix.map(u => u.foldLeft(seed)((acc, x) => acc | x)).toList
    (
      dirtyValues
        .zipWithIndex
        .map { case (x, i) =>
          if ((x == 0) && ((nonNull(i / 64) & (1L << i)) == 0))
            -1
          else
            x
        }
        .toList,
      nonNull,
    )
  }

  /** Построить прямой сегмент по словам posting сегмента для MULTI индекса
    * @param words
    *   матрица значений документов
    * @return
    *   прямой сегмент
    */
  def buildMultiForwardByPosting(
    words: Seq[Seq[Long]],
  )(docCount: Long): Seq[Set[Long]] = {
    words
      .flatMap { wordsBatch =>
        wordsBatch
          .indices
          .foldLeft(Array.fill(64)(Set[Long]())) { (acc, valueIdx) =>
            (0 until 64).foreach { i =>
              if (((wordsBatch(valueIdx) >> i) & 1) == 1)
                acc.update(i, acc(i).+(valueIdx.toLong))
            }
            acc
          }
      }
      .take(docCount.toInt)
  }

  /** Для single postings. Из слов матрицы восстанавливает ValueIdx (nonnull не
    * учитывается)
    * @param words
    *   матрица слов: в первой строке N слов первых 64-х документов, где N -
    *   число битов, затем последующие
    * @return
    *   массив значений, где индекс- номер документа
    */
  def computeRealValues(words: Seq[Seq[Long]])(docCount: Long): Array[Long] = {
    words
      .flatMap { wordsBatch =>
        wordsBatch
          .indices
          .foldLeft(Array.ofDim[Long](64)) { (acc, floorId) =>
            (0 until 64).foreach { i =>
              val x = ((wordsBatch(floorId) >> i) & 1) << floorId
              acc.update(i, x + acc(i))
            }
            acc
          }
      }
      .toArray
      .take(docCount.toInt)
  }

  /** По прямому сегменту строит битсет-результат поиска документов,
    * удовлетворяющих предикату
    * @param forward
    *   прямой сегмент
    * @param predicate
    *   предикат
    * @tparam Values
    *   тип значений у одного документа (Set или примитив)
    * @return
    *   битсет со всеми документами, удовлетворяющих предикату
    */
  def searchInForward[Values](
    forward: Seq[Values],
    predicate: Values => Boolean,
  )(docCount: Long): BitSet = {
    val arr =
      forward
        .grouped(64)
        .map(
          _.zipWithIndex
            .filter { case (y, _) =>
              predicate(y)
            }
            .map { case (_, i) =>
              1L << i
            }
            .sum,
        )
        .toSeq
    val bitset = new LongArrayBitSet(docCount)
    arr
      .zipWithIndex
      .foreach { case (x, i) =>
        bitset.setWord(i.toLong, x)
      }
    bitset
  }

  def assertBitSet(aR: BitSet)(eR: BitSet): TestResult = {
    val y = (0L until aR.wordCount).map(aR.word).toList
    val x = (0L until eR.wordCount).map(eR.word).toList
    assert(y)(hasSameElements(x))
  }

  @inline
  implicit def __intWidenToValueIdx(i: Int): ValueIdx = ValueIdx(i.toLong)

  @inline
  implicit def __intWidenToDocumentId(i: Int): DocumentId = DocumentId(i.toLong)

}
