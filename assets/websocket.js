/*
 * Android WebSocket Client
 *
 */


var ws = new WebSocket("ws://" + location.host + "/websockets");
var screen;
var cur_id = 0;

ws.onclose = function(event) {
    console.log("websocket is closed");
    $("#connection").text("停止中").css("color", "#ff0000");
};
ws.onopen = function(event) {
    console.log("websocket is opened");
    $("#connection").text("接続中").css("color", "#00a000");
};
ws.onmessage = function(event) {
    console.log("websocket onmessage");
    var datas = event.data.split("|");
    if(datas.length>1){
        if(datas[0] == "preview"){
            console.log("picture caputured");
            cur_id++;
            $("#gallery").prepend('<div class="picture_box"><a href="javascript:preview('+cur_id+')" title="No.'+cur_id+'"><img class="pic" id="p'+cur_id+'" src="data:image/jpeg;base64,'+datas[1]+'" width="' + p_width + '"/></a></div>');
        }else if(datas[0] == "screen"){
            $("#screen").attr("src","data:image/jpeg;base64,"+datas[1]).attr("width", width);
        }
    }
};
function send(msg) {
    ws.send(msg);
    return false;
}
var width=640;
var p_width=140;
function resize(diff){
    if(width + diff < 20){
        return;
    }
    width += diff;
    $("#screen").attr("width",width);
}