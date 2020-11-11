/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}

	/**
	 * 1、执行内部自定义的BeanDefinitionRegistryPostProcessor
	 * 2、通过ConfigurationClassPostProcessor将所有该被注册的Bean都扫描成BD放入BeanFactory中
	 *    并对配置类进行处理，比如将@Bean转换成BD，处理@Import注解等
	 * 3、创建BeanDefinitionRegistryPostProcessor为Bean并执行
	 * 4、创建BeanFactoryPostProcessor为Bean并执行
	 * @param beanFactoryPostProcessors   传入内置的后置处理器(非Bean的后置处理器)
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// 记录已被创建出来的Bean
		Set<String> processedBeans = new HashSet<>();

		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 执行传入的后置处理器(非Bean)
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				// 优先执行BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry();
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 记录BeanFactoryPostProcessor,等待执行postProcessBeanFactory
					registryProcessors.add(registryProcessor);
				}
				else {
					// 记录BeanFactoryPostProcessor,等待执行postProcessBeanFactory
					regularPostProcessors.add(postProcessor);
				}
			}

			/**
			 * Do not initialize FactoryBeans here: We need to leave all regular beans
			 * uninitialized to let the bean factory post-processors apply to them!
			 */

			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			/**
			 * 获取此时BeanFactory中存在的BeanDefinitionRegistryPostProcessor,此时还没有进行Bean扫描.
			 * 此处获取到的将会是之前手动放入的BD Name - org.springframework.context.annotation.internalConfigurationAnnotationProcessor
			 * 它的class是class org.springframework.context.annotation.ConfigurationClassPostProcessor
			 */
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 将实现了PriorityOrdered的BeanDefinitionRegistryPostProcessor实例化
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 记录Bean，表示已经被创建
					processedBeans.add(ppName);
				}
			}

			// 排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 记录BeanDefinitionRegistryPostProcessor
			registryProcessors.addAll(currentRegistryProcessors);

			/**
			 * 执行currentRegistryProcessors中的BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry()
			 * 在这个过程中ConfigurationClassPostProcessor会被执行.
			 * ConfigurationClassPostProcessor：
			 * 		1、找到并处理启动时指定的配置类
			 * 		2、根据@ComponentScan扫描所有的Bean
			 * 		3、处理扫描到的符合配置类条件的BD,对配置注解如@Bean、@Import、@ComponentScan等进行处理
			 * 	    4、所有的配置类都会作为BD被加载到BeanFactory中
			 * 	    5、调用ImportBeanDefinitionRegistrar
			 * 	注意：包括配置类Bean在内，整个过程中并没有Bean被创建.spring基于ASM框架的org.springframework.asm.ClassReader
			 * 	读取class文件,获取class metadata进行操作
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

			// 清空执行完毕的BeanDefinitionRegistryPostProcessor
			currentRegistryProcessors.clear();

			// 由于上面的操作使得此时已经把所有的BeanDefinition都加载到容器当中了。所以以下执行的所有类型的后置处理器
			// 实际上已经包括了用户自定义的后置处理器了，不再是框架内部自定义的后置处理器。

			// 获取所有BeanDefinitionRegistryPostProcessor的BeanName
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 创建实现了BeanDefinitionRegistryPostProcessor和Order接口的BD为Bean
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 记录Bean，表示已经被创建
					processedBeans.add(ppName);
				}
			}
			// 排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);

			// 执行实现了BeanDefinitionRegistryPostProcessor和Ordered接口的Bean
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

			// 清空执行完毕的BeanDefinitionRegistryPostProcessor
			currentRegistryProcessors.clear();

			// 执行剩余的所有BeanDefinitionRegistryPostProcessor的Bean，上面定义的processedBeans这个容器是为了记录所有
			// 已经被执行过的BeanDefinitionRegistryPostProcessor，然后在这个地方使用while循环对这个执行过的BeanDefinitionRegistryPostProcessor
			// 进行排除
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				// 创建余下的所有未被创建过的BeanDefinitionRegistryPostProcessor为Bean
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				// 排序
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				// 执行
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				// 清空
				currentRegistryProcessors.clear();
			}

			// registryProcessors中存放的是上面已经被执行过的BeanDefinitionRegistryPostProcessors，由于
			// BeanDefinitionRegistryPostProcessors是BeanFactoryPostProcessor的子类，所以当执行完BeanDefinitionRegistryPostProcessors
			// 的方法后，此时将继续执行父类的BeanFactoryPostProcessor
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// 执行由spring容器初始化过程中内部自定义的、非Bean的BeanFactoryPostProcessor,由于它们不是Bean所以直接调用
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// 这个else不会被执行，仅仅是为了做一个保险操作而已
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// 获取所有被定义为Bean的BeanFactoryPostProcessor的名称
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// 区分PriorityOrdered和Ordered排序
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			// 确保已经执行过的后置处理器不会再执行
			if (processedBeans.contains(ppName)) {

			}
			// 创建实现了BeanFactoryPostProcessor和PriorityOrdered接口的Bean
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 添加到容器中
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			// 将实现了BeanFactoryPostProcessor和Ordered接口的Bean的名字添加到容器中
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			// 将剩下的实现了BeanFactoryPostProcessor的Bean的名字添加到容器中
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 对PriorityOrdered排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 执行
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// 创建实现了Ordered接口的Bean
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 执行
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// 创建剩下的BeanFactoryBeanPostProcessor
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 执行
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		/**
		 * 真实的BeanDefinition可能已经被上面执行的后置处理器给修改，所以需要清空BeanFactory中被缓存下来的
		 * MergedBeanDefinition.
		 */
		beanFactory.clearMetadataCache();
	}

	/**
	 * 注册被定义为Bean的BeanPostProcessor为Bean，将它们按照规则进行排序，最终放入BeanPostProcessor中
	 * 最终的BeanPostProcessor顺序
	 * 		已有的(内部添加)BeanPostProcessor - PriorityOrdered-BeanPostProcessor - Ordered-BeanPostProcessor
	 * 			- BeanPostProcessor - MergedBeanDefinitionPostProcessor - ApplicationListenerDetector
	 *
	 * @see org.springframework.beans.factory.support.AbstractBeanFactory#beanPostProcessors
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// 获取到所有BeanPostProcessor的BD name
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// 计算BeanPostProcessor个数 = 已存在于BeanFactory中(beanFactory.addBeanPostProcessor) + 作为Bean存在的BeanPostProcessor + 1
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;

		// 添加一个日志打印的BeanPostProcessor
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// 记录创建完毕的实现了PriorityOrdered的BeanPostProcessor Bean
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 记录创建完毕的MergedBeanDefinitionPostProcessor 的BeanPostProcessor Bean
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		// 记录实现了Ordered接口的BeanPostProcessor BeanName
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 记录未实现任何Ordered接口的BeanPostProcessor BeanName
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		// 遍历以Bean存在的BeanPostProcessor
		for (String ppName : postProcessorNames) {
			// 处理实现了PriorityOrdered接口的BeanPostProcessor
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 创建Bean
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				// 记录Bean
				priorityOrderedPostProcessors.add(pp);
				// 筛选记录MergedBeanDefinitionPostProcessor Bean
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			// 处理实现了Ordered接口的BeanPostProcessor
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 记录BeanName
				orderedPostProcessorNames.add(ppName);
			}
			// 处理未实现Ordered的BeanPostProcessor Bean
			else {
				// 记录BeanName
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 对实现了PriorityOrdered接口的BeanPostProcessor进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 将BeanPostProcessor放入BeanFactory
		// 注意：此时的BeanFactory中是存在内置BeanPostProcessor的
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// 记录实现了Ordered接口的BeanPostProcessor Bean
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			// 创建Bean
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			// 记录Bean
			orderedPostProcessors.add(pp);
			// 筛选记录MergedBeanDefinitionPostProcessor Bean
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// Order排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 将BeanPostProcessor放入BeanFactory
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			// 创建Bean
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			// 记录Bean
			nonOrderedPostProcessors.add(pp);
			// 筛选记录MergedBeanDefinitionPostProcessor Bean
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// 将BeanPostProcessor放入BeanFactory
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// 对internalPostProcessors排序
		sortPostProcessors(internalPostProcessors, beanFactory);
		// 将BeanPostProcessor放入BeanFactory
		// 由于internalPostProcessors是实现了MergedBeanDefinitionPostProcessor 且已经被放入BeanFactory
		// 所以说MergedBeanDefinitionPostProcessor会位于BeanPostProcessor的后面(因为addBeanPostProcessor会移除已有的并添加到后面)
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// 重新注册ApplicationListenerDetector，将它移到最后后
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			/**
			 * <Note>
			 *    INSTANCE是默认排序规则，一般情况下BeanFactory就是用的这个规则。PriorityOrdered继承于
			 *    Ordered，且PriorityOrdered是空实现，所以PriorityOrdered用的也是Ordered中的顺序值，
			 *    但是INSTANCE策略会先对PriorityOrdered进行排序，然后再对Ordered进行排序，即优先处理
			 *    PriorityOrdered。
			 * </Note>
			 *
			 */
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 * 将BeanPostProcessor放入BeanFactory
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			// 若添加的BeanPostProcessor已存在，则移除旧的，添加新的进去
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example1: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
