package io.pivotal.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.pivotal.domain.Attribute;
import io.pivotal.domain.CustomJobMetric;
import io.pivotal.domain.JobDetail;
import io.pivotal.domain.Metrics;
import io.pivotal.domain.Rule;
import io.pivotal.domain.VMMetric;
import io.pivotal.service.EmailService;
import io.pivotal.service.MetricService;

@Profile("jmx")
@Service
public class Scheduler {

	@Autowired
	MetricService metricService;

	@Autowired
	EmailService emailService;

	Map<String, ArrayList<Rule>> jobRulesMap = new HashMap<>();

	private String errors = "";

	@PostConstruct
	public void readRules() {
		try {
			BufferedReader br = getBufferedReader();
			String rule = "";
			while (br.ready() && (rule = br.readLine()) != null) {
				String[] ruleDetails = rule.split("\\,");
				if (ruleDetails.length != 4) {
					System.out.println("Ignoring the rule: " + rule);
					continue;
				} else {
					String jobName = ruleDetails[0];
					String[] attributeNames = ruleDetails[1].split("\\|");
					Rule ruleObj = null;
					if (attributeNames.length == 2) {
						ruleObj = new Rule(attributeNames[0], attributeNames[1], new Integer(ruleDetails[2]),
								ruleDetails[3]);
					} else if (attributeNames.length == 1) {
						ruleObj = new Rule(attributeNames[0], null, new Integer(ruleDetails[2]), ruleDetails[3]);
					}
					ArrayList<Rule> jobRuleList = null;
					if (jobRulesMap.containsKey(jobName)) {
						jobRuleList = jobRulesMap.get(jobName);
						jobRuleList.add(ruleObj);
					} else {
						jobRuleList = new ArrayList<>();
						jobRuleList.add(ruleObj);
					}
					jobRulesMap.put(jobName, jobRuleList);
				}
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	BufferedReader getBufferedReader() {
		InputStream inputStream = Scheduler.class.getClassLoader().getResourceAsStream("rules.csv");
		Reader in = new InputStreamReader(inputStream);
		BufferedReader br = new BufferedReader(in);
		return br;
	}

	@Scheduled(fixedDelay = 60000)
	public void checkSystemHealth() {
		StringBuilder errors = new StringBuilder();

		Metrics metrics = metricService.getMetrics();
		List<VMMetric> vmMetrics = metrics.getVmMetrics();
		for (VMMetric vmMetric : vmMetrics) {
			if (!vmMetric.getFixedAttribute().getSystem_healthy().equalsIgnoreCase("1.0")) {
				JobDetail jobDetail = vmMetric.getJobDetail();
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(jobDetail.getDeployment()).append(":").append(jobDetail.getJob()).append(":")
						.append(jobDetail.getIndex()).append(":").append(jobDetail.getIp()).append("\n");
				errors.append(stringBuilder.toString());
			}
		}

		List<CustomJobMetric> customJobMetrics = metrics.getCustomJobMetrics();
		for (CustomJobMetric customJobMetric : customJobMetrics) {
			errors.append(executeRules(customJobMetric));
		}

		if (!errors.toString().isEmpty()) {
			setErrors(errors.toString());
			System.out.println(errors.toString());
			emailService.sendEmail(errors.toString());
		}

	}

	private String executeRules(CustomJobMetric customJobMetric) {
		String pattern = "###.##";
		DecimalFormat decimalFormat = new DecimalFormat(pattern);

		StringBuilder errors = new StringBuilder();
		String jobName = customJobMetric.getJobDetail().getJob().split("-")[0];
		if (jobRulesMap.containsKey(jobName)) {
			ArrayList<Rule> rules = jobRulesMap.get(jobName);
			for (Rule rule : rules) {
				List<Attribute> customAttributes = customJobMetric.getCustomAttributes();
				float value1 = 0;
				float value2 = 0;
				for (Attribute attribute : customAttributes) {
					if (attribute.getName().equalsIgnoreCase(rule.getAttributeName1())) {
						value1 = Float.valueOf(attribute.getValue());
					}

					if (rule.getAttributeName2() != null
							&& attribute.getName().equalsIgnoreCase(rule.getAttributeName2())) {
						value2 = Float.valueOf(attribute.getValue());
					}
				}

				if (value2 != 0) {
					float percentage = value2 / value1 * 100;
					if (percentage < rule.getThreadshold()) {
						errors.append(rule.getMessage().trim()).append(" ").append(decimalFormat.format(percentage))
								.append("%").append("\n");
					}
				} else {
					if (value1 > rule.getThreadshold()) {
						errors.append(rule.getMessage().trim()).append(" ").append(decimalFormat.format(value1))
								.append("\n");
					}
				}
			}
		}
		return errors.toString();
	}

	public String getErrors() {
		return errors;
	}

	public void setErrors(String errors) {
		this.errors = errors;
	}

}
