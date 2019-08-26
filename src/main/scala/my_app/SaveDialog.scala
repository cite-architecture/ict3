package ict3 

import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.{BindingSeq, Var, Vars}
import scala.scalajs.js
import scala.scalajs.js._
import org.scalajs.dom._
import org.scalajs.dom.ext._
import edu.holycross.shot.cite._
import edu.furman.classics.citewriter._
import edu.holycross.shot.citerelation._

import scala.scalajs.js.Dynamic.{ global => g }
import org.scalajs.dom.raw._
import org.scalajs.dom.document
import org.scalajs.dom.raw.Event
import org.scalajs.dom.ext.Ajax
import scala.scalajs.js.annotation.JSExport
import js.annotation._


@JSExportTopLevel("SaveDialog")
object SaveDialog {

	def readCookie: Map[String, String] = document.cookie
		.split(";")
		.toList
		.map(_.split("=").toList)
		.flatMap(x =>
			(x.headOption, x.drop(1).headOption) match {
				case (Some(k), Some(v)) => List((k.trim, v))
				case _                  => Nil
				})
		.toMap

	def readCookieValue(k:String):Option[String] = {
		val cm = readCookie
		if (cm.keys.toVector.contains(k)) {
			val r:Option[String] = Some(cm(k))
			r
		} else { 
			val r:Option[String] = None
			r
		}
	}

	def removeCookieField(k:String):Unit = {
		val cm = readCookie
		val nm = cm - k
		val d = new Date()
		writeCookie(nm)
	}

	def updateCookie(k:String, v:String):Unit = {
		val cm = readCookie
		val nm = cm ++ Map(k -> v)
		g.console.log(s"Updating cookie with ${nm}")
		val d = new Date()
		writeCookie(nm)	
	}

	def writeCookie(values:Map[String, String]): Unit = {
		val d = new Date()
		//val nextYear = d.setYear(d.getYear() + 1)
		val expiry = new Date(d.getFullYear() + 1, d.getMonth())

		values.toList.foreach {
			case (k, v) => val expires = expiry.toUTCString
			document.cookie = s"$k=$v;expires=$expires;path=/"
		}
	}

	def assembleAndSaveCex:Unit = {
		// Assemble CEX 
		val crs: CiteRelationSet = {
			CiteRelationSet(MainModel.allROIs.value.toSet)
		}
		val rsCex: String = {
			MainModel.saveAs2Column.value match {
				case true => {
					crs.relations.toVector.map( r => {
						s"${r.urn1}\t${r.urn2}"
					}).mkString("\n")
				}
				case false => {
			    CexWriter.writeCiteRelationBlock(crs, standalone = true)
				}
			}
		}

		// Write cookie
		updateCookie("editorName", MainModel.defaultExportUsername.value)
		updateCookie("defaultCexFn", MainModel.defaultExportFilename.value)

		// Write CEX 
		saveCex(MainModel.defaultExportFilename.value, rsCex)

	}

/* Methods for connecting out to Javascript */
@JSGlobal("saveCex")
@js.native
object saveCex extends js.Any {
  def apply(filename:String, data:String): js.Dynamic = js.native  
}


}
