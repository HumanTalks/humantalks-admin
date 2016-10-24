declare const $: any;
declare const google: any;
declare const config: any;

class Utils {
    static setSafe(obj: any, path: string | string[], value: any) {
        if(typeof path === 'string')                    { return Utils.setSafe(obj, path.split('.').filter(function(e){ return !!e; }), value); }
        if(!Array.isArray(path) || path.length === 0)   { return obj; }
        if(path.length === 1){
            obj[path[0]] = value;
            return obj;
        } else {
            var newObj = obj[path[0]] || {};
            obj[path[0]] = newObj;
            var newPath = path.slice(1);
            return Utils.setSafe(newObj, newPath, value);
        }
    }
    static formInputs($form) {
        return $form.find('[name]');
    }
    static formModel($form) {
        if($form.length === 0){ console.error('Invalid form element :(', $form); }
        var model = {};
        Utils.formInputs($form).each(function(){
            var value = $(this).attr('type') === 'checkbox' ? $(this).prop('checked') : $(this).val();
            if(value !== ''){
                Utils.setSafe(model, $(this).attr('name').replace('[]', ''), value);
            }
        });
        return model;
    }
    static formLabels($form) {
        if($form.length === 0){ console.error('Invalid form element :(', $form); }
        var labels = {};
        Utils.formInputs($form).each(function(){
            const id = $(this).attr('id');
            labels[id] = $form.find('[for='+id+']').text();
        });
        return labels;
    }
    static cleanForm($form): void {
        Utils.formInputs($form).each(function(){
            $(this).val('').change();
        });
    }
}

// autofocus when a modal opens
(function(){
    $('.modal').on('shown.bs.modal', function(e){
        $(this).find('input[autofocus]').focus();
    });
})();

// confirm actions
(function(){
    $('[confirm]').click( function(e){
        const text = ($(this).attr('title') || 'Confirm')+' ?';
        if(!confirm(text)) {
            e.preventDefault();
        }
    });
})();

// https://select2.github.io/
function buildSelect2CreateModal(modalSelector: string, mainInputName: string, createUrl: string, getLabel: (any) => string){
    return function($select, evt){
        var $modal = $(modalSelector);
        if($modal.length > 0){
            var $form = $modal.find('form');
            openModal($modal, evt.params.data.text);
            $form.off('submit');
            $form.one('submit', function(e){
                e.preventDefault();
                var model = Utils.formModel($form);
                apiCall(model).then(function(created){
                    addToSelect($select, created);
                    closeModal($modal);
                }, function(err){
                    if(err && err.responseJSON && err.responseJSON.error && err.responseJSON.error.description) {
                        alert('Error: '+err.responseJSON.error.description);
                    } else {
                        alert('ERROR '+err.status+' '+err.statusText+' :\n'+JSON.stringify(err.responseJSON));
                    }
                });
            });
        } else {
            alert('Unable to find modal element :('); // forgot to add modal in html ?
        }
    };

    function openModal($modal, text){
        Utils.cleanForm($modal.find('form'));
        $modal.find('input[name='+mainInputName+']').val(text);
        $modal.modal('show');
    }
    function closeModal($modal){
        $modal.modal('hide');
        Utils.cleanForm($modal.find('form'));
    }
    function apiCall(model){
        return $.ajax({
            type: 'POST',
            url: createUrl,
            data: JSON.stringify(model),
            contentType: 'application/json'
        }).then(function(res){
            return res.data;
        });
    }
    function addToSelect(select, model){
        var template = '<option value="'+model.id+'" selected>'+getLabel(model)+'</option>';
        select.append(template);
        select.trigger('change');
    }
}
var createTalkModal = buildSelect2CreateModal('#create-talk-modal', 'title', config.api.root+'/talks', talk => talk.data.title);
var createPersonModal = buildSelect2CreateModal('#create-person-modal', 'name', config.api.root+'/persons', person => person.data.name);
(function(){
    $('.select2').each(function(){
        var $select = $(this);
        $select.select2({
            width: '100%',
            theme: 'bootstrap',
            placeholder: $select.attr('placeholder'),
            allowClear: $select.attr('placeholder') !== undefined
        });
    });

    $('.select2-multi').each(function(){
        var $select = $(this);
        var opts: any = {
            width: '100%',
            theme: 'bootstrap',
            placeholder: $select.attr('placeholder'),
            allowClear: $select.attr('placeholder') !== undefined,
            tags: true,
            tokenSeparators: [',']
        };
        var onCreate: any = window[$select.attr('onCreate')];
        if(typeof onCreate === 'function'){
            opts.templateResult = buildTemplate($select.attr('onCreateLabel'));
            opts.templateSelection = buildTemplate($select.attr('onCreateLabel'));
            $select.on('select2:select', function (evt) {
                if (evt && evt.params && evt.params.data && evt.params.data.id === 'new') {
                    onCreate($select, evt);
                }
            });
        }
        $select.select2(opts);
    });

    function buildTemplate(text){
        return function(item){
            if(item.element){
                return item.text;
            } else {
                item.id = 'new';
                return text;
            }
        };
    }
})();

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

