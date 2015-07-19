package com.packt.spark

import org.apache.spark._
import org.apache.spark.rdd._

trait ExampleApp {
  def withSparkContext(f: SparkContext => Unit) : Unit = {
    val conf = 
      new SparkConf()
        .setMaster("local[4]")
        .setAppName("Coding with Spark")
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .set("spark.kryo.registrator", "com.packt.spark.KryoRegistrator")

    val sc = new SparkContext(conf)
    
    try {
      f(sc)
    } finally {
      sc.stop()
    }
  }

  def waitForUser(): Unit = {
    println("Press enter to continue..." )
    Console.readLine
  }

  def csvDataset(path: String)(implicit sc: SparkContext): RDD[String] = {
    val absPath = new java.io.File(path).getAbsolutePath
    sc.textFile(s"file://$absPath")
  }

  def sampleDataset(implicit sc: SparkContext) =
    csvDataset("./data/Parking_Violations-sample.csv")

  def fullDataset(implicit sc: SparkContext) =
    csvDataset("./data/Parking_Violations.csv")

  def main(args: Array[String]): Unit =
    run()

  def run(): Unit
}
