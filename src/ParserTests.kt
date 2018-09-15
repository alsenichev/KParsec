import com.senichev.kParsec.Result.report
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
        assertEquals("Failure(label=A andThen B, message=Unexpected 'Z', position=ParserPosition(currentLine=ZBC, line=0, column=0))",
                parseAThenB.run("ZBC").toString())
        assertEquals("Failure(label=A andThen B, message=Unexpected 'Z', position=ParserPosition(currentLine=AZC, line=0, column=1))",
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
        assertEquals("Failure(label=A andThen B orElse C, message=Unexpected 'Q', position=ParserPosition(currentLine=QBZ, line=0, column=0))",
                parseAThenBOrC.run("QBZ").toString())
        // the message is not quite correct, it will be improved in the later functions
        assertEquals("Failure(label=A andThen B orElse C, message=Unexpected 'Q', position=ParserPosition(currentLine=AQZ, line=0, column=1))",
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

    @Test fun testParseThreeDigits(){
        val parseDigit = digitChar()
        val tupleParser = parseDigit.andThen(parseDigit).andThen(parseDigit)
        fun <T> transformTuple(input: Pair<Pair<T, T>, T>) =
                listOf(input.first.first, input.first.second, input.second).joinToString(separator="")
        val flatParser = tupleParser.mapP{transformTuple(it)}
        val result =flatParser.run( "123A")
        assertEquals("Success(value=123, inputState=InputState(lines=[123A], position=Position(line=0, column=3)))", result.toString())
        val result2 =flatParser.run( "12A3")
        assertEquals("Failure(label=digit andThen digit andThen digit, message=Unexpected 'A', position=ParserPosition(currentLine=12A3, line=0, column=2))", result2.toString())
    }

    @Test fun testParseThreeDigitsWithInt(){
        val parseDigit = digitChar()
        val tupleParser = parseDigit.andThen(parseDigit).andThen(parseDigit)
        fun <T> transformTuple(input: Pair<Pair<T, T>, T>) =
                listOf(input.first.first, input.first.second, input.second).joinToString(separator="")
        val flatParser:Parser<Int> = tupleParser.mapP{transformTuple(it)}.mapP{it.toInt()}
        val result =flatParser.run( "123A")
        assertEquals("Success(value=123, inputState=InputState(lines=[123A], position=Position(line=0, column=3)))", result.toString())
        val result2 =flatParser.run( "12A3")
        assertEquals("Failure(label=digit andThen digit andThen digit, message=Unexpected 'A', position=ParserPosition(currentLine=12A3, line=0, column=2))", result2.toString())
    }

    @Test fun testAddition(){
        val parseDigit: Parser<Int> = anyOf(listOf('0'..'9').flatMap{it}).mapP{it.toInt()-'0'.toInt()}
        val add: Parser<Int> = lift2(parseDigit, parseDigit){a, b -> a + b}
        val result =add.run( "12A")
        assertEquals("Success(value=3, inputState=InputState(lines=[12A], position=Position(line=0, column=2)))", result.toString())
        val result2 =add.run( "1A2")
        assertEquals("Failure(label=anyOf [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], message=Unexpected 'A', position=ParserPosition(currentLine=1A2, line=0, column=1))", result2.toString())
    }

    @Test fun testStartsWith(){
        val stringParser = returnP("Hello")
        val letterParser = pchar('H').mapP{it.toString()}
        val startsWith = lift2(stringParser, letterParser, {str, pref -> str.startsWith(pref)})
        val result =startsWith.run( "Hello")
        assertEquals("Success(value=true, inputState=InputState(lines=[Hello], position=Position(line=0, column=1)))", result.toString())
    }

    @Test fun testSequence(){
        val parsers = listOf(pchar('A'), pchar('B'), pchar('C'))
        val combined = parsers.toSequence()
        val result =combined.run( "ABCD")
        assertEquals("Success(value=[A, B, C], inputState=InputState(lines=[ABCD], position=Position(line=0, column=3)))", result.toString())
    }

    @Test fun testSequenceWithASingleElement(){
        val parsers = listOf(pchar('A'))
        val combined = parsers.toSequence()
        val result =combined.run( "ABC")
        assertEquals("Success(value=[A], inputState=InputState(lines=[ABC], position=Position(line=0, column=1)))", result.toString())
    }

    @Test fun testPString(){
        val parseABC = pstring("ABC")
        val result = parseABC.run("ABCDE")
        assertEquals("Success(value=ABC, inputState=InputState(lines=[ABCDE], position=Position(line=0, column=3)))", result.toString())
        val result2 = parseABC.run("A|CDE")
        assertEquals("Failure(label=ABC, message=Unexpected '|', position=ParserPosition(currentLine=A|CDE, line=0, column=1))", result2.toString())
        val result3 = parseABC.run("AB|DE")
        assertEquals("Failure(label=ABC, message=Unexpected '|', position=ParserPosition(currentLine=AB|DE, line=0, column=2))", result3.toString())
    }

    @Test fun testMany(){
        val manyAB = many(pstring("AB"))
        val result = manyAB.run("ABCD")
        assertEquals("Success(value=[AB], inputState=InputState(lines=[ABCD], position=Position(line=0, column=2)))", result.toString())
        val result2 = manyAB.run("ABABCD")
        assertEquals("Success(value=[AB, AB], inputState=InputState(lines=[ABABCD], position=Position(line=0, column=4)))", result2.toString())
        val result3 = manyAB.run("ZCD")
        assertEquals("Success(value=[], inputState=InputState(lines=[ZCD], position=Position(line=0, column=0)))", result3.toString())
        val result4 = manyAB.run("AZCD")
        assertEquals("Success(value=[], inputState=InputState(lines=[AZCD], position=Position(line=0, column=0)))", result4.toString())
    }

    @Test fun testManyOnWhitespace(){
        val chars = anyOf(listOf(' ', '\t', '\n'))
        val parser = many(chars)
        val result =parser.run( "ABC")
        assertEquals("[]", result.report())
        val result1 =parser.run( " ABC")
        assertEquals("[ ]", result1.report())
        val result2 =parser.run( "\tABC")
        assertEquals("[	]", result2.report())
    }

    @Test fun testMany1(){
        val digit = anyOf(listOf('0'..'9').flatMap{it})
        val digits = many1(digit)
        val result = digits.run("1ABC")
        assertEquals("[1]", result.report())
        val result2 = digits.run("12BC")
        assertEquals("Success(value=[1, 2], inputState=InputState(lines=[12BC], position=Position(line=0, column=2)))", result2.toString())
        val result3 = digits.run("123C")
        assertEquals("Success(value=[1, 2, 3], inputState=InputState(lines=[123C], position=Position(line=0, column=3)))", result3.toString())
        val result4 = digits.run("1234")
        assertEquals("Success(value=[1, 2, 3, 4], inputState=InputState(lines=[1234], position=Position(line=0, column=4)))", result4.toString())
        val result5 = digits.run("ABC")
        assertEquals("Failure(label=anyOf [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], message=Unexpected 'A', position=ParserPosition(currentLine=ABC, line=0, column=0))", result5.toString())
    }

    @Test fun testPInt(){
        val digits = pint()
        val result = digits.run("1ABC")
        assertEquals("Success(value=1, inputState=InputState(lines=[1ABC], position=Position(line=0, column=1)))", result.toString())
        val result2 = digits.run("12BC")
        assertEquals("Success(value=12, inputState=InputState(lines=[12BC], position=Position(line=0, column=2)))", result2.toString())
        val result3 = digits.run("-123C")
        assertEquals("Success(value=-123, inputState=InputState(lines=[-123C], position=Position(line=0, column=4)))", result3.toString())
        val result4 = digits.run("1234")
        assertEquals("Success(value=1234, inputState=InputState(lines=[1234], position=Position(line=0, column=4)))", result4.toString())
        val result5 = digits.run("ABC")
        assertEquals("Failure(label=integer, message=Unexpected 'A', position=ParserPosition(currentLine=ABC, line=0, column=0))", result5.toString())
    }

    @Test fun testOpt(){
        val digit = anyOf(listOf('0'..'9').flatMap{it})
        val digitThenSemicolon = digit.andThen(pchar(';').asOption())
        val result  =digitThenSemicolon.run( "1;")
        assertEquals("Success(value=(1, Some(;)), inputState=InputState(lines=[1;], position=Position(line=0, column=2)))", result.toString())
        val result2 =digitThenSemicolon.run( "1")
        assertEquals("Success(value=(1, None), inputState=InputState(lines=[1], position=Position(line=0, column=1)))", result2.toString())
    }

    @Test fun testAndThenKeepLeft(){
        val digit = anyOf(listOf('0'..'9').flatMap{it})
        val digitThenSemicolon = digit.andThenKeepLeft(pchar(';').asOption())
        val result  =digitThenSemicolon.run( "1;")
        assertEquals("Success(value=1, inputState=InputState(lines=[1;], position=Position(line=0, column=2)))", result.toString())
        val result2 =digitThenSemicolon.run( "1")
        assertEquals("Success(value=1, inputState=InputState(lines=[1], position=Position(line=0, column=1)))", result2.toString())
    }

    @Test fun testAndThenKeepLeftWithWhitespaces(){
        val wsp = many1(pchar(' '))
        val ab = pstring("AB")
        val cd = pstring("CD")
        val ab_cd = ab.andThenKeepLeft(wsp).andThen(cd)
        val result =ab_cd.run( "AB   CD")
        assertEquals("Success(value=(AB, CD), inputState=InputState(lines=[AB   CD], position=Position(line=0, column=7)))", result.toString())
        val result1 =ab_cd.run( "ABCD")
        assertEquals("Failure(label=unknown andThen CD, message=Unexpected 'C', position=ParserPosition(currentLine=ABCD, line=0, column=2))", result1.toString())
    }

    @Test fun testBetween(){
        val pdq = pchar('"')
        val quotedInteger = pint().between(pdq, pdq)
        val result =quotedInteger.run( "\"1234\"")
        assertEquals("Success(value=1234, inputState=InputState(lines=[\"1234\"], position=Position(line=0, column=6)))", result.toString())
        val result2 =quotedInteger.run( "1234")
        assertEquals("Failure(label=unknown andThen \", message=Unexpected '1', position=ParserPosition(currentLine=1234, line=0, column=0))", result2.toString())
    }

    @Test fun testSepBy1(){
        val comma = pchar(',')
        val digit = anyOf(listOf('0'..'9').flatMap{it})
        val resultP = sepBy1(digit, comma)
        val result = resultP.run("1;")
        assertEquals("Success(value=[1], inputState=InputState(lines=[1;], position=Position(line=0, column=1)))", result.toString())
        val result1 = resultP.run("1,2;")
        assertEquals("Success(value=[1, 2], inputState=InputState(lines=[1,2;], position=Position(line=0, column=3)))", result1.toString())
        val result2 = resultP.run("1,2,3;")
        assertEquals("Success(value=[1, 2, 3], inputState=InputState(lines=[1,2,3;], position=Position(line=0, column=5)))", result2.toString())
        val result3 = resultP.run("Z;")
        assertEquals("Failure(label=anyOf [0, 1, 2, 3, 4, 5, 6, 7, 8, 9] andThen many unknown, message=Unexpected 'Z', position=ParserPosition(currentLine=Z;, line=0, column=0))", result3.toString())
    }

    @Test fun testSepBy(){
        val comma = pchar(',')
        val digit = anyOf(listOf('0'..'9').flatMap{it})
        val resultP = sepBy(digit, comma)
        val result = resultP.run("1;")
        assertEquals("Success(value=[1], inputState=InputState(lines=[1;], position=Position(line=0, column=1)))", result.toString())
        val result1 = resultP.run("1,2;")
        assertEquals("Success(value=[1, 2], inputState=InputState(lines=[1,2;], position=Position(line=0, column=3)))", result1.toString())
        val result2 = resultP.run("1,2,3;")
        assertEquals("Success(value=[1, 2, 3], inputState=InputState(lines=[1,2,3;], position=Position(line=0, column=5)))", result2.toString())
        val result3 = resultP.run("Z;")
        assertEquals("Success(value=[], inputState=InputState(lines=[Z;], position=Position(line=0, column=0)))", result3.toString())
    }

}