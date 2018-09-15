import org.junit.*

import com.senichev.kParsec.parser.*
import com.senichev.kParsec.standardParsers.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ParserTests(){

    @Test fun testCharParserSuccess(){
        val parseA = pchar('A')
        val result = parseA.run("ABC")
        assertEquals("Success(value=A, inputState=InputState(lines=[ABC], position=Position(line=0, column=1)))",
                result.toString())
    }

    @Test fun testCharParserFailure(){
        val parseA = pchar('A')
        assertEquals("Failure(label=A, message=Unexpected 'Z', position=ParserPosition(currentLine=ZBC, line=0, column=0))",
                parseA.run("ZBC").toString())
    }

    @Test fun testAndThenSuccess(){
        val parseA = pchar('A')
        val parseB = pchar('B')
        val parseAThenB = parseA.andThen(parseB)
        assertEquals("Success(value=(A, B), inputState=InputState(lines=[ABC], position=Position(line=0, column=2)))",
                parseAThenB.run("ABC").toString())
    }

    @Test fun testAndThenFailure(){
        val parseA = pchar('A')
        val parseB = pchar('B')
        val parseAThenB = parseA.andThen(parseB)
        assertEquals("Failure(label=A andThen B.label, message=Unexpected 'Z', position=ParserPosition(currentLine=ZBC, line=0, column=0))",
                parseAThenB.run("ZBC").toString())
        assertEquals("Failure(label=A andThen B.label, message=Unexpected 'Z', position=ParserPosition(currentLine=AZC, line=0, column=1))",
                parseAThenB.run("AZC").toString())
    }

    @Test fun testOrElse(){
        val parseA = pchar('A')
        val parseB = pchar('B')
        val parseAOrB = parseA.orElse(parseB)
        assertEquals("Success(value=A, inputState=InputState(lines=[AZZ], position=Position(line=0, column=1)))",
                parseAOrB.run("AZZ").toString())
        assertEquals("Success(value=B, inputState=InputState(lines=[BZZ], position=Position(line=0, column=1)))",
                parseAOrB.run("BZZ").toString())
        assertEquals("Failure(label=B, message=Unexpected 'C', position=ParserPosition(currentLine=CZZ, line=0, column=0))",
                parseAOrB.run("CZZ").toString())
    }

    @Test fun testAndThenOrElse(){
        val parseA = pchar('A')
        val parseB = pchar('B')
        val parseC = pchar('C')
        val parseBOrC = parseB.orElse(parseC)
        val parseAThenBOrC = parseA.andThen(parseBOrC)
        assertEquals("Success(value=(A, B), inputState=InputState(lines=[ABZ], position=Position(line=0, column=2)))",
                parseAThenBOrC.run("ABZ").toString())
        assertEquals("Success(value=(A, C), inputState=InputState(lines=[ACZ], position=Position(line=0, column=2)))",
                parseAThenBOrC.run("ACZ").toString())
        assertEquals("Failure(label=A andThen B orElse C.label.label, message=Unexpected 'Q', position=ParserPosition(currentLine=QBZ, line=0, column=0))",
                parseAThenBOrC.run("QBZ").toString())
        // the message is not quite correct, it will be improved in the later functions
        assertEquals("Failure(label=A andThen B orElse C.label.label, message=Unexpected 'Q', position=ParserPosition(currentLine=AQZ, line=0, column=1))",
                parseAThenBOrC.run("AQZ").toString())
    }

    @Test fun testChoice(){
        val parseA = pchar('A')
        val parseB = pchar('B')
        val parseC = pchar('C')
        val parseAOrBOrC = choice(listOf(parseA, parseB, parseC))
        assertEquals("Success(value=A, inputState=InputState(lines=[AZM], position=Position(line=0, column=1)))",
                parseAOrBOrC.run("AZM").toString())
        assertEquals("Success(value=B, inputState=InputState(lines=[BZM], position=Position(line=0, column=1)))",
                parseAOrBOrC.run("BZM").toString())
        assertEquals("Success(value=C, inputState=InputState(lines=[CZM], position=Position(line=0, column=1)))",
                parseAOrBOrC.run("CZM").toString())
        assertEquals("Failure(label=C, message=Unexpected 'M', position=ParserPosition(currentLine=MZZ, line=0, column=0))",
                parseAOrBOrC.run("MZZ").toString())
    }

    @Test fun testParseLowerCase(){
        val parseLowerCase = anyOf(listOf('a'..'z').flatMap{it})
        assertEquals("Success(value=a, inputState=InputState(lines=[aBC], position=Position(line=0, column=1)))",
                parseLowerCase.run("aBC").toString())
        assertEquals("Failure(label=anyOf [a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z], message=Unexpected 'A', position=ParserPosition(currentLine=ABC, line=0, column=0))",
                parseLowerCase.run("ABC").toString())
    }

    @Test fun testParseDigit(){
        val parseDigit = digitChar()
        assertEquals("Success(value=1, inputState=InputState(lines=[1BC], position=Position(line=0, column=1)))",
                parseDigit.run("1BC").toString())
        assertEquals("Success(value=9, inputState=InputState(lines=[9BC], position=Position(line=0, column=1)))",
                parseDigit.run("9BC").toString())
        assertEquals("Failure(label=digit, message=Unexpected '|', position=ParserPosition(currentLine=|ABC, line=0, column=0))",
                parseDigit.run("|ABC").toString())
    }

}