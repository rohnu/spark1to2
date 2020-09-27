/*
rules = [
  "Spark1to2Import"
  "Spark1to2Rest"
]
*/

package org.apache.spark.examples



import org.apache.spark.{SparkConf, SparkContext}


object Spark1to2 {

  def main(args: Array[String]) {
    if (args.length < 1) {
      System.err.println("Usage: HdfsTest <file>")
      System.exit(1)
    }
    val spark = new SparkConf().setAppName("HdfsTest")
    val sc = new SparkContext(spark)
    val file = sc.textFile(args(0))
    val mapped = file.map(s => s.length).cache()
        for (iter <- 1 to 10) {
          val start = System.currentTimeMillis()
          for (x <- mapped) { x + 2 }
          val end = System.currentTimeMillis()

    println("Iteration " + iter + " took " + (end-start) + " ms")

    sc.stop()
  }
}
}
