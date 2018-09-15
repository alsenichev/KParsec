package com.senichev.kParsec.Result

import com.senichev.kParsec.textInput.InputState
import com.senichev.kParsec.textInput.ParserPosition

/**
 * Represents the result of a parsing operation.
 */
sealed class Result<out T>

/**
 * Parsing succeed.
 */
internal data class Success<T>(val value: T, val inputState: InputState) : Result<T>()

/**
 * Parsing failed.
 */
internal data class Failure<T>(val label: String, val message: String, val position: ParserPosition): Result<T>()

/**
 *  Helper function to display the result of a parse.
 */
internal fun  Result<*>.report (): String{
    return when(this){
        is Success -> "$value"
        is Failure -> {
            val errorLine = position.currentLine
            val colPos = position.column
            val linePos = position.line
            val shift = CharArray(colPos){' '}.joinToString(separator = "")
            val failureCaret = "$shift^$message"
            "Line:$linePos Col:$colPos Error parsing $label\n$errorLine\n$failureCaret"
        }
    }
}