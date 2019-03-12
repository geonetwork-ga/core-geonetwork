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
  goog.provide('gn_mdview');




  goog.require('gn_md_feedback');
  goog.require('gn_mdview_directive');
  goog.require('gn_mdview_service');

  var module = angular.module('gn_mdview', [
    'gn_mdview_service',
    'gn_mdview_directive',
    'gn_md_feedback'
  ]);

  module.controller('GnMdViewController', [
    '$scope', '$http', '$compile', 'gnSearchSettings', 'gnSearchLocation',
    'gnMetadataActions', 'gnAlertService', '$translate', '$location',
    'gnMdView', 'gnMdViewObj', 'gnMdFormatter',
    function($scope, $http, $compile, gnSearchSettings, gnSearchLocation,
             gnMetadataActions, gnAlertService, $translate, $location,
             gnMdView, gnMdViewObj, gnMdFormatter) {

      $scope.formatter = gnSearchSettings.formatter;
      $scope.gnMetadataActions = gnMetadataActions;
      $scope.usingFormatter = false;
      $scope.compileScope = $scope.$new();
      $scope.recordIdentifierRequested = gnSearchLocation.getUuid();

      $scope.search = function(params) {
        $location.path('/search');
        $location.search(params);
      };
      $scope.deleteRecord = function(md) {
        return gnMetadataActions.deleteMd(md).then(function(data) {
          gnAlertService.addAlert({
            msg: $translate.instant('metadataRemoved',
                {title: md.title || md.defaultTitle}),
            type: 'success'
          });
          $scope.closeRecord(md);
        }, function(reason) {
          // Data needs improvements
          // See https://github.com/geonetwork/core-geonetwork/issues/723
          gnAlertService.addAlert({
            msg: reason.data,
            type: 'danger'
          });
        });
      };
      $scope.format = function(f) {
        $scope.usingFormatter = f !== undefined;
        $scope.currentFormatter = f;
        if (f) {
          gnMdFormatter.getFormatterUrl(f.url, $scope).then(function(url) {
            $http.get(url, {
              headers: {
                Accept: 'text/html'
              }
            }).then(
                function(response) {
                  var snippet = response.data.replace(
                      '<?xml version="1.0" encoding="UTF-8"?>', '');

                  $('#gn-metadata-display').find('*').remove();

                  $scope.compileScope.$destroy();

                  // Compile against a new scope
                  $scope.compileScope = $scope.$new();
                  var content = $compile(snippet)($scope.compileScope);

                  $('#gn-metadata-display').append(content);
                });
          });
        }
      };
      
      function getType(typeValue, values){
          var arr = [];
          if(!angular.isArray(values)){ values = [values]; }
          angular.forEach(values, function(val){
            arr.push({ "@type":typeValue, "name": val });
          });
          return arr;
        };
        
      // Reset current formatter to open the next record
      // in default mode.
      $scope.$watch('mdView.current.record', function() {
        $scope.usingFormatter = false;
        $scope.currentFormatter = null;
        
        var record = $scope.mdView.current.record;
        $scope.jsonId = {
          "@context": "http:\/\/schema.org",
          "@type": "Dataset",
          "author": getType('Person', record.author),
          "creator": getType('Person', record.author),
          "datePublished": record.publicationDate,
          "keywords": record.keyword,
          "license": [record.legalConstraints,"CC-BY", "http:\/\/creativecommons.org\/licenses\/by\/4.0"],
          "spatialCoverage":[],
          "inLanguage": "en",
          "name": record.title,
          "description":record.abstract
        };
            
      });

      // Know from what path we come from
      $scope.gnMdViewObj = gnMdViewObj;
      $scope.$watch('gnMdViewObj.from', function(v) {
        $scope.fromView = v ? v.substring(1) : v;
      });
    }]).directive('jsonld', ['$filter', '$sce', function($filter, $sce) {
        return {
            restrict: 'E',
            template: function() {
              return '<script type="application/ld+json" ng-bind-html="onGetJson()"></script>';
            },
            scope: {
              json: '=json'
            },
            link: function(scope, element, attrs) {
              scope.onGetJson = function() {
                return $sce.trustAsHtml($filter('json')(scope.json));
              }
            },
            replace: true
          };
        }]);
})();
