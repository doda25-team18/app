$(document).ready(function() {

	var correctCount = 0;
	var incorrectCount = 0;

	function getSMS() {
		return $("textarea").val().trim()
	}
	
	function getGuess() {
		return $("input[name='guess']:checked").val().trim()
	}
	
	function cleanResult() {
		$("#result").removeClass("correct")
		$("#result").removeClass("incorrect")
		$("#result").removeClass("error")
		$("#result").html()
	}

	$("button").click(function (e) {
		e.stopPropagation()
		e.preventDefault()

		var sms = getSMS()
		var guess = getGuess()
		
		$.ajax({
			type: "POST",
			url: "./",
			data: JSON.stringify({"sms": sms, "guess": guess}),
			contentType: "application/json",
			dataType: "json",
			success: handleResult,
			error: handleError	
		})
	})

	function handleResult(res) {
		var wasRight = res.result == getGuess()

		if (wasRight) {
			correctCount++;
		} else {
			incorrectCount++;
		}
		updateStats();

		cleanResult()		
		$("#result").addClass(wasRight ? "correct" : "incorrect")
		$("#result").html("The classifier " + (wasRight ? "agrees" : "disagrees"))		
		$("#result").show()
	}
	
	function handleError(e) {
		cleanResult()		
		$("#result").addClass("error")
		$("#result").html("An error occured (see server log).")
		$("#result").show()
	}
	
	$("textarea").on('keypress',function(e) {
		$("#result").hide()
	})
	
	$("input").click(function(e) {
		$("#result").hide()
	})

	function updateStats() {
		$("#correct-count").text(correctCount);
		$("#incorrect-count").text(incorrectCount);
		
		var totalCount = correctCount + incorrectCount;
		var accuracy = 0;
		if (totalCount > 0) {
			accuracy = Math.round((correctCount / totalCount) * 100);
		}
		$("#accuracy").text(accuracy + "%");
	}
})