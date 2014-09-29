function jumpToProblem(methodIndex, pc, lineNumber) {
	var methodsBlock = document.querySelector('.methods details')
	if (methodsBlock != undefined) {
		methodsBlock.open = true;
	}
	
	var methodId = 'm' + methodIndex;
	var methodBlock = document.querySelector('#' + methodId + ' details');
	if (methodBlock != undefined) {
		methodBlock.open = true;
	}
	
	var lineId = 'line' + lineNumber;
	var pcId = methodId + '_pc' + pc;
	
	var lineElement = document.getElementById(lineId);
	var pcElement = document.getElementById(pcId);
	if (lineElement != undefined) {
		lineElement.scrollIntoView();
	} else if (pcElement != undefined) {
		pcElement.scrollIntoView();
	} else if (methodBlock != undefined) {
		document.getElementById(methodId).scrollIntoView();
	} else {
		window.scrollTo(0,0);
	}
}