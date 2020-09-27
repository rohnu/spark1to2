
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
      case println @ Term.Apply(Term.Name("println"), _) =>
        Seq(Patch.lint(Println(println.pos)))
    })
    patches.flatten.fold(Patch.empty)(_ + _)
  }
}


class Spark1to2Session extends SemanticRule("Spark1to2Session") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    //println("Tree.structureLabeled: " + doc.tree.structureLabeled)

    val patches: Seq[Seq[Patch]] = doc.tree.collect({


      case  t @ Type.Name("SparkConf") =>
        Seq(Patch.replaceTree(t, "SparkSession.builder")+ Patch.addRight(t, s".getOrCreate() // "))


      case  t @ Term.Name("setAppName") =>
        Seq(Patch.removeTokens(t.tokens))


      case  t @ Type.Name("SparkContext") =>
        Seq(Patch.replaceTree(t, "spark"))

      case  t @ Term.Name("textFile") =>
        Seq(Patch.replaceTree(t, "read.text"))

      case  t @ Term.New(_) =>
        Seq(Patch.removeTokens(t.tokens))

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


      case t @ Type.Name("Expr") =>
        Seq(Patch.replaceTree(t, "Expr[Any]"))

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