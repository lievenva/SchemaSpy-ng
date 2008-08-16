// table-based pages are expected to set 'table' to their name
var table = null;

// sync target's visibility with the state of checkbox
function sync(cb, target) {
  var checked = cb.attr('checked');
  var displayed = target.css('display') != 'none';
  if (checked != displayed) {
    if (checked)
      target.show();
    else
      target.hide();
  }
}

// sync target's visibility with the inverse of the state of checkbox
function unsync(cb, target) {
  var checked = cb.attr('checked');
  var displayed = target.css('display') != 'none';
  if (checked == displayed) {
    if (checked)
      target.hide();
    else
      target.show();
  }
}

// associate the state of checkbox with the visibility of target
function associate(cb, target) {
  sync(cb, target);
  cb.click(function() {
    sync(cb, target);
  });
}

// select the appropriate image based on the options selected
function syncImage() {
  var implied   = $('#implied').attr('checked');
  var img;
  var map;

  if (table) {  
    if (implied) {
      map = '#impliedTwoDegreesRelationshipsGraph';
      img = '../graphs/' + table + '.implied2degrees.png';
    } else {
      var oneDegree = $('#oneDegree').attr('checked');

      if (oneDegree) {
        map = '#oneDegreeRelationshipsGraph';
        img = '../graphs/' + table + '.1degree.png';
      } else {
        map = '#twoDegreesRelationshipsGraph';
        img = '../graphs/' + table + '.2degrees.png';
      }
    }
  } else {
    var showNonKeys = $('#showNonKeys').attr('checked');
    if (implied) {
      if (showNonKeys) {
        map = '#largeImpliedRelationshipsGraph';
        img = 'graphs/summary/relationships.implied.large.png'
      } else {
        map = '#compactImpliedRelationshipsGraph';
        img = 'graphs/summary/relationships.implied.compact.png'
      }
    } else {
      if (showNonKeys) {
        map = '#largeRelationshipsGraph';
        img = 'graphs/summary/relationships.real.large.png'
      } else {
        map = '#compactRelationshipsGraph';
        img = 'graphs/summary/relationships.real.compact.png'
      }
    }
  }

  $('#relationships').attr('useMap', map);
  $('#relationships').attr('src', img);
}

// our 'ready' handler makes the page consistent
$(function(){
  associate($('#implied'),         $('.impliedRelationship'));
  associate($('#showComments'),    $('.comment'));
  associate($('#showLegend'),      $('.legend'));
  associate($('#showRelatedCols'), $('.relatedKey'));
  associate($('#showConstNames'),  $('.constraint'));
  
  syncImage();
  $('#implied,#oneDegree,#twoDegrees,#showNonKeys').click(function() {
    syncImage();
  });

  unsync($('#implied'), $('.degrees'));
  $('#implied').click(function() {
    unsync($('#implied'), $('.degrees'));
  });
  unsync($('#removeImpliedOrphans'), $('.impliedNotOrphan'));
  $('#removeImpliedOrphans').click(function() {
    unsync($('#removeImpliedOrphans'), $('.impliedNotOrphan'));
  });
});
