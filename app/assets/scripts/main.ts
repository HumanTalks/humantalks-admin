declare const $: any;
declare const google: any;

// http://www.malot.fr/bootstrap-datetimepicker/
(function(){
    $('input.input-datetime').each(function(){
        $(this).datetimepicker({
            language: 'fr',
            autoclose: true,
            initialDate: $(this).attr('startDate')
        });
    });
})();

// GMapPlace picker (https://developers.google.com/maps/documentation/javascript/examples/places-autocomplete?hl=fr)
var GMapPlacePicker = (function(){
    return {
        init: function(){
            $('.input-gmapplace').each(function() {
                var $elt = $(this);
                var $input = $elt.find('input[type="text"]');
                var mapData = initMap($elt);
                updateField($elt, mapData, readForm($elt)); // run on page load
                var autocomplete = new google.maps.places.Autocomplete($input.get(0));
                autocomplete.addListener('place_changed', function() {
                    var place = autocomplete.getPlace(); // cf https://developers.google.com/maps/documentation/javascript/3.exp/reference?hl=fr#PlaceResult
                    updateField($elt, mapData, toLocation(place));
                });
                $input.on('change', function(){
                    if($input.val() === ''){
                        updateField($elt, mapData, null);
                    }
                });
            });
        }
    };
    function initMap($elt){
        var $map = $elt.find('.map');
        var map = new google.maps.Map($map.get(0), {
            center: {lat: -33.8688, lng: 151.2195},
            zoom: 13
        });
        var marker = new google.maps.Marker({
            map: map,
            anchorPoint: new google.maps.Point(0, -29)
        });
        var infowindow = new google.maps.InfoWindow();
        return {
            $map: $map,
            map: map,
            marker: marker,
            infowindow: infowindow
        };
    }
    function updateField($elt, mapData, location){
        writeForm($elt, location);
        if(location && location.geo && location.geo.lat){
            showMap(mapData, location);
        } else {
            hideMap(mapData);
        }
    }
    function showMap(mapData, formattedPlace){
        mapData.$map.show();
        google.maps.event.trigger(mapData.map, 'resize');
        mapData.infowindow.close();
        mapData.marker.setVisible(false);
        mapData.map.setCenter(formattedPlace.geo);
        mapData.map.setZoom(15);
        mapData.marker.setPosition(formattedPlace.geo);
        mapData.marker.setVisible(true);
        mapData.infowindow.setContent(
            '<strong>'+formattedPlace.name+'</strong><br>'+
            formattedPlace.streetNo+' '+formattedPlace.street+'<br>'+
            formattedPlace.postalCode+' '+formattedPlace.locality+', '+formattedPlace.country
        );
        mapData.infowindow.open(mapData.map, mapData.marker);
    }
    function hideMap(mapData){
        mapData.$map.hide();
    }
    function writeForm($elt, formattedPlace){
        $elt.find('input[type="hidden"].gmapplace-id').val(formattedPlace ? formattedPlace.id : '');
        $elt.find('input[type="hidden"].gmapplace-name').val(formattedPlace ? formattedPlace.name : '');
        $elt.find('input[type="hidden"].gmapplace-streetNo').val(formattedPlace ? formattedPlace.streetNo : '');
        $elt.find('input[type="hidden"].gmapplace-street').val(formattedPlace ? formattedPlace.street : '');
        $elt.find('input[type="hidden"].gmapplace-postalCode').val(formattedPlace ? formattedPlace.postalCode : '');
        $elt.find('input[type="hidden"].gmapplace-locality').val(formattedPlace ? formattedPlace.locality : '');
        $elt.find('input[type="hidden"].gmapplace-country').val(formattedPlace ? formattedPlace.country : '');
        $elt.find('input[type="hidden"].gmapplace-formatted').val(formattedPlace ? formattedPlace.formatted : '');
        $elt.find('input[type="hidden"].gmapplace-lat').val(formattedPlace ? formattedPlace.geo.lat : '');
        $elt.find('input[type="hidden"].gmapplace-lng').val(formattedPlace ? formattedPlace.geo.lng : '');
        $elt.find('input[type="hidden"].gmapplace-url').val(formattedPlace ? formattedPlace.url : '');
        $elt.find('input[type="hidden"].gmapplace-website').val(formattedPlace ? formattedPlace.website : '');
        $elt.find('input[type="hidden"].gmapplace-phone').val(formattedPlace ? formattedPlace.phone : '');
    }
    function readForm($elt){
        return {
            id: $elt.find('input[type="hidden"].gmapplace-id').val(),
            name: $elt.find('input[type="hidden"].gmapplace-name').val(),
            streetNo: $elt.find('input[type="hidden"].gmapplace-streetNo').val(),
            street: $elt.find('input[type="hidden"].gmapplace-street').val(),
            postalCode: $elt.find('input[type="hidden"].gmapplace-postalCode').val(),
            locality: $elt.find('input[type="hidden"].gmapplace-locality').val(),
            country: $elt.find('input[type="hidden"].gmapplace-country').val(),
            formatted: $elt.find('input[type="hidden"].gmapplace-formatted').val(),
            geo: {
                lat: parseFloat($elt.find('input[type="hidden"].gmapplace-lat').val()),
                lng: parseFloat($elt.find('input[type="hidden"].gmapplace-lng').val())
            },
            url: $elt.find('input[type="hidden"].gmapplace-url').val(),
            website: $elt.find('input[type="hidden"].gmapplace-website').val(),
            phone: $elt.find('input[type="hidden"].gmapplace-phone').val()
        };
    }
    function toLocation(place){
        function of(elt, field){ return elt && elt[field] ? elt[field] : ''; }
        function formatAddressComponents(components){
            function findByType(components, type){
                var c = components.find(function(e: any){ return e.types.indexOf(type) >= 0; });
                return c ? c.long_name : undefined;
            }
            return {
                street_number: findByType(components, "street_number"), // ex: "119"
                route: findByType(components, "route"), // ex: "Boulevard Voltaire"
                postal_code: findByType(components, "postal_code"), // ex: "75011"
                locality: findByType(components, "locality"), // ex: "Paris"
                country: findByType(components, "country"), // ex: "France"
                administrative_area: {
                    level_1: findByType(components, "administrative_area_level_1"), // ex: "Île-de-France"
                    level_2: findByType(components, "administrative_area_level_2"), // ex: "Paris"
                    level_3: findByType(components, "administrative_area_level_3"),
                    level_4: findByType(components, "administrative_area_level_4"),
                    level_5: findByType(components, "administrative_area_level_5")
                },
                sublocality: {
                    level_1: findByType(components, "sublocality_level_1"),
                    level_2: findByType(components, "sublocality_level_2"),
                    level_3: findByType(components, "sublocality_level_3"),
                    level_4: findByType(components, "sublocality_level_4"),
                    level_5: findByType(components, "sublocality_level_5")
                }
            };
        }
        var components = formatAddressComponents(place.address_components);
        var loc = place && place.geometry ? place.geometry.location : undefined;
        return {
            id: of(place, 'place_id'),
            name: of(place, 'name'),
            streetNo: of(components, 'street_number'),
            street: of(components, 'route'),
            postalCode: of(components, 'postal_code'),
            locality: of(components, 'locality'),
            country: of(components, 'country'),
            formatted: of(place, 'formatted_address'),
            geo: {
                lat: loc ? loc.lat() : '',
                lng: loc ? loc.lng() : ''
            },
            url: of(place, 'url'),
            website: of(place, 'website'),
            phone: of(place, 'international_phone_number')
        };
    }
})();

function googleMapsInit(){
    GMapPlacePicker.init();
}