techlooper.controller("technicalDetailController", function (utils, connectionFactory, $routeParams,
                                                             technicalDetailService, $scope, $timeout, jsonValue, termService) {
  var term = $routeParams.term;

  // TODO: write a blog about dom manipulation with angularjs
  $scope.showCircle = function(skill) {
    technicalDetailService.showSkillsList(skill, $scope.termStatistic.totalJob);
    return true;
  }

  $scope.companyUrl = function(company) {//java-fpt+at-it-software-i35-en
    return sprintf("http://vietnamworks.com/%s-%s", term.toLowerCase(), company.companyId);
  }

  connectionFactory.termStatisticInOneYear({term: term})
    .success(function (data, status, headers, config) {
      $scope.termStatistic = termService.toViewTerm(data);
      technicalDetailService.trendSkills($scope.termStatistic);
    })
    .error(function (data, status, headers, config) {
    });
});
