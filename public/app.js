(function() {
  'use strict';

  var globals = typeof window === 'undefined' ? global : window;
  if (typeof globals.require === 'function') return;

  var modules = {};
  var cache = {};
  var aliases = {};
  var has = ({}).hasOwnProperty;

  var unalias = function(alias, loaderPath) {
    var result = aliases[alias] || aliases[alias + '/index.js'];
    return result || alias;
  };

  var _reg = /^\.\.?(\/|$)/;
  var expand = function(root, name) {
    var results = [], part;
    var parts = (_reg.test(name) ? root + '/' + name : name).split('/');
    for (var i = 0, length = parts.length; i < length; i++) {
      part = parts[i];
      if (part === '..') {
        results.pop();
      } else if (part !== '.' && part !== '') {
        results.push(part);
      }
    }
    return results.join('/');
  };

  var dirname = function(path) {
    return path.split('/').slice(0, -1).join('/');
  };

  var localRequire = function(path) {
    return function expanded(name) {
      var absolute = expand(dirname(path), name);
      return globals.require(absolute, path);
    };
  };

  var initModule = function(name, definition) {
    var module = {id: name, exports: {}};
    cache[name] = module;
    definition(module.exports, localRequire(name), module);
    return module.exports;
  };

  var require = function(name, loaderPath) {
    if (loaderPath == null) loaderPath = '/';
    var path = unalias(name, loaderPath);

    if (has.call(cache, path)) return cache[path].exports;
    if (has.call(modules, path)) return initModule(path, modules[path]);

    var dirIndex = expand(path, './index');
    if (has.call(cache, dirIndex)) return cache[dirIndex].exports;
    if (has.call(modules, dirIndex)) return initModule(dirIndex, modules[dirIndex]);

    throw new Error('Cannot find module "' + name + '" from ' + '"' + loaderPath + '"');
  };

  require.alias = function(from, to) {
    aliases[to] = from;
  };

  require.register = require.define = function(bundle, fn) {
    if (typeof bundle === 'object') {
      for (var key in bundle) {
        if (has.call(bundle, key)) {
          require.register(key, bundle[key]);
        }
      }
    } else {
      modules[bundle] = fn;
    }
  };

  require.list = function() {
    var result = [];
    for (var item in modules) {
      if (has.call(modules, item)) {
        result.push(item);
      }
    }
    return result;
  };

  require.brunch = true;
  require._cache = cache;
  globals.require = require;
})();
require.register("javascripts/app", function(exports, require, module) {
'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
var Search = {
  init: function init() {
    moment.locale('fr');
    this.search = instantsearch({
      appId: 'O3F8QXYK6R',
      apiKey: '36caf26b37562229d205f0eeceeac37f',
      indexName: 'humantalks',
      urlSync: true
    });

    this.showMoreTemplates = {
      inactive: '<a class="db tr purple mr2 mt1 pointer">Voir plus »</a>',
      active: '<a class="db tr purple mr2 mt1 pointer">« Voir moins</a>'
    };

    this.websiteUrl = 'https://pixelastic.github.io/humantalks/';

    this.addSearchBoxWidget();
    this.addStatsWidget();
    this.addSpeakersWidget();
    this.addLocationsWidget();
    this.addHitsWidget();
    this.addPaginationWidget();

    this.search.start();
  },
  cloudinary: function cloudinary(url, options) {
    if (!url) {
      return url;
    }
    var baseUrl = 'https://res.cloudinary.com/pixelastic-humantalks/image/fetch/';
    var stringOptions = [];

    // Handle common Cloudinary options
    if (options.width) {
      stringOptions.push('w_' + options.width);
    }
    if (options.height) {
      stringOptions.push('h_' + options.height);
    }
    if (options.quality) {
      stringOptions.push('q_' + options.quality);
    }
    if (options.crop) {
      stringOptions.push('c_' + options.crop);
    }
    if (options.radius) {
      stringOptions.push('r_' + options.radius);
    }
    if (options.format) {
      stringOptions.push('f_' + options.format);
    }
    if (options.colorize) {
      stringOptions.push('e_colorize:' + options.colorize);
    }
    if (options.grayscale) {
      stringOptions.push('e_grayscale');
    }
    if (options.color) {
      stringOptions.push('co_rgb:' + options.color);
    }
    if (options.gravity) {
      stringOptions.push('g_' + options.gravity);
    }

    // Fix remote urls
    url = url.replace(/^\/\//, 'http://');

    return '' + baseUrl + stringOptions.join(',') + '/' + url;
  },
  transformItem: function transformItem(data) {
    // <!--Todo:-->
    // <!--Lien sur le logo de clear all-->
    // <!--RWD petit écran un résultat par ligne-->

    // Various urls
    var videoUrl = data.video;
    var slidesUrl = data.slides;
    var meetupUrl = data.meetup;
    var hasVideo = !!videoUrl;
    var hasSlides = !!slidesUrl;

    // Title
    var title = Search.getHighlightedValue(data, 'title');
    // Default link goes to video, fallback on slides or meetup page
    var titleLink = videoUrl;
    if (!titleLink) {
      titleLink = slidesUrl;
    }
    if (!titleLink) {
      titleLink = meetupUrl;
    }

    // Description
    var description = data._snippetResult.description.value;
    description = description.replace(' …', '…');

    // Thumbnail
    var thumbnail = data.thumbnail;
    if (!thumbnail) {
      thumbnail = Search.websiteUrl + '/img/default.png';
    }
    var thumbnailLink = meetupUrl;
    if (hasSlides) {
      thumbnailLink = slidesUrl;
    } else if (hasVideo) {
      thumbnailLink = videoUrl;
    }

    // Authors
    var authors = _.map(data.authors, function (author, index) {
      if (!author.picture) {
        author.picture = Search.websiteUrl + '/img/default-speaker.png';
      }
      var picture = Search.cloudinary(author.picture, {
        height: 50,
        width: 50,
        quality: 90,
        crop: 'scale',
        radius: 'max',
        format: 'auto'
      });

      var link = '#';
      if (author.twitter) {
        link = 'https://twitter.com/' + author.twitter;
      }
      return {
        plainName: author.name,
        highlightedName: data._highlightResult.authors[index].name.value,
        link: link,
        picture: picture
      };
    });

    // Date
    var readableDate = _.capitalize(moment(data.date, 'YYYY-MM-DD').format('DD MMMM YYYY'));

    // Location
    var locationName = data.location;
    var locationLogo = '' + Search.websiteUrl + data.location_logo;
    var readableLocation = locationName;
    if (locationLogo) {
      var logoPicture = Search.cloudinary(locationLogo, {
        height: 20,
        quality: 90,
        crop: 'scale',
        format: 'auto'
      });
      readableLocation = '<img class="v-textbottom" src="' + logoPicture + '" alt="' + locationName + '" />';
    }

    var displayedData = {
      title: title,
      titleLink: titleLink,
      description: description,
      thumbnail: thumbnail,
      thumbnailLink: thumbnailLink,
      hasVideo: hasVideo,
      videoUrl: videoUrl,
      hasSlides: hasSlides,
      slidesUrl: slidesUrl,
      meetupUrl: meetupUrl,
      authors: authors,
      readableDate: readableDate,
      readableLocation: readableLocation
    };

    return displayedData;
  },
  getHighlightedValue: function getHighlightedValue(object, property) {
    if (!_.has(object, '_highlightResult.' + property + '.value')) {
      return object[property];
    }
    return object._highlightResult[property].value;
  },
  addSearchBoxWidget: function addSearchBoxWidget() {
    this.search.addWidget(instantsearch.widgets.searchBox({
      container: '#js-searchbar',
      wrapInput: false,
      placeholder: 'Rechercher un thème, un speaker, un lieu'
    }));
  },
  addStatsWidget: function addStatsWidget() {
    this.search.addWidget(instantsearch.widgets.stats({
      container: '#js-stats',
      templates: {
        body: function body(options) {
          return options.nbHits + ' r\xE9sultats trouv\xE9s en ' + options.processingTimeMS + 'ms';
        }
      }
    }));
  },
  addSpeakersWidget: function addSpeakersWidget() {
    this.search.addWidget(instantsearch.widgets.refinementList({
      container: '#js-speakers',
      attributeName: 'authors.name',
      operator: 'or',
      sortBy: ['isRefined', 'count:desc', 'name:asc'],
      cssClasses: {
        root: '',
        item: '',
        label: 'db relative pointer pa1 hover-purple',
        count: 'absolute right-0 top-0 mr1 br-pill bg-black-20 purple pa1 f6',
        active: 'b purple',
        checkbox: 'dn'
      },
      templates: {
        'header': '<h3 class="title f2 no-b ma0 purple">Speakers</h3>'
      },
      limit: 10,
      showMore: {
        limit: 20,
        templates: Search.showMoreTemplates
      }
    }));
  },
  addLocationsWidget: function addLocationsWidget() {
    this.search.addWidget(instantsearch.widgets.refinementList({
      container: '#js-locations',
      attributeName: 'location',
      operator: 'or',
      sortBy: ['isRefined', 'count:desc', 'name:asc'],
      cssClasses: {
        root: '',
        item: '',
        label: 'db relative pointer pa1 hover-purple',
        count: 'absolute right-0 top-0 mr1 br-pill bg-black-20 purple pa1 f6',
        active: 'b purple',
        checkbox: 'dn'
      },
      templates: {
        'header': '<h3 class="title f2 no-b ma0 purple">Lieux</h3>'
      },
      limit: 10,
      showMore: {
        limit: 20,
        templates: Search.showMoreTemplates
      }
    }));
  },
  addHitsWidget: function addHitsWidget() {
    var hitTemplate = $('#js-template-hits').html();
    var noResults = $('#js-template-noresults').html();
    this.search.addWidget(instantsearch.widgets.hits({
      container: '#js-hits',
      hitsPerPage: 20,
      cssClasses: {
        root: 'flex-row-wrap mb3',
        item: 'flex-auto flex w-100'
      },
      templates: {
        item: hitTemplate,
        empty: noResults
      },
      transformData: {
        item: Search.transformItem
      }
    }));

    // // Allow user to further select/deselect facets directly in the hits
    // let hitContainer = $('#hits');
    // hitContainer.on('click', '.js-facet-toggle', (event) => {
    //   var target = $(event.currentTarget);
    //   var facetName = target.data('facet-name');
    //   var facetValue = target.data('facet-value');
    //   Search.search.helper.toggleRefinement(facetName, facetValue).search();
    //   target.toggleClass('hit-facet__isRefined');
    // });
  },
  addPaginationWidget: function addPaginationWidget() {
    this.search.addWidget(instantsearch.widgets.pagination({
      container: '#js-pagination',
      cssClasses: {
        root: 'flex-row-nowrap justify-center',
        item: 'flex-none flex mh1 bg-purple pa0 shadow-4',
        link: 'white db pa2 link hover-bg-blue',
        active: 'underline b'
      },
      labels: {
        previous: '‹ Précédent',
        next: 'Suivant ›'
      },
      showFirstLast: false
    }));
  }
};

exports.default = Search;

});

