package com.assetdock.api.person.api;

import com.assetdock.api.checkout.application.CheckoutService;
import com.assetdock.api.checkout.application.CheckoutView;
import com.assetdock.api.person.application.CreatePersonCommand;
import com.assetdock.api.person.application.PersonManagementService;
import com.assetdock.api.person.application.PersonPageView;
import com.assetdock.api.person.application.PersonView;
import com.assetdock.api.person.application.UpdatePersonCommand;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/people")
public class PersonController {

	private final PersonManagementService personManagementService;
	private final CheckoutService checkoutService;

	public PersonController(PersonManagementService personManagementService, CheckoutService checkoutService) {
		this.personManagementService = personManagementService;
		this.checkoutService = checkoutService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PersonView create(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody CreatePersonRequest request
	) {
		return personManagementService.createPerson(
			principal,
			new CreatePersonCommand(
				request.fullName(),
				request.email(),
				request.department(),
				request.active()
			)
		);
	}

	@GetMapping
	public PersonPageView list(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size,
		@RequestParam(required = false) String search,
		@RequestParam(required = false) Boolean active
	) {
		return personManagementService.listPeople(principal, page, size, search, active);
	}

	@GetMapping("/{id}")
	public PersonView get(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return personManagementService.getPerson(principal, id);
	}

	@PutMapping("/{id}")
	public PersonView update(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody UpdatePersonRequest request
	) {
		return personManagementService.updatePerson(
			principal,
			id,
			new UpdatePersonCommand(
				request.fullName(),
				request.email(),
				request.department(),
				request.active()
			)
		);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		personManagementService.deletePerson(principal, id);
	}

	@GetMapping("/{id}/checkouts")
	public List<CheckoutView> getHistory(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return checkoutService.getHistoryByPersonId(principal, id);
	}
}
