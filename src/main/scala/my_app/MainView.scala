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

@JSExportTopLevel("MainView")
object MainView {

	val urnValidatingKeyUpHandler = { event: KeyboardEvent =>
		(event.currentTarget, event.keyCode) match {
			case (input: html.Input, KeyCode.Enter) => {
				event.preventDefault()
				if (MainModel.validUrnInBox.value) {
					MainController.changeImage(s"${input.value.toString}")
				}
				//input.value = ""
			}
			case(input: html.Input, _) =>  MainController.validateImageUrn(s"${input.value.toString}")
			case _ =>
		}
	}

	val updateDefaultDataValue = { event: KeyboardEvent =>
		(event.currentTarget, event.keyCode) match {
			case (input: html.Input, KeyCode.Enter) => {
				event.preventDefault()
					MainModel.defaultDataUrn.value = input.value.toString
			}
			case(input: html.Input, _) =>  MainModel.defaultDataUrn.value = input.value.toString
			case _ =>
		}
	}

	val urnValidatingKeyUpHandler2 = { event: KeyboardEvent =>
		(event.currentTarget, event.keyCode) match {
			case (input: html.Input, KeyCode.Enter) => {
				event.preventDefault()
			}
			case(input: html.Input, _) =>  MainController.validateDataUrn(s"${input.value.toString}")
			case _ =>
		}
	}

	
	@dom
	def mainDiv = {
	<div id="main-wrapper" class="">
		{ filePicker.bind }
		<header>
				Image Citation Tool 3 <span id="app_header_versionInfo">v.0.3.0</span>
		</header>

		<article id="main_Container">
		{ mainMessageDiv.bind }
		{ exportDiv.bind }
		{ configDiv.bind }
		{ imagePicker.bind }
		{ dataList.bind }
		{ imageContainer.bind }

		</article>
		<footer>
			{ htmlFooter.bind }		
		</footer>
		</div>
	}

	@dom
	def imagePicker = {
		<div id="ict3_urnSelect" class="ict3_config_div ict3_divVisible">

			{ imageSelect.bind }
			<label for="ict3_image_urnInput">Image URN:</label>
				<input
					class={ 
						MainModel.validUrnInBox.bind match {
							case true => "ict3_entry_valid"
							case false => "ict3_entry_invalid"
						}

					}
					id="ict3_image_urnInput"
					size={ 70 }
					type="text"
					value={ 
							MainModel.currentImage.bind match {
								case Some(u) => {
								u.toString
							}
							case _ => {
								""	
							}
						} 
					}
					onkeyup={ urnValidatingKeyUpHandler }>
				</input>

				{ retrieveImageButton.bind }

		</div>
	}

	@dom
	def imageSelect = {
		<label for="ict3_imagePopup">Images in CEX</label>
		<select id="ict3_imagePopup"
			onchange={ event: Event => {
				val thisTarget = event.target.asInstanceOf[org.scalajs.dom.raw.HTMLSelectElement]
		 		val newImageUrnString:String = thisTarget.value.toString
				MainController.changeImage(newImageUrnString)}
			}>
			{ if (MainModel.cexImages.length.bind == 0 ) { 
				<option value="None">No Images from CEX</option> 
				} else { <option value="">-</option> }

			}		
			{
				for ( v <- MainModel.cexImages ) yield { 
					<option value={ v.toString }>{ v.toString }</option>	
			}

			}
		</select>
	}

	@dom
	def retrieveImageButton = {
		<button
			onclick={ event: Event => {
				val s:String = js.Dynamic.global.document.getElementById("ict3_image_urnInput").value.toString
				//ImageModel.urn := Cite2Urn(s)
				MainController.updateUserMessage("Retrieving image…",1)
				val task = Task{ MainController.changeImage(s)}
				val future = task.runAsync
			} }
			disabled={ (MainModel.validUrnInBox.bind == false) 
		} > {
				if ( MainModel.validUrnInBox.bind == true ){
					"Retrieve Image"
				} else {
					"Invalid URN"
				}
			}
		</button>
	}

