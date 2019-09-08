package ict3 

import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.{BindingSeq, Var, Vars}
import scala.scalajs.js
import scala.scalajs.js._
import org.scalajs.dom._
import org.scalajs.dom.ext._
import edu.holycross.shot.cite._
import edu.holycross.shot.citebinaryimage._
import edu.holycross.shot.citerelation._

import scala.scalajs.js.Dynamic.{ global => g }
import org.scalajs.dom.raw._
import org.scalajs.dom.document
import org.scalajs.dom.raw.Event
import org.scalajs.dom.ext.Ajax
import scala.scalajs.js.annotation.JSExport
import js.annotation._



@JSExportTopLevel("ImageUtils")
object ImageUtils {
	


	/* return a string, the source of a remotely served image thumbnail */
	def thumbSourceRemote(urn:Cite2Urn):String = {
		ImageModel.serviceUrl.value + "?OBJ=IIP,1.0&FIF=" + ImageModel.servicePath.value + ImageUtils.getImagePathFromUrn(urn.dropExtensions) + s"${urn.dropExtensions.objectComponent}" + ImageModel.serviceSuffix.value + "&RGN=" + s"""${urn.objectExtensionOption.getOrElse("")}""" + "&wID=250&CVT=JPEG"
					//"http://www.homermultitext.org/iipsrv?OBJ=IIP,1.0&FIF=/project/homer/pyramidal/deepzoom/hmt/e4img/2017a/e4_547.tif&RGN=0.1272,0.3309,0.2210,0.05771&wID=250&CVT=JPEG"
	}

	/* return a string, the source of a remotely served image thumbnail */
	def hirezSourceRemote(urn:Cite2Urn):String = {
		ImageModel.serviceUrl.value + "?OBJ=IIP,1.0&FIF=" + ImageModel.servicePath.value + ImageUtils.getImagePathFromUrn(urn.dropExtensions) + s"${urn.dropExtensions.objectComponent}" + ImageModel.serviceSuffix.value + "&RGN=" + s"""${urn.objectExtensionOption.getOrElse("")}""" + "&wID=5000&CVT=JPEG"
	}

	def updateThumbSourceLocal(urn:Cite2Urn):Unit = {
		val roi:ImageROI = {
			urn.objectExtensionOption match {
				case Some(e) => ImageROI(e)
				case None => ImageROI("0,0,1,1")
			}
		}	
		val rL: Double = roi.left
		val rT: Double = roi.top
		val rW: Double = roi.width
		val rH: Double = roi.height
		val tempImagePath: String = getImagePathFromUrn(urn)
		val imgId: String = urn.dropExtensions.objectComponent
		val path: String = ImageModel.localpath.value + tempImagePath  + imgId + ".jpg";
		val cvs = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
		val ctx = cvs.getContext("2d");
		val offScreenImg = document.createElement("img").asInstanceOf[HTMLImageElement];
		cvs.setAttribute("crossOrigin","Anonymous")
		offScreenImg.setAttribute("crossOrigin","Anonymous")
	  offScreenImg.setAttribute("src",path);
	  offScreenImg.onload = { evt:Event =>
			  cvs.width = (offScreenImg.width * rW).toInt
				cvs.height = (offScreenImg.height * rH).toInt
				ctx.drawImage(offScreenImg,(0-(offScreenImg.width * rL).toInt),(0-(offScreenImg.height*rT)).toInt);
		    val s = cvs.toDataURL("image/png")
		    ImageModel.localThumbDataUrl.value = s
		}
	}

	def setPreferredImageSource = {
		val imgSourceStr:String = js.Dynamic.global.document.getElementById("citeMain_localImageSwitch").checked.toString
		imgSourceStr match {
			case "true" => {
				ImageModel.useLocal.value = false
			}
			case "false" => {
				ImageModel.useLocal.value = true 
			}
			case _ => g.console.log(s"checked value == '${imgSourceStr}'")
		}	

		MainModel.currentImage.value match {
			case Some(u) => {
				val path: String = ImageUtils.getTileSources(u, ImageModel.useLocal.value)	
				ImageUtils.updateImageJS(path, u.toString)
				val roisForImage = {
					CiteRelationSet(MainModel.currentImageROIs.value.toSet)
				}
				MainModel.updateImageROIs(roisForImage)
			}
		case None => {
			// do nothing
		}
		}

	}

