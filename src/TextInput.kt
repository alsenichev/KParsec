package com.senichev.kParsec.textInput

import kotlin.coroutines.experimental.*

/**
 * The position of the char in a text.
 */
internal data class Position(val line: Int = 0, val column: Int = 0){
    fun incrementColumn() = copy(column = column + 1)
    fun incrementLine() = Position(line = line + 1, column = 0)
}

/**
 * The immutable state of a text being parsed.
 */
data class InputState(val lines: List<String>, val position: Position){

    /**
     * Creates an InputState from a string.
     */
    companion object {
        // Creates an InputState from a string.
        fun fromString(input: String) : InputState{
            if(input.isEmpty()){
                return InputState(listOf(), Position())
            }else{
                val lines = input.split("\r\n", "\n")
                return InputState(lines, Position())
            }
        }
    }

    /**
     * Returns the current line.
     */
    // the expression after 'else' is a hack, because in some cases (for example 'satisfy' function)
    // treats it as a content and even tries to match some chars from it (for example jNumber_)
    fun currentLine() =  if(position.line < lines.size) lines[position.line] else "End of file"

    /**
     * Gets the next char from the input, if any, else returns null. Also return the updated InputState.
     */
    fun nextChar(): Pair<InputState, Char?>{
        val linePos = position.line
        val colPos = position.column
        if (linePos >= lines.size){
            // EOF
            return Pair(this, null)
        }else{
            val currentLine = currentLine()
            // not reached the end of the line
            if (colPos < currentLine.length){
                // return char at colPos, increment colPos
                val char = currentLine[colPos]
                val newPos = position.incrementColumn()
                val newState = copy(position = newPos)
                return Pair(newState, char)
            } else{
                // end of line, so return LF and move to next line
                val char = '\n'
                val newPos = position.incrementLine()
                val newState = copy(position = newPos)
                return Pair(newState, char)
            }
        }
    }

    /**
     * Returns the list of chars in this InputState.
     */
    fun readAllChars() = readAllCharsRec(this).toList()

    /**
     * Reads chars one by one recursively using .nextChar().
     */
    private fun readAllCharsRec(inputState: InputState): Sequence<Char> = buildSequence{
        val (remainingInput, charOpt) = inputState.nextChar()
        if(charOpt == null){
            yieldAll(emptySequence())
        }else{
            yield(charOpt!!)
            yieldAll(inputState.readAllCharsRec(remainingInput))
        }
    }
}

/**
 * Represents a position of the parser in the parsed text.
 */
internal data class ParserPosition(val currentLine: String, val line: Int, val column: Int){
    /**
     * Creates a ParserPosition from an InputState.
     */
    companion object{
        fun fromInputState(inputState: InputState): ParserPosition{
            val currentLine = inputState.currentLine()
            val line = inputState.position.line
            val column = inputState.position.column
            return ParserPosition(currentLine, line, column)
        }
    }
}