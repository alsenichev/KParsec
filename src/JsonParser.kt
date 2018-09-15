package com.senichev.kParsec.json

import com.senichev.kParsec.parser.*
import com.senichev.kParsec.standardParsers.*
import com.senichev.kParsec.option.*
import com.senichev.kParsec.result.*

//=====================================
// JSON structure
// ====================================
sealed class JValue
data class JString(val value: String): JValue(){
    override fun toString() = "JString($value)"
}
data class JNumber(val value: Float): JValue(){
    override fun toString() = "JNumber($value)"
}
data class JBool(val value: Boolean): JValue(){
    override fun toString() = "JBool($value)"
}
object JNull: JValue(){
    override fun toString() = "JNull"
}
data class JObject(val value: Map<String, JValue>) : JValue(){
    override fun toString() = "JObject($value)"
}
data class JArray(val value: List<JValue>): JValue(){
    override fun toString() = "JArray($value)"
}

// ========================================
// Parsing null
// ========================================

/**
 * Parses null.
 */
fun jNull(): Parser<JNull>{
    return pstring("null").cast(JNull).setLabel("null")
}

// ========================================
// Parsing a bool
// ========================================

/**
 * Parses a bool.
 */
fun jBool(): Parser<JBool>{
    val jtrue = pstring("true").cast(JBool(true))
    val jfalse = pstring("false").cast(JBool(false))
    return jtrue orElse jfalse setLabel("bool")
}

// =======================================
// Parsing a string
// =======================================

/**
 * Parses any unicode character other than quote and backslash.
 */
internal fun jUnescapedChar() =
        satisfy("char"){ch:Char -> ch != '\\' && ch != '\"'}

/**
 * Parses an escaped char.
 */
internal fun jEscapedChar (): Parser<Char> {
    val parsers = listOf(
            // (stringToMatch, resultChar)
            Pair("\\\"",'\"'),      // quote
            Pair("\\\\",'\\'),      // reverse solidus
            Pair("\\/",'/'),        // solidus
            Pair("\\b",'\b'),       // backspace
            Pair("\\f",'\u000C'),   // formfeed
            Pair("\\n",'\n'),       // newline
            Pair("\\r",'\r'),       // cr
            Pair("\\t",'\t')       // tab
    )
            // convert each pair into a parser
            .map{pair: Pair<String, Char> -> pstring(pair.first).cast(pair.second)}
    // combine them into one
    return choice(parsers).setLabel("escaped char")
}

/**
 * Parses a unicode char.
 */
internal fun jUnicodeChar(): Parser<Char> {

    // set up the "primitive" parsers
    val backslash = pchar('\\')
    val uChar = pchar('u')
    val hexdigit = anyOf((listOf('0'..'9') + listOf('A'..'F') + listOf('a'..'f')).flatMap{it})

    // convert the parser output (nested tuples)
    // to a char
    val convertToChar = fun(input: Pair<Pair<Pair<Char,Char>,Char>,Char>): Char{
        val (rest, h4) = input
        val (rest2, h3) = rest
        val (h1, h2) = rest2
        val number = "$h1$h2$h3$h4".toLong(radix=16)
        return number.toChar()
    }

    // set up the main parser
    return backslash
            .andThenKeepRight(uChar)
            .andThenKeepRight(hexdigit)
            .andThen(hexdigit)
            .andThen(hexdigit)
            .andThen(hexdigit)
            .mapP{convertToChar(it)}
}

/**
 * Parses a string inside the quotes.
 */
internal fun quotedString():Parser<String>{
    val quote = pchar('\"').setLabel("quote")
    val jchar = jUnescapedChar().orElse(jEscapedChar()).orElse(jUnicodeChar())
    return quote.andThenKeepRight(manyChars(jchar)).andThenKeepLeft(quote)
}

/**
 * Parses a json string.
 */
fun jString() : Parser<JString>{
    return quotedString().mapP{JString(it)}.setLabel("quoted string")
}

// ==============================================
// Parsing a number
// ==============================================

/**
 * Parses a json number.
 */
