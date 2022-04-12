package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.vsac.ValueSet;
import edu.ohsu.cmp.coach.repository.vsac.ValueSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValueSetService extends AbstractService {
    @Autowired
    private ValueSetRepository repository;

    public ValueSet getValueSet(String oid) {
        return repository.findOneByOid(oid);
    }
}
