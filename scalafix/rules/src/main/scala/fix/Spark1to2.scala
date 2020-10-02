
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
      case i @ Importer(ref, xs) if xs.toString.contains("HiveContext")  =>
        xs.map(Patch.removeImportee)
      case i @ Importer(ref, xs) if xs.toString.contains("DataFrame")  =>
        xs.map(Patch.removeImportee)
    })

    if (patches.nonEmpty) {
      patches.flatten.reduce(_ + _) +
        Patch.addGlobalImport(importer"org.apache.spark.sql.SparkSession") +
        Patch.addGlobalImport(importer"org.apache.spark.sql.{Row, SaveMode, SparkSession}") +
        Patch.addGlobalImport(importer"org.apache.spark.sql.Dataset")
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
        Seq(Patch.addRight(pl,s"//Println Syntax can be used as like  println(s(println statement)"))
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
        Seq(Patch.replaceTree(n, "spark.sparkContext"))

      case  n @ Term.New(Init(t,name,arg)) if  t.toString.contains("SQLContext") =>
        Seq(Patch.replaceTree(n, "spark"))

      case  p @ Pat.Var(Term.Name("sparkConf")) =>
      Seq(Patch.replaceTree(p, "spark"))

      case  p @ Pat.Var(Term.Name("sqlContext")) =>
        Seq(Patch.replaceTree(p, "spark"))

      case  t @ Term.Name("unionAll") =>
        Seq(Patch.replaceTree(t, "union"))

      case  t @ Term.Name("registerTempTable") =>
        Seq(Patch.replaceTree(t, "createOrReplaceTempView"))

    })
    //println("Tree.syntax: " + doc.tree.syntax)
    //println("Tree.structure: " + doc.tree.structure)


    patches.flatten.fold(Patch.empty)(_ + _)
  }

}


class Spark1to2ML extends SemanticRule("Spark1to2ML") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    //println("Tree.structureLabeled: " + doc.tree.structureLabeled)

    val patches: Seq[Seq[Patch]] = doc.tree.collect({


     case s @ Term.Select(Term.Select(Term.Name(qual), Term.Name("loadLibSVMFile")), arg) if qual.toString.contains("MLUtils") => {
        Seq(Patch.replaceTree(s, "spark.read.(\"libsvm\").load") + Patch.removeTokens(arg.tokens))

        }

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


      case  n @ Term.New(Init(t,name,arg)) if  t.toString.contains("HiveContext") =>
        Seq(Patch.replaceTree(n, "spark"))


      case  p @ Pat.Var(Term.Name("hiveContext")) =>
        Seq(Patch.replaceTree(p, "spark"))

      /*case  c @ Lit.String(syntax) if syntax.toString.toUpperCase.contains("CREATE TABLE") && !syntax.toString.toUpperCase.contains("LOCATION") =>
        Seq(Patch.removeTokens(c.tokens) + Patch.addLeft(c,"\"" + syntax.toUpperCase  + " LOCATION($file_path)\""))*/
      case Term.Apply(Term.Name("sql"), args) =>
        args.collect {
          case t @ Lit.String(syntax) if (syntax.toString.toUpperCase.contains("CREATE TABLE") || syntax.toString.toUpperCase.contains("CREATE EXTERNAL TABLE") ) && !syntax.toString.toUpperCase.contains("LOCATION") =>
            Patch.removeTokens(t.tokens) + Patch.addLeft(t,"\"" + syntax.toUpperCase  + " LOCATION($file_path)\"")
        }


      case  t @ Term.Name("parallelize") =>
        Seq(Patch.replaceTree(t, "createDataFrame"))

      case  t @ Term.Name("registerTempTable") =>
        Seq(Patch.replaceTree(t, "createOrReplaceTempView"))

      case  t @ Term.Name("unionAll") =>
        Seq(Patch.replaceTree(t, "union"))
    })
    //println("Tree.syntax: " + doc.tree.syntax)
    //println("Tree.structure: " + doc.tree.structure)


    patches.flatten.fold(Patch.empty)(_ + _)
  }

}
class Spark1to2Rest extends SemanticRule("Spark1to2Rest") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    //println("Tree.structureLabeled: " + doc.tree.structureLabeled)

    val patches: Seq[Seq[Patch]] = doc.tree.collect({


      case t @ Type.Name("fromCaseClassString") =>
        Seq(Patch.replaceTree(t, "fromJson"))

      case t @ Type.Apply(Type.Name("TypedExpr"), x :: Nil) =>
        Seq(Patch.replaceTree(t, s"Expr[$x]"))


      case  t @ Type.Select(Term.Name("Fnk"), _) =>
        Seq(Patch.removeTokens(t.tokens.take(2)))

      case s @ Term.Apply(Term.Name("struct"), args) if args.forall(x => x.isInstanceOf[Term.Assign]) =>
        val assigns = args.asInstanceOf[Seq[Term.Assign]]
        assigns.map({
          case t @ Term.Assign(n @ Term.Name(_), rhs) =>
            val equalToken = t.tokens.tokens.drop(n.tokens.end).find(_.text == "=").get
            Patch.addLeft(n, "\"") +
              Patch.addRight(n, "\"") +
              Patch.removeToken(equalToken) +
              Patch.addLeft(rhs, "<<- ")
        })

      case Term.ApplyInfix(_, p @ Term.Name("|"), _, _) =>
        Seq(Patch.replaceTree(p, ","))
    })
    //println("Tree.syntax: " + doc.tree.syntax)
    //println("Tree.structure: " + doc.tree.structure)


    patches.flatten.fold(Patch.empty)(_ + _)
  }

}
