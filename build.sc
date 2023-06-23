import mill._
import mill.scalalib.TestModule.ScalaTest

import scalalib._
import scalafmt._



val chisel3 = ivy"edu.berkeley.cs::chisel3:3.5.1"
val chiseltest = ivy"edu.berkeley.cs::chiseltest:0.5.1"
val chisel3Plugin = ivy"edu.berkeley.cs:::chisel3-plugin:3.5.1"
val scalatest = ivy"org.scalatest::scalatest:3.2.2"

object difftest extends ScalaModule {
  override def scalaVersion = "2.12.13"
  override def millSourcePath = os.pwd / "difftest"
  override def scalacPluginIvyDeps = Agg(chisel3Plugin)
  override def ivyDeps = Agg(
    chisel3,
    scalatest
  )
}

object SZU_RISCV extends ScalaModule with ScalafmtModule {

  override def millSourcePath = os.pwd
  override def scalaVersion = "2.12.13"

  def scalafmtConfigFile = T.source(os.pwd / ".scalafmt.conf")

  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
    "-P:chiselplugin:genBundleElements"
  )

  override def ivyDeps = Agg(
    chisel3,
    scalatest,
    chiseltest
  )

  override def moduleDeps = Seq(difftest)

  override def scalacPluginIvyDeps = Agg(chisel3Plugin)

  object test extends Tests with ScalaTest {
    override def ivyDeps = super.ivyDeps() ++ Agg(chiseltest)
  }


}