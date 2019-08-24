console.log("ict3.js loaded.")

var roiArray = [];
var viewer = null;
var initialLoadDone = false;
var imgUrn = "";

function clearJsRoiArray(r) {
	console.log("got here…");
	for (let i = 0; i < roiArray.length; i++) {
		var tid = "image_mappedROI_" + i;
		console.log("trying to delete " + tid);
		viewer.removeOverlay(tid);
	}
	roiArray = [];
}

function removeTempROI(r) {
	var tempArray = roiArray;
	roiArray = [];
	var tid = "ict3_temp_ROI";
	if (viewer != null) {
		viewer.removeOverlay(tid);
		for ( let i = 0; i < tempArray.length; i++) {
			if (tempArray[i].roiObjectId == "ict3_temp_ROI") {
			} else {
				roiArray.push(tempArray[i]);
			}
		}
	} else {
		console.log("null viewer");
	}
	return true;
}

/* roiArray consists of objects like this:

roiObject
			.imageUrnString
			.imageRoiString
			.classNameString
			.roiObjectId
*/

// def apply(imageUrnString: String, imageRoiString: String, classNameString: String, roiObjectId: String): js.Dynamic = js.native

function addToJsRoiArray(ius, irs, cns, roiId){
	tempMap = {imageUrnString: ius, imageRoiString: irs, classNameString: cns, roiObjectId: roiId};
	roiArray.push(tempMap);
}

/**
 * Converts a rectangle object into a ROI we can use in a URN
 * @param  {Rectangle} rect a rectangle object.
 * @return {string}   a string that describes the rectangle in percentages
 */
function rectToRoi(rect){
	var normH = viewer.world.getItemAt(0).getBounds().height;
	var normW = viewer.world.getItemAt(0).getBounds().width;
	roiRect = viewer.viewport.imageToViewportRectangle(rect);
	var rl = roiRect.x / normW;
	var rt = roiRect.y / normH;
	var rw = roiRect.width / normW;
	var rh = roiRect.height / normH;
	var newRoi = rl.toPrecision(4) + "," + rt.toPrecision(4) + "," + rw.toPrecision(4) + "," + rh.toPrecision(4);
	return newRoi;
}

function updateImageJS(path, imageUrnString){
		imgUrn = imageUrnString;
	  initOpenSeadragon(path)
}

/* Initiatlize OpenSeadragon viewer with guides, selection, and pre-load any urn */
function initOpenSeadragon(path) {
  initialLoadDone = false;

		if (viewer != null){
				viewer.destroy();
				viewer = null
		}

	viewer = OpenSeadragon({
		id: 'image_imageContainer',
		prefixUrl: 'css/images/',
		crossOriginPolicy: "Anonymous",
		defaultZoomLevel: 1,
		tileSources: path,
		// tileSources: 'http://www.homermultitext.org/iipsrv?DeepZoom=/project/homer/pyramidal/VenA/VA012RN_0013.tif.dzi',
		minZoomImageRatio: 0.1, // of viewer size
		immediateRender: true
	});

	viewer.addHandler('full-screen', function (viewer) {
		refreshRois();
	})

	// Guides plugin
	viewer.guides({
		allowRotation: false,        // Make it possible to rotate the guidelines (by double clicking them)
		horizontalGuideButton: null, // Element for horizontal guideline button
		verticalGuideButton: null,   // Element for vertical guideline button
		prefixUrl: "css/images/",             // Images folder
		removeOnClose: false,        // Remove guidelines when viewer closes
		useSessionStorage: false,    // Save guidelines in sessionStorage
		navImages: {
			guideHorizontal: {
				REST: 'guidehorizontal_rest.png',
				GROUP: 'guidehorizontal_grouphover.png',
				HOVER: 'guidehorizontal_hover.png',
				DOWN: 'guidehorizontal_pressed.png'
			},
			guideVertical: {
				REST: 'guidevertical_rest.png',
				GROUP: 'guidevertical_grouphover.png',
				HOVER: 'guidevertical_hover.png',
				DOWN: 'guidevertical_pressed.png'
			}
		}
	});

	//selection plugin
	selection = viewer.selection({
		restrictToImage: true,
		onSelection: function(rect) {
			createROI(rect);
			//addRoiOverlay()
		}
	});


	// Openseadragon does not have a ready() function, so here we are…
	// Add overlays
			setTimeout(function(){
				if (viewer.world.getItemAt(0)){
				} else {
				}
				var baseTimer = 0;
				while( !(viewer.world.getItemAt(0))){
					baseTimer = baseTimer + 1;
				}
				var normH = viewer.world.getItemAt(0).getBounds().height;
				var normW = viewer.world.getItemAt(0).getBounds().width;
				if (roiArray.length > 0){
					for (ol = 0; ol < roiArray.length; ol++){
						addRoiOverlay(roiArray[ol], normH, normW);
						/*
						var roi = roiArray[ol].imageRoiString;
						var rl = +roi.split(",")[0]
						var rt = +roi.split(",")[1]
						var rw = +roi.split(",")[2]
						var rh = +roi.split(",")[3]
						var tl = rl * normW
					  var tt = rt * normH
					  var tw = rw * normW
					  var th = rh * normH
						var osdRect = new OpenSeadragon.Rect(tl,tt,tw,th)
						var elt = document.createElement("a")
						elt.id = roiArray[ol].roiObjectId;
	 			    elt.className = roiArray[ol].classNameString;

						viewer.addOverlay(elt,osdRect)
						*/
					}
				}
			},2000);
}

