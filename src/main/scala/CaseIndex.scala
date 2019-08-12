import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.xml.XML
import org.json.JSONObject
import org.json.{XML => JXML}
import java.io._
import scalaj.http.Http
import org.json4s._
//import org.json4s.native.JsonMethods._
import org.json4s.jackson.JsonMethods._
import scala.collection.mutable.{Set => mSet}
import scala.collection.mutable.{Map => mMap}
import scala.collection.mutable.ListBuffer


object CaseIndex {
    def main(args: Array[String]) {
		// for test 
		// val tem = "/home/moko/Documents/comp9313/ass3/cases_test"
		
		// get list of files 
		val files = (new File(args(0))).listFiles.filter { f => f.isFile && (f.getName.endsWith(".xml")) }.map(_.getAbsolutePath)
		implicit val formats = DefaultFormats
		// create index and mapping 
		val es_create = Http("http://localhost:9200/legal_idx").method("PUT").header("Content-Type", "application/json").timeout(connTimeoutMs = 1000000, readTimeoutMs = 5000000).asString
		val es_mapping = Http("http://localhost:9200/legal_idx/cases/_mapping?pretty").postData("""{"cases":{"properties":{"id":{"type":"text"},"name":{"type":"text"},"AustLII":{"type":"text"},"catchphrase":{"type":"text"},"sentence":{"type":"text"},"person":{"type":"text"},"location":{"type":"text"},"organization":{"type":"text"}}}}""").method("PUT").header("Content-Type", "application/json").timeout(connTimeoutMs = 1000000, readTimeoutMs = 5000000).asString

		// for higher elasticSearch version
		// val es_mapping_ = Http("http://localhost:9200/legal_idx/cases/_mapping?include_type_name=true").postData("""{"cases":{"properties":{"id":{"type":"text"},"name":{"type":"text"},"AustLII":{"type":"text"},"catchphrase":{"type":"text"},"sentence":{"type":"text"},"person":{"type":"text"},"location":{"type":"text"},"organization":{"type":"text"}}}}""").method("PUT").header("Content-Type", "application/json").timeout(connTimeoutMs = 1000000, readTimeoutMs = 5000000).asString

		for(x<-files){
			println(x) // print out all files
			// 	for test val x = "/home/moko/Documents/comp9313/ass3/cases_test/06_14.xml"
			// load xml file 
			val xmlloaded = scala.xml.XML.loadFile(x)
			// get the name of file without extension
			val filename = x.split("\\/").last.split("\\.")(0)
			//println(filename)
			// get each tag's information
			val name = (xmlloaded \ "name").text.filter(_ >= ' ')
			val url = (xmlloaded \ "AustLII").text.filter(_ >= ' ')
			val catchphrase = new ListBuffer[String]()
			for(x<-(xmlloaded \ "catchphrases" \ "catchphrase")){
				catchphrase += x.text
			}
			val sentence = new ListBuffer[String]()
			for(x<-(xmlloaded \ "sentences" \ "sentence")){
				sentence += x.text
			}

			// store the enriched entities 
			var organization = mSet[String]()
			var location = mSet[String]()
			var person = mSet[String]()

			// for loop http request to corenlp server
			for(x<-sentence){
				val nlp = Http("http://localhost:9000/").params("annotators"->"ner","outputFormat"->"json").postData(x.toString).timeout(connTimeoutMs = 1000000, readTimeoutMs = 5000000).asString.body
				// parse the respose
				val nlp_json = parse(nlp)
				// println(nlp_json)
				println("-----",x)
				// get the ner and word tag's information
				val tokens = (nlp_json \ "sentences"\ "tokens").asInstanceOf[JArray].arr(0)
				//println("&&&&&&&&",entitymention.arr.length)
				val nnn = (tokens \"ner").asInstanceOf[JArray]
				val ttt = (tokens \"word").asInstanceOf[JArray]
				// choose the ner tag equals LOCATION or PERSON or ORGANIZATION
				println("nnn,ttt")
				for(y<-0 until nnn.arr.length){
					if ((nnn.arr(y)).extract[String] == "LOCATION") {
						println("loc")
						location += (ttt.arr(y)).extract[String]
					}else if ((nnn.arr(y)).extract[String] == "PERSON") {
						println("per")
						person += (ttt.arr(y)).extract[String]
					}else if ((nnn.arr(y)).extract[String] == "ORGANIZATION") {
						println("org")
						organization += (ttt.arr(y)).extract[String]
					}
				}
			}

			// new PrintWriter("nlp.txt") { write(nlp); close }
			// println(organization)
			// println(location)
			// println(person)

			// use JSONObject to put new information in the json string
			val xmlJSONObj:JSONObject = JXML.toJSONObject(xmlloaded.toString)
			xmlJSONObj.getJSONObject("case").put("id",filename)
			xmlJSONObj.getJSONObject("case").put("location",location.toArray)
			xmlJSONObj.getJSONObject("case").put("person",person.toArray)
			xmlJSONObj.getJSONObject("case").put("organization",organization.toArray)
			
			xmlJSONObj.getJSONObject("case").put("sentences",sentence.toArray)
			xmlJSONObj.getJSONObject("case").put("catchphrases",catchphrase.toArray)

			// leave out the  "{ "case":" in the beginning and "}" in the end
			val rstart = "^\\{\"case\":"
			val rend = "\\}$"
			// send to elasticSearch server
			val saveInElasticSearch = Http("http://localhost:9200/legal_idx/cases/"+filename+"?pretty").postData(xmlJSONObj.toString.replace("\\n"," ").replaceAll(rstart,"").replaceAll(rend,"")).method("PUT").header("Content-Type", "application/json").timeout(connTimeoutMs = 1000000, readTimeoutMs = 5000000).asString

		}
	}
}
