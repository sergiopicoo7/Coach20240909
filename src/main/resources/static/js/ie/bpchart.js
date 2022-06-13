function buildChart() {
    $('#loadingChart').addClass('hidden');
    let elNoData = $('#noChartData');
    let ctx = $('#chart');

    if (window.bpchart.data.length === 0) {
        $(elNoData).removeClass('hidden');
        $(ctx).addClass('hidden');
        return;
    }

    $(elNoData).addClass('hidden');
    $(ctx).removeClass('hidden');


    $(ctx).removeClass('hidden');
    $('#chartKeyContainer, #chartTimelineContainer').removeClass('hidden');

    let pointStyleArr = buildPointStyleArray(window.bpchart.data);

    let goal = getCurrentBPGoal();

    // see https://www.chartjs.org/docs/2.9.3/

    let config = {
        type: 'line',
        data: {
            datasets: [{
                type: 'scatter',
                label: 'Systolic',
                pointRadius: 3,
                // pointStyle: pointStyleArr,
                pointStyle: 'circle',
                fill: false,
                borderColor: 'rgba(126, 194, 185, 0.6)',
                borderWidth: 2,
                pointBorderColor: 'rgba(126, 194, 185, 1)',
                pointBackgroundColor: 'rgba(126, 194, 185, 0.6)',
                // tension: 0,
                data: toIEScatterData(window.bpchart.data, 'systolic')
            }, {
                type: 'line',
                label: 'Systolic Trend',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(0, 127, 109, 1)',
                borderWidth: 2,
                // tension: 0.1,
                data: toIETrendLineData(window.bpchart.data, 'systolic')
            }, {
                type: 'scatter',
                label: 'Diastolic',
                pointRadius: 3,
                // pointStyle: pointStyleArr,
                pointStyle: 'circle',
                fill: false,
                borderColor: 'rgba(207, 178, 137, 0.6)',
                borderWidth: 2,
                pointBorderColor: 'rgba(207, 178, 137, 1)',
                pointBackgroundColor: 'rgba(207, 178, 137, 0.6)',
                // tension: 0,
                data: toIEScatterData(window.bpchart.data, 'diastolic')
            }, {
                type: 'line',
                label: 'Diastolic Trend',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(153, 97, 36, 1)',
                borderWidth: 1,
                // tension: 0.1,
                data: toIETrendLineData(window.bpchart.data, 'diastolic')
            } ]
        },
        options: {
            title: {
                text: "Blood Pressure"
            },
            legend: {
                display: false
                // labels: {
                //     usePointStyle: true
                // }
            },
            scales: {
                x: {
                    type: 'time',
                    time: {
                        unit: 'day'
                    }
                },
                y: {
                    type: 'linear',
                    title: {
                        text: 'Blood Pressure',
                        display: true
                    },
                    suggestedMin: 0,
                    suggestedMax: 200
                }
            },
        }
    };

    if (window.bpchart.startDate !== undefined) {
        config.options.scales.x.suggestedMin = window.bpchart.startDate;
    }

    if (window.bpchart.endDate !== undefined) {
        config.options.scales.x.suggestedMax = window.bpchart.endDate;
    }

    return new Chart(ctx, config);
}

function toIEScatterData(data, type) {
    return toScatterData(data, type).map(function(item) {
        return {
            x: item.x,
            y: item.y
        }
    });
}

function toIETrendLineData(data, type) {
    return toTrendLineData(data, type).map(function(item) {
        return {
            x: item.x,
            y: item.y
        }
    });
}