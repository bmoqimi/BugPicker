Prism.hooks.add('after-highlight', function(env) {
	var numberSpans = Array.prototype.slice.call(document.querySelectorAll('.line-numbers-rows > span'))
	numberSpans.forEach(function(span,index) {
		var lineNumber = index + 1;
		span.id = 'line' + lineNumber;
	})
})