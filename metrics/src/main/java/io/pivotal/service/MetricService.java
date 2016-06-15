package io.pivotal.service;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.pivotal.domain.Attribute;
import io.pivotal.domain.CustomJobMetric;
import io.pivotal.domain.FixedAttribute;
import io.pivotal.domain.JobDetail;
import io.pivotal.domain.Metrics;
import io.pivotal.domain.VMMetric;

@Profile("jmx")
@Service
public class MetricService {
	@Autowired
	MBeanServerConnection connection;

	public Metrics getMetrics() {
		Metrics metrics = new Metrics();

		Set<ObjectInstance> objects;
		try {
			objects = connection.queryMBeans(new ObjectName("org.cloudfoundry:*"), null);

			for (ObjectInstance object : objects) {
				populateMetrics(object, metrics);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Metrics - " + metrics);

		return metrics;
	}

	private void populateMetrics(ObjectInstance object, Metrics metrics) throws Exception {
		ObjectName objectName = object.getObjectName();
		Hashtable<String, String> keyPropertyList = objectName.getKeyPropertyList();
		JobDetail jobDetail = new JobDetail();

		Enumeration<String> keys = keyPropertyList.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String value = keyPropertyList.get(key);
			populateObject(jobDetail, key, value);
		}

		if ((StringUtils.isEmpty(jobDetail.getIp()) || jobDetail.getIp().equalsIgnoreCase("null"))
				&& !jobDetail.getDeployment().equalsIgnoreCase("untitled_dev")) {
			VMMetric vmMetric = populateVMMetrics(objectName, jobDetail);
			vmMetric.setJobDetail(jobDetail);
			metrics.addVmMetrics(vmMetric);
		} else {
			CustomJobMetric customJobMetric = populateCustomJobMetrics(objectName, jobDetail);
			customJobMetric.setJobDetail(jobDetail);
			metrics.addCustomJobMetrics(customJobMetric);
		}

	}

	private void populateObject(Object obj, String key, String value) throws Exception {
		try {
			Field field = obj.getClass().getDeclaredField(key);
			field.setAccessible(true);
			field.set(obj, value);
		} catch (Exception e) {
			throw e;
		}
	}

	private VMMetric populateVMMetrics(ObjectName objectName, JobDetail jobDetail) throws Exception {
		VMMetric vmMetric = new VMMetric();
		FixedAttribute fixedAttribute = new FixedAttribute();

		MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
		MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
		if (attributes != null) {
			for (MBeanAttributeInfo mBeanAttributeInfo : attributes) {
				String attributeName = mBeanAttributeInfo.getName();
				Object attributeValue = connection.getAttribute(objectName, attributeName);

				String fieldName = attributeName.replace(".", "_");
				String fieldValue = attributeValue.toString();

				try {
					populateObject(fixedAttribute, fieldName, fieldValue);
				} catch (Exception e) {
					vmMetric.addCustomAttributes(new Attribute(attributeName, attributeValue.toString()));
				}
			}
		}

		vmMetric.setFixedAttribute(fixedAttribute);
		vmMetric.setJobDetail(jobDetail);

		return vmMetric;
	}

	private CustomJobMetric populateCustomJobMetrics(ObjectName objectName, JobDetail job) throws Exception {
		CustomJobMetric customJobMetric = new CustomJobMetric();

		MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
		MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
		if (attributes != null) {
			for (MBeanAttributeInfo mBeanAttributeInfo : attributes) {
				String attributeName = mBeanAttributeInfo.getName();
				Object attributeValue = connection.getAttribute(objectName, attributeName);
				customJobMetric.addCustomAttributes(new Attribute(attributeName, attributeValue.toString()));
			}
		}

		return customJobMetric;
	}

}
