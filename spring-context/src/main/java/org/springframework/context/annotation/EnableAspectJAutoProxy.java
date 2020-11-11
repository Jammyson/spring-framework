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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables support for handling components marked with AspectJ's {@code @Aspect} annotation,
 * similar to functionality found in Spring's {@code <aop:aspectj-autoproxy>} XML element.
 * <Trans> 启用支持处理@Aspect标注的components，和XML的<aop:aspectj-autoproxy>作用类似。 </Trans>
 *
 * To be used on @{@link Configuration} classes as follows:
 * <Trans>与@Configuration配合使用的示例</Trans>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService();
 *     }
 *
 *     &#064;Bean
 *     public MyAspect myAspect() {
 *         return new MyAspect();
 *     }
 * }</pre>
 *
 * Where {@code FooService} is a typical POJO component and {@code MyAspect} is an
 * {@code @Aspect}-style aspect:
 *
 * <pre class="code">
 * public class FooService {
 *
 *     // various methods
 * }</pre>
 *
 * <pre class="code">
 * &#064;Aspect
 * public class MyAspect {
 *
 *     &#064;Before("execution(* FooService+.*(..))")
 *     public void advice() {
 *         // advise FooService methods as appropriate
 *     }
 * }</pre>
 *
 * In the scenario above, {@code @EnableAspectJAutoProxy} ensures that {@code MyAspect}
 * will be properly processed and that {@code FooService} will be proxied mixing in the
 * advice that it contributes.
 * <Trans>上面的场景中，@EnableAspectJAutoProxy会确保MyAspect被正确的处理，FooService则会根据MyAspect的配置被代理</Trans>
 *
 * <p>Users can control the type of proxy that gets created for {@code FooService} using
 * the {@link #proxyTargetClass()} attribute.
 * <Trans> 用户可以通过使用proxyTargetClass参数来控制创建FooService的代理方式 </Trans>
 * The following enables CGLIB-style 'subclass'
 * proxies as opposed to the default interface-based JDK proxy approach.
 * <Trans>下面的例子使用基于‘子类’的CGLIB-style方式来代替了默认的基于‘接口’的JDK动态代理</Trans>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy(proxyTargetClass=true)
 * public class AppConfig {
 *     // ...
 * }</pre>
 *
 * <p>Note that {@code @Aspect} beans may be component-scanned like any other.
 * Simply mark the aspect with both {@code @Aspect} and {@code @Component}:
 * <Trans>以任何方式声明的以@Aspect标注的Bean都可以使得AOP生效，如下使用@Component和@Aspect的配合使用</Trans>
 *
 * <pre class="code">
 * package com.foo;
 *
 * &#064;Component
 * public class FooService { ... }
 *
 * &#064;Aspect
 * &#064;Component
 * public class MyAspect { ... }</pre>
 *
 * Then use the @{@link ComponentScan} annotation to pick both up:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.foo")
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     // no explicit &#064Bean definitions required
 * }</pre>
 *
 * <b>Note: {@code @EnableAspectJAutoProxy} applies to its local application context only,
 * allowing for selective proxying of beans at different levels.</b> Please redeclare
 * {@code @EnableAspectJAutoProxy} in each individual context, e.g. the common root web
 * application context and any separate {@code DispatcherServlet} application contexts,
 * if you need to apply its behavior at multiple levels.
 *
 * <p>This feature requires the presence of {@code aspectjweaver} on the classpath.
 * While that dependency is optional for {@code spring-aop} in general, it is required
 * for {@code @EnableAspectJAutoProxy} and its underlying facilities.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.aspectj.lang.annotation.Aspect
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}.
	 *
	 * <Trans>
	 *     动态代理分为两种：
	 *     		1、以接口为基础进行动态代理，动态生成目标代理接口的实现类.比如说JDK动态代理
	 *    		2、以类为基础进行动态代理,生成目标类的子类作为动态代理类.比如Cglib.
	 *	   该配置的意思就是是否以类为基础进行动态代理.true为使用CGLIB进行动态代理,false为优先使用JDK动态代理，
	 * </Trans>
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
	 * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
	 * Off by default, i.e. no guarantees that {@code AopContext} access will work.
	 * @since 4.3.1
	 *
	 * <Trans>
	 *     是否启用AopContext，声明被代理的类可以通过AopContext获取，这个配置类与Aop代理类中内部方法之间调用相关.
	 * </Trans>
	 */
	boolean exposeProxy() default false;

}
