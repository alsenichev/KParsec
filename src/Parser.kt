package com.senichev.kParsec.parser

import com.senichev.kParsec.result.*
import com.senichev.kParsec.option.*
import com.senichev.kParsec.textInput.*

/**
 * Represents a function that parses an InputState.
 */
data class Parser<out T>( val label: String, val parse: (InputState) -> Result<T>){
    override fun toString() = label
}

/**
 * Creates an InputeState from a string and runs the function of the Parser.
 */
fun <T> Parser<T>.run (input: String): Result<T>{
    return this.parse(InputState.fromString(input))
}

/**
 * Runs the function of the Parser.
 */
fun <T> Parser<T>.runOnInput (input: InputState): Result<T>{
    return this.parse(input)
}

/**
 * Match an input token if the predicate is satisfied.
 */
// ...not quite sure that it would be possible to implement this using a generic type,
// to be able, for example to parse an array of bytes. Currently will stick with Chars.
fun satisfy (label: String, predicate: (Char) -> Boolean) : Parser<Char>{
    val innerFn = fun(input: InputState) : Result<Char>{
        val (remaining, charOpt) = input.nextChar()
        if(charOpt == null){
            return Failure(label, "No more input", ParserPosition.fromInputState(input))
        }else{
            if(predicate(charOpt)){
                return Success(charOpt, remaining)
            }else{
                return Failure(label, "Unexpected '$charOpt'", ParserPosition.fromInputState(input))
            }
        }
    }
    return Parser(label, innerFn)
}

/**
 * Assign a new label to a parser.
 */
infix fun <T> Parser<T>.setLabel(label: String) : Parser<T>{
    // change the inner function to use the new label
    val innerFn = fun(input: InputState): Result<T>{
        val result: Result<T> = this.parse(input)
        when(result){
            // if success do nothing
            is Success -> return result
            // if failure, return new label
            is Failure -> return Failure(label, result.message, result.position)
        }
    }
    return Parser(label, innerFn)
}

// =====================================
// Standard combinators
// =====================================

/**
 * Lifts a value to the world of Parsers.
 */
fun <T> returnP (value: T): Parser<T>{
    // XXX - string templates fail to represent lambdas
    val label = when(value){
        is Char, Int, String -> "$value"
        else -> "unknown identifier"
    }
    return Parser(label){Success(value, it)}
}

/**
 * Changes the type of the Parser.
 */
fun <T, R> Parser<T>.mapP(func: (T) -> R): Parser<R> {
    return this.bindP{returnP(func(it))}
}

/**
 * Takes a parser-producing function and a parser, and passes the
 * output of the parser into the function, to create a new parser.
*/
fun <T, R> Parser<T>.bindP(func: (T)->Parser<R>): Parser<R>{
    var label = "unknown"
    val innerFn = fun(input: InputState): Result<R>{
        val result1 = runOnInput(input)
        when(result1){
            is Failure ->{
                label = result1.label
                return Failure<R>(result1.label, result1.message, result1.position)
            }
            is Success -> {
                val parser2 = func(result1.value)
                return parser2.runOnInput(result1.inputState)
            }
        }
    }
    return Parser(label, innerFn)
}

/**
 * Given a Parser that parses a function and a Parser that parses a value,
 * applies the function to the value only if both are successfully parsed.
 */
fun <T,R> Parser<(T) -> R>.applyP (parser: Parser<T>) : Parser<R>{
    return this.bindP{x:(T )-> R -> parser.bindP{y: T -> returnP(x(y))}}
}

/**
 * Lifts the two-parameter function to the world of Parsers.
 */
fun <T1, T2, R> lift2 (parser1: Parser<T1>, parser2: Parser<T2>, func: (T1, T2) -> R): Parser<R>{
    // curry the func to make it compatible for applyP or map
    val lifted: Parser<(T1) -> ((T2)->R)> = returnP({a -> {b-> func(a,b)}})
    val first: Parser<(T2) -> R> = lifted.applyP(parser1)
    return first.applyP(parser2)
}

/**
 * Success if both parsers are success.
 */
infix fun <T1, T2> Parser<T1>.andThen (second: Parser<T2>): Parser<Pair<T1, T2>>{
    val label = "$label andThen $second"
    return bindP{i: T1 -> second.bindP{y: T2 -> returnP(Pair(i, y))}}.setLabel(label)
}

/**
 * Success if any of the parsers is success.
 */