// inputImageUrl
(function(){
    $('.input-imageurl').each(function() {
        var $elt = $(this);
        var $input = $elt.find('input[type="text"]');
        var $preview = $elt.find('.preview');
        update($input, $preview); // run on page load
        $input.on('change', function(){
            update($input, $preview);
        });
    });
    function update($input, $preview){
        if($input.val() === ''){
            $preview.hide();
        } else {
            $preview.attr('src', $input.val());
            $preview.show();
        }
    }
})();

// inputEmbed
(function(){
    $('.input-embed').each(function() {
        var $elt = $(this);
        var $input = $elt.find('input[type="text"]');
        var $preview = $elt.find('.preview');
        update($input, $preview); // run on page load
        $input.on('change', function(){
            update($input, $preview);
        });
    });
    function update($input, $preview){
        if($input.val() === ''){
            $preview.hide();
        } else {
            getEmbedCode($input.val()).then(function(code){
                $preview.html(code);
            });
            $preview.show();
        }
    }
    function getEmbedCode(url: string) {
        return $.get(config.api.tools+'/embed?url='+encodeURIComponent(url)).then(function(res){
            return res.data.embedCode;
        });
    }
})();

// fill form with remotely fetched data
(function(){
    scraper('twitterScraper', $.debounce(250, (account: string) => {
        return $.get(config.api.tools+'/scrapers/twitter/profil?account='+account).then(function(res){
            return res.data;
        });
    }));
    scraper('emailScraper', $.debounce(250, (email: string) => {
        return $.get(config.api.tools+'/scrapers/email/profil?email='+email).then(function(res){
            return res.data;
        });
    }));

    function scraper(attr: string, fetch) {
        $('input['+attr+']').each(function(){
            var $input = $(this);
            update($input, fetch); // run on page load
            $input.on('change', function(){
                update($input, fetch);
            });
        });
    }
    function update($field, getInfos){
        if ($field.val() !== '') {
            getInfos($field.val()).then((data: any) => {
                if(data){
                    attrToField($field, 'fullname', data.name);
                    attrToField($field, 'avatar', data.avatar);
                    attrToField($field, 'bio', data.bio);
                    attrToField($field, 'background', data.backgroundImage);
                    attrToField($field, 'site', data.url);
                    attrToField($field, 'twitter', data.twitter);
                    attrToField($field, 'linkedin', data.linkedin);
                }
            });
        }
    }
    function attrToField($field, attrName: string, value: string){
        var attr = $field.attr(attrName);
        if(attr){
            var $target = $('#'+attr);
            if($target.val() === ''){
                $target.val(value).change();
            }
        }
    }
})();

// Look for duplicates
(function(){
    $('form[duplicates]').each(function(){
        const $form = $(this);
        const duplicatesUrl = $form.attr('duplicates');
        const labels = Utils.formLabels($form);
        Utils.formInputs($form).on('change', $.debounce(250, function(){
            const model = Utils.formModel($form);
            fetchDuplicates(duplicatesUrl, model).then(function(duplicates){
                renderDuplicates($form, duplicates, labels);
            });
        }));
    });
    function fetchDuplicates(url, model) {
        return $.ajax({
            url: url,
            type: 'post',
            data: JSON.stringify(model),
            headers: {
                'Content-Type': 'application/json'
            }
        }).then(function(data){
            return data ? data.data : undefined;
        });
    }
    function renderDuplicates($form, duplicates: any[], labels) {
        $form.find('.duplicates-alert').remove();
        if(duplicates.length > 0) {
            const items = duplicates.map((d: any) => renderName(d) + renderSimilarities(d.similarities, labels));
            $form.prepend(renderAlert(items));
        }
    }
    function renderName(duplicate): string {
        return duplicate.url ? `<a href="${duplicate.url}" target="_blank">${duplicate.name}</a>` : duplicate.name;
    }
    function renderSimilarities(similarities: string[], labels): string {
        if(Array.isArray(similarities) && similarities.length > 0) {
            return  ' (' + similarities.map(s => (labels[s] || s).toLowerCase()).join(', ') + ' similaire'+(similarities.length > 1 ? 's' : '')+')';
        } else {
            return '';
        }
    }
    function renderAlert(items: string[]): string {
        return `<div class="alert alert-warning duplicates-alert" role="alert">
                <strong>Attention!</strong> Merci de vérifier que vous n'êtes pas en train de créer un doublon.<br>
                Entrées similaires :
                <ul>
                    ${items.map(i => '<li>'+i+'</li>').join('')}
                </ul>
            </div>`;
    }
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
