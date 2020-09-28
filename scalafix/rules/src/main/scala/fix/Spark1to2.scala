
package fix

import scalafix.v1._

import scala.meta._

object SelectField {

  def unapply(tree: Tree): Option[Vector[String]] =
    tree match {
      case Term.Select(Term.Name(">"), Term.Name(arg))                                 => Some(Vector(arg))
      case Term.Select(Term.Select(SelectField(args), Term.Name(">")), Term.Name(arg)) => Some(args :+ arg)
      case _                                                                           => None
    }
}

object PathFrom {

  def unapply(tree: Tree): Option[(String, Vector[String])] =
    tree match {
      case Term.Select(Term.Select(Term.Name(pos), Term.Name(">")), Term.Name(arg)) =>
        Some((pos, Vector(arg)))
      case Term.Select(Term.Select(PathFrom(pos, args), Term.Name(">")), Term.Name(arg)) =>
        Some((pos, args :+ arg))
      case _ => None
    }
}

class Spark1to2Path extends SemanticRule("Spark1to2Path") {
  override def fix(implicit doc: SemanticDocument): Patch = {

    val pathRewrite: Seq[Patch] = doc.tree
      .collect({
        case s @ SelectField(args) =>
          (s.pos, Patch.replaceTree(s, args.mkString("path\"", ".", "\"")))

        case p @ PathFrom(root, args) =>
          (p.pos, Patch.replaceTree(p, args.mkString("path\"$" + root + ".", ".", "\"")))
      })
      .groupBy(_._1.start)
      .map(_._2.maxBy(_._1.text.length)._2)
      .toSeq

    pathRewrite.fold(Patch.empty)(_ + _)
  }

}

class Spark1to2Import extends SemanticRule("Spark1to2Import") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    val patches: Seq[Seq[Patch]] = doc.tree.collect({
      case i @ Importer(ref, xs) if xs.toString.contains("SparkConf") || xs.toString.contains("{SparkConf, SparkContext}") || xs.toString.contains("SparkContext")=>
        xs.map(Patch.removeImportee)
    })

    if (patches.nonEmpty) {
      patches.flatten.reduce(_ + _) +
        Patch.addGlobalImport(importer"org.apache.spark.sql.SparkSession")
    } else Patch.empty
  }
}

case class Println(position: Position) extends Diagnostic {
  def message = "Println Syntax : println(s\"<your message>\")"
}


class Spark1to2Println extends SemanticRule("Spark1to2Println") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val patches: Seq[Seq[Patch]] = doc.tree.collect ({
     /*case println @ Term.Apply(Term.Name("println"), _) =>
        Seq(Patch.lint(Println(println.pos)))*/

      case pl @ Term.Apply(Term.Name("println"), _) =>
        Seq(Patch.addRight(pl,s" //Println Syntax can be used as like  println(s\"<your message>\")"))
    })
    patches.flatten.fold(Patch.empty)(_ + _)
  }
}


class Spark1to2Session extends SemanticRule("Spark1to2Session") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    //println("Tree.structureLabeled: " + doc.tree.structureLabeled)

    val patches: Seq[Seq[Patch]] = doc.tree.collect({


      case  n @ Term.New(Init(t,name,arg)) if  t.toString.contains("SparkConf") =>
        Seq(Patch.replaceTree(n, "SparkSession.builder.appName(\"<AppName>\").getOrCreate() // Edit your App Name, if using"))


      case  t @ Term.Name("setAppName") =>
        Seq(Patch.removeTokens(t.tokens))

      case  n @ Term.New(Init(t,name,arg)) if  t.toString.contains("SparkContext") =>
        Seq(Patch.replaceTree(n, "spark"))

      case  p @ Pat.Var(Term.Name("sparkConf")) =>
      Seq(Patch.replaceTree(p, "spark"))

      case  t @ Term.Name("textFile") =>
        Seq(Patch.replaceTree(t, "read.text(\"<file_arg>\").rdd // Include File_Argument if any "))

      case  t @ Term.Name("parallelize") =>
        Seq(Patch.replaceTree(t, "sparkContext.parallelize"))
    })
    //println("Tree.syntax: " + doc.tree.syntax)
    //println("Tree.structure: " + doc.tree.structure)


    patches.flatten.fold(Patch.empty)(_ + _)
  }

}

class Spark1to2HiveSession extends SemanticRule("Spark1to2HiveSession") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    //println("Tree.structureLabeled: " + doc.tree.structureLabeled)

    val patches: Seq[Seq[Patch]] = doc.tree.collect({


      case  n @ Term.New(Init(t,name,arg)) if  t.toString.contains("SparkConf") =>
        Seq(Patch.replaceTree(n, "SparkSession.builder.appName(\"<HiveAppName>\").config(\"spark.sql.warehouse.dir\", warehouseLocation).enableHiveSupport().getOrCreate() // Edit your Spark-Hive App Name, if using"))


      case  t @ Term.Name("setAppName") =>
        Seq(Patch.removeTokens(t.tokens))

      case  n @ Term.New(Init(t,name,arg)) if  t.toString.contains("SparkContext") =>
        Seq(Patch.replaceTree(n, "spark"))

      case  n @ Term.New(Init(t,name,arg)) if  t.toString.contains("HiveContext") =>
        Seq(Patch.replaceTree(n, "spark"))

      case  p @ Pat.Var(Term.Name("sparkConf")) =>
        Seq(Patch.replaceTree(p, "spark"))

      case  p @ Pat.Var(Term.Name("hiveContext")) =>
        Seq(Patch.replaceTree(p, "spark"))

      case  t @ Term.Name("parallelize") =>
        Seq(Patch.replaceTree(t, "createDataFrame"))

      case  t @ Term.Name("toDF") =>
        Seq(Patch.removeTokens(t.tokens))

      case  t @ Term.Name("registerTempTable") =>
        Seq(Patch.replaceTree(t, "createOrReplaceTempView"))
    })
    //println("Tree.syntax: " + doc.tree.syntax)
    //println("Tree.structure: " + doc.tree.structure)


    patches.flatten.fold(Patch.empty)(_ + _)
  }

