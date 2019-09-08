package ict3
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.{BindingSeq, Var, Vars}
import scala.scalajs.js
import scala.scalajs.js._
import js.annotation._
import scala.concurrent._
import collection.mutable
import collection.mutable._
import scala.scalajs.js.Dynamic.{ global => g }
import org.scalajs.dom._
import org.scalajs.dom.ext._
import org.scalajs.dom.raw._
import edu.holycross.shot.cite._
import edu.holycross.shot.scm._
import edu.holycross.shot.citerelation._
import edu.holycross.shot.citebinaryimage._
import edu.holycross.shot.ohco2._
import edu.holycross.shot.citeobj._


import monix.execution.Scheduler.Implicits.global
import monix.eval._

import scala.scalajs.js.annotation.JSExport



@JSExportTopLevel("ImageModel")
object ImageModel {

val serviceUrl = Var("http://www.homermultitext.org/iipsrv?")
val serviceZoomService = Var("DeepZoom=")
val servicePath = Var("/project/homer/pyramidal/deepzoom/")
val serviceSuffix = Var(".tif")
val serviceZoomPostfix = Var(".dzi")
val localpath = Var("../../../image_archive/")
val thumbWidth = Var(250)
val fullWidth = Var(5000)	
val thumbUrn = Var[Option[Cite2Urn]](None)
val localThumbDataUrl = Var("")

val useLocal = Var(true)
//var useLocalVar = false



}
