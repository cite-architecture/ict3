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
import edu.holycross.shot.ohco2._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.citerelation._


import monix.execution.Scheduler.Implicits.global
import monix.eval._

import scala.scalajs.js.annotation.JSExport

@JSExportTopLevel("MainController")
object MainController {


	/* 
		Initiate app with a URL to an online CEX file	
	*/
	@JSExport
	def main(libUrl: String): Unit = {
		MainModel.requestParamUrn = MainController.getRequestUrn
		if (libUrl.size > 3){
			loadRemoteLibrary(libUrl)
		}
	}

	@JSExportTopLevel("validateCtsUrn")
	def validateCtsUrn(us: String): Boolean = {
		try {
			val u = CtsUrn(us)
			true
		}	catch {
			case e:Exception => {
				false
			}
		}

	}

	@JSExportTopLevel("validateCite2Urn")
	def validateCite2Urn(us: String): Boolean = {
		try {
			val u = Cite2Urn(us)
			true
		}	catch {
			case e:Exception => {
				false
			}
		}
	}

	def validateImageUrn(us: String):Unit = {
		if (validateCite2Urn(us)) {
			MainModel.validUrnInBox.value = true	
			MainModel.currentImage.value = Some(Cite2Urn(us))
		} else {
			MainModel.validUrnInBox.value = false
			MainModel.currentImage.value = None 
		}
	}

	def validateDataUrn(us: String):Unit = {
		val isCite2Urn: Boolean = validateCite2Urn(us)
		val isCtsUrn: Boolean = validateCtsUrn(us)
		if (isCite2Urn) {
			MainModel.validDataUrnInBox.value = true	
			MainModel.activeNewData.value = Some(Cite2Urn(us))
		} else if (isCtsUrn) {
			MainModel.validDataUrnInBox.value = true
			MainModel.activeNewData.value = Some(CtsUrn(us))
		} else {
			MainModel.validDataUrnInBox.value = false
			MainModel.activeNewData.value = None
		}

	}

	/** 
		* Validate URN. If URN chosen from popup, set text-entry box to match
		* !! Load new image !!
		* !! Load any ROIs for it, from data !!
	*/
	def changeImage(us:String):Unit = { 
		if (validateCite2Urn(us)) {
			js.Dynamic.global.document.getElementById("ict3_image_urnInput").value = us
			validateImageUrn(us)
			MainModel.clearImageROIs
			ImageUtils.clearJsRoiArray(true)
			retrieveImage(Cite2Urn(us))
		} else {
			MainModel.currentImage.value = None
			js.Dynamic.global.document.getElementById("ict3_image_urnInput").value = ""
			MainModel.validUrnInBox.value = false
		}
	}


	def retrieveImage(u:Cite2Urn):Unit = {
			// Get ROIs
			val imgUrn: Cite2Urn = u.dropExtensions
			val roisForImage: CiteRelationSet = CiteRelationSet(MainModel.allROIs.value.toVector.filter(_.urn1 ~~ imgUrn).toSet)
			// Retrieve Image
			val path: String = ImageUtils.getTileSources(u)	
			ImageUtils.updateImageJS(path, imgUrn.toString)
			MainModel.updateImageROIs(roisForImage)
	}

	// Reads CEX file, creates repositories for Texts, Objects, and Images
	// *** Apropos Microservice ***
	@dom
	def updateRepository(cexString: String) = {
			g.console.log("Will update repository.")
			val library = CiteLibrary(cexString)
			val rso: Option[CiteRelationSet] = library.relationSet
			rso match {
				case Some(rs) => {
					MainController.updateUserMessage("Loading new alignment relations.",1)
					val imageRelations = rs.verb(MainModel.verbUrn)
					MainModel.updateAllROIs(imageRelations)
				}
				case None => {
					MainController.updateUserMessage("This CEX data contained no image-illustration relations.",2)
				}
			}
			MainModel.requestParamUrn match {
				case Some(u) => {
					val us = u.toString
					changeImage(us)	
				}
				case None => {
					// do nothing	
				}
			}
		}


