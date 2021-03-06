/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

(function() {

  goog.provide('gn_search_default');




  goog.require('cookie_warning');
  goog.require('gn_mdactions_directive');
  goog.require('gn_related_directive');
  goog.require('gn_search');
  goog.require('gn_search_default_config');
  goog.require('gn_search_default_directive');

  var module = angular.module('gn_search_default',
      ['gn_search', 'gn_search_default_config',
       'gn_search_default_directive', 'gn_related_directive',
       'cookie_warning', 'gn_mdactions_directive']);


  module.controller('gnsSearchPopularController', [
    '$scope', 'gnSearchSettings',
    function($scope, gnSearchSettings) {
      $scope.searchObj = {
        permalink: false,
        params: {
          sortBy: 'popularity',
          from: 1,
          to: 9
        }
      };
    }]);


  module.controller('gnsSearchLatestController', [
    '$scope',
    function($scope) {
      $scope.searchObj = {
        permalink: false,
        params: {
          sortBy: 'changeDate',
          from: 1,
          to: 9
        }
      };
    }]);
  module.controller('gnsDefault', [
    '$scope',
    '$location',
    'suggestService',
    '$http',
    '$timeout',
    '$translate',
    'gnUtilityService',
    'gnSearchSettings',
    'gnViewerSettings',
    'gnMap',
    'gnMdView',
    'gnMdViewObj',
    'gnWmsQueue',
    'gnSearchLocation',
    'gnOwsContextService',
    'hotkeys',
    'gnGlobalSettings',
    function($scope, $location, suggestService, $http, $timeout, $translate,
             gnUtilityService, gnSearchSettings, gnViewerSettings,
             gnMap, gnMdView, mdView, gnWmsQueue,
             gnSearchLocation, gnOwsContextService,
             hotkeys, gnGlobalSettings) {

      var viewerMap = gnSearchSettings.viewerMap;
      var searchMap = gnSearchSettings.searchMap;
      var recordMap = gnSearchSettings.recordMap;

      $scope.isArray = angular.isArray;
      $scope.modelOptions = angular.copy(gnGlobalSettings.modelOptions);
      $scope.modelOptionsForm = angular.copy(gnGlobalSettings.modelOptions);
      $scope.gnWmsQueue = gnWmsQueue;
      $scope.$location = $location;
      $scope.activeTab = '/home';
      $scope.resultTemplate = gnSearchSettings.resultTemplate;
      $scope.viewTemplates = gnSearchSettings.resultViewTpls;
      $scope.facetsSummaryType = gnSearchSettings.facetsSummaryType;
      $scope.location = gnSearchLocation;
      $scope.states = {};
      $scope.visible = true;
      $scope.states.activeItem = $scope.viewTemplates[0].tooltip;
      $scope.toggleMap = function () {
        $(searchMap.getTargetElement()).toggle();
        $('button.gn-minimap-toggle > i').toggleClass('fa-angle-double-left fa-angle-double-right');
      };

      $scope.changeView = function (index) {
        $scope.states.activeItem = $scope.viewTemplates[index].tooltip;
        $scope.resultTemplate = $scope.viewTemplates[index].tplUrl;        
      };

      hotkeys.bindTo($scope)
        .add({
            combo: 'h',
            description: $translate.instant('hotkeyHome'),
            callback: function(event) {
              $location.path('/search');
            }
          }).add({
            combo: 't',
            description: $translate.instant('hotkeyFocusToSearch'),
            callback: function(event) {
              event.preventDefault();
              var anyField = $('#gn-any-field');
              if (anyField) {
                gnUtilityService.scrollTo();
                $location.path('/search');
                anyField.focus();
              }
            }
          }).add({
            combo: 'enter',
            description: $translate.instant('hotkeySearchTheCatalog'),
            allowIn: 'INPUT',
            callback: function() {
              $location.search('tab=search');
            }
            //}).add({
            //  combo: 'r',
            //  description: $translate.instant('hotkeyResetSearch'),
            //  allowIn: 'INPUT',
            //  callback: function () {
            //    $scope.resetSearch();
            //  }
          }).add({
            combo: 'm',
            description: $translate.instant('hotkeyMap'),
            callback: function(event) {
              $location.path('/map');
            }
          });


      // TODO: Previous record should be stored on the client side
      $scope.mdView = mdView;
      gnMdView.initMdView();

      $scope.getAsArray = function(values){
        if(angular.isArray(values)){
          return values;
        }else{
          return [values];          
        }
      }

      $scope.isHprmLink = function(association){
        var link = association.split('~')[3];
        if(link && link.startsWith('http://rmweb/HPEContentManager/')){
          return true;
        }
        return false;
      }

      $scope.isAvailable = function(item, item1){
        if(item || item1){
          return true;
        }
    
        return false;
      }

      $scope.toggleAndTriggerSearch = function(){
       var adv_opened = $('#adv-1').hasClass('in');
        if(adv_opened === true){
          $('#adv-1').collapse('toggle');
        }
        $scope.$broadcast('search');        
      }
      $scope.displayKeyword = function(thesaurus, keywords){
        if(thesaurus.toLowerCase() === 'other' && keywords.length <= 1){
          return false;
        }
        return true;
      }
      $scope.getKeywordTitle = function(title){
        if(title.toLowerCase().indexOf('theme.anzrc') >= 0){
          title = 'ANZRC Fields Of Research';
        }       
        return title;
      }
     
      $scope.getBoundingBox = function(bbox) {
        var bboxStr = '';
        if(bbox){
          var vals = bbox[0].split(',');
          if(vals[0]){
            bboxStr += parseFloat(vals[0].split(' ')[0]).toFixed(2) + ', ' + parseFloat(vals[0].split(' ')[1]).toFixed(2) + ', ';
          }
          if(vals[2]){
            bboxStr += parseFloat(vals[2].split(' ')[0]).toFixed(2) + ', ' + parseFloat(vals[2].split(' ')[1]).toFixed(2);
          }
        }

        return bboxStr;
      }
      $scope.getCitation = function(md){
        if(md){
          var citationUrl = '';
          if(angular.isArray(md.author) && md.author.length > 0){
            citationUrl = md.author.join(', ') + ' ';
          }else{
            if(md.author){
              citationUrl = md.author + ' ';
            }
          }
  
          if(md.publicationDate){
            var date = new Date(md.publicationDate);
            citationUrl += date.getFullYear() + '. ';
          }
          
          citationUrl += md.title + '. ';
  
          if(md.issueIdentification){
            var rec = md.issueIdentification.toLowerCase().includes('rec') ? '' : 'Record ';
            citationUrl = citationUrl + rec + md.issueIdentification + '. ';
          }
  
          citationUrl += 'Geoscience Australia, Canberra. ';
          
          if(md.DOI){
            citationUrl += md.DOI;
          }else if(md.PID){
            citationUrl += md.PID;
          }else{
            var scope = md.type === 'service' ? 'service' : 'dataset';
            citationUrl += "http://pid.geoscience.gov.au/" + scope + "/ga/" + md.eCatId;
          }
          
          return citationUrl;
        }
        
      }

      $scope.goToSearch = function (any) {
        $location.path('/search').search({'any': any});
      };
      $scope.canEdit = function(record) {
        // TODO: take catalog config for harvested records
        if (record && record['geonet:info'] &&
            record['geonet:info'].edit == 'true') {
          return true;
        }
        return false;
      };
      $scope.openRecord = function(index, md, records) {
        $timeout(function() {
          var myEl = angular.element(document.querySelector('#download-tab a'));
          myEl.triggerHandler('click');
        });
        gnMdView.feedMd(index, md, records);
      };

      $scope.closeRecord = function() {
        gnMdView.removeLocationUuid();
      };
      $scope.nextRecord = function() {
        var nextRecordId = mdView.current.index + 1;
        if (nextRecordId === mdView.records.length) {
          // When last record of page reached, go to next page...
          // Not the most elegant way to do it, but it will
          // be easier using Solr search components
          $scope.$broadcast('nextPage');
        } else {
          $scope.openRecord(nextRecordId);
        }
      };
      $scope.previousRecord = function() {
        var prevRecordId = mdView.current.index - 1;
        if (prevRecordId === -1) {
          $scope.$broadcast('previousPage');
        } else {
          $scope.openRecord(prevRecordId);
        }
      };

      $scope.reloadMap = function(){
        recordMap.updateSize();
      }
      $scope.infoTabs = {
        lastRecords: {
          title: 'lastRecords',
          titleInfo: '',
          active: true
        },
        preferredRecords: {
          title: 'preferredRecords',
          titleInfo: '',
          active: false
        }};

      // Set the default browse mode for the home page
      $scope.$watch('searchInfo', function (n, o) {
        if (angular.isDefined($scope.searchInfo.facet)) {
          if ($scope.searchInfo.facet['inspireThemes'].length > 0) {
            $scope.browse = 'inspire';
          } else if ($scope.searchInfo.facet['topicCats'].length > 0) {
            $scope.browse = 'topics';
          //} else if ($scope.searchInfo.facet['categories'].length > 0) {
          //  $scope.browse = 'cat';
          }
        }
      });

      $scope.$on('layerAddedFromContext', function(e,l) {
        var md = l.get('md');
        if(md) {
          var linkGroup = md.getLinkGroup(l);
          gnMap.feedLayerWithRelated(l,linkGroup);
        }
      });

      $scope.resultviewFns = {
        addMdLayerToMap: function (link, md) {

          if (gnMap.isLayerInMap(viewerMap,
              link.name, link.url)) {
            return;
          }
          gnMap.addWmsFromScratch(viewerMap, link.url, link.name, false, md).
          then(function(layer) {
            if(layer) {
              gnMap.feedLayerWithRelated(layer, link.group);
            }
          });
      },
        addAllMdLayersToMap: function (layers, md) {
          angular.forEach(layers, function (layer) {
            $scope.resultviewFns.addMdLayerToMap(layer, md);
          });
        },
        loadMap: function (map, md) {
          gnOwsContextService.loadContextFromUrl(map.url, viewerMap);
        }
      };

      // Manage route at start and on $location change
      if (!$location.path()) {
        $location.path('/home');
      }
      $scope.activeTab = $location.path().
          match(/^(\/[a-zA-Z0-9]*)($|\/.*)/)[1];

      $scope.$on('$locationChangeSuccess', function(next, current) {
        $scope.activeTab = $location.path().
            match(/^(\/[a-zA-Z0-9]*)($|\/.*)/)[1];

        if (gnSearchLocation.isSearch() && (!angular.isArray(
            searchMap.getSize()) || searchMap.getSize()[0] < 0)) {
          setTimeout(function() {
            searchMap.updateSize();

            // TODO: load custom context to the search map
            //gnOwsContextService.loadContextFromUrl(
            //  gnViewerSettings.defaultContext,
            //  searchMap);

          }, 0);
        }
        if (gnSearchLocation.isMap() && (!angular.isArray(
            viewerMap.getSize()) || viewerMap.getSize().indexOf(0) >= 0)) {
          setTimeout(function() {
            viewerMap.updateSize();
          }, 0);
        }

      });

      angular.extend($scope.searchObj, {
        advancedMode: false,
        from: 1,
        to: 30,
        selectionBucket: 's101',
        viewerMap: viewerMap,
        searchMap: searchMap,
        recordMap: recordMap,
        mapfieldOption: {
          relations: ['within']
        },
        defaultParams: {
          'facet.q': '',
          resultType: gnSearchSettings.facetsSummaryType || 'details'
        },
        params: {
          'facet.q': '',
          resultType: gnSearchSettings.facetsSummaryType || 'details'
        }
      }, gnSearchSettings.sortbyDefault);

    }])
    .filter('join', function () {
      return function join(array, separator, prop) {
          if (!Array.isArray(array)) {
        return array; // if not array return original - can also throw error
          }
  
          return (!!prop ? array.map(function (item) {
              return item[prop];
          }) : array).join(separator);
      };
  })
  .filter('replace', [function () {
    return function (input, from, to) {
      if(input === undefined) {
        return;
      }
      var regex = new RegExp(from, 'g');
      return input.replace(regex, to);
    };
  }]);
})();