	@dom
	def imageContainer = {
		<div id="ict3_zoomHelp">
			2-fingers to zoom. 3 fingers to move. Zooming only in “draw” mode (type ‘c’ or click <strong>off</strong> the <em>draw</em> button to move/pan).
		</div>
		<div id="image_imageContainer"></div>
	}

	@dom
	def newObjectField = {
		<div id="ict3_newROI_data"
			class="">
				<span id="ict3_SectionHeading">{
				MainModel.activeNewROI.bind match {
								case Some(u) => "New Relation"
								case None => "Draw on image to add data"
							}
				}</span>
				<div id="ict3_newRelationHideWrapper" class={
				MainModel.activeNewROI.bind match {
								case Some(u) => "app_visible"
								case None => "app_hidden"
							}
				}
			>
					<span 
						id="ict3_newImageRoiUrnSpan" 
						class="ict3_data_urn ict3_data_urn1">{ 
								MainModel.activeNewROI.bind match {
									case Some(u) => MainModel.truncUrn(u)
									case None => ""
								}
						 }</span>
					<input id="ict3_newROIDataInput" type="text" size={30} value={ 
						MainModel.defaultDataUrn.bind.toString
					} 
						onkeyup={ urnValidatingKeyUpHandler2 }
					/>
					<button id="ict3_newRoiEnter"
					type="button"
					disabled = { 
							!(MainModel.validDataUrnInBox.bind)
					}
					onclick={ event: Event => {
						MainModel.createNewRelation
						ImageUtils.removeTempROI(true)
						MainModel.activeNewROI.value = None	
						// Save data URN to cookie
						SaveDialog.updateCookie("defaultAlignmentUrn", MainModel.defaultDataUrn.value.toString )
						MainModel.activeNewData.value = None
						js.Dynamic.global.document.getElementById("ict3_newROIDataInput").value = MainModel.defaultDataUrn.value.toString	
					} }
					>Save Relation</button>
					<button id="ict3_newRoiCancel"
						type="button"
						disabled = { false }
						onclick={ event: Event => {
							// Do something to kill new ROI
							MainModel.activeNewROI.value = None	
							ImageUtils.removeTempROI(true)
							} }
					>Cancel</button>
				</div>
		</div>
	}

	

