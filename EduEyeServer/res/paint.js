//Paint element
//Draw stuff on canvas
var Painter =  {
	radius:  10,
	fillColor: 'rgba(255,255,255,0.1',
	mode: 'highlighter',

	init: function(element) {
		Painter.canvas = document.getElementById(element);
		Painter.ctx = Painter.canvas.getContext('2d');

		Painter.mousePointer = document.getElementById('mousePointer');
		Painter.mousePointer.ctx = Painter.mousePointer.getContext('2d');

		Painter.crop = document.getElementById('crop');
		Painter.crop.ctx = Painter.mousePointer.getContext('2d');

		Painter.compositeOperation = Painter.ctx.globalCompositeOperation;

		//make it fullscreen!
		Painter.canvas.width = $(window).width();
		Painter.canvas.height = $(window).height();
		Painter.mousePointer.width = $(window).width();
		Painter.mousePointer.height = $(window).height();
		Painter.crop.width = $(window).width();
		Painter.crop.height = $(window).height();

		//add event listeners
		Painter.canvas.addEventListener('mousedown', Painter.onmousedown, false); 
		Painter.canvas.addEventListener('mouseup', Painter.onmouseup, false); 
		Painter.canvas.addEventListener('mousemove', Painter.onmousemove, false); 

		//Painter.mousePointer.addEventListener('mousemove', Painter.movepointer, false);

		Painter.animate();
	 },

	fillCircle: function(x, y, radius, fillColor) {
		var ctx = Painter.ctx;
		//ctx.fillStyle = fillColor;
		ctx.lineTo(x, y);
        ctx.stroke();
		//ctx.arc(x, y, radius, 0, Math.PI*2, false);
		//ctx.fill();
	},

	//bind mouse events
	onmousemove: function(e) {

		Painter.x = e.pageX
		Painter.y = e.pageY;

		if(!Painter.canvas.isDrawing) {
			return; 
		}
		var x = e.pageX - this.offsetLeft;
		var y = e.pageY - this.offsetTop;
		Painter.fillCircle(x, y, Painter.radius, Painter.fillColor);
	},

	onmousedown: function(e) {
		if(e.button == 2) {
			Painter.setEraser();
		}
		else if(Painter.mode=='highlighter'){
			Painter.setHighlighter();
		}

		Painter.canvas.isDrawing = true;
		var ctx = Painter.ctx;

		//ctx.moveTo(x, y);
		ctx.strokeStyle = Painter.fillColor;
        ctx.lineWidth = Painter.radius;
        ctx.lineCap = 'round';
     	ctx.beginPath(); 
     	ctx.moveTo(e.pageX, e.pageY);
    	 


	},

	onmouseup: function(e) {
		Painter.canvas.isDrawing = false;
	},

	clearCanvas: function() {
		//Clear canvas
		Painter.ctx.clearRect(0, 0, Painter.canvas.width, Painter.canvas.height);
	},

	setEraser: function() {
		Painter.mode = 'eraser';
		Painter.fillColor = 'rgba(0,0,0,1.0)';
		Painter.ctx.globalCompositeOperation = 'destination-out';
		Painter.radius = 30;

		if(!$('#eraser').hasClass('active')) {
			$('#eraser').addClass('active');
		}
	},

	setHighlighter : function() {
		Painter.mode = 'highlighter';
		Painter.fillColor = 'rgba(255,255, 0, 0.5)';
		Painter.ctx.globalCompositeOperation =  'destination-atop';
		Painter.radius = 10;
		
		if(!$('#highlighter').hasClass('active')) {
			$('#highlighter').addClass('active');
		}
	},

	animate: function() {
		requestAnimationFrame(Painter.animate);
		Painter.mousePointer.ctx.clearRect(0, 0, Painter.mousePointer.width, Painter.mousePointer.height);
		Painter.drawPointer();		
	},

	movepointer: function(e) {
		Painter.x = e.pageX
		Painter.y = e.pageY;
	},

	drawPointer: function() {
		if($('#controls').is(':hover')) {
			Painter.mousePointer.ctx.clearRect(0, 0, Painter.mousePointer.width, Painter.mousePointer.height);
		}

		else {
			var context = Painter.mousePointer.ctx;
			if(Painter.mode == 'highlighter') {

			    context.beginPath();
			    context.arc(Painter.x, Painter.y, Painter.radius, 0, Math.PI*2, false);
			    context.fillStyle = Painter.fillColor;
			    context.fill();
			    context.strokeStyle = Painter.fillColor;
			    context.stroke();

			}

			else if(Painter.mode == 'eraser') {
			    
			    context.beginPath();
			    context.arc(Painter.x, Painter.y, Painter.radius, 0, Math.PI*2, false);
			    context.fillStyle = 'rgba(255,255,255,0)';
			    context.fill();
			    context.lineWidth = 2;
			    context.strokeStyle = '#999';
			    context.stroke();

			}
		}
	}

	
};



