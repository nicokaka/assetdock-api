package com.assetdock.api.person.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.person.domain.Person;
import com.assetdock.api.person.domain.PersonRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonManagementService {

	private final PersonRepository personRepository;
	private final TenantAccessService tenantAccessService;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public PersonManagementService(
		PersonRepository personRepository,
		TenantAccessService tenantAccessService,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.personRepository = personRepository;
		this.tenantAccessService = tenantAccessService;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	@Transactional
	public PersonView createPerson(AuthenticatedUserPrincipal actor, CreatePersonCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetWriteAccess(actor, organizationId);

		if (command.email() != null && !command.email().isBlank()) {
			if (personRepository.existsByOrganizationIdAndEmail(organizationId, command.email())) {
				throw new InvalidPersonRequestException("Email is already in use by another person in this organization.");
			}
		}

		Instant now = Instant.now(clock);
		Person person = new Person(
			UUID.randomUUID(),
			organizationId,
			command.fullName().trim(),
			command.email() == null || command.email().isBlank() ? null : command.email().trim(),
			command.department() == null || command.department().isBlank() ? null : command.department().trim(),
			command.active(),
			now,
			now
		);

		Person savedPerson = personRepository.save(person);

		auditLogService.recordInCurrentTransaction(new AuditLogCommand(
			savedPerson.organizationId(),
			actor.userId(),
			AuditEventType.USER_CREATED, // We can reuse USER_CREATED/UPDATED audit event types or map them
			"person",
			savedPerson.id(),
			"SUCCESS",
			java.util.Map.of(
				"fullName", savedPerson.fullName(),
				"email", savedPerson.email() != null ? savedPerson.email() : "",
				"department", savedPerson.department() != null ? savedPerson.department() : ""
			)
		));

		return toView(savedPerson);
	}

	@Transactional(readOnly = true)
	public PersonPageView listPeople(AuthenticatedUserPrincipal actor, Integer page, Integer size, String search, Boolean active) {
		int actualPage = page != null && page > 0 ? page : 1;
		int actualSize = size != null && size > 0 && size <= 100 ? size : 20;
		int offset = (actualPage - 1) * actualSize;

		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetReadAccess(actor, organizationId);

		List<PersonView> items = personRepository.findAllPaginated(organizationId, actualSize, offset, search, active)
			.stream()
			.map(this::toView)
			.toList();

		long totalItems = personRepository.countForOrganization(organizationId, search, active);
		int totalPages = (int) Math.ceil((double) totalItems / actualSize);

		return new PersonPageView(items, actualPage, actualSize, totalItems, totalPages);
	}

	@Transactional(readOnly = true)
	public PersonView getPerson(AuthenticatedUserPrincipal actor, UUID personId) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetReadAccess(actor, organizationId);

		Person person = personRepository.findByIdAndOrganizationId(personId, organizationId)
			.orElseThrow(PersonNotFoundException::new);

		return toView(person);
	}

	@Transactional
	public PersonView updatePerson(AuthenticatedUserPrincipal actor, UUID personId, UpdatePersonCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetWriteAccess(actor, organizationId);

		Person existingPerson = personRepository.findByIdAndOrganizationId(personId, organizationId)
			.orElseThrow(PersonNotFoundException::new);

		if (command.email() != null && !command.email().isBlank() && !command.email().equalsIgnoreCase(existingPerson.email())) {
			if (personRepository.existsByOrganizationIdAndEmail(organizationId, command.email())) {
				throw new InvalidPersonRequestException("Email is already in use by another person in this organization.");
			}
		}

		Instant now = Instant.now(clock);
		Person updatedPerson = new Person(
			existingPerson.id(),
			organizationId,
			command.fullName().trim(),
			command.email() == null || command.email().isBlank() ? null : command.email().trim(),
			command.department() == null || command.department().isBlank() ? null : command.department().trim(),
			command.active(),
			existingPerson.createdAt(),
			now
		);

		personRepository.update(updatedPerson);

		auditLogService.recordInCurrentTransaction(new AuditLogCommand(
			organizationId,
			actor.userId(),
			AuditEventType.USER_UPDATED,
			"person",
			updatedPerson.id(),
			"SUCCESS",
			java.util.Map.of(
				"fullName", updatedPerson.fullName(),
				"email", updatedPerson.email() != null ? updatedPerson.email() : "",
				"department", updatedPerson.department() != null ? updatedPerson.department() : ""
			)
		));

		return toView(updatedPerson);
	}

	@Transactional
	public void deletePerson(AuthenticatedUserPrincipal actor, UUID personId) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetWriteAccess(actor, organizationId);

		Person existingPerson = personRepository.findByIdAndOrganizationId(personId, organizationId)
			.orElseThrow(PersonNotFoundException::new);

		// Deactivate the person instead of hard delete to keep audit history
		Instant now = Instant.now(clock);
		Person deactivatedPerson = new Person(
			existingPerson.id(),
			organizationId,
			existingPerson.fullName(),
			existingPerson.email(),
			existingPerson.department(),
			false, // inactive
			existingPerson.createdAt(),
			now
		);

		personRepository.update(deactivatedPerson);

		auditLogService.recordInCurrentTransaction(new AuditLogCommand(
			organizationId,
			actor.userId(),
			AuditEventType.USER_DISABLED,
			"person",
			existingPerson.id(),
			"SUCCESS",
			java.util.Map.of("fullName", existingPerson.fullName())
		));
	}

	private PersonView toView(Person person) {
		return new PersonView(
			person.id(),
			person.organizationId(),
			person.fullName(),
			person.email(),
			person.department(),
			person.active(),
			person.createdAt(),
			person.updatedAt()
		);
	}

	private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
		if (actor.organizationId() == null) {
			throw new InvalidPersonRequestException("Tenant organization context is required.");
		}
		return actor.organizationId();
	}
}
