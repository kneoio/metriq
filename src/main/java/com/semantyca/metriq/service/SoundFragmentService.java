package com.semantyca.metriq.service;

import com.semantyca.core.service.AbstractService;
import com.semantyca.core.service.UserService;
import com.semantyca.metriq.repository.SoundFragmentRepository;
import com.semantyca.mixpla.model.soundfragment.SoundFragment;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ApplicationScoped
public class SoundFragmentService extends AbstractService<SoundFragment, Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundFragmentService.class);

    private final SoundFragmentRepository repository;


    protected SoundFragmentService() {
        super(null);
        this.repository = null;
    }

    @Inject
    public SoundFragmentService(UserService userService, SoundFragmentRepository repository) {
        super(userService);
        this.repository = repository;
    }


    public Uni<Integer> hardDelete(UUID id) {
        assert repository != null;
        return repository.hardDelete(id);
    }


}
