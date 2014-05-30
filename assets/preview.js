/*
 * Preview Gallery
 *
 */
var current_preview = -1;
function preview(no){
    var top = document.getElementById("content").offsetTop;
    var left = document.getElementById("content").offsetLeft;

    current_preview = parseInt(no);
    console.log($("#p"+ no).attr("src"));
    $("#preview_space img").attr("src",$("#p"+no).attr("src"));
    $("#preview_back").css("height",$(document).height() + "px").show();
    $("#preview_space").css("top","" + top + "px").css("left","" + left + "px").show();
}

function next_preview(){
    if($("#p" + (current_preview + 1)).size() > 0) {
        preview(current_preview + 1);
    }else{
        preview(0);
    }
}

function close_preview(){
    $("#preview_space").hide();
    $("#preview_back").hide();
}