package com.packt.spark.section2

import com.packt.spark._
import org.apache.spark._
import geotrellis.vector._

import org.apache.spark.storage._
import com.github.nscala_time.time.Imports._

object KeyValueRDDs extends ExampleApp {
  def run() =
    withSparkContext { implicit sc =>
      val neighborhoods = Neighborhoods.fromJson("data/Neighborhoods_Philadelphia.geojson")
      val bcNeighborhoods = sc.broadcast(neighborhoods)

      val timeFilters = 
        Map[String, DateTime => Int](
          ("allTime", { dt => 0 }),
          ("monthOfYear", { dt => dt.getMonthOfYear }),
          ("dayOfMonth", { dt => dt.getDayOfMonth }),
          ("dayOfYear", { dt => dt.getDayOfYear }),
          ("dayOfWeek", { dt => dt.getDayOfWeek }),
          ("hourOfDay", { dt => dt.getHourOfDay })
        )

      val violationsWithNeighborhoods = 
        violations
          .flatMap { violation =>
            bcNeighborhoods.value
              .find(_.geom.contains(violation.location))
              .map { case Feature(_, data) =>
                (violation, data)
              }
           }
          .persist(StorageLevel.MEMORY_ONLY)
//          .persist(StorageLevel.MEMORY_AND_DISK)
//          .persist(StorageLevel.MEMORY_ONLY_SER)
//          .persist(StorageLevel.MEMORY_AND_DISK)


      val densityAggregations =
        timeFilters.map { case (key, groupingFunc) =>
          val neighborhoodViolationDensities =
            violationsWithNeighborhoods
              .map { case (violation, data) =>
                val timeGroup = groupingFunc(violation.issueTime)
                ((data, timeGroup), 1)
               }
              .reduceByKey { (a, b) => a + b }
              .map { case ((NeighborhoodData(name, area), timeGroup), count) =>
                ((name, timeGroup), count / area)
               }
              .collect
              .toMap
          (key, neighborhoodViolationDensities)
        }

      for((timeKey, map) <- densityAggregations) {
        println(timeKey)
        map.foreach { case ((neighborhood, timeGroup), density) =>
          println(s"  $neighborhood $timeGroup   $density")
        }
      }

      waitForUser()
    }

  // def run() =
  //   withSparkContext { implicit sc =>
  //     val neighborhoods = Neighborhoods.fromJson("data/Neighborhoods_Philadelphia.geojson")
  //     val bcNeighborhoods = sc.broadcast(neighborhoods)

  //     val neighborhoodViolationDensities =
  //       fullDataset
  //         .flatMap(Violation.fromRow _)
  //         .flatMap { violationEntry =>
  //           val nbh = bcNeighborhoods.value
  //           nbh
  //             .find(_.geom.contains(violationEntry.location))
  //             .map { case Feature(_, data) =>
  //               (data, 1)
  //              }
  //          }
  //         .reduceByKey { (a, b) => a + b }
  //         .map { case (NeighborhoodData(name, area), count) => 
  //           (count / area, name)
  //          }
  //         .sortByKey()
  //         .collect

  //     neighborhoodViolationDensities
  //       .foreach { case (density, name) =>
  //         println(s"$name   $density")
  //       }

  //     waitForUser()
  //   }
}
