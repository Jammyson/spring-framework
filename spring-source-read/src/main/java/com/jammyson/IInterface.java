package com.jammyson;

import org.springframework.beans.factory.support.SimpleInstantiationStrategy;

/**
 * @author Y.H.Zhou - zhouyuhang@deepexi.com
 * @since 2020/11/10.
 * <p> 用于测试如果Bean实现了接口并重写了方法,那么Bean将会怎样被创建 </p>
 * @see SimpleInstantiationStrategy#instantiate(org.springframework.beans.factory.support.RootBeanDefinition, java.lang.String, org.springframework.beans.factory.BeanFactory)
 */
public interface IInterface {

	void doSomething();
}
