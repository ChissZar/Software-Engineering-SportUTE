package vn.edu.ute.sportute.service;

import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.ute.sportute.util.IdGenerator;
import static org.mockito.Mockito.*;

final class TestIds {
    static void inject(Object service) {
        var counter = new java.util.concurrent.atomic.AtomicInteger();
        var ids=mock(IdGenerator.class, invocation -> invocation.getMethod().getName().equals("generateId")
            ? invocation.getArgument(0) + String.format("%03d",counter.incrementAndGet()) : null);
        ReflectionTestUtils.setField(service,"idGenerator",ids);
    }
}
