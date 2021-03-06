/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.weld.bootstrap.events;

import static org.jboss.weld.util.Observers.validateObserverMethod;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.builder.ObserverMethodConfigurator;

import org.jboss.weld.bootstrap.events.builder.ObserverMethodBuilderImpl;
import org.jboss.weld.bootstrap.events.builder.ObserverMethodConfiguratorImpl;
import org.jboss.weld.logging.BootstrapLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Preconditions;

/**
 * Implementation of the event used to notify observers for each observer method that is added.
 *
 * @author David Allen
 * @author Martin Kouba
 */
public class ProcessObserverMethodImpl<T, X> extends AbstractDefinitionContainerEvent
        implements ProcessObserverMethod<T, X> {

    public static <T, X> ObserverMethod<T> fire(BeanManagerImpl beanManager, AnnotatedMethod<X> beanMethod, ObserverMethod<T> observerMethod) {
        ProcessObserverMethodImpl<T, X> event = new ProcessObserverMethodImpl<T, X>(beanManager, beanMethod, observerMethod) {
        };
        event.fire();
        if (event.vetoed) {
            return null;
        }
        return event.observerMethod;
    }

    private final AnnotatedMethod<X> beanMethod;
    private final ObserverMethod<T> initialObserverMethod;
    private ObserverMethod<T> observerMethod;
    private ObserverMethodConfiguratorImpl<T> configurator;
    private boolean vetoed;

    // we need this to ensure that configurator and set method are not invoked within one observer
    private boolean observerMethodSet;

    private ProcessObserverMethodImpl(BeanManagerImpl beanManager, AnnotatedMethod<X> beanMethod, ObserverMethod<T> observerMethod) {
        super(beanManager, ProcessObserverMethod.class, new Type[] { observerMethod.getObservedType(), observerMethod.getBeanClass() });
        this.beanMethod = beanMethod;
        this.initialObserverMethod = observerMethod;
        this.observerMethod = observerMethod;
    }

    public AnnotatedMethod<X> getAnnotatedMethod() {
        checkWithinObserverNotification();
        return beanMethod;
    }

    public ObserverMethod<T> getObserverMethod() {
        checkWithinObserverNotification();
        return observerMethod;
    }

    @Override
    public void setObserverMethod(ObserverMethod<T> observerMethod) {
        if (configurator != null) {
            throw BootstrapLogger.LOG.configuratorAndSetMethodBothCalled(ProcessObserverMethod.class.getSimpleName(), getReceiver());
        }
        Preconditions.checkArgumentNotNull(observerMethod, "observerMethod");
        checkWithinObserverNotification();
        replaceObserverMethod(observerMethod);
        observerMethodSet = true;
    }

    @Override
    public ObserverMethodConfigurator<T> configureObserverMethod() {
        if (observerMethodSet) {
            throw BootstrapLogger.LOG.configuratorAndSetMethodBothCalled(ProcessObserverMethod.class.getSimpleName(), getReceiver());
        }
        checkWithinObserverNotification();
        if (configurator == null) {
            configurator = new ObserverMethodConfiguratorImpl<>(observerMethod);
        }
        return configurator;
    }

    @Override
    public void veto() {
        checkWithinObserverNotification();
        vetoed = true;
    }

    public boolean isDirty() {
        return observerMethod != initialObserverMethod;
    }

    @Override
    public void postNotify(Extension extension) {
        super.postNotify(extension);
        if (configurator != null) {
            replaceObserverMethod(new ObserverMethodBuilderImpl<>(configurator).build());
            configurator = null;
        }
        observerMethodSet = false;
    }

    private void replaceObserverMethod(ObserverMethod<T> observerMethod) {
        validateObserverMethod(observerMethod, getBeanManager(), initialObserverMethod);
        this.observerMethod = observerMethod;
    }

}
