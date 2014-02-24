var dgram = require('dgram');
var fs = require('fs');

var PORT = 55555;

var server = dgram.createSocket('udp4'); 
server.on('message', function (msg, rinfo) {
	console.log('server got: ' + msg + ' from ' + rinfo.address + ':' + rinfo.port); 

	var message = new Buffer("Hello back!"); 
	server.send(message, 0, message.length, 55551, rinfo.address, function(err, bytes) {});
});

server.on('listening', function() {
	var address = server.address(); 
	console.log('Server listening ' + address.address + ':' + address.port);
});


server.bind(PORT);

