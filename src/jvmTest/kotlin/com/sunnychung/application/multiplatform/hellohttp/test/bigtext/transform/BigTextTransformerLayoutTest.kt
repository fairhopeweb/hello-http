package com.sunnychung.application.multiplatform.hellohttp.test.bigtext.transform

import com.sunnychung.application.multiplatform.hellohttp.extension.intersect
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.BigTextVerifyImpl
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.FixedWidthCharMeasurer
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.random
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.randomString
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.verifyBigTextImplAgainstTestString
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.MonospaceTextLayouter
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.TreeMap
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BigTextTransformerLayoutTest {

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun noTransformation(chunkSize: Int) { if (chunkSize != 16) return
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        verifyBigTextImplAgainstTestString(testString = testString, bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun inserts(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.insertAt(58, "[INSERT0]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.insertAt(16, "[INSERT1]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.insertAt(47, "[INSERT2]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun insertLines(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.insertAt(58, "[IN\nSERT0\n]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.insertAt(16, "\n[INSER\nT1]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.insertAt(47, "[INSERT2]\n")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun insertBeforeLineBreak(chunkSize: Int) {
        val testString = "1234567890<234\n<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.insertAt(14, "567890")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    @Order(Integer.MAX_VALUE - 100) // This test is pretty time-consuming. Run at the last!
    fun manyInserts1(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        random = Random(23456)

        repeat(600) {
            val pos = when (random.nextInt(10)) {
                in 0 .. 1 -> 0
                in 2 .. 3 -> v.originalLength
                else -> random.nextInt(1, v.originalLength)
            }
            val length = when (random.nextInt(10)) {
                in 0 .. 2 -> 1 + random.nextInt(3)
                in 3 .. 4 -> random.nextInt(4, 11)
                in 5 .. 6 -> random.nextInt(11, 300)
                7 -> random.nextInt(300, 1000)
                8 -> random.nextInt(1000, 10000)
                9 -> random.nextInt(10000, 100000)
                else -> throw IllegalStateException()
            }
            v.insertAt(pos, randomString(length, isAddNewLine = true))
            verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    @Order(Integer.MAX_VALUE - 100) // This test is pretty time-consuming. Run at the last!
    fun manyInserts2(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(randomString(1000000, isAddNewLine = true))
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        random = Random(2223456)

        repeat(200) {
            val pos = when (random.nextInt(10)) {
                in 0 .. 1 -> 0
                in 2 .. 3 -> v.originalLength
                else -> random.nextInt(1, v.originalLength)
            }
            val length = when (random.nextInt(10)) {
                in 0 .. 2 -> 1 + random.nextInt(3)
                in 3 .. 4 -> random.nextInt(4, 11)
                in 5 .. 6 -> random.nextInt(11, 300)
                7 -> random.nextInt(300, 1000)
                8 -> random.nextInt(1000, 10000)
                9 -> random.nextInt(10000, 100000)
                else -> throw IllegalStateException()
            }
            v.insertAt(pos, randomString(length, isAddNewLine = true))
            verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deletes(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.delete(46 .. 68)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(10 .. 19)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(30 .. 43)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deletesLines1(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.delete(v.length - 12 until v.length)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(20 .. 21)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(41 .. 46)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deletesLines2(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.delete(42 .. 43)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(44 .. 45)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(21 .. 21)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deletesAtTheEnd(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        val originalLength = testString.length
        v.delete(originalLength - 3 until originalLength)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(originalLength - 5 until originalLength - 3)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(originalLength - 9 until originalLength - 5)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(originalLength - 10 until originalLength - 9)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(23 until originalLength - 10)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deletesAtTheBeginning(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        val originalLength = testString.length
        v.delete(0 .. 1)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(2 .. 18)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(19 .. 22)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(23 .. 41)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(42 until originalLength - 7)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deleteEverything1(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        val originalLength = testString.length
        v.delete(0 .. 36)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(37 until originalLength)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deleteEverything2(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        val originalLength = testString.length
        v.delete(0 until originalLength)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    @Order(Integer.MAX_VALUE - 100)
    fun manyDeletes(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(randomString(1300000, isAddNewLine = true))
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        random = Random(1123456)

        val deletedIntervals = TreeMap<Int, Int>()

        repeat(1000) {
            // create a random interval that does not overlap with previous ones
            val pos = run {
                var p: Int
                do {
                    var isPositionUsed = false
                    p = random.nextInt(0, v.originalLength)
                    val deleted = deletedIntervals.subMap(-1, true, p, true).lastEntry()
                    if (deleted != null && deleted.key + deleted.value >= p) {
                        isPositionUsed = true
                    }
                } while (isPositionUsed)
                p
            }
            val length = run {
                var len = when (random.nextInt(10)) {
                    in 0 .. 2 -> 1 + random.nextInt(3)
                    in 3 .. 4 -> random.nextInt(4, 11)
                    in 5 .. 6 -> random.nextInt(11, 100)
                    7 -> random.nextInt(100, 1000)
                    8 -> random.nextInt(1000, 10000)
                    9 -> random.nextInt(10000, 100000)
                    else -> throw IllegalStateException()
                }
                len = minOf(v.originalLength - pos, len)
                val deleted = deletedIntervals.subMap(pos, true, pos + len, false).firstEntry()
                // for example, deleted interval: 3 ..< 10
                // pos = 1, len = 9 (1 ..< 10)
                // -> new range = 1 ..< 3, len = 2
                if (deleted != null && !((deleted.key until deleted.key + deleted.value) intersect (pos until pos + len)).isEmpty()) {
                    len = deleted.key - pos
                }
                len
            }
            deletedIntervals[pos] = length
            v.delete(pos until pos + length)
//            println("new len = ${v.bigTextImpl.length}")
            verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun replaces(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefg\nhij<BC\nDEFGHIJ<row break< shou\nld h<appen her<e\n."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = testString.length

        v.replace(44 .. 69, "[Replace0]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.replace(16 .. 18, "[Replace\n1]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.replace(24 .. 33, "[Replace2]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.replace(34 .. 43, "[Replace3]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.replace(0 .. 8, "[Replace4]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.replace(originalLength - 1 until originalLength, "[Replace5]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun replaceWholeText(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefg\nhij<BC\nDEFGHIJ<row break< shou\nld h<appen her<e\n.\n"
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = testString.length

        v.replace(0 until originalLength, "[Replace All!]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    @Order(Integer.MAX_VALUE - 100)
    fun manyReplaces(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(randomString(200000, isAddNewLine = true))
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        random = Random(1123456)

        val replacedIntervals = TreeMap<Int, Int>()

        repeat(1000) {
            // create a random interval that does not overlap with previous ones
            val pos = run {
                var p: Int
                do {
                    var isPositionUsed = false
                    p = random.nextInt(0, v.originalLength)
                    val deleted = replacedIntervals.subMap(-1, true, p, true).lastEntry()
                    if (deleted != null && deleted.key + deleted.value >= p) {
                        isPositionUsed = true
                    }
                } while (isPositionUsed)
                p
            }
            val length = run {
                var len = when (random.nextInt(10)) {
                    in 0 .. 2 -> 1 + random.nextInt(3)
                    in 3 .. 4 -> random.nextInt(4, 11)
                    in 5 .. 6 -> random.nextInt(11, 100)
                    7 -> random.nextInt(100, 1000)
                    8 -> random.nextInt(1000, 10000)
                    9 -> random.nextInt(10000, 100000)
                    else -> throw IllegalStateException()
                }
                len = minOf(v.originalLength - pos, len)
                val deleted = replacedIntervals.subMap(pos, true, pos + len, false).firstEntry()
                // for example, deleted interval: 3 ..< 10
                // pos = 1, len = 9 (1 ..< 10)
                // -> new range = 1 ..< 3, len = 2
                if (deleted != null && !((deleted.key until deleted.key + deleted.value) intersect (pos until pos + len)).isEmpty()) {
                    len = deleted.key - pos
                }
                assert(len >= 0)
                len
            }
            val newLen = when (random.nextInt(10)) {
                in 0 .. 2 -> random.nextInt(4)
                in 3 .. 4 -> random.nextInt(4, 11)
                in 5 .. 6 -> random.nextInt(11, 100)
                7 -> random.nextInt(100, 1000)
                8 -> random.nextInt(1000, 7000)
                9 -> random.nextInt(7000, 30000)
                else -> throw IllegalStateException()
            }
            replacedIntervals[pos] = length
            v.replace(pos until pos + length, randomString(newLen, isAddNewLine = true))
            verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
        }
    }
}
