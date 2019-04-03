package DynamicScaleTest



import com.intuit.karate.gatling.PreDef._
import io.gatling.core.Predef._
import io.gatling.core.structure.PopulationBuilder
import mock.MockUtils

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.Source
import scala.util.parsing.json.JSON

class GatlingSimulation extends Simulation {

  MockUtils.startServer(0)

  var inputFile= System.getProperty("ScenarioWorkflow","PerfScenarios.json")
  var path= this.getClass.getResource("/"+inputFile).getPath
  var body = Source.fromFile(path).getLines.mkString
  var jsonParse = JSON.parseFull(body)
  var jsonMap = jsonParse.get.asInstanceOf[Map[String, Any]]
  var featureList = jsonMap.get("scenarios").get.asInstanceOf[List[Map[String,String]]]

  val protocol = karateProtocol(
    "/cats/{id}" -> Nil,
    "/cats" -> pauseFor("get" -> 15, "post" -> 25)
  )
  protocol.nameResolver = (req, ctx) => req.getHeader("karate-name")

  def scenarioList() : Seq[PopulationBuilder]= {
    var seqScn = new mutable.ArraySeq[PopulationBuilder](featureList.size)
    var i = 0
    for(x<-featureList){
      var y =x
      var Scenario = scenario(x("scenarioName"))
        .exec(karateFeature("classpath:"+x("feature").toString))
        .inject( rampUsers(x("noOfUsers").toInt) during(x("Duration").toDouble seconds))//Injects a given number of users with a linear ramp over a given duration.
      seqScn(i) = Scenario
      i=i+1
    }
    seqScn
  }

  setUp(scenarioList: _*).protocols(protocol)
}