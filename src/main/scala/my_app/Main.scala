package ict3
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.{BindingSeq, Var, Vars}
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.dom
import org.scalajs.dom.document
import org.scalajs.dom.raw.Event
import org.scalajs.dom.ext.Ajax
import scala.concurrent
											.ExecutionContext
											.Implicits
											.global

import scala.scalajs.js
import scala.scalajs.js._
import js.annotation._
import scala.scalajs.js.annotation.JSExport

@JSExportTopLevel("CiteMain")
class Main(remoteUrl: String,
			serviceUrl: String,
			serviceZoomService: String,
			servicePath: String,
			serviceSuffix: String,
			serviceZoomPostfix: String,
			localpath: String,
			useLocal: Boolean) {

	@JSExport
	def main(): Unit = {
		MainController.main(
			remoteUrl: String,
			serviceUrl: String,
			serviceZoomService: String,
			servicePath: String,
			serviceSuffix: String,
			serviceZoomPostfix: String,
			localpath: String,
			useLocal: Boolean
		)

	}




}
