package com.packt.spark.section2

import com.packt.spark._
import org.apache.spark._

import geotrellis.vector._
import geotrellis.vector.io.json._
import com.github.nscala_time.time.Imports._

object Serialization extends ExampleApp {
  class TimeDelta(start: DateTime) {
    def apply(other: DateTime): Long =
      other.getMillis - start.getMillis
  }

  def run() =
    withSparkContext { implicit sc =>
      val bcTimeDelta = sc.broadcast(new TimeDelta(new DateTime(2005, 1, 1, 0, 0, 0)))

      val maxTime =
        sampleDataset
          .flatMap(Violation.fromRow _)
          .map { v => bcTimeDelta.value(v.issueTime) }
          .max

      println(s"maxTime: ${maxTime}.")
    }
}
