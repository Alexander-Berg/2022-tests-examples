package ru.yandex.qe.dispenser.ws.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceMapping;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceMappingDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceMappingDaoTest extends AcceptanceTestBase {

    @Autowired
    private ResourceMappingDao resourceMappingDao;

    private Resource storage;
    private Service nirvana;
    private Resource ytGpu;

    @BeforeEach
    public void init() {
        resourceMappingDao.clear();

        nirvana = Hierarchy.get().getServiceReader().read(NIRVANA);
        storage = Hierarchy.get().getResourceReader().read(new Resource.Key(STORAGE, nirvana));
        ytGpu = Hierarchy.get().getResourceReader().read(new Resource.Key(YT_GPU, nirvana));
        resourceMappingDao.create(Collections.singleton(new ResourceMapping(storage, storage.getPublicKey(), NIRVANA)));
    }

    @Test
    public void getMappingOptional() {
        final Optional<ResourceMapping> opt = resourceMappingDao.getMappingOptional(storage.getId());
        assertTrue(opt.isPresent());
        final ResourceMapping mapping = opt.get();
        assertEquals(storage, mapping.getResource());
        assertEquals(storage.getPublicKey(), mapping.getForeignResourceKey());
        assertEquals(NIRVANA, mapping.getForeignServiceKey());
    }

    @Test
    public void getMappingOptionalResource() {
        final Optional<ResourceMapping> opt = resourceMappingDao.getMappingOptional(storage);
        assertTrue(opt.isPresent());
        final ResourceMapping mapping = opt.get();
        assertEquals(storage, mapping.getResource());
        assertEquals(storage.getPublicKey(), mapping.getForeignResourceKey());
        assertEquals(NIRVANA, mapping.getForeignServiceKey());
    }

    @Test
    public void getAll() {
        final Set<ResourceMapping> all = resourceMappingDao.getAll();
        assertEquals(1, all.size());

        resourceMappingDao.create(Collections.singleton(new ResourceMapping(ytGpu, ytGpu.getPublicKey(), NIRVANA)));
        assertEquals(2, resourceMappingDao.getAll().size());
    }

    @Test
    public void getAllByResources() {
        Set<ResourceMapping> mappings = resourceMappingDao.getMappingForResources(Arrays.asList(storage, ytGpu));
        assertEquals(1, mappings.size());

        resourceMappingDao.create(Collections.singleton(new ResourceMapping(ytGpu, ytGpu.getPublicKey(), NIRVANA)));
        mappings = resourceMappingDao.getMappingForResources(Arrays.asList(storage, ytGpu));
        assertEquals(2, mappings.size());
    }
}
