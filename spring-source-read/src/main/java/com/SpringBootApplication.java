package com;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by @author Zyh on @since 2019/10/8.
 * <p></p>
 */
public class SpringBootApplication {

	public static void main(String[] args) {

		// XML方式
		// ClassPathXmlApplicationContext classPathXmlApplicationContext
		// = new ClassPathXmlApplicationContext();

		// 注解方式启动容器
		AnnotationConfigApplicationContext annotationConfigApplicationContext
				= new AnnotationConfigApplicationContext(StartConfig.class);

		// 根据包名扫描
		// annotationConfigApplicationContext.scan();
	}

	/**
	 * <question>
	 *     如果不加@Configuration然后作为直接传入AnnotationConfigApplicationContext会怎么样
	 * </question>
	 * 如果不加@Configuration，这个类同样也是作为Bean被加载进spring容器中，但是如果不加的话那么这个配置类中@Bean方法
	 * 就只是一个普通的factory-method，详见@Bean中的注释中如果不和@Configuration配合使用时@Bean的作用。
	 */
	@Configuration
	@ComponentScan("com.jammyson")
	public static class StartConfig {}
}