/**
  * Intercepts new object from Scala, and adds a rect
  *
*/
function jsGetsRectFromScala(ius, irs, cns, roiId){
	tempMap = {imageUrnString: ius, imageRoiString: irs, classNameString: cns, roiObjectId: roiId};
	addRoiOverlay(tempMap);
}

	/**
 * Adds a new ROI overlay using the provided ROI object
 * @param {Object} roiObj   the object that contains the data needed to create
 *                          the overlay for this ROI
 */
function addRoiOverlay(roiObj) {
	var normH = viewer.world.getItemAt(0).getBounds().height;
	var normW = viewer.world.getItemAt(0).getBounds().width;
	var roi = roiObj.imageRoiString;
	var rl = +roi.split(",")[0];
	var rt = +roi.split(",")[1];
	var rw = +roi.split(",")[2];
	var rh = +roi.split(",")[3];
	var tl = rl * normW;
	var tt = rt * normH;
	var tw = rw * normW;
	var th = rh * normH;
	var osdRect = new OpenSeadragon.Rect(tl,tt,tw,th);
	var elt = document.createElement("a");
	elt.id = roiObj.roiObjectId;
	elt.className = roiObj.classNameString;

	viewer.addOverlay(elt,osdRect);

	/*
	$("a#" + elt.id ).on("click",function(){
		if ( $(this).hasClass("image_roiGroupSelected")){
			removeAllHighlights();
		} else {
			removeAllHighlights();
			$(this).addClass("image_roiGroupSelected");
			ict2_drawPreviewFromUrn( $(this).data("urn") );
			var liId = roiToUrnId(this.id);
			$("li#"+liId).addClass("image_roiGroupSelected");
		}
	});
	*/

}

/**
 * Creates a ROI from the selection rect created by Openseadragon.
 * @param  {Rectangle} rect rectangular object (the selection)
 */
function createROI(rect){
	var newRoi = rectToRoi(rect);
	var newUrnStripped = imgUrn.split("@")[0]
	var newUrn = newUrnStripped + "@" + newRoi;

	// image_mappedROI image_roiGroup_new image_mappedUrn_new
	var newClassName = "image_mappedROI image_roiGroup_new image_mappedUrn_new";
	var newRoiId = "ict3_temp_ROI";

	var roiObj = {imageUrnString: newUrn, imageRoiString: newRoi, classNameString: newClassName, roiObjectId: newRoiId};
	roiArray.push(roiObj);
	addRoiOverlay(roiObj);
	addRoiListing(roiObj);
	//updateShareUrl();
}

function addRoiListing(roiObject) {
	//var urnField = document.getElementById("newImageRoiUrnSpan");
	//urnField.innerHTML = roiObject.imageUrnString;
	ICT_NewROI.userDrewNewROI(roiObject.imageUrnString);
}

/*
roiObject
			.imageUrnString
			.imageRoiString
			.classNameString
			.roiObjectId
*/
