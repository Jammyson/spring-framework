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

package org.springframework.aop.aspectj.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

	/**
	 * 缓存被定义为切面的BeanName
	 * {@link BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors()}
	 */
	@Nullable
	private volatile List<String> aspectBeanNames;

	/**
	 * 缓存切面Bean(单例Bean)中定义的Advice方法解析完毕的Advisor
	 * {@link BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors()}
	 */
	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	/**
	 * 缓存切面Bean(非单例)的MetadataAwareAspectInstanceFactory,它是对切面class的封装.
	 * {@link BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors()}
	 */
	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 * <trans>
	 *     遍历所有的Bean,根据是否使用@AspectJ标注为筛选条件获取被声明为切面的Bean.
	 *     然后筛选切面Bean中中使用{@link ASPECTJ_ANNOTATION_CLASSES}注解声明
	 *     为Advice的方法,将注解转换成相应的Advice处理器,并使用Advisor对Advise处理器、通知method
	 * 	   通知作用的PointCut进行封装.
	 * </trans>
	 *
	 * @return 所有切面Bean中定义的通知方法的Advisor
	 */
	public List<Advisor> buildAspectJAdvisors() {
		// 被定义为切面的BeanName
		List<String> aspectNames = this.aspectBeanNames;

		/**
		 * 筛选BeanName,为aspectNames赋值
		 * 第一次执行当前方法时aspectBeanNames将会被赋值.
		 */
		if (aspectNames == null) {
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					// 记录构建完毕的Advisor
					List<Advisor> advisors = new ArrayList<>();

					aspectNames = new ArrayList<>();

					// 获取所有BeanName
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);

					// 筛选
					for (String beanName : beanNames) {
						/**
						 * 根据BeanName过滤Bean,判断给定的BeanName是否符合条件,默认情况下返回true
						 * @see AnnotationAwareAspectJAutoProxyCreator.BeanFactoryAspectJAdvisorsBuilderAdapter#isEligibleBean(java.lang.String)
						 * @see AnnotationAwareAspectJAutoProxyCreator#isEligibleAspectBean(java.lang.String)
						 */
						if (!isEligibleBean(beanName)) {
							continue;
						}

						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
						Class<?> beanType = this.beanFactory.getType(beanName);
						if (beanType == null) {
							continue;
						}

						/**
						 * 过滤未被定义为切面的BeanClass.
						 * 		1、使用@@Aspect标注
						 * 		2、类中定义了使用以ajc$开头的属性名(field style,这是AspectJ定义切面的规则)
						 */
						if (this.advisorFactory.isAspect(beanType)) {
							// 记录切面的BeanName
							aspectNames.add(beanName);

							// 构建切面Metadata
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								// spring对切面Metadata的封装
								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);

								/**
								 * 筛选给定aspectInstanceFactory中的切面class中声明为Advice的方法,并获取到相应的Advice处理器,
								 * 然后将Advice处理器使用Advisor包装.
								 */
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);

								if (this.beanFactory.isSingleton(beanName)) {
									// 将构建完毕的Advisor放入缓存中
									this.advisorsCache.put(beanName, classAdvisors);
								}
								else {
									// 非单例Bean切面仅缓存切面class(MetadataAwareAspectInstanceFactory)
									this.aspectFactoryCache.put(beanName, factory);
								}

								advisors.addAll(classAdvisors);
							} else {
								// Per target or per this.
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory);
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}

					// 返回所有的定义为Bean的切面
					this.aspectBeanNames = aspectNames;
					return advisors;
				}
			}
		}

		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			}
			else {
				// 从缓存中获取，避免重复解析
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				// 对于切面 bean 类型，获取 bean 中定义的所有切点，并为每个切点生成对应的增强器；
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 * <trans> 用于判断给定的Bean是否是一个合格的、能够被代理的Bean </trans>
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}
}
