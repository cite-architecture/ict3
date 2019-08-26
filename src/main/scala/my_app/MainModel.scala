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



@JSExportTopLevel("MainModel")
object MainModel {

	/* Exorting CEX */

	val defaultExportFilename = Var("ict3_export.cex")
	val defaultExportUsername = Var("ICT3_User")
	val saveAs2Column = Var(false)

	/* Generating ROI alignments */

	// The basis of a urn for alingments
	val defaultDataUrn = Var("urn:cts:greekLit:tlg0012.tlg001:")
	// Should always be this
	val verbUrn = Cite2Urn("urn:cite2:cite:verbs.v1:illustrates")
	// ROIs for curently displayed image
	val currentImageROIs = Vars[CiteTriple]()
	// All ROIs, from CEX and generated ones
	val allROIs = Vars[CiteTriple]()
	// All images present in relations from loaded CEX
	val cexImages = Vars[Cite2Urn]()

	/* Loading an Image */
	// The currently loaded image
	val currentImage = Var[Option[Cite2Urn]](None)

	// Local or remote images?
	val useLocal = Var(false)

	// Does the text input field have a valid URN in it?
	val validUrnInBox = Var(false)
	val validDataUrnInBox = Var(false)

	/* REQUEST PARAMS */
	var requestParamUrn:Option[Urn] = None

	/* control panels &c. */
	val userMessage = Var("Main loaded.")
	val userAlert = Var("default")
	val userMessageVisibility = Var("app_hidden")
	var msgTimer:scala.scalajs.js.timers.SetTimeoutHandle = null

	/* Stuff for when the user draws a new ROI */
	val activeNewROI = Var[Option[Cite2Urn]](None)
	val activeNewData = Var[Option[Urn]](None)

	/* Stuff for editing an existing Triple */
	val currentlyBeingEdited = Var[Option[CiteTriple]](None)

	def deleteRelation( ct: CiteTriple ):Unit = {
		val allRs: Vector[CiteTriple] = allROIs.value.toVector.filterNot(_ == ct)
		val imageRs: Vector[CiteTriple] = currentImageROIs.value.toVector.filterNot(_ == ct)
		val setAllRs: CiteRelationSet = CiteRelationSet(allRs.toSet)
		val setImageRs: CiteRelationSet = CiteRelationSet(imageRs.toSet)
		ImageUtils.clearJsRoiArray(true)
		updateAllROIs(setAllRs)
		updateImageROIs(setImageRs)
		MainController.retrieveImage(currentImage.value.get)
	}

	def tripleToId(ct: CiteTriple): String = {
		val u1: String = ct.urn1.toString.replaceAll("[:.,@]","")
		val u2: String = ct.urn2.toString.replaceAll("[:.,@]","")
		s"${u1}_${u2}"
	}


	def createNewRelation: Unit = {
		try {
			val newROI: Cite2Urn = activeNewROI.value.get
			val newData: Urn = activeNewData.value.get
			val verb: Cite2Urn = verbUrn
			val ct = CiteTriple(newROI, verb, newData)
			val tempAllROIs: CiteRelationSet = CiteRelationSet( (allROIs.value :+ ct).toSet )
			val tempCurrentImageROIs: CiteRelationSet = CiteRelationSet( (currentImageROIs.value :+ ct).toSet )
			updateAllROIs(tempAllROIs)
			updateImageROIs(tempCurrentImageROIs)
			//val currentImageURNString: String = currentImage.value.get.toString
			//MainController.changeImage(currentImageURNString)
			ImageUtils.passNewRectToJS(ct)
		} catch {
			case e: Exception => {
				var m = s"Failed to create new relation (MainModel.createNewRelation): ${e}"
				MainController.updateUserMessage(m, 2)
				g.console.log(m)
			}
		}
	}

	/* Utility Functions */

	def truncUrn(u:Urn): String = {
		u match {
			case Cite2Urn(_) => truncUrn(u.asInstanceOf[Cite2Urn])
			case CtsUrn(_) => truncUrn(u.asInstanceOf[CtsUrn])
			case _ => s"${u}"
		}
	}

	def truncUrn(u:CtsUrn):String = {
		val beginning:String = s"urn:cts:"
		val ending:String = {
			val workString = u.workParts.mkString.takeRight(4)
			s"${workString}:${u.passageComponent}"
		}
		s"${beginning}…${ending}"
	}

	def truncUrn(u:Cite2Urn):String = {
		val beginning:String = s"urn:cite2:"
		val ending:String = {
			u.objectExtensionOption match {
				case Some(oe) => u.dropExtensions.objectComponent + "…" + u.objectExtension.takeRight(4)
				case None => u.objectComponent
			}
		}
		s"${beginning}…${ending}"
	}


	def clearAllROIs: Unit = {
		allROIs.value.clear
		currentImageROIs.value.clear
		cexImages.value.clear
	}	


	def updateAllROIs(rois: CiteRelationSet): Unit = {
		clearAllROIs
		for ( r <- rois.relations) {
			allROIs.value += r
		}	
		val justImages: Vector[Cite2Urn] = {
			rois.relations.toVector.map(_.urn1.asInstanceOf[Cite2Urn].dropExtensions).distinct.sortBy(_.toString)
		}
		for ( u <- justImages) {
			cexImages.value += u
		}
	}	

	def clearImageROIs: Unit = {
		currentImageROIs.value.clear
	}

	def updateImageROIs(rois: CiteRelationSet): Unit = {
		clearImageROIs
		for ( r <- rois.relations ) {
			currentImageROIs.value += r
		}
		// Now add them to the JS Array
		/*
		roiObject
			.imageUrnString
			.imageRoiString
			.classNameString
			.roiObjectId
		*/
		for ( r <- currentImageROIs.value ) {
			try {
				val wholeImageURN: Cite2Urn = r.urn1.asInstanceOf[Cite2Urn]
				val justUrnString: String = {
					wholeImageURN.objectExtensionOption match {
						case Some(e) => wholeImageURN.dropExtensions.toString
						case None => wholeImageURN.toString
					}
				}
				val justROI: String = {
					wholeImageURN.objectExtensionOption match{
						case Some(e) => e
						case None => ""
					}
				}
				val thisIndex: Int = currentImageROIs.value.indexOf(r)
				val roiObjectId: String = s"image_mappedROI_${thisIndex}"
				val thisClassNameString: String = s"image_mappedROI image_roiGroup_${thisIndex} image_mappedUrn_${thisIndex}"
				val roiTest:Boolean = {
					(( justROI.size > 0) &&
					(justROI.split(',').size == 4))
				}
				if (roiTest) {
					ImageUtils.addToJsRoiArray(justUrnString, justROI, thisClassNameString, roiObjectId)
				} else {
					g.console.log(s"${justUrnString} has no ROI extension.")
				}
			} catch {
				case e: Exception => MainController.updateUserMessage(s"Unable to make CITE2 Urn from Urn1 of triple: ${r}", 2)
			}
		}	

	}


}