	/*
		Loads library from local CEX file; updates repository
	*/
	def loadLocalLibrary(e: Event):Unit = {
		val reader = new org.scalajs.dom.raw.FileReader()
		MainController.updateUserMessage("Loading local library.",0)
		 reader.readAsText(e.target.asInstanceOf[org.scalajs.dom.raw.HTMLInputElement].files(0))
		reader.onload = (e: Event) => {
			val contents = reader.result.asInstanceOf[String]
			//MainModel.requestParameterUrn.value = MainController.getRequestUrn
			MainController.updateRepository(contents)
		}
	}

	/*
		Loads library from local CEX file; updates repository
	*/
	def loadRemoteLibrary(url: String):Unit = {
		val xhr = new XMLHttpRequest()
		xhr.open("GET", url )
		xhr.onload = { (e: Event) =>
			if (xhr.status == 200) {
				val contents:String = xhr.responseText
				MainController.updateUserMessage("Loading remote library.",1)
				MainController.updateRepository(contents)
			} else {
				MainController.updateUserMessage(s"Request for remote library failed with code ${xhr.status}",2)
			}
		}
		xhr.send()
	}

	/*
	 	Handles displaying messages to the user, color-coded according to type.
	 	Fades after 10 seconds.		
	*/
	def updateUserMessage(msg: String, alert: Int): Unit = {
		MainModel.userMessageVisibility.value = "app_visible"
		MainModel.userMessage.value = msg
		alert match {
			case 0 => MainModel.userAlert.value = "default"
			case 1 => MainModel.userAlert.value = "wait"
			case 2 => MainModel.userAlert.value = "warn"
		}
		js.timers.clearTimeout(MainModel.msgTimer)
		MainModel.msgTimer = js.timers.setTimeout(4000){ MainModel.userMessageVisibility.value = "app_hidden" }
	}

	/* Get Request Parameter */
	def getRequestUrn:Option[Urn] = {
	val currentUrl = 	js.Dynamic.global.location.href
		val requestParamUrnString = currentUrl.toString.split('?')
		val requestUrn:Option[Urn] = requestParamUrnString.size match {
			case s if (s > 1) => {
				try {
					val parts = requestParamUrnString(1).split("=")
					if ( parts.size > 1) {
						if ( parts(0) == "urn" ) {
							val decryptedString:String = js.URIUtils.decodeURIComponent(parts(1))
							val decryptedUrn:Option[Urn] = {
								parts(1).take(8) match {
									case ("urn:cts:") => Some(CtsUrn(decryptedString))
									case ("urn:cite") => Some(Cite2Urn(decryptedString).dropProperty)
									case _ => {
										None
									}
								}
							}
							decryptedUrn
						} else {
							None
						}
					} else {
						None
					}
				} catch {
					case e:Exception => {
						MainController.updateUserMessage(s"Failed to load request-parameter URN: ${e}",1)
						None
					}
				}
			}
			case _  => {
				None
			}
		}
		g.console.log(s"found request param: ${requestUrn}")
		requestUrn
	}

	dom.render(document.body, MainView.mainDiv)
	// Grab cookies
	val editorCookie: Option[String] = SaveDialog.readCookieValue("editorName")		
	editorCookie match {
		case Some(c) => MainModel.defaultExportUsername.value = c
		case None => // do nothing
	}
	val filenameCookie: Option[String] = SaveDialog.readCookieValue("defaultCexFn")		
	filenameCookie match {
		case Some(c) => MainModel.defaultExportFilename.value = c
		case None => // do nothing
	}
	val urnCookie: Option[String] = SaveDialog.readCookieValue("defaultAlignmentUrn")		
	try {
		urnCookie match {
			case Some(c) => MainModel.defaultDataUrn.value = c
			case None => // do nothing
		}
	} catch {
		case e: Exception => MainController.updateUserMessage(s"Could not make CTS URN out of value from cookie: ${e}", 2)
	}


}