	/**
 * Returns an Image Path from the given URN.
 * @param  Cite2Urn urn the urn to analyse
 * @return String the Image Path
 */
def getImagePathFromUrn(urn: Cite2Urn) = {
	val ns  = urn.namespace
	val collection = urn.collection
	val version = urn.version
	val tempPath = ns + "/" + collection + "/" + version + "/"
	tempPath
}

/**
 * Returns the Tilesource for the given Image URN
 * @param  {string} imgUrn the URN of the Image
 * @return {string}       the URL of the TileSource
 */
def getTileSources(imgUrn: Cite2Urn, useLocal:Boolean): String = {
	val plainUrn: Cite2Urn = imgUrn.dropExtensions
	val imgId = plainUrn.objectComponent
	val imagePath = getImagePathFromUrn(plainUrn);

	val ts: String = 	{
		if (useLocal ){
			val ts: String = ImageModel.localpath.value + imagePath + imgId + ImageModel.serviceZoomPostfix.value
			ts
		} else {
			val ts: String = ImageModel.serviceUrl.value + ImageModel.serviceZoomService.value + ImageModel.servicePath.value + imagePath + imgId + ImageModel.serviceSuffix.value + ImageModel.serviceZoomPostfix.value
			ts
		}
	}
	ts
}

@JSExportTopLevel("ICT_NewROI")
object ICT_NewROI {
  @JSExport
  def userDrewNewROI(urnString: String): Unit = {
    try {
    	val u: Cite2Urn = Cite2Urn(urnString)
	    MainModel.activeNewROI.value = Some(u)
    } catch {
    	case e: Exception => MainController.updateUserMessage(s"Failed to make CITE2 URN out of: ${urnString}",2)
    }
  }
}

@JSExportTopLevel("ICT_HighlightData")
object ICT_HighlightData {
  @JSExport
  def highlightData(idIndex: String): Unit = {
    try {
    	// image_mappedROI_3 (in graphic)
   	// image_roiGroup_1 (in data)
			val thisClass = idIndex.replaceAll("image_mappedROI_", "image_roiGroup_")
    	for ( cir <- MainModel.currentImageROIs.value ){
    		val thisID: String = 	s"li_${MainModel.tripleToId(cir)}"
    		val el = js.Dynamic.global.document.getElementById(thisID).asInstanceOf[org.scalajs.dom.raw.HTMLLIElement]
    		if (el.classList.contains("image_roi_selected")) {
    			el.classList.remove("image_roi_selected")
    		}
    		if (el.classList.contains(thisClass)) {
    			el.classList.add("image_roi_selected")
    		}
    	}
    } catch {
    	case e: Exception => {
    		MainController.updateUserMessage(s"Something went wrong highlighting data.",2)
    		g.console.log(s"${e}")
    	}

    }
  }
}

@JSExportTopLevel("ICT_HighlightROI")
object ICT_HighlightROI {
  @JSExport
  def highlightROI(idIndex: String): Unit = {
    try {
    	for ( cir <- MainModel.currentImageROIs.value ){
    		val idx = MainModel.currentImageROIs.value.indexOf(cir)
    		val tempId = s"image_mappedROI_${idx}"
    		val el = js.Dynamic.global.document.getElementById(tempId).asInstanceOf[org.scalajs.dom.raw.HTMLAnchorElement]
    		if (el.classList.contains("image_roi_selected")) {
    			el.classList.remove("image_roi_selected")
    		}
    	}
  		val el = js.Dynamic.global.document.getElementById(idIndex).asInstanceOf[org.scalajs.dom.raw.HTMLAnchorElement]
  		el.classList.add("image_roi_selected")

    } catch {
    	case e: Exception => {
    		MainController.updateUserMessage(s"Something went wrong highlighting data.",2)
    		g.console.log(s"${e}")
    	}

    }
  }
}

/* Methods for connecting out to Javascript */


	@JSGlobal("clearJsRoiArray")
	@js.native
	object clearJsRoiArray extends js.Any {
		def apply(really:Boolean): js.Dynamic = js.native
	}

	/* Methods for connecting out to Javascript */
	@JSGlobal("removeTempROI")
	@js.native
	object removeTempROI extends js.Any {
		def apply(really:Boolean = true): js.Dynamic = js.native
	}

	@JSGlobal("addToJsRoiArray")
	@js.native
	object addToJsRoiArray extends js.Any {
		def apply(imageUrnString: String, imageRoiString: String, classNameString: String, roiObjectId: String): js.Dynamic = js.native
	}

	@JSGlobal("updateImageJS")
	@js.native
	object updateImageJS extends js.Any {
		def apply(path: String, imageUrnString: String): js.Dynamic = js.native
	}

	@JSGlobal("jsGetsRectFromScala")
	@js.native
	object jsGetsRectFromScala extends js.Any {
		def apply(imageUrnString: String, imageRoiString: String, classNameString: String, roiObjectId: String): js.Dynamic = js.native
	}

	@JSGlobal("jsSetLocalThumb")
	@js.native
	object jsSetLocalThumb extends js.Any {
		def apply(localPath: String, imageRoiString: String, imgElId: String): js.Dynamic = js.native
	}

	def passNewRectToJS(ct: CiteTriple) = {
		val wholeImageURN: Cite2Urn = ct.urn1.asInstanceOf[Cite2Urn]
		val justUrnString: String = wholeImageURN.dropExtensions.toString
		val justROI: String = {
			wholeImageURN.objectExtensionOption match{
				case Some(e) => e
				case None => ""
			}
		}
		val thisIndex: Int = MainModel.currentImageROIs.value.indexOf(ct)
		val roiObjectId: String = s"image_mappedROI_${thisIndex}"
		val thisClassNameString: String = s"image_mappedROI image_roiGroup_${thisIndex} image_mappedUrn_${thisIndex}"
		ImageUtils.jsGetsRectFromScala(justUrnString, justROI, thisClassNameString, roiObjectId)
	}
}