package io.pivotal.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventService {

	static String EVENT_API_URI = "/v2/events";

	@Autowired
	CloudFoundryClientService clientService;

	public Integer getTotalAppCreateEvents() {
		Map<String, Object> respMap = clientService.getResponseMap(EVENT_API_URI + "?q=type:audit.app.create");
		Integer totalCount = clientService.getTotalResults(respMap);
		return totalCount;
	}

	public Integer getTotalAppDeleteEvents() {
		Map<String, Object> respMap = clientService.getResponseMap(EVENT_API_URI + "?q=type:audit.app.delete-request");
		Integer totalCount = clientService.getTotalResults(respMap);
		return totalCount;
	}

	public Integer getTotalAppCrashEvents() {
		Map<String, Object> respMap = clientService.getResponseMap(EVENT_API_URI + "?q=type:audit.app.crash");
		Integer totalCount = clientService.getTotalResults(respMap);
		return totalCount;
	}

	public Integer getTotalAuthorizedSSHEvents() {
		Map<String, Object> respMap = clientService.getResponseMap(EVENT_API_URI + "?q=type:audit.app.ssh-authorized");
		Integer totalCount = clientService.getTotalResults(respMap);
		return totalCount;
	}

	public Integer getTotalUnAuthorizedSSHEvents() {
		Map<String, Object> respMap = clientService
				.getResponseMap(EVENT_API_URI + "?q=type:audit.app.ssh-unauthorized");
		Integer totalCount = clientService.getTotalResults(respMap);
		return totalCount;
	}

	public Integer getTotalAppStartEvents() {
		Map<String, Object> respMap = clientService.getResponseMap(EVENT_API_URI + "?q=type:audit.app.start");
		Integer totalCount = clientService.getTotalResults(respMap);
		return totalCount;
	}

	public Integer getTotalAppStopEvents() {
		Map<String, Object> respMap = clientService.getResponseMap(EVENT_API_URI + "?q=type:audit.app.stop");
		Integer totalCount = clientService.getTotalResults(respMap);
		return totalCount;
	}

	public Integer getTotalAppUpdateEvents() {
		Map<String, Object> respMap = clientService.getResponseMap(EVENT_API_URI + "?q=type:audit.app.update");
		Integer totalCount = clientService.getTotalResults(respMap);
		return totalCount;
	}

}
