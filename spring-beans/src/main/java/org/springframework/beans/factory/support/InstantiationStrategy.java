/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

/**
 * Interface responsible for creating instances corresponding to a root bean definition.
 * <trans>
 *		根据给定的RooBeanDefinition创建Bean对象.
 * </trans>
 *
 * <p>This is pulled out into a strategy as various approaches are possible,
 * including using CGLIB to create subclasses on the fly to support Method Injection.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public interface InstantiationStrategy {

	/**
	 * Return an instance of the bean with the given name in this factory.
	 * <trans> 根据给定的BD，使用无参构造函数创建Bean.在spring注解的方式下都是使用这个方法进行实例化 </trans>
	 *
	 * @param bd the bean definition
	 * 待创建的BD
	 *
	 * @param beanName the name of the bean when it is created in this context.
	 * The name can be {@code null} if we are autowiring a bean which doesn't
	 * belong to the factory.
	 * 待创建的BeanName
	 *
	 * @param owner the owning BeanFactory
	 * 当前BeanFactory
	 *
	 * @return a bean instance for this bean definition
	 * 实例化完毕的Bean
	 *
	 * @see AbstractAutowireCapableBeanFactory#instantiateBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition)
	 * @see org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java:1435
	 *
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner)
			throws BeansException;

	/**
	 * Return an instance of the bean with the given name in this factory,
	 * creating it via the given constructor.
	 * <trans> 根据给定的构造方法实例化Bean.构造注入的方式就是使用这个方法进行实例化 </trans>
	 *
	 * @param bd the bean definition
	 * @param beanName the name of the bean when it is created in this context.
	 * The name can be {@code null} if we are autowiring a bean which doesn't
	 * belong to the factory.
	 * @param owner the owning BeanFactory   BeanFactory
	 * @param ctor the constructor to use    指定的构造函数
	 * @param args the constructor arguments to apply   指定的构造参数
	 * @return a bean instance for this bean definition   实例化完毕的Bean
	 * @throws BeansException if the instantiation attempt failed
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			Constructor<?> ctor, Object... args) throws BeansException;

	/**
	 * Return an instance of the bean with the given name in this factory,
	 * creating it via the given factory method.
	 * <trans> 通过给定的Factory method实例化Bean.使用@Bean就是用的这个方法进行实例化的 </trans>
	 *
	 * @param bd the bean definition
	 * 使用的BD
	 *
	 * @param beanName the name of the bean when it is created in this context.
	 * The name can be {@code null} if we are autowiring a bean which doesn't
	 * belong to the factory.
	 * BeanName
	 *
	 * @param owner the owning BeanFactory
	 * 要创建的实例属于哪个BeanFactory
	 *
	 * @param factoryBean the factory bean instance to call the factory method on,
	 * or {@code null} in case of a static factory method
	 * factory method所在的Bean，如果为null则说明factory method为静态方法
	 *
	 * @param factoryMethod the factory method to use
	 * 实例化Bean的方法
	 *
	 * @param args the factory method arguments to apply
	 * 实例化Bean方法的参数
	 *
	 * @return a bean instance for this bean definition
	 * 实例化完毕的Bean
	 */
	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			@Nullable Object factoryBean, Method factoryMethod, Object... args)
			throws BeansException;

}
