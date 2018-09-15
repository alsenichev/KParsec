import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

import com.senichev.kParsec.result.*
import com.senichev.kParsec.parser.*
import com.senichev.kParsec.json.*


class JsonParserTests {

    @Test fun testNull(){
        val result = jNull().run("null")
        assertEquals("JNull", result.report())
        val result2 = jNull().run("nulp")
        assertEquals("Line:0 Col:3 Error parsing null\nnulp\n   ^Unexpected 'p'", result2.report())
    }

    @Test fun testBool(){
        val result = jBool().run("true")
        assertEquals("JBool(true)", result.report())
        val result2 = jBool().run("false")
        assertEquals("JBool(false)", result2.report())
        val result3 = jBool().run("trux")
        // backtracking issue, trying to parse 'false', because 'true' failed
        assertEquals("Line:0 Col:0 Error parsing bool\ntrux\n^Unexpected 't'", result3.report())
    }

    @Test fun testUnescapedChar(){
        val result = jUnescapedChar().run("a")
        assertEquals("a", result.report())
        val result2 = jUnescapedChar().run("\"")
        assertEquals("Line:0 Col:0 Error parsing char\n\"\n^Unexpected '\"'", result2.report())
    }

    @Test fun testEscapedChar(){
        val result = jEscapedChar().run("\\\\")
        assertEquals("\\", result.report())
        val result2 = jEscapedChar().run("\\t")
        assertEquals("	", result2.report())
        val result3 = jEscapedChar().run("a")
        assertEquals("Line:0 Col:0 Error parsing escaped char\na\n^Unexpected 'a'", result3.report())
    }

    @Test fun testUnicodeChar(){
        val result = jUnicodeChar().run("\\u263A")
        assertEquals("☺", result.report())
    }

    @Test fun testString(){
        assertEquals("JString()", jString().run("\"\"").report())
        assertEquals("JString(a)", jString().run("\"a\"").report())
        assertEquals("JString(ab)", jString().run("\"ab\"").report())
        assertEquals("JString(ab	de)", jString().run("\"ab\\tde\"").report())
        assertEquals("JString(ab☺de)", jString().run("\"ab\\u263Ade\"").report())
    }

    @Test fun testNumber(){
        assertEquals("JNumber(123.0)", jNumber().run("123").report())
        assertEquals("JNumber(-123.0)", jNumber().run("-123").report())
        assertEquals("JNumber(123.4)", jNumber().run("123.4").report())
        assertEquals("JNumber(-123.0)", jNumber().run("-123.").report())   // JNumber -123.0 -- should fail!
        assertEquals("JNumber(0.0)", jNumber().run("00.1").report())    // JNumber 0 -- should fail!
        assertEquals("JNumber(1230000.0)", jNumber().run("123e4").report())
        assertEquals("JNumber(1.234E7)", jNumber().run("123.4e5").report())
        assertEquals("JNumber(0.001234)", jNumber().run("123.4e-5").report())
    }

    @Test fun testNumber_(){
        assertEquals("JNumber(123.0)", jNumber_.run("123").report())
        assertEquals("JNumber(-123.0)", jNumber_.run("-123").report())
        assertEquals("JNumber(123.4)", jNumber_.run("123.4").report())
        assertEquals("Line:0 Col:4 Error parsing number andThen unknown\n-123.\n    ^Unexpected '.'", jNumber_.run("-123.").report())
        assertEquals("Line:0 Col:1 Error parsing number andThen unknown\n00.1\n ^Unexpected '0'", jNumber_.run("00.1").report())
        assertEquals("JNumber(1230000.0)", jNumber_.run("123e4").report())
        assertEquals("JNumber(1.234E7)", jNumber_.run("123.4e5").report())
        assertEquals("JNumber(0.001234)", jNumber_.run("123.4e-5").report())
    }

    @Test fun testArray(){
        assertEquals("JArray([JNumber(1.0), JNumber(2.0)])", jArray().run("[1, 2]").report())
        assertEquals("Line:0 Col:1 Error parsing array\n[, 1, 2]\n ^Unexpected ','", jArray().run("[, 1, 2]").report())
        assertEquals("JArray([JBool(true), JBool(false)])", jArray().run("[true, false]").report())
        assertEquals("JArray([JString(one), JString(two)])", jArray().run("[\"one\",\"two\"]").report())
        assertEquals("JArray([JNull, JNull])", jArray().run("[null, null]").report())
        assertEquals("JArray([JArray([JNumber(1.0), JNumber(2.0)]), JArray([JNumber(-1.0), JNumber(-2.0)])])", jArray().run("[[1,2], [-1, -2]]").report())

    }

    @Test fun testObject(){
        assertEquals("JObject({a=JNumber(1.0), b=JNumber(2.0)})", jObject().run("""{ "a":1, "b" : 2 }""").report())
        assertEquals("Line:0 Col:16 Error parsing object\n{ \"a\":1, \"b\" : 2, }\n                ^Unexpected ','", jObject().run("""{ "a":1, "b" : 2, }""").report())
    }

    @Test fun testJson(){
        val json = """{ "name" : "Aleksey", "isMale" : true, "bday" : {"year":1972, "month":12, "day":21 }, "favouriteColors" : ["blue", "green"] }"""
        assertEquals("JObject({name=JString(Aleksey), isMale=JBool(true), bday=JObject({year=JNumber(1972.0), month=JNumber(12.0), day=JNumber(21.0)}), favouriteColors=JArray([JString(blue), JString(green)])})",
                parseJson(json).report())
    }

    @Test fun testJson2(){
        val json =
                """{
        	"widget": {
                "debug": "on", "window": {
                	"title": "Sample Konfabulator Widget", "name": "main_window", "width": 500, "height": 500
                    },
                    "image": {
                    	"src": "Images/Sun.png", "name": "sun1", "hOffset": 250, "vOffset": 250, "alignment": "center"
                    },
                    "text": {
                        "data": "Click Here", "size": 36, "style": "bold", "name": "text1", "hOffset": 250, "vOffset": 100, "alignment": "center", "onMouseUp": "sun1.opacity = (sun1.opacity / 100) * 90;" }
                        }
            } """
        val expected = "JObject({widget=JObject({debug=JString(on), window=JObject({title=JString(Sample Konfabulator Widget), name=JString(main_window), width=JNumber(500.0), height=JNumber(500.0)}), image=JObject({src=JString(Images/Sun.png), name=JString(sun1), hOffset=JNumber(250.0), vOffset=JNumber(250.0), alignment=JString(center)}), text=JObject({data=JString(Click Here), size=JNumber(36.0), style=JString(bold), name=JString(text1), hOffset=JNumber(250.0), vOffset=JNumber(100.0), alignment=JString(center), onMouseUp=JString(sun1.opacity = (sun1.opacity / 100) * 90;)})})})"
        assertEquals(expected, parseJson(json).report())
    }
}