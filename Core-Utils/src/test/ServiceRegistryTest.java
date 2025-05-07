package com.minecraft.core.api.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ServiceRegistry class
 */
class ServiceRegistryTest {
    
    @Mock
    private TestService mockService;
    
    private AutoCloseable closeable;
    
    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        ServiceRegistry.init();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        ServiceRegistry.shutdown();
        closeable.close();
    }
    
    @Test
    void registerService() {
        // Register a service
        assertTrue(ServiceRegistry.register(TestService.class, mockService));
        
        // Verify it's registered
        assertTrue(ServiceRegistry.isRegistered(TestService.class));
        
        // Get the service
        TestService service = ServiceRegistry.getService(TestService.class);
        assertNotNull(service);
        assertEquals(mockService, service);
    }
    
    @Test
    void registerDuplicateService() {
        // Register a service
        assertTrue(ServiceRegistry.register(TestService.class, mockService));
        
        // Try to register it again (should fail)
        assertFalse(ServiceRegistry.register(TestService.class, mockService));
    }
    
    @Test
    void unregisterService() {
        // Register a service
        ServiceRegistry.register(TestService.class, mockService);
        
        // Unregister it
        assertTrue(ServiceRegistry.unregister(TestService.class));
        
        // Verify it's no longer registered
        assertFalse(ServiceRegistry.isRegistered(TestService.class));
        assertNull(ServiceRegistry.getService(TestService.class));
    }
    
    @Test
    void unregisterNonExistentService() {
        // Try to unregister a service that doesn't exist
        assertFalse(ServiceRegistry.unregister(TestService.class));
    }
    
    @Test
    void getAllServices() {
        // Register a service
        ServiceRegistry.register(TestService.class, mockService);
        
        // Get all services
        assertEquals(1, ServiceRegistry.getAllServices().size());
        assertTrue(ServiceRegistry.getAllServices().containsKey(TestService.class));
        assertEquals(mockService, ServiceRegistry.getAllServices().get(TestService.class));
    }
    
    @Test
    void shutdown() {
        // Register a service
        ServiceRegistry.register(TestService.class, mockService);
        
        // Shutdown
        ServiceRegistry.shutdown();
        
        // Initialize again
        ServiceRegistry.init();
        
        // Verify everything is cleared
        assertFalse(ServiceRegistry.isRegistered(TestService.class));
        assertEquals(0, ServiceRegistry.getAllServices().size());
    }
    
    /**
     * Interface for testing
     */
    private interface TestService {
        void doSomething();
    }
}