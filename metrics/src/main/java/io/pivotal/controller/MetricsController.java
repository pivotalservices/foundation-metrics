package io.pivotal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.pivotal.domain.Metrics;
import io.pivotal.service.MetricService;

@Profile("jmx")
@RestController
@RequestMapping(value = "metrics")
public class MetricsController {
	@Autowired
	MetricService metricService;

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET)
	public Metrics getAllMetrics() {
		Metrics metrics = metricService.getMetrics();
		return metrics;
	}
}
