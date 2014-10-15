// Functions to scroll to points in decompiled bytecode
function openBytecodeMethodsBlock() {
	var methodsBlock = document.querySelector('.methods details');
	if (methodsBlock != undefined) {
		methodsBlock.open = true;
	}
}
function getBytecodeMethodBlock(methodIndex) {
	return document.querySelector('#m' + methodIndex + ' details');
}
function openBytecodeMethod(methodIndex) {
	var methodBlock = getBytecodeMethodBlock(methodIndex);
	if (methodBlock != undefined) {
		methodBlock.open = true;
	}
}
function jumpToMethodInBytecode(methodIndex) {
	openBytecodeMethodsBlock();
	openBytecodeMethod(methodIndex);
	getBytecodeMethodBlock(methodIndex).scrollIntoView();
}
function jumpToProblemInBytecode(methodIndex, pc) {
	openBytecodeMethodsBlock();
	openBytecodeMethod(methodIndex);
	
	var pcElement = document.getElementById('m' + methodIndex + '_pc' + pc);
	if (pcElement != undefined) {
		pcElement.scrollIntoView();
	} else if (getBytecodeMethodBlock(methodIndex) != undefined) {
		getBytecodeMethodBlock(methodIndex).scrollIntoView();
	} else {
		window.scrollTo(0,0);
	}
}

// Functions to scroll to points in bytecode
function jumpToLineInSourceCode(line) {
	var lineElement = document.getElementById('line' + line)
	if (lineElement != undefined) {
		lineElement.scrollIntoView();
	} else {
		window.scrollTo(0,0);
	}
}