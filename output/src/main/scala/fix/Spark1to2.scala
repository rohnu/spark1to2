/*
rules = [
  "Spark1to2Import"
  "Spark1to2Rest"
]
*/

package org.apache.spark.examples



import org.apache.spark.sql.SparkSession

object Spark1to2 {

  def main(args: Array[String]) {
    if (args.length < 1) {
      System.err.println("Usage: HdfsTest <file>")
      System.exit(1)
    }
    val spark = SparkSession.builder.appName("<AppName>").enableHiveSupport().getOrCreate() // Edit your App Name, if usingSparkSession.builder.appName("<HiveAppName>").config("spark.sql.warehouse.dir", warehouseLocation).enableHiveSupport().getOrCreate() // Edit your Spark-Hive App Name, if using.("HdfsTest")
    val sc = spark.sparkContext
    import spark.sql
    val file = sc.textFile(args(0))
    sql("CREATE EXTERNAL TABLE MY_SPARK_TABLE(ID STRING) LOCATION($file_path)").show(100,false)
    val mapped = file.map(s => s.length).cache()
    for (iter <- 1 to 10) {
      val start = System.currentTimeMillis()
      for (x <- mapped) { x + 2 }
      val end = System.currentTimeMillis()

      println("Iteration " + iter + " took " + (end-start) + " ms")//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)//Println Syntax can be used as like  println(s(println statement)

      sc.stop()
    }

  }
}