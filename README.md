Scalafix takes care of easy, repetitive and tedious code transformations so you can focus on the changes that truly deserve your attention. In a nutshell, scalafix reads a source file, transforms usage of unsupported features into newer alternatives, and writes the final result back to the original source file. Scalafix aims to automate the migration of as many unsupported features as possible. There will be cases where scalafix is unable to perform automatic migration. In such situations, scalafix will point to the offending source line and provide instructions on how to refactor the code. The objective is that your existing Scala code base gets up and running faster with new Scala versions

# Spark 1.6 to 2.4

CDP ships with Spark 2.4 in both Spark 2.4 on both Private and Public Cloud.

Purpose

```
As we know that the latest release of Spark 2.0 has too much enhancement and new features. If you are using Spark 1.6.4 and now you want to move your application with Spark 2.4.4 that time you have to take care for some changes which happened in the API. In this blog we are going to get an overview of common changes and we are going to achieve the automation using refactoring tool.
```
1)	First noticeable changes are Spark Context which is the entry point to Spark. It is one of the very first objects you create while developing a Spark application. In Spark 2.4, SparkContext constructor has been deprecated hence, the recommendation is to use a static method getOrCreate() with SparkSession API. We can get SparkContext and SqlContext both in the SparkSession 

```
# From 
val spark = new SparkConf().setAppName("HdfsTest")
    val sc = new SparkContext(spark)
    
 To
 val spark = SparkSession.builder.appName("<AppName>").getOrCreate() 
    val sc = spark.sparkContext
    
```

2) DataFrame variable replace with Dataset[Row] : DataFrame is not available in the Spark 2.0, We are using Dataset[Row]. Where ever we are using DataFrame we will replace it with Dataset[Row] for Spark SQL or Dataset[_] for MLIB.

```
Eg. In Spark 1.6 —> import org.apache.spark.sql.DataFrame;
Eg. In Spark 2.4 —> import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
```

# Scalafix rules for My Repo

To develop rule:
```
cd repo-name # The project you want to implement rules for.

sbt new scalacenter/scalafix.g8 --repo="Repository Name"
cd scalafix
sbt tests/test
https://scalacenter.github.io/scalafix/docs/developers/setup.html
```

Import into IntelliJ
```
The project generated should import into IntelliJ like a normal project. Input and output test files are written in plain *.scala files and should have full IDE support.
```
To run scalafix:

If you have the source code for the rule on your local machine, you can run a custom rule using the file:/path/to/NamedLiteralArguments.scala syntax.
```
scalafix --rules=file:/path/to/spark1to2.scala
```

Scalafix 101 video:
```
https://www.youtube.com/watch?v=uaMWvkCJM_E
```
