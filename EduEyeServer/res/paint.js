//Paint element
//Draw stuff on canvas
var Painter =  {
	radius:  10,
	fillColor: 'rgba(255,255,255,0.1',
	mode: 'highlighter',

	init: function(element) {
		Painter.canvas = document.getElementById(element);
		Painter.ctx = Painter.canvas.getContext('2d');
		Painter.compositeOperation = Painter.ctx.globalCompositeOperation;

		//make it fullscreen!
		Painter.canvas.width = $(window).width();
		Painter.canvas.height = $(window).height();

		//add event listeners
		Painter.canvas.addEventListener('mousedown', Painter.onmousedown, false); 
		Painter.canvas.addEventListener('mouseup', Painter.onmouseup, false); 
		Painter.canvas.addEventListener('mousemove', Painter.onmousemove, false); 
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
	},

	setHighlighter : function() {
		Painter.mode = 'highlighter';
		Painter.fillColor = 'rgba(255,255, 0, 0.6)';
		Painter.ctx.globalCompositeOperation =  'destination-atop';
		Painter.radius = 10;
	}


	
};



