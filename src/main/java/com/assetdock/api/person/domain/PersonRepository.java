package com.assetdock.api.person.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository {

	boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);

	Person save(Person person);

	List<Person> findAllPaginated(UUID organizationId, int limit, int offset, String search, Boolean active);
	
	long countForOrganization(UUID organizationId, String search, Boolean active);

	Optional<Person> findByIdAndOrganizationId(UUID personId, UUID organizationId);

	Optional<Person> findById(UUID personId);

	Person update(Person person);
}
