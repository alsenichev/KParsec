import org.junit.*

import com.senichev.kParsec.parser.*
import com.senichev.kParsec.standardParsers.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PartOneTests(){

    @Test fun testCharParserSuccess(){
        val parseA = pchar('A')
        val input = "ABC"
        val result = parseA.run(input)
        assertEquals("Success(value=A, remaining=InputState(lines=[ABC], position=Position(line=0, column=1)))", result.toString())
    }



}