	@dom
	def dataList = {
		<div id="ict3_data">
			<div id="ict3_imgPreviewDiv">
				<a id="ict3_imgHiRezLink" target="_blank" href={
							ImageModel.thumbUrn.bind match {
								case Some(roi) => {
										ImageModel.useLocal.bind match {
											case true => {
												ImageUtils.updateThumbSourceLocal(roi)
												ImageModel.localThumbDataUrl.bind
											}
											case false => {
												ImageUtils.hirezSourceRemote(roi)
											}
										}	
								}
								case None => ""
							}	
					}>
					<img id="ict3_imgPreview" src={
							ImageModel.thumbUrn.bind match {
								case Some(roi) => {
										ImageModel.useLocal.bind match {
											case false => ImageUtils.thumbSourceRemote(roi) 
											case true => {
												ImageUtils.updateThumbSourceLocal(roi)
												ImageModel.localThumbDataUrl.bind
											}
										}	
								}
								case None => ""
							}	
					}/>
				</a>
			</div>
			{ newObjectField.bind }
			<div id="ict3_dataPairs">
					<ul id="ict3_dataPairsList">
					{ for ( ct <- MainModel.currentImageROIs) yield {
						<li id= { s"li_${MainModel.tripleToId(ct)}" }
								onmouseenter={ event: Event => {
										val idx = MainModel.currentImageROIs.value.toVector.indexOf(ct)
										val idxString = s"image_mappedROI_${idx}"
										ImageUtils.ICT_HighlightData.highlightData(idxString)
										ImageUtils.ICT_HighlightROI.highlightROI(idxString)
									}
								}	
								onmouseleave={ event: Event => {
										val idx = MainModel.currentImageROIs.value.toVector.indexOf(ct)
										val idxString = s"image_mappedROI_${idx}"
									}
								}	
								
								class={
									val groupIndex: Int = MainModel.currentImageROIs.value.indexOf(ct)
									val groupId: Int = (groupIndex % 25) 
									s"image_roiGroup image_roiGroup_${groupId}"
								}>
							
							<a
								onclick={ event: Event => {
									val mouseEvent = event.asInstanceOf[MouseEvent]
									if (mouseEvent.metaKey){
										MainView.showURNPopup(ct)
									} else {
										MainView.showURNPopup(ct)
									}
								} 

								}
								id={ s"roi_record_${MainModel.tripleToId(ct)}" }
								class="image_roi_link" 

								> {  

								<span class="ict3_data_urn ict3_data_urn1">{ s"${MainModel.truncUrn(ct.urn1)}" }</span>
								<span class="ict3_data_urn ict3_data_urn2">{ s"${MainModel.truncUrn(ct.urn2)}" }</span>

								} </a>
								<button onclick={ event: Event => {
										val roi = ct.urn1.asInstanceOf[Cite2Urn]
										ImageModel.thumbUrn.value = Some(roi)	
										//ImageUtils.loadPreview(roi, ImageModel.useLocal.value)	
									}}>Show Detail</button>
								<button
								onclick={ event: Event => MainModel.deleteRelation( ct )}
								class="ict3_editDeleteButton" id= { s"ict3_delete_${MainModel.tripleToId(ct)}" } >Delete</button>
							</li>
						} 
					}	
					</ul>
			</div>			
		</div>
	}

	@dom
	def filePicker = {
		<span id="app_filePickerSpan">
			<label for="app_filePicker">Choose a local <code>.cex</code> file</label>
			<input
				id="app_filePicker"
				type="file"
				onchange={ event: Event => MainController.loadLocalLibrary( event )}
				></input>
		</span>
	}

		@dom
		def configDiv = {
			<div id="ict_config" class="ict3_config_div">
				<a id="configShowHide" class="showHideSection"
					onclick={ event: Event => {
						MainView.configDivShown.value = !(MainView.configDivShown.value)
					} }
					>{
					if (MainView.configDivShown.bind) "hide" else "show"
				}</a>
				<span id="ict3_SectionHeading">Configuration</span>
				<div id="configHideWrapper" class={
					if (MainView.configDivShown.bind) "app_visible" else "app_hidden"
					}>
					<span class="groupingSpan">
						<label for="textUrnField">URN Base for Relations</label>
						<input
						 onkeyup={ updateDefaultDataValue }
						 id="textUrnField" type="text" size={60} value={ MainModel.defaultDataUrn.bind } />
					</span>

					{ imageLocalRemoteSwitch.bind }
				</div>

			</div>
		}

		val exportDivShown = Var(false)
		val configDivShown = Var(false)

