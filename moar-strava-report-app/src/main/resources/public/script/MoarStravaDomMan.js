if (!location.href.startsWith("http://localhost")) {
  if (location.protocol == "http:") {
    location.href = "https:" + window.location.href.substring(window.location.protocol.length);
  }
}

window.onload = function() {
  var moarStravaData = {};
  var moarDigest = "";
  var loadedCharts = false;
  var user = null;
  var connectEl = document.getElementById("connect");
  var loadingEl = document.getElementById("loading");
  var primaryEl = document.getElementById("primary");
  var gearsEl = document.getElementById("gears");
  var statusEl = document.getElementById("status");
  var divChartYearMap = {};

  function error(code) {
    location.href = "?error=" + code;
  }

  function getDivChartEl(year) {
    var yearKey = "y" + year;
    var el = divChartYearMap[yearKey];
    if(el) {
      return el;
    } else {
      var yearDiv = document.createElement("div");
      primaryEl.appendChild(yearDiv);
      var yearDivChart = document.createElement("div");
      yearDiv.className = "year";
      yearDiv.appendChild(yearDivChart);
      divChartYearMap[yearKey] = yearDivChart;
      return yearDivChart;
    }
  }

  function drawCharts() {
    var scrollTop = document.documentElement.scrollTop;

    var moar = moarStravaData;
    if(!moar.running) {
      gearsEl.style.display = "none";
      statusEl.style.display = "none";
    } else {
      statusEl.style.display = "block";
      gearsEl.style.display = "block";
    }

    var years = moar.years;
    if(!years) {
      error("drawCharts")
      return;
    }
    for(var y = 0; y < years.length; y++) {
      var year = years[y];
      var detail = year[1];
      var yearData = year[2];
      year = year[0];
      var yearDivChart = getDivChartEl(year);
      drawChart(year, detail, yearData, yearDivChart);
    }

    document.documentElement.scrollTop = scrollTop;
  }

  function drawChart(year, detail, yearData, element) {
    var yearLabel = year == -1 ? "All Time" : "" + year;
    var milesLabel = detail ? "Miles" : "Miles ~";
    var dataArray = [ [ yearLabel, milesLabel ] ];
    for (var i = 0; i < yearData.length; i++) {
      var item = yearData[i];
      if(item.miles > 0) {
        dataArray.push([ item.displayName, item.miles ]);
      }
    }

    var data = google.visualization.arrayToDataTable(dataArray);
    var options = {
      sortColumn: 1,
      sortAscending: false
    };
    var chart = new google.visualization.Table(element);
    chart.draw(data, options);
  }

  function clearPrimaryEl() {
    while (primaryEl.firstChild) {
      primaryEl.removeChild(primaryEl.firstChild);
    }
  }

  function getResults() {
    var Http = new XMLHttpRequest();
    var url = "/results/" + user;
    Http.open("GET", url);
    Http.send();
    Http.onerror = function() {
      error("http");
    }
    Http.onreadystatechange = function() {
      if (this.readyState === 4 && this.status === 200) {
        loadingEl.style.display = "none";
        moarStravaData = JSON.parse(Http.responseText);
        statusEl.innerHTML = moarStravaData.status;
        if(moarStravaData.running) {
          window.setTimeout(function() {
            getResults();
          }, 15000);
        }
        if(loadedCharts) {
          drawCharts();
        } else {
          loadedCharts = true;
          google.charts.load("current", {
            "packages" : [ "corechart", "table" ]
          });
          google.charts.setOnLoadCallback(drawCharts);
        }
      }
    }
  }

  var url = location.href;
  var hashKey = "#user=";
  var hashIdx = url.indexOf(hashKey);
  if (hashIdx != -1) {
    clearPrimaryEl();
    gearsEl.style.diplay = "block";
    user = url.substring(hashIdx + hashKey.length);
    location.hash = "";
    getResults()
  } else {
    loadingEl.style.display = "none";
    connectEl.style.display = "block";
  }
  primaryEl.style.display = "block";
}
