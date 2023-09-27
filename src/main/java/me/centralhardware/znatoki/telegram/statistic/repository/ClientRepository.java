package me.centralhardware.znatoki.telegram.statistic.repository;

import me.centralhardware.znatoki.telegram.statistic.entity.Client;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientRepository extends CrudRepository<Client, Integer> {

    List<Client> findAll();

    List<Client> findByOrganizationId(UUID organizationId);

    List<Client> findAllByNameAndSecondNameAndLastName(String name, String secondName, String lastName);

    List<Client> findByTelephone(String telephone);

    List<Client> findByTelephoneResponsible(String telephoneMother);

    @Query(value = """
        SELECT *
        FROM client
        WHERE lower(trim(concat(id, ' ', name, ' ', second_name, ' ', last_name))) = lower(:fio)
        ORDER BY date_of_record DESC
        LIMIT 1
    """, nativeQuery = true)
    Client findByFioAndId(@Param("fio") String fio);

}