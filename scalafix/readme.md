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
scalafix --rules=file:/path/to/NamedLiteralArguments.scala
```

Scalafix 101 video:
```
https://www.youtube.com/watch?v=uaMWvkCJM_E
```
