package com.senichev.kParsec.standardParsers

import com.senichev.kParsec.parser.*
import com.senichev.kParsec.option.*

// =========================================
// Char and string parsing
// =========================================

/**
 * Creates a Parser to match a single char.
 */
fun pchar(charToMatch: Char): Parser<Char> {
    return satisfy("$charToMatch") {it == charToMatch}
}

/**
 * Choose any of a list of characters.
 */
fun anyOf(chars: List<Char>): Parser<Char> {
    return choice(chars.map{pchar(it)}).setLabel("anyOf $chars")
}

/**
 * Parses a sequence of zero or more chars with the char parser cp.
 * Returns the parsed chars as a string.
 */
fun manyChars(cp: Parser<Char>): Parser<String> {
    return many(cp).mapP{it.joinToString(separator="")}
}

/**
 * Parses a sequence of one or more chars with the char parser cp.
 * Returns the parsed chars as a string.
 */
fun manyChars1(cp: Parser<Char>): Parser<String> {
    return many1(cp).mapP{it.joinToString(separator="")}
}

/**
 * Parses a string.
 */
fun pstring(str: String): Parser<String> {
            // convert to the list of char
    return str.toCharArray()
            // map each char to a pchar
            .map{pchar(it)}
            // convert to Parser<List<Char>>
            .toSequence()
            // convert to Parser<String>
            .mapP{it.joinToString(separator="")}
            // set label
            .setLabel(str)
}

// ==============================
// Whitespace parsing
// ==============================

/**
 * Creates a Parser to match a single whitespace char.
 */
fun whitespaceChar(): Parser<Char> {
    return satisfy("whitespace"){it.isWhitespace()}
}

/**
 * Parse zero or more whitespace chars.
 */
fun spaces(): Parser<List<Char>> {
    return many(whitespaceChar())
}

/**
 * Parse one or more whitespace chars.
 */
fun spaces1(): Parser<List<Char>> {
    return many1(whitespaceChar())
}

// ================================
// Number parsing
// ================================

/**
 * Creates a Parser to match a single digit char.
 */
fun digitChar(): Parser<Char> {
    return satisfy("digit"){it.isDigit()}
}

/**
 * Creates a parser for a signed Int.
 */
fun pint(): Parser<Int>{
    val resultToInt = fun (input:Pair<Option<Char>, String>): Int{
        val number = input.second.toInt()// TODO handle int overflow
        return when(input.first){
            is Some -> number*(-1)
            is None -> number
        }
    }
    val digits = manyChars1(digitChar())
    return pchar('-').asOption().andThen(digits).mapP{resultToInt(it)}.setLabel("integer")
}

// helper to wrap data to create a float number
private data class FloatInfo(val sign: Char?, val digits1: List<Char>, val digits2: List<Char>)

/**
 * Creates a parser for a signed Float.
 */
fun pfloat(): Parser<Float>{
    val resultToFloat = fun (input: Pair<Pair<Pair<Option<Char>, String>, Char>, String>): Float{
        val (pair1, digits2) = input
        val (pair2, _) = pair1
        val (sign, digits1) = pair2
        val part1 = digits1
        val part2 = digits2
        val number = "$part1.$part2".toFloat()

        return when (sign){
            is Some -> number*(-1)
            is None -> number
        }
    }
    val digits = manyChars1(digitChar())
    return pchar('-')
            .asOption()
            .andThen(digits)
            .andThen(pchar('.'))
            .andThen(digits)
            .mapP{resultToFloat(it)}
            .setLabel("float")
}