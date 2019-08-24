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
	/**
	 * Distributes the drawing of the preview image to the right function
	 * depending on local or remote setting
	 * @param  {string} urn the URN we want to draw a preview of
	 */
	@JSExportTopLevel("ict2_drawPreviewFromUrn")
	def ict2_drawPreviewFromUrn(urn: Cite2Urn): Unit = {
			urn.objectExtensionOption match {
				case Some (oe) => {
					var newRoi: ImageROI = ImageROI(oe)
					if (MainModel.useLocal.value){
						ImageUtils.getLocalPreview(newRoi)
					} else {
						ImageUtils.getRemotePreview(newRoi)
					}
				}
				case None => // do nothing
			}
	}

	/**
	 * Gets the local preview using the provided ROI parameter
	 * @param  {string} newRoi the ROI of the image
	 */
	@JSExportTopLevel("getLocalPreview")
	def getLocalPreview(newRoi: ImageROI): Unit = {
		// do nothing yet	
	}

	/**
	 * Creates the SRC attribute for the image used in the preview window using
	 * the provided ROI
	 * @param  {string} roi the ROI of this image
	 */
	@JSExportTopLevel("getRemotePreview")
	def getRemotePreview(roi: ImageROI): Unit = {
		// do nothing yet
	}

	def setPreferredImageSource = {
		val imgSourceStr:String = js.Dynamic.global.document.getElementById("citeMain_localImageSwitch").checked.toString
		imgSourceStr match {
			case "true" => MainModel.useLocal.value = false
			case _ => MainModel.useLocal.value = true 
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
def getTileSources(imgUrn: Cite2Urn): String = {
	val plainUrn: Cite2Urn = imgUrn.dropExtensions
	val imgId = plainUrn.objectComponent
	val imagePath = getImagePathFromUrn(plainUrn);

	val ts: String = 	{
		if (ImageModel.useLocal. value ){
			ImageModel.localpath.value + imagePath + imgId + ImageModel.serviceSuffix.value
		} else {
			ImageModel.serviceUrl.value + ImageModel.serviceZoomService.value + ImageModel.servicePath.value + imagePath + imgId + ImageModel.serviceSuffix.value + ImageModel.serviceZoomPostfix.value
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