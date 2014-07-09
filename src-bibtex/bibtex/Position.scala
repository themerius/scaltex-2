package io.bibimbap
package bibtex

// From Scala 2.10
// https://github.com/scala/scala/blob/2.10.x/src/library/scala/io/Position.scala

abstract class Position {
  /** Definable behavior for overflow conditions.
   */
  def checkInput(line: Int, column: Int): Unit

  /** Number of bits used to encode the line number */
  final val LINE_BITS   = 20
  /** Number of bits used to encode the column number */
  final val COLUMN_BITS = 31 - LINE_BITS // no negatives => 31
  /** Mask to decode the line number */
  final val LINE_MASK   = (1 << LINE_BITS) - 1
  /** Mask to decode the column number */
  final val COLUMN_MASK = (1 << COLUMN_BITS) - 1

  /** Encodes a position into a single integer. */
  final def encode(line: Int, column: Int): Int = {
    checkInput(line, column)

    if (line >= LINE_MASK)
      LINE_MASK << COLUMN_BITS
    else
      (line << COLUMN_BITS) | scala.math.min(COLUMN_MASK, column)
  }

  /** Returns the line number of the encoded position. */
  final def line(pos: Int): Int = (pos >> COLUMN_BITS) & LINE_MASK

  /** Returns the column number of the encoded position. */
  final def column(pos: Int): Int = pos & COLUMN_MASK

  /** Returns a string representation of the encoded position. */
  def toString(pos: Int): String = line(pos) + ":" + column(pos)
}

object Position extends Position {
  /** The undefined position */
  @deprecated("This will be removed", "2.9.0")
  final val NOPOS = 0

  /** The first position in a source file */
  @deprecated("This will be removed", "2.9.0")
  final val FIRSTPOS = encode(1, 1)

  def checkInput(line: Int, column: Int) {
    if (line < 0)
      throw new IllegalArgumentException(line + " < 0")
    if ((line == 0) && (column != 0))
      throw new IllegalArgumentException(line + "," + column + " not allowed")
    if (column < 0)
      throw new IllegalArgumentException(line + "," + column + " not allowed")
  }
}