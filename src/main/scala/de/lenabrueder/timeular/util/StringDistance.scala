package de.lenabrueder.timeular.util

/**
  * This class contains code from https://github.com/vickumar1981/stringdistance/blob/master/src/main/scala/com/github/vickumar1981/stringdistance/impl/LongestCommonSeqImpl.scala
  * which can be removed as soon as https://github.com/vickumar1981/stringdistance/issues/41 is fixed (2.13 support).
  *
  * This part is licensed under APACHE 2 license.
  *
  * I will remove the code as soon as I can simply use the library.
  */
object LongestCommonSeq {
  private def lcs(x: String, y: String, m: Int, n: Int): Int = {
    if (m == 0 || n == 0) 0
    else if (x(m - 1) == y(n - 1)) 1 + lcs(x, y, m - 1, n - 1)
    else math.max(lcs(x, y, m, n - 1), lcs(x, y, m - 1, n))
  }

  def distance(s1: String, s2: String): Int = lcs(s1, s2, s1.length, s2.length)
}
