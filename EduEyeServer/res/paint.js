//Paint element
//Draw stuff on canvas
var Painter =  {
	radius:  10,
	fillColor: 'rgba(255,255,255,0.1',
	mode: 'highlighter',
	doCrop: false,

	init: function(element) {
		Painter.canvas = document.getElementById(element);
		Painter.ctx = Painter.canvas.getContext('2d');

		Painter.mousePointer = document.getElementById('mousePointer');
		Painter.mousePointer.ctx = Painter.mousePointer.getContext('2d');

		Painter.crop = document.getElementById('crop');
		Painter.crop.ctx = Painter.crop.getContext('2d');

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
		if(!Painter.doCrop) {
			var x = e.pageX - this.offsetLeft;
			var y = e.pageY - this.offsetTop;
			Painter.fillCircle(x, y, Painter.radius, Painter.fillColor);
		}
	},

	onmousedown: function(e) {
		
		if(Painter.doCrop) {
			//if mouse down happens on top of square, mark onCrop true	
			if(e.pageX >= Painter.x0 && e.pageY >= Painter.y0 && e.pageX <= Painter.x1 && e.pageY <= Painter.y1) {
				console.log("ontop");
				Painter.canvas.onCrop = true;

				//save clicked position to calculate 
				//change in position
				//and closes corner
				Painter.x2 = e.pageX; 
				Painter.y2 = e.pageY; 
			}
			else {
				Painter.x0 = e.pageX; 
				Painter.y0 = e.pageY;
			}
		}

		if(e.button == 2) { 
			Painter.oldMode = Painter.mode;
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
		//set back highlighter (or any other tool)
		if(Painter.oldMode) {
			Painter.mode = Painter.oldMode;
			if(Painter.mode === 'highlighter') {
				Painter.setHighlighter();
			}
		}
		Painter.oldMode = null;

		if(Painter.doCrop) {
			Painter.x1 = e.pageX; 
			Painter.y1 = e.pageY;
		}

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
		if($('#highlighter').hasClass('active')) {
			$('#highlighter').removeClass('active');
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
		if($('#eraser').hasClass('active')) {
			$('#eraser').removeClass('active');
		}
	},

	animate: function() {
		requestAnimationFrame(Painter.animate);
		Painter.mousePointer.ctx.clearRect(0, 0, Painter.mousePointer.width, Painter.mousePointer.height);
		Painter.crop.ctx.clearRect(0, 0, Painter.crop.width, Painter.crop.height);

		Painter.drawPointer();	

		if(Painter.doCrop) {
			Painter.drawCrop();	
		}	
	},

	movepointer: function(e) {
		Painter.x = e.pageX
		Painter.y = e.pageY;
	},

	drawPointer: function() {
				//Painter.crop.ctx.clearRect(0, 0, Painter.crop.width, Painter.crop.height);

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
	}, 

	drawCrop: function() {
		var context = Painter.crop.ctx;

		//Painter.crop.ctx.beginPath();
		Painter.crop.ctx.fillStyle = '#222';
		Painter.crop.ctx.fillRect(0,0, Painter.crop.width, Painter.crop.height);
		Painter.crop.ctx.fill();


			//Drawing and started on top of the square
		if(Painter.canvas.isDrawing && Painter.canvas.onCrop) {

			//Left top  x0,y0
			//width 	x1 - x0
			//height	y1 - y0

			//change in width = w + (x2 - x)
			//change in height = h + (y1 - y)

			// This solutions works the worng way (althoug it works)
			//var recW = (Painter.x1 - Painter.x0) + (Painter.x2 - Painter.x); 
			//var recH = (Painter.y1 - Painter.y0) + (Painter.y2 - Painter.y);

			var recW = (Painter.x1 - Painter.x0) + (Painter.x - Painter.x2); 
			var recH = (Painter.y1 - Painter.y0) + (Painter.y - Painter.y2);
			
			//var recW = Painter.x - Painter.x0; 
			//var recH = Painter.y - Painter.y0;
			context.beginPath();
		    context.rect(Painter.x0, Painter.y0, recW, recH);
		    context.fill();
		    context.lineWidth = 8;
		    context.strokeStyle = 'rgba(255,255,255,0.5)';
		    context.stroke();

		}

		else if(Painter.canvas.onCrop && !Painter.canvas.isDrawing) {

//			Painter.x1 = Painter.x + (Painter.x - Painter.x2);
//			Painter.y1 = Painter.y + (Painter.y - Painter.y2);

			// draws the square after mouse up (final shape after resize)
			// atm. the square will be resized to the position of the mouse at end of editing
			// this should be fixed
			var recW = Painter.x1 - Painter.x0; 
			var recH = Painter.y1 - Painter.y0;
			context.beginPath();
		    context.rect(Painter.x0, Painter.y0, recW, recH);
		    context.fill();
		    context.lineWidth = 8;
		    context.strokeStyle = 'rgba(255,255,255,0.5)';
		    context.stroke();


			Painter.canvas.onCrop = false;
		}
				//if true, clicked down
		else if(Painter.canvas.isDrawing) {
			var recW = Painter.x - Painter.x0; 
			var recH = Painter.y - Painter.y0;

		    context.beginPath();
		    context.rect(Painter.x0, Painter.y0, recW, recH);
		    context.fill();
		    context.lineWidth = 8;
		    context.strokeStyle = 'rgba(255,255,255,0.5)';
		    context.stroke();
		}
		else {
			var recW = Painter.x1 - Painter.x0; 
			var recH = Painter.y1 - Painter.y0;
			context.beginPath();
		    context.rect(Painter.x0, Painter.y0, recW, recH);
		    context.fill();
		    context.lineWidth = 8;
		    context.strokeStyle = 'rgba(255,255,255,0.5)';
		    context.stroke();
		}

		Painter.crop.ctx.clearRect(Painter.x0, Painter.y0, recW, recH);


	}

	
};



