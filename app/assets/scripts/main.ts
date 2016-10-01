declare const $: any;

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