infix fun <T> Parser<T>.orElse (second: Parser<T>): Parser<T>{
    val innerFn = fun(input: InputState): Result<T>{
        val result1 = runOnInput(input)
        when(result1){
            is Success -> return result1
            is Failure ->{
                return second.runOnInput(input)
            }
        }
    }
    return Parser("$label orElse $second", innerFn)
}

/**
 * Matches a parser zero or more times.
 */
fun <T> parseZeroOrMore(parser: Parser<T>, input: InputState) : Pair<List<T>, InputState>{
    // run parser with the input
    val firstResult = parser.runOnInput(input)
    // test the Result
    return when(firstResult){
        // if parser fails, return empty list
        is Failure -> Pair(listOf(), input)
        is Success ->{
            // if parse succeeds, call recursively
            // to get subsequent values
            val (subsequentValues, inputState) = parseZeroOrMore(parser, firstResult.inputState)
            val values = listOf(firstResult.value) + subsequentValues
            Pair(values, inputState)
        }
    }
}

/**
 * Transforms a list of Parsers to the Parser of the list of values.
 */
fun <T> List<Parser<T>>.toSequence(): Parser<List<T>>{
    // the function that will prepend a value to the list
    val cons = fun (head: T, tail: List<T>): List<T>{
        return listOf(head) + tail
    }
    // lift the function to the world of Parsers
    val consP: (Parser<T>, Parser<List<T>>) -> Parser<List<T>> = {p1, p2 -> lift2(p1, p2, cons)}

    if(this.isEmpty()){
        return returnP(listOf())
    }else{
        // prepend the first element to the list of values in the Parser
        return consP(this.first(), (this.drop(1)).toSequence())
    }
}

/**
 * Match zero or more occurrences of the specified parser.
 */
fun <T> many (parser: Parser<T>): Parser<List<T>>{
    val parse = {input:InputState -> parseZeroOrMore(parser, input)}
    val success = {pair:Pair<List<T>, InputState> -> Success(pair.first, pair.second)}
    val innerFn = {input:InputState -> success(parse(input))}
    return Parser("many $parser", innerFn)
}

/**
 * Match one or more occurrences of the specified parser.
 */
fun <T> many1 (parser: Parser<T>): Parser<List<T>>{
    return parser.bindP{head:T -> many(parser).bindP{tail:List<T> -> returnP(listOf(head) + tail)}}
}

/**
 * Match any of the parsers in the list.
 */
fun <T> choice (parsers: List<Parser<T>>):Parser<T>{
    return parsers.reduce{p1, p2 -> p1.orElse(p2)}
}

/**
 * Throws away the result of the second parser.
 */
infix fun <T1,T2> Parser<T1>.andThenKeepLeft(parser: Parser<T2>): Parser<T1>{
    // create a pair and then keep only the first value
    return this.andThen(parser).mapP{ i:Pair<T1, T2> -> i.first }
}

/**
 * Throws away the result of the first parser.
 */
infix fun <T1,T2> Parser<T1>.andThenKeepRight(parser: Parser<T2>): Parser<T2>{
    return this.andThen(parser).mapP{ i:Pair<T1, T2> -> i.second }
}

/**
 * Parsers one or more occurrences of parser separated by sep.
 */
fun <T1, T2> sepBy1(parser: Parser<T1>, sep: Parser<T2>): Parser<List<T1>>{
    val sepThenP = sep.andThenKeepRight(parser)
    return parser.andThen(many(sepThenP)).mapP{i:Pair<T1,List<T1>>->listOf(i.first) + i.second}
}

/**
 * Parsers zero or more occurrences of parser separated by sep.
 */
fun <T1,T2> sepBy (parser:Parser<T1>, sep: Parser<T2>): Parser<List<T1>>{
    return sepBy1(parser, sep).orElse(returnP(listOf()))
}

/**
 * Matches the parser between two parsers and throws away the results of the surrounding parsers.
 */
fun <T,T1,T2> Parser<T>.between(parser1: Parser<T1>, parser2: Parser<T2>): Parser<T>{
    return parser1.andThenKeepRight(this).andThenKeepLeft(parser2)
}

/**
 * Creates a Parser of a nullable char
 */
fun <T> Parser<T>.asOption() : Parser<Option<T>>{
    val some: Parser<Option<T>> = mapP{ Some(it) }
    val none: Parser<Option<T>> = returnP(None())
    return some orElse none
}

/**
 * Applies the parser, ignores the result and returns value
 */
fun <T, R> Parser<T>.cast(value: R): Parser<R>{
    return this.mapP{_ -> value}
}
