//Paint element
//Draw stuff on canvas
var Painter =  {
	radius:  10,
	fillColor: 'rgba(255,255,255,0.1',
	brushColor: {r: 255, g: 0, b: 255, a: 128},
	mode: 'highlighter',
	doCrop: false,
	onCrop : false,

	init: function(element) {
		Painter.canvas = document.getElementById(element);
		Painter.ctx = Painter.canvas.getContext('2d');

		Painter.mousePointer = document.getElementById('mousePointer');
		Painter.mousePointer.ctx = Painter.mousePointer.getContext('2d');

		Painter.crop = document.getElementById('crop');
		Painter.crop.ctx = Painter.crop.getContext('2d');

		Painter.compositeOperation = Painter.ctx.globalCompositeOperation;
		console.log(Painter.ctx.globalCompositeOperation)
		//make it fullscreen!
		Painter.canvas.width = $(window).width();
		Painter.canvas.height = $(window).height();
		Painter.mousePointer.width = $(window).width();
		Painter.mousePointer.height = $(window).height();
		Painter.crop.width = $(window).width();
		Painter.crop.height = $(window).height();

		//add event listeners
		Painter.canvas.addEventListener('mousedown', Painter.onmousedown, true); 
		Painter.canvas.addEventListener('mouseup', Painter.onmouseup, true); 
		Painter.canvas.addEventListener('mousemove', Painter.onmousemove, true); 

		//Painter.mousePointer.addEventListener('mousemove', Painter.movepointer, false);
	 },

	fillCircle: function(ctx, x0, y0) {
		if (Painter.mode === "highlighter" || Painter.mode === "eraser") {
			var px = ctx.createImageData(1, 1);
			var size = Painter.radius;
			
			px.data[0] = Painter.brushColor.r;
			px.data[1] = Painter.brushColor.g;
			px.data[2] = Painter.brushColor.b;
			px.data[3] = Painter.brushColor.a;
			
			
			var dist = Math.sqrt(Math.pow(x0 - Painter.oldX, 2) + Math.pow(y0 - Painter.oldY, 2));
			var dx = dist === 0 ? 0 : (x0 - Painter.oldX) / dist;
			var dy = dist === 0 ? 0 : (y0 - Painter.oldY) / dist;
			
			// Draw everything.
			for (var i = 0; i <= dist; i+=size/2) {
				var x1 = i * dx + Painter.oldX;
				var y1 = i * dy + Painter.oldY;
				
				for (var y = y1 - size; y < y1 + size; ++y) {
					for (var x = x1 - size; x < x1 + size; ++x) {
						if (Math.pow(x-x1, 2) + Math.pow(y-y1, 2) < size*size) {
							ctx.putImageData(px, x, y);
						}
					}
				}
			}

			Painter.oldX = x0;
			Painter.oldY = y0;
		}
	},

	//bind mouse events
	onmousemove: function(e) {
		e.preventDefault();
		e.stopPropagation();
		
		Painter.x = e.pageX;
		Painter.y = e.pageY;
		
		Painter.drawPointer();
		
		if(Painter.mode !=='hand' && Painter.canvas.isDrawing) {
			var x = e.pageX - this.offsetLeft;
			var y = e.pageY - this.offsetTop;
			Painter.fillCircle(Painter.ctx, x, y);
		}
		

		if(Painter.doCrop) {
			Painter.drawCrop();	
		}
		
		Painter.oldX = e.pageX;
		Painter.oldY = e.pageY;
	},

	onmousedown: function(e) {
		e.preventDefault();
		e.stopPropagation();
		
		if (e.button === 2) { 
			Painter.oldMode = Painter.mode;
			Painter.setEraser();
		}
		else if (Painter.mode === 'highlighter'){
			Painter.setHighlighter();
		}
		else if (Painter.mode === 'hand') {
			Painter.setHand();
		}


		if(Painter.doCrop && Painter.mode == 'hand') {
			//if mouse down happens on top of square, mark onCrop true	
			if(e.pageX >= Painter.x0 && e.pageY >= Painter.y0 && e.pageX <= Painter.x1 && e.pageY <= Painter.y1) {
				//click happened inside old selected area
				//We shall resize it in stead of drawing a new one
				Painter.onCrop = true;

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

		Painter.canvas.isDrawing = true;
		/*
		var ctx = Painter.ctx;
		ctx.strokeStyle = Painter.fillColor;
        ctx.lineWidth = Painter.radius;
        ctx.lineCap = 'round';
     	ctx.beginPath(); 
     	ctx.moveTo(e.pageX, e.pageY);
		*/
		Painter.oldX = e.pageX;
		Painter.oldY = e.pageY;
		
		Painter.fillCircle(Painter.ctx, Painter.oldX, Painter.oldY);
		//requestAnimationFrame(function() {});
	},

	onmouseup: function(e) {
		e.preventDefault();
		e.stopPropagation();
		
		if(Painter.doCrop && Painter.mode == 'hand' && !Painter.onCrop) {
			Painter.x1 = e.pageX; 
			Painter.y1 = e.pageY;
		}


		//set back highlighter (or any other tool)
		if (Painter.oldMode) {
			$('.selectable').removeClass('active');
			Painter.mode = Painter.oldMode;

			if(Painter.mode === 'highlighter') {
				Painter.setHighlighter();
			}
			else if(Painter.mode === 'hand') {
				Painter.setHand();
			}
		}
		Painter.oldMode = null;

		Painter.canvas.isDrawing = false;

	},

	// Called by an UI button.
	clearCanvas: function() {
		//Clear canvas
		Painter.ctx.clearRect(0, 0, Painter.canvas.width, Painter.canvas.height);
	},

	setEraser: function() {
		Painter.mode = 'eraser';
		Painter.brushColor = {r: 0, g: 0, b: 0, a:0};
		Painter.fillColor = 'rgba(0,0,0,1.0)';
		Painter.radius = 30;
		$('.selectable').removeClass('active');
		$('#eraser').addClass('active');

	},

	setHighlighter : function() {

		Painter.removePointer();
		Painter.mode = 'highlighter';
		Painter.brushColor = {r: 255, g: 255, b: 0, a: 128};
		Painter.fillColor = 'rgba(255,255, 0, 0.5)';
		Painter.radius = 10;
		$('.selectable').removeClass('active');
		$('#highlighter').addClass('active');
	},

	setHand: function() {
		Painter.removePointer();
		Painter.mode = 'hand';
		$('.selectable').removeClass('active');
		$('#hand').addClass('active');
	},

	drawPointer: function() {
		Painter.removePointer();
		var oldX = Painter.oldX;
		var oldY = Painter.oldY;
		
		// Remove the old pointer
		var context = Painter.mousePointer.ctx;
		var temp = Painter.brushColor.a;
		Painter.radius += 2;
		Painter.brushColor.a = 0;
		Painter.fillCircle(context, Painter.oldX, Painter.oldY);
		Painter.brushColor.a = temp;
		Painter.radius -= 2;

		
		Painter.oldX = Painter.x;
		Painter.oldY = Painter.y;
		
		if(!$('#controls').is(':hover')) {
			if(Painter.mode == 'highlighter') {
				
				Painter.fillCircle(context, Painter.x, Painter.y);
				
			    //context.beginPath();
			    //context.arc(Painter.x, Painter.y, Painter.radius, 0, Math.PI*2, false);
			    //context.fillStyle = Painter.fillColor;
			    //context.fill();
			    //context.strokeStyle = Painter.fillColor;
			    //context.stroke();

			}

			else if(Painter.mode == 'eraser') {
			    
				var temp = $(true, Painter.brushColor);
				Painter.brushColor = {r: 200, g: 200, b: 200, a: 255};
				Painter.fillCircle(context, Painter.x, Painter.y);
				Painter.radius -= 2;
				Painter.brushColor = {r: 200, g: 200, b: 200, a: 0};
				Painter.fillCircle(context, Painter.x, Painter.y);
				Painter.radius += 2;
				Painter.brushColor = temp;
				
			    //context.beginPath();
			    //context.arc(Painter.x, Painter.y, Painter.radius, 0, Math.PI*2, false);
			    //context.fillStyle = 'rgba(255,255,255,0)';
			    //context.fill();
			    //context.lineWidth = 2;
			    //context.strokeStyle = '#999';
			    //context.stroke();

			}
		}
		
		Painter.oldX = oldX;
		Painter.oldY = oldY;
	}, 

	drawCrop: function() {
		var context = Painter.crop.ctx;
		context.clearRect(0, 0, Painter.crop.width, Painter.crop.height);


		//Painter.crop.ctx.beginPath();
		Painter.crop.ctx.fillStyle = '#222';
		Painter.crop.ctx.fillRect(0,0, Painter.crop.width, Painter.crop.height);
		Painter.crop.ctx.fill();


		//Drawing and started on top of the square (live resize)
		if(Painter.canvas.isDrawing && Painter.onCrop  && Painter.mode == 'hand') {

			var recW = (Painter.x1 - Painter.x0) + (Painter.x - Painter.x2); 
			var recH = (Painter.y1 - Painter.y0) + (Painter.y - Painter.y2);
		
			Painter.x1n = Painter.x0 + recW; 
			Painter.y1n = Painter.y0 + recH;


			context.beginPath();
		    context.rect(Painter.x0, Painter.y0, recW, recH);
		    context.fill();
		    context.lineWidth = 8;
		    context.strokeStyle = 'rgba(255,255,255,0.5)';
		    context.stroke();


		}

		//crop-mode (reisze crop) and mouse lifted up
		else if(Painter.onCrop && !Painter.canvas.isDrawing && Painter.mode == 'hand') {
		    Painter.x1 = Painter.x1n; 
		    Painter.y1 = Painter.y1n;

    		var recW = Painter.x1 - Painter.x0; 
			var recH = Painter.y1 - Painter.y0;

			context.beginPath();
		    context.rect(Painter.x0, Painter.y0, recW, recH);
		    context.fill();
		    context.lineWidth = 8;
		    context.strokeStyle = 'rgba(255,255,255,0.5)';
		    context.stroke();

		    Painter.onCrop = false;
		} 


			 //if true, clicked down - live drawing
		else if(Painter.canvas.isDrawing  && Painter.mode == 'hand' ) {
			var recW = Painter.x - Painter.x0; 
			var recH = Painter.y - Painter.y0;

		    context.beginPath();
		    context.rect(Painter.x0, Painter.y0, recW, recH);
		    context.fill();
		    context.lineWidth = 8;
		    context.strokeStyle = 'rgba(255,255,255,0.5)';
		    context.stroke();
		}

		//just draw the area!
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


	},

	removePointer: function() {
			// Remove the eraser on mouse up.
			var context = Painter.mousePointer.ctx;
			context.clearRect(0, 0, Painter.mousePointer.width, Painter.mousePointer.height);

			// var temp = Painter.brushColor.a;
			// Painter.radius += 2;
			// Painter.brushColor.a = 0;
			// Painter.fillCircle(context, Painter.oldX, Painter.oldY);
			// Painter.brushColor.a = temp;
			// Painter.radius -= 2;
	}



	
};