		@dom 
		def exportDiv = {
			<div id="ict_export" class="ict3_config_div">
				<a id="exportShowHide" class="showHideSection"
					onclick={ event: Event => {
						MainView.exportDivShown.value = !(MainView.exportDivShown.value)
					} }
				>{
					if (MainView.exportDivShown.bind) "hide" else "show"
				}</a>
				<span id="ict3_SectionHeading">Export Settings</span>
				<div id="exportHideWrapper" class={
					if (MainView.exportDivShown.bind) "app_visible" else "app_hidden"
					}>
					<span class="groupingSpan">
						<label for="fileNameField">Filename for CEX</label>
						<input id="fileNameField" type="text" size={30} value={ MainModel.defaultExportFilename.bind }
							onchange={ event: Event => {
								val thisTarget = event.target.asInstanceOf[org.scalajs.dom.raw.HTMLInputElement]
								val tempFileName:String = thisTarget.value
								MainModel.defaultExportFilename.value = tempFileName
							}}
						/>
					</span>
					<span class="groupingSpan">
						<label for="userNameField">Editor Name</label>
						<input id="userNameField" type="text" size={30} value={ MainModel.defaultExportUsername.bind } 
							onchange={ event: Event => {
								val thisTarget = event.target.asInstanceOf[org.scalajs.dom.raw.HTMLInputElement]
								val tempUserName:String = thisTarget.value
								MainModel.defaultExportUsername.value = tempUserName
							}}
							/>
					</span>

					{ saveCancelButtons.bind }

					<span class="">
						<label for="saveAs2columnCheckbox">Save as 2-column file</label>
						<input 
							onchange={ event: Event => {
								val thisTarget = event.target.asInstanceOf[org.scalajs.dom.raw.HTMLInputElement]
								val tempFileName:String = MainModel.defaultExportFilename.value
								MainModel.saveAs2Column.value = thisTarget.checked 
								MainModel.saveAs2Column.value match {
									case true => { 
										MainModel.defaultExportFilename.value = tempFileName.replaceAll(".cex",".tsv")
										}
									case false => {
										MainModel.defaultExportFilename.value = tempFileName.replaceAll(".tsv",".cex")
									}
								}

							}}
							id="saveAs2columnCheckbox" type="checkbox"  checked={ MainModel.saveAs2Column.bind } />
					</span>

				</div>

			</div>
		}

		@dom
		def saveCancelButtons = {
			<span id="downloadCancelOkay">
				<button id="downloadOkay"
					type="button"
					disabled = { false }
					onclick={ event: Event => {
						SaveDialog.assembleAndSaveCex
					} }
					
				>Download</button>
				<button id="downloadCancel"
					type="button"
					disabled = { false }
				>Cancel</button>
			</span>
		}

		@dom
		def htmlFooter = {
			<p>
				CITE/CTS is ©2002–2019 Neel Smith and Christopher Blackwell. 
				This tool for working with images cited via CITE3 URNs is ©2019 by Christopher Blackwell. 
				ICT3 is available for use, modification, and distribution under the terms of the <a href="https://opensource.org/licenses/gpl-3.0.html">GPL 3.0</a> license. ICT3 takes advantage of <a href="http://openseadragon.github.io">Openseadragon</a>.
		</p>
		}

	@dom
	def imageLocalRemoteSwitch = {
			<div id="imageSourceSwitchContainer" class="app_visible">
				Image Source:
				<div class="onoffswitch app_visible">
				    <input type="checkbox" name="onoffswitch" class="onoffswitch-checkbox app_visible"
				    id="citeMain_localImageSwitch" checked={!(ImageModel.useLocal.bind)}
						onchange={ event: Event => {
								ImageUtils.setPreferredImageSource 
							}
					}/>
			    <label class="onoffswitch-label" for="citeMain_localImageSwitch">
			        <span class="image_onoffswitch-inner onoffswitch-inner"></span>
			        <span class="image_onoffswitch-switch onoffswitch-switch"></span>
			    </label>
				</div>
				<span class="app_visible">
					<span class={
							ImageModel.useLocal.bind match {
								case true => "app_visible"	
								case false => "app_hidden"
							}
						}>Using Local Images</span>
					<span class={
							ImageModel.useLocal.bind match {
								case false => "app_visible"	
								case true => "app_hidden"
							}
						}>Using Remote Images</span>
			  	</span>
			</div>
	}

	@dom
	def mainMessageDiv = {
			<div id="main_message" class={ s"app_message ${MainModel.userMessageVisibility.bind} ${MainModel.userAlert.bind}" } >
				{ MainModel.userMessage.bind }
			</div>
	}

	def showURNPopup(ct:CiteTriple):Unit = {
		val alertMessage = s"URNs for Copying:\n\n${ct.urn1.toString}\n\n${ct.relation.toString}\n\n${ct.urn2.toString}"
		g.window.alert(alertMessage)	
	}


}