fun jNumber(): Parser<JNumber>{
    val optSign = pchar('-').asOption()
    val zero = pstring("0")
    val digitOneNine = satisfy("1-9"){it.isDigit() && it != '0'}
    val digit = satisfy("digit"){it.isDigit()}
    val point = pchar('.')
    val e = pchar('e') orElse pchar('E')
    val optPlusMinus = (pchar('-') orElse pchar('+')).asOption()
    val nonZeroInt = digitOneNine.andThen(manyChars(digit)).mapP{val (first, second)=it; "$first$second"}
    val intPart = zero orElse nonZeroInt
    val fractionPart = point andThenKeepRight manyChars1(digit)
    val exponentPart = e andThenKeepRight optPlusMinus andThen manyChars1(digit)
    // utility function to convert an optional value to a string, or "" if missing
    fun <T> Option<T>.asString (f: (T) -> String): String {
        return when(this){
            is None -> ""
            is Some -> f(this.value)
        }
    }
    // utility to convert ugly nested Pairs to a number
    fun convertToJNumber(input: Pair<Pair<Pair<Option<Char>, String>, Option<String>>, Option<Pair<Option<Char>, String>>>): JNumber{
        // convert to strings and let .NET parse them! - crude but ok for now.
        val (rest, expPart) = input
        val (rest2, fractionPart) = rest
        val (optSign, intPart) = rest2

        val signStr = optSign.asString{it.toString()} // e.g. "-"
        val fractionPartStr = fractionPart.asString{".$it"}  // e.g. ".456"
        val expPartStr = expPart.asString{
            val(optSign, digits) = it
            val sign = optSign.asString{i -> i.toString()}
            "e$sign$digits"          // e.g. "e-12"
        }
        // add the parts together and convert to a float, then wrap in a JNumber
        return JNumber("$signStr$intPart$fractionPartStr$expPartStr".toFloat())
    }
    return (optSign andThen intPart andThen fractionPart.asOption() andThen exponentPart.asOption())
            .mapP{convertToJNumber(it)}.setLabel("number")
}

// not a very good hack to improve error message - it seeks for a space after the end of the input state
val jNumber_ = jNumber() andThenKeepLeft spaces1()

// =================================================
// Parsing Array
// =================================================

/**
 * Circumvents the mutual recursive definition: to make JArray we need JValue,
 * but to make the choices of JValue we need JArray.
 */
class ForwardedParser(){
    // initialized with a placeholder, after the client is ready, this will be set to a valid parser
    var parserRef: Parser<JValue> = Parser<JValue>("unknown"){ throw IllegalStateException("unfixed forwarded parser") }

    // wrapper of the parser reference supplied to the client
    val parser: Parser<JValue>
        //forwards input to the placeholder
        get() = Parser("unknown"){parserRef.runOnInput(it)}
}

/**
 * Creates JArray parser, with a placeholder valueParser
 */
internal fun jArray_(valueParser: Parser<JValue>): Parser<JArray> {
    val left = pchar('[').andThenKeepLeft(spaces())
    val right = pchar(']').andThenKeepLeft(spaces())
    val comma = pchar(',').andThenKeepLeft(spaces())

    val value = valueParser.andThenKeepLeft(spaces())
    // set up the list parser
    val values = sepBy1(value, comma)

    // set up the main parser
    return values.between(left, right).mapP{JArray(it)}.setLabel("array")
}

/**
 * Creates a Parser to parse json array.
 */
fun jArray(): Parser<JArray>{
    val forwardedParser = ForwardedParser()
    val jarray = jArray_(forwardedParser.parser)
    val jobject = jObject_(forwardedParser.parser)
    forwardedParser.parserRef = choice(listOf(jNull(), jBool(), jNumber(), jString(), jarray, jobject ))
    return jarray
}

// =========================================
// Parsing an object
// =========================================

/**
 * Creates JObject parser with a placeholder value parser.
 */
internal fun jObject_(valueParser: Parser<JValue>): Parser<JObject> {

    // set up the "primitive" parsers
    val left = pchar('{') andThenKeepLeft spaces()
    val right = pchar('}') andThenKeepLeft spaces()
    val colon = pchar(':') andThenKeepLeft spaces()
    val comma = pchar(',') andThenKeepLeft spaces()
    val key = quotedString() andThenKeepLeft spaces()
    val value = valueParser andThenKeepLeft spaces()

    // set up the list parser
    val keyValue = key.andThenKeepLeft(colon).andThen(value)
    val keyValues = sepBy1(keyValue, comma)

    // set up the main parser
    return keyValues.between(left, right).mapP{it.toMap()}.mapP{JObject(it)}.setLabel("object")
}

/**
 * Creates a JObject parser.
 */
fun jObject(): Parser<JObject>{
    val forwardedParser = ForwardedParser()
    val jarray = jArray_(forwardedParser.parser)
    val jobject = jObject_(forwardedParser.parser)
    forwardedParser.parserRef = choice(listOf(jNull(), jBool(), jNumber(), jString(), jarray, jobject ))
    return jobject
}

// ===============================
// Complete json parser
// ===============================

/**
 * Creates a json parser.
 */
fun jsonParser(): Parser<JValue> {
    val forwardedParser = ForwardedParser()
    val jarray = jArray_(forwardedParser.parser)
    val jobject = jObject_(forwardedParser.parser)
    val result = choice(listOf(jNull(), jBool(), jNumber(), jString(), jarray, jobject ))
    forwardedParser.parserRef = result
    return result
}

/**
 * Parses a json string.
 */
fun parseJson(input: String): Result<JValue>{
    return jsonParser().run(input)
}