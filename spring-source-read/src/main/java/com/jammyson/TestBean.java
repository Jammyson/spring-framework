package com.jammyson;

import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @author Y.H.Zhou - zhouyuhang@deepexi.com
 * @since 2020/11/10.
 * <p> 普通声明一个Bean,用于源码调试 </p>
 */
@Component
public class TestBean {
	/**
	 * 测试spring的ByType是如何获取类型的.
	 * 结论: spring存在这样的约定: 如果一个类中的成员变量需要进行ByType注入,那么该成员变量一定存在
	 * 对应的set方法,并且该set方法的命名就是基于成员变量的命名.
	 * 比如说如果一个类中定义了一个setId的方法,那么spring就会认为该成员变量名称为id(去掉set然后转驼峰).
	 * 所以spring是根据set方法名称进行寻找的,与形参无关
	 * @see AbstractAutowireCapableBeanFactory#unsatisfiedNonSimpleProperties(
	 * org.springframework.beans.factory.support.AbstractBeanDefinition, org.springframework.beans.BeanWrapper)
	 */
	private Integer integer;

	public void setInteger(Integer a) {
		this.integer = a;
	}
}
