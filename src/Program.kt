import com.senichev.kParsec.json.parseJson
import com.senichev.kParsec.result.report

fun main(args: Array<String>){
    val json = """{
        | "name" : "Aleksey",
        |  "isMale" : true3,
        |  "bday" : {"year":1972, "month":12, "day":21 },
        |   "favouriteColors" : ["blue", "green"] }""".trimMargin()

    val result = parseJson(json).report()
    print(result)
}